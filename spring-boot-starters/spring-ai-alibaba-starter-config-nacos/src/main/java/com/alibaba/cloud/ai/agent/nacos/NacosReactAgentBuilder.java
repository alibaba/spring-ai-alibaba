/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.agent.nacos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.agent.nacos.utils.ChatOptionsProxy;
import com.alibaba.cloud.ai.agent.nacos.vo.AgentVO;
import com.alibaba.cloud.ai.agent.nacos.vo.McpServersVO;
import com.alibaba.cloud.ai.agent.nacos.vo.ModelVO;
import com.alibaba.cloud.ai.agent.nacos.vo.PromptVO;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.node.AgentLlmNode;
import com.alibaba.cloud.ai.graph.agent.node.AgentToolNode;
import com.alibaba.cloud.ai.observation.model.ObservationMetadataAwareOptions;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.config.NacosConfigService;
import com.alibaba.nacos.common.utils.StringUtils;
import io.micrometer.observation.ObservationRegistry;
import org.apache.commons.collections4.CollectionUtils;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;

import static com.alibaba.cloud.ai.agent.nacos.NacosAgentPromptBuilder.getMetadata;
import static com.alibaba.cloud.ai.agent.nacos.NacosMcpToolsInjector.convert;
import static com.alibaba.cloud.ai.agent.nacos.NacosModelInjector.replaceModel;
import static com.alibaba.cloud.ai.agent.nacos.NacosPromptInjector.getPromptByKey;

public class NacosReactAgentBuilder extends NacosAgentPromptBuilder {

	NacosContextHolder agentVOHolder = new NacosContextHolder();
	private NacosOptions nacosOptions;
	
	private List<ToolCallback> localTools = new ArrayList<>();

	public NacosReactAgentBuilder nacosOptions(NacosOptions nacosOptions) {
		this.nacosOptions = nacosOptions;
		return this;
	}
	
	private org.springframework.context.ApplicationEventPublisher eventPublisher;
	
	public NacosReactAgentBuilder eventPublisher(org.springframework.context.ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
		return this;
	}

	@Override
	public ReactAgent build() {
		if (this.name == null) {
			this.name = nacosOptions.getAgentName();
		}
		//1.load agent base vo
		AgentVO agentVO = NacosAgentInjector.loadAgentVO(nacosOptions);
		agentVOHolder.setAgentVO(agentVO);
		this.description = agentVO.getDescription();
		//2.load prompt vo
		PromptVO promptVO = getPromptByKey(nacosOptions, agentVO.getPromptKey());
		agentVOHolder.setPromptVO(promptVO);
		String systemPrompt = promptVO.getTemplate();

		//3.load model vo
		ModelVO modelVO = NacosModelInjector.getModelByAgentName(nacosOptions);
		agentVOHolder.setModelVO(modelVO);

		//4.build chat options and create model
		OpenAiChatOptions openAiChatOptions = buildProxyChatOptions(modelVO, getMetadata(promptVO));
		agentVOHolder.setObservationMetadataAwareOptions((ObservationMetadataAwareOptions) openAiChatOptions);
		this.chatOptions = openAiChatOptions;
		this.model = createModel(nacosOptions, modelVO, openAiChatOptions);

		//5.build chat client
		ChatClient.Builder clientBuilder = null;
		ObservationConfiguration observationConfiguration = nacosOptions.getObservationConfiguration();
		if (observationConfiguration == null) {
			clientBuilder = ChatClient.builder(model);
		}
		else {
			clientBuilder = ChatClient.builder(model, observationConfiguration.getObservationRegistry() == null ? ObservationRegistry.NOOP : observationConfiguration.getObservationRegistry(), nacosOptions.getObservationConfiguration()
					.getChatClientObservationConvention(), this.advisorObservationConvention);
		}

		clientBuilder.defaultOptions(chatOptions);
		chatClient = clientBuilder.build();

		//6.load mcp servers
		McpServersVO mcpServersVO = NacosMcpToolsInjector.getMcpServersVO(nacosOptions);
		agentVOHolder.setMcpServersVO(mcpServersVO);
		
		separateInterceptorsByType();
		
		List<ToolCallback> allTools = new ArrayList<>();
		this.localTools = gatherLocalTools();
		List<ToolCallback> mcpTools = convert(nacosOptions, mcpServersVO, eventPublisher);
		allTools.addAll(localTools);
		if (mcpTools != null) {
			allTools.addAll(mcpTools);
		}
		
		//7. build tools
		AgentLlmNode.Builder llmNodeBuilder = AgentLlmNode.builder()
				.chatClient(chatClient)
				.systemPrompt(systemPrompt);
		if (outputKey != null && !outputKey.isEmpty()) {
			llmNodeBuilder.outputKey(outputKey);
		}
		if (CollectionUtils.isNotEmpty(allTools)) {
			llmNodeBuilder.toolCallbacks(allTools);
		}
		AgentLlmNode llmNode = llmNodeBuilder.build();

		AgentToolNode.Builder builder = AgentToolNode.builder();
		if (toolExecutionExceptionProcessor != null) {
			builder.toolExecutionExceptionProcessor(toolExecutionExceptionProcessor);
		} else {
			builder.toolExecutionExceptionProcessor(DefaultToolExecutionExceptionProcessor.builder()
					.alwaysThrow(false)
					.build());
		}
		AgentToolNode toolNode;
		if (resolver != null) {
			builder.toolCallbackResolver(resolver);
		}
		if (CollectionUtils.isNotEmpty(allTools)) {
			builder.toolCallbacks(allTools);
		}
		toolNode = builder.build();
		
		// register listeners.

		//register model  listener
		registerModelListener(chatClient, nacosOptions);
		this.description = agentVO.getDescription();

		//register mcp tools
		registryMcpServerListener(llmNode, toolNode, nacosOptions);
		ReactAgent reactAgent = new ReactAgent(llmNode, toolNode, buildConfig(),this);
		agentVOHolder.setReactAgent(reactAgent);
		//register agent base and prompt
		registerAgentWithPrompt(nacosOptions, agentVO, agentVOHolder, reactAgent);
		return reactAgent;
	}

	private void registryMcpServerListener(AgentLlmNode llmNode, AgentToolNode toolNode, NacosOptions nacosOptions) {

		try {
			String dataId = (nacosOptions.isMcpServersEncrypted() ? "cipher-kms-aes-256-" : "") + "mcp-servers.json";
			nacosOptions.getNacosConfigService()
					.addListener(dataId, "ai-agent-" + nacosOptions.getAgentName(), new AbstractListener() {
						@Override
						public void receiveConfigInfo(String configInfo) {
							McpServersVO mcpServersVO = JSON.parseObject(configInfo, McpServersVO.class);
							List<ToolCallback> mcpTools = convert(nacosOptions, mcpServersVO, eventPublisher);
							if (mcpTools != null) {
								List<ToolCallback> allTools = new ArrayList<>();
								allTools.addAll(localTools);
								allTools.addAll(mcpTools);
								toolNode.setToolCallbacks(allTools);
								llmNode.setToolCallbacks(allTools);
							}

						}
					});
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}

	}

	void registerAgentWithPrompt(NacosOptions nacosOptions, AgentVO agentVO, NacosContextHolder nacosContextHolder, ReactAgent reactAgent) {
		try {
			NacosConfigService nacosConfigService = nacosOptions.getNacosConfigService();
			//1. register agent base listener
			String dataIdT = (nacosOptions.isAgentBaseEncrypted() ? "cipher-kms-aes-256-" : "") + "agent-base.json";
			nacosConfigService.addListener(dataIdT, "ai-agent-" + nacosOptions.getAgentName(),
					new AgentBaseListener(nacosOptions, agentVO.getPromptKey(), nacosContextHolder));
			//2. registry prompt vo listener
			registerPromptListener(nacosOptions, nacosContextHolder, agentVO.getPromptKey(), reactAgent);

		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * register prompt with key.
	 */
	static void registerPromptListener(NacosOptions nacosOptions, NacosContextHolder nacosContextHolder,
			String promptKey, ReactAgent reactAgent) {
		try {
			PromptListener promptListener = new PromptListener(nacosContextHolder, reactAgent);
			String dataId = (nacosOptions.isPromptEncrypted() ? "cipher-kms-aes-256-" : "") + String.format("prompt-%s.json", promptKey);

			nacosOptions.getNacosConfigService().addListener(dataId,
					"nacos-ai-meta", promptListener);
			nacosContextHolder.promptListeners.put(promptKey, promptListener);
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

	private void registerModelListener(ChatClient chatClient, NacosOptions nacosOptions) {

		try {
			String agentName = nacosOptions.getAgentName();
			String dataIdT = (nacosOptions.isModelEncrypted() ? "cipher-kms-aes-256-" : "") + "model.json";
			nacosOptions.getNacosConfigService()
					.addListener(dataIdT, "ai-agent-" + agentName, new AbstractListener() {
						@Override
						public void receiveConfigInfo(String configInfo) {
							ModelVO modelVO = JSON.parseObject(configInfo, ModelVO.class);
							try {
								OpenAiChatOptions openAiChatOptions = buildProxyChatOptions(modelVO, getMetadata(agentVOHolder.promptVO));
								ChatModel chatModelNew = createModel(nacosOptions, modelVO, openAiChatOptions);
								replaceModel(chatClient, chatModelNew, openAiChatOptions);
								agentVOHolder.setObservationMetadataAwareOptions((ObservationMetadataAwareOptions) openAiChatOptions);
							}
							catch (Exception e) {
								throw new RuntimeException(e);
							}
						}
					});
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}


	private OpenAiChatOptions buildProxyChatOptions(ModelVO model, Map<String, String> metadata) {
		OpenAiChatOptions.Builder chatOptionsBuilder = OpenAiChatOptions.builder();
		if (model.getTemperature() != null) {
			chatOptionsBuilder.temperature(Double.parseDouble(model.getTemperature()));
		}
		if (model.getMaxTokens() != null) {
			chatOptionsBuilder.maxTokens(Integer.parseInt(model.getMaxTokens()));
		}
		chatOptionsBuilder.internalToolExecutionEnabled(false);
		OpenAiChatOptions openaiChatOptions = chatOptionsBuilder
				.model(model.getModel())
				.build();
		return (OpenAiChatOptions) ChatOptionsProxy.createProxy(openaiChatOptions, metadata);

	}

	private static ChatModel createModel(NacosOptions nacosOptions, ModelVO model, OpenAiChatOptions openAiChatOptions) {

		OpenAiApi openAiApi = OpenAiApi.builder()
				.apiKey(model.getApiKey()).baseUrl(model.getBaseUrl())
				.build();

		OpenAiChatModel.Builder builder = OpenAiChatModel.builder().defaultOptions(openAiChatOptions)
				.openAiApi(openAiApi);
		//inject observation config.
		ObservationConfiguration observationConfiguration = nacosOptions.getObservationConfiguration();
		if (observationConfiguration != null) {
			if (observationConfiguration.getToolCallingManager() != null) {
				builder.toolCallingManager(observationConfiguration.getToolCallingManager());
			}
			if (observationConfiguration.getObservationRegistry() != null) {
				builder.observationRegistry(observationConfiguration.getObservationRegistry());
			}
		}

		OpenAiChatModel openAiChatModel = builder.build();
		if (observationConfiguration != null && observationConfiguration.getChatModelObservationConvention() != null) {
			openAiChatModel.setObservationConvention(observationConfiguration
					.getChatModelObservationConvention());
		}

		return openAiChatModel;
	}

}

/**
 * prompt listener
 * 1. update instruction of react agent
 * 2. update prompt key and version of observation meta.
 */
class PromptListener extends AbstractListener {

	private final NacosContextHolder nacosContextHolder;

	private final ReactAgent reactAgent;

	public PromptListener(NacosContextHolder nacosContextHolder, ReactAgent reactAgent) {
		this.reactAgent = reactAgent;
		this.nacosContextHolder = nacosContextHolder;
	}

	@Override
	public void receiveConfigInfo(String configInfo) {
		PromptVO promptVO = JSON.parseObject(configInfo, PromptVO.class);

		if (promptVO != null && promptVO.getTemplate() != null) {
			nacosContextHolder.getObservationMetadataAwareOptions().getObservationMetadata()
					.putAll(getMetadata(promptVO));
			reactAgent.setSystemPrompt(promptVO.getTemplate());
		}

	}
}


class AgentBaseListener extends AbstractListener {

	String currentPromptKey;

	private NacosOptions nacosOptions;

	private NacosContextHolder nacosContextHolder;

	public AgentBaseListener(NacosOptions nacosOptions, String initPromptKey, NacosContextHolder nacosContextHolder) {
		this.currentPromptKey = initPromptKey;
		this.nacosContextHolder = nacosContextHolder;
		this.nacosOptions = nacosOptions;
	}

	@Override
	public void receiveConfigInfo(String configInfo) {
		if (StringUtils.isBlank(configInfo)) {
			return;
		}

		AgentVO agentVO = JSON.parseObject(configInfo, AgentVO.class);
		String newPromptKey = agentVO.getPromptKey();
		if (StringUtils.isBlank(newPromptKey) || newPromptKey.equals(currentPromptKey)) {
			return;
		}
		//prompt key changed
		try {
			PromptVO newPromptVO = getPromptByKey(nacosOptions, newPromptKey);
			nacosContextHolder.getObservationMetadataAwareOptions().getObservationMetadata()
					.putAll(getMetadata(newPromptVO));
			nacosContextHolder.getReactAgent().setSystemPrompt(newPromptVO.getTemplate());

			NacosReactAgentBuilder.registerPromptListener(nacosOptions, nacosContextHolder, newPromptKey, nacosContextHolder.getReactAgent());
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		if (nacosContextHolder.getPromptListeners().containsKey(currentPromptKey)) {
			Listener listener = nacosContextHolder.getPromptListeners().remove(currentPromptKey);
			String dataId = (nacosOptions.isPromptEncrypted() ? "cipher-kms-aes-256-" : "") + String.format("prompt-%s.json", currentPromptKey);
			nacosOptions.getNacosConfigService().removeListener(dataId,
					"nacos-ai-meta", listener);
		}

		currentPromptKey = newPromptKey;
	}
}

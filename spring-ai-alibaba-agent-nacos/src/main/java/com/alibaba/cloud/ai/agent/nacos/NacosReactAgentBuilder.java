/*
 * Copyright 2024-2025 the original author or authors.
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

import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.agent.nacos.utils.CglibProxyFactory;
import com.alibaba.cloud.ai.agent.nacos.vo.AgentVO;
import com.alibaba.cloud.ai.agent.nacos.vo.McpServersVO;
import com.alibaba.cloud.ai.agent.nacos.vo.ModelVO;
import com.alibaba.cloud.ai.agent.nacos.vo.PromptVO;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.node.LlmNode;
import com.alibaba.cloud.ai.graph.node.ToolNode;
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

import static com.alibaba.cloud.ai.agent.nacos.NacosAgentPromptBuilder.getMetadata;
import static com.alibaba.cloud.ai.agent.nacos.NacosMcpToolsInjector.convert;
import static com.alibaba.cloud.ai.agent.nacos.NacosModelInjector.replaceModel;
import static com.alibaba.cloud.ai.agent.nacos.NacosPromptInjector.getPromptByKey;

public class NacosReactAgentBuilder extends NacosAgentPromptBuilder {

	NacosContextHolder agentVOHolder = new NacosContextHolder();
	private NacosOptions nacosOptions;

	public NacosReactAgentBuilder nacosOptions(NacosOptions nacosOptions) {
		this.nacosOptions = nacosOptions;
		return this;
	}

	@Override
	public ReactAgent build() throws GraphStateException {
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
		this.instruction = promptVO.getTemplate();

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
		ObservationConfigration observationConfigration = nacosOptions.getObservationConfigration();
		if (observationConfigration == null) {
			clientBuilder = ChatClient.builder(model);
		}
		else {
			clientBuilder = ChatClient.builder(model, observationConfigration.getObservationRegistry() == null ? ObservationRegistry.NOOP : observationConfigration.getObservationRegistry(), nacosOptions.getObservationConfigration()
					.getChatClientObservationConvention());
		}

		clientBuilder.defaultOptions(chatOptions);
		clientBuilder.defaultSystem(instruction);
		chatClient = clientBuilder.build();

		//6.load mcp servers
		McpServersVO mcpServersVO = NacosMcpToolsInjector.getMcpServersVO(nacosOptions);
		agentVOHolder.setMcpServersVO(mcpServersVO);

		this.tools = convert(nacosOptions, mcpServersVO);

		//7. build tools
		LlmNode.Builder llmNodeBuilder = LlmNode.builder().stream(true).chatClient(chatClient)
				.messagesKey(this.inputKey);
		if (outputKey != null && !outputKey.isEmpty()) {
			llmNodeBuilder.outputKey(outputKey);
		}
		if (CollectionUtils.isNotEmpty(tools)) {
			llmNodeBuilder.toolCallbacks(tools);
		}
		LlmNode llmNode = llmNodeBuilder.build();

		ToolNode toolNode = null;
		if (resolver != null) {
			toolNode = ToolNode.builder().toolCallbackResolver(resolver).build();
		}
		else if (tools != null) {
			toolNode = ToolNode.builder().toolCallbacks(tools).build();
		}
		else {
			toolNode = ToolNode.builder().build();
		}

		// register listeners.

		//register model  listener
		registerModelListener(chatClient, nacosOptions);
		this.description = agentVO.getDescription();

		//register mcp tools
		registryMcpServerListener(llmNode, toolNode, nacosOptions);
		ReactAgent reactAgent = new ReactAgent(llmNode, toolNode, this);
		agentVOHolder.setReactAgent(reactAgent);
		//register agent base and prompt
		registerAgentWithPrompt(nacosOptions, agentVO, agentVOHolder, reactAgent);
		return reactAgent;
	}

	private void registryMcpServerListener(LlmNode llmNode, ToolNode toolNode, NacosOptions nacosOptions) {

		try {
			nacosOptions.getNacosConfigService()
					.addListener("mcp-servers.json", "ai-agent-" + nacosOptions.getAgentName(), new AbstractListener() {
						@Override
						public void receiveConfigInfo(String configInfo) {
							McpServersVO mcpServersVO = JSON.parseObject(configInfo, McpServersVO.class);
							List<ToolCallback> toolCallbacks = convert(nacosOptions, mcpServersVO);
							if (toolCallbacks != null) {
								toolNode.setToolCallbacks(toolCallbacks);
								llmNode.setToolCallbacks(toolCallbacks);
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
			nacosConfigService.addListener("agent-base.json", "ai-agent-" + nacosOptions.getAgentName(),
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
	 * @param nacosOptions
	 * @param promptKey
	 */
	static void registerPromptListener(NacosOptions nacosOptions, NacosContextHolder nacosContextHolder,
			String promptKey, ReactAgent reactAgent) {
		try {
			PromptListener promptListener = new PromptListener(nacosContextHolder, reactAgent);
			nacosOptions.getNacosConfigService().addListener(String.format("prompt-%s.json", promptKey),
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
			String dataIdT = String.format(nacosOptions.isModelConfigEncrypted() ? "cipher-kms-aes-256-model.json" : "model.json", agentName);
			nacosOptions.getNacosConfigService()
					.addListener(dataIdT, "ai-agent-" + agentName, new AbstractListener() {
						@Override
						public void receiveConfigInfo(String configInfo) {
							ModelVO modelVO = JSON.parseObject(configInfo, ModelVO.class);
							try {
								OpenAiChatOptions openAiChatOptions = buildProxyChatOptions(modelVO, getMetadata(agentVOHolder.promptVO));
								ChatModel chatModelNew = createModel(nacosOptions, modelVO, openAiChatOptions);
								replaceModel(chatClient, chatModelNew);
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
		return (OpenAiChatOptions) CglibProxyFactory.createProxy(openaiChatOptions, metadata);

	}

	private static ChatModel createModel(NacosOptions nacosOptions, ModelVO model, OpenAiChatOptions openAiChatOptions) {

		OpenAiApi openAiApi = OpenAiApi.builder()
				.apiKey(model.getApiKey()).baseUrl(model.getBaseUrl())
				.build();

		OpenAiChatModel.Builder builder = OpenAiChatModel.builder().defaultOptions(openAiChatOptions)
				.openAiApi(openAiApi);
		//inject observation config.
		ObservationConfigration observationConfigration = nacosOptions.getObservationConfigration();
		if (observationConfigration != null) {
			if (observationConfigration.getToolCallingManager() != null) {
				builder.toolCallingManager(observationConfigration.getToolCallingManager());
			}
			if (observationConfigration.getObservationRegistry() != null) {
				builder.observationRegistry(observationConfigration.getObservationRegistry());
			}
		}

		OpenAiChatModel openAiChatModel = builder.build();
		if (observationConfigration != null && observationConfigration.getChatModelObservationConvention() != null) {
			openAiChatModel.setObservationConvention(observationConfigration
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
			reactAgent.setInstruction(promptVO.getTemplate());
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
			nacosContextHolder.getReactAgent().setInstruction(newPromptVO.getTemplate());

			NacosReactAgentBuilder.registerPromptListener(nacosOptions, nacosContextHolder, newPromptKey, nacosContextHolder.getReactAgent());
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		if (nacosContextHolder.getPromptListeners().containsKey(currentPromptKey)) {
			Listener listener = nacosContextHolder.getPromptListeners().remove(currentPromptKey);
			nacosOptions.getNacosConfigService().removeListener(String.format("prompt-%s.json", currentPromptKey),
					"nacos-ai-meta", listener);
		}

		currentPromptKey = newPromptKey;
	}
}

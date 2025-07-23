/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.example.deepresearch.agents;

import com.alibaba.cloud.ai.example.deepresearch.config.PythonCoderProperties;
import com.alibaba.cloud.ai.example.deepresearch.model.mutiagent.AgentType;
import com.alibaba.cloud.ai.example.deepresearch.tool.PlannerTool;
import com.alibaba.cloud.ai.example.deepresearch.tool.PythonReplTool;
import com.alibaba.cloud.ai.example.deepresearch.util.Multiagent.AgentPromptTemplateUtil;
import com.alibaba.cloud.ai.example.deepresearch.util.ResourceUtil;
import com.alibaba.cloud.ai.toolcalling.jinacrawler.JinaCrawlerConstants;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.Map;

@Configuration
public class AgentsConfiguration {

	@Value("classpath:prompts/researcher.md")
	private Resource researcherPrompt;

	@Value("classpath:prompts/coder.md")
	private Resource coderPrompt;

	@Value("classpath:prompts/buildInteractiveHtmlPrompt.md")
	private Resource interactionPrompt;

	@Value("classpath:prompts/reflection.md")
	private Resource reflectionPrompt;

	@Value("classpath:prompts/reporter.md")
	private Resource reporterPrompt;

	@Autowired
	private ApplicationContext context;

	@Autowired(required = false)
	private Map<String, AsyncMcpToolCallbackProvider> agent2AsyncMcpToolCallbackProvider;

	@Autowired(required = false)
	private Map<String, SyncMcpToolCallbackProvider> agent2SyncMcpToolCallbackProvider;

	/**
	 * Return the tool name array that have corresponding beans.
	 */
	private String[] getAvailableTools(String... toolNames) {
		return Arrays.stream(toolNames).filter(context::containsBean).toArray(String[]::new);
	}

	/**
	 * 获取指定代理的MCP工具回调, 这边我把mcp创建的部分改为在结点处进行动态创建和加载，所以会返回空数组
	 */
	private ToolCallback[] getMcpToolCallbacks(String agentName) {
		if (CollectionUtils.isEmpty(agent2SyncMcpToolCallbackProvider)
				&& CollectionUtils.isEmpty(agent2AsyncMcpToolCallbackProvider)) {
			return new ToolCallback[0];
		}

		if (!CollectionUtils.isEmpty(agent2SyncMcpToolCallbackProvider)) {
			SyncMcpToolCallbackProvider toolCallbackProvider = agent2SyncMcpToolCallbackProvider.get(agentName);
			if (toolCallbackProvider != null) {
				return toolCallbackProvider.getToolCallbacks();
			}
		}

		if (!CollectionUtils.isEmpty(agent2AsyncMcpToolCallbackProvider)) {
			AsyncMcpToolCallbackProvider toolCallbackProvider = agent2AsyncMcpToolCallbackProvider.get(agentName);
			if (toolCallbackProvider != null) {
				return toolCallbackProvider.getToolCallbacks();
			}
		}

		// 如果没有找到有效的工具回调提供者，返回空数组
		return new ToolCallback[0];
	}

	/**
	 * 提取MutiAgent配置
	 */
	private ChatClient.Builder configureAgentBuilder(ChatClient.Builder builder, String agentName,
			AgentType agentType) {
		return builder.defaultSystem(AgentPromptTemplateUtil.buildCompletePrompt(agentType))
			.defaultToolCallbacks(getMcpToolCallbacks(agentName));
	}

	/**
	 * Create Research Agent ChatClient Bean
	 * @param researchChatClientBuilder ChatClientBuilder McpAsyncClient and the locally
	 * configure ToolCallbackProviders.
	 * @return ChatClient
	 */
	@Bean
	public ChatClient researchAgent(ChatClient.Builder researchChatClientBuilder) {
		ToolCallback[] mcpCallbacks = getMcpToolCallbacks("researchAgent");

		var builder = researchChatClientBuilder.defaultSystem(ResourceUtil.loadResourceAsString(researcherPrompt));
		var toolArray = this.getAvailableTools(JinaCrawlerConstants.TOOL_NAME);
		if (toolArray.length > 0) {
			builder = builder.defaultToolNames(toolArray);
		}
		return builder.defaultToolCallbacks(mcpCallbacks).build();
	}

	/**
	 * Create Coder Agent ChatClient Bean
	 * @param coderChatClientBuilder ChatClientBuilder McpAsyncClient and the locally
	 * configure ToolCallbackProviders.
	 * @return ChatClient
	 */
	@Bean
	public ChatClient coderAgent(ChatClient.Builder coderChatClientBuilder, PythonCoderProperties coderProperties) {
		ToolCallback[] mcpCallbacks = getMcpToolCallbacks("coderAgent");

		return coderChatClientBuilder.defaultSystem(ResourceUtil.loadResourceAsString(coderPrompt))
			.defaultTools(new PythonReplTool(coderProperties))
			.defaultToolCallbacks(mcpCallbacks)
			.build();
	}

	@Bean
	public ChatClient coordinatorAgent(ChatClient.Builder coordinatorChatClientBuilder, PlannerTool plannerTool) {
		return coordinatorChatClientBuilder
			.defaultOptions(ToolCallingChatOptions.builder()
				.internalToolExecutionEnabled(false) // 禁用内部工具执行
				.build())
			// 当前CoordinatorNode节点只绑定一个计划工具
			.defaultTools(plannerTool)
			.build();
	}

	@Bean
	public ChatClient plannerAgent(ChatClient.Builder plannerChatClientBuilder) {
		return plannerChatClientBuilder.build();
	}

	@Bean
	public ChatClient reporterAgent(ChatClient.Builder reporterChatClientBuilder) {
		return reporterChatClientBuilder.defaultSystem(ResourceUtil.loadResourceAsString(reporterPrompt)).build();
	}

	@Bean
	public ChatClient interactionAgent(ChatClient.Builder interactionChatClientBuilder) {
		return interactionChatClientBuilder.defaultSystem(ResourceUtil.loadResourceAsString(interactionPrompt)).build();
	}

	@Bean
	public ChatClient rewriteAndMultiQueryAgent(ChatClient.Builder rewriteAndMultiQueryChatClientBuilder) {
		return rewriteAndMultiQueryChatClientBuilder.build();
	}

	@Bean
	public ChatClient infoCheckAgent(ChatClient.Builder infoCheckChatClientBuilder) {
		return infoCheckChatClientBuilder.build();
	}

	@Bean
	public ChatClient reflectionAgent(ChatClient.Builder reflectionChatClientBuilder) {
		return reflectionChatClientBuilder.defaultSystem(ResourceUtil.loadResourceAsString(reflectionPrompt)).build();
	}

	@Bean
	@ConditionalOnProperty(name = "spring.ai.alibaba.deepresearch.smart-agents.enabled", havingValue = "true",
			matchIfMissing = false)
	public ChatClient academicResearchAgent(ChatClient.Builder academicResearchChatClientBuilder) {
		return configureAgentBuilder(academicResearchChatClientBuilder, "academicResearchAgent",
				AgentType.ACADEMIC_RESEARCH)
			.build();
	}

	@Bean
	@ConditionalOnProperty(name = "spring.ai.alibaba.deepresearch.smart-agents.enabled", havingValue = "true",
			matchIfMissing = false)
	public ChatClient lifestyleTravelAgent(ChatClient.Builder lifestyleTravelChatClientBuilder) {
		return configureAgentBuilder(lifestyleTravelChatClientBuilder, "lifestyleTravelAgent",
				AgentType.LIFESTYLE_TRAVEL)
			.build();
	}

	@Bean
	@ConditionalOnProperty(name = "spring.ai.alibaba.deepresearch.smart-agents.enabled", havingValue = "true",
			matchIfMissing = false)
	public ChatClient encyclopediaAgent(ChatClient.Builder encyclopediaChatClientBuilder) {
		return configureAgentBuilder(encyclopediaChatClientBuilder, "encyclopediaAgent", AgentType.ENCYCLOPEDIA)
			.build();
	}

	@Bean
	@ConditionalOnProperty(name = "spring.ai.alibaba.deepresearch.smart-agents.enabled", havingValue = "true",
			matchIfMissing = false)
	public ChatClient dataAnalysisAgent(ChatClient.Builder dataAnalysisChatClientBuilder) {
		return configureAgentBuilder(dataAnalysisChatClientBuilder, "dataAnalysisAgent", AgentType.DATA_ANALYSIS)
			.build();
	}

}

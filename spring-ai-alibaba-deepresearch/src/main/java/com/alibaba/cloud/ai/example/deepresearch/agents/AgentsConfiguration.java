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

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.example.deepresearch.config.PythonCoderProperties;
import com.alibaba.cloud.ai.example.deepresearch.tool.McpClientToolCallbackProvider;
import com.alibaba.cloud.ai.example.deepresearch.tool.PlannerTool;
import com.alibaba.cloud.ai.example.deepresearch.tool.PythonReplTool;
import com.alibaba.cloud.ai.example.deepresearch.util.ResourceUtil;
import com.alibaba.cloud.ai.toolcalling.jinacrawler.JinaCrawlerConstants;
import com.alibaba.cloud.ai.toolcalling.tavily.TavilySearchConstants;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Set;

@Configuration
public class AgentsConfiguration {

	@Value("classpath:prompts/researcher.md")
	private Resource researcherPrompt;

	@Value("classpath:prompts/coder.md")
	private Resource coderPrompt;

	@Value("classpath:agents-config.json")
	private Resource agentsConfig;

	private final ApplicationContext context;

	private JSONObject configJson;

	public AgentsConfiguration(ApplicationContext context) {
		this.context = context;
	}

	@PostConstruct
	public void afterPropertiesSet() throws IOException {
		String configContent = agentsConfig.getContentAsString(Charset.defaultCharset());
		this.configJson = JSON.parseObject(configContent);
	}

	private String getModelName(String agentName) {
		return configJson.getJSONObject("models").getString(agentName);
	}

	/**
	 * Return the tool name array that have corresponding beans.
	 */
	private String[] getAvailableTools(String... toolNames) {
		return Arrays.stream(toolNames).filter(context::containsBean).toArray(String[]::new);
	}

	/**
	 * Create Research Agent ChatClient Bean
	 * @param chatClientBuilder ChatClientBuilder McpAsyncClient and the locally configure
	 * ToolCallbackProviders.
	 * @return ChatClient
	 */
	@Bean
	public ChatClient researchAgent(ChatClient.Builder chatClientBuilder,
			McpClientToolCallbackProvider mcpClientToolCallbackProvider) {
		Set<ToolCallback> defineCallback = mcpClientToolCallbackProvider.findToolCallbacks("researchAgent");
		return chatClientBuilder.defaultSystem(ResourceUtil.loadResourceAsString(researcherPrompt))
			.defaultOptions(DashScopeChatOptions.builder().withModel(getModelName("researchAgent")).build())
			.defaultToolNames(this.getAvailableTools(TavilySearchConstants.TOOL_NAME, JinaCrawlerConstants.TOOL_NAME))
			.defaultToolCallbacks(defineCallback.toArray(ToolCallback[]::new))
			.build();
	}

	/**
	 * Create Coder Agent ChatClient Bean
	 * @param chatClientBuilder ChatClientBuilder McpAsyncClient and the locally configure
	 * ToolCallbackProviders.
	 * @return ChatClient
	 */
	@Bean
	public ChatClient coderAgent(ChatClient.Builder chatClientBuilder, PythonCoderProperties coderProperties,
			McpClientToolCallbackProvider mcpClientToolCallbackProvider) {
		Set<ToolCallback> defineCallback = mcpClientToolCallbackProvider.findToolCallbacks("coderAgent");
		return chatClientBuilder.defaultSystem(ResourceUtil.loadResourceAsString(coderPrompt))
			.defaultOptions(DashScopeChatOptions.builder().withModel(getModelName("coderAgent")).build())
			.defaultTools(new PythonReplTool(coderProperties))
			.defaultToolCallbacks(defineCallback.toArray(ToolCallback[]::new))
			.build();
	}

	@Bean
	public ChatClient coordinatorAgent(ChatClient.Builder chatClientBuilder) {
		return chatClientBuilder
			.defaultOptions(ToolCallingChatOptions.builder()
				.model(getModelName("coordinatorAgent"))
				.internalToolExecutionEnabled(false) // 禁用内部工具执行
				.build())
			// 当前CoordinatorNode节点只绑定一个计划工具
			.defaultTools(new PlannerTool())
			.build();
	}

	@Bean
	public ChatClient plannerAgent(ChatClient.Builder chatClientBuilder) {
		return chatClientBuilder
			.defaultOptions(DashScopeChatOptions.builder().withModel(getModelName("plannerAgent")).build())
			.build();
	}

	@Bean
	public ChatClient reporterAgent(ChatClient.Builder chatClientBuilder) {
		return chatClientBuilder
			.defaultOptions(DashScopeChatOptions.builder().withModel(getModelName("reporterAgent")).build())
			.build();
	}

}

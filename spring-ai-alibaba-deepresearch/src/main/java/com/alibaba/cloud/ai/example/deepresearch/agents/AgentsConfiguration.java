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

import com.alibaba.cloud.ai.example.deepresearch.tool.PythonReplTool;
import com.alibaba.cloud.ai.toolcalling.tavily.TavilySearchProperties;
import lombok.SneakyThrows;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.tool.resolution.SpringBeanToolCallbackResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;

import java.nio.charset.Charset;

// todo 该类待调整

@Configuration
public class AgentsConfiguration {

	@Autowired
	private PythonReplTool pythonReplTool;

	/**
	 * TODO The prompt is beta.
	 */
	@Value("classpath:prompts/bginvestigation.md")
	private Resource bginvestigationPrompt;

	@Value("classpath:prompts/researcher.md")
	private Resource researcherPrompt;

	@Value("classpath:prompts/coder.md")
	private Resource coderPrompt;

	@Value("classpath:prompts/reporter.md")
	private Resource reporterPrompt;

	// @Bean
	// public ToolCallbackProvider webSearchToolCallbackProvider() {
	// return MethodToolCallbackProvider.builder().toolObjects(webSearchTool).build();
	// }

	@Bean
	@ConditionalOnProperty(prefix = TavilySearchProperties.PREFIX, name = "enabled", havingValue = "true")
	public ToolCallbackProvider tavilySearchServiceCallbackProvider(GenericApplicationContext applicationContext) {
		SpringBeanToolCallbackResolver springBeanToolCallbackResolver = SpringBeanToolCallbackResolver.builder()
			.applicationContext(applicationContext)
			.build();
		ToolCallback tavilySearch = springBeanToolCallbackResolver.resolve("tavilySearch");
		return ToolCallbackProvider.from(tavilySearch);
	}

	@Bean
	public ToolCallbackProvider pythonReplToolCallbackProvider() {
		return MethodToolCallbackProvider.builder().toolObjects(pythonReplTool).build();
	}

	@SneakyThrows
	@Bean
	public ChatClient backgroundInvestigationAgent(ChatClient.Builder chatClientBuilder) {
		return chatClientBuilder.defaultSystem(bginvestigationPrompt.getContentAsString(Charset.defaultCharset()))
			.build();
	}

	/**
	 * Create Research Agent ChatClient Bean
	 * @param chatClientBuilder ChatClientBuilder McpAsyncClient and the locally configure
	 * ToolCallbackProviders.
	 * @return ChatClient
	 */
	@SneakyThrows
	@Bean
	public ChatClient researchAgent(ChatClient.Builder chatClientBuilder) {
		return chatClientBuilder.defaultSystem(researcherPrompt.getContentAsString(Charset.defaultCharset())).build();
	}

	/**
	 * Create Coder Agent ChatClient Bean
	 * @param chatClientBuilder ChatClientBuilder McpAsyncClient and the locally configure
	 * ToolCallbackProviders.
	 * @return ChatClient
	 */
	@SneakyThrows
	@Bean
	public ChatClient coderAgent(ChatClient.Builder chatClientBuilder) {
		return chatClientBuilder.defaultSystem(coderPrompt.getContentAsString(Charset.defaultCharset())).build();
	}

	/**
	 * Create Reporter Agent ChatClient Bean
	 * @param chatClientBuilder ChatClientBuilder McpAsyncClient and the locally configure
	 * ToolCallbackProviders.
	 * @return ChatClient
	 */
	@SneakyThrows
	@Bean
	public ChatClient reporterAgent(ChatClient.Builder chatClientBuilder) {
		return chatClientBuilder.defaultSystem(reporterPrompt.getContentAsString(Charset.defaultCharset())).build();
	}

}

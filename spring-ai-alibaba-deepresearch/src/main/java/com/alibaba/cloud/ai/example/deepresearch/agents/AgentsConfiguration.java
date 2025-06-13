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
import com.alibaba.cloud.ai.example.deepresearch.tool.PythonReplTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

@Configuration
public class AgentsConfiguration {

	@Value("classpath:prompts/researcher.md")
	private Resource researcherPrompt;

	@Value("classpath:prompts/coder.md")
	private Resource coderPrompt;

	/**
	 * Create Research Agent ChatClient Bean
	 * @param chatClientBuilder ChatClientBuilder McpAsyncClient and the locally configure
	 * ToolCallbackProviders.
	 * @return ChatClient
	 */
	@Bean
	public ChatClient researchAgent(ChatClient.Builder chatClientBuilder) {
		Assert.notNull(researcherPrompt, "researcherPrompt cannot be null");
		try (InputStream inputStream = researcherPrompt.getInputStream()) {
			var template = StreamUtils.copyToString(inputStream, Charset.defaultCharset());
			Assert.hasText(template, "template cannot be null or empty");
			return chatClientBuilder.defaultSystem(template)
					.defaultToolNames("tavilySearch")
					// .defaultToolNames("tavilySearch", "firecrawlFunction") todo 待调整
					.build();
		}
		catch (IOException ex) {
			throw new RuntimeException("Failed to read resource", ex);
		}
	}

	/**
	 * Create Coder Agent ChatClient Bean
	 * @param chatClientBuilder ChatClientBuilder McpAsyncClient and the locally configure
	 * ToolCallbackProviders.
	 * @return ChatClient
	 */
	@Bean
	public ChatClient coderAgent(ChatClient.Builder chatClientBuilder, PythonCoderProperties coderProperties) {
		Assert.notNull(coderPrompt, "coderPrompt cannot be null");
		try (InputStream inputStream = coderPrompt.getInputStream()) {
			var template = StreamUtils.copyToString(inputStream, Charset.defaultCharset());
			Assert.hasText(template, "template cannot be null or empty");
			return chatClientBuilder.defaultSystem(template)
					.defaultTools(new PythonReplTool(coderProperties))
					.build();
		}
		catch (IOException ex) {
			throw new RuntimeException("Failed to read resource", ex);
		}
	}

}

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
package com.alibaba.cloud.ai.dashscope.chat;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.Assumptions;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for DashScope Chat functionality. These tests will only run if
 * AI_DASHSCOPE_API_KEY environment variable is set.
 *
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class DashScopeChatIT {

	// Test constants
	private static final String TEST_MODEL = "qwen-turbo";

	private static final String TEST_PROMPT = "你好，请介绍一下你自己。";

	private static final String API_KEY_ENV = "AI_DASHSCOPE_API_KEY";

	private String apiKey;

	@BeforeEach
	void setUp() {
		// Get API key from environment variable
		apiKey = System.getenv(API_KEY_ENV);
		// Skip tests if API key is not set
		Assumptions.assumeTrue(apiKey != null && !apiKey.trim().isEmpty(),
				"Skipping tests because " + API_KEY_ENV + " environment variable is not set");
	}

	/**
	 * Test basic chat functionality with simple text prompt.
	 */
	@Test
	void testBasicChat() {
		// Create real API client with API key from environment
		DashScopeApi realApi = new DashScopeApi(apiKey);

		// Create chat model with default options
		DashScopeChatOptions options = DashScopeChatOptions.builder().withModel(TEST_MODEL).build();
		DashScopeChatModel chatModel = new DashScopeChatModel(realApi, options);

		// Create prompt with user message
		UserMessage message = new UserMessage(TEST_PROMPT);
		Prompt prompt = new Prompt(message);

		// Call the chat model
		Generation response = chatModel.call(prompt).getResult();

		// Verify response
		assertThat(response).isNotNull();
		assertThat(response.getOutput().getText()).isNotEmpty();
		System.out.println("Chat Response: " + response.getOutput().getText());
	}

	/**
	 * Test streaming chat functionality.
	 */
	@Test
	void testStreamChat() {
		// Create real API client with API key from environment
		DashScopeApi realApi = new DashScopeApi(apiKey);

		// Create chat model with default options
		DashScopeChatOptions options = DashScopeChatOptions.builder().withModel(TEST_MODEL).build();
		DashScopeChatModel chatModel = new DashScopeChatModel(realApi, options);

		// Create prompt with user message
		UserMessage message = new UserMessage(TEST_PROMPT);
		Prompt prompt = new Prompt(message);

		// Call the streaming API and collect responses
		StringBuilder responseBuilder = new StringBuilder();
		Flux<Generation> responseFlux = chatModel.stream(prompt).map(ChatResponse::getResult);

		responseFlux.doOnNext(generation -> {
			String content = generation.getOutput().getText();
			System.out.println("Streaming chunk: " + content);
			responseBuilder.append(content);
		}).blockLast();

		// Verify final response
		String finalResponse = responseBuilder.toString();
		assertThat(finalResponse).isNotEmpty();
		System.out.println("Final streaming response: " + finalResponse);
	}

}

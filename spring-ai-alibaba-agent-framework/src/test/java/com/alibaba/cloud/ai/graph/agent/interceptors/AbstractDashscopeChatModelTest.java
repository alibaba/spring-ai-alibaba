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
package com.alibaba.cloud.ai.graph.agent.interceptors;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;

/**
 * Shared base class for creating a DashScope ChatModel through the OpenAI-compatible API.
 * Subclasses can use the initialized {@link #chatModel} field directly.
 *
 * <p>Set the following environment variables before running these tests:
 * <ul>
 *   <li>{@code AI_DASHSCOPE_API_KEY}  - DashScope API key (required; tests are skipped when absent)</li>
 *   <li>{@code AI_DASHSCOPE_BASE_URL} - API base URL (optional; defaults to DashScope public compatible endpoint)</li>
 *   <li>{@code AI_DASHSCOPE_MODEL}    - model name (optional; defaults to qwen-plus)</li>
 * </ul>
 */
abstract class AbstractDashscopeChatModelTest {

	// Base URL only. Spring AI's OpenAI client appends the chat-completions path.
	private static final String DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode";

	private static final String DEFAULT_MODEL = "qwen-plus";

	protected ChatModel chatModel;

	@BeforeEach
	void initChatModel() {
		final String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
		Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(),
				"Skipping: AI_DASHSCOPE_API_KEY environment variable is not set");

		final String baseUrl = System.getenv().getOrDefault("AI_DASHSCOPE_BASE_URL", DEFAULT_BASE_URL);
		final String model = System.getenv().getOrDefault("AI_DASHSCOPE_MODEL", DEFAULT_MODEL);

		chatModel = OpenAiChatModel.builder()
				.options(OpenAiChatOptions.builder()
						.baseUrl(baseUrl)
						.apiKey(apiKey)
						.model(model)
						.build())
				.build();
	}
}

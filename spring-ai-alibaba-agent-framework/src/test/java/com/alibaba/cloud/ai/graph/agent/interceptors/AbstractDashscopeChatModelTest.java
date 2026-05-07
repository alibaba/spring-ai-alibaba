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
import org.springframework.ai.openai.api.OpenAiApi;

/**
 * 共享 base class：通过 OpenAI 兼容接口初始化 DashScope ChatModel。
 * 子类用 {@link #chatModel} 字段直接拿到注入好的实例。
 *
 * <p>运行前需设置以下环境变量：
 * <ul>
 *   <li>{@code AI_DASHSCOPE_API_KEY}  - DashScope API 密钥（必填，缺失时测试自动跳过）</li>
 *   <li>{@code AI_DASHSCOPE_BASE_URL} - API 地址（可选，默认 DashScope 公网兼容端点）</li>
 *   <li>{@code AI_DASHSCOPE_MODEL}    - 模型名称（可选，默认 qwen-plus）</li>
 * </ul>
 */
abstract class AbstractDashscopeChatModelTest {

	private static final String DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";

	private static final String DEFAULT_MODEL = "qwen-plus";

	protected ChatModel chatModel;

	@BeforeEach
	void initChatModel() {
		final String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
		Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(),
				"Skipping: AI_DASHSCOPE_API_KEY environment variable is not set");

		final String baseUrl = System.getenv().getOrDefault("AI_DASHSCOPE_BASE_URL", DEFAULT_BASE_URL);
		final String model = System.getenv().getOrDefault("AI_DASHSCOPE_MODEL", DEFAULT_MODEL);

		OpenAiApi openAiApi = OpenAiApi.builder()
				.baseUrl(baseUrl)
				.apiKey(apiKey)
				.build();
		chatModel = OpenAiChatModel.builder()
				.openAiApi(openAiApi)
				.defaultOptions(OpenAiChatOptions.builder().model(model).build())
				.build();
	}
}

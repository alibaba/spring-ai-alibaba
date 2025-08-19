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
package com.alibaba.cloud.ai.util;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

/**
 * AI 模型配置类
 *
 * <p>
 * 本配置类提供了 ChatModel 和 EmbeddingModel 的配置。
 *
 * <p>
 * EmbeddingModel 选择策略：
 * <ul>
 * <li>优先使用 DashScope EmbeddingModel（当配置了 spring.ai.dashscope.api-key）</li>
 * <li>备选使用 OpenAI EmbeddingModel（当未配置 DashScope API Key 时）</li>
 * </ul>
 *
 * <p>
 * 为了避免与 Spring Boot 自动配置冲突，建议在 application.yml 中设置： <pre>
 * spring:
 *   ai:
 *     openai:
 *       embedding:
 *         enabled: false  # 禁用 OpenAI EmbeddingModel 自动配置
 * </pre>
 *
 * @author Spring AI Alibaba Team
 */
@Configuration
public class AiConfiguration {

	@Value("${spring.ai.openai.api-key}")
	private String openAiApiKey;

	@Value("${spring.ai.openai.base-url:}")
	private String baseUrl;

	@Value("${spring.ai.openai.model:}")
	private String model;

	@Value("${spring.ai.dashscope.api-key:}")
	private String dashScopeApiKey;

	@Value("${spring.ai.dashscope.embedding.model:text-embedding-v2}")
	private String dashScopeEmbeddingModel;

	@Value("${spring.ai.openai.embedding.model:text-embedding-ada-002}")
	private String openAiEmbeddingModel;

	@Value("${spring.ai.openai.embedding.embeddings-path:}")
	private String openAiEmbeddingsPath;

	@Value("${spring.ai.openai.completions-path:}")
	private String openAiCompletionsPath;

	@Bean
	public ChatModel chatModel() {
		OpenAiApi openAiApi = OpenAiApi.builder().apiKey(openAiApiKey).baseUrl(baseUrl).build();
		OpenAiChatOptions openAiChatOptions = OpenAiChatOptions.builder().model(model).temperature(0.7).build();
		return OpenAiChatModel.builder().openAiApi(openAiApi).defaultOptions(openAiChatOptions).build();
	}

	@Bean
	public ChatClient chatClient(@Qualifier("chatModel") ChatModel chatModel) {
		return ChatClient.create(chatModel);
	}

	/**
	 * DashScope EmbeddingModel configuration
	 * <p>
	 * Enabled when spring.ai.dashscope.api-key is configured, highest priority
	 */
	@Bean("embeddingModel")
	@Primary
	@ConditionalOnProperty(name = "spring.ai.dashscope.api-key")
	@ConditionalOnMissingBean(EmbeddingModel.class)
	public EmbeddingModel dashScopeEmbeddingModel() {
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(dashScopeApiKey).build();
		DashScopeEmbeddingOptions options = DashScopeEmbeddingOptions.builder()
			.withModel(dashScopeEmbeddingModel)
			.build();
		return new DashScopeEmbeddingModel(dashScopeApi, MetadataMode.EMBED, options);
	}

	/**
	 * Custom OpenAI EmbeddingModel configuration
	 * <p>
	 * Used when DashScope API Key is not configured
	 * <p>
	 * Use @ConditionalOnMissingBean to avoid conflict with auto-configuration
	 */
	@Bean("embeddingModel")
	@Primary
	@ConditionalOnProperty(name = "spring.ai.dashscope.api-key", havingValue = "", matchIfMissing = true)
	@ConditionalOnMissingBean(EmbeddingModel.class)
	public EmbeddingModel customOpenAiEmbeddingModel() {
		if (!StringUtils.hasText(openAiApiKey)) {
			throw new IllegalStateException(
					"Either spring.ai.dashscope.api-key or spring.ai.openai.api-key must be configured");
		}
		OpenAiApi.Builder builder = OpenAiApi.builder().apiKey(openAiApiKey).baseUrl(baseUrl);
		if (StringUtils.hasText(openAiEmbeddingsPath)) {
			builder.embeddingsPath(openAiEmbeddingsPath);
		}
		if (StringUtils.hasText(openAiCompletionsPath)) {
			builder.completionsPath(openAiCompletionsPath);
		}
		OpenAiApi openAiApi = builder.build();
		OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder().model(openAiEmbeddingModel).build();
		return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, options);
	}

}

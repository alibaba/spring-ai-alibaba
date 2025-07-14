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
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

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

	@Value("${spring.ai.dashscope.api-key:}")
	private String dashScopeApiKey;

	@Value("${spring.ai.dashscope.embedding.model:text-embedding-v2}")
	private String dashScopeEmbeddingModel;

	/**
	 * 复用OpenAiChatModel
	 * @param openAiChatModel
	 * @return
	 */
	@Bean
	public ChatClient chatClient(OpenAiChatModel openAiChatModel) {
		return ChatClient.create(openAiChatModel);
	}

	/**
	 * DashScope EmbeddingModel 配置
	 * <p>
	 * 当配置了 spring.ai.dashscope.api-key 时启用，优先级最高
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
	 * 自定义 OpenAI EmbeddingModel 配置
	 * <p>
	 * 当没有配置 DashScope API Key 时使用
	 * <p>
	 * 使用 @ConditionalOnMissingBean 避免与自动配置冲突
	 */
	@Bean("embeddingModel")
	@Primary
	@ConditionalOnProperty(name = "spring.ai.dashscope.api-key", havingValue = "", matchIfMissing = true)
	@ConditionalOnMissingBean(EmbeddingModel.class)
	public EmbeddingModel customOpenAiEmbeddingModel(OpenAiEmbeddingModel openAiEmbeddingModel) {
		// 复用openAiEmbeddingModel，同时复用OpenAiEmbeddingOptions
		return openAiEmbeddingModel;
	}

}

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

package com.alibaba.cloud.ai.mcp.router.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * EmbeddingModel 配置类 提供默认的 EmbeddingModel 实现，用于开发和测试
 */
@Configuration
public class EmbeddingModelConfig {

	@Value("${spring.ai.dashscope.api-key:default_api_key}")
	private String apiKey;

	@Bean(name = "dashscopeEmbeddingModel")
	@ConditionalOnMissingBean
	public EmbeddingModel dashscopeEmbeddingModel() {
		if (apiKey == null || apiKey.isEmpty()) {
			throw new IllegalArgumentException("Environment variable DASHSCOPE_API_KEY is not set.");
		}
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(apiKey).build();

		return new DashScopeEmbeddingModel(dashScopeApi, MetadataMode.EMBED,
				DashScopeEmbeddingOptions.builder().withModel("text-embedding-v2").build());
	}

}

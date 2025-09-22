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
package com.alibaba.cloud.ai.annotation;

import com.alibaba.cloud.ai.service.simple.AgentVectorStoreManager;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Vector Store Configuration Class Ensures AgentVectorStoreManager is available in
 * management module
 */
@Configuration
public class VectorStoreConfig {

	/**
	 * Create AgentVectorStoreManager Bean Will not be created repeatedly if it already
	 * exists in chat module
	 */
	@Bean
	@ConditionalOnMissingBean
	public AgentVectorStoreManager agentVectorStoreManager(EmbeddingModel embeddingModel) {
		return new AgentVectorStoreManager(embeddingModel);
	}

}

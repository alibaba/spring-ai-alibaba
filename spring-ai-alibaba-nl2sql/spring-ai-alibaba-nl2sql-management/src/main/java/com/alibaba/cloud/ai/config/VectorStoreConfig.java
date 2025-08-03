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
package com.alibaba.cloud.ai.config;

import com.alibaba.cloud.ai.service.simple.AgentVectorStoreManager;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 向量存储配置类 确保AgentVectorStoreManager在管理模块中可用
 */
@Configuration
public class VectorStoreConfig {

	/**
	 * 创建AgentVectorStoreManager Bean 如果chat模块中已经存在，则不会重复创建
	 */
	@Bean
	@ConditionalOnMissingBean
	public AgentVectorStoreManager agentVectorStoreManager(EmbeddingModel embeddingModel) {
		return new AgentVectorStoreManager(embeddingModel);
	}

}

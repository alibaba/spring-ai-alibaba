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
package com.alibaba.cloud.ai.vectorstore.oceanbase;

import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * OceanBase Vector Store Auto Configuration
 *
 * @author xxsc0529
 * @since 2025-04-01
 */
@AutoConfiguration
@ConditionalOnClass({ EmbeddingModel.class, OceanBaseVectorStore.class })
@EnableConfigurationProperties({ OceanBaseVectorStoreProperties.class })
@ConditionalOnProperty(prefix = "spring.ai.vectorstore.oceanbase", name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class OceanBaseVectorStoreAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public DataSource oceanbaseDataSource(OceanBaseVectorStoreProperties properties) {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setUrl(properties.getUrl());
		dataSource.setUsername(properties.getUsername());
		dataSource.setPassword(properties.getPassword());
		return dataSource;
	}

	@Bean
	@ConditionalOnMissingBean(BatchingStrategy.class)
	public BatchingStrategy oceanbaseBatchingStrategy() {
		return new TokenCountBatchingStrategy();
	}

	@Bean
	@ConditionalOnMissingBean
	public OceanBaseVectorStore oceanBaseVectorStore(DataSource dataSource, EmbeddingModel embeddingModel,
			OceanBaseVectorStoreProperties properties, ObjectProvider<ObservationRegistry> observationRegistry,
			ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
			BatchingStrategy batchingStrategy) {

		var builder = OceanBaseVectorStore.builder(properties.getTableName(), dataSource, embeddingModel)
			.batchingStrategy(batchingStrategy)
			.observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
			.customObservationConvention(customObservationConvention.getIfAvailable(() -> null));
		if (properties.getDefaultTopK() >= 0) {
			builder.defaultTopK(properties.getDefaultTopK());
		}

		if (properties.getDefaultSimilarityThreshold() >= 0.0) {
			builder.defaultSimilarityThreshold(properties.getDefaultSimilarityThreshold());
		}
		return builder.build();
	}

}

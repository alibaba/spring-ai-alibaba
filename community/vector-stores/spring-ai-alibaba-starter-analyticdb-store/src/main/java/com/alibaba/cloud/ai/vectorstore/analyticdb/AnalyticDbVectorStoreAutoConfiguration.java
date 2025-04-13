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
package com.alibaba.cloud.ai.vectorstore.analyticdb;

import com.aliyun.gpdb20160503.Client;
import com.aliyun.teaopenapi.models.Config;
import io.micrometer.observation.ObservationRegistry;
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

/**
 * @author HeYQ
 * @since 2025-03-04 13:10
 */
@AutoConfiguration
@ConditionalOnClass({ EmbeddingModel.class, Client.class, AnalyticDbVectorStore.class })
@EnableConfigurationProperties({ AnalyticDbVectorStoreProperties.class })
@ConditionalOnProperty(prefix = "spring.ai.vectorstore.analytic", name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class AnalyticDbVectorStoreAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public Client analyticdbClient(AnalyticDbVectorStoreProperties properties) throws Exception {
		Config clientConfig = Config.build(properties.toAnalyticDbClientParams());
		return new Client(clientConfig);
	}

	@Bean
	@ConditionalOnMissingBean(BatchingStrategy.class)
	BatchingStrategy analyticdbBatchingStrategy() {
		return new TokenCountBatchingStrategy();
	}

	@Bean
	@ConditionalOnMissingBean
	public AnalyticDbVectorStore analyticdbVectorStore(Client client, EmbeddingModel embeddingModel,
			AnalyticDbVectorStoreProperties properties, ObjectProvider<ObservationRegistry> observationRegistry,
			ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
			BatchingStrategy batchingStrategy) {

		AnalyticDbConfig config = new AnalyticDbConfig().setAccessKeyId(properties.getAccessKeyId())
			.setAccessKeySecret(properties.getAccessKeySecret())
			.setRegionId(properties.getRegionId())
			.setDbInstanceId(properties.getDbInstanceId())
			.setManagerAccount(properties.getManagerAccount())
			.setManagerAccountPassword(properties.getManagerAccountPassword())
			.setNamespace(properties.getNamespace())
			.setNamespacePassword(properties.getNamespacePassword());
		if (properties.getMetrics() != null) {
			config.setMetrics(properties.getMetrics());
		}
		if (properties.getReadTimeout() != null) {
			config.setReadTimeout(properties.getReadTimeout());
		}
		if (properties.getUserAgent() != null) {
			config.setUserAgent(properties.getUserAgent());
		}
		var builder = AnalyticDbVectorStore.builder(properties.getCollectName(), config, client, embeddingModel)
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

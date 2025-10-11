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
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConfigurationCondition;

/**
 * Auto-configuration that registers the AnalyticDB vector store when
 * {@code spring.ai.vectorstore.type=analyticdb} (or the legacy
 * {@code spring.ai.vectorstore.analytic.enabled=true}) is present.
 *
 * @author Alibaba Cloud
 */
@AutoConfiguration
@ConditionalOnClass({ EmbeddingModel.class, Client.class, AnalyticDbVectorStore.class })
@EnableConfigurationProperties(AnalyticDbVectorStoreProperties.class)
@Conditional(AnalyticDbVectorStoreAutoConfiguration.AnalyticDbActivationCondition.class)
public class AnalyticDbVectorStoreAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public Client analyticDbClient(AnalyticDbVectorStoreProperties properties) throws Exception {
		Config clientConfig = Config.build(properties.toAnalyticDbClientParams());
		return new Client(clientConfig);
	}

	@Bean
	@ConditionalOnMissingBean(BatchingStrategy.class)
	public BatchingStrategy analyticDbBatchingStrategy() {
		return new TokenCountBatchingStrategy();
	}

	@Bean
	@ConditionalOnMissingBean
	public AnalyticDbVectorStore analyticDbVectorStore(Client client, EmbeddingModel embeddingModel,
			AnalyticDbVectorStoreProperties properties, ObjectProvider<ObservationRegistry> observationRegistryProvider,
			ObjectProvider<VectorStoreObservationConvention> observationConventionProvider,
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

		AnalyticDbVectorStore.Builder builder = AnalyticDbVectorStore.builder(properties.getCollectName(), config, client,
				embeddingModel).batchingStrategy(batchingStrategy)
					.observationRegistry(observationRegistryProvider
						.getIfUnique(() -> ObservationRegistry.NOOP))
					.customObservationConvention(observationConventionProvider.getIfAvailable(() -> null));

		if (properties.getDefaultTopK() != null && properties.getDefaultTopK() >= 0) {
			builder.defaultTopK(properties.getDefaultTopK());
		}
		if (properties.getDefaultSimilarityThreshold() != null && properties.getDefaultSimilarityThreshold() >= 0.0) {
			builder.defaultSimilarityThreshold(properties.getDefaultSimilarityThreshold());
		}

		return builder.build();
	}

	static class AnalyticDbActivationCondition extends AnyNestedCondition {

		AnalyticDbActivationCondition() {
			super(ConfigurationCondition.ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnProperty(prefix = "spring.ai.vectorstore", name = "type", havingValue = "analyticdb")
		static class OnVectorStoreType {
		}

		@ConditionalOnProperty(prefix = AnalyticDbVectorStoreProperties.CONFIG_PREFIX, name = "enabled",
				havingValue = "true")
		static class OnLegacyEnabledProperty {
		}

	}

}

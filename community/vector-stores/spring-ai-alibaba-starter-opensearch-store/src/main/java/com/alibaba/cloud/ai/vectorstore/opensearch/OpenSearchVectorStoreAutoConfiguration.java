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
package com.alibaba.cloud.ai.vectorstore.opensearch;

import com.aliyun.ha3engine.vector.Client;
import com.aliyun.ha3engine.vector.models.Config;
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
 * @author fuyou.lxm 北极星
 */
@AutoConfiguration
@ConditionalOnClass({ OpenSearchVectorStore.class, EmbeddingModel.class, Client.class })
@EnableConfigurationProperties({ OpenSearchVectorStoreProperties.class, OpenSearchVectorStoreOptions.class })
@ConditionalOnProperty(prefix = "spring.ai.alibaba.vectorstore.opensearch", name = "enabled", havingValue = "true")
public class OpenSearchVectorStoreAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	BatchingStrategy batchingStrategy() {
		return new TokenCountBatchingStrategy();
	}

	@Bean
	@ConditionalOnMissingBean
	public Client openSearchClient(OpenSearchVectorStoreProperties properties) throws Exception {
		Config clientConfig = Config.build(properties.toClientParams());
		return new Client(clientConfig);
	}

	@Bean
	@ConditionalOnMissingBean
	public OpenSearchApi openSearchApi(OpenSearchVectorStoreProperties properties) {
		return new OpenSearchApi(properties);
	}

	@Bean
	@ConditionalOnMissingBean
	public OpenSearchVectorStore openSearchVectorStore(OpenSearchApi openSearchApi, EmbeddingModel embeddingModel,
			BatchingStrategy batchingStrategy, OpenSearchVectorStoreOptions options,
			ObjectProvider<ObservationRegistry> observationRegistry,
			ObjectProvider<VectorStoreObservationConvention> customObservationConvention) {

		return OpenSearchVectorStore.builder(openSearchApi, embeddingModel)
			.batchingStrategy(batchingStrategy)
			.options(options)
			.observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
			.customObservationConvention(customObservationConvention.getIfAvailable(() -> null))
			.build();
	}

}

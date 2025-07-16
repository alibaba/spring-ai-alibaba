/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.example.deepresearch.config.rag;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;

import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStoreOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.File;
import java.net.URI;

/**
 * Configuration for creating the VectorStore bean based on application properties.
 *
 * @author hupei
 */
@Configuration
@ConditionalOnProperty(prefix = RagProperties.RAG_PREFIX, name = "enabled", havingValue = "true")
@EnableConfigurationProperties(RagProperties.class)
public class RagVectorStoreConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(RagVectorStoreConfiguration.class);

	@Configuration
	@ConditionalOnProperty(prefix = RagProperties.RAG_PREFIX, name = "vector-store-type", havingValue = "simple",
			matchIfMissing = true)
	static class SimpleVectorStoreConfiguration {

		@Bean
		public VectorStore simpleVectorStore(EmbeddingModel embeddingModel, RagProperties ragProperties) {
			logger.info("Initializing SimpleVectorStore.");
			var simpleVectorStore = SimpleVectorStore.builder(embeddingModel).build();
			String storagePath = ragProperties.getSimple().getStoragePath();
			if (StringUtils.hasText(storagePath)) {
				File storageFile = new File(storagePath);
				if (storageFile.exists()) {
					logger.info("Loading SimpleVectorStore from file: {}", storagePath);
					simpleVectorStore.load(storageFile);
				}
			}
			return simpleVectorStore;
		}

	}

	@Configuration
	@ConditionalOnProperty(prefix = RagProperties.RAG_PREFIX, name = "vector-store-type", havingValue = "elasticsearch")
	static class ElasticsearchVectorStoreConfiguration {

		@Bean
		public RestClient elasticsearchRestClient(RagProperties ragProperties) {
			logger.info("Initializing Elasticsearch RestClient.");
			RagProperties.Elasticsearch esProps = ragProperties.getElasticsearch();
			URI uri = URI.create(esProps.getUris());

			CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			if (StringUtils.hasText(esProps.getUsername())) {
				credentialsProvider.setCredentials(AuthScope.ANY,
						new UsernamePasswordCredentials(esProps.getUsername(), esProps.getPassword()));
			}

			return RestClient.builder(new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme()))
				.setHttpClientConfigCallback(
						httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
				.build();
		}

		@Bean
		public VectorStore elasticsearchVectorStore(RestClient elasticsearchRestClient, EmbeddingModel embeddingModel,
				RagProperties ragProperties) {
			RagProperties.Elasticsearch esProps = ragProperties.getElasticsearch();
			logger.info("Initializing ElasticsearchVectorStore with index: {}", esProps.getIndexName());
			ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();
			// Optional: defaults to "spring-ai-document-index"
			options.setIndexName(esProps.getIndexName());
			// Optional: defaults to COSINE
			options.setSimilarity(esProps.getSimilarityFunction());
			// Optional: defaults to model dimensions or 1536
			options.setDimensions(esProps.getDimensions());

			return ElasticsearchVectorStore.builder(elasticsearchRestClient, embeddingModel)
				// Optional: use custom options
				.options(options)
				// Optional: defaults to false
				.initializeSchema(true)
				// Optional: defaults to TokenCountBatchingStrategy
				.batchingStrategy(new TokenCountBatchingStrategy())
				.build();
		}

	}

}

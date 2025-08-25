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
package com.alibaba.cloud.ai.connector.config.milvus;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Configuration
@EnableConfigurationProperties({ MilvusVectorStoreProperties.class })
@ConditionalOnProperty(prefix = "spring.ai.alibaba.nl2sql.milvus", name = "enabled", havingValue = "true",
		matchIfMissing = false)
@ConditionalOnClass({ EmbeddingModel.class, MilvusServiceClient.class })
public class MilvusVectorStoreConfiguration {

	@Bean
	public MilvusServiceClient milvusClient(MilvusVectorStoreProperties properties) {
		if (!StringUtils.hasText(properties.getHost()))
			throw new IllegalArgumentException(
					"Milvus host cannot be empty,please config the value of spring.ai.vectorstore.milvus.host");
		// spring.ai.vectorstore.milvus.port cannot be empty
		if (!Objects.nonNull(properties.getPort()))
			throw new IllegalArgumentException(
					"Milvus port cannot be empty,please config the value of spring.ai.vectorstore.milvus.port");
		// spring.ai.vectorstore.milvus.databaseName cannot be empty
		if (!StringUtils.hasText(properties.getDatabaseName()))
			throw new IllegalArgumentException(
					"Milvus databaseName cannot be empty,please config the value of spring.ai.vectorstore.milvus.databaseName");

		ConnectParam.Builder builder = ConnectParam.newBuilder()
			.withHost(properties.getHost())
			.withPort(properties.getPort());

		if (StringUtils.hasText(properties.getUsername()) && StringUtils.hasText(properties.getPassword())) {
			builder.withAuthorization(properties.getUsername(), properties.getPassword());
		}

		builder.withDatabaseName(properties.getDatabaseName());

		return new MilvusServiceClient(builder.build());
	}

	@Bean
	public MilvusVectorStore milvusVectorStore(MilvusServiceClient milvusClient, EmbeddingModel embeddingModel,
			MilvusVectorStoreProperties properties) {
		checkMilvusPropertiesBeforeInitVectorStore(properties);

		return MilvusVectorStore.builder(milvusClient, embeddingModel)
			.iDFieldName(properties.getIdFieldName())
			.contentFieldName(properties.getContentFieldName())
			.metadataFieldName(properties.getMetadataFieldName())
			.embeddingFieldName(properties.getEmbeddingFieldName())
			.embeddingDimension(properties.getEmbeddingDimension())
			.collectionName(properties.getCollectionName())
			.databaseName(properties.getDatabaseName())
			.indexType(IndexType.IVF_FLAT)
			.metricType(MetricType.COSINE)
			.batchingStrategy(new TokenCountBatchingStrategy())
			.initializeSchema(properties.isInitializeSchema())
			.build();
	}

	private static void checkMilvusPropertiesBeforeInitVectorStore(MilvusVectorStoreProperties properties) {
		// idFieldName，contentFieldName,metadataFieldName,embeddingFieldName,embeddingDimension
		// 必填
		if (!StringUtils.hasText(properties.getIdFieldName()))
			throw new IllegalArgumentException(
					"Milvus idFieldName cannot be empty,please config the value of spring.ai.vectorstore.milvus.idFieldName");

		if (!StringUtils.hasText(properties.getContentFieldName()))
			throw new IllegalArgumentException(
					"Milvus contentFieldName cannot be empty,please config the value of spring.ai.vectorstore.milvus.contentFieldName");

		if (!StringUtils.hasText(properties.getMetadataFieldName()))
			throw new IllegalArgumentException(
					"Milvus metadataFieldName cannot be empty,please config the value of spring.ai.vectorstore.milvus.metadataFieldName");

		if (!StringUtils.hasText(properties.getEmbeddingFieldName()))
			throw new IllegalArgumentException(
					"Milvus embeddingFieldName cannot be empty,please config the value of spring.ai.vectorstore.milvus.embeddingFieldName");

		if (!Objects.nonNull(properties.getEmbeddingDimension()))
			throw new IllegalArgumentException(
					"Milvus embeddingDimension cannot be empty,please config the value of spring.ai.vectorstore.milvus.embeddingDimension");
	}

}

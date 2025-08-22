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
package com.alibaba.cloud.ai.vectorstore.tair;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Test class for TairVectorStore to verify the fix for createObservationContextBuilder
 * method.
 *
 * @author tfh-yqr
 * @since 1.0.0-M3
 */
class TairVectorStoreTest {

	@Test
	void testCreateObservationContextBuilder() {
		// Create mock dependencies
		TairVectorApi mockTairVectorApi = mock(TairVectorApi.class);
		EmbeddingModel mockEmbeddingModel = mock(EmbeddingModel.class);

		// Create TairVectorStore instance
		TairVectorStore vectorStore = TairVectorStore.builder(mockTairVectorApi, mockEmbeddingModel).build();

		// Test that createObservationContextBuilder no longer returns null
		VectorStoreObservationContext.Builder builder = vectorStore.createObservationContextBuilder("test_operation");

		// Verify the builder is not null
		assertThat(builder).isNotNull();

		// Verify the builder has the correct properties
		VectorStoreObservationContext context = builder.build();
		assertThat(context.getDatabaseSystem()).isEqualTo("tair");
		assertThat(context.getOperationName()).isEqualTo("test_operation");
		assertThat(context.getCollectionName()).isEqualTo("spring_ai_tair_vector_store"); // default
																							// value
		assertThat(context.getDimensions()).isEqualTo(1536); // default value
		assertThat(context.getSimilarityMetric()).isEqualTo("L2"); // default value
	}

	@Test
	void testCreateObservationContextBuilderWithCustomOptions() {
		// Create mock dependencies
		TairVectorApi mockTairVectorApi = mock(TairVectorApi.class);
		EmbeddingModel mockEmbeddingModel = mock(EmbeddingModel.class);

		// Create custom options
		TairVectorStoreOptions customOptions = new TairVectorStoreOptions();
		customOptions.setIndexName("custom_index");
		customOptions.setDimensions(512);
		customOptions.setDistanceMethod(com.aliyun.tair.tairvector.params.DistanceMethod.IP);

		// Create TairVectorStore instance with custom options
		TairVectorStore vectorStore = TairVectorStore.builder(mockTairVectorApi, mockEmbeddingModel)
			.options(customOptions)
			.build();

		// Test createObservationContextBuilder with custom options
		VectorStoreObservationContext.Builder builder = vectorStore.createObservationContextBuilder("custom_operation");

		// Verify the builder is not null
		assertThat(builder).isNotNull();

		// Verify the builder has the correct custom properties
		VectorStoreObservationContext context = builder.build();
		assertThat(context.getDatabaseSystem()).isEqualTo("tair");
		assertThat(context.getOperationName()).isEqualTo("custom_operation");
		assertThat(context.getCollectionName()).isEqualTo("custom_index");
		assertThat(context.getDimensions()).isEqualTo(512);
		assertThat(context.getSimilarityMetric()).isEqualTo("IP");
	}

}

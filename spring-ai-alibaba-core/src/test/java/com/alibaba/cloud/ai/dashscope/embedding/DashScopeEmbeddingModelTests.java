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
package com.alibaba.cloud.ai.dashscope.embedding;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.EmbeddingList;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.EmbeddingUsage;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.Embeddings;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.Embedding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test cases for DashScopeEmbeddingModel. Tests cover basic embedding operations, error
 * handling, and various edge cases.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
class DashScopeEmbeddingModelTests {

	// Test constants
	private static final String TEST_MODEL = "text-embedding-v2";

	private static final String TEST_TEXT_TYPE = "document";

	private static final String TEST_REQUEST_ID = "test-request-id";

	private static final String TEST_TEXT = "Hello, world!";

	private DashScopeApi dashScopeApi;

	private DashScopeEmbeddingModel embeddingModel;

	private DashScopeEmbeddingOptions defaultOptions;

	@BeforeEach
	void setUp() {
		// Initialize mock objects and test instances
		dashScopeApi = Mockito.mock(DashScopeApi.class);
		defaultOptions = DashScopeEmbeddingOptions.builder().withModel(TEST_MODEL).withTextType(TEST_TEXT_TYPE).build();
		embeddingModel = new DashScopeEmbeddingModel(dashScopeApi, MetadataMode.EMBED, defaultOptions);
	}

	@Test
	void testBasicEmbedding() {
		// Test basic embedding with a single text input
		float[] embeddingVector = { 0.1f, 0.2f, 0.3f };
		Embedding embedding = new Embedding(0, embeddingVector);
		Embeddings embeddings = new Embeddings(List.of(embedding));
		EmbeddingUsage usage = new EmbeddingUsage(10L);
		EmbeddingList embeddingList = new EmbeddingList(TEST_REQUEST_ID, null, null, embeddings, usage);
		ResponseEntity<EmbeddingList> responseEntity = ResponseEntity.ok(embeddingList);

		when(dashScopeApi.embeddings(any())).thenReturn(responseEntity);

		// Test embedding a single text
		EmbeddingResponse response = embeddingModel.embedForResponse(List.of(TEST_TEXT));

		assertThat(response.getResults()).hasSize(1);
		assertThat(response.getResults().get(0).getOutput()).containsExactly(embeddingVector);
		assertThat(response.getResults().get(0).getIndex()).isEqualTo(0);
	}

	@Test
	void testMultipleEmbeddings() {
		// Test embedding multiple texts
		float[] vector1 = { 0.1f, 0.2f, 0.3f };
		float[] vector2 = { 0.4f, 0.5f, 0.6f };
		List<Embedding> embeddingList = Arrays.asList(new Embedding(0, vector1), new Embedding(1, vector2));
		Embeddings embeddings = new Embeddings(embeddingList);
		EmbeddingUsage usage = new EmbeddingUsage(20L);
		EmbeddingList response = new EmbeddingList(TEST_REQUEST_ID, null, null, embeddings, usage);
		ResponseEntity<EmbeddingList> responseEntity = ResponseEntity.ok(response);

		when(dashScopeApi.embeddings(any())).thenReturn(responseEntity);

		List<String> texts = Arrays.asList("First text", "Second text");
		EmbeddingResponse embeddingResponse = embeddingModel.embedForResponse(texts);

		assertThat(embeddingResponse.getResults()).hasSize(2);
		assertThat(embeddingResponse.getResults().get(0).getOutput()).containsExactly(vector1);
		assertThat(embeddingResponse.getResults().get(1).getOutput()).containsExactly(vector2);
	}

	@Test
	void testEmbedDocument() {
		// Test embedding a Document object
		float[] embeddingVector = { 0.1f, 0.2f, 0.3f };
		Embedding embedding = new Embedding(0, embeddingVector);
		Embeddings embeddings = new Embeddings(List.of(embedding));
		EmbeddingUsage usage = new EmbeddingUsage(10L);
		EmbeddingList embeddingList = new EmbeddingList(TEST_REQUEST_ID, null, null, embeddings, usage);
		ResponseEntity<EmbeddingList> responseEntity = ResponseEntity.ok(embeddingList);

		when(dashScopeApi.embeddings(any())).thenReturn(responseEntity);

		Document document = new Document(TEST_TEXT, Map.of("key", "value"));
		float[] result = embeddingModel.embed(document);

		assertThat(result).containsExactly(embeddingVector);
	}

	@Test
	void testErrorHandling() {
		// Test error handling with error response
		EmbeddingList errorResponse = new EmbeddingList(TEST_REQUEST_ID, "ERROR_CODE", "Error message", null,
				new EmbeddingUsage(0L));
		ResponseEntity<EmbeddingList> responseEntity = ResponseEntity.ok(errorResponse);

		when(dashScopeApi.embeddings(any())).thenReturn(responseEntity);

		assertThatThrownBy(() -> embeddingModel.embedForResponse(List.of(TEST_TEXT)))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Embedding failed");
	}

	@Test
	void testNullInputHandling() {
		// Test handling of null inputs
		assertThatThrownBy(() -> embeddingModel.embed((Document) null)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Document must not be null");

		assertThatThrownBy(() -> embeddingModel.embedForResponse((List<String>) null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Texts must not be null");
	}

	@Test
	void testCustomOptions() {
		// Test embedding with custom options
		float[] embeddingVector = { 0.1f, 0.2f, 0.3f };
		Embedding embedding = new Embedding(0, embeddingVector);
		Embeddings embeddings = new Embeddings(List.of(embedding));
		EmbeddingUsage usage = new EmbeddingUsage(10L);
		EmbeddingList embeddingList = new EmbeddingList(TEST_REQUEST_ID, null, null, embeddings, usage);
		ResponseEntity<EmbeddingList> responseEntity = ResponseEntity.ok(embeddingList);

		when(dashScopeApi.embeddings(any())).thenReturn(responseEntity);

		DashScopeEmbeddingOptions customOptions = DashScopeEmbeddingOptions.builder()
			.withModel("custom-model")
			.withTextType("query")
			.withDimensions(512)
			.build();

		EmbeddingRequest request = new EmbeddingRequest(List.of(TEST_TEXT), customOptions);
		EmbeddingResponse response = embeddingModel.call(request);

		assertThat(response.getResults()).hasSize(1);
		assertThat(response.getResults().get(0).getOutput()).containsExactly(embeddingVector);
	}

	@Test
	void testEmptyResponse() {
		// Test handling of empty response with non-null usage
		EmbeddingUsage usage = new EmbeddingUsage(0L);
		EmbeddingList emptyResponse = new EmbeddingList(TEST_REQUEST_ID, null, null, new Embeddings(List.of()), usage);
		ResponseEntity<EmbeddingList> responseEntity = ResponseEntity.ok(emptyResponse);

		when(dashScopeApi.embeddings(any())).thenReturn(responseEntity);

		EmbeddingResponse response = embeddingModel.embedForResponse(List.of(TEST_TEXT));

		assertThat(response.getResults()).isEmpty();
		assertThat(response.getMetadata().getUsage().getTotalTokens()).isZero();
	}

}

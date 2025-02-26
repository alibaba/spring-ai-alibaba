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
package com.alibaba.cloud.ai.dashscope.rerank;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.RerankResponse;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.RerankResponseOutput;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.RerankResponseOutputResult;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.TokenUsage;
import com.alibaba.cloud.ai.document.DocumentWithScore;
import com.alibaba.cloud.ai.model.RerankRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test cases for DashScopeRerankModel. Tests cover constructor validation, reranking
 * functionality, error handling, and response processing.
 *
 * @author yuanci.ytb
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
@ExtendWith(MockitoExtension.class)
class DashScopeRerankModelTests {

	// Test constants
	private static final String TEST_MODEL = "gte-rerank";

	private static final String TEST_QUERY = "test query";

	private static final String TEST_DOC_TEXT = "test document text";

	private static final Double TEST_SCORE = 0.85;

	@Mock
	private DashScopeApi dashScopeApi;

	private DashScopeRerankModel rerankModel;

	private DashScopeRerankOptions defaultOptions;

	@BeforeEach
	void setUp() {
		// Initialize default options
		defaultOptions = DashScopeRerankOptions.builder()
			.withModel(TEST_MODEL)
			.withTopN(3)
			.withReturnDocuments(false)
			.build();

		// Initialize rerank model
		rerankModel = new DashScopeRerankModel(dashScopeApi, defaultOptions);
	}

	/**
	 * Test constructor with null DashScopeApi. Verifies that constructor throws
	 * IllegalArgumentException when DashScopeApi is null.
	 */
	@Test
	void testConstructorWithNullApi() {
		assertThatThrownBy(() -> new DashScopeRerankModel(null)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("DashScopeApi must not be null");
	}

	/**
	 * Test constructor with null options. Verifies that constructor throws
	 * IllegalArgumentException when options is null.
	 */
	@Test
	void testConstructorWithNullOptions() {
		assertThatThrownBy(() -> new DashScopeRerankModel(dashScopeApi, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Options must not be null");
	}

	/**
	 * Test reranking with null query. Verifies that reranking throws
	 * IllegalArgumentException when query is null.
	 */
	@Test
	void testRerankWithNullQuery() {
		List<Document> documents = Collections.singletonList(new Document(TEST_DOC_TEXT));
		RerankRequest request = new RerankRequest(null, documents);

		assertThatThrownBy(() -> rerankModel.call(request)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("query must not be null");
	}

	/**
	 * Test reranking with null documents. Verifies that reranking throws
	 * IllegalArgumentException when documents list is null.
	 */
	@Test
	void testRerankWithNullDocuments() {
		RerankRequest request = new RerankRequest(TEST_QUERY, null);

		assertThatThrownBy(() -> rerankModel.call(request)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("documents must not be null");
	}

	/**
	 * Test successful reranking. Verifies that reranking returns correct scores and
	 * documents.
	 */
	@Test
	void testSuccessfulRerank() {
		// Prepare test data
		Document doc1 = new Document(TEST_DOC_TEXT + "1");
		Document doc2 = new Document(TEST_DOC_TEXT + "2");
		List<Document> documents = Arrays.asList(doc1, doc2);

		// Mock API response
		RerankResponseOutputResult result1 = new RerankResponseOutputResult(0, 0.9, new HashMap<>());
		RerankResponseOutputResult result2 = new RerankResponseOutputResult(1, 0.7, new HashMap<>());
		RerankResponseOutput output = new RerankResponseOutput(Arrays.asList(result1, result2));
		TokenUsage usage = new TokenUsage(10, 20, 30);
		RerankResponse apiResponse = new RerankResponse(output, usage, "test-request-id");

		when(dashScopeApi.rerankEntity(any())).thenReturn(ResponseEntity.ok(apiResponse));

		// Execute rerank
		RerankRequest request = new RerankRequest(TEST_QUERY, documents);
		com.alibaba.cloud.ai.model.RerankResponse response = rerankModel.call(request);

		// Verify results
		List<DocumentWithScore> results = response.getResults();
		assertThat(results).hasSize(2);
		assertThat(results.get(0).getScore()).isEqualTo(0.9);
		assertThat(results.get(1).getScore()).isEqualTo(0.7);
		assertThat(results.get(0).getOutput()).isEqualTo(doc1);
		assertThat(results.get(1).getOutput()).isEqualTo(doc2);
	}

	/**
	 * Test reranking with empty response. Verifies that reranking handles empty API
	 * response correctly.
	 */
	@Test
	void testEmptyResponse() {
		// Prepare test data
		Document doc = new Document(TEST_DOC_TEXT);
		List<Document> documents = Collections.singletonList(doc);

		// Mock empty API response
		when(dashScopeApi.rerankEntity(any())).thenReturn(ResponseEntity.ok(null));

		// Execute rerank
		RerankRequest request = new RerankRequest(TEST_QUERY, documents);
		com.alibaba.cloud.ai.model.RerankResponse response = rerankModel.call(request);

		// Verify empty results
		assertThat(response.getResults()).isEmpty();
	}

	/**
	 * Test reranking with custom options. Verifies that reranking uses custom options
	 * correctly.
	 */
	@Test
	void testCustomOptions() {
		// Prepare test data
		Document doc = new Document(TEST_DOC_TEXT);
		List<Document> documents = Collections.singletonList(doc);

		// Create custom options
		DashScopeRerankOptions customOptions = DashScopeRerankOptions.builder()
			.withModel("custom-model")
			.withTopN(5)
			.withReturnDocuments(true)
			.build();

		// Mock API response
		RerankResponseOutputResult result = new RerankResponseOutputResult(0, TEST_SCORE, new HashMap<>());
		RerankResponseOutput output = new RerankResponseOutput(Collections.singletonList(result));
		RerankResponse apiResponse = new RerankResponse(output, new TokenUsage(10, 20, 30), "test-request-id");

		when(dashScopeApi.rerankEntity(any())).thenReturn(ResponseEntity.ok(apiResponse));

		// Execute rerank with custom options
		RerankRequest request = new RerankRequest(TEST_QUERY, documents, customOptions);
		com.alibaba.cloud.ai.model.RerankResponse response = rerankModel.call(request);

		// Verify results
		assertThat(response.getResults()).hasSize(1);
		assertThat(response.getResults().get(0).getScore()).isEqualTo(TEST_SCORE);
	}

}

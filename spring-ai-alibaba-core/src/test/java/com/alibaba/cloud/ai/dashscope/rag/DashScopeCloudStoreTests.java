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
package com.alibaba.cloud.ai.dashscope.rag;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.common.DashScopeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test cases for DashScopeCloudStore.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
class DashScopeCloudStoreTests {

	@Mock
	private DashScopeApi dashScopeApi;

	private DashScopeCloudStore cloudStore;

	private DashScopeStoreOptions options;

	private static final String TEST_INDEX_NAME = "test-index";

	private static final String TEST_PIPELINE_ID = "test-pipeline-id";

	private static final String TEST_QUERY = "test query";

	@BeforeEach
	void setUp() {
		// Initialize Mockito annotations
		MockitoAnnotations.openMocks(this);

		// Set up basic configuration
		options = new DashScopeStoreOptions(TEST_INDEX_NAME);
		cloudStore = new DashScopeCloudStore(dashScopeApi, options);

		// Set up basic mock behavior
		when(dashScopeApi.getPipelineIdByName(TEST_INDEX_NAME)).thenReturn(TEST_PIPELINE_ID);
	}

	@Test
	void testAddDocumentsWithNullList() {
		// Test adding null document list
		assertThrows(DashScopeException.class, () -> cloudStore.add(null));
	}

	@Test
	void testAddDocumentsWithEmptyList() {
		// Test adding empty document list
		assertThrows(DashScopeException.class, () -> cloudStore.add(new ArrayList<>()));
	}

	@Test
	void testAddDocumentsSuccessfully() {
		// Create test documents
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("key", "value");

		List<Document> documents = Arrays.asList(new Document("id1", "content1", metadata),
				new Document("id2", "content2", metadata));

		// Execute add operation
		cloudStore.add(documents);

		// Verify API call
		verify(dashScopeApi).upsertPipeline(eq(documents), eq(options));
	}

	@Test
	void testDeleteDocumentsWithNonExistentIndex() {
		// Mock non-existent index scenario
		when(dashScopeApi.getPipelineIdByName(TEST_INDEX_NAME)).thenReturn(null);

		// Test document deletion
		List<String> ids = Arrays.asList("id1", "id2");
		assertThrows(DashScopeException.class, () -> cloudStore.delete(ids));
	}

	@Test
	void testDeleteDocumentsSuccessfully() {
		// Prepare test data
		List<String> ids = Arrays.asList("id1", "id2");

		// Execute delete operation
		cloudStore.delete(ids);

		// Verify API call
		verify(dashScopeApi).deletePipelineDocument(TEST_PIPELINE_ID, ids);
	}

	@Test
	void testSimilaritySearchWithNonExistentIndex() {
		// Mock non-existent index scenario
		when(dashScopeApi.getPipelineIdByName(TEST_INDEX_NAME)).thenReturn(null);

		// Test similarity search
		assertThrows(DashScopeException.class, () -> cloudStore.similaritySearch(TEST_QUERY));
	}

	@Test
	void testSimilaritySearchSuccessfully() {
		// Prepare test data
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("key", "value");

		List<Document> expectedResults = Arrays.asList(new Document("id1", "result1", metadata),
				new Document("id2", "result2", metadata));
		when(dashScopeApi.retriever(anyString(), anyString(), any())).thenReturn(expectedResults);

		// Execute search
		List<Document> results = cloudStore.similaritySearch(TEST_QUERY);

		// Verify results
		assertThat(results).isEqualTo(expectedResults);
		verify(dashScopeApi).retriever(eq(TEST_PIPELINE_ID), eq(TEST_QUERY), any());
	}

	@Test
	void testSimilaritySearchWithSearchRequest() {
		// Prepare test data
		SearchRequest request = SearchRequest.builder().query(TEST_QUERY).topK(5).build();

		Map<String, Object> metadata = new HashMap<>();
		metadata.put("key", "value");

		List<Document> expectedResults = Arrays.asList(new Document("id1", "result1", metadata),
				new Document("id2", "result2", metadata));
		when(dashScopeApi.retriever(anyString(), anyString(), any())).thenReturn(expectedResults);

		// Execute search
		List<Document> results = cloudStore.similaritySearch(request);

		// Verify results
		assertThat(results).isEqualTo(expectedResults);
		verify(dashScopeApi).retriever(eq(TEST_PIPELINE_ID), eq(TEST_QUERY), any());
	}

	@Test
	void testGetName() {
		// Test getting name
		String name = cloudStore.getName();
		assertThat(name).isEqualTo("DashScopeCloudStore");
	}

}

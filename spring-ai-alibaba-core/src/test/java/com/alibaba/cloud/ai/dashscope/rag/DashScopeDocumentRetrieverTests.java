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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Test cases for DashScopeDocumentRetriever. Tests cover document retrieval
 * functionality, error handling, and edge cases.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
@ExtendWith(MockitoExtension.class)
class DashScopeDocumentRetrieverTests {

	// Test constants
	private static final String TEST_INDEX_NAME = "test-index";

	private static final String TEST_PIPELINE_ID = "test-pipeline-id";

	private static final String TEST_QUERY = "test query";

	private static final String TEST_DOC_ID = "test-doc-id";

	private static final String TEST_DOC_TEXT = "test document text";

	@Mock
	private DashScopeApi dashScopeApi;

	private DashScopeDocumentRetriever retriever;

	private DashScopeDocumentRetrieverOptions options;

	@BeforeEach
	void setUp() {
		// Initialize options with test values
		options = DashScopeDocumentRetrieverOptions.builder().withIndexName(TEST_INDEX_NAME).build();

		// Create retriever instance
		retriever = new DashScopeDocumentRetriever(dashScopeApi, options);
	}

	@Test
	void testConstructorWithNullOptions() {
		// Test constructor with null options
		assertThatThrownBy(() -> new DashScopeDocumentRetriever(dashScopeApi, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("RetrieverOptions must not be null");
	}

	@Test
	void testConstructorWithNullIndexName() {
		// Test constructor with null index name
		DashScopeDocumentRetrieverOptions optionsWithNullIndex = new DashScopeDocumentRetrieverOptions();
		assertThatThrownBy(() -> new DashScopeDocumentRetriever(dashScopeApi, optionsWithNullIndex))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("IndexName must not be null");
	}

	@Test
    void testRetrieveWithNonExistentIndex() {
        // Mock API response for non-existent index
        when(dashScopeApi.getPipelineIdByName(TEST_INDEX_NAME)).thenReturn(null);

        // Test retrieval with non-existent index
        assertThatThrownBy(() -> retriever.retrieve(new Query(TEST_QUERY)))
                .isInstanceOf(DashScopeException.class)
                .hasMessageContaining("Index:" + TEST_INDEX_NAME + " NotExist");
    }

	@Test
	void testSuccessfulRetrieval() {
		// Create test document with metadata
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("doc_name", "test.txt");
		metadata.put("title", "Test Document");
		Document testDoc = new Document(TEST_DOC_ID, TEST_DOC_TEXT, metadata);
		List<Document> expectedDocs = List.of(testDoc);

		// Mock API responses
		when(dashScopeApi.getPipelineIdByName(TEST_INDEX_NAME)).thenReturn(TEST_PIPELINE_ID);
		when(dashScopeApi.retriever(eq(TEST_PIPELINE_ID), eq(TEST_QUERY), any(DashScopeDocumentRetrieverOptions.class)))
			.thenReturn(expectedDocs);

		// Test successful document retrieval
		List<Document> retrievedDocs = retriever.retrieve(new Query(TEST_QUERY));

		// Verify retrieved documents
		assertThat(retrievedDocs).isNotNull().hasSize(1);
		Document retrievedDoc = retrievedDocs.get(0);
		assertThat(retrievedDoc.getId()).isEqualTo(TEST_DOC_ID);
		assertThat(retrievedDoc.getText()).isEqualTo(TEST_DOC_TEXT);
		assertThat(retrievedDoc.getMetadata()).containsEntry("doc_name", "test.txt")
			.containsEntry("title", "Test Document");
	}

	@Test
    void testEmptyRetrieval() {
        // Mock API responses for empty result
        when(dashScopeApi.getPipelineIdByName(TEST_INDEX_NAME)).thenReturn(TEST_PIPELINE_ID);
        when(dashScopeApi.retriever(eq(TEST_PIPELINE_ID), eq(TEST_QUERY), any(DashScopeDocumentRetrieverOptions.class)))
                .thenReturn(new ArrayList<>());

        // Test retrieval with no results
        List<Document> retrievedDocs = retriever.retrieve(new Query(TEST_QUERY));

        // Verify empty result
        assertThat(retrievedDocs).isNotNull().isEmpty();
    }

	@Test
	void testRetrievalWithMultipleDocuments() {
		// Create multiple test documents
		List<Document> expectedDocs = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			Map<String, Object> metadata = new HashMap<>();
			metadata.put("doc_name", "test" + i + ".txt");
			metadata.put("title", "Test Document " + i);
			Document doc = new Document("doc-" + i, "content " + i, metadata);
			expectedDocs.add(doc);
		}

		// Mock API responses
		when(dashScopeApi.getPipelineIdByName(TEST_INDEX_NAME)).thenReturn(TEST_PIPELINE_ID);
		when(dashScopeApi.retriever(eq(TEST_PIPELINE_ID), eq(TEST_QUERY), any(DashScopeDocumentRetrieverOptions.class)))
			.thenReturn(expectedDocs);

		// Test retrieval with multiple documents
		List<Document> retrievedDocs = retriever.retrieve(new Query(TEST_QUERY));

		// Verify retrieved documents
		assertThat(retrievedDocs).isNotNull().hasSize(3);
		for (int i = 0; i < retrievedDocs.size(); i++) {
			Document doc = retrievedDocs.get(i);
			assertThat(doc.getId()).isEqualTo("doc-" + i);
			assertThat(doc.getText()).isEqualTo("content " + i);
			assertThat(doc.getMetadata()).containsEntry("doc_name", "test" + i + ".txt")
				.containsEntry("title", "Test Document " + i);
		}
	}

}

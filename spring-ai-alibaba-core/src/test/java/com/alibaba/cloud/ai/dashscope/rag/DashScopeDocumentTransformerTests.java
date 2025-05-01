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
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test cases for DashScopeDocumentTransformer. Tests cover constructor validation,
 * document splitting, error handling, and successful transformations.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
class DashScopeDocumentTransformerTests {

	// Test constants
	private static final String TEST_DOC_ID = "test_doc_1";

	private static final String TEST_CONTENT = "This is a test document content";

	private static final String TEST_CHUNK_CONTENT = "This is a test chunk content";

	private static final int TEST_CHUNK_ID = 1;

	@Mock
	private DashScopeApi dashScopeApi;

	private DashScopeDocumentTransformer transformer;

	private DashScopeDocumentTransformerOptions options;

	@BeforeEach
	void setUp() {
		// Initialize mocks
		MockitoAnnotations.openMocks(this);

		// Create default options
		options = DashScopeDocumentTransformerOptions.builder()
			.withChunkSize(500)
			.withOverlapSize(100)
			.withSeparator("|,|，|。|？|！|\\n|\\\\?|\\\\!")
			.withFileType("idp")
			.withLanguage("cn")
			.build();

		// Create transformer instance
		transformer = new DashScopeDocumentTransformer(dashScopeApi, options);
	}

	/**
	 * Test constructor with null DashScopeApi. Should throw IllegalArgumentException.
	 */
	@Test
	void testConstructorWithNullApi() {
		assertThatThrownBy(() -> new DashScopeDocumentTransformer(null)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("DashScopeApi must not be null");
	}

	/**
	 * Test constructor with null options. Should throw IllegalArgumentException.
	 */
	@Test
	void testConstructorWithNullOptions() {
		assertThatThrownBy(() -> new DashScopeDocumentTransformer(dashScopeApi, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("DashScopeDocumentTransformerOptions must not be null");
	}

	/**
	 * Test applying transformation with null documents list. Should throw
	 * RuntimeException.
	 */
	@Test
	void testApplyWithNullDocuments() {
		assertThatThrownBy(() -> transformer.apply(null)).isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Documents must not be null");
	}

	/**
	 * Test applying transformation with empty documents list. Should throw
	 * RuntimeException.
	 */
	@Test
	void testApplyWithEmptyDocuments() {
		assertThatThrownBy(() -> transformer.apply(Collections.emptyList())).isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Documents must not be null");
	}

	/**
	 * Test applying transformation with multiple documents. Should throw RuntimeException
	 * as only one document is supported.
	 */
	@Test
	void testApplyWithMultipleDocuments() {
		Map<String, Object> metadata = new HashMap<>();
		List<Document> documents = List.of(new Document(TEST_DOC_ID, TEST_CONTENT, metadata),
				new Document("test_doc_2", "Another test content", metadata));

		assertThatThrownBy(() -> transformer.apply(documents)).isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Just support one Document");
	}

	/**
	 * Test successful document splitting. Should return list of document chunks with
	 * correct IDs and content.
	 */
	@Test
	void testSuccessfulSplit() {
		// Create test document
		Map<String, Object> metadata = new HashMap<>();
		Document document = new Document(TEST_DOC_ID, TEST_CONTENT, metadata);

		// Mock API response
		DashScopeApi.DocumentSplitResponse.DocumentChunk chunk = new DashScopeApi.DocumentSplitResponse.DocumentChunk(
				TEST_CHUNK_ID, TEST_CHUNK_CONTENT, null, null, null, null);
		DashScopeApi.DocumentSplitResponse.DocumentSplitResponseData chunkService = new DashScopeApi.DocumentSplitResponse.DocumentSplitResponseData(
				List.of(chunk));
		DashScopeApi.DocumentSplitResponse response = new DashScopeApi.DocumentSplitResponse(chunkService);

		when(dashScopeApi.documentSplit(any(), any())).thenReturn(ResponseEntity.ok(response));

		// Perform transformation
		List<Document> result = transformer.apply(List.of(document));

		// Verify results
		assertThat(result).isNotNull().hasSize(1);
		Document resultDoc = result.get(0);
		assertThat(resultDoc.getId()).isEqualTo(TEST_DOC_ID + "_" + TEST_CHUNK_ID);
		assertThat(resultDoc.getText()).isEqualTo(TEST_CHUNK_CONTENT);
		assertThat(resultDoc.getMetadata()).isEqualTo(metadata);
	}

	/**
	 * Test split with null API response. Should throw DashScopeException.
	 */
	@Test
	void testSplitWithNullResponse() {
		Map<String, Object> metadata = new HashMap<>();
		Document document = new Document(TEST_DOC_ID, TEST_CONTENT, metadata);

		when(dashScopeApi.documentSplit(any(), any())).thenReturn(null);

		assertThatThrownBy(() -> transformer.apply(List.of(document))).isInstanceOf(DashScopeException.class);
	}

	/**
	 * Test split with empty chunks in response. Should throw DashScopeException.
	 */
	@Test
	void testSplitWithEmptyChunks() {
		Map<String, Object> metadata = new HashMap<>();
		Document document = new Document(TEST_DOC_ID, TEST_CONTENT, metadata);

		DashScopeApi.DocumentSplitResponse.DocumentSplitResponseData chunkService = new DashScopeApi.DocumentSplitResponse.DocumentSplitResponseData(
				Collections.emptyList());
		DashScopeApi.DocumentSplitResponse response = new DashScopeApi.DocumentSplitResponse(chunkService);

		when(dashScopeApi.documentSplit(any(), any())).thenReturn(ResponseEntity.ok(response));

		assertThatThrownBy(() -> transformer.apply(List.of(document))).isInstanceOf(DashScopeException.class);
	}

}

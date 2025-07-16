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
package com.alibaba.cloud.ai.model;

import com.alibaba.cloud.ai.document.DocumentWithScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for RerankResponse. Tests the functionality of rerank response handling and
 * metadata information.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
class RerankResponseTests {

	private List<DocumentWithScore> documents;

	private RerankResponseMetadata metadata;

	private static final double TEST_SCORE = 0.95;

	@BeforeEach
	void setUp() {
		// Initialize test documents
		documents = new ArrayList<>();
		Map<String, Object> docMetadata = new HashMap<>();
		docMetadata.put("key1", "value1");

		Document doc = new Document("test content", docMetadata);
		DocumentWithScore docWithScore = DocumentWithScore.builder().withDocument(doc).withScore(TEST_SCORE).build();
		documents.add(docWithScore);

		// Initialize metadata
		metadata = new RerankResponseMetadata();
	}

	@Test
	void testConstructorWithDocuments() {
		// Test constructor with only documents parameter
		RerankResponse response = new RerankResponse(documents);

		// Verify documents are set correctly
		assertThat(response.getResults()).isEqualTo(documents);
		// Verify metadata is initialized with default values
		assertThat(response.getMetadata()).isNotNull();
	}

	@Test
	void testConstructorWithDocumentsAndMetadata() {
		// Test constructor with both documents and metadata parameters
		RerankResponse response = new RerankResponse(documents, metadata);

		// Verify both documents and metadata are set correctly
		assertThat(response.getResults()).isEqualTo(documents);
		assertThat(response.getMetadata()).isEqualTo(metadata);
	}

	@Test
	void testGetResultWithEmptyDocuments() {
		// Test getResult() with empty documents list
		RerankResponse response = new RerankResponse(new ArrayList<>());

		// Verify null is returned when documents list is empty
		assertThat(response.getResult()).isNull();
	}

	@Test
	void testGetResultWithDocuments() {
		// Test getResult() with non-empty documents list
		RerankResponse response = new RerankResponse(documents);

		// Verify first document is returned
		assertThat(response.getResult()).isEqualTo(documents.get(0));
	}

	@Test
	void testGetResults() {
		// Test getResults() method
		RerankResponse response = new RerankResponse(documents);

		// Verify all documents are returned
		assertThat(response.getResults()).isNotNull().hasSize(documents.size()).containsExactlyElementsOf(documents);
	}

	@Test
	void testGetMetadata() {
		// Test getMetadata() method
		RerankResponse response = new RerankResponse(documents, metadata);

		// Verify metadata is returned correctly
		assertThat(response.getMetadata()).isNotNull().isEqualTo(metadata);
	}

}

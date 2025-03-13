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
package com.alibaba.cloud.ai.document;

import com.alibaba.cloud.ai.model.RerankResultMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for DocumentWithScore
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
class DocumentWithScoreTests {

	private static final String TEST_ID = "test-doc-001";

	@Test
	void testBuilderAndGetters() {
		// Test building DocumentWithScore using builder pattern and verify getters
		Document document = new Document(TEST_ID, "test content", createMetadata());
		Double score = 0.95;
		RerankResultMetadata metadata = new RerankResultMetadata();

		DocumentWithScore documentWithScore = DocumentWithScore.builder()
			.withDocument(document)
			.withScore(score)
			.withMetadata(metadata)
			.build();

		// Verify all fields are set correctly
		assertThat(documentWithScore.getScore()).isEqualTo(score);
		assertThat(documentWithScore.getOutput()).isEqualTo(document);
		assertThat(documentWithScore.getMetadata()).isEqualTo(metadata);
	}

	@Test
	void testEqualsAndHashCode() {
		// Test equals and hashCode methods - using same ID for documents that should be
		// equal
		Document document1 = new Document(TEST_ID, "test content", createMetadata());
		Document document2 = new Document(TEST_ID, "test content", createMetadata());
		Double score = 0.95;
		RerankResultMetadata metadata1 = new RerankResultMetadata();
		RerankResultMetadata metadata2 = new RerankResultMetadata();

		// Create two objects with same score and document but different metadata
		DocumentWithScore doc1 = DocumentWithScore.builder()
			.withDocument(document1)
			.withScore(score)
			.withMetadata(metadata1)
			.build();

		DocumentWithScore doc2 = DocumentWithScore.builder()
			.withDocument(document2)
			.withScore(score)
			.withMetadata(metadata2)
			.build();

		// Create object with different score
		DocumentWithScore doc3 = DocumentWithScore.builder()
			.withDocument(document2)
			.withScore(0.85)
			.withMetadata(metadata2)
			.build();

		// Test equality - should be equal even with different metadata
		assertThat(doc1).isEqualTo(doc2);
		assertThat(doc1.hashCode()).isEqualTo(doc2.hashCode());

		// Test inequality - different score should make objects unequal
		assertThat(doc1).isNotEqualTo(doc3);
		assertThat(doc1.hashCode()).isNotEqualTo(doc3.hashCode());

		// Create object with different document
		Document document3 = new Document("test-doc-002", "different content", createMetadata());
		DocumentWithScore doc4 = DocumentWithScore.builder()
			.withDocument(document3)
			.withScore(score)
			.withMetadata(metadata1)
			.build();

		// Test inequality - different document should make objects unequal
		assertThat(doc1).isNotEqualTo(doc4);
		assertThat(doc1.hashCode()).isNotEqualTo(doc4.hashCode());
	}

	@Test
	void testToString() {
		// Test toString method
		Document document = new Document(TEST_ID, "test content", createMetadata());
		Double score = 0.95;

		DocumentWithScore documentWithScore = DocumentWithScore.builder()
			.withDocument(document)
			.withScore(score)
			.build();

		String toString = documentWithScore.toString();

		// Verify toString contains essential information
		assertThat(toString).contains("DocumentWithScore").contains("score=" + score).contains("document=" + document);
	}

	@Test
	void testSettersAndGetters() {
		// Test setters and getters
		DocumentWithScore documentWithScore = new DocumentWithScore();
		Document document = new Document(TEST_ID, "test content", createMetadata());
		Double score = 0.95;
		RerankResultMetadata metadata = new RerankResultMetadata();

		documentWithScore.setDocument(document);
		documentWithScore.setScore(score);
		documentWithScore.setMetadata(metadata);

		assertThat(documentWithScore.getOutput()).isEqualTo(document);
		assertThat(documentWithScore.getScore()).isEqualTo(score);
		assertThat(documentWithScore.getMetadata()).isEqualTo(metadata);
	}

	private Map<String, Object> createMetadata() {
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("key1", "value1");
		metadata.put("key2", "value2");
		return metadata;
	}

}

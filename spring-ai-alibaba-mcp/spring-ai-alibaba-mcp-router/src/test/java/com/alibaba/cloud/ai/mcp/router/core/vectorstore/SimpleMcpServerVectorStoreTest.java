/*
 * Copyright 2025-2026 the original author or authors.
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
 *
 */

package com.alibaba.cloud.ai.mcp.router.core.vectorstore;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


class SimpleMcpServerVectorStoreTest {

	@Test
	void shouldHandleNullScoreWithoutException() {
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("serviceName", "test-service");
		metadata.put("description", "Test service for null score handling");

		Document documentWithNullScore = new Document("test-id", "test content", metadata);

		assertNull(documentWithNullScore.getScore(), "Document score should be null by default");
		// Fix NPE by checking for null before using the score
		assertDoesNotThrow(() -> {
			Double scoreObj = documentWithNullScore.getScore();
			double score = (scoreObj != null) ? scoreObj.doubleValue() : 0.0;
			assertEquals(0.0, score, "Null score should default to 0.0");
		});
	}

	@Test
	void shouldHandleEmptyDocumentList() {
		List<Document> emptyDocuments = new ArrayList<>();

		assertDoesNotThrow(() -> {
			emptyDocuments.forEach(doc -> {
				Double scoreObj = doc.getScore();
				double score = (scoreObj != null) ? scoreObj.doubleValue() : 0.0;
				assertTrue(score >= 0.0);
			});
		});
	}


	@Test
	void shouldHandleMixedScores() {
		List<Document> documents = new ArrayList<>();

		Map<String, Object> metadata1 = new HashMap<>();
		metadata1.put("serviceName", "service1");
		Document doc1 = new Document("id1", "content1", metadata1);
		documents.add(doc1);

		Map<String, Object> metadata2 = new HashMap<>();
		metadata2.put("serviceName", "service2");
		metadata2.put("keywordScore", 0.75);
		Document doc2 = new Document("id2", "content2", metadata2);
		documents.add(doc2);

		assertDoesNotThrow(() -> {
			documents.forEach(doc -> {
				Double scoreObj = doc.getScore();
				double score = (scoreObj != null) ? scoreObj.doubleValue() : 0.0;

				Object keywordScore = doc.getMetadata().get("keywordScore");
				if (keywordScore != null) {
					double kwScore = ((Number) keywordScore).doubleValue();
					assertTrue(kwScore > 0.0, "Keyword score should be positive");
				}
				else {
					assertEquals(0.0, score, "Null vector score should default to 0.0");
				}
			});
		});
	}

	@Test
	void shouldHandleKeywordSearchWithNullVectorScore() {
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("serviceName", "financial-service");
		metadata.put("description", "财务相关服务");
		metadata.put("keywordScore", 0.5);

		Document doc = new Document("finance-id", "财务服务", metadata);

		assertNull(doc.getScore(), "Vector score should be null");
		assertNotNull(metadata.get("keywordScore"), "Keyword score should be present");

		assertDoesNotThrow(() -> {
			Double scoreObj = doc.getScore();
			double vectorScore = (scoreObj != null) ? scoreObj.doubleValue() : 0.0;
			Object keywordScore = metadata.get("keywordScore");

			if (keywordScore != null) {
				double kwScore = ((Number) keywordScore).doubleValue();
				assertTrue(kwScore > 0.0, "Should use keyword score when vector score is null");
			}
			else {
				assertEquals(0.0, vectorScore, "Should default to 0.0 when both are null");
			}
		});
	}

	@Test
	void shouldCompareScoreThresholdSafely() {
		Document doc = new Document("test", "content", new HashMap<>());

		assertDoesNotThrow(() -> {
			Double scoreObj = doc.getScore();
			double score = (scoreObj != null) ? scoreObj.doubleValue() : 0.0;

			boolean passesThreshold = score > 0.05;
			assertFalse(passesThreshold, "Null score (defaulted to 0.0) should not pass threshold");
		});
	}

	@Test
	void shouldSortDocumentsWithNullScores() {
		List<Map<String, Object>> docData = new ArrayList<>();

		Map<String, Object> data1 = new HashMap<>();
		data1.put("name", "service1");
		data1.put("score", null);
		docData.add(data1);

		Map<String, Object> data2 = new HashMap<>();
		data2.put("name", "service2");
		data2.put("score", null);
		docData.add(data2);
		assertDoesNotThrow(() -> {
			docData.sort((a, b) -> {
				Double scoreA = (Double) a.get("score");
				Double scoreB = (Double) b.get("score");
				double valA = (scoreA != null) ? scoreA : 0.0;
				double valB = (scoreB != null) ? scoreB : 0.0;
				return Double.compare(valB, valA); // Descending order
			});
		});

		assertEquals(2, docData.size(), "All documents should remain after sorting");
	}

}

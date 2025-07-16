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
package com.alibaba.cloud.ai.reader.arxiv;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.springframework.ai.document.Document;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for arXiv document reader
 *
 * @author brianxiadong
 */

@DisabledIf("GithubCI")
public class ArxivDocumentReaderTest {

	private static final String TEST_QUERY = "cat:cs.AI AND ti:\"artificial intelligence\"";

	private static final int MAX_SIZE = 2;

	/**
	 * Check if the tests are running in Local. In GitHub CI environment, this test not
	 * running.
	 */
	static boolean GithubCI() {
		return "true".equals(System.getenv("ENABLE_TEST_CI"));
	}

	@Test
	public void testDocumentReader() {
		// Create document reader
		ArxivDocumentReader reader = new ArxivDocumentReader(TEST_QUERY, MAX_SIZE);

		// Get documents
		List<Document> documents = reader.get();

		// Verify results
		assertFalse(documents.isEmpty(), "Should return at least one document");

		// Verify metadata of the first document
		Document firstDoc = documents.get(0);
		assertNotNull(firstDoc.getText(), "Document content should not be null");

		// Verify metadata
		var metadata = firstDoc.getMetadata();
		assertNotNull(metadata.get(ArxivResource.ENTRY_ID), "Should contain article ID");
		assertNotNull(metadata.get(ArxivResource.TITLE), "Should contain title");
		assertNotNull(metadata.get(ArxivResource.AUTHORS), "Should contain authors");
		assertNotNull(metadata.get(ArxivResource.SUMMARY), "Should contain summary");
		assertNotNull(metadata.get(ArxivResource.CATEGORIES), "Should contain categories");
		assertNotNull(metadata.get(ArxivResource.PRIMARY_CATEGORY), "Should contain primary category");
		assertNotNull(metadata.get(ArxivResource.PDF_URL), "Should contain PDF URL");

		// Verify categories
		@SuppressWarnings("unchecked")
		List<String> categories = (List<String>) metadata.get(ArxivResource.CATEGORIES);
		assertTrue(categories.contains("cs.AI"), "Should contain cs.AI category");
	}

	@Test
	public void testGetSummaries() {
		// Create document reader
		ArxivDocumentReader reader = new ArxivDocumentReader(TEST_QUERY, MAX_SIZE);

		// Get summary documents
		List<Document> documents = reader.getSummaries();

		// Verify results
		assertFalse(documents.isEmpty(), "Should return at least one document");

		// Verify the first document
		Document firstDoc = documents.get(0);

		// Verify content (summary)
		assertNotNull(firstDoc.getText(), "Document content (summary) should not be null");
		assertFalse(firstDoc.getText().trim().isEmpty(), "Document content (summary) should not be empty string");

		// Verify metadata
		var metadata = firstDoc.getMetadata();
		assertNotNull(metadata.get(ArxivResource.ENTRY_ID), "Should contain article ID");
		assertNotNull(metadata.get(ArxivResource.TITLE), "Should contain title");
		assertNotNull(metadata.get(ArxivResource.AUTHORS), "Should contain authors");
		assertNotNull(metadata.get(ArxivResource.SUMMARY), "Should contain summary");
		assertNotNull(metadata.get(ArxivResource.CATEGORIES), "Should contain categories");
		assertNotNull(metadata.get(ArxivResource.PRIMARY_CATEGORY), "Should contain primary category");
		assertNotNull(metadata.get(ArxivResource.PDF_URL), "Should contain PDF URL");

		// Verify summary content matches the summary in metadata
		assertEquals(firstDoc.getText(), metadata.get(ArxivResource.SUMMARY),
				"Document content should match the summary in metadata");
	}

}

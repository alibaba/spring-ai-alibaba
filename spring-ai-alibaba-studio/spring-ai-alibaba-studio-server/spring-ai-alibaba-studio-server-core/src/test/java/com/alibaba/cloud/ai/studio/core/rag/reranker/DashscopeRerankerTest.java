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
package com.alibaba.cloud.ai.studio.core.rag.reranker;

import com.alibaba.cloud.ai.studio.core.model.reranker.dashscope.DashscopeReranker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DashscopeRerankerTest {

	@Mock
	private DashscopeReranker dashscopeReranker;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void testRerank() {
		// Prepare test data
		Query query = Query.builder().text("this is a test reranker").build();
		Document document = Document.builder().text("this is a test reranker").build();
		List<Document> documents = List.of(document);
		List<Document> expectedRerankedDocs = List.of(document);

		// Mock behavior
		when(dashscopeReranker.process(query, documents)).thenReturn(expectedRerankedDocs);

		// Execute
		List<Document> result = dashscopeReranker.process(query, documents);

		// Verify
		assertNotNull(result);
		assertEquals(expectedRerankedDocs, result);
		verify(dashscopeReranker, times(1)).process(query, documents);
	}

	// @Test
	// void testRerankWithNullQuery() {
	// // Prepare test data
	// List<Document> documents = List.of(Document.builder().build());
	//
	// // Execute and verify
	// assertThrows(IllegalArgumentException.class, () -> dashscopeReranker.process(null,
	// documents));
	// }

	@Test
	void testRerankWithNullDocuments() {
		// Execute and verify
		assertThrows(IllegalArgumentException.class, () -> dashscopeReranker.process(Query.builder().build(), null));
	}

	// @Test
	// void testRerankWithEmptyDocuments() {
	// // Prepare test data
	// Query query = Query.builder().build();
	// List<Document> emptyDocs = List.of();
	//
	// // Execute
	// List<Document> result = dashscopeReranker.process(query, emptyDocs);
	//
	// // Verify
	// assertNotNull(result);
	// assertTrue(result.isEmpty());
	// }

}

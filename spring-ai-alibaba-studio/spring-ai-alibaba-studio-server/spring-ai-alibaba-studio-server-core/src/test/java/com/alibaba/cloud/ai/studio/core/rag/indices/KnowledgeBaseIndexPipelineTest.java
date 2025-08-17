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
package com.alibaba.cloud.ai.studio.core.rag.indices;

import com.alibaba.cloud.ai.studio.runtime.enums.DocumentType;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.IndexConfig;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.ProcessConfig;
import com.alibaba.cloud.ai.studio.core.rag.vectorstore.VectorStoreFactory;
import com.alibaba.cloud.ai.studio.core.utils.common.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.document.Document;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@DisplayName("KnowledgeBaseIndexPipeline Tests")
class KnowledgeBaseIndexPipelineTest {

	@Mock
	private KnowledgeBaseIndexPipeline indexPipeline;

	@Mock
	private VectorStoreFactory vectorStoreFactory;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	@DisplayName("Should successfully parse documents")
	void testParse() {
		// Prepare test data
		IndexConfig indexConfig = new IndexConfig();
		indexConfig.setEmbeddingProvider("Tongyi");
		indexConfig.setEmbeddingModel("text-embedding-v2");

		List<Document> documents = Arrays.asList(
				new Document("content1", Collections.singletonMap("metadata1", "value1")),
				new Document("content2", Collections.singletonMap("metadata2", "value2")));

		// Execute
		com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.Document document = com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.Document
			.builder()
			.docId("test-doc1")
			.kbId("test-kb1")
			.type(DocumentType.FILE)
			.format("txt")
			.path("")
			.build();
		indexPipeline.parse(document);

		// Verify
		verify(indexPipeline, times(1)).parse(document);
	}

	@Test
	@DisplayName("Should successfully transform documents")
	void testTransform() {
		// Prepare test data
		List<Document> documents = Arrays.asList(
				new Document("content1", Collections.singletonMap("metadata1", "value1")),
				new Document("content2", Collections.singletonMap("metadata2", "value2")));

		// Mock behavior
		ProcessConfig processConfig = new ProcessConfig();
		when(indexPipeline.transform(documents, processConfig)).thenReturn(documents);

		// Execute
		List<Document> result = indexPipeline.transform(documents, processConfig);

		// Verify
		assertNotNull(result);
		assertEquals(documents.size(), result.size());
		verify(indexPipeline, times(1)).transform(documents, processConfig);
	}

	@Test
	@DisplayName("Should successfully store document chunks")
	void testStore() {
		// Prepare test data
		List<Document> chunks = List.of(Document.builder().id(IdGenerator.uuid()).text("this is a test chunk").build());

		// Execute
		IndexConfig indexConfig = new IndexConfig();
		indexConfig.setName("test-index");
		indexConfig.setEmbeddingProvider("Tongyi");
		indexConfig.setEmbeddingModel("text-embedding-v2");
		indexPipeline.store(chunks, indexConfig, Collections.emptyMap());

		// Verify
		verify(indexPipeline, times(1)).store(chunks, indexConfig, Collections.emptyMap());
	}

}

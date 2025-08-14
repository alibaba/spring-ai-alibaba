/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.studio.core.base.manager;

import com.alibaba.cloud.ai.studio.runtime.domain.app.FileSearchOptions;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.DocumentChunk;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.KnowledgeBase;
import com.alibaba.cloud.ai.studio.core.model.llm.ModelFactory;
import com.alibaba.cloud.ai.studio.core.rag.KnowledgeBaseService;
import com.alibaba.cloud.ai.studio.core.rag.retriever.KnowledgeBaseDocumentRetriever;
import com.alibaba.cloud.ai.studio.core.rag.vectorstore.VectorStoreFactory;
import com.alibaba.cloud.ai.studio.core.rag.DocumentChunkConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Manager class for document retrieval operations. Handles the creation and execution of
 * document retrievers for knowledge bases.
 *
 * @since 1.0.0.3
 */
@Component
@RequiredArgsConstructor
public class DocumentRetrieverManager {

	/** Factory for creating vector stores */
	private final VectorStoreFactory vectorStoreFactory;

	/** Factory for creating AI models */
	private final ModelFactory modelFactory;

	/** Service for managing knowledge bases */
	private final KnowledgeBaseService knowledgeBaseService;

	/**
	 * Creates a document retriever for the specified search options.
	 * @param searchOptions Options for file search
	 * @return Configured document retriever
	 */
	public DocumentRetriever getDocumentRetriever(FileSearchOptions searchOptions) {
		List<KnowledgeBase> knowledgeBases = knowledgeBaseService.listKnowledgeBases(searchOptions.getKbIds());
		return new KnowledgeBaseDocumentRetriever(knowledgeBases, vectorStoreFactory, modelFactory, searchOptions);
	}

	/**
	 * Retrieves document chunks based on the query and search options.
	 * @param query Search query
	 * @param searchOptions Options for file search
	 * @return List of retrieved document chunks
	 */
	public List<DocumentChunk> retrieve(Query query, FileSearchOptions searchOptions) {
		DocumentRetriever documentRetriever = getDocumentRetriever(searchOptions);
		List<Document> documents = documentRetriever.retrieve(query);
		return documents.stream().map(DocumentChunkConverter::toDocumentChunk).toList();
	}

}

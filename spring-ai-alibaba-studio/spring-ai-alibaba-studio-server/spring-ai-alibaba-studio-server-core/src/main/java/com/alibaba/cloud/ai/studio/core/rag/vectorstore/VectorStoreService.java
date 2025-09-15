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

package com.alibaba.cloud.ai.studio.core.rag.vectorstore;

import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.DocumentChunk;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.IndexConfig;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

/**
 * Service interface for managing vector stores and document chunks. Provides operations
 * for index management and document chunk operations.
 *
 * @since 1.0.0.3
 */
public interface VectorStoreService {

	/**
	 * Creates a new vector store index with the specified configuration.
	 * @param indexConfig Configuration for the new index
	 */
	void createIndex(IndexConfig indexConfig);

	/**
	 * Deletes an existing vector store index.
	 * @param indexConfig Configuration of the index to delete
	 */
	void deleteIndex(IndexConfig indexConfig);

	/**
	 * Retrieves a vector store instance for the specified index.
	 * @param indexConfig Configuration of the index
	 * @return VectorStore instance
	 */
	VectorStore getVectorStore(IndexConfig indexConfig);

	/**
	 * Lists document chunks based on search criteria.
	 * @param indexConfig Configuration of the index
	 * @param searchRequest Search parameters
	 * @return Paged list of document chunks
	 */
	PagingList<DocumentChunk> listDocumentChunks(IndexConfig indexConfig, SearchRequest searchRequest);

	/**
	 * Updates multiple document chunks in the vector store.
	 * @param indexConfig Configuration of the index
	 * @param chunks List of document chunks to update
	 */
	void updateDocumentChunks(IndexConfig indexConfig, List<DocumentChunk> chunks);

	/**
	 * Updates the enabled status of specified document chunks.
	 * @param indexConfig Configuration of the index
	 * @param chunkIds List of chunk IDs to update
	 * @param enabled New enabled status
	 */
	void updateDocumentChunkStatus(IndexConfig indexConfig, List<String> chunkIds, boolean enabled);

}

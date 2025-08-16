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

package com.alibaba.cloud.ai.studio.core.rag;

import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.CreateDocumentRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.DeleteChunkRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.DeleteDocumentRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.Document;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.DocumentChunk;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.DocumentQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.IndexDocumentRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.KnowledgeBase;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.UpdateChunkRequest;
import com.alibaba.cloud.ai.studio.runtime.enums.DocumentIndexStatus;
import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;

import java.util.List;

/**
 * Service interface for managing documents in the knowledge base. Handles document
 * creation, indexing, and chunk management operations.
 *
 * @since 1.0.0.3
 */

public interface DocumentService {

	/**
	 * Creates documents in the knowledge base and initiates async indexing
	 * @param request Document creation request
	 * @return List of created document IDs
	 */
	List<String> createDocuments(CreateDocumentRequest request);

	/**
	 * Retrieves a document by its ID
	 * @param docId Document ID
	 * @return Document information
	 */
	Document getDocument(String docId);

	/**
	 * Lists documents with pagination
	 * @param kbId Knowledge base ID
	 * @param query Pagination query parameters
	 * @return Paginated list of documents
	 */
	PagingList<Document> listDocuments(String kbId, DocumentQuery query);

	/**
	 * Updates document information
	 * @param document Document to update
	 */
	void updateDocument(Document document);

	/**
	 * Updates document enabled status and related chunks
	 * @param docId Document ID
	 * @param enabled Enable/disable flag
	 */
	void updateDocumentEnabledStatus(String docId, Boolean enabled);

	/**
	 * Updates document indexing status
	 * @param docId Document ID
	 * @param indexStatus New index status
	 */
	void updateDocumentIndexStatus(String docId, DocumentIndexStatus indexStatus);

	/**
	 * Deletes documents and their associated chunks
	 * @param request Document deletion request
	 */
	void deleteDocuments(DeleteDocumentRequest request);

	/**
	 * Retrieves knowledge base information for a document
	 * @param docId Document ID
	 * @return Knowledge base information
	 */
	KnowledgeBase getKnowledgeBase(String docId);

	/**
	 * Creates a document chunk
	 * @param chunk Chunk information
	 * @return Created chunk ID
	 */
	String createDocumentChunk(DocumentChunk chunk);

	/**
	 * Updates document chunk information
	 * @param chunk Chunk to update
	 */
	void updateDocumentChunk(DocumentChunk chunk);

	/**
	 * Deletes document chunks
	 * @param request Chunk deletion request
	 */
	void deleteDocumentChunks(DeleteChunkRequest request);

	/**
	 * Deletes chunks associated with specified documents
	 * @param kbId Knowledge base ID
	 * @param docIds List of document IDs
	 */
	void deleteChunksByDocId(String kbId, List<String> docIds);

	/**
	 * Lists document chunks with pagination
	 * @param docId Document ID
	 * @param query Pagination query parameters
	 * @return Paginated list of document chunks
	 */
	PagingList<DocumentChunk> listDocumentChunks(String docId, BaseQuery query);

	/**
	 * Previews document chunks before indexing
	 * @param request Document indexing request
	 * @return List of preview chunks
	 */
	List<DocumentChunk> previewDocumentChunks(IndexDocumentRequest request);

	/**
	 * Re-indexes a document
	 * @param request Document indexing request
	 */
	void reIndexDocument(IndexDocumentRequest request);

	/**
	 * Updates chunk enabled status
	 * @param request Chunk update request
	 */
	void updateChunkEnabledStatus(UpdateChunkRequest request);

}

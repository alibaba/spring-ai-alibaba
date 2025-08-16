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

/**
 * Constants for RAG (Retrieval-Augmented Generation) service.
 *
 * @since 1.0.0.3
 */
public interface RagConstants {

	/** Field name for text content */
	String TEXT_FIELD = "content";

	/** Field name for vector embeddings */
	String VECTOR_FIELD = "embedding";

	/** Key for document ID */
	String KEY_DOC_ID = "doc_id";

	/** Key for document name */
	String KEY_DOC_NAME = "doc_name";

	/** Key for document title */
	String KEY_TITLE = "title";

	/** Key for workspace ID */
	String KEY_WORKSPACE_ID = "workspace_id";

	/** Key for enabled status */
	String KEY_ENABLED = "enabled";

	/** Key for chunk index */
	String KEY_CHUNK_INDEX = "index";

	/** Default dimension for vector embeddings */
	int DEFAULT_DIMENSION = 1536;

	/** Search timeout in seconds */
	int SEARCH_TIMEOUT = 30;

	/** Placeholder for documents */
	String DOCUMENTS_PLACEHOLDER = "documents";

	/** File search call identifier */
	String FILE_SEARCH_CALL = "file_search_call";

	/** File search result identifier */
	String FILE_SEARCH_RESULT = "file_search_result";

	/** Request context identifier */
	String REQUEST_CONTEXT = "request_context";

}

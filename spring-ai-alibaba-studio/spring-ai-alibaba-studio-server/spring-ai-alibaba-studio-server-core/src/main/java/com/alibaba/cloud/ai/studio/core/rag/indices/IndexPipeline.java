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

package com.alibaba.cloud.ai.studio.core.rag.indices;

import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.IndexConfig;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.ProcessConfig;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

/**
 * Interface for document indexing pipeline. Defines the core operations for processing
 * and storing documents in a RAG system.
 *
 * @version 1.0.0
 * @since jdk8
 */
public interface IndexPipeline {

	/**
	 * Parses a document into a list of Document objects.
	 * @param document The input document to be parsed
	 * @return List of parsed Document objects
	 */
	List<Document> parse(com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.Document document);

	/**
	 * Transforms a list of documents according to the processing configuration.
	 * @param documents List of documents to transform
	 * @param processConfig Configuration for document processing
	 * @return List of transformed documents
	 */
	List<Document> transform(List<Document> documents, ProcessConfig processConfig);

	/**
	 * Stores the processed document chunks in the index.
	 * @param chunks List of document chunks to store
	 * @param indexConfig Configuration for index storage
	 * @param metadata Additional metadata for storage
	 */
	void store(List<Document> chunks, IndexConfig indexConfig, Map<String, Object> metadata);

}

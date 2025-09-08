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

package com.alibaba.cloud.ai.example.deepresearch.rag.core;

import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;

import java.util.List;
import java.util.Map;

/**
 * A unified RAG processor interface that supports pre-processing and post-processing logic, hybrid querying, and filter expressions.
 *
 * @author hupei
 */
public interface HybridRagProcessor {

	/**
	 * Executes the complete RAG processing pipeline
	 * @param query The original query
	 * @param options Configuration parameters containing context information such as session_id, user_id
	 * @return Processed document list
	 */
	List<Document> process(Query query, Map<String, Object> options);

	/**
	 * Query pre-processing: query expansion, translation, etc.
	 * @param query The original query
	 * @param options Configuration parameters
	 * @return Processed query list
	 */
	List<Query> preProcess(Query query, Map<String, Object> options);

	/**
	 * Executes hybrid retrieval (supports ES hybrid query and vector search)
	 * @param queries Processed query list
	 * @param filterExpression Filter expression consistent with VectorStoreDataIngestionService's metadata logic
	 * @param options Configuration parameters
	 * @return Retrieved document list
	 */
	List<Document> hybridRetrieve(List<Query> queries,
			co.elastic.clients.elasticsearch._types.query_dsl.Query filterExpression, Map<String, Object> options);

	/**
	 * Document post-processing: relevance sorting, deduplication, compression, etc.
	 * @param documents Retrieved document list
	 * @param options Configuration parameters
	 * @return Post-processed document list
	 */
	List<Document> postProcess(List<Document> documents, Map<String, Object> options);

	/**
	 * Constructs ES filter expressions based on metadata context
	 * @param options Configuration parameters including session_id, user_id, source_type, etc.
	 * @return ES filter query object
	 */
	co.elastic.clients.elasticsearch._types.query_dsl.Query buildFilterExpression(Map<String, Object> options);

}

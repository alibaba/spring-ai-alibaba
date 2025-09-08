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
package com.alibaba.cloud.ai.example.deepresearch.rag.strategy;

import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

/**
 * Defines a strategy interface for retrieving documents from data sources.
 * Each data source (e.g., user-uploaded files, professional knowledge bases)
 * should have its specific implementation.
 */
public interface RetrievalStrategy {

	/**
	 * Retrieves relevant documents from a specific data source based on the query and options.
	 * @param query The user's query string.
	 * @param options A map containing additional parameters such as session_id, user_id, etc.,
	 *                used for context filtering.
	 * @return A list of relevant documents.
	 */
	List<Document> retrieve(String query, Map<String, Object> options);

	/**
	 * Returns the unique name of this strategy, used for identification and selection in configuration.
	 * @return The strategy name.
	 */
	String getStrategyName();
}

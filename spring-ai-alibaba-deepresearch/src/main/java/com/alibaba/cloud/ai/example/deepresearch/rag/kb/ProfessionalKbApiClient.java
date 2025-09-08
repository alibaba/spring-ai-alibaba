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

package com.alibaba.cloud.ai.example.deepresearch.rag.kb;

import com.alibaba.cloud.ai.example.deepresearch.rag.kb.model.KbSearchResult;

import java.util.List;
import java.util.Map;

/**
 * Professional Knowledge Base API Client Interface
 *
 * @author hupei
 */
public interface ProfessionalKbApiClient {

	/**
	 * Searches the knowledge base
	 * @param query The search query text
	 * @param options Configuration parameters that may include maxResults, timeout, etc.
	 * @return List of search results
	 */
	List<KbSearchResult> search(String query, Map<String, Object> options);

	/**
	 * Retrieves the supported provider types
	 * @return Provider types such as "dashscope", "custom", etc.
	 */
	String getProvider();

	/**
	 * Checks whether the client is properly configured
	 * @return Availability status
	 */
	boolean isAvailable();

}

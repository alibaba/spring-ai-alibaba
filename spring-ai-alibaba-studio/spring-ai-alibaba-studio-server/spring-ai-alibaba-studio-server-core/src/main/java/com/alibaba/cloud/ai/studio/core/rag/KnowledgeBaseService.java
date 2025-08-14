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

import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.KnowledgeBase;

import java.util.List;

/**
 * Service interface for managing knowledge bases. Provides CRUD operations and query
 * capabilities for knowledge base resources.
 *
 * @since 1.0.0.3
 */
public interface KnowledgeBaseService {

	/**
	 * Creates a new knowledge base.
	 * @param kb The knowledge base to create
	 * @return The ID of the created knowledge base
	 */
	String createKnowledgeBase(KnowledgeBase kb);

	/**
	 * Retrieves a knowledge base by its ID.
	 * @param kbId The ID of the knowledge base to retrieve
	 * @return The knowledge base instance
	 */
	KnowledgeBase getKnowledgeBase(String kbId);

	/**
	 * Lists knowledge bases with pagination support.
	 * @param query The query parameters including pagination info
	 * @return A paged list of knowledge bases
	 */
	PagingList<KnowledgeBase> listKnowledgeBases(BaseQuery query);

	/**
	 * Updates an existing knowledge base.
	 * @param kb The knowledge base to update
	 */
	void updateKnowledgeBase(KnowledgeBase kb);

	/**
	 * Deletes a knowledge base by its ID.
	 * @param kbId The ID of the knowledge base to delete
	 */
	void deleteKnowledgeBase(String kbId);

	/**
	 * Retrieves multiple knowledge bases by their IDs.
	 * @param kbIds List of knowledge base IDs to retrieve
	 * @return List of knowledge base instances
	 */
	List<KnowledgeBase> listKnowledgeBases(List<String> kbIds);

}

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

import com.alibaba.cloud.ai.example.deepresearch.rag.SourceTypeEnum;
import com.alibaba.cloud.ai.example.deepresearch.rag.core.HybridRagProcessor;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "spring.ai.alibaba.deepresearch.rag", name = "enabled", havingValue = "true")
public class UserFileRetrievalStrategy implements RetrievalStrategy {

	private final HybridRagProcessor hybridRagProcessor;

	public UserFileRetrievalStrategy(HybridRagProcessor hybridRagProcessor) {
		this.hybridRagProcessor = hybridRagProcessor;
	}

	@Override
	public String getStrategyName() {
		return "userFile";
	}

	@Override
	public List<Document> retrieve(String query, Map<String, Object> options) {
		String sessionId = (String) options.get("session_id");
		if (sessionId == null || sessionId.isBlank()) {
			// If no session_id is provided, this strategy should not return any results
			return List.of();
		}

		// Build context options for user file retrieval,
		// consistent with VectorStoreDataIngestionService metadata logic
		Map<String, Object> ragOptions = new HashMap<>(options);
		ragOptions.put("source_type", SourceTypeEnum.USER_UPLOAD.getValue());
		ragOptions.put("session_id", sessionId);

		// Use unified RAG processor to execute the complete processing pipeline
		Query ragQuery = new Query(query);
		return hybridRagProcessor.process(ragQuery, ragOptions);
	}

}

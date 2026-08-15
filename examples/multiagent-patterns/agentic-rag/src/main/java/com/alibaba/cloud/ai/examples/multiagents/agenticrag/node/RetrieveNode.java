/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.multiagents.agenticrag.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

/**
 * Retrieves documents from the vector store. Uses the refined {@code search_query}
 * when available (set by the quality gate on a retry), otherwise the original question.
 */
public class RetrieveNode implements NodeAction {

	private static final int TOP_K = 3;

	private final VectorStore vectorStore;

	public RetrieveNode(VectorStore vectorStore) {
		this.vectorStore = vectorStore;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		String query = state.value("search_query")
				.map(Object::toString)
				.orElse(state.value("question").map(Object::toString).orElse(""));
		List<Document> docs = vectorStore.similaritySearch(SearchRequest.builder().query(query).topK(TOP_K).build());
		List<String> contents = docs.stream().map(Document::getText).toList();
		int rounds = state.value("retry_count")
				.map(Object::toString)
				.map(Integer::parseInt)
				.orElse(0) + 1;
		return Map.of("documents", contents, "retrieval_rounds", rounds);
	}
}

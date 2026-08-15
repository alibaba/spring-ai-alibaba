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
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;

/**
 * Quality gate: verifies that the answer is complete and grounded in the retrieved
 * context. When the answer is incomplete, refines the search query and routes back to
 * the {@code retrieve} node, up to {@link #MAX_RETRIES} rounds.
 */
public class CheckNode implements NodeAction {

	private static final int MAX_RETRIES = 2;

	private final ChatModel chatModel;

	private final String promptTemplate;

	public CheckNode(ChatModel chatModel, String promptTemplate) {
		this.chatModel = chatModel;
		this.promptTemplate = promptTemplate;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		int retryCount = state.value("retry_count")
				.map(Object::toString)
				.map(Integer::parseInt)
				.orElse(0);
		if (retryCount >= MAX_RETRIES) {
			return Map.of("route", "done");
		}

		String question = state.value("question").map(Object::toString).orElse("");
		String answer = state.value("final_answer").map(Object::toString).orElse("");
		@SuppressWarnings("unchecked")
		List<String> docs = (List<String>) state.value("documents").orElse(List.of());

		// Direct answers (no retrieval) are final: the quality gate only applies to
		// answers grounded in retrieved context.
		if (docs.isEmpty()) {
			return Map.of("route", "done");
		}

		String context = String.join("\n\n", docs);

		String prompt = promptTemplate.formatted(question, context, answer);
		String response = chatModel.call(new Prompt(prompt)).getResult().getOutput().getText();
		if (response != null && response.trim().toUpperCase().startsWith("INCOMPLETE")) {
			String improvedQuery = extractImprovedQuery(response, question);
			return Map.of("route", "retry", "search_query", improvedQuery, "retry_count", retryCount + 1);
		}
		return Map.of("route", "done");
	}

	/**
	 * The model replies with {@code INCOMPLETE} followed by a refined search query on
	 * the next non-empty line. Falls back to the original question when parsing fails.
	 */
	private String extractImprovedQuery(String response, String fallbackQuery) {
		for (String line : response.split("\n")) {
			String trimmed = line.trim();
			if (!trimmed.isEmpty() && !trimmed.equalsIgnoreCase("INCOMPLETE")) {
				return trimmed;
			}
		}
		return fallbackQuery;
	}
}

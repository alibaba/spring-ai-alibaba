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
package com.alibaba.cloud.ai.examples.multiagents.routing.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Preprocessing node: query enrichment, validation, metadata.
 * Converts raw input to messages and adds trace metadata for the routing pipeline.
 */
public class PreprocessNode implements NodeAction {

	private static final Logger log = LoggerFactory.getLogger(PreprocessNode.class);

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		String input = state.value("input")
				.map(Object::toString)
				.orElse(state.value("query").map(Object::toString).orElse(""));

		// Validation: reject empty or too short queries
		if (input == null || input.isBlank()) {
			throw new IllegalArgumentException("Query cannot be empty");
		}
		if (input.length() < 3) {
			throw new IllegalArgumentException("Query too short for meaningful routing");
		}

		// Enrichment: normalize and trim
		String enrichedQuery = input.trim();
		if (enrichedQuery.length() > 2000) {
			enrichedQuery = enrichedQuery.substring(0, 2000) + "...";
			log.debug("Query truncated to 2000 chars");
		}

		// Metadata for observability
		String traceId = UUID.randomUUID().toString();
		String timestamp = Instant.now().toString();

		List<org.springframework.ai.chat.messages.Message> messages = List.of(new UserMessage(enrichedQuery));

		log.debug("Preprocess: traceId={}, enriched query length={}", traceId, enrichedQuery.length());

		return Map.of(
				"input", enrichedQuery,
				"messages", messages,
				"preprocess_metadata", Map.of(
						"traceId", traceId,
						"timestamp", timestamp,
						"originalLength", input.length()));
	}
}

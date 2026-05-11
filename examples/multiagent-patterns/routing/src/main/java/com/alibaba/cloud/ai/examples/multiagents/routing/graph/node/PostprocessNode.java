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

import com.alibaba.cloud.ai.graph.agent.flow.node.RoutingMergeNode;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

/**
 * Postprocessing node: formatting, logging, metadata.
 * Wraps the merged routing result with metadata and produces the final answer.
 */
public class PostprocessNode implements NodeAction {

	private static final Logger log = LoggerFactory.getLogger(PostprocessNode.class);

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		String mergedResult = state.value(RoutingMergeNode.DEFAULT_MERGED_OUTPUT_KEY)
				.map(Object::toString)
				.orElse("No result from routing.");

		@SuppressWarnings("unchecked")
		Map<String, Object> preprocessMeta = (Map<String, Object>) state.value("preprocess_metadata")
				.orElse(Map.of());

		String traceId = (String) preprocessMeta.getOrDefault("traceId", "unknown");
		String timestamp = Instant.now().toString();

		// Format final answer with metadata header
		String formatted = String.format("""
				--- Answer (traceId=%s) ---
				%s
				---
				Generated at: %s
				""", traceId, mergedResult, timestamp);

		log.info("Postprocess: traceId={}, result length={}", traceId, mergedResult.length());

		return Map.of(
				"final_answer", formatted,
				"postprocess_metadata", Map.of(
						"traceId", traceId,
						"timestamp", timestamp,
						"resultLength", mergedResult.length()));
	}
}

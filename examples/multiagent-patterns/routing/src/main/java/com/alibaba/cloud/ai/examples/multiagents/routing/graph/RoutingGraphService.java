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
package com.alibaba.cloud.ai.examples.multiagents.routing.graph;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

/**
 * Service that invokes the routing graph: preprocess → routing (LlmRoutingAgent with merge) → postprocess.
 */
public class RoutingGraphService {

	private static final Logger log = LoggerFactory.getLogger(RoutingGraphService.class);

	private final CompiledGraph routingGraph;

	public RoutingGraphService(CompiledGraph routingGraph) {
		this.routingGraph = routingGraph;
	}

	/**
	 * Run the full pipeline: preprocess → routing (with merge) → postprocess.
	 */
	public RoutingGraphResult run(String query) throws GraphRunnerException {
		Map<String, Object> inputs = Map.of("input", query);
		Optional<OverAllState> resultOpt = routingGraph.invoke(inputs);

		if (resultOpt.isEmpty()) {
			return new RoutingGraphResult(query, null, "No result from graph.");
		}

		OverAllState state = resultOpt.get();
		String finalAnswer = state.value("final_answer")
				.map(Object::toString)
				.orElse(state.value("merged_result").map(Object::toString).orElse("No result."));

		log.debug("Routing graph completed, answer length={}", finalAnswer.length());

		return new RoutingGraphResult(query, state, finalAnswer);
	}

	public record RoutingGraphResult(String query, OverAllState state, String finalAnswer) {
	}
}

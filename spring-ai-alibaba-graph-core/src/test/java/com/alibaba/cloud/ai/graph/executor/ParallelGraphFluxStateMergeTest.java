/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.executor;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Verifies that parallel streaming branches merge their {@link GraphResponse#done(Object)}
 * state updates before the converged node runs.
 */
public class ParallelGraphFluxStateMergeTest {

	@Test
	void testParallelGraphFluxDoneMapIsMergedBeforeJoinNode() throws Exception {
		CompiledGraph graph = buildGraph();

		OverAllState finalState = graph.invoke(Map.of()).orElseThrow();

		assertEquals("left", finalState.value("join_left", ""));
		assertEquals("right", finalState.value("join_right", ""));
		assertEquals(List.of("left", "right"), finalState.value("join_results", List.of()));
	}

	@Test
	void testParallelGraphFluxDoneNonMapKeepsGraphResponseState() throws Exception {
		CompiledGraph graph = buildGraphWithNonMapDoneValue();

		OverAllState finalState = graph.invoke(Map.of()).orElseThrow();

		assertEquals("left-token", finalState.value("join_left_payload", ""));
		assertEquals("right", finalState.value("join_right", ""));
	}

	private static CompiledGraph buildGraph() throws Exception {
		StateGraph stateGraph = new StateGraph(() -> {
			Map<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("left_result", new ReplaceStrategy());
			strategies.put("right_result", new ReplaceStrategy());
			strategies.put("parallel_results", new AppendStrategy());
			strategies.put("join_left", new ReplaceStrategy());
			strategies.put("join_right", new ReplaceStrategy());
			strategies.put("join_results", new ReplaceStrategy());
			return strategies;
		}).addNode("left", node_async(state -> Map.of("left_stream", Flux.just(
				GraphResponse.done(Map.of(
						"left_result", "left",
						"parallel_results", List.of("left")))))))
			.addNode("right", node_async(state -> Map.of("right_stream", Flux.just(
					GraphResponse.done(Map.of(
							"right_result", "right",
							"parallel_results", List.of("right")))))))
			.addNode("join", node_async(state -> {
				assertInstanceOf(String.class, state.value("left_result", ""));
				assertInstanceOf(String.class, state.value("right_result", ""));
				return Map.of(
						"join_left", state.value("left_result", ""),
						"join_right", state.value("right_result", ""),
						"join_results", state.value("parallel_results", List.of()));
			}))
			.addEdge(START, "left")
			.addEdge(START, "right")
			.addEdge("left", "join")
			.addEdge("right", "join")
			.addEdge("join", END);
		return stateGraph.compile();
	}

	private static CompiledGraph buildGraphWithNonMapDoneValue() throws Exception {
		StateGraph stateGraph = new StateGraph(() -> {
			Map<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("left_stream", new ReplaceStrategy());
			strategies.put("right_result", new ReplaceStrategy());
			strategies.put("join_left_payload", new ReplaceStrategy());
			strategies.put("join_right", new ReplaceStrategy());
			return strategies;
		}).addNode("left", node_async(state -> Map.of("left_stream",
				Flux.just(GraphResponse.done("left-token")))))
			.addNode("right", node_async(state -> Map.of("right_stream",
					Flux.just(GraphResponse.done(Map.of("right_result", "right"))))))
			.addNode("join", node_async(state -> {
				GraphResponse<?> leftResponse = assertInstanceOf(
						GraphResponse.class, state.value("left_stream", Object.class).orElseThrow());
				return Map.of(
						"join_left_payload", leftResponse.resultValue().orElse(""),
						"join_right", state.value("right_result", ""));
			}))
			.addEdge(START, "left")
			.addEdge(START, "right")
			.addEdge("left", "join")
			.addEdge("right", "join")
			.addEdge("join", END);
		return stateGraph.compile();
	}

}

/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Reproduces issue #4594: graph execution advances nodes by recursively
 * appending {@code concatWith(Flux.defer(...))}, so the reactive operator
 * chain grows linearly with the number of executed nodes and long-running
 * loops (e.g. many-turn conversations) blow the stack.
 *
 * @see <a href="https://github.com/alibaba/spring-ai-alibaba/issues/4594">Issue #4594</a>
 */
public class DeepLoopStackDepthTest {

	private static final int ITERATIONS = 3000;

	@Test
	public void longSelfLoopShouldCompleteWithoutStackOverflow() throws Exception {
		StateGraph graph = new StateGraph(() -> {
			Map<String, com.alibaba.cloud.ai.graph.KeyStrategy> strategies = new HashMap<>();
			strategies.put("count", new ReplaceStrategy());
			return strategies;
		}).addNode("worker", node_async(state -> {
			int count = (int) state.value("count").orElse(0);
			return Map.of("count", count + 1);
		}))
			.addEdge(START, "worker")
			.addConditionalEdges("worker",
					edge_async(state -> (int) state.value("count").orElse(0) < ITERATIONS ? "loop" : "done"),
					Map.of("loop", "worker", "done", END));

		CompiledGraph app = graph.compile();
		app.setMaxIterations(ITERATIONS + 10);

		// Before the fix the StackOverflowError was swallowed by Reactor (onErrorDropped)
		// and the stream never terminated, so guard with a timeout instead of blocking forever.
		OverAllState finalState = app.stream(Map.of("count", 0))
			.map(NodeOutput::state)
			.blockLast(java.time.Duration.ofSeconds(90));

		assertEquals(ITERATIONS, finalState.value("count").get());
	}

}

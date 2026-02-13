/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParallelMultiNodeBranchTest {

	private static KeyStrategyFactory createKeyStrategyFactory() {
		return () -> {
			HashMap<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("messages", new AppendStrategy());
			return strategies;
		};
	}

	private AsyncNodeAction makeNode(String id) {
		return node_async(state -> Map.of("messages", id));
	}

	@Test
	void testParallelBranchesWithMultipleNodesAndJoin() throws Exception {
		StateGraph graph = new StateGraph(createKeyStrategyFactory())
			.addNode("A", makeNode("A"))
			.addNode("A1", makeNode("A1"))
			.addNode("A1b", makeNode("A1b"))
			.addNode("A2", makeNode("A2"))
			.addNode("A2b", makeNode("A2b"))
			.addNode("B", makeNode("B"))
			.addNode("C", makeNode("C"))
			.addEdge(START, "A")
			.addEdge("A", "A1")
			.addEdge("A", "A2")
			.addEdge("A1", "A1b")
			.addEdge("A1b", "B")
			.addEdge("A2", "A2b")
			.addEdge("A2b", "B")
			.addEdge("B", "C")
			.addEdge("C", END);

		CompiledGraph compiled = graph.compile();

		final OverAllState[] finalState = new OverAllState[1];
		compiled.stream(Map.of())
			.map(NodeOutput::state)
			.doOnNext(s -> finalState[0] = s)
			.blockLast();

		assertTrue(finalState[0] != null);
		List<String> messages = (List<String>) finalState[0].value("messages").get();
		// Must contain all nodes
		assertTrue(messages.contains("A"));
		assertTrue(messages.containsAll(List.of("A1", "A1b", "A2", "A2b")));
		assertEquals("C", messages.get(messages.size() - 1));

		// A should be first
		assertEquals("A", messages.get(0));
		// B must occur before C
		int idxB = messages.lastIndexOf("B");
		int idxC = messages.lastIndexOf("C");
		assertTrue(idxB >= 0 && idxC > idxB);
		// Total messages: A + (A1, A1b, A2, A2b in any order) + B + C = 7
		assertEquals(7, messages.size());
	}
}


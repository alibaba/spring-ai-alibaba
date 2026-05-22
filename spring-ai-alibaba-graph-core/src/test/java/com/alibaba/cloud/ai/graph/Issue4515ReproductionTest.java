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

package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Issue4515ReproductionTest {

	private static final Logger log = LoggerFactory.getLogger(Issue4515ReproductionTest.class);

	private KeyStrategyFactory createKeyStrategyFactory() {
		return () -> {
			Map<String, KeyStrategy> keyStrategies = new HashMap<>();
			keyStrategies.put("input", new ReplaceStrategy());
			keyStrategies.put("output", new AppendStrategy());
			return keyStrategies;
		};
	}

	private CompiledGraph makeSubGraph() throws GraphStateException {
		return new StateGraph(createKeyStrategyFactory())
			.addNode("subA", node_async(state -> Map.of("output", "sub_graph_output")))
			.addEdge(START, "subA")
			.addEdge("subA", END)
			.compile();
	}

	@SuppressWarnings("rawtypes")
	private List outputOf(Optional<OverAllState> result) {
		return (List) result.orElseThrow().value("output").orElseThrow();
	}

	/**
	 * Scenario 01:
	 * START -> B(subGraph1) -> C(subGraph2) -> END.
	 *
	 * Both subgraphs are distinct compiled instances. Each subgraph contributes one
	 * "sub_graph_output", so final output size must be 2.
	 */
	@Test
	void test01_twoDistinctSubGraphsInSerial() throws Exception {
		CompiledGraph graph = new StateGraph(createKeyStrategyFactory())
			.addNode("B", makeSubGraph())
			.addNode("C", makeSubGraph())
			.addEdge(START, "B")
			.addEdge("B", "C")
			.addEdge("C", END)
			.compile();

		List<?> output = outputOf(graph.invoke(Map.of("input", "hello")));
		log.info("[spring-ai-alibaba-repro] test01 output size={}, output={}", output.size(), output);
		assertEquals(2, output.size());
	}

	/**
	 * Scenario 02:
	 * START -> B(subGraph) -> C(same subGraph instance) -> END.
	 *
	 * The same compiled subgraph instance is reused by two parent nodes. This case
	 * verifies checkpoint/threadId isolation and ensures no cross-node state pollution.
	 */
	@Test
	void test02_sameSubGraphInstanceRegisteredTwice() throws Exception {
		CompiledGraph subGraph = makeSubGraph();

		CompiledGraph graph = new StateGraph(createKeyStrategyFactory())
			.addNode("B", subGraph)
			.addNode("C", subGraph)
			.addEdge(START, "B")
			.addEdge("B", "C")
			.addEdge("C", END)
			.compile();

		List<?> output = outputOf(graph.invoke(Map.of("input", "hello")));
		log.info("[spring-ai-alibaba-repro] test02 output size={}, output={}", output.size(), output);
		assertEquals(2, output.size());
	}

	/**
	 * Scenario 03:
	 * START -> A(parent node) -> B(subGraph1) -> C(subGraph2) -> END.
	 *
	 * Parent node A contributes "parent_output" once, and the two serial subgraphs
	 * contribute two "sub_graph_output" items. Final output size must be 3.
	 */
	@Test
	void test03_parentNodePlusSerialSubGraphs() throws Exception {
		CompiledGraph graph = new StateGraph(createKeyStrategyFactory())
			.addNode("A", node_async(state -> Map.of("output", "parent_output")))
			.addNode("B", makeSubGraph())
			.addNode("C", makeSubGraph())
			.addEdge(START, "A")
			.addEdge("A", "B")
			.addEdge("B", "C")
			.addEdge("C", END)
			.compile();

		List<?> output = outputOf(graph.invoke(Map.of("input", "hello")));
		log.info("[spring-ai-alibaba-repro] test03 output size={}, output={}", output.size(), output);
		assertEquals(3, output.size());
		assertTrue(output.contains("parent_output"));
	}

}

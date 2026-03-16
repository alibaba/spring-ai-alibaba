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

import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for OverAllState serialization with subgraph execution.
 */
public class OverAllStateDeltaDataTest {

	private static final Logger log = LoggerFactory.getLogger(OverAllStateDeltaDataTest.class);

	private static final String KEY_OUTPUT = "output";

	/**
	 * Create KeyStrategyFactory with append strategy for output key.
	 * @return KeyStrategyFactory configured with append strategy.
	 */
	private KeyStrategyFactory createKeyStrategyFactory() {
		return () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put(KEY_OUTPUT, new AppendStrategy());
			return keyStrategyMap;
		};
	}

	/**
	 * Create a simple async node that returns a map with the given ID as value for
	 * the output key.
	 * @param id The identifier for the node action.
	 * @return An AsyncNodeAction producing a map with the message ID.
	 */
	private AsyncNodeAction makeNode(String id) {
		return node_async(state -> {
			log.info("Executing node: {}", id);
			return Map.of(KEY_OUTPUT, id);
		});
	}

	/**
	 * Test serialization and execution of graph with multiple sub-graphs using append
	 * strategy.
	 */
	@Test
	public void testMarkForRemovalSerialization() throws Exception {
		KeyStrategyFactory keyStrategyFactory = createKeyStrategyFactory();

		// Build sub graph A
		StateGraph subGraphA = new StateGraph(keyStrategyFactory);
		subGraphA.addNode("a", makeNode("a"));
		subGraphA.addNode("b", makeNode("b"));
		subGraphA.addNode("c", makeNode("c"));
		subGraphA.addEdge(START, "a");
		subGraphA.addEdge("a", "b");
		subGraphA.addEdge("b", "c");
		subGraphA.addEdge("c", END);
		CompiledGraph compiledGraphA = subGraphA.compile();

		// Build sub graph B
		StateGraph subGraphB = new StateGraph(keyStrategyFactory);
		subGraphB.addNode("c", makeNode("c"));
		subGraphB.addNode("d", makeNode("d"));
		subGraphB.addEdge(START, "c");
		subGraphB.addEdge("c", "d");
		subGraphB.addEdge("d", END);
		CompiledGraph compiledGraphB = subGraphB.compile();

		// Build main graph
		StateGraph mainGraph = new StateGraph(keyStrategyFactory);
		mainGraph.addNode("a", makeNode("a"));
		mainGraph.addNode("subA", compiledGraphA);
		mainGraph.addNode("subB", compiledGraphB);
		mainGraph.addEdge(START, "a");
		mainGraph.addEdge("a", "subA");
		mainGraph.addEdge("subA", "subB");
		mainGraph.addEdge("subB", END);
		CompiledGraph compiledGraph = mainGraph.compile();

		OverAllState state = compiledGraph.invoke(Collections.emptyMap()).orElseThrow();
		List<Object> output = state.value(KEY_OUTPUT, Collections.emptyList());

		log.info("actual output size: {}, which suppose to be 6", output.size());
		log.info("actual output: {}", output);
		log.info(", which suppose to be [a, a, b, c, c, d]");

		assertNotNull(output, "Output should not be null");
		assertEquals(6, output.size(), "Output should contain 6 elements");
		assertEquals(List.of("a", "a", "b", "c", "c", "d"), output,
				"Output should be [a, a, b, c, c, d]");
	}

}

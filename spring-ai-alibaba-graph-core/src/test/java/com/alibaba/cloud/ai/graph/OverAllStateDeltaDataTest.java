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
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
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
 * Unit tests for OverAllState behavior with append strategy and subgraph execution.
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
	 * Test execution of graph with multiple sub-graphs using the append strategy.
	 */
	@Test
	public void testAppendStrategyWithSubGraphs() throws Exception {
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

		OverAllState state = compiledGraph.invoke(Map.of(KEY_OUTPUT, "A")).orElseThrow();
		List<Object> output = state.value(KEY_OUTPUT, Collections.emptyList());

		log.info("actual output size: {}, which is supposed to be 7", output.size());
		log.info("actual output: {}", output);
		log.info("expected output: [A, a, a, b, c, c, d]");

		assertNotNull(output, "Output should not be null");
		assertEquals(7, output.size(), "Output should contain 7 elements");
		assertEquals(List.of("A", "a", "a", "b", "c", "c", "d"), output,
				"Output should be [A, a, a, b, c, c, d]");

		Map<String, Object> deltaData = state.deltaData();
		assertNotNull(deltaData, "Delta data should not be null");
		List<String> deltaOutput = (List<String>) deltaData.get(KEY_OUTPUT);
		assertNotNull(deltaOutput, "Delta data should contain output key");
		assertEquals(6, deltaOutput.size(), "Delta data should contain 6 entries");
		assertEquals(List.of("a", "a", "b", "c", "c", "d"), deltaOutput,
				"Delta output should be [a, a, b, c, c, d]");
	}

	/**
	 * Test execution of graph with parallel nodes using the append strategy.
	 */
	@Test
	void testAppendStrategyWithParallelNodes() throws Exception {
		KeyStrategyFactory keyStrategyFactory = createKeyStrategyFactory();

		// Build main graph with parallel nodes
		StateGraph mainGraph = new StateGraph(keyStrategyFactory);

		mainGraph.addNode("a", makeNode("a"));
		mainGraph.addNode("b", makeNode("b"));
		mainGraph.addNode("c", makeNode("c"));

		mainGraph.addEdge(START, "a");
		mainGraph.addEdge(START, "b");
		mainGraph.addEdge("a", "c");
		mainGraph.addEdge("b", "c");
		mainGraph.addEdge("c", END);

		CompiledGraph compiledGraph = mainGraph.compile();

		OverAllState state = compiledGraph.invoke(Map.of(KEY_OUTPUT, "A")).orElseThrow();
		List<Object> output = state.value(KEY_OUTPUT, Collections.emptyList());

		assertNotNull(output, "Output should not be null");
		assertEquals(4, output.size(), "Output should contain 4 elements");
		// parallel result processing ordered as Node Registration order, which is a, b, c
		assertEquals(List.of("A", "a", "b", "c"), output,
				"Output should be [A, a, b, c]");

		assertNotNull(state.deltaData(), "Delta data should not be null");
		List<String> outPutDeltas = (List<String>) state.deltaData().get(KEY_OUTPUT);
		assertNotNull(outPutDeltas, "Delta data should contain output key");
		assertEquals(3, outPutDeltas.size(), "Delta data should contain 3 entries");
		assertEquals(List.of("a", "b", "c"), outPutDeltas,
				"Delta output should be [a, b, c]");
	}

	@Test
	void testDeltaDataWithSnapshot() throws Exception {

		final String threadId = "test-thread-1";

		KeyStrategyFactory keyStrategyFactory = createKeyStrategyFactory();

		// Build main graph
		StateGraph mainGraph = new StateGraph(keyStrategyFactory);
		mainGraph.addNode("a", makeNode("a"));
		mainGraph.addNode("b", makeNode("b"));

		mainGraph.addEdge(START, "a");
		mainGraph.addEdge("a", "b");
		mainGraph.addEdge("b", END);

		CompiledGraph compiledGraph = mainGraph.compile(
				CompileConfig.builder()
						.interruptAfter("a")
						.saverConfig(
								SaverConfig.builder()
										.register(new MemorySaver())
										.build()
						).build()
		);

		RunnableConfig runnableConfig = RunnableConfig.builder()
				.threadId(threadId)
				.build();

		// First execution to capture snapshot after node "a"
		OverAllState state = compiledGraph.invoke(Map.of(KEY_OUTPUT, "A"), runnableConfig).orElseThrow();
		List<Object> output = state.value(KEY_OUTPUT, Collections.emptyList());
		log.info("Output after first execution: {}", output);
		assertNotNull(output, "Output should not be null");
		assertEquals(2, output.size(), "Output should contain 2 elements");
		assertEquals(List.of("A", "a"), output,
				"Output should be [A, a]");

		Map<String, Object> deltaData = state.deltaData();
		assertNotNull(deltaData, "Delta data should not be null");
		List<String> deltaOutput = (List<String>) deltaData.get(KEY_OUTPUT);
		assertNotNull(deltaOutput, "Delta data should contain output key");
		assertEquals(1, deltaOutput.size(), "Delta data should contain 1 entry");
		assertEquals(List.of("a"), deltaOutput,
				"Delta output should be [a]");

		// Simulate resuming from snapshot
		StateSnapshot stateSnapshot = compiledGraph.getState(runnableConfig);
		OverAllState stateResumed = compiledGraph.invoke(Collections.emptyMap(), stateSnapshot.config()).orElseThrow();
		List<Object> outputResumed = stateResumed.value(KEY_OUTPUT, Collections.emptyList());
		log.info("Output after resuming from snapshot: {}", outputResumed);
		assertNotNull(outputResumed, "Output should not be null");
		assertEquals(3, outputResumed.size(), "Output should contain 3 elements");
		assertEquals(List.of("A", "a", "b"), outputResumed,
				"Output should be [A, a, b]");

		Map<String, Object> deltaDataResumed = stateResumed.deltaData();
		assertNotNull(deltaDataResumed, "Delta data should not be null");
		List<String> deltaOutputResumed = (List<String>) deltaDataResumed.get(KEY_OUTPUT);
		assertNotNull(deltaOutputResumed, "Delta data should contain output key");
		assertEquals(2, deltaOutputResumed.size(), "Delta data should contain 2 entry");
		assertEquals(List.of("a", "b"), deltaOutputResumed,
				"Delta output should be [a, b]");

	}

}

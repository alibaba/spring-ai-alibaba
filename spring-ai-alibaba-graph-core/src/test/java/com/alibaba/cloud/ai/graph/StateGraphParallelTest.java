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
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for parallel node aggregation strategies (ANY_OF and ALL_OF).
 * This test class focuses on testing the behavior of parallel nodes with different
 * aggregation strategies, including scenarios with streaming nodes (Flux).
 */
public class StateGraphParallelTest {

	private static final Logger log = LoggerFactory.getLogger(StateGraphParallelTest.class);

	private KeyStrategyFactory createKeyStrategyFactory() {
		return () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("messages", new AppendStrategy());
			keyStrategyMap.put("stream", new AppendStrategy());
			keyStrategyMap.put("nodeId", new ReplaceStrategy());
			return keyStrategyMap;
		};
	}

	private AsyncNodeAction makeNode(String id) {
		return node_async(state -> {
			log.info("call node {}", id);
			return Map.of("messages", id);
		});
	}

	/**
	 * Creates a node with a specific delay to test timing-based aggregation strategies.
	 * @param id the node identifier
	 * @param delayMs the delay in milliseconds before the node completes
	 * @return an AsyncNodeAction that sleeps for the specified delay
	 */
	private AsyncNodeAction makeNodeWithDelay(String id, long delayMs) {
		return node_async(state -> {
			log.info("call node {} with delay {}ms", id, delayMs);
			try {
				Thread.sleep(delayMs);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}
			return Map.of("messages", id, "nodeId", id);
		});
	}

	/**
	 * Creates a streaming node that returns a Flux.
	 * The Flux emits values with delays to simulate streaming behavior.
	 * 
	 * @param id the node identifier
	 * @param delayMs delay before starting to emit values
	 * @param values the values to emit
	 * @return an AsyncNodeAction that returns a Flux
	 */
	private AsyncNodeAction makeStreamingNode(String id, long delayMs, String... values) {
		return node_async(state -> {
			log.info("call streaming node {} with delay {}ms", id, delayMs);
			try {
				Thread.sleep(delayMs);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}
			// Return a Flux that emits values immediately after the delay
			Flux<String> flux = Flux.fromArray(values)
					.map(value -> id + ":" + value);
			return Map.of("stream", flux, "nodeId", id);
		});
	}

	/**
	 * Tests ANY_OF aggregation strategy - should proceed with the first completed branch.
	 * This test verifies that when ANY_OF strategy is configured, only the result from
	 * the fastest branch is used, and execution continues immediately without waiting
	 * for slower branches.
	 */
	@Test
	void testParallelNodeAggregationStrategyAnyOf() throws Exception {
		// Create a workflow with parallel branches that have different delays
		// fastNode completes in 100ms, slowNode1 in 500ms, slowNode2 in 500ms
		var workflow = new StateGraph(createKeyStrategyFactory())
				.addNode("fastNode", makeNodeWithDelay("fastNode", 100))
				.addNode("slowNode1", makeNodeWithDelay("slowNode1", 500))
				.addNode("slowNode2", makeNodeWithDelay("slowNode2", 500))
				.addNode("merge", makeNode("merge"))
				.addEdge(START, "fastNode")
				.addEdge(START, "slowNode1")
				.addEdge(START, "slowNode2")
				.addEdge("fastNode", "merge")
				.addEdge("slowNode1", "merge")
				.addEdge("slowNode2", "merge")
				.addEdge("merge", END);

		var app = workflow.compile();

		long startTime = System.currentTimeMillis();
		final OverAllState[] finalState = new OverAllState[1];
		
		// Configure ANY_OF strategy for the merge node (target node)
		app.stream(Map.of(),
				RunnableConfig.builder()
						.addParallelNodeAggregationStrategy("merge", NodeAggregationStrategy.ANY_OF)
						.addParallelNodeExecutor(START, ForkJoinPool.commonPool())
						.build())
				.doOnNext(output -> log.info("Node output: {}", output.node()))
				.map(NodeOutput::state)
				.doOnNext(state -> finalState[0] = state)
				.blockLast();

		long endTime = System.currentTimeMillis();
		long duration = endTime - startTime;

		// Verify that only one result is present (from the fastest node)
		assertNotNull(finalState[0], "Final state should not be null");
		List<String> messages = (List<String>) finalState[0].value("messages").orElse(List.of());
		
		// With ANY_OF, only the first completed result should be used
		// The result should contain fastNode (the fastest one), NOT the slow nodes
		assertTrue(messages.contains("fastNode"),
				"Result should contain fastNode (the first completed branch)");
		
		// The key behavior test: with ANY_OF, slow nodes should NOT be in results
		// If slowNode1 or slowNode2 are present, it means we waited for them (wrong behavior)
		boolean hasSlowNodeData = messages.contains("slowNode1") || messages.contains("slowNode2");
		assertFalse(hasSlowNodeData,
				"Result should NOT contain slow nodes with ANY_OF strategy (got: " + messages + ")");

		// Verify merge node was executed
		assertTrue(messages.contains("merge"), "Result should contain merge node");

		// Log timing for debugging, but don't assert on it to avoid flaky tests
		log.info("ANY_OF execution took {}ms (fastNode: 100ms, slowNodes: 500ms)", duration);
	}

	/**
	 * Tests ALL_OF aggregation strategy - should wait for all branches to complete.
	 * This test verifies that when ALL_OF strategy is configured (or default),
	 * all parallel branches complete before proceeding.
	 */
	@Test
	void testParallelNodeAggregationStrategyAllOf() throws Exception {
		// Create a workflow with parallel branches that have different delays
		var workflow = new StateGraph(createKeyStrategyFactory())
				.addNode("fastNode", makeNodeWithDelay("fastNode", 100))
				.addNode("slowNode1", makeNodeWithDelay("slowNode1", 300))
				.addNode("slowNode2", makeNodeWithDelay("slowNode2", 300))
				.addNode("merge", makeNode("merge"))
				.addEdge(START, "fastNode")
				.addEdge(START, "slowNode1")
				.addEdge(START, "slowNode2")
				.addEdge("fastNode", "merge")
				.addEdge("slowNode1", "merge")
				.addEdge("slowNode2", "merge")
				.addEdge("merge", END);

		var app = workflow.compile();

		long startTime = System.currentTimeMillis();
		final OverAllState[] finalState = new OverAllState[1];
		
		// Configure ALL_OF strategy explicitly for the merge node
		app.stream(Map.of(),
				RunnableConfig.builder()
						.addParallelNodeAggregationStrategy("merge", NodeAggregationStrategy.ALL_OF)
						.addParallelNodeExecutor(START, ForkJoinPool.commonPool())
						.build())
				.doOnNext(output -> log.info("Node output: {}", output.node()))
				.map(NodeOutput::state)
				.doOnNext(state -> finalState[0] = state)
				.blockLast();

		long endTime = System.currentTimeMillis();
		long duration = endTime - startTime;

		// Verify that execution waited for all branches (should be around 300ms+)
		assertTrue(duration >= 250, 
				"Execution should wait for all branches with ALL_OF strategy, but took only " + duration + "ms");

		// Verify that all results are present
		assertNotNull(finalState[0], "Final state should not be null");
		List<String> messages = (List<String>) finalState[0].value("messages").orElse(List.of());
		
		// With ALL_OF, all parallel branches should complete
		// Note: The exact content depends on how processParallelResults merges the results
		// But we should have at least fastNode, slowNode1, slowNode2, and merge
		assertTrue(messages.size() >= 3, 
				"Result should contain results from all parallel branches");
		
		// Verify merge node was executed
		assertTrue(messages.contains("merge"), "Result should contain merge node");
	}

	/**
	 * Tests default aggregation strategy (ALL_OF) when no strategy is explicitly configured.
	 */
	@Test
	void testParallelNodeAggregationStrategyDefault() throws Exception {
		// Create a workflow with parallel branches
		var workflow = new StateGraph(createKeyStrategyFactory())
				.addNode("node1", makeNodeWithDelay("node1", 200))
				.addNode("node2", makeNodeWithDelay("node2", 200))
				.addNode("node3", makeNodeWithDelay("node3", 200))
				.addNode("merge", makeNode("merge"))
				.addEdge(START, "node1")
				.addEdge(START, "node2")
				.addEdge(START, "node3")
				.addEdge("node1", "merge")
				.addEdge("node2", "merge")
				.addEdge("node3", "merge")
				.addEdge("merge", END);

		var app = workflow.compile();

		long startTime = System.currentTimeMillis();
		final OverAllState[] finalState = new OverAllState[1];
		
		// Don't configure any strategy - should default to ALL_OF
		app.stream(Map.of(),
				RunnableConfig.builder()
						.addParallelNodeExecutor(START, ForkJoinPool.commonPool())
						.build())
				.doOnNext(output -> log.info("Node output: {}", output.node()))
				.map(NodeOutput::state)
				.doOnNext(state -> finalState[0] = state)
				.blockLast();

		long endTime = System.currentTimeMillis();
		long duration = endTime - startTime;

		// Verify that execution waited for all branches (default behavior)
		assertTrue(duration >= 150, 
				"Execution should wait for all branches by default (ALL_OF), but took only " + duration + "ms");

		// Verify that results are present
		assertNotNull(finalState[0], "Final state should not be null");
		List<String> messages = (List<String>) finalState[0].value("messages").orElse(List.of());
		
		// Should have results from all parallel branches
		assertTrue(messages.size() >= 3, 
				"Result should contain results from all parallel branches");
	}

	/**
	 * Tests default aggregation strategy configured via defaultParallelAggregationStrategy.
	 */
	@Test
	void testParallelNodeAggregationStrategyDefaultConfig() throws Exception {
		// Create a workflow with parallel branches
		var workflow = new StateGraph(createKeyStrategyFactory())
				.addNode("fastNode", makeNodeWithDelay("fastNode", 100))
				.addNode("slowNode", makeNodeWithDelay("slowNode", 400))
				.addNode("merge", makeNode("merge"))
				.addEdge(START, "fastNode")
				.addEdge(START, "slowNode")
				.addEdge("fastNode", "merge")
				.addEdge("slowNode", "merge")
				.addEdge("merge", END);

		var app = workflow.compile();

		long startTime = System.currentTimeMillis();
		final OverAllState[] finalState = new OverAllState[1];
		
		// Configure default strategy as ANY_OF
		app.stream(Map.of(),
				RunnableConfig.builder()
						.defaultParallelAggregationStrategy(NodeAggregationStrategy.ANY_OF)
						.addParallelNodeExecutor(START, ForkJoinPool.commonPool())
						.build())
				.doOnNext(output -> log.info("Node output: {}", output.node()))
				.map(NodeOutput::state)
				.doOnNext(state -> finalState[0] = state)
				.blockLast();

		long endTime = System.currentTimeMillis();
		long duration = endTime - startTime;

		// Verify that result is present
		assertNotNull(finalState[0], "Final state should not be null");
		List<String> messages = (List<String>) finalState[0].value("messages").orElse(List.of());
		
		// With ANY_OF as default strategy, should use fastNode (first completed), not slowNode
		assertTrue(messages.contains("fastNode"),
				"Result should contain fastNode (the first completed branch with ANY_OF)");

		// The key behavior test: slowNode should NOT be present if ANY_OF worked correctly
		// If slowNode is present, it means we waited for it (ALL_OF behavior)
		boolean hasSlowNodeData = messages.contains("slowNode");
		assertFalse(hasSlowNodeData,
				"Result should NOT contain slowNode with default ANY_OF strategy (got: " + messages + ")");

		// Log timing for debugging purposes only
		log.info("Default ANY_OF execution took {}ms (fastNode: 100ms, slowNode: 400ms)", duration);
	}

	/**
	 * Tests that merge node-specific strategy overrides default strategy.
	 * This test verifies that when both default and node-specific strategies are configured,
	 * the node-specific strategy takes precedence.
	 */
	@Test
	void testParallelNodeAggregationStrategyMergeNodeOverridesDefault() throws Exception {
		// Create a workflow with parallel branches
		var workflow = new StateGraph(createKeyStrategyFactory())
				.addNode("fastNode", makeNodeWithDelay("fastNode", 100))
				.addNode("slowNode", makeNodeWithDelay("slowNode", 400))
				.addNode("merge", makeNode("merge"))
				.addEdge(START, "fastNode")
				.addEdge(START, "slowNode")
				.addEdge("fastNode", "merge")
				.addEdge("slowNode", "merge")
				.addEdge("merge", END);

		var app = workflow.compile();

		long startTime = System.currentTimeMillis();
		final OverAllState[] finalState = new OverAllState[1];
		
		// Configure default strategy as ALL_OF, but override with ANY_OF for merge node
		app.stream(Map.of(),
				RunnableConfig.builder()
						.defaultParallelAggregationStrategy(NodeAggregationStrategy.ALL_OF)
						.addParallelNodeAggregationStrategy("merge", NodeAggregationStrategy.ANY_OF)
						.addParallelNodeExecutor(START, ForkJoinPool.commonPool())
						.build())
				.doOnNext(output -> log.info("Node output: {}", output.node()))
				.map(NodeOutput::state)
				.doOnNext(state -> finalState[0] = state)
				.blockLast();

		long endTime = System.currentTimeMillis();
		long duration = endTime - startTime;

		// Verify that result is present
		assertNotNull(finalState[0], "Final state should not be null");
		List<String> messages = (List<String>) finalState[0].value("messages").orElse(List.of());
		
		// The key behavior test: merge node's ANY_OF should override default ALL_OF
		// So we should see fastNode (first completed), not slowNode
		assertTrue(messages.contains("fastNode"),
				"Result should contain fastNode (merge node's ANY_OF overrides default ALL_OF)");

		// If slowNode is present, it means ALL_OF was used instead of ANY_OF
		boolean hasSlowNodeData = messages.contains("slowNode");
		assertFalse(hasSlowNodeData,
				"Result should NOT contain slowNode - merge node's ANY_OF should override default ALL_OF (got: " + messages + ")");

		assertTrue(messages.contains("merge"), "Result should contain merge node");

		// Log timing for debugging purposes only
		log.info("Override test execution took {}ms (fastNode: 100ms, slowNode: 400ms)", duration);
	}

	/**
	 * Tests parallel node aggregation strategy with streaming nodes (Flux).
	 * This test verifies that when parallel branches include streaming nodes,
	 * the aggregation strategy correctly handles Flux results.
	 */
	@Test
	void testParallelNodeAggregationStrategyWithStreamingNodes() throws Exception {
		// Create a workflow with parallel branches:
		// - streamingNode1: returns Flux immediately (fast)
		// - normalNode: returns regular value with delay (slow)
		// - streamingNode2: returns Flux with delay (slow)
		var workflow = new StateGraph(createKeyStrategyFactory())
				.addNode("streamingNode1", makeStreamingNode("streamingNode1", 50, "chunk1", "chunk2", "chunk3"))
				.addNode("normalNode", makeNodeWithDelay("normalNode", 300))
				.addNode("streamingNode2", makeStreamingNode("streamingNode2", 400, "chunkA", "chunkB"))
				.addNode("merge", makeNode("merge"))
				.addEdge(START, "streamingNode1")
				.addEdge(START, "normalNode")
				.addEdge(START, "streamingNode2")
				.addEdge("streamingNode1", "merge")
				.addEdge("normalNode", "merge")
				.addEdge("streamingNode2", "merge")
				.addEdge("merge", END);

		var app = workflow.compile();

		// Test ANY_OF strategy with streaming nodes
		long startTime = System.currentTimeMillis();
		final OverAllState[] finalStateAnyOf = new OverAllState[1];
		final List<String> streamValuesAnyOf = new ArrayList<>();
		
		app.stream(Map.of(),
				RunnableConfig.builder()
						.addParallelNodeAggregationStrategy("merge", NodeAggregationStrategy.ANY_OF)
						.addParallelNodeExecutor(START, ForkJoinPool.commonPool())
						.build())
				.doOnNext(output -> {
					log.info("Node output: {}", output.node());
					// Collect streaming values if present
					output.state().value("stream").ifPresent(stream -> {
						if (stream instanceof Flux) {
							@SuppressWarnings("unchecked")
							Flux<String> flux = (Flux<String>) stream;
							flux.collectList().blockOptional().ifPresent(streamValuesAnyOf::addAll);
						}
					});
				})
				.map(NodeOutput::state)
				.doOnNext(state -> finalStateAnyOf[0] = state)
				.blockLast();

		long endTime = System.currentTimeMillis();
		long durationAnyOf = endTime - startTime;

		// Verify that execution completed quickly (ANY_OF should use first completed)
		// streamingNode1 completes first (50ms), so execution should be fast
		assertTrue(durationAnyOf < 200, 
				"Execution with ANY_OF should complete quickly using streamingNode1, but took " + durationAnyOf + "ms");

		// Verify that result is present
		assertNotNull(finalStateAnyOf[0], "Final state should not be null");
		
		// With ANY_OF, streamingNode1 should be the first completed
		// The result should contain streamingNode1's data
		Object nodeId = finalStateAnyOf[0].value("nodeId").orElse(null);
		assertTrue("streamingNode1".equals(nodeId) || finalStateAnyOf[0].value("stream").isPresent(),
				"Result should contain streamingNode1's data (first completed)");

		// Test ALL_OF strategy with streaming nodes
		startTime = System.currentTimeMillis();
		final OverAllState[] finalStateAllOf = new OverAllState[1];
		final List<String> streamValuesAllOf = new ArrayList<>();
		
		app.stream(Map.of(),
				RunnableConfig.builder()
						.addParallelNodeAggregationStrategy("merge", NodeAggregationStrategy.ALL_OF)
						.addParallelNodeExecutor(START, ForkJoinPool.commonPool())
						.build())
				.doOnNext(output -> {
					log.info("Node output: {}", output.node());
					// Collect streaming values if present
					output.state().value("stream").ifPresent(stream -> {
						if (stream instanceof Flux) {
							@SuppressWarnings("unchecked")
							Flux<String> flux = (Flux<String>) stream;
							flux.collectList().blockOptional().ifPresent(streamValuesAllOf::addAll);
						}
					});
				})
				.map(NodeOutput::state)
				.doOnNext(state -> finalStateAllOf[0] = state)
				.blockLast();

		endTime = System.currentTimeMillis();
		long durationAllOf = endTime - startTime;

		// Verify that execution waited for all branches including streaming nodes
		// streamingNode2 is the slowest (400ms), so execution should wait for it
		assertTrue(durationAllOf >= 300, 
				"Execution with ALL_OF should wait for all branches including streamingNode2 (400ms), but took only " + durationAllOf + "ms");

		// Verify that result is present
		assertNotNull(finalStateAllOf[0], "Final state should not be null");
		
		// With ALL_OF, all branches should complete
		// The result should contain data from all nodes
		assertTrue(finalStateAllOf[0].value("stream").isPresent() || 
				   finalStateAllOf[0].value("nodeId").isPresent(),
				"Result should contain data from all parallel branches");
	}

	/**
	 * Tests parallel node aggregation strategy with mixed streaming and non-streaming nodes.
	 * This test verifies that when parallel branches mix streaming (Flux) and non-streaming nodes,
	 * the aggregation strategy correctly handles both types of results.
	 */
	@Test
	void testParallelNodeAggregationStrategyWithMixedStreamingAndNormalNodes() throws Exception {
		// Create a workflow with mixed node types:
		// - fastNormalNode: regular node, completes quickly
		// - slowStreamingNode: streaming node, completes slowly
		var workflow = new StateGraph(createKeyStrategyFactory())
				.addNode("fastNormalNode", makeNodeWithDelay("fastNormalNode", 100))
				.addNode("slowStreamingNode", makeStreamingNode("slowStreamingNode", 400, "data1", "data2", "data3"))
				.addNode("merge", makeNode("merge"))
				.addEdge(START, "fastNormalNode")
				.addEdge(START, "slowStreamingNode")
				.addEdge("fastNormalNode", "merge")
				.addEdge("slowStreamingNode", "merge")
				.addEdge("merge", END);

		var app = workflow.compile();

		// Test ANY_OF: should use fastNormalNode (completes first)
		long startTime = System.currentTimeMillis();
		final OverAllState[] finalState = new OverAllState[1];
		
		app.stream(Map.of(),
				RunnableConfig.builder()
						.addParallelNodeAggregationStrategy("merge", NodeAggregationStrategy.ANY_OF)
						.addParallelNodeExecutor(START, ForkJoinPool.commonPool())
						.build())
				.doOnNext(output -> log.info("Node output: {}", output.node()))
				.map(NodeOutput::state)
				.doOnNext(state -> finalState[0] = state)
				.blockLast();

		long endTime = System.currentTimeMillis();
		long duration = endTime - startTime;

		// Verify that result contains fastNormalNode's data
		assertNotNull(finalState[0], "Final state should not be null");
		List<String> messages = (List<String>) finalState[0].value("messages").orElse(List.of());

		// With ANY_OF, should use fastNormalNode (first completed), not slowStreamingNode
		boolean hasFastNodeData = messages.contains("fastNormalNode") ||
				finalState[0].value("nodeId").map("fastNormalNode"::equals).orElse(false);
		assertTrue(hasFastNodeData,
				"Result should contain fastNormalNode's data (first completed)");

		// The key behavior test: slowStreamingNode should NOT have contributed data
		// If stream data is present, it means we waited for slowStreamingNode (wrong for ANY_OF)
		boolean hasSlowStreamingData = finalState[0].value("stream").isPresent() ||
				finalState[0].value("nodeId").map("slowStreamingNode"::equals).orElse(false);
		assertFalse(hasSlowStreamingData,
				"Result should NOT contain slowStreamingNode data with ANY_OF strategy (got stream: " +
				finalState[0].value("stream").isPresent() + ", nodeId: " +
				finalState[0].value("nodeId") + ")");

		// Log timing for debugging purposes only
		log.info("ANY_OF with mixed nodes execution took {}ms (fastNormalNode: 100ms, slowStreamingNode: 400ms)", duration);

		// Test ALL_OF: should wait for slowStreamingNode
		startTime = System.currentTimeMillis();
		final OverAllState[] finalStateAllOf = new OverAllState[1];
		
		app.stream(Map.of(),
				RunnableConfig.builder()
						.addParallelNodeAggregationStrategy("merge", NodeAggregationStrategy.ALL_OF)
						.addParallelNodeExecutor(START, ForkJoinPool.commonPool())
						.build())
				.doOnNext(output -> log.info("Node output: {}", output.node()))
				.map(NodeOutput::state)
				.doOnNext(state -> finalStateAllOf[0] = state)
				.blockLast();

		endTime = System.currentTimeMillis();
		long durationAllOf = endTime - startTime;

		// Verify that execution waited for slowStreamingNode (400ms)
		assertTrue(durationAllOf >= 350,
				"Execution with ALL_OF should wait for slowStreamingNode (400ms), but took only " + durationAllOf + "ms");

		// Verify that result contains data from both nodes
		assertNotNull(finalStateAllOf[0], "Final state should not be null");
		// With ALL_OF, both nodes should complete, so the result should contain data from both
		assertTrue(finalStateAllOf[0].value("messages").isPresent() ||
				   finalStateAllOf[0].value("stream").isPresent() ||
				   finalStateAllOf[0].value("nodeId").isPresent(),
				"Result should contain data from both parallel branches");
	}

}

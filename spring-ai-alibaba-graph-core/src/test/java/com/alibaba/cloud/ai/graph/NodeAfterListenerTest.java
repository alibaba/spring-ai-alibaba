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

import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.streaming.GraphFlux;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Libres-coder
 */

public class NodeAfterListenerTest {

	private static final Logger log = LoggerFactory.getLogger(NodeAfterListenerTest.class);

	private KeyStrategyFactory createKeyStrategyFactory() {
		return () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("messages", new AppendStrategy());
			return keyStrategyMap;
		};
	}

	private AsyncNodeActionWithConfig makeNode(String name) {
		return AsyncNodeActionWithConfig.node_async((state, config) -> {
			log.info("Executing node: {}", name);
			return Map.of("messages", name);
		});
	}

	private AsyncNodeActionWithConfig makeStreamingNode(String name) {
		return AsyncNodeActionWithConfig.node_async((state, config) -> {
			log.info("Executing streaming node: {}", name);
			Flux<String> dataFlux = Flux.just("chunk1", "chunk2", "chunk3");
			GraphFlux<String> graphFlux = GraphFlux.of(
				name,
				"messages",
				dataFlux,
				chunk -> name + "_" + chunk,
				chunk -> chunk
			);
			return Map.of("stream", graphFlux);
		});
	}

	@Test
	void testNodeAfterListenerForNormalNodes() throws Exception {
		Map<String, Integer> beforeCount = new ConcurrentHashMap<>();
		Map<String, Integer> afterCount = new ConcurrentHashMap<>();
		Map<String, Map<String, Object>> afterStates = new ConcurrentHashMap<>();

		var workflow = new StateGraph(createKeyStrategyFactory())
			.addNode("A", makeNode("A"))
			.addNode("B", makeNode("B"))
			.addEdge(START, "A")
			.addEdge("A", "B")
			.addEdge("B", END);

		var app = workflow.compile(CompileConfig.builder().withLifecycleListener(new GraphLifecycleListener() {
			@Override
			public void before(String nodeId, Map<String, Object> state, RunnableConfig config, Long curTime) {
				beforeCount.merge(nodeId, 1, Integer::sum);
				log.info("BEFORE: nodeId={}, state={}", nodeId, state);
			}

			@Override
			public void after(String nodeId, Map<String, Object> state, RunnableConfig config, Long curTime) {
				afterCount.merge(nodeId, 1, Integer::sum);
				afterStates.put(nodeId, state);
				log.info("AFTER: nodeId={}, state={}", nodeId, state);
			}
		}).build());

		app.stream(Map.of()).blockLast();

		assertEquals(2, beforeCount.size(), "Before should be called for 2 nodes");
		assertEquals(2, afterCount.size(), "After should be called for 2 nodes");
		
		assertTrue(beforeCount.containsKey("A"), "Before should be called for node A");
		assertTrue(beforeCount.containsKey("B"), "Before should be called for node B");
		assertTrue(afterCount.containsKey("A"), "After should be called for node A");
		assertTrue(afterCount.containsKey("B"), "After should be called for node B");

		List<String> stateA = (List<String>) afterStates.get("A").get("messages");
		assertNotNull(stateA, "State A should have messages");
		assertTrue(stateA.contains("A"), "State A should contain 'A'");

		List<String> stateB = (List<String>) afterStates.get("B").get("messages");
		assertNotNull(stateB, "State B should have messages");
		assertTrue(stateB.contains("A"), "State B should contain 'A' from previous node");
		assertTrue(stateB.contains("B"), "State B should contain 'B' from current node");
	}

	@Test
	void testNodeAfterListenerForStreamingNodes() throws Exception {
		Map<String, Integer> beforeCount = new ConcurrentHashMap<>();
		Map<String, Integer> afterCount = new ConcurrentHashMap<>();
		Map<String, Map<String, Object>> afterStates = new ConcurrentHashMap<>();

		var workflow = new StateGraph(createKeyStrategyFactory())
			.addNode("normalNode", makeNode("normalNode"))
			.addNode("streamingNode", makeStreamingNode("streamingNode"))
			.addNode("finalNode", makeNode("finalNode"))
			.addEdge(START, "normalNode")
			.addEdge("normalNode", "streamingNode")
			.addEdge("streamingNode", "finalNode")
			.addEdge("finalNode", END);

		var app = workflow.compile(CompileConfig.builder().withLifecycleListener(new GraphLifecycleListener() {
			@Override
			public void before(String nodeId, Map<String, Object> state, RunnableConfig config, Long curTime) {
				beforeCount.merge(nodeId, 1, Integer::sum);
				log.info("BEFORE: nodeId={}, state={}", nodeId, state);
			}

			@Override
			public void after(String nodeId, Map<String, Object> state, RunnableConfig config, Long curTime) {
				afterCount.merge(nodeId, 1, Integer::sum);
				afterStates.put(nodeId, state);
				log.info("AFTER: nodeId={}, state={}", nodeId, state);
			}
		}).build());

		app.stream(Map.of()).blockLast();

		assertEquals(3, beforeCount.size(), "Before should be called for 3 nodes");
		assertEquals(3, afterCount.size(), "After should be called for 3 nodes (including streaming)");
		
		assertTrue(afterCount.containsKey("normalNode"), "After should be called for normalNode");
		assertTrue(afterCount.containsKey("streamingNode"), "After should be called for streamingNode");
		assertTrue(afterCount.containsKey("finalNode"), "After should be called for finalNode");

		Map<String, Object> streamingState = afterStates.get("streamingNode");
		assertNotNull(streamingState, "Streaming node should have state in after callback");
		assertNotNull(streamingState.get("messages"), "Streaming node state should have messages");
	}

	@Test
	void testNodeAfterListenerForParallelNodes() throws Exception {
		Map<String, Integer> beforeCount = new ConcurrentHashMap<>();
		Map<String, Integer> afterCount = new ConcurrentHashMap<>();
		List<String> afterNodeIds = new ArrayList<>();

		var workflow = new StateGraph(createKeyStrategyFactory())
			.addNode("A", makeNode("A"))
			.addNode("A1", makeNode("A1"))
			.addNode("A2", makeNode("A2"))
			.addNode("A3", makeNode("A3"))
			.addNode("B", makeNode("B"))
			.addEdge(START, "A")
			.addEdge("A", "A1")
			.addEdge("A", "A2")
			.addEdge("A", "A3")
			.addEdge("A1", "B")
			.addEdge("A2", "B")
			.addEdge("A3", "B")
			.addEdge("B", END);

		var app = workflow.compile(CompileConfig.builder().withLifecycleListener(new GraphLifecycleListener() {
			@Override
			public void before(String nodeId, Map<String, Object> state, RunnableConfig config, Long curTime) {
				synchronized (beforeCount) {
					beforeCount.merge(nodeId, 1, Integer::sum);
					log.info("BEFORE: nodeId={}", nodeId);
				}
			}

			@Override
			public void after(String nodeId, Map<String, Object> state, RunnableConfig config, Long curTime) {
				synchronized (afterCount) {
					afterCount.merge(nodeId, 1, Integer::sum);
					afterNodeIds.add(nodeId);
					log.info("AFTER: nodeId={}", nodeId);
				}
			}
		}).build());

		app.stream(Map.of(), RunnableConfig.builder()
			.addParallelNodeExecutor("A", ForkJoinPool.commonPool())
			.build()).blockLast();

		assertTrue(afterCount.containsKey("A"), "After should be called for node A");
		assertTrue(afterCount.containsKey("A1"), "After should be called for parallel node A1");
		assertTrue(afterCount.containsKey("A2"), "After should be called for parallel node A2");
		assertTrue(afterCount.containsKey("A3"), "After should be called for parallel node A3");
		assertTrue(afterCount.containsKey("B"), "After should be called for node B");

		assertTrue(afterNodeIds.stream().anyMatch(id -> id.contains("PARALLEL")), 
			"After should be called for parallel node wrapper");
	}

	@Test
	void testNodeAfterListenerReceivesCorrectStatePerNode() throws Exception {
		Map<String, String> nodeLastMessages = new ConcurrentHashMap<>();
		Map<String, Integer> nodeMessageCounts = new ConcurrentHashMap<>();

		var workflow = new StateGraph(createKeyStrategyFactory())
			.addNode("node1", makeNode("node1_data"))
			.addNode("node2", makeNode("node2_data"))
			.addNode("node3", makeNode("node3_data"))
			.addEdge(START, "node1")
			.addEdge("node1", "node2")
			.addEdge("node2", "node3")
			.addEdge("node3", END);

		var app = workflow.compile(CompileConfig.builder().withLifecycleListener(new GraphLifecycleListener() {
			@Override
			public void after(String nodeId, Map<String, Object> state, RunnableConfig config, Long curTime) {
				List<String> messages = (List<String>) state.get("messages");
				if (messages != null && !messages.isEmpty()) {
					String lastMessage = messages.get(messages.size() - 1);
					nodeLastMessages.put(nodeId, lastMessage);
					nodeMessageCounts.put(nodeId, messages.size());
					log.info("AFTER {}: last message = {}, total messages = {}", nodeId, lastMessage, messages.size());
				}
			}
		}).build());

		app.stream(Map.of()).blockLast();

		assertEquals(3, nodeLastMessages.size(), "Should have captured 3 node states");
		
		assertEquals("node1_data", nodeLastMessages.get("node1"), 
			"node1 after should have node1_data as last message (not previous node's data)");
		assertEquals("node2_data", nodeLastMessages.get("node2"), 
			"node2 after should have node2_data as last message (not node1_data)");
		assertEquals("node3_data", nodeLastMessages.get("node3"), 
			"node3 after should have node3_data as last message (not node2_data)");

		assertEquals(1, nodeMessageCounts.get("node1"), "node1 should have 1 message");
		assertEquals(2, nodeMessageCounts.get("node2"), "node2 should have 2 messages");
		assertEquals(3, nodeMessageCounts.get("node3"), "node3 should have 3 messages");
	}
}


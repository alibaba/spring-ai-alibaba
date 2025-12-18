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

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.*;


public class TimeTravelTest {


	private KeyStrategyFactory createKeyStrategyFactory() {
		return () -> {
			Map<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("k1", new ReplaceStrategy());
			strategies.put("k2", new ReplaceStrategy());
			return strategies;
		};
	}


	@Test
	public void testResumeFromHistoricalCheckpoint() throws Exception {
		NodeAction node1 = state -> Map.of("k1", "v1");
		NodeAction node2 = state -> Map.of("k2", "v2");

		StateGraph workflow = new StateGraph(createKeyStrategyFactory())
				.addNode("node1", node_async(node1))
				.addNode("node2", node_async(node2));

		workflow.addEdge(START, "node1");
		workflow.addEdge("node1", "node2");
		workflow.addEdge("node2", END);

		var memory = new MemorySaver();
		var compileConfig = CompileConfig.builder()
				.saverConfig(SaverConfig.builder()
						.register(memory)
						.build())
				.build();

		CompiledGraph compiledGraph = workflow.compile(compileConfig);

		var config = RunnableConfig.builder()
				.threadId("test_thread")
				.build();

		Flux<NodeOutput> stream = compiledGraph.stream(Map.of(), config);
		var outputs = stream.collectList().block();

		assertNotNull(outputs);
		assertEquals(4, outputs.size());

		Collection<StateSnapshot> history = compiledGraph.getStateHistory(config);
		assertNotNull(history);
		assertTrue(history.size() >= 2, "Should have at least 2 historical states");

		StateSnapshot node1Snapshot = new ArrayList<>(history).stream()
				.filter(s -> "node1".equals(s.node()))
				.findFirst()
				.orElseThrow();

		String checkpointId = node1Snapshot.config().checkPointId().orElse(null);
		assertNotNull(checkpointId, "checkpointId should not be null");

		var resumeConfig = RunnableConfig.builder()
				.threadId("test_thread")
				.checkPointId(checkpointId)
				.build();

		Flux<NodeOutput> resumeStream = compiledGraph.stream(Map.of(), resumeConfig);
		var resumeOutputs = resumeStream.collectList().block();

		assertNotNull(resumeOutputs);
		assertTrue(resumeOutputs.size() >= 2, "Should have at least 2 outputs after resume (node2 and END)");

		var lastOutput = resumeOutputs.get(resumeOutputs.size() - 1);
		var finalState = lastOutput.state().data();
		assertTrue(finalState.containsKey("k1"), "Should contain k1");
		assertTrue(finalState.containsKey("k2"), "Should contain k2");
		assertEquals("v1", finalState.get("k1"));
		assertEquals("v2", finalState.get("k2"));
	}

	@Test
	public void testMultipleResumesFromSameCheckpoint() throws Exception {
		NodeAction node1 = state -> Map.of("k1", "v1");
		NodeAction node2 = state -> Map.of("k2", "v2");

		StateGraph workflow = new StateGraph(createKeyStrategyFactory())
				.addNode("node1", node_async(node1))
				.addNode("node2", node_async(node2));

		workflow.addEdge(START, "node1");
		workflow.addEdge("node1", "node2");
		workflow.addEdge("node2", END);

		var memory = new MemorySaver();
		var compileConfig = CompileConfig.builder()
				.saverConfig(SaverConfig.builder()
						.register(memory)
						.build())
				.build();

		CompiledGraph compiledGraph = workflow.compile(compileConfig);

		var config = RunnableConfig.builder().threadId("test_thread_2").build();
		compiledGraph.invoke(Map.of(), config);

		Collection<StateSnapshot> history = compiledGraph.getStateHistory(config);
		StateSnapshot node1Snapshot = new ArrayList<>(history).stream()
				.filter(s -> "node1".equals(s.node()))
				.findFirst()
				.orElseThrow();

		String checkpointId = node1Snapshot.config().checkPointId().orElse(null);

		var resumeConfig1 = RunnableConfig.builder()
				.threadId("test_thread_2")
				.checkPointId(checkpointId)
				.build();
		var result1 = compiledGraph.invoke(Map.of(), resumeConfig1);
		assertTrue(result1.isPresent());

		var resumeConfig2 = RunnableConfig.builder()
				.threadId("test_thread_2")
				.checkPointId(checkpointId)
				.build();
		var result2 = compiledGraph.invoke(Map.of(), resumeConfig2);
		assertTrue(result2.isPresent());

		assertEquals(result1.get().value("k1").get(), result2.get().value("k1").get());
		assertEquals(result1.get().value("k2").get(), result2.get().value("k2").get());
	}

	@Test
	public void testResumeFromDifferentCheckpoints() throws Exception {
		NodeAction node1 = state -> Map.of("k1", "v1");
		NodeAction node2 = state -> Map.of("k2", "v2");

		StateGraph workflow = new StateGraph(createKeyStrategyFactory())
				.addNode("node1", node_async(node1))
				.addNode("node2", node_async(node2));

		workflow.addEdge(START, "node1");
		workflow.addEdge("node1", "node2");
		workflow.addEdge("node2", END);

		var memory = new MemorySaver();
		var compileConfig = CompileConfig.builder()
				.saverConfig(SaverConfig.builder()
						.register(memory)
						.build())
				.build();

		CompiledGraph compiledGraph = workflow.compile(compileConfig);

		var config = RunnableConfig.builder().threadId("test_thread_3").build();
		compiledGraph.invoke(Map.of(), config);

		Collection<StateSnapshot> history = compiledGraph.getStateHistory(config);
		var historyList = new ArrayList<>(history);

		StateSnapshot node1Snapshot = historyList.stream()
				.filter(s -> "node1".equals(s.node()))
				.findFirst()
				.orElseThrow();

		var resumeFromNode1 = RunnableConfig.builder()
				.threadId("test_thread_3")
				.checkPointId(node1Snapshot.config().checkPointId().orElse(null))
				.build();
		var resultFromNode1 = compiledGraph.invoke(Map.of(), resumeFromNode1);
		assertTrue(resultFromNode1.isPresent());
		assertTrue(resultFromNode1.get().value("k1").isPresent());
		assertTrue(resultFromNode1.get().value("k2").isPresent());
	}

	@Test
	public void testCheckpointStateIntegrity() throws Exception {
		NodeAction node1 = state -> Map.of("k1", "v1");
		NodeAction node2 = state -> Map.of("k2", "v2");

		StateGraph workflow = new StateGraph(createKeyStrategyFactory())
				.addNode("node1", node_async(node1))
				.addNode("node2", node_async(node2));

		workflow.addEdge(START, "node1");
		workflow.addEdge("node1", "node2");
		workflow.addEdge("node2", END);

		var memory = new MemorySaver();
		var compileConfig = CompileConfig.builder()
				.saverConfig(SaverConfig.builder()
						.register(memory)
						.build())
				.build();

		CompiledGraph compiledGraph = workflow.compile(compileConfig);

		var config = RunnableConfig.builder().threadId("test_thread_4").build();
		var finalResult = compiledGraph.invoke(Map.of(), config);
		assertTrue(finalResult.isPresent());

		Collection<StateSnapshot> history = compiledGraph.getStateHistory(config);
		var historyList = new ArrayList<>(history);

		assertTrue(historyList.size() >= 2, "Should have at least 2 historical states");

		StateSnapshot node1Snapshot = historyList.stream()
				.filter(s -> "node1".equals(s.node()))
				.findFirst()
				.orElseThrow();
		assertNotNull(node1Snapshot.state());
		assertTrue(node1Snapshot.state().data().containsKey("k1"));

		StateSnapshot node2Snapshot = historyList.stream()
				.filter(s -> "node2".equals(s.node()))
				.findFirst()
				.orElseThrow();
		assertNotNull(node2Snapshot.state());
		assertTrue(node2Snapshot.state().data().containsKey("k1"));
		assertTrue(node2Snapshot.state().data().containsKey("k2"));
	}
}

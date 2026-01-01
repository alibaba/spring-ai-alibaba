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

import com.alibaba.cloud.ai.graph.checkpoint.savers.file.FileSystemSaver;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.alibaba.cloud.ai.graph.serializer.std.SpringAIStateSerializer;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for StateGraph StateSerializer configuration.
 */
public class StateGraphSerializerTest {

	@Test
	public void testStateGraphWithSpringAIStateSerializer() throws Exception {
		// Create StateGraph with SpringAIStateSerializer
		StateSerializer serializer = new SpringAIStateSerializer();
		KeyStrategyFactory keyStrategyFactory = () -> Map.of("value", new ReplaceStrategy());
		
		StateGraph graph = new StateGraph("testGraph", keyStrategyFactory, serializer);
		
		// Verify serializer is set correctly
		StateSerializer retrievedSerializer = graph.getStateSerializer();
		assertNotNull(retrievedSerializer);
		assertInstanceOf(SpringAIStateSerializer.class, retrievedSerializer);
		
		// Test serialization/deserialization
		testSerializerFunctionality(graph, serializer);
	}

	@Test
	public void testStateGraphWithSpringAIJacksonStateSerializer() throws Exception {
		// Create StateGraph with SpringAIJacksonStateSerializer
		StateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
		KeyStrategyFactory keyStrategyFactory = () -> Map.of("value", new ReplaceStrategy());
		
		StateGraph graph = new StateGraph("testGraph", keyStrategyFactory, serializer);
		
		// Verify serializer is set correctly
		StateSerializer retrievedSerializer = graph.getStateSerializer();
		assertNotNull(retrievedSerializer);
		assertInstanceOf(SpringAIJacksonStateSerializer.class, retrievedSerializer);
		
		// Test serialization/deserialization
		testSerializerFunctionality(graph, serializer);
	}

	@Test
	public void testStateGraphWithDefaultSerializer() throws Exception {
		// Create StateGraph without specifying serializer (should use default)
		KeyStrategyFactory keyStrategyFactory = () -> Map.of("value", new ReplaceStrategy());
		
		StateGraph graph = new StateGraph("testGraph", keyStrategyFactory);
		
		// Verify default serializer is set (should be JacksonSerializer)
		StateSerializer retrievedSerializer = graph.getStateSerializer();
		assertNotNull(retrievedSerializer);
		// Default should be JacksonSerializer (extends SpringAIJacksonStateSerializer)
		assertInstanceOf(SpringAIJacksonStateSerializer.class, retrievedSerializer);
	}

	@Test
	public void testStateGraphSerializerUsedInExecution() throws Exception {
		// Create StateGraph with specific serializer
		StateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
		KeyStrategyFactory keyStrategyFactory = () -> Map.of("result", new ReplaceStrategy());
		
		StateGraph graph = new StateGraph("testGraph", keyStrategyFactory, serializer)
			.addEdge(START, "node1")
			.addNode("node1", node_async(state -> {
				return Map.of("result", "test_value");
			}))
			.addEdge("node1", END);
		
		// Compile and execute
		CompiledGraph compiledGraph = graph.compile();
		java.util.Optional<OverAllState> result = compiledGraph.invoke(Map.of());
		
		// Verify execution works correctly
		assertTrue(result.isPresent());
		assertEquals("test_value", result.get().value("result").orElse(null));
		
		// Verify the serializer is used for state cloning
		StateSerializer usedSerializer = graph.getStateSerializer();
		assertNotNull(usedSerializer);
		assertInstanceOf(SpringAIJacksonStateSerializer.class, usedSerializer);
	}

	@Test
	public void testStateGraphSerializerConsistency() throws Exception {
		// Create serializer instance
		StateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
		KeyStrategyFactory keyStrategyFactory = () -> Map.of("value", new ReplaceStrategy());
		
		// Create StateGraph with the serializer
		StateGraph graph1 = new StateGraph("graph1", keyStrategyFactory, serializer);
		StateGraph graph2 = new StateGraph("graph2", keyStrategyFactory, serializer);
		
		// Both should use the same serializer instance
		StateSerializer serializer1 = graph1.getStateSerializer();
		StateSerializer serializer2 = graph2.getStateSerializer();
		
		assertNotNull(serializer1);
		assertNotNull(serializer2);
		assertEquals(serializer1.getClass(), serializer2.getClass());
	}

	@Test
	public void testStateGraphSerializerWithFileSystemSaver() throws Exception {
		// Create StateGraph with specific serializer
		StateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
		KeyStrategyFactory keyStrategyFactory = () -> Map.of("steps", new ReplaceStrategy());
		
		StateGraph graph = new StateGraph("testGraph", keyStrategyFactory, serializer)
			.addEdge(START, "node1")
			.addNode("node1", node_async(state -> {
				int steps = (int) state.value("steps").orElse(0);
				return Map.of("steps", steps + 1);
			}))
			.addEdge("node1", END);
		
		// Create FileSystemSaver using the same serializer from StateGraph
		FileSystemSaver saver =
			FileSystemSaver.builder()
					.targetFolder(Paths.get("target", "test-serializer-saver"))
					.stateSerializer(graph.getStateSerializer())
					.build();
		
		// Verify serializer consistency
		StateSerializer graphSerializer = graph.getStateSerializer();
		assertNotNull(graphSerializer);
		
		// Compile with saver
		CompileConfig config = CompileConfig.builder()
			.saverConfig(com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig.builder()
				.register(saver)
				.build())
			.build();
		
		CompiledGraph compiledGraph = graph.compile(config);
		
		// Execute and verify
		RunnableConfig runnableConfig = RunnableConfig.builder().threadId("test-thread").build();
		java.util.Optional<OverAllState> result = compiledGraph.invoke(Map.of(), runnableConfig);
		
		assertTrue(result.isPresent());
		assertEquals(1, result.get().value("steps").orElse(0));
		
		// Cleanup
		saver.deleteFile(runnableConfig);
	}

	/**
	 * Helper method to test serializer functionality.
	 */
	private void testSerializerFunctionality(StateGraph graph, StateSerializer serializer) 
			throws IOException, ClassNotFoundException {
		// Create test state data
		Map<String, Object> testData = new HashMap<>();
		testData.put("key1", "value1");
		testData.put("key2", 42);
		testData.put("key3", true);
		
		// Serialize
		byte[] serialized = serializer.dataToBytes(testData);
		assertNotNull(serialized);
		assertTrue(serialized.length > 0);
		
		// Deserialize
		Map<String, Object> deserialized = serializer.dataFromBytes(serialized);
		assertNotNull(deserialized);
		assertEquals("value1", deserialized.get("key1"));
		assertEquals(42, deserialized.get("key2"));
		assertEquals(true, deserialized.get("key3"));
	}

}

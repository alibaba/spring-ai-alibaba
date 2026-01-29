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

import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.file.FileSystemSaver;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for simple App.
 */
public class StateGraphFileSystemSaverTest {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StateGraphFileSystemSaverTest.class);

	final String rootPath = Paths.get("target", "checkpoint").toString();

	@Test
	public void testCheckpointSaverResubmit() throws Exception {
		int expectedSteps = 5;
		KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactoryBuilder().addStrategy("steps")
			.addStrategy("messages", KeyStrategy.APPEND)
			.build();
		StateGraph workflow = new StateGraph(keyStrategyFactory).addEdge(START, "agent_1")
			.addNode("agent_1", node_async(state -> {
				int defaultSteps = (int) state.value("steps").orElse(0);
				int steps = defaultSteps + 1;
				log.info("agent_1: step: {}", steps);
				return Map.of("steps", steps, "messages", format("agent_1:step %d", steps));
			}))
			.addConditionalEdges("agent_1", edge_async(state -> {
				int steps = (int) state.data().get("steps");
				if (steps >= expectedSteps) {
					return "exit";
				}
				return "next";
			}), Map.of("next", "agent_1", "exit", END));

		var saver = FileSystemSaver.builder()
				.targetFolder(Paths.get(rootPath, "testCheckpointSaverResubmit"))
				.stateSerializer(workflow.getStateSerializer())
				.build();

		CompileConfig compileConfig = CompileConfig.builder()
			.saverConfig(SaverConfig.builder()
				.register(saver)
				.build())
			.build();

		CompiledGraph app = workflow.compile(compileConfig);

		RunnableConfig runnableConfig_1 = RunnableConfig.builder().threadId("thread_1").build();

		RunnableConfig runnableConfig_2 = RunnableConfig.builder().threadId("thread_2").build();

		try {

			for (int execution = 0; execution < 2; execution++) {

				// Clear checkpoints at the start of each execution to ensure clean slate
				saver.deleteFile(runnableConfig_1);
				saver.deleteFile(runnableConfig_2);

				Optional<OverAllState> state = app.invoke(Map.of(), runnableConfig_1);

				assertTrue(state.isPresent());
				assertEquals(expectedSteps + (execution * 2), (int) state.get().data().get("steps"));

				List<String> messages = (List<String>) state.get().data().get("messages");
				assertFalse(messages.isEmpty());

				log.info("thread_1: execution: {} messages:\n{}\n", execution, messages);

				assertEquals(expectedSteps + execution * 2, messages.size());
				for (int i = 0; i < messages.size(); i++) {
					assertEquals(format("agent_1:step %d", (i + 1)), messages.get(i));
				}

				StateSnapshot snapshot = app.getState(runnableConfig_1);

				assertNotNull(snapshot);
				log.info("SNAPSHOT:\n{}\n", snapshot);

				// SUBMIT NEW THREAD 2

				state = app.invoke(emptyMap(), runnableConfig_2);

				assertTrue(state.isPresent());
				assertEquals(expectedSteps + execution, (int) state.get().data().get("steps"));
				messages = (List<String>) state.get().data().get("messages");

				log.info("thread_2: execution: {} messages:\n{}\n", execution, messages);

				assertEquals(expectedSteps + execution, messages.size());

				// RE-SUBMIT THREAD 1
				state = app.invoke(Map.of(), runnableConfig_1);

				assertTrue(state.isPresent());
				assertEquals(expectedSteps + 1 + execution * 2, (int) state.get().data().get("steps"));
				messages = (List<String>) state.get().data().get("messages");

				log.info("thread_1: execution: {} messages:\n{}\n", execution, messages);

				assertEquals(expectedSteps + 1 + execution * 2, messages.size());

			}
		}
		finally {
			// Cleanup checkpoints after test completion
			saver.deleteFile(runnableConfig_1);
			saver.deleteFile(runnableConfig_2);
		}
	}

	@Test
	public void testCheckpointSaverWithManualRelease() throws Exception {
		int expectedSteps = 5;
		KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactoryBuilder().addStrategy("steps")
			.addStrategy("messages", KeyStrategy.APPEND)
			.build();
		StateGraph workflow = new StateGraph(keyStrategyFactory).addEdge(START, "agent_1")
			.addNode("agent_1", node_async(state -> {
				int defaultSteps = (int) state.value("steps").orElse(0);
				int steps = defaultSteps + 1;
				log.info("agent_1: step: {}", steps);
				return Map.of("steps", steps, "messages", format("agent_1:step %d", steps));
			}))
			.addConditionalEdges("agent_1", edge_async(state -> {
				int steps = (int) state.data().get("steps");
				if (steps >= expectedSteps) {
					return "exit";
				}
				return "next";
			}), Map.of("next", "agent_1", "exit", END));

		var saver = FileSystemSaver.builder()
				.targetFolder(Paths.get(rootPath, "testCheckpointSaverWithManualRelease"))
				.stateSerializer(workflow.getStateSerializer())
				.build();

		CompileConfig compileConfig = CompileConfig.builder()
			.saverConfig(SaverConfig.builder()
				.register(saver)
				.build())
			.build();

		CompiledGraph app = workflow.compile(compileConfig);

		RunnableConfig runnableConfig_1 = RunnableConfig.builder().threadId("thread_1").build();

		RunnableConfig runnableConfig_2 = RunnableConfig.builder().threadId("thread_2").build();

		var state = app.invoke(Map.of(), runnableConfig_1);

		assertTrue(state.isPresent());
		assertEquals(expectedSteps, (int) state.get().data().get("steps"));

		var tag = saver.release(runnableConfig_1);
		assertNotNull(tag);
		assertEquals("thread_1", tag.threadId());

		var tagState = tag.checkpoints().stream().map(Checkpoint::getState).findFirst();
		assertTrue(tagState.isPresent());

		assertIterableEquals(state.get().data().entrySet(), tagState.get().entrySet());

		var messages = (List<String>) state.get().data().get("messages");

		assertEquals(expectedSteps, messages.size());

		for (int i = 0; i < messages.size(); i++) {
			assertEquals(format("agent_1:step %d", (i + 1)), messages.get(i));
		}

		var ex = assertThrowsExactly(IllegalStateException.class, () -> app.getState(runnableConfig_1));
		assertEquals("Missing Checkpoint!", ex.getMessage());

		// SUBMIT NEW THREAD 2

		state = app.invoke(emptyMap(), runnableConfig_2);

		assertTrue(state.isPresent());
		assertEquals(expectedSteps, (int) state.get().data().get("steps"));
		messages = (List<String>) state.get().data().get("messages");

		tag = saver.release(runnableConfig_2);
		assertNotNull(tag);
		assertEquals("thread_2", tag.threadId());

		tagState = tag.checkpoints().stream().map(Checkpoint::getState).findFirst();
		assertTrue(tagState.isPresent());

		assertIterableEquals(state.get().data().entrySet(), tagState.get().entrySet());

		assertEquals(expectedSteps, messages.size());

		// RE-SUBMIT THREAD 1
		state = app.invoke(Map.of(), runnableConfig_1);

		assertTrue(state.isPresent());
		assertEquals(expectedSteps, (int) state.get().data().get("steps"));

		tag = saver.release(runnableConfig_1);
		assertNotNull(tag);
		assertEquals("thread_1", tag.threadId());

		tagState = tag.checkpoints().stream().map(Checkpoint::getState).findFirst();
		assertTrue(tagState.isPresent());

		assertIterableEquals(state.get().data().entrySet(), tagState.get().entrySet());

	}

	@Test
	public void testCheckpointSaverWithAutoRelease() throws Exception {
		int expectedSteps = 5;
		KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactoryBuilder().addStrategy("steps")
			.addStrategy("messages", KeyStrategy.APPEND)
			.build();
		StateGraph workflow = new StateGraph(keyStrategyFactory).addEdge(START, "agent_1")
			.addNode("agent_1", node_async(state -> {
				int defaultSteps = (int) state.value("steps").orElse(0);
				int steps = defaultSteps + 1;
				log.info("agent_1: step: {}", steps);
				return Map.of("steps", steps, "messages", format("agent_1:step %d", steps));
			}))
			.addConditionalEdges("agent_1", edge_async(state -> {
				int steps = (int) state.data().get("steps");
				if (steps >= expectedSteps) {
					return "exit";
				}
				return "next";
			}), Map.of("next", "agent_1", "exit", END));

		var saver = FileSystemSaver.builder()
				.targetFolder(Paths.get(rootPath, "testCheckpointSaverWithAutoRelease"))
				.stateSerializer(workflow.getStateSerializer())
				.build();

		CompileConfig compileConfig = CompileConfig.builder()
			.saverConfig(SaverConfig.builder()
				.register(saver)
				.build())
			.releaseThread(true)
			.build();

		var app = workflow.compile(compileConfig);

		var runnableConfig_1 = RunnableConfig.builder().threadId("thread_1").build();

		var runnableConfig_2 = RunnableConfig.builder().threadId("thread_2").build();

		var state_1 = app.invoke(Map.of(), runnableConfig_1);

		assertTrue(state_1.isPresent());
		assertEquals(expectedSteps, (int) state_1.get().data().get("steps"));

		var tag = saver.release(runnableConfig_1);
		assertNotNull(tag);
		assertEquals("thread_1", tag.threadId());

		var tagState = tag.checkpoints().stream().map(Checkpoint::getState).findFirst();
		assertTrue(tagState.isEmpty());

		var messages = (List<String>) state_1.get().data().get("messages");

		assertEquals(expectedSteps, messages.size());

		for (int i = 0; i < messages.size(); i++) {
			assertEquals(format("agent_1:step %d", (i + 1)), messages.get(i));
		}

		var ex = assertThrowsExactly(IllegalStateException.class, () -> app.getState(runnableConfig_1));
		assertEquals("Missing Checkpoint!", ex.getMessage());

		// SUBMIT NEW THREAD 2

		var state_2 = app.invoke(emptyMap(), runnableConfig_2);

		assertTrue(state_2.isPresent());
		assertEquals(expectedSteps, (int) state_2.get().data().get("steps"));
		messages = (List<String>) state_2.get().data().get("messages");

		tag = saver.release(runnableConfig_2);
		assertEquals("thread_2", tag.threadId());
		assertNotNull(tag);

		tagState = tag.checkpoints().stream().map(Checkpoint::getState).findFirst();

		assertTrue(tagState.isEmpty());
		assertEquals(expectedSteps, messages.size());

		// RE-SUBMIT THREAD 1
		var dataFlux = app.graphResponseStream(Map.of(), runnableConfig_1);

		AtomicReference<Object> lastResult = new AtomicReference<>();
		state_1 = dataFlux.flatMap(data -> {
			if (data.isDone()) {
				// TODO, collect data.resultValue if necessary.
				lastResult.set(data.resultValue());
				return Flux.empty();
			}
			if (data.isError()) {
				return Mono.fromFuture(data.getOutput()).onErrorMap(throwable -> throwable).flux();
			}
			return Mono.fromFuture(data.getOutput()).flux();
		}).reduce((a, b) -> b).map(NodeOutput::state).blockOptional();

		assertTrue(state_1.isPresent());

		var result = (Optional<Object>) lastResult.get();

		assertTrue(result.isPresent());
		assertInstanceOf(BaseCheckpointSaver.Tag.class, result.get());

		tag = result.map(BaseCheckpointSaver.Tag.class::cast).orElseThrow();
		tagState = tag.checkpoints().stream().map(Checkpoint::getState).findFirst();

		assertTrue(tagState.isPresent());
		assertIterableEquals(state_1.get().data().entrySet(), tagState.get().entrySet());

	}


	@Test
	public void testFileSystemSaverMultipleRoundTrips() throws Exception {

		KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactoryBuilder()
			.addStrategy("counter")
			.addStrategy("graphResponse")
			.build();

		StateGraph workflow = new StateGraph(keyStrategyFactory)
			.addEdge(START, "node1")
			.addNode("node1", node_async(state -> {
				int counter = (int) state.value("counter").orElse(0);
				counter++;

				Map<String, Object> metadata = Map.of(
					"iteration", counter,
					"nodeId", "node1",
					"timestamp", System.currentTimeMillis()
				);
				GraphResponse<?> response = GraphResponse.of("Result " + counter, metadata);

				log.info("Node1 execution {}: created GraphResponse with metadata: {}", counter, metadata);

				return Map.of(
					"counter", counter,
					"graphResponse", response
				);
			}))
			.addEdge("node1", END);
		var saver = FileSystemSaver.builder()
			.targetFolder(Paths.get(rootPath, "bug3895-test"))
			.stateSerializer(workflow.getStateSerializer())
			.build();

		CompileConfig compileConfig = CompileConfig.builder()
			.saverConfig(SaverConfig.builder()
				.register(saver)
				.build())
			.build();

		CompiledGraph app = workflow.compile(compileConfig);

		String threadId = "bug3895-test-thread";
		RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();

		try {
			saver.deleteFile(config);

			for (int i = 0; i < 5; i++) {

				Optional<OverAllState> stateOpt = app.invoke(Map.of(), config);


				assertTrue(stateOpt.isPresent(), "State should be present");
				OverAllState result = stateOpt.get();
				assertEquals(i + 1, result.data().get("counter"), "Counter should be " + (i + 1));

				Object graphResponseObj = result.data().get("graphResponse");
				assertNotNull(graphResponseObj, "GraphResponse should not be null");
				assertTrue(graphResponseObj instanceof GraphResponse,
					"Should be GraphResponse instance");

				@SuppressWarnings("unchecked")
				GraphResponse<Object> graphResponse = (GraphResponse<Object>) graphResponseObj;

				Map<String, Object> metadata = graphResponse.getAllMetadata();
				assertFalse(metadata.containsKey("@class"),
					"Iteration " + (i + 1) + ": metadata should NOT contain @class field (Bug #3895)");
				assertFalse(metadata.containsKey("@type"),
					"Iteration " + (i + 1) + ": metadata should NOT contain @type field");
				assertFalse(metadata.containsKey("@typeHint"),
					"Iteration " + (i + 1) + ": metadata should NOT contain @typeHint field");

				assertEquals(3, metadata.size(),
					"Iteration " + (i + 1) + ": metadata should have exactly 3 fields (no accumulated type markers)");
				assertEquals(i + 1, metadata.get("iteration"),
					"Iteration metadata should match");
				assertEquals("node1", metadata.get("nodeId"),
					"NodeId should be preserved");
				assertNotNull(metadata.get("timestamp"),
					"Timestamp should be preserved");


				StateSnapshot snapshot = app.getState(config);
				assertNotNull(snapshot, "Snapshot should not be null");

				OverAllState snapshotState = snapshot.state();
				assertNotNull(snapshotState, "Snapshot state should not be null");
				assertEquals(i + 1, snapshotState.data().get("counter"),
					"Snapshot counter should match");

			}

		} finally {
			saver.deleteFile(config);
		}
	}

	@Test
	public void testNestedGraphResponseWithFileSystemSaver() throws Exception {

		KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactoryBuilder()
			.addStrategy("data")
			.build();

		StateGraph workflow = new StateGraph(keyStrategyFactory)
			.addEdge(START, "node1")
			.addNode("node1", node_async(state -> {
				GraphResponse<?> innerResponse = GraphResponse.of(
					"Inner result",
					Map.of("inner_key", "inner_value")
				);

				GraphResponse<?> outerResponse = GraphResponse.of(
					Map.of("nested", innerResponse, "other", "data"),
					Map.of("outer_key", "outer_value")
				);

				return Map.of("data", outerResponse);
			}))
			.addEdge("node1", END);

		var saver = FileSystemSaver.builder()
			.targetFolder(Paths.get(rootPath, "bug3895-nested-test"))
			.stateSerializer(workflow.getStateSerializer())
			.build();

		CompileConfig compileConfig = CompileConfig.builder()
			.saverConfig(SaverConfig.builder()
				.register(saver)
				.build())
			.build();

		CompiledGraph app = workflow.compile(compileConfig);
		RunnableConfig config = RunnableConfig.builder().threadId("nested-test").build();

		try {
			saver.deleteFile(config);

			for (int i = 0; i < 3; i++) {

				Optional<OverAllState> stateOpt = app.invoke(Map.of(), config);
				assertTrue(stateOpt.isPresent(), "State should be present");
				OverAllState result = stateOpt.get();

				Object dataObj = result.data().get("data");
				assertNotNull(dataObj, "Data should not be null");
				assertTrue(dataObj instanceof GraphResponse, "Should be GraphResponse");

				@SuppressWarnings("unchecked")
				GraphResponse<Object> outerResponse = (GraphResponse<Object>) dataObj;

				Map<String, Object> outerMetadata = outerResponse.getAllMetadata();
				assertFalse(outerMetadata.containsKey("@class"),
					"Outer metadata should NOT contain @class");
				assertEquals(1, outerMetadata.size(),
					"Outer metadata should have 1 field");

				Object resultValue = outerResponse.resultValue().orElse(null);
				assertNotNull(resultValue, "Result value should not be null");
				assertTrue(resultValue instanceof Map, "Result should be a Map");

				@SuppressWarnings("unchecked")
				Map<String, Object> resultMap = (Map<String, Object>) resultValue;

				Object nestedObj = resultMap.get("nested");
				assertTrue(nestedObj instanceof GraphResponse,
					"Nested value should be GraphResponse");

				@SuppressWarnings("unchecked")
				GraphResponse<Object> innerResponse = (GraphResponse<Object>) nestedObj;
				Map<String, Object> innerMetadata = innerResponse.getAllMetadata();
				assertFalse(innerMetadata.containsKey("@class"),
					"Inner metadata should NOT contain @class");
				assertEquals(1, innerMetadata.size(),
					"Inner metadata should have 1 field");

			}

		} finally {
			saver.deleteFile(config);
		}
	}


	@Test
	public void testUserScenarioWithoutExplicitMetadata() throws Exception {

		KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactoryBuilder()
			.addStrategy("messages")
			.addStrategy("lastResponse")
			.build();

		StateGraph workflow = new StateGraph(keyStrategyFactory)
			.addEdge(START, "processNode")
			.addNode("processNode", node_async(state -> {
				@SuppressWarnings("unchecked")
				var messages = (java.util.List<String>) state.value("messages").orElse(new java.util.ArrayList<String>());
				var newMessages = new java.util.ArrayList<>(messages);
				newMessages.add("Message " + (messages.size() + 1));

				GraphResponse<?> response = GraphResponse.of(
					"Processed message " + newMessages.size()
				);


				return Map.of(
					"messages", newMessages,
					"lastResponse", response
				);
			}))
			.addEdge("processNode", END);
		var saver = FileSystemSaver.builder()
			.targetFolder(Paths.get(rootPath, "bug3895-user-scenario"))
			.stateSerializer(workflow.getStateSerializer())
			.build();

		CompileConfig compileConfig = CompileConfig.builder()
			.saverConfig(SaverConfig.builder()
				.register(saver)
				.build())
			.build();

		CompiledGraph app = workflow.compile(compileConfig);
		String threadId = "user-scenario-thread";
		RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();

		try {
			saver.deleteFile(config);

			for (int round = 0; round < 5; round++) {

				Optional<OverAllState> stateOpt = app.invoke(Map.of(), config);
				assertTrue(stateOpt.isPresent(), "State should be present");

				OverAllState result = stateOpt.get();
				var messages = (java.util.List<?>) result.data().get("messages");
				assertNotNull(messages, "Messages should not be null");
				assertEquals(round + 1, messages.size(),
					"Round " + (round + 1) + ": Should have " + (round + 1) + " messages");

				Object lastResponseObj = result.data().get("lastResponse");
				assertNotNull(lastResponseObj, "LastResponse should not be null");
				assertTrue(lastResponseObj instanceof GraphResponse,
					"LastResponse should be GraphResponse instance");

				@SuppressWarnings("unchecked")
				GraphResponse<Object> lastResponse = (GraphResponse<Object>) lastResponseObj;

				Map<String, Object> metadata = lastResponse.getAllMetadata();

				assertFalse(metadata.containsKey("@class"),
					"Round " + (round + 1) + ": metadata should NOT contain @class field");
				assertFalse(metadata.containsKey("@type"),
					"Round " + (round + 1) + ": metadata should NOT contain @type field");
				assertFalse(metadata.containsKey("@typeHint"),
					"Round " + (round + 1) + ": metadata should NOT contain @typeHint field");

				StateSnapshot snapshot = app.getState(config);
				assertNotNull(snapshot, "Snapshot should not be null");

				OverAllState snapshotState = snapshot.state();
				assertNotNull(snapshotState, "Snapshot state should not be null");

				Object restoredResponseObj = snapshotState.data().get("lastResponse");
				if (restoredResponseObj instanceof GraphResponse) {
					@SuppressWarnings("unchecked")
					GraphResponse<Object> restoredResponse = (GraphResponse<Object>) restoredResponseObj;
					Map<String, Object> restoredMetadata = restoredResponse.getAllMetadata();

					assertFalse(restoredMetadata.containsKey("@class"),
						"Round " + (round + 1) + ": Restored metadata should NOT contain @class");

				}
			}

		} finally {
			saver.deleteFile(config);
		}
	}

	@Test
	public void testSameThreadIdMultipleInvocations() throws Exception {

		KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactoryBuilder()
			.addStrategy("counter")
			.addStrategy("response")
			.build();

		StateGraph workflow = new StateGraph(keyStrategyFactory)
			.addEdge(START, "node1")
			.addNode("node1", node_async(state -> {
				int counter = (int) state.value("counter").orElse(0);
				counter++;

				Map<String, Object> metadata = Map.of(
					"requestId", "req-" + counter,
					"timestamp", System.currentTimeMillis(),
					"nodeId", "node1"
				);
				GraphResponse<?> response = GraphResponse.of("Result " + counter, metadata);

				return Map.of(
					"counter", counter,
					"response", response
				);
			}))
			.addEdge("node1", END);

		var saver = FileSystemSaver.builder()
			.targetFolder(Paths.get(rootPath, "same-threadid-test"))
			.stateSerializer(workflow.getStateSerializer())
			.build();

		CompileConfig compileConfig = CompileConfig.builder()
			.saverConfig(SaverConfig.builder()
				.register(saver)
				.build())
			.build();

		CompiledGraph app = workflow.compile(compileConfig);

		String threadId = "same-thread-id";
		RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();

		try {
			saver.deleteFile(config);

			for (int i = 0; i < 10; i++) {

				Optional<OverAllState> stateOpt = app.invoke(Map.of(), config);

				assertTrue(stateOpt.isPresent(), "State should be present");
				OverAllState result = stateOpt.get();
				assertEquals(i + 1, result.data().get("counter"), "Counter should be " + (i + 1));

				Object responseObj = result.data().get("response");
				assertNotNull(responseObj, "Response should not be null");
				assertTrue(responseObj instanceof GraphResponse, "Should be GraphResponse instance");

				@SuppressWarnings("unchecked")
				GraphResponse<Object> graphResponse = (GraphResponse<Object>) responseObj;

				Map<String, Object> metadata = graphResponse.getAllMetadata();

				assertFalse(metadata.containsKey("@class"),
					"Invocation " + (i + 1) + ": metadata should NOT contain @class field");
				assertFalse(metadata.containsKey("@type"),
					"Invocation " + (i + 1) + ": metadata should NOT contain @type field");
				assertFalse(metadata.containsKey("@typeHint"),
					"Invocation " + (i + 1) + ": metadata should NOT contain @typeHint field");

				assertEquals(3, metadata.size(),
					"Invocation " + (i + 1) + ": metadata should have exactly 3 fields");
				assertEquals("req-" + (i + 1), metadata.get("requestId"),
					"RequestId should match");
				assertEquals("node1", metadata.get("nodeId"),
					"NodeId should be preserved");
				assertNotNull(metadata.get("timestamp"),
					"Timestamp should be preserved");


				StateSnapshot snapshot = app.getState(config);
				assertNotNull(snapshot, "Snapshot should not be null");

				OverAllState snapshotState = snapshot.state();
				assertNotNull(snapshotState, "Snapshot state should not be null");
				assertEquals(i + 1, snapshotState.data().get("counter"),
					"Snapshot counter should match");

				Object snapshotResponseObj = snapshotState.data().get("response");
				if (snapshotResponseObj instanceof GraphResponse) {
					@SuppressWarnings("unchecked")
					GraphResponse<Object> snapshotResponse = (GraphResponse<Object>) snapshotResponseObj;
					Map<String, Object> snapshotMetadata = snapshotResponse.getAllMetadata();

					assertFalse(snapshotMetadata.containsKey("@class"),
						"Invocation " + (i + 1) + ": Snapshot metadata should NOT contain @class");
					assertFalse(snapshotMetadata.containsKey("@type"),
						"Invocation " + (i + 1) + ": Snapshot metadata should NOT contain @type");
					assertFalse(snapshotMetadata.containsKey("@typeHint"),
						"Invocation " + (i + 1) + ": Snapshot metadata should NOT contain @typeHint");

				}
			}

		} finally {
			saver.deleteFile(config);
		}
	}

}

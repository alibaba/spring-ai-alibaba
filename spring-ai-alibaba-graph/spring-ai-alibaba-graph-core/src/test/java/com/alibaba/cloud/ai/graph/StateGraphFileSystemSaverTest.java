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

import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant;
import com.alibaba.cloud.ai.graph.checkpoint.savers.FileSystemSaver;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.*;

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

		var saver = new FileSystemSaver(Paths.get(rootPath, "testCheckpointSaverResubmit"),
				workflow.getStateSerializer());

		CompileConfig compileConfig = CompileConfig.builder()
			.saverConfig(SaverConfig.builder().type(SaverConstant.FILE).register(SaverConstant.FILE, saver).build())
			.build();

		CompiledGraph app = workflow.compile(compileConfig);

		RunnableConfig runnableConfig_1 = RunnableConfig.builder().threadId("thread_1").build();

		RunnableConfig runnableConfig_2 = RunnableConfig.builder().threadId("thread_2").build();

		try {

			for (int execution = 0; execution < 2; execution++) {

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

			// saver.clear(runnableConfig_1);
			// saver.clear(runnableConfig_2);
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

		var saver = new FileSystemSaver(Paths.get(rootPath, "testCheckpointSaverWithManualRelease"),
				workflow.getStateSerializer());

		CompileConfig compileConfig = CompileConfig.builder()
			.saverConfig(SaverConfig.builder().type(SaverConstant.FILE).register(SaverConstant.FILE, saver).build())
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

		var saver = new FileSystemSaver(Paths.get(rootPath, "testCheckpointSaverWithAutoRelease"),
				workflow.getStateSerializer());

		CompileConfig compileConfig = CompileConfig.builder()
			.saverConfig(SaverConfig.builder().type(SaverConstant.FILE).register(SaverConstant.FILE, saver).build())
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
		var iterator = app.stream(Map.of(), runnableConfig_1);

		state_1 = iterator.stream().reduce((a, b) -> b).map(NodeOutput::state);
		assertTrue(state_1.isPresent());
		assertInstanceOf(AsyncGenerator.HasResultValue.class, iterator);

		var result = ((AsyncGenerator.HasResultValue) iterator).resultValue();

		assertTrue(result.isPresent());
		assertInstanceOf(BaseCheckpointSaver.Tag.class, result.get());

		tag = result.map(BaseCheckpointSaver.Tag.class::cast).orElseThrow();
		tagState = tag.checkpoints().stream().map(Checkpoint::getState).findFirst();

		assertTrue(tagState.isPresent());
		assertIterableEquals(state_1.get().data().entrySet(), tagState.get().entrySet());

	}

}

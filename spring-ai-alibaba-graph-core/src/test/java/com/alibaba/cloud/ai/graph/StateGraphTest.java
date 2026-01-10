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

import com.alibaba.cloud.ai.graph.action.*;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.async.AsyncGeneratorQueue;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.serializer.plain_text.PlainTextStateSerializer;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.alibaba.cloud.ai.graph.state.AppenderChannel;
import com.alibaba.cloud.ai.graph.state.RemoveByHash;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.cloud.ai.graph.utils.EdgeMappings;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.NamedExecutable;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.*;

public class StateGraphTest {

	private static final Logger log = LoggerFactory.getLogger(StateGraphTest.class);

	/**
	 * Sorts a map by its keys and returns a list of entries.
	 *
	 * @param map The map to be sorted.
	 * @return A list of map entries sorted by key.
	 */
	public static <T> List<Entry<String, T>> sortMap(Map<String, T> map) {
		return map.entrySet().stream().sorted(Entry.comparingByKey()).collect(Collectors.toList());
	}

	/**
	 * Filters out internal keys (starting with underscore) from a map.
	 * This is useful for test assertions to ignore automatically-injected keys like
	 * _graph_execution_id_.
	 *
	 * @param map The map to filter.
	 * @return A new map without internal keys.
	 */
	public static <T> Map<String, T> filterInternalKeys(Map<String, T> map) {
		return map.entrySet().stream()
				.filter(entry -> !entry.getKey().startsWith("_"))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}

	/**
	 * Removes an element from the list based on the provided RemoveIdentifier.
	 */
	private static void removeFromList(List<Object> result, AppenderChannel.RemoveIdentifier<Object> removeIdentifier) {
		for (int i = 0; i < result.size(); i++) {
			if (removeIdentifier.compareTo(result.get(i), i) == 0) {
				result.remove(i);
				break;
			}
		}
	}

	/**
	 * Evaluates removal operations on a list based on RemoveIdentifiers in
	 * newValues.
	 */
	private static AppenderChannel.RemoveData<Object> evaluateRemoval(List<Object> oldValues, List<?> newValues) {

		final var result = new AppenderChannel.RemoveData<>(oldValues, newValues);

		newValues.stream().filter(value -> value instanceof AppenderChannel.RemoveIdentifier<?>).forEach(value -> {
			result.newValues().remove(value);
			var removeIdentifier = (AppenderChannel.RemoveIdentifier<Object>) value;
			removeFromList(result.oldValues(), removeIdentifier);
		});
		return result;

	}

	/**
	 * Creates an OverAllState instance with predefined strategies for testing
	 * purposes.
	 */
	private static KeyStrategyFactory createKeyStrategyFactory() {
		return () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("steps", (o, o2) -> o2);
			keyStrategyMap.put("messages", new AppendStrategy());
			return keyStrategyMap;
		};
	}

	/**
	 * Tests the validation logic of the StateGraph, ensuring proper exceptions are
	 * thrown
	 * when the graph is not correctly configured.
	 */
	@Test
	void testValidation() throws Exception {

		StateGraph workflow = new StateGraph();
		GraphStateException exception = assertThrows(GraphStateException.class, workflow::compile);
		System.out.println(exception.getMessage());
		assertEquals("missing Entry Point", exception.getMessage());

		workflow.addEdge(START, "agent_1");

		exception = assertThrows(GraphStateException.class, workflow::compile);
		assertEquals("edge sourceId 'agent_1' refers to undefined node!", exception.getMessage());

		workflow.addNode("agent_1", node_async((state) -> {
			log.info("agent_1\n{}", state);
			return Map.of("prop1", "test");
		}));

		assertNotNull(workflow.compile());

		workflow.addEdge("agent_1", END);

		assertNotNull(workflow.compile());

		exception = assertThrows(GraphStateException.class, () -> workflow.addEdge(END, "agent_1"));
		log.info("{}", exception.getMessage());

		workflow.addNode("agent_2", node_async(state -> {
			log.info("agent_2\n{}", state);
			return Map.of("prop2", "test");
		}));

		workflow.addEdge("agent_2", "agent_3");

		exception = assertThrows(GraphStateException.class, workflow::compile);
		log.info("{}", exception.getMessage());

		exception = assertThrows(GraphStateException.class,
				() -> workflow.addConditionalEdges("agent_1", edge_async(state -> "agent_3"), Map.of()));
		log.info("{}", exception.getMessage());

	}

	/**
	 * Tests a simple graph with one node that updates the state.
	 */
	@Test
	public void testRunningOneNode() throws Exception {
		StateGraph workflow = new StateGraph(() -> {
			HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
			keyStrategyHashMap.put("prop1", (o, o2) -> o2);
			return keyStrategyHashMap;
		}).addEdge(START, "agent_1").addNode("agent_1", node_async(state -> {
			log.info("agent_1\n{}", state);
			return Map.of("prop1", "test");
		})).addEdge("agent_1", END);

		CompiledGraph app = workflow.compile();

		Optional<OverAllState> result = app.invoke(Map.of(OverAllState.DEFAULT_INPUT_KEY, "test1"));
		System.out.println("result = " + result);
		assertTrue(result.isPresent());

		Map<String, String> expected = Map.of("input", "test1", "prop1", "test");
		assertIterableEquals(sortMap(expected), sortMap(filterInternalKeys(result.get().data())));
	}

	/**
	 * Tests a graph where nodes append messages to a shared list.
	 */
	@Test
	void testWithAppender() throws Exception {
		StateGraph workflow = new StateGraph(createKeyStrategyFactory()).addNode("agent_1", node_async(state -> {
			System.out.println("agent_1");
			return Map.of("messages", "message1");
		})).addNode("agent_2", node_async(state -> {
			System.out.println("agent_2");
			return Map.of("messages", new String[] { "message2" });
		})).addNode("agent_3", node_async(state -> {
			System.out.println("agent_3");
			List<String> messages = Optional.ofNullable(state.value("messages").get())
					.filter(List.class::isInstance)
					.map(List.class::cast)
					.orElse(new ArrayList<>());

			int steps = messages.size() + 1;

			return Map.of("messages", "message3", "steps", steps);
		}))
				.addEdge("agent_1", "agent_2")
				.addEdge("agent_2", "agent_3")
				.addEdge(StateGraph.START, "agent_1")
				.addEdge("agent_3", StateGraph.END);

		CompiledGraph app = workflow.compile();

		Optional<OverAllState> result = app.invoke(Map.of());

		assertTrue(result.isPresent());
		System.out.println(result.get().data());
		List<String> listResult = (List<String>) result.get().value("messages").get();
		assertIterableEquals(List.of("message1", "message2", "message3"), listResult);

	}

	@Test
	public void testRunnableConfigMetadata() throws Exception {

		// Create an async node action that validates metadata in the config
		var agent = AsyncNodeActionWithConfig.node_async((state, config) -> {

			// Verify that the metadata "configData" is present in the configuration
			assertTrue(config.metadata("configData").isPresent());

			log.info("agent_1\n{}", state);
			return Map.of("prop1", "test");
		});

		// Build a workflow with a custom key strategy using ReplaceStrategy for "prop1"
		var workflow = new StateGraph(() -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("prop1", new ReplaceStrategy());
			return keyStrategyMap;
		}).addEdge(START, "agent_1").addNode("agent_1", agent).addEdge("agent_1", END);

		// Compile the workflow into a runnable graph
		var app = workflow.compile();

		// Configure RunnableConfig with metadata to be passed during execution
		var config = RunnableConfig.builder().addMetadata("configData", "test").build();

		// Execute the graph with input and configured metadata
		var result = app.invoke(Map.of("input", "test1"), config);
		assertTrue(result.isPresent());

		// Expected output after execution
		Map<String, String> expected = Map.of("input", "test1", "prop1", "test");

		// Validate the actual output matches the expected values
		assertIterableEquals(sortMap(expected), sortMap(filterInternalKeys(result.get().data())));
	}

	/**
	 * Tests message appending and single message removal in a graph flow.
	 */
	@Test
	void testWithAppenderOneRemove() throws Exception {
		StateGraph workflow = new StateGraph(createKeyStrategyFactory()).addNode("agent_1", node_async(state -> {
			log.info("agent_1");
			return Map.of("messages", "message1");
		})).addNode("agent_2", node_async(state -> {
			log.info("agent_2");
			return Map.of("messages", new String[] { "message2" });
		})).addNode("agent_3", node_async(state -> {
			System.out.println("agent_3");
			List<String> messages = Optional.ofNullable(state.value("messages").get())
					.filter(List.class::isInstance)
					.map(List.class::cast)
					.orElse(new ArrayList<>());

			int steps = messages.size() + 1;
			return Map.of("messages", RemoveByHash.of("message2"), "steps", steps);
		}))
				.addEdge("agent_1", "agent_2")
				.addEdge("agent_2", "agent_3")
				.addEdge(START, "agent_1")
				.addEdge("agent_3", END);

		CompiledGraph app = workflow.compile();

		Optional<OverAllState> result = app.invoke(Map.of());

		assertTrue(result.isPresent());
		log.info("{}", result.get().data());
		List<String> messages = (List<String>) result.get().value("messages").get();
		assertEquals(3, result.get().value("steps").get());
		assertEquals(1, messages.size());
		assertIterableEquals(List.of("message1"), messages);

	}

	/**
	 * Tests combining both appending and removing messages in a multi-step graph
	 * flow.
	 */
	@Test
	void testWithAppenderOneAppendOneRemove() throws Exception {
		StateGraph workflow = new StateGraph(createKeyStrategyFactory())
				.addNode("agent_1", node_async(state -> Map.of("messages", "message1")))
				.addNode("agent_2", node_async(state -> Map.of("messages", new String[] { "message2" })))
				.addNode("agent_3",
						node_async(state -> Map.of("messages", List.of("message3", RemoveByHash.of("message2")))))
				.addNode("agent_4", node_async(state -> {
					System.out.println("agent_3");
					List messages = Optional.of(state.value("messages").get())
							.filter(List.class::isInstance)
							.map(List.class::cast)
							.orElse(new ArrayList<>());

					int steps = messages.size() + 1;
					return Map.of("messages", List.of("message4"), "steps", steps);
				}))
				.addEdge("agent_1", "agent_2")
				.addEdge("agent_2", "agent_3")
				.addEdge("agent_3", "agent_4")
				.addEdge(START, "agent_1")
				.addEdge("agent_4", END);

		CompiledGraph app = workflow.compile();

		Optional<OverAllState> result = app.invoke(Map.of());

		assertTrue(result.isPresent());

		System.out.println(result.get().data());
		List<String> messages = (List<String>) result.get().value("messages").get();
		assertEquals(3, result.get().value("steps").get());
		assertEquals(3, messages.size());
		assertIterableEquals(List.of("message1", "message3", "message4"), messages);

	}

	/**
	 * Tests subgraph functionality where one graph is embedded within another.
	 */
	@Test
	public void testWithSubgraph() throws Exception {

		var childStep1 = node_async((OverAllState state) -> Map.of("messages", "child:step1"));

		var childStep2 = node_async((OverAllState state) -> Map.of("messages", "child:step2"));

		var childStep3 = node_async((OverAllState state) -> Map.of("messages", "child:step3"));

		var workflowChild = new StateGraph().addNode("child:step_1", childStep1)
				.addNode("child:step_2", childStep2)
				.addNode("child:step_3", childStep3)
				.addEdge(START, "child:step_1")
				.addEdge("child:step_1", "child:step_2")
				.addEdge("child:step_2", "child:step_3")
				.addEdge("child:step_3", END);

		var step1 = node_async((OverAllState state) -> Map.of("messages", "step1"));

		var step2 = node_async((OverAllState state) -> Map.of("messages", "step2"));

		var step3 = node_async((OverAllState state) -> Map.of("messages", "step3"));

		var workflowParent = new StateGraph(createKeyStrategyFactory()).addNode("step_1", step1)
				.addNode("step_2", step2)
				.addNode("step_3", step3)
				.addNode("subgraph", workflowChild)
				.addEdge(START, "step_1")
				.addEdge("step_1", "step_2")
				.addEdge("step_2", "subgraph")
				.addEdge("subgraph", "step_3")
				.addEdge("step_3", END)
				.compile();

		// 使用实时流式处理，收集最后一个状态
		final OverAllState[] finalState = new OverAllState[1];
		workflowParent.stream(Map.of())
				.doOnNext(System.out::println) // 实时输出每个节点执行结果
				.map(NodeOutput::state)
				.doOnNext(state -> finalState[0] = state) // 保存最后的状态
				.blockLast(); // 只等待流完成，不阻塞中间过程

		assertTrue(finalState[0] != null);
		assertIterableEquals(List.of("step1", "step2", "child:step1", "child:step2", "child:step3", "step3"),
				(List<Object>) finalState[0].value("messages").get());

	}

	/**
	 * Helper method to create a node action with logging functionality.
	 */
	private AsyncNodeAction makeNode(String id) {
		return node_async(state -> {
			log.info("call node {}", id);
			if (id.equalsIgnoreCase("A2")) {
				log.info("sleep A2");
				Thread.sleep(2000);
			}
			return Map.of("messages", id);
		});
	}

	private AsyncNodeAction makeNodeForStream(String id) {
		return node_async(state -> {
			log.info("call node {}", id);
			final AsyncGenerator<NodeOutput> it = AsyncGeneratorQueue.of(new LinkedBlockingQueue<>(), queue -> {
				for (int i = 0; i < 10; ++i) {
					queue.add(AsyncGenerator.Data.of(completedFuture(new StreamingOutput(id + i, id, state))));
				}
			});

			return Map.of("messages", it);
		});
	}

	/**
	 * Tests parallel branch execution in a graph.
	 */
	@Test
	void testWithParallelBranch() throws Exception {
		var workflow = new StateGraph(createKeyStrategyFactory()).addNode("A", makeNode("A"))
				.addNode("A1", makeNode("A1"))
				.addNode("A2", makeNode("A2"))
				.addNode("A3", makeNode("A3"))
				.addNode("B", makeNode("B"))
				.addNode("C", makeNode("C"))
				.addEdge("A", "A1")
				.addEdge("A", "A2")
				.addEdge("A", "A3")
				.addEdge("A1", "B")
				.addEdge("A2", "B")
				.addEdge("A3", "B")
				.addEdge("B", "C")
				.addEdge(START, "A")
				.addEdge("C", END);

		var app = workflow.compile(CompileConfig.builder().withLifecycleListener(new GraphLifecycleListener() {
			@Override
			public void before(String nodeId, Map<String, Object> state, RunnableConfig config, Long curTime) {
				log.info("node before ,node = {},state = {}", nodeId, state);
			}

			@Override
			public void after(String nodeId, Map<String, Object> state, RunnableConfig config, Long curTime) {
				log.info("node after ,node = {},state = {}", nodeId, state);
			}
		}).build());

		final OverAllState[] finalState = new OverAllState[1];
		app.stream(Map.of(),
				RunnableConfig.builder().addParallelNodeExecutor("A", ForkJoinPool.commonPool()).build())
				.doOnNext(output -> System.out.println(output))
				.map(NodeOutput::state)
				.doOnNext(state -> finalState[0] = state)
				.blockLast();

		assertTrue(finalState[0] != null);
		List<String> messages = (List<String>) finalState[0].value("messages").get();
		log.info("messages: {}", messages);

		// 验证所有节点都被执行，但不关心并行节点的顺序
		assertEquals("A", messages.get(0)); // A 应该是第一个
		assertEquals("B", messages.get(messages.size() - 2)); // B 应该是倒数第二个
		assertEquals("C", messages.get(messages.size() - 1)); // C 应该是最后一个

		// 验证并行节点 A1, A2, A3 都在结果中
		assertTrue(messages.contains("A1"), "A1 should be in the result");
		assertTrue(messages.contains("A2"), "A2 should be in the result");
		assertTrue(messages.contains("A3"), "A3 should be in the result");

		// 验证总长度正确
		assertEquals(6, messages.size(), "Should have 6 messages: A, A1, A2, A3, B, C");

		workflow = new StateGraph(createKeyStrategyFactory()).addNode("A", makeNode("A"))
				.addNode("A1", makeNode("A1"))
				.addNode("A2", makeNode("A2"))
				.addNode("A3", makeNode("A3"))
				.addNode("B", makeNode("B"))
				.addNode("C", makeNode("C"))
				.addEdge("A1", "B")
				.addEdge("A2", "B")
				.addEdge("A3", "B")
				.addEdge("B", "C")
				.addEdge(START, "A1")
				.addEdge(START, "A2")
				.addEdge(START, "A3")
				.addEdge("C", END);

		app = workflow.compile();

		// 第二个测试也使用实时流式处理
		final OverAllState[] finalState2 = new OverAllState[1];
		app.stream(Map.of(),
				RunnableConfig.builder().addParallelNodeExecutor(START, Executors.newSingleThreadExecutor()).build())
				.doOnNext(output -> System.out.println(output))
				.map(NodeOutput::state)
				.doOnNext(state -> finalState2[0] = state)
				.blockLast();

		assertTrue(finalState2[0] != null);
		List<String> messages2 = (List<String>) finalState2[0].value("messages").get();

		// 验证所有节点都被执行，但不关心并行节点的顺序
		assertEquals("B", messages2.get(messages2.size() - 2)); // B 应该是倒数第二个
		assertEquals("C", messages2.get(messages2.size() - 1)); // C 应该是最后一个

		// 验证并行节点 A1, A2, A3 都在结果中
		assertTrue(messages2.contains("A1"), "A1 should be in the result");
		assertTrue(messages2.contains("A2"), "A2 should be in the result");
		assertTrue(messages2.contains("A3"), "A3 should be in the result");

		// 验证总长度正确
		assertEquals(5, messages2.size(), "Should have 5 messages: A1, A2, A3, B, C");

	}

	/**
	 * Tests parallel branch execution in a graph.
	 */
	@Test
	void testWithParallelBranchWithStream() throws GraphStateException {
		var workflow = new StateGraph(createKeyStrategyFactory()).addNode("A", makeNode("A"))
				.addNode("A1", makeNodeForStream("A1"))
				.addNode("A2", makeNodeForStream("A2"))
				.addNode("C", makeNode("C"))
				.addEdge("A", "A1")
				.addEdge("A", "A2")
				.addEdge("A1", "C")
				.addEdge("A2", "C")
				.addEdge(START, "A")
				.addEdge("C", END);
		var app = workflow.compile();

		app.stream(Map.of()).subscribe(output -> {
			System.out.println("Node output: " + output);
		});
	}

	@Test
	void testCommandNode() throws Exception {

		AsyncCommandAction commandAction = (state,
				config) -> completedFuture(new Command("C2", Map.of("messages", "B", "next_node", "C2")));

		var graph = new StateGraph().addNode("A", makeNode("A"))
				.addNode("B", commandAction, EdgeMappings.builder().toEND().to("C1").to("C2").build())
				.addNode("C1", makeNode("C1"))
				.addNode("C2", makeNode("C2"))
				.addEdge(START, "A")
				.addEdge("A", "B")
				.addEdge("C1", END)
				.addEdge("C2", END)
				.compile();

		// 使用实时流式处理，收集所有步骤用于测试验证
		final List<NodeOutput> allSteps = new ArrayList<>();
		graph.stream(Map.of())
				.doOnNext(System.out::println) // 实时输出每个节点执行结果
				.doOnNext(allSteps::add) // 收集所有步骤
				.blockLast(); // 只等待流完成，不阻塞中间过程

		assertEquals(5, allSteps.size());
		assertEquals("B", allSteps.get(2).node());
		assertEquals("C2", allSteps.get(2).state().value("next_node").orElse(null));

	}

	@Test
	public void testRunnableInterrupt() throws Exception {
		KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactoryBuilder().addStrategy("prop1", (o, o2) -> o2)
				.build();

		StateGraph workflow = new StateGraph(keyStrategyFactory).addEdge(START, "agent_1")
				.addEdge("agent_1", "agent_2")
				.addNode("agent_1", AsyncNodeActionWithConfig.node_async((state, config) -> {
					log.info("agent_1\n{}", state);
					config.markNodeAsInterrupted("agent_1");
					return Map.of("prop1", "test");
				}))
				.addNode("agent_2", AsyncNodeActionWithConfig.node_async((state, config) -> {
					log.info("agent_2\n{}", state);
					return Map.of("prop1", "test_2");
				}))
				.addEdge("agent_2", END);

		CompiledGraph app = workflow.compile();
		RunnableConfig runnableConfig = new RunnableConfig.Builder().threadId("thread1").build();
		Optional<OverAllState> result = app.invoke(Map.of(OverAllState.DEFAULT_INPUT_KEY, "test1"), runnableConfig);
		System.out.println("result = " + result);
		assertTrue(result.isPresent());

		// resume - 使用实时流式处理
		final OverAllState[] resumeState = new OverAllState[1];
		app.stream(null, runnableConfig)
				.doOnNext(output -> System.out.println("Resume: " + output)) // 实时输出恢复过程
				.map(NodeOutput::state)
				.doOnNext(state -> resumeState[0] = state) // 保存最后的状态
				.blockLast(); // 只等待流完成，不阻塞中间过程

		assertTrue(resumeState[0] != null);
		System.out.println("final result = " + Optional.of(resumeState[0]));

	}

	/**
	 * Tests error conditions related to parallel branches in graph configuration.
	 */
	@Test
	void testWithParallelBranchWithErrors() throws Exception {
		var onlyOneTarget = new StateGraph(createKeyStrategyFactory()).addNode("A", makeNode("A"))
				.addNode("A1", makeNode("A1"))
				.addNode("A2", makeNode("A2"))
				.addNode("A3", makeNode("A3"))
				.addNode("B", makeNode("B"))
				.addNode("C", makeNode("C"))
				.addEdge("A", "A1")
				.addEdge("A", "A2")
				.addEdge("A", "A3")
				.addEdge("A1", "B")
				.addEdge("A2", "B")
				.addEdge("A3", "C")
				.addEdge("B", "C")
				.addEdge(START, "A")
				.addEdge("C", END);

		var exception = assertThrows(GraphStateException.class, onlyOneTarget::compile);
		assertEquals("parallel node [A] must have only one target, but [B, C] have been found!",
				exception.getMessage());

		var noConditionalEdge = new StateGraph(createKeyStrategyFactory()).addNode("A", makeNode("A"))
				.addNode("A1", makeNode("A1"))
				.addNode("A2", makeNode("A2"))
				.addNode("A3", makeNode("A3"))
				.addNode("B", makeNode("B"))
				.addNode("C", makeNode("C"))
				.addEdge("A", "A1")
				.addEdge("A", "A3")
				.addEdge("A1", "B")
				.addEdge("A2", "B")
				.addEdge("A3", "B")
				.addEdge("B", "C")
				.addEdge(START, "A")
				.addEdge("C", END);

		exception = assertThrows(GraphStateException.class,
				() -> noConditionalEdge.addConditionalEdges("A", edge_async(state -> "next"), Map.of("next", "A2")));
		assertEquals("conditional edge from 'A' already exist!", exception.getMessage());

		var noConditionalEdgeOnBranch = new StateGraph(createKeyStrategyFactory()).addNode("A", makeNode("A"))
			.addNode("A1", makeNode("A1"))
			.addNode("A2", makeNode("A2"))
			.addNode("A3", makeNode("A3"))
			.addNode("B", makeNode("B"))
			.addNode("C", makeNode("C"))
			.addEdge("A", "A1")
			.addEdge("A", "A2")
			.addEdge("A", "A3")
			.addEdge("A1", "B")
			.addEdge("A2", "B")
			.addConditionalEdges("A3", edge_async(state -> "next"), Map.of("next", "B"))
			.addEdge("B", "C")
			.addEdge(START, "A")
			.addEdge("C", END);

		exception = assertThrows(GraphStateException.class, noConditionalEdgeOnBranch::compile);
		assertEquals(
				"parallel node doesn't support conditional branch, but on [A] a conditional branch on [A3] have been found!",
				exception.getMessage());

		var noDuplicateTarget = new StateGraph(createKeyStrategyFactory()).addNode("A", makeNode("A"))
				.addNode("A1", makeNode("A1"))
				.addNode("A2", makeNode("A2"))
				.addNode("A3", makeNode("A3"))
				.addNode("B", makeNode("B"))
				.addNode("C", makeNode("C"))
				.addEdge("A", "A1")
				.addEdge("A", "A2")
				.addEdge("A", "A3")
				.addEdge("A", "A2")
				.addEdge("A1", "B")
				.addEdge("A2", "B")
				.addEdge("A3", "B")
				.addEdge("B", "C")
				.addEdge(START, "A")
				.addEdge("C", END);

		exception = assertThrows(GraphStateException.class, noDuplicateTarget::compile);
		assertEquals("edge [A] has duplicate targets [A2]!", exception.getMessage());

	}

	/**
	 * Tests serialization capabilities of the StateGraph using different
	 * serializers.
	 */
	@Test
	public void testWithSubSerialize() throws Exception {
		KeyStrategyFactory keyStrategyFactory = () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("prop1", (o, o2) -> o2);
			return keyStrategyMap;
		};
		PlainTextStateSerializer plainTextStateSerializer = new SpringAIJacksonStateSerializer(OverAllState::new,
				new ObjectMapper());
		StateGraph workflow = new StateGraph(keyStrategyFactory, plainTextStateSerializer).addEdge(START, "agent_1")
				.addNode("agent_1", node_async(state -> {
					log.info("agent_1\n{}", state);
					return Map.of("prop1", "test", "user", new User("zhangsan", 16), "userList",
							List.of(new User("lisi", 18)), "userAry", new User[] { new User("wangwu", 20) });
				}))
				.addEdge("agent_1", END);

		CompiledGraph app = workflow.compile();

		Optional<OverAllState> result = app.invoke(Map.of(OverAllState.DEFAULT_INPUT_KEY, "test1"));
		System.out.println("result = " + result);
		assertTrue(result.isPresent());

		Map<String, Object> expected = Map.of("input", "test1", "prop1", "test", "user", new User("zhangsan", 16),
				"userList", List.of(new User("lisi", 18)), "userAry", new User[] { new User("wangwu", 20) });

		HashMap<String, Object> expectedMClone = new HashMap<>(expected);
		HashMap<String, Object> resultClone = new HashMap<>(filterInternalKeys(result.get().data()));
		Object expectedAry = expectedMClone.remove("userAry");
		Object resultAry = resultClone.remove("userAry");
		assertIterableEquals(sortMap(expectedMClone), sortMap(resultClone));
		assertArrayEquals((User[]) expectedAry, (User[]) resultAry);
	}

	/**
	 * Used to provide test data for the testWithSubSerialize method
	 *
	 * @param name
	 * @param age
	 */
	record User(String name, int age) {
	}

	/**
	 * Tests creation of a StateGraph using a custom OverAllStateFactory.
	 */
	@Test
	public void testCreateStateGraph() throws Exception {
		StateGraph workflow = new StateGraph(createKeyStrategyFactory()).addEdge(START, "agent_1")
				.addNode("agent_1", node_async(state -> {
					log.info("agent_1\n{}", state);
					return Map.of("prop1", "test");
				}))
				.addEdge("agent_1", END);

		CompiledGraph app = workflow.compile();

		Optional<OverAllState> result = app.invoke(Map.of(OverAllState.DEFAULT_INPUT_KEY, "test1"));
		System.out.println("result = " + result);
		assertTrue(result.isPresent());

		Map<String, String> expected = Map.of("input", "test1", "prop1", "test");
		assertIterableEquals(sortMap(expected), sortMap(filterInternalKeys(result.get().data())));
	}

	/**
	 * Test creating a state graph with custom key strategies using a lambda
	 * function.
	 * This test verifies that the graph correctly handles state updates using
	 * ReplaceStrategy for specific keys.
	 */
	@Test
	public void testKeyStrategyFactoryCreateStateGraph() throws Exception {
		StateGraph workflow = new StateGraph(() -> {
			HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
			keyStrategyHashMap.put("prop1", new ReplaceStrategy());
			keyStrategyHashMap.put("input", new ReplaceStrategy());
			return keyStrategyHashMap;
		}).addEdge(START, "agent_1").addNode("agent_1", node_async(state -> {
			log.info("agent_1\n{}", state);
			return Map.of("prop1", "test");
		})).addEdge("agent_1", END);

		CompiledGraph app = workflow.compile();

		Optional<OverAllState> result = app.invoke(Map.of(OverAllState.DEFAULT_INPUT_KEY, "test1"));
		System.out.println("result = " + result);
		assertTrue(result.isPresent());

		Map<String, String> expected = Map.of("input", "test1", "prop1", "test");
		assertIterableEquals(sortMap(expected), sortMap(filterInternalKeys(result.get().data())));
	}

	@Test
	public void testLifecycleListenerGraphWithLIFO() throws Exception {
		StateGraph workflow = new StateGraph(() -> {
			HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
			keyStrategyHashMap.put("prop1", new ReplaceStrategy());
			keyStrategyHashMap.put("input", new ReplaceStrategy());
			return keyStrategyHashMap;
		}).addEdge(START, "agent_1").addNode("agent_1", node_async(state -> {
			log.info("agent_1\n{}", state);
			return Map.of("prop1", "test");
		})).addEdge("agent_1", END);

		CompiledGraph app = workflow
				.compile(CompileConfig.builder().withLifecycleListener(new GraphLifecycleListener() {
					@Override
					public void onComplete(String nodeId, Map<String, Object> state, RunnableConfig config) {
						log.info("listener1 ,node = {},state = {}", nodeId, state);
					}

					@Override
					public void onStart(String nodeId, Map<String, Object> state, RunnableConfig config) {
						log.info("listener1 ,node = {},state = {}", nodeId, state);
					}
				}).withLifecycleListener(new GraphLifecycleListener() {
					@Override
					public void onStart(String nodeId, Map<String, Object> state, RunnableConfig config) {
						log.info("listener2 ,node = {},state = {}", nodeId, state);
					}

					@Override
					public void onComplete(String nodeId, Map<String, Object> state, RunnableConfig config) {
						log.info("listener2 ,node = {},state = {}", nodeId, state);
					}
				}).build());

		app.invoke(Map.of(OverAllState.DEFAULT_INPUT_KEY, "test1"));
	}

	/**
	 * Test graph execution lifecycle listeners for start and complete events. This
	 * test
	 * ensures that onStart and onComplete callbacks are properly triggered during
	 * graph
	 * execution.
	 */
	@Test
	public void testLifecycleListenerGraphWithCompleteAndStart() throws Exception {
		StateGraph workflow = new StateGraph(() -> {
			HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
			keyStrategyHashMap.put("prop1", new ReplaceStrategy());
			keyStrategyHashMap.put("input", new ReplaceStrategy());
			return keyStrategyHashMap;
		}).addEdge(START, "agent_1").addNode("agent_1", node_async(state -> {
			log.info("agent_1\n{}", state);
			return Map.of("prop1", "test");
		})).addEdge("agent_1", END);

		CompiledGraph app = workflow
				.compile(CompileConfig.builder().withLifecycleListener(new GraphLifecycleListener() {
					@Override
					public void onComplete(String nodeId, Map<String, Object> state, RunnableConfig config) {
						log.info("node = {},state = {}", nodeId, state);
					}

					@Override
					public void onStart(String nodeId, Map<String, Object> state, RunnableConfig config) {
						log.info("node = {},state = {}", nodeId, state);
					}
				}).build());

		app.invoke(Map.of(OverAllState.DEFAULT_INPUT_KEY, "test1"));
	}

	/**
	 * Test graph execution error handling through lifecycle listener. This test
	 * ensures
	 * that onError callback is properly triggered when an exception occurs during
	 * node
	 * execution.
	 */
	@Test
	public void testLifecycleListenerGraphWithError() throws Exception {
		StateGraph workflow = new StateGraph(() -> {
			HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
			keyStrategyHashMap.put("prop1", new ReplaceStrategy());
			keyStrategyHashMap.put("input", new ReplaceStrategy());
			return keyStrategyHashMap;
		}).addEdge(START, "agent_1").addNode("agent_1", node_async(state -> {
			log.info("agent_1\n{}", state);
			int a = 1 / 0; // Force division by zero error
			return Map.of("prop1", "test");
		})).addEdge("agent_1", END);

		CompiledGraph app = workflow
				.compile(CompileConfig.builder().withLifecycleListener(new GraphLifecycleListener() {
					@Override
					public void onComplete(String nodeId, Map<String, Object> state, RunnableConfig config) {
						log.info("node = {},state = {}", nodeId, state);
					}

					@Override
					public void onStart(String nodeId, Map<String, Object> state, RunnableConfig config) {
						log.info("node = {},state = {}", nodeId, state);
					}

					@Override
					public void onError(String nodeId, Map<String, Object> state, Throwable ex, RunnableConfig config) {
						log.error("node = {},state = {}", nodeId, state, ex);
					}
				}).build());

		assertThrows(ArithmeticException.class,
				(NamedExecutable) () -> app.invoke(Map.of(OverAllState.DEFAULT_INPUT_KEY, "test1")));
	}

	/**
	 * Tests that lifecycle listeners receive correct nodeId for parallel node
	 * children.
	 */
	@Test
	void testParallelNodeLifecycleListenerNodeId() throws Exception {
		List<String> beforeNodeIds = new ArrayList<>();
		List<String> afterNodeIds = new ArrayList<>();

		var workflow = new StateGraph(createKeyStrategyFactory()).addNode("A", makeNode("A"))
				.addNode("A1", makeNode("A1"))
				.addNode("A2", makeNode("A2"))
				.addNode("A3", makeNode("A3"))
				.addNode("B", makeNode("B"))
				.addEdge("A", "A1")
				.addEdge("A", "A2")
				.addEdge("A", "A3")
				.addEdge("A1", "B")
				.addEdge("A2", "B")
				.addEdge("A3", "B")
				.addEdge(START, "A")
				.addEdge("B", END);

		var app = workflow.compile(CompileConfig.builder().withLifecycleListener(new GraphLifecycleListener() {
			@Override
			public void before(String nodeId, Map<String, Object> state, RunnableConfig config, Long curTime) {
				synchronized (beforeNodeIds) {
					beforeNodeIds.add(nodeId);
					log.info("Lifecycle before: nodeId = {}", nodeId);
				}
			}

			@Override
			public void after(String nodeId, Map<String, Object> state, RunnableConfig config, Long curTime) {
				synchronized (afterNodeIds) {
					afterNodeIds.add(nodeId);
					log.info("Lifecycle after: nodeId = {}", nodeId);
				}
			}
		}).build());

		app.stream(Map.of(), RunnableConfig.builder().build())
				.blockLast();

		log.info("Before nodeIds: {}", beforeNodeIds);
		log.info("After nodeIds: {}", afterNodeIds);

		assertTrue(beforeNodeIds.contains("A"));
		assertTrue(afterNodeIds.contains("A"));

		assertTrue(beforeNodeIds.contains("__PARALLEL__(A)"));
		assertTrue(afterNodeIds.contains("__PARALLEL__(A)"));

		assertTrue(beforeNodeIds.contains("A1"));
		assertTrue(afterNodeIds.contains("A1"));

		assertTrue(beforeNodeIds.contains("A2"));
		assertTrue(afterNodeIds.contains("A2"));

		assertTrue(beforeNodeIds.contains("A3"));
		assertTrue(afterNodeIds.contains("A3"));

		assertTrue(beforeNodeIds.contains("B"));
		assertTrue(afterNodeIds.contains("B"));

		long parallelIdCount = beforeNodeIds.stream().filter(id -> id.equals("__PARALLEL__(A)")).count();
		assertEquals(1, parallelIdCount);
	}

	/**
	 * Tests ParallelNode thread pool optimization and logging functionality.
	 * Verifies that the default thread pool is properly configured and logs metrics
	 * correctly.
	 */
	@Test
	void testParallelNodeThreadPoolOptimization() throws Exception {
		// Create a simple parallel workflow
		var workflow = new StateGraph(createKeyStrategyFactory())
				.addNode("parallelParent", makeNode("parallelParent"))
				.addNode("child1", makeNode("child1"))
				.addNode("child2", makeNode("child2"))
				.addNode("child3", makeNode("child3"))
				.addNode("merge", makeNode("merge"))
				.addEdge(START, "parallelParent")
				.addEdge("parallelParent", "child1")
				.addEdge("parallelParent", "child2")
				.addEdge("parallelParent", "child3")
				.addEdge("child1", "merge")
				.addEdge("child2", "merge")
				.addEdge("child3", "merge")
				.addEdge("merge", END);

		// Compile the workflow
		var app = workflow.compile();

		// Capture log output to verify thread pool logging
		final List<String> logMessages = new ArrayList<>();

		// Create a test listener to capture lifecycle events
		var testListener = new GraphLifecycleListener() {
			@Override
			public void before(String nodeId, Map<String, Object> state, RunnableConfig config, Long curTime) {
				if (nodeId.contains("PARALLEL")) {
					logMessages.add("Parallel node started: " + nodeId);
				}
			}

			@Override
			public void after(String nodeId, Map<String, Object> state, RunnableConfig config, Long curTime) {
				if (nodeId.contains("PARALLEL")) {
					logMessages.add("Parallel node completed: " + nodeId);
				}
			}
		};

		// Add the listener to compile config
		var config = CompileConfig.builder().withLifecycleListener(testListener).build();

		// Override the default workflow compilation to use our config
		var appWithConfig = workflow.compile(config);

		// Execute the workflow
		appWithConfig.stream(Map.of()).blockLast();

		// Verify that all nodes were executed
		assertTrue(logMessages.size() >= 2, "Should have at least parallel start and complete messages");

		// Verify that we have the expected parallel node markers
		boolean hasParallelStart = logMessages.stream().anyMatch(msg -> msg.contains("Parallel node started"));
		boolean hasParallelComplete = logMessages.stream().anyMatch(msg -> msg.contains("Parallel node completed"));

		assertTrue(hasParallelStart, "Should have parallel node start message");
		assertTrue(hasParallelComplete, "Should have parallel node complete message");
	}

	@Test
	public void testCommandEdgeGraph() throws Exception {
		StateGraph workflow = new StateGraph(
				() -> Map.of("prop1", new ReplaceStrategy(), "input", new ReplaceStrategy()))

				.addNode("agent_1", node_async(state -> {
					log.info("agent_1\n{}", state);

					return Map.of("prop1", "agent_1");
				}))
				.addNode("agent_2", node_async(state -> {
					log.info("agent_2\n{}", state);

					return Map.of("prop1", "agent_2");
				}))
				.addNode("agent_3", node_async(state -> {
					log.info("agent_3\n{}", state);
					assertEquals("command content", state.value("prop1", String.class).get());
					return Map.of("prop1", "agent_3");
				}))
				.addConditionalEdges("agent_2",
						AsyncCommandAction
								.node_async(
										(state, config) -> new Command("agent_2", Map.of("prop1", "command content"))),
						Map.of("agent_2", "agent_3"))
				.addEdge(START, "agent_1")
				.addEdge("agent_3", END)
				.addEdge("agent_1", "agent_2");
		CompiledGraph compile = workflow.compile();
		compile.invoke(Map.of(OverAllState.DEFAULT_INPUT_KEY, "test1"));
	}

	@Test
	public void testCommandNodeGraph() throws Exception {
		StateGraph graph = new StateGraph(() -> {
			HashMap<String, KeyStrategy> stringKeyStrategyHashMap = new HashMap<>();
			stringKeyStrategyHashMap.put("messages", new AppendStrategy());
			return stringKeyStrategyHashMap;
		});

		CommandAction commandAction = (state, config) -> new Command("node1", Map.of("messages", "go to command node"));
		graph.addNode("testCommandNode", AsyncCommandAction.node_async(commandAction),
				Map.of("node1", "node1", "node2", "node2"));

		graph.addNode("node1", makeNode("node1"));
		graph.addNode("node2", makeNode("node2"));

		graph.addEdge(START, "testCommandNode");
		graph.addEdge("node1", "node2");
		graph.addEdge("node2", END);

		CompiledGraph compile = graph.compile();
		String plantuml = compile.getGraph(GraphRepresentation.Type.PLANTUML).content();
		String mermaid = compile.getGraph(GraphRepresentation.Type.MERMAID).content();
		System.out.println("===============plantuml===============");
		System.out.println(plantuml);
		System.out.println("===============mermaid===============");
		System.out.println(mermaid);

		OverAllState state = compile.invoke(Map.of()).orElseThrow();
		assertEquals(List.of("go to command node", "node1", "node2"), state.value("messages", List.class).get());
	}

	@Test
	public void testParallelInterrupt() throws GraphStateException {
		KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactoryBuilder().addStrategy("prop1", (o, o2) -> o2)
				.build();

		StateGraph workflow = new StateGraph(keyStrategyFactory).addEdge(START, "agent_1")
				.addEdge(START, "agent_2")
				.addEdge("agent_2", "agent_3")
				.addEdge("agent_1", "agent_3")
				.addNode("agent_1", AsyncNodeActionWithConfig.node_async((state, config) -> {
					log.info("agent_1\n{}", state);
					return Map.of("prop1", "test");
				}))
				.addNode("agent_2", AsyncNodeActionWithConfig.node_async((state, config) -> {
					log.info("agent_2\n{}", state);
					return Map.of("prop1", "test_2");
				}))
				.addNode("agent_3", AsyncNodeActionWithConfig.node_async((state, config) -> {
					log.info("agent_3\n{}", state);
					return Map.of("prop1", "test_3");
				}))
				.addEdge("agent_3", END);

		CompiledGraph app = workflow
				.compile(CompileConfig.builder().interruptBefore("agent_2").interruptBeforeEdge(true).build());
		RunnableConfig runnableConfig = new RunnableConfig.Builder().threadId("thread1").build();
		Optional<OverAllState> result = app.invoke(Map.of(OverAllState.DEFAULT_INPUT_KEY, "test1"), runnableConfig);
		System.out.println("result = " + result);
		assertTrue(result.isPresent());

		// resume
		result = app.stream(null, runnableConfig).reduce((a, b) -> b).map(NodeOutput::state).blockOptional();
		System.out.println("result = " + result);
		assertTrue(result.isPresent());

	}

	@Test
	public void testStreamingNodeWithFluxException() throws Exception {
		StateGraph workflow = new StateGraph(createKeyStrategyFactory()).addEdge(START, "agent_1")
				.addNode("agent_1", node_async(state -> {
					log.info("agent_1\n{}", state);
					return Map.of("pro1", Flux.just("response1", "response2", "response3")
							.map(value -> {
								if (value.equals("response3")) {
									throw new RuntimeException("Exception in map operation");
								}
								return value;
							}));
				}))
				.addEdge("agent_1", END);

		CompiledGraph app = workflow.compile();

		assertThrows(RuntimeException.class,
				() -> app.invoke(Map.of(OverAllState.DEFAULT_INPUT_KEY, "test1")));

		Flux<NodeOutput> flux = app.stream(Map.of(OverAllState.DEFAULT_INPUT_KEY, "test1"));

		// 验证前两个元素正常输出
		Flux<NodeOutput> fluxForFirstTwo = app.stream(Map.of(OverAllState.DEFAULT_INPUT_KEY, "test1"));
		List<NodeOutput> firstTwoElements = fluxForFirstTwo.take(2).collectList().block();
		assertNotNull(firstTwoElements);
		assertEquals(2, firstTwoElements.size());

		// 验证第三个元素会抛出异常
		assertThrows(RuntimeException.class, () -> flux.blockLast());
	}

	@Test
	public void testStreamingNodeWithNodeException() throws Exception {
		StateGraph workflow = new StateGraph(createKeyStrategyFactory()).addEdge(START, "agent_1")
				.addNode("agent_1", node_async(state -> {
					throw new RuntimeException("forced exception for testing");
				}))
				.addEdge("agent_1", END);

		CompiledGraph app = workflow.compile();

		// 验证 invoke 会抛出异常
		assertThrows(RuntimeException.class,
				() -> app.invoke(Map.of(OverAllState.DEFAULT_INPUT_KEY, "test1")));

		// 验证 stream 也会抛出异常
		Flux<NodeOutput> flux = app.stream(Map.of(OverAllState.DEFAULT_INPUT_KEY, "test1"));
		assertThrows(RuntimeException.class, () -> flux.blockLast());
	}

	/**
	 * Tests the addConditionalEdges method with AsyncEdgeActionWithConfig and a mapping of conditional routes.
	 * This test verifies that conditional edges work properly with configuration-based routing.
	 */
	@Test
	public void testAddConditionalEdgesWithConfig() throws Exception {
		// Create a state graph with key strategies
		StateGraph workflow = new StateGraph(() -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("messages", new AppendStrategy());
			keyStrategyMap.put("prop1", new ReplaceStrategy());
			return keyStrategyMap;
		});

		// Add nodes to the workflow
		workflow.addNode("start", node_async(state -> {
			log.info("start node");
			return Map.of("messages", "start");
		}))
		.addNode("conditional_node", node_async(state -> {
			log.info("conditional_node");
			return Map.of("messages", "processing");
		}))
		.addNode("route1", node_async(state -> {
			log.info("route1");
			return Map.of("messages", "route1_result", "prop1", "value1");
		}))
		.addNode("route2", node_async(state -> {
			log.info("route2");
			return Map.of("messages", "route2_result", "prop1", "value2");
		}))
		.addNode("end", node_async(state -> {
			log.info("end");
			return Map.of("messages", "end");
		}));

		// Add conditional edges using the AsyncEdgeActionWithConfig with explicit cast to resolve method ambiguity
		workflow.addConditionalEdges("conditional_node",AsyncEdgeActionWithConfig.edge_async((state, config) -> {
			// Check if there's a specific value in the state to determine the route
			String prop1Value = state.value("prop1",String.class).get();
			if (prop1Value.equals("value1")) {
				return "route1";
			} else {
				return "route2";
			}
		})  , Map.of(
			"route1", "route1",
			"route2", "route2"
		));

		// Add regular edges to connect the workflow
		workflow.addEdge(START, "start")
			.addEdge("start", "conditional_node")
			.addEdge("route1", "end")
			.addEdge("route2", "end")
			.addEdge("end", END);

		// Compile the workflow
		CompiledGraph app = workflow.compile();

		// Test the workflow with initial state that should route to route2
		Optional<OverAllState> result = app.invoke(Map.of("prop1", "initial_value"),RunnableConfig.builder().threadId("test-thread-1").build());
		assertTrue(result.isPresent());
		log.info("Result: {}", result.get().data());

		// Verify the result went through the correct route
		List<String> messages = (List<String>) result.get().value("messages").get();
		assertIterableEquals(List.of("start", "processing", "route2_result", "end"), messages);

		// Test with state that should route to route1
		Optional<OverAllState> result2 = app.invoke(Map.of("prop1", "value1"), RunnableConfig.builder().threadId("test-thread-2").build());
		assertTrue(result2.isPresent());
		log.info("Result2: {}", result2.get().data());

		List<String> messages2 = (List<String>) result2.get().value("messages").get();
		assertIterableEquals(List.of("start", "processing", "route1_result", "end"), messages2);
	}

	/**
	 * Tests the addConditionalEdges method with configuration-based routing.
	 * This test verifies that the conditional edge can access and use the RunnableConfig.
	 */
	@Test
	public void testAddConditionalEdgesWithConfigAccess() throws Exception {
		StateGraph workflow = new StateGraph(() -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("messages", new AppendStrategy());
			return keyStrategyMap;
		});

		// Add nodes
		workflow.addNode("start", node_async(state -> {
			log.info("start node");
			return Map.of("messages", "start");
		}))
		.addNode("conditional_node", node_async(state -> {
			log.info("conditional_node");
			return Map.of("messages", "processing");
		}))
		.addNode("config_route", node_async(state -> {
			log.info("config_route");
			return Map.of("messages", "config_route_result");
		}))
		.addNode("default_route", node_async(state -> {
			log.info("default_route");
			return Map.of("messages", "default_route_result");
		}));

		workflow.addConditionalEdges("conditional_node", AsyncEdgeActionWithConfig.edge_async( (state, config) -> {
			// Check if a specific value is in the config metadata
			Optional<Object> configValue = config.metadata("test_route");
			if (configValue.isPresent() && "config_route".equals(configValue.get())) {
				return "config_route";
			} else {
				return "default_route";
			}
		}), Map.of(
			"config_route", "config_route",
			"default_route", "default_route"
		));

		workflow.addEdge(START, "start")
			.addEdge("start", "conditional_node")
			.addEdge("config_route", END)
			.addEdge("default_route", END);

		CompiledGraph app = workflow.compile();

		// Test with config that should route to config_route
		RunnableConfig config = RunnableConfig.builder().threadId("config-thread").addMetadata("test_route", "config_route").build();
		Optional<OverAllState> result = app.invoke(Map.of(), config);
		assertTrue(result.isPresent());
		log.info("Config result: {}", result.get().data());

		List<String> messages = (List<String>) result.get().value("messages").get();
		assertIterableEquals(List.of("start", "processing", "config_route_result"), messages);

		// Test with default config that should route to default_route
		RunnableConfig defaultConfig = RunnableConfig.builder().threadId("default-thread").build();
		Optional<OverAllState> result2 = app.invoke(Map.of(), defaultConfig);
		assertTrue(result2.isPresent());
		log.info("Default config result: {}", result2.get().data());

		List<String> messages2 = (List<String>) result2.get().value("messages").get();
		assertIterableEquals(List.of("start", "processing", "default_route_result"), messages2);
	}

	/**
	 * Tests error conditions for the addConditionalEdges method.
	 */
	@Test
	public void testAddConditionalEdgesWithConfigErrors() throws GraphStateException {
		StateGraph workflow = new StateGraph();

		// Add a node to work with
		workflow.addNode("node1", node_async(state -> Map.of()));

		// Test that adding conditional edges to END node throws an exception
		GraphStateException exception = assertThrows(GraphStateException.class, () -> {
			workflow.addConditionalEdges(END, AsyncEdgeActionWithConfig.edge_async((state, config) -> "next"), Map.of("next", "node1"));
		});

		assertEquals("END is not a valid edge sourceId!", exception.getMessage());

		// Test that adding conditional edges with empty mappings throws an exception
		GraphStateException exception2 = assertThrows(GraphStateException.class, () -> {
			workflow.addConditionalEdges("node1", AsyncEdgeActionWithConfig.edge_async((state, config) -> "next"), Map.of());
		});

		assertEquals("edge mapping is empty!", exception2.getMessage());

		// Test that adding conditional edges with null mappings throws an exception
		GraphStateException exception3 = assertThrows(GraphStateException.class, () -> {
			workflow.addConditionalEdges("node1",AsyncEdgeActionWithConfig.edge_async((state, config) -> "next"), null);
		});

		assertEquals("edge mapping is empty!", exception3.getMessage());
	}

	@Test
	void testCommandNode_Issue3917() throws Exception {
		AsyncCommandAction commandAction = (state, config) ->
				completedFuture( new Command("D",
						Map.of( "messages", "B",
								"next_node", "C2")) );

		var graph = new StateGraph(createKeyStrategyFactory())
				.addNode("A", makeNode("A"))
				.addNode("B", commandAction, EdgeMappings.builder()
						.toEND()
						.to("C")
						.to("D")
						.build())
				.addNode("C1", makeNode("C1"))
				.addNode("C2", makeNode("C2"))
				.addNode("C", makeNode("C"))
				.addNode("D", makeNode("D"))
				.addEdge(START, "A")
				.addEdge("A", "B")
				.addEdge("D","C1")
				.addEdge("D","C2")
				.addEdge("C", END)
				.addEdge( "C1", END )
				.addEdge( "C2", END )
				.compile();

		var steps = Objects.requireNonNull(graph.stream(Map.of())
				.doOnNext(nodeOutput -> log.info("node: " + nodeOutput.node()))
				.collectList()
				.block());


		assertEquals(6, steps.size());
		assertEquals( "B", steps.get(2).state().value("messages",List.class).get().get(1));
		assertEquals( "C2", steps.get(2).state().value("next_node").orElse(null));

	}

}

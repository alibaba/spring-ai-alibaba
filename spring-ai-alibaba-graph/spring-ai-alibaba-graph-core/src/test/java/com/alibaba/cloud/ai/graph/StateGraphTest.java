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

import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.serializer.plain_text.PlainTextStateSerializer;
import com.alibaba.cloud.ai.graph.state.AppenderChannel;
import com.alibaba.cloud.ai.graph.state.RemoveByHash;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;

import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import org.bsc.async.AsyncGenerator;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StateGraphTest {

	private static final Logger log = LoggerFactory.getLogger(StateGraphTest.class);

	public static <T> List<Map.Entry<String, T>> sortMap(Map<String, T> map) {
		return map.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList());
	}

	@Test
	void testValidation() throws Exception {

		StateGraph workflow = new StateGraph(new OverAllState());
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

		// exception = assertThrows(GraphStateException.class, () ->
		// workflow.addEdge("agent_1", "agent_2"));
		// System.out.println(exception.getMessage());

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

	@Test
	public void testRunningOneNode() throws Exception {
		OverAllState overAllState = new OverAllState().registerKeyAndStrategy("prop1", (o, o2) -> o2);
		StateGraph workflow = new StateGraph(overAllState).addEdge(START, "agent_1")
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
		assertIterableEquals(sortMap(expected), sortMap(result.get().data()));
		// assertDictionaryOfAnyEqual( expected, result.data )

	}

	@Test
	void testWithAppender() throws Exception {
		OverAllState overAllState = getOverAllState();
		StateGraph workflow = new StateGraph(overAllState).addNode("agent_1", node_async(state -> {
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

	private static void removeFromList(List<Object> result, AppenderChannel.RemoveIdentifier<Object> removeIdentifier) {
		for (int i = 0; i < result.size(); i++) {
			if (removeIdentifier.compareTo(result.get(i), i) == 0) {
				result.remove(i);
				break;
			}
		}
	}

	private static AppenderChannel.RemoveData<Object> evaluateRemoval(List<Object> oldValues, List<?> newValues) {

		final var result = new AppenderChannel.RemoveData<>(oldValues, newValues);

		newValues.stream().filter(value -> value instanceof AppenderChannel.RemoveIdentifier<?>).forEach(value -> {
			result.newValues().remove(value);
			var removeIdentifier = (AppenderChannel.RemoveIdentifier<Object>) value;
			removeFromList(result.oldValues(), removeIdentifier);

		});
		return result;

	}

	@Test
	void testWithAppenderOneRemove() throws Exception {
		OverAllState overAllState = getOverAllState();
		StateGraph workflow = new StateGraph(overAllState).addNode("agent_1", node_async(state -> {
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

	@Test
	void testWithAppenderOneAppendOneRemove() throws Exception {
		OverAllState overAllState = getOverAllState();
		StateGraph workflow = new StateGraph(overAllState)
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

	private static OverAllState getOverAllState() {
		return new OverAllState().registerKeyAndStrategy("steps", (o, o2) -> o2)
			.registerKeyAndStrategy("messages", new AppendStrategy());
	}

	@Test
	public void testWithSubgraph() throws Exception {
		// todo: invoke 传入 inputs 内容
		OverAllState overAllState = getOverAllState();
		var childStep1 = node_async((OverAllState state) -> Map.of("messages", "child:step1"));

		var childStep2 = node_async((OverAllState state) -> Map.of("messages", "child:step2"));

		var childStep3 = node_async((OverAllState state) -> Map.of("messages", "child:step3"));

		var workflowChild = new StateGraph().addNode("child:step_1", childStep1)
			.addNode("child:step_2", childStep2)
			.addNode("child:step_3", childStep3)
			.addEdge(START, "child:step_1")
			.addEdge("child:step_1", "child:step_2")
			.addEdge("child:step_2", "child:step_3")
			.addEdge("child:step_3", END)
		// .compile()
		;
		var step1 = node_async((OverAllState state) -> Map.of("messages", "step1"));

		var step2 = node_async((OverAllState state) -> Map.of("messages", "step2"));

		var step3 = node_async((OverAllState state) -> Map.of("messages", "step3"));

		var workflowParent = new StateGraph(overAllState).addNode("step_1", step1)
			.addNode("step_2", step2)
			.addNode("step_3", step3)
			.addNode("subgraph", workflowChild)
			.addEdge(START, "step_1")
			.addEdge("step_1", "step_2")
			.addEdge("step_2", "subgraph")
			.addEdge("subgraph", "step_3")
			.addEdge("step_3", END)
			.compile();
		// todo：
		var result = workflowParent.stream(Map.of())
			.stream()
			.peek(nodeOutput -> System.out.println(
					"node = " + nodeOutput.node() + "     message = " + nodeOutput.state().value("messages").get()))
			.reduce((a, b) -> b)
			.map(NodeOutput::state);

		assertTrue(result.isPresent());
		assertIterableEquals(List.of("step1", "step2", "child:step1", "child:step2", "child:step3", "step3"),
				(List<Object>) result.get().value("messages").get());

	}

	private AsyncNodeAction makeNode(String id) {
		return node_async(state -> {
			log.info("call node {}", id);
			return Map.of("messages", id);
		});
	}

	@Test
	void testWithParallelBranch() throws Exception {
		OverAllState overAllState = getOverAllState();
		var workflow = new StateGraph(overAllState).addNode("A", makeNode("A"))
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

		var app = workflow.compile();

		var result = app.stream()
			.stream()
			.peek(nodeOutput -> System.out.println(
					"node = " + nodeOutput.node() + "     message = " + nodeOutput.state().value("messages").get()))
			.reduce((a, b) -> b)
			.map(NodeOutput::state);
		assertTrue(result.isPresent());
		assertIterableEquals(List.of("A", "A1", "A2", "A3", "B", "C"),
				(List<String>) result.get().value("messages").get());

		workflow = new StateGraph(getOverAllState()).addNode("A", makeNode("A"))
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

		result = app.stream()
			.stream()
			.peek(nodeOutput -> System.out.println(
					"node = " + nodeOutput.node() + "     message = " + nodeOutput.state().value("messages").get()))
			.reduce((a, b) -> b)
			.map(NodeOutput::state);

		assertTrue(result.isPresent());
		assertIterableEquals(List.of("A1", "A2", "A3", "B", "C"), (List<String>) result.get().value("messages").get());

	}

	@Test
	void testWithParallelBranchWithErrors() throws Exception {

		// ONLY ONE TARGET
		var onlyOneTarget = new StateGraph(getOverAllState()).addNode("A", makeNode("A"))
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

		var noConditionalEdge = new StateGraph(getOverAllState()).addNode("A", makeNode("A"))
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

		var noConditionalEdgeOnBranch = new StateGraph(getOverAllState()).addNode("A", makeNode("A"))
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

		var noDuplicateTarget = new StateGraph(getOverAllState()).addNode("A", makeNode("A"))
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

	@Test
	public void testWithSubSerialize() throws Exception {
		OverAllState overAllState = new OverAllState().registerKeyAndStrategy("prop1", (o, o2) -> o2);
		String input = "jackson1";
		PlainTextStateSerializer plainTextStateSerializer;
		if (input.equals("jackson")) {
			plainTextStateSerializer = new StateGraph.JacksonSerializer();
		}
		else {
			plainTextStateSerializer = new StateGraph.GsonSerializer();
		}
		StateGraph workflow = new StateGraph(overAllState, plainTextStateSerializer).addEdge(START, "agent_1")
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
		assertIterableEquals(sortMap(expected), sortMap(result.get().data()));
	}

	public OverAllStateFactory overAllStateFactory() {
		return () -> new OverAllState().registerKeyAndStrategy("prop1", (o, o2) -> o2);
	}

	@Test
	public void testCreateStateGraph() throws Exception {
		StateGraph workflow = new StateGraph(overAllStateFactory()).addEdge(START, "agent_1")
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
		assertIterableEquals(sortMap(expected), sortMap(result.get().data()));
	}

	@Test
	void testGetResultFromGenerator() throws Exception {
		var workflow = new StateGraph(() -> new OverAllState().registerKeyAndStrategy("messages", new AppendStrategy()))
			.addEdge(START, "agent_1")
			.addNode("agent_1", makeNode("agent_1"))
			.addEdge("agent_1", END);

		var app = workflow.compile();

		var iterator = app.stream(Map.of());
		for (var i : iterator) {
			System.out.println(i);
		}

		var generator = (AsyncGenerator.HasResultValue) iterator;

		System.out.println(generator.resultValue().orElse(null));

	}

	@Test
	public void testNodeActionStream() throws Exception {
		StateGraph stateGraph = new StateGraph(
				() -> new OverAllState().registerKeyAndStrategy("messages", new AppendStrategy())
					.registerKeyAndStrategy("count", (oldValue, newValue) -> oldValue == null ? newValue : 1))
			.addNode("collectInput", node_async(s -> {
				// 处理输入
				String input = s.value("input", "");
				return Map.of("messages", "Received: " + input, "count", 1);
			}))
			.addNode("processData", node_async(s -> {
				// 处理数据 - 这里可以是耗时操作，会以流式方式返回结果
				final List<String> data = asList("这是", "一个", "流式", "输出", "测试");
				AtomicInteger timeOff = new AtomicInteger(1);
				final AsyncGenerator<NodeOutput> it = AsyncGenerator.collect(data.iterator(),
						(index, add) -> add.accept(of("processData", index, 500L * timeOff.getAndIncrement(), s)));
				return Map.of("messages", it);
			}))
			.addNode("generateResponse", node_async(s -> {
				// 生成最终响应
				int count = s.value("count", 0);
				return Map.of("messages", "Response generated (processed " + count + " items)", "result", "Success");
			}))
			.addEdge(START, "collectInput")
			.addEdge("collectInput", "processData")
			.addEdge("processData", "generateResponse")
			.addEdge("generateResponse", END);

		CompiledGraph compiledGraph = stateGraph.compile();
		// 初始化输入
		for (var output : compiledGraph.stream(Map.of("input", "hoho~~"))) {
			if (output instanceof AsyncGenerator<?>) {
				AsyncGenerator asyncGenerator = (AsyncGenerator) output;
				System.out.println("Streaming chunk: " + asyncGenerator);
			}
			else {
				System.out.println("Node output: " + output);
			}
		}
	}

	static CompletableFuture<NodeOutput> of(String node, String index, long delayInMills, OverAllState overAllState) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				Thread.sleep(delayInMills);
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			return new StreamingOutput(index, node, overAllState);
		});
		// return completedFuture("e" + index);
	}

}

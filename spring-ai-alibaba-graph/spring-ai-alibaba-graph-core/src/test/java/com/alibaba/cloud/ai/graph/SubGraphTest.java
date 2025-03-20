/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.graph;

import java.io.IOException;
import java.util.*;
import java.util.logging.LogManager;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant;
import com.alibaba.cloud.ai.graph.state.AppenderChannel;
import lombok.extern.slf4j.Slf4j;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static java.util.Collections.unmodifiableList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class SubGraphTest {

	@BeforeAll
	public static void initLogging() throws IOException {
		try (var is = SubGraphTest.class.getResourceAsStream("/logging.properties")) {
			LogManager.getLogManager().readConfiguration(is);
		}
	}

	private AsyncNodeAction _makeNode(String id) {
		return node_async(state -> Map.of("messages", id));
	}

	private List<String> _execute(CompiledGraph workflow, Map<String, Object> input) throws Exception {
		return workflow.stream().stream().peek(System.out::println).map(NodeOutput::node).toList();
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

	private static OverAllState getOverAllState() {
		return new OverAllState().input(Map.of())
			.registerKeyAndStrategy("a", (o, o2) -> o2)
			.registerKeyAndStrategy("b", (o, o2) -> o2)
			.registerKeyAndStrategy("c", (o, o2) -> o2)
			.registerKeyAndStrategy("steps", (o, o2) -> o2)
			.registerKeyAndStrategy("messages", (oldValue, newValue) -> {
				if (newValue == null) {
					return oldValue;
				}

				boolean oldValueIsList = oldValue instanceof List<?>;

				if (oldValueIsList && newValue instanceof AppenderChannel.RemoveIdentifier<?>) {
					var result = new ArrayList<>((List<Object>) oldValue);
					removeFromList(result, (AppenderChannel.RemoveIdentifier) newValue);
					return unmodifiableList(result);
				}

				List<Object> list = null;
				if (newValue instanceof List) {
					list = new ArrayList<>((List<?>) newValue);
				}
				else if (newValue.getClass().isArray()) {
					list = new ArrayList<>(Arrays.asList((Object[]) newValue));
				}
				else if (newValue instanceof Collection) {
					list = new ArrayList<>((Collection<?>) newValue);
				}

				if (oldValueIsList) {
					List<Object> oldList = (List<Object>) oldValue;
					if (list != null) {
						if (list.isEmpty()) {
							return oldValue;
						}
						if (oldValueIsList) {
							var result = evaluateRemoval((List<Object>) oldValue, list);
							List<Object> mergedList = Stream
								.concat(result.oldValues().stream(), result.newValues().stream())
								.distinct()
								.collect(Collectors.toList());
							return mergedList;
						}
						oldList.addAll(list);
					}
					else {
						oldList.add(newValue);
					}
					return oldList;
				}
				else {
					ArrayList<Object> arrayResult = new ArrayList<>();
					arrayResult.add(newValue);
					return arrayResult;
				}
			});
	}

	@Test
	public void testMergeSubgraph01() throws Exception {

		var workflowChild = new StateGraph().addNode("B1", _makeNode("B1"))
			.addNode("B2", _makeNode("B2"))
			.addEdge(START, "B1")
			.addEdge("B1", "B2")
			.addEdge("B2", END);

		var workflowParent = new StateGraph(getOverAllState()).addNode("A", _makeNode("A"))
			.addSubgraph("B", workflowChild)
			.addNode("C", _makeNode("C"))
			.addEdge(START, "A")
			.addEdge("A", "B")
			.addEdge("B", "C")
			.addEdge("C", END);

		var B_B1 = SubGraphNode.formatId("B", "B1");
		var B_B2 = SubGraphNode.formatId("B", "B2");

		var app = workflowParent.compile();

		assertIterableEquals(List.of(START, "A", B_B1, B_B2, "C", END), _execute(app, Map.of()));

	}

	@Test
	public void testMergeSubgraph02() throws Exception {

		var workflowChild = new StateGraph().addNode("B1", _makeNode("B1"))
			.addNode("B2", _makeNode("B2"))
			.addEdge(START, "B1")
			.addEdge("B1", "B2")
			.addEdge("B2", END);

		var workflowParent = new StateGraph(getOverAllState()).addNode("A", _makeNode("A"))
			.addSubgraph("B", workflowChild)
			.addNode("C", _makeNode("C"))
			.addConditionalEdges(START, edge_async(state -> "a"), Map.of("a", "A", "b", "B"))
			.addEdge("A", "B")
			.addEdge("B", "C")
			.addEdge("C", END)
		// .compile(compileConfig)
		;

		var processed = ProcessedNodesEdgesAndConfig.process(workflowParent, CompileConfig.builder().build());
		processed.nodes().elements.forEach(System.out::println);
		processed.edges().elements.forEach(System.out::println);

		assertEquals(4, processed.nodes().elements.size());
		assertEquals(5, processed.edges().elements.size());

		var B_B1 = SubGraphNode.formatId("B", "B1");
		var B_B2 = SubGraphNode.formatId("B", "B2");

		var app = workflowParent.compile();

		assertIterableEquals(List.of(START, "A", B_B1, B_B2, "C", END), _execute(app, Map.of()));

	}

	@Test
	public void testMergeSubgraph03() throws Exception {

		var workflowChild = new StateGraph().addNode("B1", _makeNode("B1"))
			.addNode("B2", _makeNode("B2"))
			.addNode("C", _makeNode("subgraph(C)"))
			.addEdge(START, "B1")
			.addEdge("B1", "B2")
			.addConditionalEdges("B2", edge_async(state -> "c"), Map.of(END, END, "c", "C"))
			.addEdge("C", END);

		var workflowParent = new StateGraph(getOverAllState()).addNode("A", _makeNode("A"))
			.addSubgraph("B", workflowChild)
			.addNode("C", _makeNode("C"))
			.addConditionalEdges(START, edge_async(state -> "a"), Map.of("a", "A", "b", "B"))
			.addEdge("A", "B")
			.addEdge("B", "C")
			.addEdge("C", END)
		// .compile(compileConfig)
		;

		var processed = ProcessedNodesEdgesAndConfig.process(workflowParent, CompileConfig.builder().build());
		processed.nodes().elements.forEach(System.out::println);
		processed.edges().elements.forEach(System.out::println);

		assertEquals(5, processed.nodes().elements.size());
		assertEquals(6, processed.edges().elements.size());

		var B_B1 = SubGraphNode.formatId("B", "B1");
		var B_B2 = SubGraphNode.formatId("B", "B2");
		var B_C = SubGraphNode.formatId("B", "C");

		var app = workflowParent.compile();

		assertIterableEquals(List.of(START, "A", B_B1, B_B2, B_C, "C", END), _execute(app, Map.of()));

	}

	@Test
	public void testMergeSubgraph03WithInterruption() throws Exception {
		OverAllState overAllState = getOverAllState();
		var workflowChild = new StateGraph().addNode("B1", _makeNode("B1"))
			.addNode("B2", _makeNode("B2"))
			.addNode("C", _makeNode("subgraph(C)"))
			.addEdge(START, "B1")
			.addEdge("B1", "B2")
			.addConditionalEdges("B2", edge_async(state -> "c"), Map.of(END, END, "c", "C"))
			.addEdge("C", END);

		var workflowParent = new StateGraph(overAllState).addNode("A", _makeNode("A"))
			.addSubgraph("B", workflowChild)
			.addNode("C", _makeNode("C"))
			.addConditionalEdges(START, edge_async(state -> "a"), Map.of("a", "A", "b", "B"))
			.addEdge("A", "B")
			.addEdge("B", "C")
			.addEdge("C", END)
		// .compile(compileConfig)
		;

		var B_B1 = SubGraphNode.formatId("B", "B1");
		var B_B2 = SubGraphNode.formatId("B", "B2");
		var B_C = SubGraphNode.formatId("B", "C");

		SaverConfig saver = SaverConfig.builder().register(SaverConstant.MEMORY, new MemorySaver()).build();

		var withSaver = workflowParent.compile(CompileConfig.builder().saverConfig(saver).build());

		assertIterableEquals(List.of(START, "A", B_B1, B_B2, B_C, "C", END), _execute(withSaver, Map.of()));

		// INTERRUPT AFTER B1
		CompileConfig compileConfig = CompileConfig.builder().saverConfig(saver).interruptAfter(B_B1).build();
		var interruptAfterB1 = workflowParent.compile(compileConfig);
		assertIterableEquals(List.of(START, "A", B_B1), _execute(interruptAfterB1, Map.of()));

		// RESUME AFTER B1
		overAllState.withResume();
		assertIterableEquals(List.of(B_B2, B_C, "C", END), _execute(interruptAfterB1, Map.of()));

		// INTERRUPT AFTER B2
		OverAllState overAllState1 = getOverAllState();
		workflowParent.setOverAllState(overAllState1);
		var interruptAfterB2 = workflowParent
			.compile(CompileConfig.builder().saverConfig(saver).interruptAfter(B_B2).build());

		assertIterableEquals(List.of(START, "A", B_B1, B_B2), _execute(interruptAfterB2, Map.of()));

		// RESUME AFTER B2
		overAllState1.withResume();
		assertIterableEquals(List.of(B_C, "C", END), _execute(interruptAfterB2, null));

		// INTERRUPT BEFORE C
		OverAllState overAllState2 = getOverAllState();
		workflowParent.setOverAllState(overAllState2);
		var interruptBeforeC = workflowParent
			.compile(CompileConfig.builder().saverConfig(saver).interruptBefore("C").build());

		assertIterableEquals(List.of(START, "A", B_B1, B_B2, B_C), _execute(interruptBeforeC, Map.of()));

		// RESUME AFTER B2
		overAllState2.withResume();
		assertIterableEquals(List.of("C", END), _execute(interruptBeforeC, null));

		// INTERRUPT BEFORE SUBGRAPH B
		OverAllState overAllState3 = getOverAllState();
		workflowParent.setOverAllState(overAllState3);
		var interruptBeforeSubgraphB = workflowParent
			.compile(CompileConfig.builder().saverConfig(saver).interruptBefore("B").build());
		assertIterableEquals(List.of(START, "A"), _execute(interruptBeforeSubgraphB, Map.of()));

		// RESUME AFTER SUBGRAPH B
		overAllState3.withResume();
		assertIterableEquals(List.of(B_B1, B_B2, B_C, "C", END), _execute(interruptBeforeSubgraphB, null));

		// INTERRUPT AFTER SUBGRAPH B
		OverAllState overAllState4 = getOverAllState();
		workflowParent.setOverAllState(overAllState4);
		var exception = assertThrows(GraphStateException.class,
				() -> workflowParent.compile(CompileConfig.builder().saverConfig(saver).interruptAfter("B").build()));

		assertEquals(
				"'interruption after' on subgraph is not supported yet! consider to use 'interruption before' node: 'C'",
				exception.getMessage());

	}

	@Test
	public void testMergeSubgraph04() throws Exception {
		OverAllState overAllState = getOverAllState();
		var workflowChild = new StateGraph().addNode("B1", _makeNode("B1"))
			.addNode("B2", _makeNode("B2"))
			.addNode("C", _makeNode("subgraph(C)"))
			.addEdge(START, "B1")
			.addEdge("B1", "B2")
			.addConditionalEdges("B2", edge_async(state -> "c"), Map.of(END, END, "c", "C"))
			.addEdge("C", END);

		var workflowParent = new StateGraph(overAllState).addNode("A", _makeNode("A"))
			.addSubgraph("B", workflowChild)
			.addNode("C", _makeNode("C"))
			.addConditionalEdges(START, edge_async(state -> "a"), Map.of("a", "A", "b", "B"))
			.addEdge("A", "B")
			.addConditionalEdges("B", edge_async(state -> "c"), Map.of("c", "C", "a", "A"/*
																							 * END,
																							 * END
																							 */))
			.addEdge("C", END);

		var processed = ProcessedNodesEdgesAndConfig.process(workflowParent, CompileConfig.builder().build());
		processed.nodes().elements.forEach(System.out::println);
		processed.edges().elements.forEach(System.out::println);

		assertEquals(5, processed.nodes().elements.size());
		assertEquals(6, processed.edges().elements.size());

		var B_B1 = SubGraphNode.formatId("B", "B1");
		var B_B2 = SubGraphNode.formatId("B", "B2");
		var B_C = SubGraphNode.formatId("B", "C");

		var app = workflowParent.compile();

		assertIterableEquals(List.of(START, "A", B_B1, B_B2, B_C, "C", END), _execute(app, Map.of()));

	}

	@Test
	public void testMergeSubgraph04WithInterruption() throws Exception {
		OverAllState overAllState = getOverAllState();
		var workflowChild = new StateGraph().addNode("B1", _makeNode("B1"))
			.addNode("B2", _makeNode("B2"))
			.addNode("C", _makeNode("subgraph(C)"))
			.addEdge(START, "B1")
			.addEdge("B1", "B2")
			.addConditionalEdges("B2", edge_async(state -> "c"), Map.of(END, END, "c", "C"))
			.addEdge("C", END);

		var workflowParent = new StateGraph(overAllState).addNode("A", _makeNode("A"))
			.addSubgraph("B", workflowChild)
			.addNode("C", _makeNode("C"))
			.addNode("C1", _makeNode("C1"))
			.addConditionalEdges(START, edge_async(state -> "a"), Map.of("a", "A", "b", "B"))
			.addEdge("A", "B")
			.addConditionalEdges("B", edge_async(state -> "c"), Map.of("c", "C1", "a", "A" /*
																							 * END,
																							 * END
																							 */))
			.addEdge("C1", "C")
			.addEdge("C", END);

		var B_B1 = SubGraphNode.formatId("B", "B1");
		var B_B2 = SubGraphNode.formatId("B", "B2");
		var B_C = SubGraphNode.formatId("B", "C");

		SaverConfig saver = SaverConfig.builder().register(SaverConstant.MEMORY, new MemorySaver()).build();

		var withSaver = workflowParent.compile(CompileConfig.builder().saverConfig(saver).build());

		assertIterableEquals(List.of(START, "A", B_B1, B_B2, B_C, "C1", "C", END), _execute(withSaver, Map.of()));

		// INTERRUPT AFTER B1
		OverAllState overAllState0 = getOverAllState();
		workflowParent.setOverAllState(overAllState0);
		var interruptAfterB1 = workflowParent
			.compile(CompileConfig.builder().saverConfig(saver).interruptAfter(B_B1).build());
		assertIterableEquals(List.of(START, "A", B_B1), _execute(interruptAfterB1, Map.of()));

		// RESUME AFTER B1
		overAllState0.withResume();
		assertIterableEquals(List.of(B_B2, B_C, "C1", "C", END), _execute(interruptAfterB1, null));

		// INTERRUPT AFTER B2
		OverAllState overAllState1 = getOverAllState();
		workflowParent.setOverAllState(overAllState1);
		var interruptAfterB2 = workflowParent
			.compile(CompileConfig.builder().saverConfig(saver).interruptAfter(B_B2).build());

		assertIterableEquals(List.of(START, "A", B_B1, B_B2), _execute(interruptAfterB2, Map.of()));

		// RESUME AFTER B2
		overAllState1.withResume();
		assertIterableEquals(List.of(B_C, "C1", "C", END), _execute(interruptAfterB2, null));

		// INTERRUPT BEFORE C
		OverAllState overAllState2 = getOverAllState();
		workflowParent.setOverAllState(overAllState2);
		var interruptBeforeC = workflowParent
			.compile(CompileConfig.builder().saverConfig(saver).interruptBefore("C").build());

		assertIterableEquals(List.of(START, "A", B_B1, B_B2, B_C, "C1"), _execute(interruptBeforeC, Map.of()));

		// RESUME BEFORE C
		overAllState2.withResume();
		assertIterableEquals(List.of("C", END), _execute(interruptBeforeC, null));

		// INTERRUPT BEFORE SUBGRAPH B
		OverAllState overAllState3 = getOverAllState();
		workflowParent.setOverAllState(overAllState3);
		var interruptBeforeB = workflowParent
			.compile(CompileConfig.builder().saverConfig(saver).interruptBefore("B").build());
		assertIterableEquals(List.of(START, "A"), _execute(interruptBeforeB, Map.of()));

		// RESUME BEFORE SUBGRAPH B
		overAllState3.withResume();
		assertIterableEquals(List.of(B_B1, B_B2, B_C, "C1", "C", END), _execute(interruptBeforeB, null));

		//
		// INTERRUPT AFTER SUBGRAPH B
		//
		OverAllState overAllState4 = getOverAllState();
		workflowParent.setOverAllState(overAllState4);
		var exception = assertThrows(GraphStateException.class,
				() -> workflowParent.compile(CompileConfig.builder().saverConfig(saver).interruptAfter("B").build()));

		assertEquals("'interruption after' on subgraph is not supported yet!", exception.getMessage());

	}

	@Test
	public void testCheckpointWithSubgraph() throws Exception {

		SaverConfig saver = SaverConfig.builder().register(SaverConstant.MEMORY, new MemorySaver()).build();

		var compileConfig = CompileConfig.builder().saverConfig(saver).build();
		OverAllState overAllState = getOverAllState();
		var workflowChild = new StateGraph().addNode("step_1", _makeNode("child:step1"))
			.addNode("step_2", _makeNode("child:step2"))
			.addNode("step_3", _makeNode("child:step3"))
			.addEdge(START, "step_1")
			.addEdge("step_1", "step_2")
			.addEdge("step_2", "step_3")
			.addEdge("step_3", END)
		// .compile(compileConfig)
		;

		var workflowParent = new StateGraph(overAllState).addNode("step_1", _makeNode("step1"))
			.addNode("step_2", _makeNode("step2"))
			.addNode("step_3", _makeNode("step3"))
			.addSubgraph("subgraph", workflowChild)
			.addEdge(START, "step_1")
			.addEdge("step_1", "step_2")
			.addEdge("step_2", "subgraph")
			.addEdge("subgraph", "step_3")
			.addEdge("step_3", END)
			.compile(compileConfig);

		var result = workflowParent.stream()
			.stream()
			.peek(n -> log.info("{}", n))
			.reduce((a, b) -> b)
			.map(NodeOutput::state);

		assertTrue(result.isPresent());
		assertIterableEquals(List.of("step1", "step2", "child:step1", "child:step2", "child:step3", "step3"),
				(List<String>) result.get().value("messages").get());

	}

}

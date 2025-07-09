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

import com.alibaba.cloud.ai.graph.action.*;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.LogManager;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SubGraphTest {

	private static final Logger log = LoggerFactory.getLogger(SubGraphTest.class);

	/**
	 * Initialize logging configuration before all tests.
	 */
	@BeforeAll
	public static void initLogging() throws IOException {
		try (var is = SubGraphTest.class.getResourceAsStream("/logging.properties")) {
			LogManager.getLogManager().readConfiguration(is);
		}
	}

	/**
	 * Create an AsyncNodeAction that returns a map with the given ID as value for
	 * "messages".
	 * @param id The identifier for the node action.
	 * @return An AsyncNodeAction producing a map with the message ID.
	 */
	private AsyncNodeAction _makeNode(String id) {
		return node_async(state -> Map.of("messages", id));
	}

	/**
	 * Execute the workflow and extract the names of processed nodes.
	 * @param workflow Compiled graph to execute.
	 * @param input Initial input data for execution.
	 * @return A list containing the names of executed nodes in order.
	 * @throws Exception If an error occurs during execution.
	 */
	private List<String> _execute(CompiledGraph workflow, Map<String, Object> input) throws Exception {
		return workflow.stream(input, RunnableConfig.builder().threadId("SubGraphTest").build())
			.stream()
			.peek(System.out::println)
			.map(NodeOutput::node)
			.toList();
	}

	/**
	 * Get an initialized OverAllState instance with predefined key strategies.
	 * @return Initialized OverAllState object.
	 */
	private static KeyStrategyFactory createKeyStrategyFactory() {
		return () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("a", (o, o2) -> o);
			keyStrategyMap.put("b", (o, o2) -> o2);
			keyStrategyMap.put("c", (o, o2) -> o2);
			keyStrategyMap.put("steps", (o, o2) -> o2);
			keyStrategyMap.put("messages", new AppendStrategy());
			return keyStrategyMap;
		};
	}

	/**
	 * Test basic subgraph merging functionality without conditional edges.
	 */
	@Test
	public void testMergeSubgraph01() throws Exception {

		var workflowChild = new StateGraph().addNode("B1", _makeNode("B1"))
			.addNode("B2", _makeNode("B2"))
			.addEdge(START, "B1")
			.addEdge("B1", "B2")
			.addEdge("B2", END);

		var workflowParent = new StateGraph(createKeyStrategyFactory()).addNode("A", _makeNode("A"))
			.addNode("B", workflowChild)
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

	/**
	 * Test subgraph merging with conditional edge handling.
	 */
	@Test
	public void testMergeSubgraph02() throws Exception {

		var workflowChild = new StateGraph().addNode("B1", _makeNode("B1"))
			.addNode("B2", _makeNode("B2"))
			.addEdge(START, "B1")
			.addEdge("B1", "B2")
			.addEdge("B2", END);

		var workflowParent = new StateGraph(createKeyStrategyFactory()).addNode("A", _makeNode("A"))
			.addNode("B", workflowChild)
			.addNode("C", _makeNode("C"))
			.addConditionalEdges(START, edge_async(state -> "a"), Map.of("a", "A", "b", "B"))
			.addEdge("A", "B")
			.addEdge("B", "C")
			.addEdge("C", END);

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

	/**
	 * Test subgraph merging with nested conditional edges.
	 */
	@Test
	public void testMergeSubgraph03() throws Exception {

		var workflowChild = new StateGraph().addNode("B1", _makeNode("B1"))
			.addNode("B2", _makeNode("B2"))
			.addNode("C", _makeNode("subgraph(C)"))
			.addEdge(START, "B1")
			.addEdge("B1", "B2")
			.addConditionalEdges("B2", edge_async(state -> "c"), Map.of(END, END, "c", "C"))
			.addEdge("C", END);

		var workflowParent = new StateGraph(createKeyStrategyFactory()).addNode("A", _makeNode("A"))
			.addNode("B", workflowChild)
			.addNode("C", _makeNode("C"))
			.addConditionalEdges(START, edge_async(state -> "a"), Map.of("a", "A", "b", "B"))
			.addEdge("A", "B")
			.addEdge("B", "C")
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

	/**
	 * Test subgraph merging with interruption handling at different points.
	 */
	@Test
	public void testMergeSubgraph03WithInterruption() throws Exception {
		var workflowChild = new StateGraph().addNode("B1", _makeNode("B1"))
			.addNode("B2", _makeNode("B2"))
			.addNode("C", _makeNode("subgraph(C)"))
			.addEdge(START, "B1")
			.addEdge("B1", "B2")
			.addConditionalEdges("B2", edge_async(state -> "c"), Map.of(END, END, "c", "C"))
			.addEdge("C", END);

		var workflowParent = new StateGraph(createKeyStrategyFactory()).addNode("A", _makeNode("A"))
			.addNode("B", workflowChild)
			.addNode("C", _makeNode("C"))
			.addConditionalEdges(START, edge_async(state -> "a"), Map.of("a", "A", "b", "B"))
			.addEdge("A", "B")
			.addEdge("B", "C")
			.addEdge("C", END);

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
		assertIterableEquals(List.of(B_B2, B_C, "C", END), _execute(interruptAfterB1, null));

		// INTERRUPT AFTER B2
		var interruptAfterB2 = workflowParent
			.compile(CompileConfig.builder().saverConfig(saver).interruptAfter(B_B2).build());

		assertIterableEquals(List.of(START, "A", B_B1, B_B2), _execute(interruptAfterB2, Map.of()));

		// RESUME AFTER B2
		assertIterableEquals(List.of(B_C, "C", END), _execute(interruptAfterB2, null));

		// INTERRUPT BEFORE C
		var interruptBeforeC = workflowParent
			.compile(CompileConfig.builder().saverConfig(saver).interruptBefore("C").build());

		assertIterableEquals(List.of(START, "A", B_B1, B_B2, B_C), _execute(interruptBeforeC, Map.of()));

		// RESUME AFTER B2
		assertIterableEquals(List.of("C", END), _execute(interruptBeforeC, null));

		// INTERRUPT BEFORE SUBGRAPH B
		var interruptBeforeSubgraphB = workflowParent
			.compile(CompileConfig.builder().saverConfig(saver).interruptBefore("B").build());
		assertIterableEquals(List.of(START, "A"), _execute(interruptBeforeSubgraphB, Map.of()));

		// RESUME AFTER SUBGRAPH B
		assertIterableEquals(List.of(B_B1, B_B2, B_C, "C", END), _execute(interruptBeforeSubgraphB, null));

		// INTERRUPT AFTER SUBGRAPH B
		var exception = assertThrows(GraphStateException.class,
				() -> workflowParent.compile(CompileConfig.builder().saverConfig(saver).interruptAfter("B").build()));

		assertEquals(
				"'interruption after' on subgraph is not supported yet! consider to use 'interruption before' node: 'C'",
				exception.getMessage());

	}

	/**
	 * Test more complex subgraph merging with multiple conditional branches.
	 */
	@Test
	public void testMergeSubgraph04() throws Exception {
		var workflowChild = new StateGraph().addNode("B1", _makeNode("B1"))
			.addNode("B2", _makeNode("B2"))
			.addNode("C", _makeNode("subgraph(C)"))
			.addEdge(START, "B1")
			.addEdge("B1", "B2")
			.addConditionalEdges("B2", edge_async(state -> "c"), Map.of(END, END, "c", "C"))
			.addEdge("C", END);

		var workflowParent = new StateGraph(createKeyStrategyFactory()).addNode("A", _makeNode("A"))
			.addNode("B", workflowChild)
			.addNode("C", _makeNode("C"))
			.addConditionalEdges(START, edge_async(state -> "a"), Map.of("a", "A", "b", "B"))
			.addEdge("A", "B")
			.addConditionalEdges("B", edge_async(state -> "c"), Map.of("c", "C", "a", "A"))
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

	/**
	 * Test complex subgraph merging with multiple interruptions at various points.
	 */
	@Test
	public void testMergeSubgraph04WithInterruption() throws Exception {
		var workflowChild = new StateGraph().addNode("B1", _makeNode("B1"))
			.addNode("B2", _makeNode("B2"))
			.addNode("C", _makeNode("subgraph(C)"))
			.addEdge(START, "B1")
			.addEdge("B1", "B2")
			.addConditionalEdges("B2", edge_async(state -> "c"), Map.of(END, END, "c", "C"))
			.addEdge("C", END);

		var workflowParent = new StateGraph(createKeyStrategyFactory()).addNode("A", _makeNode("A"))
			.addNode("B", workflowChild)
			.addNode("C", _makeNode("C"))
			.addNode("C1", _makeNode("C1"))
			.addConditionalEdges(START, edge_async(state -> "a"), Map.of("a", "A", "b", "B"))
			.addEdge("A", "B")
			.addConditionalEdges("B", edge_async(state -> "c"), Map.of("c", "C1", "a", "A"))
			.addEdge("C1", "C")
			.addEdge("C", END);

		var B_B1 = SubGraphNode.formatId("B", "B1");
		var B_B2 = SubGraphNode.formatId("B", "B2");
		var B_C = SubGraphNode.formatId("B", "C");

		SaverConfig saver = SaverConfig.builder().register(SaverConstant.MEMORY, new MemorySaver()).build();

		var withSaver = workflowParent.compile(CompileConfig.builder().saverConfig(saver).build());

		assertIterableEquals(List.of(START, "A", B_B1, B_B2, B_C, "C1", "C", END), _execute(withSaver, Map.of()));

		// INTERRUPT AFTER B1
		var interruptAfterB1 = workflowParent
			.compile(CompileConfig.builder().saverConfig(saver).interruptAfter(B_B1).build());
		assertIterableEquals(List.of(START, "A", B_B1), _execute(interruptAfterB1, Map.of()));

		// RESUME AFTER B1
		assertIterableEquals(List.of(B_B2, B_C, "C1", "C", END), _execute(interruptAfterB1, null));

		// INTERRUPT AFTER B2
		var interruptAfterB2 = workflowParent
			.compile(CompileConfig.builder().saverConfig(saver).interruptAfter(B_B2).build());

		assertIterableEquals(List.of(START, "A", B_B1, B_B2), _execute(interruptAfterB2, Map.of()));

		// RESUME AFTER B2
		assertIterableEquals(List.of(B_C, "C1", "C", END), _execute(interruptAfterB2, null));

		// INTERRUPT BEFORE C
		var interruptBeforeC = workflowParent
			.compile(CompileConfig.builder().saverConfig(saver).interruptBefore("C").build());

		assertIterableEquals(List.of(START, "A", B_B1, B_B2, B_C, "C1"), _execute(interruptBeforeC, Map.of()));

		// RESUME BEFORE C
		assertIterableEquals(List.of("C", END), _execute(interruptBeforeC, null));

		// INTERRUPT BEFORE SUBGRAPH B
		var interruptBeforeB = workflowParent
			.compile(CompileConfig.builder().saverConfig(saver).interruptBefore("B").build());
		assertIterableEquals(List.of(START, "A"), _execute(interruptBeforeB, Map.of()));

		// RESUME BEFORE SUBGRAPH B
		assertIterableEquals(List.of(B_B1, B_B2, B_C, "C1", "C", END), _execute(interruptBeforeB, null));

		// INTERRUPT AFTER SUBGRAPH B
		var exception = assertThrows(GraphStateException.class,
				() -> workflowParent.compile(CompileConfig.builder().saverConfig(saver).interruptAfter("B").build()));

		assertEquals("'interruption after' on subgraph is not supported yet!", exception.getMessage());

	}

	/**
	 * Test checkpointing behavior with subgraphs involved.
	 */
	@Test
	public void testCheckpointWithSubgraph() throws Exception {

		SaverConfig saver = SaverConfig.builder().register(SaverConstant.MEMORY, new MemorySaver()).build();

		var compileConfig = CompileConfig.builder().saverConfig(saver).build();
		var workflowChild = new StateGraph().addNode("step_1", _makeNode("child:step1"))
			.addNode("step_2", _makeNode("child:step2"))
			.addNode("step_3", _makeNode("child:step3"))
			.addEdge(START, "step_1")
			.addEdge("step_1", "step_2")
			.addEdge("step_2", "step_3")
			.addEdge("step_3", END);

		var workflowParent = new StateGraph(createKeyStrategyFactory()).addNode("step_1", _makeNode("step1"))
			.addNode("step_2", _makeNode("step2"))
			.addNode("step_3", _makeNode("step3"))
			.addNode("subgraph", workflowChild)
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

	/**
	 * Test alternative methods for creating and integrating subgraphs.
	 */
	@Test
	public void testOtherCreateSubgraph2() throws Exception {
		SaverConfig saver = SaverConfig.builder().register(SaverConstant.MEMORY, new MemorySaver()).build();

		var compileConfig = CompileConfig.builder().saverConfig(saver).build();
		var workflowChild = new StateGraph(createKeyStrategyFactory()).addNode("step_1", _makeNode("child:step1"))
			.addNode("step_2", _makeNode("child:step2"))
			.addNode("step_3", _makeNode("child:step3"))
			.addEdge(START, "step_1")
			.addEdge("step_1", "step_2")
			.addEdge("step_2", "step_3")
			.addEdge("step_3", END);

		var workflowParent = new StateGraph(createKeyStrategyFactory()).addNode("step_1", _makeNode("step1"))
			.addNode("step_2", _makeNode("step2"))
			.addNode("step_3", _makeNode("step3"))
			.addNode("subgraph", AsyncNodeActionWithConfig.node_async((t, config) -> {
				// Reference the parent class Overallstate or create a new one
				return workflowChild.compile().invoke(Map.copyOf(t.data())).orElseThrow().data();
			}))
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
	}

	@Test
	public void testCommandNodeSubGraph() throws Exception {
		StateGraph childGraph = new StateGraph(() -> {
			HashMap<String, KeyStrategy> stringKeyStrategyHashMap = new HashMap<>();
			stringKeyStrategyHashMap.put("messages", new AppendStrategy());
			return stringKeyStrategyHashMap;
		});
		childGraph.addNode("node1", _makeNode("node1"));
		childGraph.addNode("node2", _makeNode("node2"));
		CommandAction commandAction = new CommandAction() {
			@Override
			public Command apply(OverAllState state, RunnableConfig config) throws Exception {
				return new Command("node1", Map.of("messages", "go to node 1"));
			}
		};
		childGraph.addNode("commandNode", AsyncCommandAction.node_async(commandAction),
				Map.of("node1", "node1", "node2", "node2"));

		childGraph.addEdge(START, "commandNode");
		childGraph.addEdge("node1", "node2");
		childGraph.addEdge("node2", END);

		StateGraph parentGraph = new StateGraph(() -> {
			HashMap<String, KeyStrategy> stringKeyStrategyHashMap = new HashMap<>();
			stringKeyStrategyHashMap.put("messages", new AppendStrategy());
			return stringKeyStrategyHashMap;
		});

		parentGraph.addNode("p_node1", _makeNode("p_node1"));
		parentGraph.addNode("p_node2", _makeNode("p_node2"));

		parentGraph.addNode("c_graph", childGraph);

		parentGraph.addNode("p_command_node", AsyncCommandAction.node_async(new CommandAction() {
			@Override
			public Command apply(OverAllState state, RunnableConfig config) throws Exception {
				return new Command("p_node1", Map.of("messages", "go to p_node1"));
			}
		}), Map.of("p_node1", "p_node1", "p_node2", "p_node2"));

		parentGraph.addEdge(START, "p_command_node");
		parentGraph.addEdge("p_node1", "p_node2");
		parentGraph.addEdge("p_node2", "c_graph");
		parentGraph.addEdge("c_graph", END);

		CompiledGraph compile = parentGraph.compile();
		System.out.println(compile.getGraph(GraphRepresentation.Type.PLANTUML).content());
		OverAllState state = compile.invoke(Map.of()).orElseThrow();
		assertEquals(
				Map.of("messages", List.of("go to p_node1", "p_node1", "p_node2", "go to node 1", "node1", "node2")),
				state.data());
	}

}

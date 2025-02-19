package com.alibaba.cloud.ai.graph;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.LogManager;

import lombok.extern.slf4j.Slf4j;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.prebuilt.MessagesState;
import com.alibaba.cloud.ai.graph.prebuilt.MessagesStateGraph;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class SubGraphTest {

	@BeforeAll
	public static void initLogging() throws IOException {
		try (var is = StateGraphPersistenceTest.class.getResourceAsStream("/logging.properties")) {
			LogManager.getLogManager().readConfiguration(is);
		}
	}

	private AsyncNodeAction<MessagesState<String>> _makeNode(String id) {
		return node_async(state -> Map.of("messages", id));
	}

	private List<String> _execute(CompiledGraph<?> workflow, Map<String, Object> input) throws Exception {
		return workflow.stream(input).stream().peek(System.out::println).map(NodeOutput::node).toList();
	}

	@Test
	public void testMergeSubgraph01() throws Exception {

		var workflowChild = new MessagesStateGraph<String>().addNode("B1", _makeNode("B1"))
			.addNode("B2", _makeNode("B2"))
			.addEdge(START, "B1")
			.addEdge("B1", "B2")
			.addEdge("B2", END);

		var workflowParent = new MessagesStateGraph<String>().addNode("A", _makeNode("A"))
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

		var workflowChild = new MessagesStateGraph<String>().addNode("B1", _makeNode("B1"))
			.addNode("B2", _makeNode("B2"))
			.addEdge(START, "B1")
			.addEdge("B1", "B2")
			.addEdge("B2", END);

		var workflowParent = new MessagesStateGraph<String>().addNode("A", _makeNode("A"))
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

		var workflowChild = new MessagesStateGraph<String>().addNode("B1", _makeNode("B1"))
			.addNode("B2", _makeNode("B2"))
			.addNode("C", _makeNode("subgraph(C)"))
			.addEdge(START, "B1")
			.addEdge("B1", "B2")
			.addConditionalEdges("B2", edge_async(state -> "c"), Map.of(END, END, "c", "C"))
			.addEdge("C", END);

		var workflowParent = new MessagesStateGraph<String>().addNode("A", _makeNode("A"))
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

		var workflowChild = new MessagesStateGraph<String>().addNode("B1", _makeNode("B1"))
			.addNode("B2", _makeNode("B2"))
			.addNode("C", _makeNode("subgraph(C)"))
			.addEdge(START, "B1")
			.addEdge("B1", "B2")
			.addConditionalEdges("B2", edge_async(state -> "c"), Map.of(END, END, "c", "C"))
			.addEdge("C", END);

		var workflowParent = new MessagesStateGraph<String>().addNode("A", _makeNode("A"))
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

		var saver = new MemorySaver();

		var withSaver = workflowParent.compile(CompileConfig.builder().checkpointSaver(saver).build());

		assertIterableEquals(List.of(START, "A", B_B1, B_B2, B_C, "C", END), _execute(withSaver, Map.of()));

		// INTERRUPT AFTER B1
		var interruptAfterB1 = workflowParent
			.compile(CompileConfig.builder().checkpointSaver(saver).interruptAfter(B_B1).build());
		assertIterableEquals(List.of(START, "A", B_B1), _execute(interruptAfterB1, Map.of()));

		// RESUME AFTER B1
		assertIterableEquals(List.of(B_B2, B_C, "C", END), _execute(interruptAfterB1, null));

		// INTERRUPT AFTER B2
		var interruptAfterB2 = workflowParent
			.compile(CompileConfig.builder().checkpointSaver(saver).interruptAfter(B_B2).build());

		assertIterableEquals(List.of(START, "A", B_B1, B_B2), _execute(interruptAfterB2, Map.of()));

		// RESUME AFTER B2
		assertIterableEquals(List.of(B_C, "C", END), _execute(interruptAfterB2, null));

		// INTERRUPT BEFORE C
		var interruptBeforeC = workflowParent
			.compile(CompileConfig.builder().checkpointSaver(saver).interruptBefore("C").build());

		assertIterableEquals(List.of(START, "A", B_B1, B_B2, B_C), _execute(interruptBeforeC, Map.of()));

		// RESUME AFTER B2
		assertIterableEquals(List.of("C", END), _execute(interruptBeforeC, null));

		// INTERRUPT BEFORE SUBGRAPH B
		var interruptBeforeSubgraphB = workflowParent
			.compile(CompileConfig.builder().checkpointSaver(saver).interruptBefore("B").build());
		assertIterableEquals(List.of(START, "A"), _execute(interruptBeforeSubgraphB, Map.of()));

		// RESUME AFTER SUBGRAPH B
		assertIterableEquals(List.of(B_B1, B_B2, B_C, "C", END), _execute(interruptBeforeSubgraphB, null));

		// INTERRUPT AFTER SUBGRAPH B
		var exception = assertThrows(GraphStateException.class, () -> workflowParent
			.compile(CompileConfig.builder().checkpointSaver(saver).interruptAfter("B").build()));

		assertEquals(
				"'interruption after' on subgraph is not supported yet! consider to use 'interruption before' node: 'C'",
				exception.getMessage());

	}

	@Test
	public void testMergeSubgraph04() throws Exception {

		var workflowChild = new MessagesStateGraph<String>().addNode("B1", _makeNode("B1"))
			.addNode("B2", _makeNode("B2"))
			.addNode("C", _makeNode("subgraph(C)"))
			.addEdge(START, "B1")
			.addEdge("B1", "B2")
			.addConditionalEdges("B2", edge_async(state -> "c"), Map.of(END, END, "c", "C"))
			.addEdge("C", END);

		var workflowParent = new MessagesStateGraph<String>().addNode("A", _makeNode("A"))
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

		var workflowChild = new MessagesStateGraph<String>().addNode("B1", _makeNode("B1"))
			.addNode("B2", _makeNode("B2"))
			.addNode("C", _makeNode("subgraph(C)"))
			.addEdge(START, "B1")
			.addEdge("B1", "B2")
			.addConditionalEdges("B2", edge_async(state -> "c"), Map.of(END, END, "c", "C"))
			.addEdge("C", END);

		var workflowParent = new MessagesStateGraph<String>().addNode("A", _makeNode("A"))
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

		var saver = new MemorySaver();

		var withSaver = workflowParent.compile(CompileConfig.builder().checkpointSaver(saver).build());

		assertIterableEquals(List.of(START, "A", B_B1, B_B2, B_C, "C1", "C", END), _execute(withSaver, Map.of()));

		// INTERRUPT AFTER B1
		var interruptAfterB1 = workflowParent
			.compile(CompileConfig.builder().checkpointSaver(saver).interruptAfter(B_B1).build());
		assertIterableEquals(List.of(START, "A", B_B1), _execute(interruptAfterB1, Map.of()));

		// RESUME AFTER B1
		assertIterableEquals(List.of(B_B2, B_C, "C1", "C", END), _execute(interruptAfterB1, null));

		// INTERRUPT AFTER B2
		var interruptAfterB2 = workflowParent
			.compile(CompileConfig.builder().checkpointSaver(saver).interruptAfter(B_B2).build());

		assertIterableEquals(List.of(START, "A", B_B1, B_B2), _execute(interruptAfterB2, Map.of()));

		// RESUME AFTER B2
		assertIterableEquals(List.of(B_C, "C1", "C", END), _execute(interruptAfterB2, null));

		// INTERRUPT BEFORE C
		var interruptBeforeC = workflowParent
			.compile(CompileConfig.builder().checkpointSaver(saver).interruptBefore("C").build());

		assertIterableEquals(List.of(START, "A", B_B1, B_B2, B_C, "C1"), _execute(interruptBeforeC, Map.of()));

		// RESUME BEFORE C
		assertIterableEquals(List.of("C", END), _execute(interruptBeforeC, null));

		// INTERRUPT BEFORE SUBGRAPH B
		var interruptBeforeB = workflowParent
			.compile(CompileConfig.builder().checkpointSaver(saver).interruptBefore("B").build());
		assertIterableEquals(List.of(START, "A"), _execute(interruptBeforeB, Map.of()));

		// RESUME BEFORE SUBGRAPH B
		assertIterableEquals(List.of(B_B1, B_B2, B_C, "C1", "C", END), _execute(interruptBeforeB, null));

		//
		// INTERRUPT AFTER SUBGRAPH B
		//
		var exception = assertThrows(GraphStateException.class, () -> workflowParent
			.compile(CompileConfig.builder().checkpointSaver(saver).interruptAfter("B").build()));

		assertEquals("'interruption after' on subgraph is not supported yet!", exception.getMessage());

	}

	@Test
	public void testCheckpointWithSubgraph() throws Exception {

		var compileConfig = CompileConfig.builder().checkpointSaver(new MemorySaver()).build();

		var workflowChild = new MessagesStateGraph<String>().addNode("step_1", _makeNode("child:step1"))
			.addNode("step_2", _makeNode("child:step2"))
			.addNode("step_3", _makeNode("child:step3"))
			.addEdge(START, "step_1")
			.addEdge("step_1", "step_2")
			.addEdge("step_2", "step_3")
			.addEdge("step_3", END)
		// .compile(compileConfig)
		;

		var workflowParent = new MessagesStateGraph<String>().addNode("step_1", _makeNode("step1"))
			.addNode("step_2", _makeNode("step2"))
			.addNode("step_3", _makeNode("step3"))
			.addSubgraph("subgraph", workflowChild)
			.addEdge(START, "step_1")
			.addEdge("step_1", "step_2")
			.addEdge("step_2", "subgraph")
			.addEdge("subgraph", "step_3")
			.addEdge("step_3", END)
			.compile(compileConfig);

		var result = workflowParent.stream(Map.of())
			.stream()
			.peek(n -> log.info("{}", n))
			.reduce((a, b) -> b)
			.map(NodeOutput::state);

		assertTrue(result.isPresent());
		assertIterableEquals(List.of("step1", "step2", "child:step1", "child:step2", "child:step3", "step3"),
				result.get().messages());

	}

}

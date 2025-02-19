package com.alibaba.cloud.ai.graph;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.prebuilt.MessagesState;
import com.alibaba.cloud.ai.graph.state.*;
import org.junit.jupiter.api.Test;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for simple App.
 */
@Slf4j
public class StateGraphTest {

	static class State extends MessagesState<String> {

		public State(Map<String, Object> initData) {
			super(initData);
		}

		int steps() {
			return this.<Integer>value("steps").orElse(0);
		}

	}

	public static <T> List<Map.Entry<String, T>> sortMap(Map<String, T> map) {
		return map.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList());
	}

	@Test
	void testValidation() throws Exception {

		StateGraph<AgentState> workflow = new StateGraph<>(AgentState::new);
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

		StateGraph<AgentState> workflow = new StateGraph<>(AgentState::new).addEdge(START, "agent_1")
			.addNode("agent_1", node_async(state -> {
				log.info("agent_1\n{}", state);
				return Map.of("prop1", "test");
			}))
			.addEdge("agent_1", END);

		CompiledGraph<AgentState> app = workflow.compile();

		Optional<AgentState> result = app.invoke(Map.of("input", "test1"));
		assertTrue(result.isPresent());

		Map<String, String> expected = Map.of("input", "test1", "prop1", "test");

		assertIterableEquals(sortMap(expected), sortMap(result.get().data()));
		// assertDictionaryOfAnyEqual( expected, result.data )

	}

	@Test
	void testWithAppender() throws Exception {

		StateGraph<State> workflow = new StateGraph<>(State.SCHEMA, State::new).addNode("agent_1", node_async(state -> {
			System.out.println("agent_1");
			return Map.of("messages", "message1");
		})).addNode("agent_2", node_async(state -> {
			System.out.println("agent_2");
			return Map.of("messages", new String[] { "message2" });
		})).addNode("agent_3", node_async(state -> {
			System.out.println("agent_3");
			int steps = state.messages().size() + 1;
			return Map.of("messages", "message3", "steps", steps);
		}))
			.addEdge("agent_1", "agent_2")
			.addEdge("agent_2", "agent_3")
			.addEdge(START, "agent_1")
			.addEdge("agent_3", END);

		CompiledGraph<State> app = workflow.compile();

		Optional<State> result = app.invoke(Map.of());

		assertTrue(result.isPresent());
		System.out.println(result.get().data());
		assertEquals(3, result.get().steps());
		assertEquals(3, result.get().messages().size());
		assertIterableEquals(List.of("message1", "message2", "message3"), result.get().messages());

	}

	@Test
	void testWithAppenderOneRemove() throws Exception {

		StateGraph<State> workflow = new StateGraph<>(State.SCHEMA, State::new).addNode("agent_1", node_async(state -> {
			log.info("agent_1");
			return Map.of("messages", "message1");
		})).addNode("agent_2", node_async(state -> {
			log.info("agent_2");
			return Map.of("messages", new String[] { "message2" });
		})).addNode("agent_3", node_async(state -> {
			log.info("agent_3");
			int steps = state.messages().size() + 1;
			return Map.of("messages", RemoveByHash.of("message2"), "steps", steps);
		}))
			.addEdge("agent_1", "agent_2")
			.addEdge("agent_2", "agent_3")
			.addEdge(START, "agent_1")
			.addEdge("agent_3", END);

		CompiledGraph<State> app = workflow.compile();

		Optional<State> result = app.invoke(Map.of());

		assertTrue(result.isPresent());
		log.info("{}", result.get().data());
		assertEquals(3, result.get().steps());
		assertEquals(1, result.get().messages().size());
		assertIterableEquals(List.of("message1"), result.get().messages());

	}

	@Test
	void testWithAppenderOneAppendOneRemove() throws Exception {

		StateGraph<State> workflow = new StateGraph<>(State.SCHEMA, State::new)
			.addNode("agent_1", node_async(state -> Map.of("messages", "message1")))
			.addNode("agent_2", node_async(state -> Map.of("messages", new String[] { "message2" })))
			.addNode("agent_3",
					node_async(state -> Map.of("messages", List.of("message3", RemoveByHash.of("message2")))))
			.addNode("agent_4", node_async(state -> {
				int steps = state.messages().size() + 1;
				return Map.of("messages", List.of("message4"), "steps", steps);

			}))
			.addEdge("agent_1", "agent_2")
			.addEdge("agent_2", "agent_3")
			.addEdge("agent_3", "agent_4")
			.addEdge(START, "agent_1")
			.addEdge("agent_4", END);

		CompiledGraph<State> app = workflow.compile();

		Optional<State> result = app.invoke(Map.of());

		assertTrue(result.isPresent());
		System.out.println(result.get().data());
		assertEquals(3, result.get().steps());
		assertEquals(3, result.get().messages().size());
		assertIterableEquals(List.of("message1", "message3", "message4"), result.get().messages());

	}

	@Test
	public void testWithSubgraph() throws Exception {

		var childStep1 = node_async((State state) -> Map.of("messages", "child:step1"));

		var childStep2 = node_async((State state) -> Map.of("messages", "child:step2"));

		var childStep3 = node_async((State state) -> Map.of("messages", "child:step3"));

		var workflowChild = new StateGraph<>(State.SCHEMA, State::new).addNode("child:step_1", childStep1)
			.addNode("child:step_2", childStep2)
			.addNode("child:step_3", childStep3)
			.addEdge(START, "child:step_1")
			.addEdge("child:step_1", "child:step_2")
			.addEdge("child:step_2", "child:step_3")
			.addEdge("child:step_3", END)
		// .compile()
		;
		var step1 = node_async((State state) -> Map.of("messages", "step1"));

		var step2 = node_async((State state) -> Map.of("messages", "step2"));

		var step3 = node_async((State state) -> Map.of("messages", "step3"));

		var workflowParent = new StateGraph<>(State.SCHEMA, State::new).addNode("step_1", step1)
			.addNode("step_2", step2)
			.addNode("step_3", step3)
			.addSubgraph("subgraph", workflowChild)
			.addEdge(START, "step_1")
			.addEdge("step_1", "step_2")
			.addEdge("step_2", "subgraph")
			.addEdge("subgraph", "step_3")
			.addEdge("step_3", END)
			.compile();

		var result = workflowParent.stream(Map.of())
			.stream()
			.peek(System.out::println)
			.reduce((a, b) -> b)
			.map(NodeOutput::state);

		assertTrue(result.isPresent());
		assertIterableEquals(List.of("step1", "step2", "child:step1", "child:step2", "child:step3", "step3"),
				result.get().messages());

	}

	private AsyncNodeAction<State> makeNode(String id) {
		return node_async(state -> {
			log.info("call node {}", id);
			return Map.of("messages", id);
		});
	}

	@Test
	void testWithParallelBranch() throws Exception {

		var workflow = new StateGraph<State>(State.SCHEMA, State::new).addNode("A", makeNode("A"))
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

		var result = app.stream(Map.of()).stream().peek(System.out::println).reduce((a, b) -> b).map(NodeOutput::state);
		assertTrue(result.isPresent());
		assertIterableEquals(List.of("A", "A1", "A2", "A3", "B", "C"), result.get().messages());

		workflow = new StateGraph<State>(State.SCHEMA, State::new)
			// .addNode("A", makeNode("A"))
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

		result = app.stream(Map.of()).stream().peek(System.out::println).reduce((a, b) -> b).map(NodeOutput::state);

		assertTrue(result.isPresent());
		assertIterableEquals(List.of("A1", "A2", "A3", "B", "C"), result.get().messages());

	}

	@Test
	void testWithParallelBranchWithErrors() throws Exception {

		// ONLY ONE TARGET
		var onlyOneTarget = new StateGraph<>(State.SCHEMA, State::new).addNode("A", makeNode("A"))
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

		var noConditionalEdge = new StateGraph<>(State.SCHEMA, State::new).addNode("A", makeNode("A"))
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

		var noConditionalEdgeOnBranch = new StateGraph<>(State.SCHEMA, State::new).addNode("A", makeNode("A"))
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

		var noDuplicateTarget = new StateGraph<>(State.SCHEMA, State::new).addNode("A", makeNode("A"))
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

}

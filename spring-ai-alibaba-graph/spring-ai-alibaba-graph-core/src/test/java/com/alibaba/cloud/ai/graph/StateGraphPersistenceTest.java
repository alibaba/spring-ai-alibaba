//package com.alibaba.cloud.ai.graph;
//
//import java.io.IOException;
//import java.util.Map;
//import java.util.Optional;
//import java.util.logging.LogManager;
//import java.util.stream.Collectors;
//
//import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
//import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant;
//import lombok.extern.slf4j.Slf4j;
//import com.alibaba.cloud.ai.graph.action.EdgeAction;
//import com.alibaba.cloud.ai.graph.action.NodeAction;
//import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
//import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
//import com.alibaba.cloud.ai.graph.prebuilt.MessagesState;
//import com.alibaba.cloud.ai.graph.state.AgentState;
//import com.alibaba.cloud.ai.graph.state.StateSnapshot;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//
//import static java.lang.String.format;
//import static java.util.Collections.emptyMap;
//import static com.alibaba.cloud.ai.graph.StateGraph.END;
//import static com.alibaba.cloud.ai.graph.StateGraph.START;
//import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
//import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertFalse;
//import static org.junit.jupiter.api.Assertions.assertInstanceOf;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
///**
// * Unit test for simple App.
// */
//@Slf4j
//public class StateGraphPersistenceTest {
//
//	static class State extends MessagesState<String> {
//
//		public State(Map<String, Object> initData) {
//			super(initData);
//		}
//
//		int steps() {
//			return this.<Integer>value("steps").orElse(0);
//		}
//
//	}
//
//	NodeAction agent_whether = state -> {
//		String lastMessage = state.lastMessage().orElseThrow(() -> new IllegalStateException("No last message!"));
//
//		if (lastMessage.contains("temperature")) {
//			return Map.of("messages", "whether in Naples is sunny");
//		}
//		if (lastMessage.contains("whether")) {
//			return Map.of("messages", "tool_calls");
//		}
//		if (lastMessage.contains("bartolo")) {
//			return Map.of("messages", "Hi bartolo, nice to meet you too! How can I assist you today?");
//		}
//		if (state.messages().stream().anyMatch(m -> m.contains("bartolo"))) {
//			return Map.of("messages", "Hi, bartolo welcome back?");
//		}
//		throw new IllegalStateException("unknown message!");
//	};
//
//	// Simulate LLM agent
//	NodeAction<State> tool_whether = state -> Map.of("messages", "temperature in Napoli is 30 degree");
//
//	EdgeAction<State> shouldContinue_whether = state -> state.lastMessage()
//		.filter(m -> m.equals("tool_calls"))
//		.map(m -> "tools")
//		.orElse(END);
//
//	@BeforeAll
//	public static void initLogging() throws IOException {
//		try (var is = StateGraphPersistenceTest.class.getResourceAsStream("/logging.properties")) {
//			LogManager.getLogManager().readConfiguration(is);
//		}
//	}
//
//	@Test
//	public void testCheckpointInitialState() throws Exception {
//		NodeAction agent_1 = state -> {
//			log.info("agent_1");
//			return Map.of("agent_1:prop1", "agent_1:test");
//		};
//
//		var workflow = new StateGraph(OverAllState::new).addNode("agent_1", node_async(agent_1))
//			.addEdge(START, "agent_1")
//			.addEdge("agent_1", END);
//
//		var saver = new MemorySaver();
//		SaverConfig saverConfig = SaverConfig.builder().register(SaverConstant.MEMORY, saver).build();
//
//		var compileConfig = CompileConfig.builder().saverConfig(saverConfig).build();
//
//		var runnableConfig = RunnableConfig.builder().build();
//		var app = workflow.compile(compileConfig);
//
//		Map<String, Object> inputs = Map.of("input", "test1");
//
//		var initState = app.cloneState(app.getInitialState(inputs, runnableConfig));
//
//		assertEquals(1, initState.data().size());
//		assertTrue(initState.value("input").isPresent());
//		assertEquals("test1", initState.value("input").get());
//
//		//
//		// Test checkpoint not override inputs
//		//
//		var newState = new AgentState(Map.of("input", "test2"));
//
//		saver.put(runnableConfig,
//				Checkpoint.builder().state(newState.data()).nodeId(START).nextNodeId("agent_1").build());
//
//		app = workflow.compile(compileConfig);
//		initState = app.cloneState(app.getInitialState(inputs, runnableConfig));
//
//		assertEquals(1, initState.data().size());
//		assertTrue(initState.value("input").isPresent());
//		assertEquals("test1", initState.value("input").get());
//
//		// Test checkpoints are saved
//		newState = new AgentState(Map.of("input", "test2", "agent_1:prop1", "agent_1:test"));
//		saver.put(runnableConfig, Checkpoint.builder().state(newState).nodeId("agent_1").nextNodeId(END).build());
//		app = workflow.compile(compileConfig);
//		initState = app.cloneState(app.getInitialState(inputs, runnableConfig));
//
//		assertEquals(2, initState.data().size());
//		assertTrue(initState.value("input").isPresent());
//		assertEquals("test1", initState.value("input").get());
//		assertTrue(initState.value("agent_1:prop1").isPresent());
//		assertEquals("agent_1:test", initState.value("agent_1:prop1").get());
//
//		var checkpoints = saver.list(runnableConfig);
//		assertEquals(2, checkpoints.size());
//		Optional<Checkpoint> last = saver.get(runnableConfig);
//		assertTrue(last.isPresent());
//		assertEquals("agent_1", last.get().getNodeId());
//		assertNotNull(last.get().getState().get("agent_1:prop1"));
//		assertEquals("agent_1:test", last.get().getState().get("agent_1:prop1"));
//
//	}
//
//	@Test
//	public void testCheckpointSaverResubmit() throws Exception {
//		int expectedSteps = 5;
//
//		NodeAction agent_1 = state -> {
//			Optional<Object> steps1 = state.value("steps");
//			log.info("agent_1: step: {}", steps1);
//			return Map.of("steps", steps1.get(), "messages", format("agent_1:step %d", steps));
//		};
//
//		EdgeAction shouldContinue = state -> {
//			Optional<Object> steps = state.value("steps");
//			int stepsInt = (int) steps.get();
//			if (stepsInt >= expectedSteps) {
//				return "exit";
//			}
//			return "next";
//		};
//
//		var workflow = new StateGraph<>(State.SCHEMA, State::new).addEdge(START, "agent_1")
//			.addNode("agent_1", node_async(agent_1))
//			.addConditionalEdges("agent_1", edge_async(shouldContinue), Map.of("next", "agent_1", "exit", END));
//		;
//
//		var saver = new MemorySaver();
//		SaverConfig saverConfig = SaverConfig.builder().register(SaverConstant.MEMORY, saver).build();
//
//		var compileConfig = CompileConfig.builder().saverConfig(saverConfig).build();
//
//		var app = workflow.compile(compileConfig);
//
//		Map<String, Object> inputs = Map.of("steps", 0);
//
//		var runnableConfig = RunnableConfig.builder().threadId("thread_1").build();
//
//		var state = app.invoke(inputs, runnableConfig);
//
//		assertTrue(state.isPresent());
//		assertEquals(expectedSteps, state.get().steps());
//
//		var messages = state.get().messages();
//		assertFalse(messages.isEmpty());
//
//		log.info("{}", messages);
//
//		assertEquals(expectedSteps, messages.size());
//		for (int i = 0; i < messages.size(); i++) {
//			assertEquals(format("agent_1:step %d", i + 1), messages.get(i));
//		}
//
//		var snapshot = app.getState(runnableConfig);
//
//		assertNotNull(snapshot);
//		log.info("SNAPSHOT:\n{}\n", snapshot);
//
//		// SUBMIT NEW THREAD 2
//		runnableConfig = RunnableConfig.builder().threadId("thread_2").build();
//
//		state = app.invoke(emptyMap(), runnableConfig);
//
//		assertTrue(state.isPresent());
//		assertEquals(expectedSteps, state.get().steps());
//		messages = state.get().messages();
//
//		log.info("{}", messages);
//
//		assertEquals(expectedSteps, messages.size());
//		for (int i = 0; i < messages.size(); i++) {
//			assertEquals(format("agent_1:step %d", i + 1), messages.get(i));
//		}
//
//		// RE-SUBMIT THREAD 1
//		state = app.invoke(emptyMap(), runnableConfig);
//
//		assertTrue(state.isPresent());
//		assertEquals(expectedSteps + 1, state.get().steps());
//		messages = state.get().messages();
//
//		log.info("{}", messages);
//
//		assertEquals(expectedSteps + 1, messages.size());
//
//	}
//
//	@Test
//	public void testViewAndUpdatePastGraphState() throws Exception {
//
//		var workflow = new StateGraph<>(State.SCHEMA, State::new).addNode("agent", node_async(agent_whether))
//			.addNode("tools", node_async(tool_whether))
//			.addEdge(START, "agent")
//			.addConditionalEdges("agent", edge_async(shouldContinue_whether), Map.of("tools", "tools", END, END))
//			.addEdge("tools", "agent");
//
//		var saver = new MemorySaver();
//		SaverConfig saverConfig = SaverConfig.builder().register(SaverConstant.MEMORY, saver).build();
//
//		var compileConfig = CompileConfig.builder().saverConfig(saverConfig).build();
//
//		var app = workflow.compile(compileConfig);
//
//		Map<String, Object> inputs = Map.of("messages", "whether in Naples?");
//
//		var runnableConfig = RunnableConfig.builder().threadId("thread_1").build();
//
//		var results = app.streamSnapshots(inputs, runnableConfig).stream().collect(Collectors.toList());
//
//		results
//			.forEach(r -> log.info("{}: Node: {} - {}", r.getClass().getSimpleName(), r.node(), r.state().messages()));
//
//		assertEquals(5, results.size());
//		assertInstanceOf(NodeOutput.class, results.get(0));
//		assertInstanceOf(StateSnapshot.class, results.get(1));
//		assertInstanceOf(StateSnapshot.class, results.get(2));
//		assertInstanceOf(StateSnapshot.class, results.get(3));
//		assertInstanceOf(NodeOutput.class, results.get(4));
//
//		var snapshot = app.getState(runnableConfig);
//		assertNotNull(snapshot);
//		assertEquals(END, snapshot.next());
//
//		log.info("LAST SNAPSHOT:\n{}\n", snapshot);
//
//		var stateHistory = app.getStateHistory(runnableConfig);
//		stateHistory.forEach(state -> log.info("SNAPSHOT HISTORY:\n{}\n", state));
//		assertNotNull(stateHistory);
//		assertEquals(4, stateHistory.size());
//
//		for (StateSnapshot<State> s : stateHistory) {
//			log.info("SNAPSHOT HISTORY:\n{}\n", s);
//		}
//
//		results = app.stream(null, runnableConfig).stream().collect(Collectors.toList());
//
//		assertNotNull(results);
//		assertFalse(results.isEmpty());
//		assertEquals(1, results.size());
//		assertTrue(results.get(0).state().lastMessage().isPresent());
//		assertEquals("whether in Naples is sunny", results.get(0).state().lastMessage().get());
//
//		Optional<StateSnapshot<State>> firstSnapshot = stateHistory.stream().reduce((first, second) -> second); // take
//																												// the
//																												// last
//		assertTrue(firstSnapshot.isPresent());
//		assertTrue(firstSnapshot.get().state().lastMessage().isPresent());
//		assertEquals("whether in Naples?", firstSnapshot.get().state().lastMessage().get());
//
//		var toReplay = firstSnapshot.get().config();
//
//		toReplay = app.updateState(toReplay, Map.of("messages", "i'm bartolo"));
//		results = app.stream(null, toReplay).stream().collect(Collectors.toList());
//
//		assertNotNull(results);
//		assertFalse(results.isEmpty());
//		assertEquals(2, results.size());
//		assertEquals(END, results.get(1).node());
//		assertTrue(results.get(1).state().lastMessage().isPresent());
//		assertEquals("Hi bartolo, nice to meet you too! How can I assist you today?",
//				results.get(0).state().lastMessage().get());
//
//	}
//
//	@Test
//	public void testPauseAndUpdatePastGraphState() throws Exception {
//
//		var workflow = new StateGraph<>(State.SCHEMA, State::new).addNode("agent", node_async(agent_whether))
//			.addNode("tools", node_async(tool_whether))
//			.addEdge(START, "agent")
//			.addConditionalEdges("agent", edge_async(shouldContinue_whether), Map.of("tools", "tools", END, END))
//			.addEdge("tools", "agent");
//
//		var saver = new MemorySaver();
//		SaverConfig saverConfig = SaverConfig.builder().register(SaverConstant.MEMORY, saver).build();
//
//		var compileConfig = CompileConfig.builder().saverConfig(saverConfig).interruptBefore("tools").build();
//
//		var app = workflow.compile(compileConfig);
//
//		var runnableConfig = RunnableConfig.builder().threadId("thread_1").build();
//
//		log.info("FIRST CALL WITH INTERRUPT BEFORE 'tools'");
//		Map<String, Object> inputs = Map.of("messages", "whether in Naples?");
//		var results = app.stream(inputs, runnableConfig)
//			.stream()
//			.peek(n -> log.info("{}", n))
//			.collect(Collectors.toList());
//		assertNotNull(results);
//		assertEquals(2, results.size());
//		assertEquals(START, results.get(0).node());
//		assertEquals("agent", results.get(1).node());
//		assertTrue(results.get(1).state().lastMessage().isPresent());
//
//		var state = app.getState(runnableConfig);
//
//		assertNotNull(state);
//		assertEquals("tools", state.next());
//
//		log.info("RESUME CALL");
//		results = app.stream(null, state.config()).stream().peek(n -> log.info("{}", n)).collect(Collectors.toList());
//
//		assertNotNull(results);
//		assertEquals(3, results.size());
//		assertEquals("tools", results.get(0).node());
//		assertEquals("agent", results.get(1).node());
//		assertEquals(END, results.get(2).node());
//		assertTrue(results.get(2).state().lastMessage().isPresent());
//		assertEquals("whether in Naples is sunny", results.get(2).state().lastMessage().get());
//
//	}
//
//}

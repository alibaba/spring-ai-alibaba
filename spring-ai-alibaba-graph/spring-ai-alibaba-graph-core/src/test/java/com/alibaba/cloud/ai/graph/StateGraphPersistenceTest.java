package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.AgentState;
import com.alibaba.cloud.ai.graph.state.AppenderChannel;
import com.alibaba.cloud.ai.graph.state.Channel;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.alibaba.cloud.ai.graph.utils.CollectionsUtils;
import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static com.alibaba.cloud.ai.graph.utils.CollectionsUtils.listOf;
import static com.alibaba.cloud.ai.graph.utils.CollectionsUtils.mapOf;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for simple App.
 */
@Slf4j
public class StateGraphPersistenceTest {

	static class MessagesState extends AgentState {

		static Map<String, Channel<?>> SCHEMA = CollectionsUtils.mapOf("messages",
				AppenderChannel.<String>of(ArrayList::new));

		public MessagesState(Map<String, Object> initData) {
			super(initData);
		}

		int steps() {
			return value("steps", 0);
		}

		List<String> messages() {
			return this.<List<String>>value("messages").orElseThrow(() -> new RuntimeException("messages not found"));
		}

		Optional<String> lastMessage() {
			List<String> messages = messages();
			if (messages.isEmpty()) {
				return Optional.empty();
			}
			return Optional.of(messages.get(messages.size() - 1));
		}

	}

	@Test
	public void testCheckpointInitialState() throws Exception {

		StateGraph<AgentState> workflow = new StateGraph<>(AgentState::new).addEdge(StateGraph.START, "agent_1")
			.addNode("agent_1", AsyncNodeAction.node_async(state -> {
				log.info("agent_1");
				return CollectionsUtils.mapOf("agent_1:prop1", "agent_1:test");
			}))
			.addEdge("agent_1", StateGraph.END);

		MemorySaver saver = new MemorySaver();
		SaverConfig saverConfig = SaverConfig.builder()
				.type(SaverConstant.MEMORY)
				.register(SaverConstant.MEMORY,saver)
				.build();
		CompileConfig compileConfig = CompileConfig.builder().saverConfig(saverConfig).build();

		RunnableConfig runnableConfig = RunnableConfig.builder().build();
		CompiledGraph<AgentState> app = workflow.compile(compileConfig);

		Map<String, Object> inputs = CollectionsUtils.mapOf("input", "test1");

		AgentState initState = app.cloneState(app.getInitialState(inputs, runnableConfig));

		assertEquals(1, initState.data().size());
		assertTrue(initState.value("input").isPresent());
		assertEquals("test1", initState.value("input").get());

		//
		// Test checkpoint not override inputs
		//
		AgentState newState = new AgentState(CollectionsUtils.mapOf("input", "test2"));

		saver.put(runnableConfig,
				Checkpoint.builder().state(newState.data()).nodeId(StateGraph.START).nextNodeId("agent_1").build());

		app = workflow.compile(compileConfig);
		initState = app.cloneState(app.getInitialState(inputs, runnableConfig));

		assertEquals(1, initState.data().size());
		assertTrue(initState.value("input").isPresent());
		assertEquals("test1", initState.value("input").get());

		// Test checkpoints are saved
		newState = new AgentState(CollectionsUtils.mapOf("input", "test2", "agent_1:prop1", "agent_1:test"));
		saver.put(runnableConfig,
				Checkpoint.builder().state(newState).nodeId("agent_1").nextNodeId(StateGraph.END).build());
		app = workflow.compile(compileConfig);
		initState = app.cloneState(app.getInitialState(inputs, runnableConfig));

		assertEquals(2, initState.data().size());
		assertTrue(initState.value("input").isPresent());
		assertEquals("test1", initState.value("input").get());
		assertTrue(initState.value("agent_1:prop1").isPresent());
		assertEquals("agent_1:test", initState.value("agent_1:prop1").get());

		java.util.Collection<Checkpoint> checkpoints = saver.list(runnableConfig);
		assertEquals(2, checkpoints.size());
		Optional<Checkpoint> last = saver.get(runnableConfig);
		assertTrue(last.isPresent());
		assertEquals("agent_1", last.get().getNodeId());
		assertNotNull(last.get().getState().get("agent_1:prop1"));
		assertEquals("agent_1:test", last.get().getState().get("agent_1:prop1"));

	}

	private StateGraph<MessagesState> workflow01(int expectedSteps) throws Exception {
		return new StateGraph<>(MessagesState.SCHEMA, MessagesState::new).addEdge(StateGraph.START, "agent_1")
			.addNode("agent_1", AsyncNodeAction.node_async(state -> {
				int steps = state.steps() + 1;
				log.info("agent_1: step: {}", steps);
				return CollectionsUtils.mapOf("steps", steps, "messages", format("agent_1:step %d", steps));
			}))
			.addConditionalEdges("agent_1", AsyncEdgeAction.edge_async(state -> {
				int steps = state.steps();
				if (steps >= expectedSteps) {
					return "exit";
				}
				return "next";
			}), CollectionsUtils.mapOf("next", "agent_1", "exit", StateGraph.END));
	}

	@Test
	public void testCheckpointSaverResubmit() throws Exception {
		int expectedSteps = 5;

		StateGraph<MessagesState> workflow = workflow01(expectedSteps);

		MemorySaver saver = new MemorySaver();
		SaverConfig saverConfig = SaverConfig.builder()
				.type(SaverConstant.MEMORY)
				.register(SaverConstant.MEMORY,saver)
				.build();

		CompileConfig compileConfig = CompileConfig.builder().saverConfig(saverConfig).build();

		CompiledGraph<MessagesState> app = workflow.compile(compileConfig);

		Map<String, Object> inputs = CollectionsUtils.mapOf("steps", 0);

		RunnableConfig runnableConfig = RunnableConfig.builder().threadId("thread_1").build();

		Optional<MessagesState> state = app.invoke(inputs, runnableConfig);

		assertTrue(state.isPresent());
		assertEquals(expectedSteps, state.get().steps());

		List<String> messages = state.get().messages();
		assertFalse(messages.isEmpty());

		log.info("{}", messages);

		assertEquals(expectedSteps, messages.size());
		for (int i = 0; i < messages.size(); i++) {
			assertEquals(format("agent_1:step %d", i + 1), messages.get(i));
		}

		StateSnapshot<MessagesState> snapshot = app.getState(runnableConfig);

		assertNotNull(snapshot);
		log.info("SNAPSHOT:\n{}\n", snapshot);

		// SUBMIT NEW THREAD 2
		runnableConfig = RunnableConfig.builder().threadId("thread_2").build();

		state = app.invoke(emptyMap(), runnableConfig);

		assertTrue(state.isPresent());
		assertEquals(expectedSteps, state.get().steps());
		messages = state.get().messages();

		log.info("{}", messages);

		assertEquals(expectedSteps, messages.size());
		for (int i = 0; i < messages.size(); i++) {
			assertEquals(format("agent_1:step %d", i + 1), messages.get(i));
		}

		// RE-SUBMIT THREAD 1
		state = app.invoke(emptyMap(), runnableConfig);

		assertTrue(state.isPresent());
		assertEquals(expectedSteps + 1, state.get().steps());
		messages = state.get().messages();

		log.info("{}", messages);

		assertEquals(expectedSteps + 1, messages.size());

	}

	@Test
	public void testViewAndUpdatePastGraphState() throws Exception {

		StateGraph<MessagesState> workflow = new StateGraph<>(MessagesState.SCHEMA, MessagesState::new)
			.addNode("agent", AsyncNodeAction.node_async(state -> {
				String lastMessage = state.lastMessage()
					.orElseThrow(() -> new IllegalStateException("No last message!"));

				if (lastMessage.contains("temperature")) {
					return CollectionsUtils.mapOf("messages", "whether in Naples is sunny");
				}
				if (lastMessage.contains("whether")) {
					return CollectionsUtils.mapOf("messages", "tool_calls");
				}
				if (lastMessage.contains("bartolo")) {
					return CollectionsUtils.mapOf("messages",
							"Hi bartolo, nice to meet you too! How can I assist you today?");
				}
				if (state.messages().stream().anyMatch(m -> m.contains("bartolo"))) {
					return CollectionsUtils.mapOf("messages", "Hi, bartolo welcome back?");
				}
				throw new IllegalStateException("unknown message!");
			}))
			.addNode("tools",
					AsyncNodeAction
						.node_async(state -> CollectionsUtils.mapOf("messages", "temperature in Napoli is 30 degree")))
			.addEdge(StateGraph.START, "agent")
			.addConditionalEdges("agent",
					AsyncEdgeAction.edge_async(state -> state.lastMessage()
						.filter(m -> m.equals("tool_calls"))
						.map(m -> "tools")
						.orElse(StateGraph.END)),
					CollectionsUtils.mapOf("tools", "tools", StateGraph.END, StateGraph.END))
			.addEdge("tools", "agent");

		MemorySaver saver = new MemorySaver();
		SaverConfig saverConfig = SaverConfig.builder()
				.type(SaverConstant.MEMORY)
				.register(SaverConstant.MEMORY, saver)
				.build();
		CompileConfig compileConfig = CompileConfig.builder().saverConfig(saverConfig).build();

		CompiledGraph<MessagesState> app = workflow.compile(compileConfig);

		Map<String, Object> inputs = CollectionsUtils.mapOf("messages", "whether in Naples?");

		RunnableConfig runnableConfig = RunnableConfig.builder().threadId("thread_1").build();

		List<NodeOutput<MessagesState>> results = app.streamSnapshots(inputs, runnableConfig)
			.stream()
			.collect(Collectors.toList());

		results
			.forEach(r -> log.info("{}: Node: {} - {}", r.getClass().getSimpleName(), r.node(), r.state().messages()));

		assertEquals(5, results.size());
		assertInstanceOf(NodeOutput.class, results.get(0));
		assertInstanceOf(StateSnapshot.class, results.get(1));
		assertInstanceOf(StateSnapshot.class, results.get(2));
		assertInstanceOf(StateSnapshot.class, results.get(3));
		assertInstanceOf(NodeOutput.class, results.get(4));

		StateSnapshot<MessagesState> snapshot = app.getState(runnableConfig);
		assertNotNull(snapshot);
		Assertions.assertEquals(StateGraph.END, snapshot.next());

		log.info("LAST SNAPSHOT:\n{}\n", snapshot);

		java.util.Collection<StateSnapshot<MessagesState>> stateHistory = app.getStateHistory(runnableConfig);
		stateHistory.forEach(state -> log.info("SNAPSHOT HISTORY:\n{}\n", state));
		assertNotNull(stateHistory);
		assertEquals(4, stateHistory.size());

		for (StateSnapshot<MessagesState> s : stateHistory) {
			log.info("SNAPSHOT HISTORY:\n{}\n", s);
		}

		results = app.stream(null, runnableConfig).stream().collect(Collectors.toList());

		assertNotNull(results);
		assertFalse(results.isEmpty());
		assertEquals(1, results.size());
		assertTrue(results.get(0).state().lastMessage().isPresent());
		assertEquals("whether in Naples is sunny", results.get(0).state().lastMessage().get());

		Optional<StateSnapshot<MessagesState>> firstSnapshot = stateHistory.stream().reduce((first, second) -> second); // take
																														// the
																														// last
		assertTrue(firstSnapshot.isPresent());
		assertTrue(firstSnapshot.get().state().lastMessage().isPresent());
		assertEquals("whether in Naples?", firstSnapshot.get().state().lastMessage().get());

		RunnableConfig toReplay = firstSnapshot.get().config();

		toReplay = app.updateState(toReplay, CollectionsUtils.mapOf("messages", "i'm bartolo"));
		results = app.stream(null, toReplay).stream().collect(Collectors.toList());

		assertNotNull(results);
		assertFalse(results.isEmpty());
		assertEquals(2, results.size());
		Assertions.assertEquals(StateGraph.END, results.get(1).node());
		assertTrue(results.get(1).state().lastMessage().isPresent());
		assertEquals("Hi bartolo, nice to meet you too! How can I assist you today?",
				results.get(0).state().lastMessage().get());

	}

	@Test
	public void testPauseAndUpdatePastGraphState() throws Exception {
		StateGraph<MessagesState> workflow = new StateGraph<>(MessagesState.SCHEMA, MessagesState::new)
			.addNode("agent", AsyncNodeAction.node_async(state -> {
				String lastMessage = state.lastMessage()
					.orElseThrow(() -> new IllegalStateException("No last message!"));

				if (lastMessage.contains("temperature")) {
					return CollectionsUtils.mapOf("messages", "whether in Naples is sunny");
				}
				if (lastMessage.contains("whether")) {
					return CollectionsUtils.mapOf("messages", "tool_calls");
				}
				if (lastMessage.contains("bartolo")) {
					return CollectionsUtils.mapOf("messages",
							"Hi bartolo, nice to meet you too! How can I assist you today?");
				}
				if (state.messages().stream().anyMatch(m -> m.contains("bartolo"))) {
					return CollectionsUtils.mapOf("messages", "Hi, bartolo welcome back?");
				}
				throw new IllegalStateException("unknown message!");
			}))
			.addNode("tools",
					AsyncNodeAction
						.node_async(state -> CollectionsUtils.mapOf("messages", "temperature in Napoli is 30 degree")))
			.addEdge(StateGraph.START, "agent")
			.addConditionalEdges("agent",
					AsyncEdgeAction.edge_async(state -> state.lastMessage()
						.filter(m -> m.equals("tool_calls"))
						.map(m -> "tools")
						.orElse(StateGraph.END)),
					CollectionsUtils.mapOf("tools", "tools", StateGraph.END, StateGraph.END))
			.addEdge("tools", "agent");

		MemorySaver saver = new MemorySaver();
		SaverConfig saverConfig = SaverConfig.builder()
				.type(SaverConstant.MEMORY)
				.register(SaverConstant.MEMORY,saver)
				.build();

		CompileConfig compileConfig = CompileConfig.builder().saverConfig(saverConfig).interruptBefore("tools").build();

		CompiledGraph<MessagesState> app = workflow.compile(compileConfig);

		RunnableConfig runnableConfig = RunnableConfig.builder().threadId("thread_1").build();

		Map<String, Object> inputs = CollectionsUtils.mapOf("messages", "whether in Naples?");
		List<NodeOutput<MessagesState>> results = app.stream(inputs, runnableConfig)
			.stream()
			.collect(Collectors.toList());
		results.forEach(System.out::println);
		assertNotNull(results);
		assertEquals(2, results.size());
		Assertions.assertEquals(StateGraph.START, results.get(0).node());
		assertEquals("agent", results.get(1).node());
		assertTrue(results.get(1).state().lastMessage().isPresent());

		StateSnapshot<MessagesState> state = app.getState(runnableConfig);

		assertNotNull(state);
		assertEquals("tools", state.next());

		results = app.stream(null, state.config()).stream().collect(Collectors.toList());

		assertNotNull(results);
		assertEquals(3, results.size());
		assertEquals("tools", results.get(0).node());
		assertEquals("agent", results.get(1).node());
		Assertions.assertEquals(StateGraph.END, results.get(2).node());
		assertTrue(results.get(2).state().lastMessage().isPresent());

		System.out.println(results.get(2).state().lastMessage().get());
	}

}

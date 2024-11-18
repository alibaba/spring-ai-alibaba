package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.action.llm.LLMNodeAction;
import com.alibaba.cloud.ai.graph.state.AgentState;
import com.alibaba.cloud.ai.graph.state.AppendableValue;
import com.alibaba.cloud.ai.graph.state.AppenderChannel;
import com.alibaba.cloud.ai.graph.state.Channel;
import com.alibaba.cloud.ai.graph.utils.CollectionsUtils;
import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.*;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.graph.action.llm.LLMNodeAction.MESSAGES_KEY;
import static com.alibaba.cloud.ai.graph.utils.CollectionsUtils.listOf;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for simple App.
 */
@Slf4j
public class StateGraphTest {

	public static <T> List<Map.Entry<String, T>> sortMap(Map<String, T> map) {
		return map.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList());
	}

	@Test
	void testValidation() throws Exception {

		StateGraph<AgentState> workflow = new StateGraph<>(AgentState::new);
		GraphStateException exception = assertThrows(GraphStateException.class, workflow::compile);
		System.out.println(exception.getMessage());
		assertEquals("missing Entry Point", exception.getMessage());

		workflow.addEdge(StateGraph.START, "agent_1");

		exception = assertThrows(GraphStateException.class, workflow::compile);
		System.out.println(exception.getMessage());
		assertEquals("entryPoint: agent_1 doesn't exist!", exception.getMessage());

		workflow.addNode("agent_1", AsyncNodeAction.node_async((state) -> {
			System.out.print("agent_1 ");
			System.out.println(state);
			return CollectionsUtils.mapOf("prop1", "test");
		}));

		assertNotNull(workflow.compile());

		workflow.addEdge("agent_1", StateGraph.END);

		assertNotNull(workflow.compile());

		exception = assertThrows(GraphStateException.class, () -> workflow.addEdge(StateGraph.END, "agent_1"));
		System.out.println(exception.getMessage());

		exception = assertThrows(GraphStateException.class, () -> workflow.addEdge("agent_1", "agent_2"));
		System.out.println(exception.getMessage());

		workflow.addNode("agent_2", AsyncNodeAction.node_async(state -> {

			System.out.print("agent_2: ");
			System.out.println(state);

			return CollectionsUtils.mapOf("prop2", "test");
		}));

		workflow.addEdge("agent_2", "agent_3");

		exception = assertThrows(GraphStateException.class, workflow::compile);
		System.out.println(exception.getMessage());

		exception = assertThrows(GraphStateException.class, () -> workflow.addConditionalEdges("agent_1",
				AsyncEdgeAction.edge_async(state -> "agent_3"), CollectionsUtils.mapOf()));
		System.out.println(exception.getMessage());

	}

	@Test
	public void testRunningOneNode() throws Exception {

		StateGraph<AgentState> workflow = new StateGraph<>(AgentState::new).addEdge(StateGraph.START, "agent_1")
			.addNode("agent_1", AsyncNodeAction.node_async(state -> {
				System.out.print("agent_1");
				System.out.println(state);
				return CollectionsUtils.mapOf("prop1", "test");
			}))
			.addEdge("agent_1", StateGraph.END);

		CompiledGraph<AgentState> app = workflow.compile();

		Optional<AgentState> result = app.invoke(CollectionsUtils.mapOf("input", "test1"));
		assertTrue(result.isPresent());

		Map<String, String> expected = CollectionsUtils.mapOf("input", "test1", "prop1", "test");

		assertIterableEquals(sortMap(expected), sortMap(result.get().data()));
		// assertDictionaryOfAnyEqual( expected, result.data )

	}

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

	}

	@Test
	void testWithAppender() throws Exception {

		StateGraph<MessagesState> workflow = new StateGraph<>(MessagesState.SCHEMA, MessagesState::new)
			.addNode("agent_1", AsyncNodeAction.node_async(state -> {
				System.out.println("agent_1");
				return CollectionsUtils.mapOf("messages", "message1");
			}))
			.addNode("agent_2", AsyncNodeAction.node_async(state -> {
				System.out.println("agent_2");
				return CollectionsUtils.mapOf("messages", new String[] { "message2" });
			}))
			.addNode("agent_3", AsyncNodeAction.node_async(state -> {
				System.out.println("agent_3");
				int steps = state.messages().size() + 1;
				return CollectionsUtils.mapOf("messages", "message3", "steps", steps);
			}))
			.addEdge("agent_1", "agent_2")
			.addEdge("agent_2", "agent_3")
			.addEdge(StateGraph.START, "agent_1")
			.addEdge("agent_3", StateGraph.END);

		CompiledGraph<MessagesState> app = workflow.compile();

		Optional<MessagesState> result = app.invoke(CollectionsUtils.mapOf());

		assertTrue(result.isPresent());
		System.out.println(result.get().data());
		assertEquals(3, result.get().steps());
		assertEquals(3, result.get().messages().size());
		assertIterableEquals(CollectionsUtils.listOf("message1", "message2", "message3"), result.get().messages());

	}

	static class MessagesStateDeprecated extends AgentState {

		public MessagesStateDeprecated(Map<String, Object> initData) {
			super(initData);
			appendableValue("messages"); // tip: initialize messages
		}

		int steps() {
			return value("steps").map(Integer.class::cast).orElse(0);
		}

		AppendableValue<String> messages() {
			return appendableValue("messages");
		}

	}

	@Test
	void testWithAppenderDeprecated() throws Exception {

		StateGraph<MessagesStateDeprecated> workflow = new StateGraph<>(MessagesStateDeprecated::new)
			.addNode("agent_1", AsyncNodeAction.node_async(state -> {
				System.out.println("agent_1");
				return CollectionsUtils.mapOf("messages", "message1");
			}))
			.addNode("agent_2", AsyncNodeAction.node_async(state -> {
				System.out.println("agent_2");
				return CollectionsUtils.mapOf("messages", "message2");
			}))
			.addNode("agent_3", AsyncNodeAction.node_async(state -> {
				System.out.println("agent_3");
				AppendableValue<String> messages = state.messages();
				int steps = messages.size() + 1;
				return CollectionsUtils.mapOf("messages", "message3", "steps", steps);
			}))
			.addEdge("agent_1", "agent_2")
			.addEdge("agent_2", "agent_3")
			.addEdge(StateGraph.START, "agent_1")
			.addEdge("agent_3", StateGraph.END);

		CompiledGraph<MessagesStateDeprecated> app = workflow.compile();

		Optional<MessagesStateDeprecated> result = app.invoke(CollectionsUtils.mapOf());

		assertTrue(result.isPresent());
		assertEquals(3, result.get().messages().size());
		assertEquals(3, result.get().steps());
		assertIterableEquals(CollectionsUtils.listOf("message1", "message2", "message3"),
				result.get().messages().values());

	}

	@Test
	void testWithLLMNodeAction() throws Exception {
		NodeAction<MessagesState> llmNode = LLMNodeAction.builder(new DashScopeChatModel(new DashScopeApi("${DASHSCOPE_API_KEY}")))
				.systemMessage("You're a code writer with strong language skills and coding skills")
				.build();
		Map<String,Object> stateData = llmNode.apply(new MessagesState(Map.of(MESSAGES_KEY, List.of(new UserMessage("can you provide a best practice using spring ai?")))));
		assertEquals(1, stateData.size());
		System.out.println(stateData);

	}


}

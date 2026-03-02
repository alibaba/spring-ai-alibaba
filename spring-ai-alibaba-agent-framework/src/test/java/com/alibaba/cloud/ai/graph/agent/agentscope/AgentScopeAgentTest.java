/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent.agentscope;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.Model;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration tests for {@link AgentScopeAgent}: standalone usage (call, invoke, stream)
 * and usage as a graph node via {@link AgentScopeAgent#asNode(boolean, boolean)}.
 */
class AgentScopeAgentTest {

	private static final String MOCK_RESPONSE_TEXT = "Hello from agent";

	private ReActAgent mockReActAgent;

	@BeforeEach
	void setUp() {
		mockReActAgent = mock(ReActAgent.class);
		when(mockReActAgent.getName()).thenReturn("testAgent");
		when(mockReActAgent.getDescription()).thenReturn("A test agent");
		Msg resultMsg = Msg.builder()
				.name("assistant")
				.role(MsgRole.ASSISTANT)
				.content(TextBlock.builder().text(MOCK_RESPONSE_TEXT).build())
				.build();
		Event agentResultEvent = new Event(EventType.AGENT_RESULT, resultMsg, true);
		when(mockReActAgent.stream(any(List.class), any(StreamOptions.class)))
				.thenReturn(Flux.just(agentResultEvent));
	}

	@Nested
	@DisplayName("Standalone usage")
	class StandaloneTests {

		@Test
		@DisplayName("invoke returns state with messages containing assistant response")
		void invoke_returnsStateWithMessages() throws GraphRunnerException {
			AgentScopeAgent agent = AgentScopeAgent.fromAgent(mockReActAgent)
					.name("testAgent")
					.build();

			Optional<OverAllState> result = agent.invoke("Hello");

			assertTrue(result.isPresent(), "Result should be present");
			OverAllState state = result.get();
			assertTrue(state.value("messages").isPresent(), "Messages should be present");
			@SuppressWarnings("unchecked")
			List<Object> messages = (List<Object>) state.value("messages").get();
			assertFalse(messages.isEmpty(), "Messages should not be empty");
			Object last = messages.get(messages.size() - 1);
			assertTrue(last instanceof AssistantMessage, "Last message should be AssistantMessage");
			assertEquals(MOCK_RESPONSE_TEXT, ((AssistantMessage) last).getText(), "Response text should match");
			verify(mockReActAgent).stream(any(List.class), any(StreamOptions.class));
		}

		@Test
		@DisplayName("call returns AssistantMessage with expected text")
		void call_returnsAssistantMessage() throws GraphRunnerException {
			AgentScopeAgent agent = AgentScopeAgent.fromAgent(mockReActAgent)
					.name("testAgent")
					.build();

			AssistantMessage message = agent.call("Hello");

			assertNotNull(message, "Message should not be null");
			assertEquals(MOCK_RESPONSE_TEXT, message.getText(), "Response text should match");
			verify(mockReActAgent).stream(any(List.class), any(StreamOptions.class));
		}

		@Test
		@DisplayName("call with List<Message> delegates to ReActAgent and returns assistant response")
		void callWithMessages_returnsAssistantMessage() throws GraphRunnerException {
			AgentScopeAgent agent = AgentScopeAgent.fromAgent(mockReActAgent)
					.name("testAgent")
					.build();

			AssistantMessage message = agent.call(List.of(new UserMessage("Hi")));

			assertNotNull(message, "Message should not be null");
			assertEquals(MOCK_RESPONSE_TEXT, message.getText(), "Response text should match");
			verify(mockReActAgent).stream(any(List.class), any(StreamOptions.class));
		}

		@Test
		@DisplayName("stream emits NodeOutput and completes")
		void stream_emitsAndCompletes() throws GraphRunnerException {
			AgentScopeAgent agent = AgentScopeAgent.fromAgent(mockReActAgent)
					.name("testAgent")
					.build();

			List<NodeOutput> collected = agent.stream(new UserMessage("Hello"))
					.collectList()
					.block();

			assertNotNull(collected, "Collected list should not be null");
			assertFalse(collected.isEmpty(), "Stream should emit at least one output");
			Optional<NodeOutput> last = collected.stream().reduce((a, b) -> b);
			assertTrue(last.isPresent(), "Should have last output");
			last.ifPresent(out -> {
				if (out instanceof StreamingOutput<?> so) {
					assertEquals("testAgent", so.agent(), "Agent name should match");
				}
			});
			verify(mockReActAgent).stream(any(List.class), any(StreamOptions.class));
		}
	}

	@Nested
	@DisplayName("As graph node")
	class AsNodeTests {

		@Test
		@DisplayName("asNode returns non-null node with correct id")
		void asNode_returnsNodeWithCorrectId() throws GraphStateException {
			AgentScopeAgent agent = AgentScopeAgent.fromAgent(mockReActAgent)
					.name("subAgent")
					.build();

			var node = agent.asNode(true, false);

			assertNotNull(node, "asNode should return non-null Node");
			assertEquals("subAgent", node.id(), "Node id should match agent name");
		}

		@Test
		@DisplayName("agent as subgraph node in parent graph runs and returns result")
		void agentAsNode_inParentGraph_returnsResult() throws GraphStateException, GraphRunnerException {
			AgentScopeAgent agent = AgentScopeAgent.fromAgent(mockReActAgent)
					.name("subAgent")
					.build();

			// Build a parent graph: START -> subAgent (as node) -> END
			StateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
			Map<String, com.alibaba.cloud.ai.graph.KeyStrategy> keyMap = new HashMap<>();
			keyMap.put("messages", new AppendStrategy());
			keyMap.put("output", new com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy());
			StateGraph graph = new StateGraph("parent", () -> keyMap, serializer);

			CompiledGraph agentCompiled = agent.getAndCompileGraph();
			graph.addNode(agent.name(), agentCompiled);
			graph.addEdge(START, agent.name());
			graph.addEdge(agent.name(), END);

			CompiledGraph parentCompiled = graph.compile();
			Map<String, Object> inputs = Map.of(
					"messages", List.of(new UserMessage("Hello from parent"))
			);
			Optional<OverAllState> result = parentCompiled.invoke(inputs, RunnableConfig.builder().build());

			assertTrue(result.isPresent(), "Parent graph result should be present");
			assertTrue(result.get().value("messages").isPresent(), "Messages should be present");
			@SuppressWarnings("unchecked")
			List<Object> messages = (List<Object>) result.get().value("messages").get();
			assertFalse(messages.isEmpty(), "Messages should not be empty");
			Object last = messages.get(messages.size() - 1);
			assertTrue(last instanceof AssistantMessage, "Last message should be AssistantMessage");
			assertEquals(MOCK_RESPONSE_TEXT, ((AssistantMessage) last).getText(), "Response text should match");
			verify(mockReActAgent).stream(any(List.class), any(StreamOptions.class));
		}

		@Test
		@DisplayName("agent as node streams GraphResponse and completes with done")
		void agentAsNode_streamCompletesWithDone() throws GraphStateException {
			AgentScopeAgent agent = AgentScopeAgent.fromAgent(mockReActAgent)
					.name("subAgent")
					.build();

			StateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
			Map<String, com.alibaba.cloud.ai.graph.KeyStrategy> keyMap = new HashMap<>();
			keyMap.put("messages", new AppendStrategy());
			StateGraph graph = new StateGraph("parent", () -> keyMap, serializer);
			graph.addNode(agent.name(), agent.getAndCompileGraph());
			graph.addEdge(START, agent.name());
			graph.addEdge(agent.name(), END);

			CompiledGraph parentCompiled = graph.compile();
			Map<String, Object> inputs = Map.of("messages", List.of(new UserMessage("Hi")));
			Flux<GraphResponse<NodeOutput>> responseFlux = parentCompiled.graphResponseStream(inputs, RunnableConfig.builder().build());

			List<GraphResponse<NodeOutput>> responses = responseFlux.collectList().block();
			assertNotNull(responses, "Responses should not be null");
			assertFalse(responses.isEmpty(), "Should have at least one response");
			Optional<GraphResponse<NodeOutput>> done = responses.stream()
					.filter(GraphResponse::isDone)
					.reduce((a, b) -> b);
			assertTrue(done.isPresent(), "Stream should complete with done response");
			assertTrue(done.get().resultValue().isPresent(), "Done should carry result");
		}
	}

	/**
	 * Integration example: SequentialAgent with ReactAgent (Spring AI Alibaba) and
	 * AgentScopeAgent in sequence. Runs only when AI_DASHSCOPE_API_KEY is set.
	 */
	@Nested
	@DisplayName("SequentialAgent integration example")
	@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
	class SequentialIntegrationTests {

		private ChatModel chatModel;

		@BeforeEach
		void setUpSequential() {
			DashScopeApi dashScopeApi = DashScopeApi.builder()
					.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
					.build();
			this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
		}

		@Test
		@DisplayName("SequentialAgent with ReactAgent then AgentScopeAgent compiles, runs and both outputs present")
		void sequentialReactThenAgentScope_compilesAndBothOutputsPresent() throws Exception {
			ReactAgent firstAgent = ReactAgent.builder()
					.name("first_agent")
					.model(chatModel)
					.description("第一步：根据用户输入做简要分析")
					.instruction("你是一名分析员，请对用户输入做一句话简要分析。用户输入： {input}")
					.outputKey("first_output")
					.build();

			// Real ReActAgent (AgentScope) with DashScope model
			Model scopeModel = io.agentscope.core.model.DashScopeChatModel.builder()
					.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
					.modelName("qwen-plus")
					.build();
			ReActAgent scopeReAct = ReActAgent.builder()
					.name("second_agent")
					.description("第二步：基于前序结果做补充结论")
					.sysPrompt("你基于前序分析做简短补充结论，一两句话即可。")
					.model(scopeModel)
					.memory(new InMemoryMemory())
					.build();

			AgentScopeAgent secondAgent = AgentScopeAgent.fromAgent(scopeReAct)
					.name("second_agent")
					.description("第二步：基于前序结果做补充结论")
					.instruction("你基于前序分析做补充结论。前序结果： {first_output}。")
					.outputKey("second_output")
					.build();

			SequentialAgent sequentialAgent = SequentialAgent.builder()
					.name("two_step_workflow")
					.description("顺序流程：先由 ReactAgent 分析，再由 AgentScopeAgent 补充结论")
					.subAgents(List.of(firstAgent, secondAgent))
					.build();

			GraphRepresentation representation = sequentialAgent.getGraph()
					.getGraph(GraphRepresentation.Type.PLANTUML);
			assertNotNull(representation, "Graph representation should not be null");
			assertNotNull(representation.content(), "Graph representation content should not be null");

			String content = representation.content();
			assertTrue(content.contains("two_step_workflow"), "Graph should contain sequential workflow");
			assertTrue(content.contains("first_agent"), "Graph should contain first agent (ReactAgent)");
			assertTrue(content.contains("second_agent"), "Graph should contain second agent (AgentScopeAgent)");

			Optional<OverAllState> result = sequentialAgent.invoke("请分析一下当前大模型的发展趋势。");
			assertTrue(result.isPresent(), "Sequential invoke result should be present");
			OverAllState state = result.get();

			assertTrue(state.value("first_output").isPresent(), "first_output should be present after first agent");
			Object firstOut = state.value("first_output").get();
			AssistantMessage firstContent = firstOut instanceof List
					? (AssistantMessage) ((List<?>) firstOut).get(0)
					: (AssistantMessage) firstOut;
			assertNotNull(firstContent.getText(), "First agent output should not be null");

			assertTrue(state.value("second_output").isPresent(), "second_output should be present after second agent (AgentScope)");
			Object secondOut = state.value("second_output").get();
			AssistantMessage secondContent = secondOut instanceof List
					? (AssistantMessage) ((List<?>) secondOut).get(0)
					: (AssistantMessage) secondOut;
			assertNotNull(secondContent.getText(), "Second agent output should not be null");
			assertFalse(secondContent.getText().isBlank(), "Second output from real ReActAgent should be non-empty");

			// Test sequentialAgent.stream(): should emit NodeOutputs and final state should contain both outputs
			List<NodeOutput> streamOutputs = sequentialAgent.stream("请简要分析大模型的应用场景。")
					.collectList()
					.block();
			assertNotNull(streamOutputs, "Stream outputs should not be null");
			assertFalse(streamOutputs.isEmpty(), "Stream should emit at least one NodeOutput");
			Optional<NodeOutput> lastStreamOutput = streamOutputs.stream().reduce((a, b) -> b);
			assertTrue(lastStreamOutput.isPresent(), "Stream should have a last output");
			lastStreamOutput.ifPresent(out -> {
				assertNotNull(out.state(), "Last stream output should carry state");
				assertTrue(out.state().value("first_output").isPresent(), "Stream final state should contain first_output");
				assertTrue(out.state().value("second_output").isPresent(), "Stream final state should contain second_output");
			});
		}
	}
}

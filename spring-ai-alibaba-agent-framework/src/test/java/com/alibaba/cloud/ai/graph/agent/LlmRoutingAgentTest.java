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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static com.alibaba.cloud.ai.graph.agent.tools.PoetTool.createPoetToolCallback;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class LlmRoutingAgentTest {

	private ChatModel chatModel;

	@BeforeEach
	void setUp() {
		// Create DashScopeApi instance using the API key from environment variable
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();

		// Create DashScope ChatModel instance
		this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
	}

	@Test
	public void testLlmRoutingAgent() throws Exception {
		ReactAgent proseWriterAgent = ReactAgent.builder()
			.name("prose_writer_agent")
			.model(chatModel)
			.description("Can write prose articles.")
			.instruction("You are a renowned writer skilled in writing prose. Please respond to user questions.")
			.outputKey("prose_article")
			.build();

		ReactAgent poemWriterAgent = ReactAgent.builder()
			.name("poem_writer_agent")
			.model(chatModel)
			.description("Can write modern poetry.")
			.instruction("You are a famous poet skilled in modern poetry. Please use tools to respond to user questions.")
			.outputKey("poem_article")
			.tools(List.of(createPoetToolCallback()))
			.build();

		LlmRoutingAgent blogAgent = LlmRoutingAgent.builder()
			.name("blog_agent")
			.model(chatModel)
			.description("Can write articles or poems based on user-provided topics.")
			.subAgents(List.of(proseWriterAgent, poemWriterAgent))
			.build();

		try {

			GraphRepresentation representation = blogAgent.getGraph().getGraph(GraphRepresentation.Type.PLANTUML);
			System.out.println(representation.content());

			Optional<OverAllState> result = blogAgent.invoke("帮我写一个100字左右的现代诗");
			blogAgent.invoke("帮我写一个100字左右的现代诗");
			Optional<OverAllState> result3 = blogAgent.invoke("帮我写一个100字左右的现代诗");

			// 验证结果不为空
			assertTrue(result.isPresent(), "Result should be present");
			assertTrue(result3.isPresent(), "Third result should be present");

			OverAllState state = result.get();
			OverAllState state3 = result3.get();

			assertTrue(state.value("input").isPresent(), "Input should be present in state");
			assertEquals("帮我写一个100字左右的现代诗", state.value("input").get(), "Input should match the request");

			assertTrue(state.value("poem_article").isPresent(), "Poem article should be present");
			AssistantMessage poemContent = (AssistantMessage) state.value("poem_article").get();
			assertNotNull(poemContent.getText(), "Poem content should not be null");

			assertTrue(state3.value("poem_article").isPresent(), "Poem article should be present");
			AssistantMessage poemContent3 = (AssistantMessage) state3.value("poem_article").get();
			assertNotNull(poemContent3.getText(), "Poem content should not be null");

			System.out.println(result.get());
			System.out.println("------------------");
			System.out.println(result3.get());
		}
		catch (java.util.concurrent.CompletionException e) {
			e.printStackTrace();
			fail("LlmRoutingAgent execution failed: " + e.getMessage());
		}

		// Verify all hooks were executed
	}

	/**
	 * Test using MessageTrimmingHook directly at LlmRoutingAgent level.
	 * This trims memory before routing decisions, reducing token consumption for the LlmRoutingAgent itself.
	 */
	@Test
	public void testLlmRoutingAgentWithHooksAtRoutingLevel() throws Exception {
		// Create trackable MessageTrimmingHook, keeping only the last 3 messages (for routing decisions)
		TrackableMessageTrimmingHook routingLevelHook = new TrackableMessageTrimmingHook(3);

		// Create regular sub-agents (without hooks)
		ReactAgent proseWriterAgent = ReactAgent.builder()
			.name("prose_writer_agent")
			.model(chatModel)
			.description("Can write prose articles.")
			.instruction("You are a renowned writer skilled in writing prose. Please respond to user questions.")
			.saver(new MemorySaver())
			.outputKey("prose_article")
			.build();

		ReactAgent poemWriterAgent = ReactAgent.builder()
			.name("poem_writer_agent")
			.model(chatModel)
			.description("Can write modern poetry.")
			.instruction("You are a famous poet skilled in modern poetry. Please respond to user questions.")
			.saver(new MemorySaver())
			.outputKey("poem_article")
			.build();

		// Configure hook at LlmRoutingAgent level
		LlmRoutingAgent blogAgent = LlmRoutingAgent.builder()
			.name("blog_agent")
			.model(chatModel)
			.description("Can write articles or poems based on user-provided topics.")
			.subAgents(List.of(proseWriterAgent, poemWriterAgent))
			.hooks(routingLevelHook)  // Configure hook at LlmRoutingAgent level
			.saver(new MemorySaver())
			.build();

		try {
			// Use the same threadId for multiple calls
			RunnableConfig config = RunnableConfig.builder().threadId("test-routing-hook").build();

			// First call
			routingLevelHook.reset();
			Optional<OverAllState> result1 = blogAgent.invoke("Write a prose about spring", config);
			assertTrue(result1.isPresent(), "First result should be present");
			System.out.println("First call completed");
			
			// Verify first call: hook should be called once, input message count=1, no trimming needed
			assertEquals(1, routingLevelHook.getCallCount(), "Hook should be called once in first call");
			assertEquals(1, routingLevelHook.getLastInputMessageCount(), "First call has only 1 user message");
			assertEquals(1, routingLevelHook.getLastOutputMessageCount(), "First call message count <= 3, no trimming needed");
			System.out.println("Verification passed: First call messages not trimmed (1 message)");
			System.out.println("------------------");

			// Second call (same threadId, history now contains: user1, assistant1, user2 = 3 messages)
			routingLevelHook.reset();
			Optional<OverAllState> result2 = blogAgent.invoke("Now write a poem about summer", config);
			assertTrue(result2.isPresent(), "Second result should be present");
			System.out.println("Second call completed");
			
			// Verify second call: hook should be called once, input ~3 messages (user1+assistant1+user2)
			assertEquals(1, routingLevelHook.getCallCount(), "Hook should be called once in second call");
			assertTrue(routingLevelHook.getLastInputMessageCount() >= 3, 
				"Second call should have at least 3 messages (including history), actual: " + routingLevelHook.getLastInputMessageCount());
			assertEquals(3, routingLevelHook.getLastOutputMessageCount(), 
				"Second call should trim to 3 messages, actual: " + routingLevelHook.getLastOutputMessageCount());
			System.out.println("Verification passed: Second call messages trimmed (from " + routingLevelHook.getLastInputMessageCount() + " to 3 messages)");
			System.out.println("------------------");

			// Third call (verify continuous trimming with more history)
			routingLevelHook.reset();
			Optional<OverAllState> result3 = blogAgent.invoke("Write another poem about autumn", config);
			assertTrue(result3.isPresent(), "Third result should be present");
			System.out.println("Third call completed");
			
			// Verify third call: should continue trimming to 3 messages
			assertEquals(1, routingLevelHook.getCallCount(), "Hook should be called once in third call");
			assertTrue(routingLevelHook.getLastInputMessageCount() > 3, 
				"Third call should have more than 3 history messages, actual: " + routingLevelHook.getLastInputMessageCount());
			assertEquals(3, routingLevelHook.getLastOutputMessageCount(), 
				"Third call should trim to 3 messages, actual: " + routingLevelHook.getLastOutputMessageCount());
			System.out.println("Verification passed: Third call messages trimmed (from " + routingLevelHook.getLastInputMessageCount() + " to 3 messages)");
			System.out.println("------------------");

			// Verify results
			System.out.println("Test successful: LlmRoutingAgent level hooks work correctly!");
			System.out.println("- Hook executes before routing decision and trims memory");
			System.out.println("- Reduces token consumption for LlmRoutingAgent");
			System.out.println("- Prevents unlimited memory growth");
		}
		catch (Exception e) {
			e.printStackTrace();
			fail("LlmRoutingAgent with hooks at routing level failed: " + e.getMessage());
		}
	}

	/**
	 * Trackable MessageTrimmingHook for test verification.
	 * Records the input and output message counts for each call.
	 */
	@HookPositions({HookPosition.BEFORE_MODEL})
	static class TrackableMessageTrimmingHook extends MessagesModelHook {
		private final int maxMessages;
		private int callCount = 0;
		private int lastInputMessageCount = 0;
		private int lastOutputMessageCount = 0;

		public TrackableMessageTrimmingHook(int maxMessages) {
			this.maxMessages = maxMessages;
		}

		@Override
		public String getName() {
			return "trackable_message_trimming_hook";
		}

		@Override
		public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
			callCount++;
			lastInputMessageCount = previousMessages.size();

			if (previousMessages.size() <= maxMessages) {
				// If message count is within limit, no trimming needed
				lastOutputMessageCount = previousMessages.size();
				System.out.println("TrackableMessageTrimmingHook [Call #" + callCount + "]: Message count=" 
					+ lastInputMessageCount + ", no trimming needed");
				return new AgentCommand(previousMessages);
			}

			// Keep only the last maxMessages messages
			List<Message> trimmedMessages = previousMessages.subList(
				previousMessages.size() - maxMessages,
				previousMessages.size()
			);
			lastOutputMessageCount = trimmedMessages.size();

			System.out.println("TrackableMessageTrimmingHook [Call #" + callCount + "]: Trimmed messages from " 
				+ lastInputMessageCount + " to " + lastOutputMessageCount);

			// Use REPLACE policy to replace all messages
			return new AgentCommand(new ArrayList<>(trimmedMessages), UpdatePolicy.REPLACE);
		}

		// Test helper methods
		public int getCallCount() {
			return callCount;
		}

		public int getLastInputMessageCount() {
			return lastInputMessageCount;
		}

		public int getLastOutputMessageCount() {
			return lastOutputMessageCount;
		}

		public void reset() {
			callCount = 0;
			lastInputMessageCount = 0;
			lastOutputMessageCount = 0;
		}
	}

}

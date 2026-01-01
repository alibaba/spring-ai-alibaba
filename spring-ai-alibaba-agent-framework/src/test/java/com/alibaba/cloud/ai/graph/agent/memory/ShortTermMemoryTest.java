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
package com.alibaba.cloud.ai.graph.agent.memory;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.tools.WeatherTool;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for ReactAgent short-term memory functionality using MemorySaver and threadId
 *
 * Short-term memory allows the agent to remember previous interactions within a single thread/session.
 * This test demonstrates:
 * 1. Basic memory retention across multiple calls with same threadId
 * 2. Memory isolation between different threads
 * 3. Message trimming to manage context window
 * 4. Message deletion for memory cleanup
 */
@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class ShortTermMemoryTest {

	private ChatModel chatModel;
	private ToolCallback weatherTool;

	@BeforeEach
	void setUp() {
		// Create DashScopeApi instance using the API key from environment variable
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		// Create DashScope ChatModel instance
		this.chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// Create weather tool
		this.weatherTool = WeatherTool.createWeatherTool("getWeather", new WeatherTool());
	}

	/**
	 * Test 1: Basic short-term memory - agent remembers user's name across multiple interactions
	 */
	@Test
	void testBasicShortTermMemory() throws Exception {
		// Create agent with MemorySaver
		ReactAgent agent = ReactAgent.builder()
				.name("memory_agent")
				.model(chatModel)
				.saver(new MemorySaver())
				.build();

		// Create config with threadId to maintain conversation context
		RunnableConfig config = RunnableConfig.builder()
				.threadId("conversation_1")
				.build();

		// First interaction: introduce name
		Optional<OverAllState> result1 = agent.invoke("你好！我叫张三。", config);
		assertTrue(result1.isPresent(), "First response should be present");

		AssistantMessage response1 = (AssistantMessage) result1.get().value("messages")
				.map(m -> ((List<Message>) m).get(((List<Message>) m).size() - 1))
				.orElseThrow();
		System.out.println("Response 1: " + response1.getText());
		assertTrue(response1.getText().contains("张三") || response1.getText().contains("你好"),
				"Agent should acknowledge the greeting");

		// Second interaction: ask for name
		Optional<OverAllState> result2 = agent.invoke("我叫什么名字？", config);
		assertTrue(result2.isPresent(), "Second response should be present");

		AssistantMessage response2 = (AssistantMessage) result2.get().value("messages")
				.map(m -> ((List<Message>) m).get(((List<Message>) m).size() - 1))
				.orElseThrow();
		System.out.println("Response 2: " + response2.getText());
		assertTrue(response2.getText().contains("张三"),
				"Agent should remember the name from previous interaction");
	}

	/**
	 * Test 2: Memory isolation - different threads maintain separate contexts
	 */
	@Test
	void testMemoryIsolationBetweenThreads() throws Exception {
		ReactAgent agent = ReactAgent.builder()
				.name("memory_agent")
				.model(chatModel)
				.saver(new MemorySaver())
				.build();

		// Thread 1: User named Alice
		RunnableConfig config1 = RunnableConfig.builder()
				.threadId("thread_alice")
				.build();

		agent.invoke("你好，我叫 Alice。", config1);

		// Thread 2: User named Bob
		RunnableConfig config2 = RunnableConfig.builder()
				.threadId("thread_bob")
				.build();

		agent.invoke("你好，我叫 Bob。", config2);

		// Ask for name in thread 1 - should be Alice
		Optional<OverAllState> result1 = agent.invoke("我叫什么名字？", config1);
		AssistantMessage response1 = (AssistantMessage) result1.get().value("messages")
				.map(m -> ((List<Message>) m).get(((List<Message>) m).size() - 1))
				.orElseThrow();
		System.out.println("Thread 1 response: " + response1.getText());
		assertTrue(response1.getText().contains("Alice"),
				"Thread 1 should remember Alice");

		// Ask for name in thread 2 - should be Bob
		Optional<OverAllState> result2 = agent.invoke("我叫什么名字？", config2);
		AssistantMessage response2 = (AssistantMessage) result2.get().value("messages")
				.map(m -> ((List<Message>) m).get(((List<Message>) m).size() - 1))
				.orElseThrow();
		System.out.println("Thread 2 response: " + response2.getText());
		assertTrue(response2.getText().contains("Bob"),
				"Thread 2 should remember Bob");
	}

	/**
	 * Test 3: Message trimming - keep context window manageable
	 */
	@Test
	void testMessageTrimming() throws Exception {
		// Create a hook that trims messages to keep only the first and last 3 messages
		ModelHook messageTrimmingHook = new ModelHook() {
			private static final int MAX_MESSAGES = 5;

			@Override
			public String getName() {
				return "message_trimming";
			}

			@Override
			public List<JumpTo> canJumpTo() {
				return List.of();
			}

			@Override
			public HookPosition[] getHookPositions() {
				return new HookPosition[]{HookPosition.BEFORE_MODEL};
			}

			@Override
			public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
				Optional<Object> messagesOpt = state.value("messages");
				if (messagesOpt.isEmpty()) {
					return CompletableFuture.completedFuture(Map.of());
				}

				List<Message> messages = (List<Message>) messagesOpt.get();
				System.out.println("Current message count: " + messages.size());

				if (messages.size() <= MAX_MESSAGES) {
					return CompletableFuture.completedFuture(Map.of()); // No need to trim
				}

				// Keep the first message (system message) and the last few messages
				Message firstMsg = messages.get(0);
				int keepCount = 4; // Keep last 4 messages
				List<Message> recentMessages = messages.subList(
						messages.size() - keepCount,
						messages.size()
				);

				List<Message> newMessages = new ArrayList<>();
				newMessages.add(firstMsg);
				newMessages.addAll(recentMessages);

				System.out.println("Trimmed message count: " + newMessages.size());
				return CompletableFuture.completedFuture(Map.of("messages", newMessages));
			}

			@Override
			public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
				return CompletableFuture.completedFuture(Map.of());
			}
		};

		ReactAgent agent = ReactAgent.builder()
				.name("trimming_agent")
				.model(chatModel)
				.hooks(messageTrimmingHook)
				.saver(new MemorySaver())
				.build();

		RunnableConfig config = RunnableConfig.builder()
				.threadId("trimming_thread")
				.build();

		// Have multiple interactions
		agent.invoke("你好，我叫 Bob", config);
		agent.invoke("我喜欢猫", config);
		agent.invoke("我也喜欢狗", config);
		agent.invoke("给我讲个笑话", config);

		// After multiple interactions, the message count should be trimmed
		Optional<OverAllState> result = agent.invoke("我叫什么名字？", config);
		AssistantMessage response = (AssistantMessage) result.get().value("messages")
				.map(m -> ((List<Message>) m).get(((List<Message>) m).size() - 1))
				.orElseThrow();

		System.out.println("Response after trimming: " + response.getText());
		// Note: Due to trimming, the agent might not remember the name
		// This demonstrates the trade-off between memory and context window size
	}

	/**
	 * Test 4: Message deletion - clear old messages
	 */
	@Test
	void testMessageDeletion() throws Exception {
		// Create a hook that deletes old messages after model response
		ModelHook messageDeletionHook = new ModelHook() {
			@Override
			public String getName() {
				return "message_deletion";
			}

			@Override
			public List<JumpTo> canJumpTo() {
				return List.of();
			}

			@Override
			public HookPosition[] getHookPositions() {
				return new HookPosition[]{HookPosition.AFTER_MODEL};
			}

			@Override
			public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
				return CompletableFuture.completedFuture(Map.of());
			}

			@Override
			public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
				Optional<Object> messagesOpt = state.value("messages");
				if (messagesOpt.isEmpty()) {
					return CompletableFuture.completedFuture(Map.of());
				}

				List<Message> messages = (List<Message>) messagesOpt.get();
				System.out.println("Message count before deletion: " + messages.size());

				if (messages.size() > 4) {
					// Keep only the last 4 messages
					List<Message> recentMessages = messages.subList(
							messages.size() - 4,
							messages.size()
					);
					System.out.println("Message count after deletion: " + recentMessages.size());
					return CompletableFuture.completedFuture(Map.of("messages", recentMessages));
				}

				return CompletableFuture.completedFuture(Map.of());
			}
		};

		ReactAgent agent = ReactAgent.builder()
				.name("deletion_agent")
				.model(chatModel)
				.hooks(messageDeletionHook)
				.saver(new MemorySaver())
				.build();

		RunnableConfig config = RunnableConfig.builder()
				.threadId("deletion_thread")
				.build();

		// Have multiple interactions
		agent.invoke("记住：密码是 12345", config);
		agent.invoke("再记住：我的邮箱是 test@example.com", config);
		agent.invoke("今天天气怎么样？", config);

		// After deletion hook, old messages should be removed
		Optional<OverAllState> result = agent.invoke("密码是什么？", config);
		AssistantMessage response = (AssistantMessage) result.get().value("messages")
				.map(m -> ((List<Message>) m).get(((List<Message>) m).size() - 1))
				.orElseThrow();

		System.out.println("Response after deletion: " + response.getText());
		// Due to message deletion, the agent should not remember the password
		assertFalse(response.getText().contains("12345"),
				"Old messages should be deleted");
	}

	/**
	 * Test 5: Memory with tools - agent remembers context while using tools
	 */
	@Test
	void testMemoryWithTools() throws Exception {
		ReactAgent agent = ReactAgent.builder()
				.name("tool_memory_agent")
				.model(chatModel)
				.tools(weatherTool)
				.saver(new MemorySaver())
				.build();

		RunnableConfig config = RunnableConfig.builder()
				.threadId("tool_thread")
				.build();

		// First interaction: user provides their location
		agent.invoke("我住在北京。", config);

		// Second interaction: ask about weather (agent should remember location)
		Optional<OverAllState> result = agent.invoke("我这里的天气怎么样？", config);
		assertTrue(result.isPresent(), "Response should be present");

		AssistantMessage response = (AssistantMessage) result.get().value("messages")
				.map(m -> ((List<Message>) m).get(((List<Message>) m).size() - 1))
				.orElseThrow();

		System.out.println("Weather response: " + response.getText());
		// Agent should use the weather tool and remember the user is in Beijing
	}

	/**
	 * Test 6: Resume conversation from checkpoint
	 */
	@Test
	void testResumeConversation() throws Exception {
		MemorySaver saver = new MemorySaver();
		String threadId = "resume_thread";

		// Create first agent instance
		ReactAgent agent1 = ReactAgent.builder()
				.name("resume_agent")
				.model(chatModel)
				.saver(saver)
				.build();

		RunnableConfig config = RunnableConfig.builder()
				.threadId(threadId)
				.build();

		// Have a conversation
		agent1.invoke("你好，我叫李明，今年25岁。", config);

		// Create a new agent instance with the same saver and threadId
		ReactAgent agent2 = ReactAgent.builder()
				.name("resume_agent")
				.model(chatModel)
				.saver(saver)
				.build();

		// Continue the conversation with new agent instance
		Optional<OverAllState> result = agent2.invoke("我叫什么名字？多大了？", config);
		AssistantMessage response = (AssistantMessage) result.get().value("messages")
				.map(m -> ((List<Message>) m).get(((List<Message>) m).size() - 1))
				.orElseThrow();

		System.out.println("Resumed conversation response: " + response.getText());
		assertTrue(response.getText().contains("李明") || response.getText().contains("25"),
				"Agent should remember information from previous conversation");
	}
}

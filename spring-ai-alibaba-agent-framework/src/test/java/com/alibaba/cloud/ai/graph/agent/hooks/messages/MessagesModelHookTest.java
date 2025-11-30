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
package com.alibaba.cloud.ai.graph.agent.hooks.messages;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AppendPolicy;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
public class MessagesModelHookTest {

	private ChatModel chatModel;

	@BeforeEach
	void setUp() {
		// Create DashScopeApi instance using the API key from environment variable
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();

		// Create DashScope ChatModel instance
		this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
	}

	/**
	 * Test 1: Verify MessagesModelHook is correctly loaded and executed
	 */
	@Test
	public void testMessagesModelHookLoadedAndExecuted() throws Exception {
		AtomicInteger beforeModelCallCount = new AtomicInteger(0);
		AtomicInteger afterModelCallCount = new AtomicInteger(0);

		TestMessagesModelHook hook = new TestMessagesModelHook("test_hook", beforeModelCallCount, afterModelCallCount);

		ReactAgent agent = createAgentWithMessagesHook(hook, "test-agent-loaded");

		System.out.println("\n=== 测试 MessagesModelHook 被正确加载且执行 ===");

		List<Message> messages = new ArrayList<>();
		messages.add(new UserMessage("你好，请简单介绍一下自己。"));

		Optional<OverAllState> result = agent.invoke(messages);

		assertTrue(result.isPresent(), "结果应该存在");
		assertTrue(beforeModelCallCount.get() > 0, "beforeModel 应该被调用");
		assertTrue(afterModelCallCount.get() > 0, "afterModel 应该被调用");

		System.out.println("✓ beforeModel 调用次数: " + beforeModelCallCount.get());
		System.out.println("✓ afterModel 调用次数: " + afterModelCallCount.get());
	}

	/**
	 * Test 2: Verify REPLACE policy works correctly
	 */
	@Test
	public void testReplacePolicy() throws Exception {
		ReplacePolicyMessagesHook hook = new ReplacePolicyMessagesHook();

		ReactAgent agent = createAgentWithMessagesHook(hook, "test-agent-replace");

		System.out.println("\n=== 测试 REPLACE 策略 ===");

		List<Message> messages = new ArrayList<>();
		messages.add(new UserMessage("原始消息1"));
		messages.add(new UserMessage("原始消息2"));

		Optional<OverAllState> result = agent.invoke(messages);

		assertTrue(result.isPresent(), "结果应该存在");
		Object messagesObj = result.get().value("messages").get();
		assertNotNull(messagesObj, "消息应该存在于结果中");

		if (messagesObj instanceof List) {
			List<Message> resultMessages = (List<Message>) messagesObj;
			System.out.println("返回消息数量: " + resultMessages.size());

			// 验证消息被替换：应该包含替换后的系统消息，且不包含原始消息
			boolean foundSystemMessage = false;
			boolean foundOriginalMessage1 = false;
			boolean foundOriginalMessage2 = false;
			
			for (Message message : resultMessages) {
				if (message instanceof SystemMessage) {
					String content = message.getText();
					if (content.contains("这是替换后的系统消息")) {
						foundSystemMessage = true;
					}
				} else if (message instanceof UserMessage) {
					String content = message.getText();
					if (content.equals("原始消息1")) {
						foundOriginalMessage1 = true;
					}
					if (content.equals("原始消息2")) {
						foundOriginalMessage2 = true;
					}
				}
			}
			
			assertTrue(foundSystemMessage, "应该找到替换后的系统消息");
			assertTrue(foundOriginalMessage2, "应该找到最后一条用户原始消息2");
			assertFalse(foundOriginalMessage1, "不应该找到第一条用户原始消息1");
			// 由于 REPLACE 策略，原始消息可能被替换，但 agent 执行过程中可能会添加新的消息
			// 所以我们主要验证替换后的系统消息存在
			System.out.println("✓ 成功验证 REPLACE 策略：替换后的系统消息存在");
		}
	}

	/**
	 * Test 3: Verify APPEND policy works correctly
	 */
	@Test
	public void testAppendPolicy() throws Exception {
		AppendPolicyMessagesHook hook = new AppendPolicyMessagesHook();

		ReactAgent agent = createAgentWithMessagesHook(hook, "test-agent-append");

		System.out.println("\n=== 测试 APPEND 策略 ===");

		List<Message> messages = new ArrayList<>();
		messages.add(new UserMessage("原始用户消息"));

		Optional<OverAllState> result = agent.invoke(messages);

		assertTrue(result.isPresent(), "结果应该存在");
		Object messagesObj = result.get().value("messages").get();
		assertNotNull(messagesObj, "消息应该存在于结果中");

		if (messagesObj instanceof List) {
			List<Message> resultMessages = (List<Message>) messagesObj;
			System.out.println("返回消息数量: " + resultMessages.size());

			// 验证消息被追加而不是替换
			boolean foundOriginalMessage = false;
			boolean foundAppendedMessage = false;
			for (Message message : resultMessages) {
				if (message instanceof UserMessage) {
					String content = message.getText();
					if (content.equals("原始用户消息")) {
						foundOriginalMessage = true;
					}
					if (content.equals("这是追加的消息")) {
						foundAppendedMessage = true;
					}
				}
			}
			assertTrue(foundOriginalMessage, "应该保留原始消息");
			assertTrue(foundAppendedMessage, "应该找到追加的消息");
			System.out.println("✓ 成功验证 APPEND 策略：消息被追加而不是替换");
		}
	}

	/**
	 * Test 4: Verify MessagesModelHook and ModelHook can work together
	 */
	@Test
	public void testMessagesModelHookWithModelHook() throws Exception {
		AtomicInteger messagesHookBeforeCount = new AtomicInteger(0);
		AtomicInteger messagesHookAfterCount = new AtomicInteger(0);
		AtomicInteger modelHookBeforeCount = new AtomicInteger(0);
		AtomicInteger modelHookAfterCount = new AtomicInteger(0);

		TestMessagesModelHook messagesHook = new TestMessagesModelHook("test_messages_hook",
				messagesHookBeforeCount, messagesHookAfterCount);
		TestModelHook modelHook = new TestModelHook("test_model_hook",
				modelHookBeforeCount, modelHookAfterCount);

		ReactAgent agent = ReactAgent.builder()
				.name("test-agent-both-hooks")
				.model(chatModel)
				.hooks(List.of(messagesHook, modelHook))
				.saver(new MemorySaver())
				.build();

		System.out.println("\n=== 测试 MessagesModelHook 和 ModelHook 同时使用 ===");

		List<Message> messages = new ArrayList<>();
		messages.add(new UserMessage("你好，请简单介绍一下自己。"));

		Optional<OverAllState> result = agent.invoke(messages);

		assertTrue(result.isPresent(), "结果应该存在");
		assertTrue(messagesHookBeforeCount.get() > 0, "MessagesModelHook beforeModel 应该被调用");
		assertTrue(messagesHookAfterCount.get() > 0, "MessagesModelHook afterModel 应该被调用");
		assertTrue(modelHookBeforeCount.get() > 0, "ModelHook beforeModel 应该被调用");
		assertTrue(modelHookAfterCount.get() > 0, "ModelHook afterModel 应该被调用");

		System.out.println("✓ MessagesModelHook beforeModel 调用次数: " + messagesHookBeforeCount.get());
		System.out.println("✓ MessagesModelHook afterModel 调用次数: " + messagesHookAfterCount.get());
		System.out.println("✓ ModelHook beforeModel 调用次数: " + modelHookBeforeCount.get());
		System.out.println("✓ ModelHook afterModel 调用次数: " + modelHookAfterCount.get());
		System.out.println("✓ 两个 Hook 可以同时正常运行");
	}

	private ReactAgent createAgentWithMessagesHook(MessagesModelHook hook, String name) throws GraphStateException {
		return ReactAgent.builder()
				.name(name)
				.model(chatModel)
				.hooks(List.of(hook))
				.saver(new MemorySaver())
				.build();
	}

	/**
	 * Test MessagesModelHook implementation for testing
	 */
	@HookPositions({HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL})
	private static class TestMessagesModelHook extends MessagesModelHook {
		private final String name;
		private final AtomicInteger beforeModelCallCount;
		private final AtomicInteger afterModelCallCount;

		public TestMessagesModelHook(String name, AtomicInteger beforeModelCallCount,
				AtomicInteger afterModelCallCount) {
			this.name = name;
			this.beforeModelCallCount = beforeModelCallCount;
			this.afterModelCallCount = afterModelCallCount;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
			beforeModelCallCount.incrementAndGet();
			System.out.println("TestMessagesModelHook.beforeModel called with " + previousMessages.size() + " messages");
			return new AgentCommand(previousMessages);
		}

		@Override
		public AgentCommand afterModel(List<Message> previousMessages, RunnableConfig config) {
			afterModelCallCount.incrementAndGet();
			System.out.println("TestMessagesModelHook.afterModel called with " + previousMessages.size() + " messages");
			return new AgentCommand(previousMessages);
		}
	}

	/**
	 * MessagesModelHook implementation that uses REPLACE policy
	 */
	@HookPositions({HookPosition.BEFORE_MODEL})
	private static class ReplacePolicyMessagesHook extends MessagesModelHook {
		@Override
		public String getName() {
			return "replace_policy_hook";
		}

		@Override
		public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
			// Replace all messages with a new system message and keep the last user message
			// This ensures agent can still function while demonstrating REPLACE policy
			List<Message> newMessages = new ArrayList<>();
			newMessages.add(new SystemMessage("这是替换后的系统消息"));
			// Keep the last user message so agent can still respond
			if (!previousMessages.isEmpty()) {
				Message lastMessage = previousMessages.get(previousMessages.size() - 1);
				if (lastMessage instanceof UserMessage) {
					newMessages.add(lastMessage);
				}
			}
			return new AgentCommand(newMessages, AppendPolicy.REPLACE);
		}
	}

	/**
	 * MessagesModelHook implementation that uses APPEND policy
	 */
	@HookPositions({HookPosition.BEFORE_MODEL})
	private static class AppendPolicyMessagesHook extends MessagesModelHook {
		@Override
		public String getName() {
			return "append_policy_hook";
		}

		@Override
		public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
			// Append a new message to existing messages
			List<Message> newMessages = new ArrayList<>();
			newMessages.add(new UserMessage("这是追加的消息"));
			return new AgentCommand(newMessages, AppendPolicy.APPEND);
		}
	}

	/**
	 * Test ModelHook implementation for testing
	 */
	@HookPositions({HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL})
	private static class TestModelHook extends ModelHook {
		private final String name;
		private final AtomicInteger beforeModelCallCount;
		private final AtomicInteger afterModelCallCount;

		public TestModelHook(String name, AtomicInteger beforeModelCallCount, AtomicInteger afterModelCallCount) {
			this.name = name;
			this.beforeModelCallCount = beforeModelCallCount;
			this.afterModelCallCount = afterModelCallCount;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
			beforeModelCallCount.incrementAndGet();
			System.out.println("TestModelHook.beforeModel called");
			return CompletableFuture.completedFuture(Map.of());
		}

		@Override
		public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
			afterModelCallCount.incrementAndGet();
			System.out.println("TestModelHook.afterModel called");
			return CompletableFuture.completedFuture(Map.of());
		}
	}
}


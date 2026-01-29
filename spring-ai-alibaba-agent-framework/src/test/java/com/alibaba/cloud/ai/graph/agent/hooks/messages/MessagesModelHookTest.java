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
package com.alibaba.cloud.ai.graph.agent.hooks.messages;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
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
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

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
	 * Test 4: Verify JumpTo End functionality - skip subsequent hooks
	 */
	@Test
	public void testJumpToEnd() throws Exception {
		AtomicInteger firstHookBeforeCount = new AtomicInteger(0);
		AtomicInteger firstHookAfterCount = new AtomicInteger(0);
		AtomicInteger secondHookBeforeCount = new AtomicInteger(0);
		AtomicInteger secondHookAfterCount = new AtomicInteger(0);
		AtomicInteger thirdHookBeforeCount = new AtomicInteger(0);
		AtomicInteger thirdHookAfterCount = new AtomicInteger(0);

		// First hook will jump to end, skipping subsequent hooks
		JumpToEndMessagesHook firstHook = new JumpToEndMessagesHook("jump_to_end_hook",
				firstHookBeforeCount, firstHookAfterCount);
		// Second hook should be skipped
		TestMessagesModelHook secondHook = new TestMessagesModelHook("second_hook",
				secondHookBeforeCount, secondHookAfterCount);
		// Third hook should also be skipped
		TestMessagesModelHook thirdHook = new TestMessagesModelHook("third_hook",
				thirdHookBeforeCount, thirdHookAfterCount);

		ReactAgent agent = ReactAgent.builder()
				.name("test-agent-jump-to-end")
				.model(chatModel)
				.hooks(List.of(firstHook, secondHook, thirdHook))
				.saver(new MemorySaver())
				.build();

		System.out.println("\n=== 测试 JumpTo End 功能 ===");

		List<Message> messages = new ArrayList<>();
		messages.add(new UserMessage("你好，请简单介绍一下自己。"));

		Optional<OverAllState> result = agent.invoke(messages);

		assertTrue(result.isPresent(), "结果应该存在");

		// First hook should be called
		assertTrue(firstHookBeforeCount.get() > 0, "第一个 hook 的 beforeModel 应该被调用");
		assertTrue(firstHookAfterCount.get() == 0, "第一个 hook 的 afterModel 不应该被调用（因为跳转到 end）");

		// Second and third hooks should be skipped
		assertEquals(0, secondHookBeforeCount.get(), "第二个 hook 的 beforeModel 不应该被调用（被跳过）");
		assertEquals(0, secondHookAfterCount.get(), "第二个 hook 的 afterModel 不应该被调用（被跳过）");
		assertEquals(0, thirdHookBeforeCount.get(), "第三个 hook 的 beforeModel 不应该被调用（被跳过）");
		assertEquals(0, thirdHookAfterCount.get(), "第三个 hook 的 afterModel 不应该被调用（被跳过）");

		System.out.println("✓ 第一个 hook beforeModel 调用次数: " + firstHookBeforeCount.get());
		System.out.println("✓ 第一个 hook afterModel 调用次数: " + firstHookAfterCount.get());
		System.out.println("✓ 第二个 hook beforeModel 调用次数: " + secondHookBeforeCount.get());
		System.out.println("✓ 第二个 hook afterModel 调用次数: " + secondHookAfterCount.get());
		System.out.println("✓ 第三个 hook beforeModel 调用次数: " + thirdHookBeforeCount.get());
		System.out.println("✓ 第三个 hook afterModel 调用次数: " + thirdHookAfterCount.get());
		System.out.println("✓ 成功验证 JumpTo End：后续 hooks 被正确跳过");
	}

	/**
	 * Test 5: Verify JumpTo End with mixed MessagesModelHook and ModelHook
	 */
	@Test
	public void testJumpToEndWithMixedHooks() throws Exception {
		AtomicInteger messagesHookBeforeCount = new AtomicInteger(0);
		AtomicInteger messagesHookAfterCount = new AtomicInteger(0);
		AtomicInteger modelHookBeforeCount = new AtomicInteger(0);
		AtomicInteger modelHookAfterCount = new AtomicInteger(0);
		AtomicInteger secondMessagesHookBeforeCount = new AtomicInteger(0);
		AtomicInteger secondMessagesHookAfterCount = new AtomicInteger(0);

		// First MessagesModelHook will jump to end
		JumpToEndMessagesHook firstMessagesHook = new JumpToEndMessagesHook("first_messages_hook",
				messagesHookBeforeCount, messagesHookAfterCount);
		// ModelHook should be skipped
		TestModelHook modelHook = new TestModelHook("test_model_hook",
				modelHookBeforeCount, modelHookAfterCount);
		// Second MessagesModelHook should also be skipped
		TestMessagesModelHook secondMessagesHook = new TestMessagesModelHook("second_messages_hook",
				secondMessagesHookBeforeCount, secondMessagesHookAfterCount);

		ReactAgent agent = ReactAgent.builder()
				.name("test-agent-jump-to-end-mixed")
				.model(chatModel)
				.hooks(List.of(firstMessagesHook, modelHook, secondMessagesHook))
				.saver(new MemorySaver())
				.build();

		System.out.println("\n=== 测试 JumpTo End 功能（混合 MessagesModelHook 和 ModelHook）===");

		List<Message> messages = new ArrayList<>();
		messages.add(new UserMessage("你好，请简单介绍一下自己。"));

		Optional<OverAllState> result = agent.invoke(messages);

		assertTrue(result.isPresent(), "结果应该存在");

		// First MessagesModelHook should be called
		assertTrue(messagesHookBeforeCount.get() > 0, "第一个 MessagesModelHook 的 beforeModel 应该被调用");
		assertEquals(0, messagesHookAfterCount.get(), "第一个 MessagesModelHook 的 afterModel 不应该被调用");

		// ModelHook should be skipped
		assertEquals(0, modelHookBeforeCount.get(), "ModelHook 的 beforeModel 不应该被调用（被跳过）");
		assertEquals(0, modelHookAfterCount.get(), "ModelHook 的 afterModel 不应该被调用（被跳过）");

		// Second MessagesModelHook should be skipped
		assertEquals(0, secondMessagesHookBeforeCount.get(), "第二个 MessagesModelHook 的 beforeModel 不应该被调用（被跳过）");
		assertEquals(0, secondMessagesHookAfterCount.get(), "第二个 MessagesModelHook 的 afterModel 不应该被调用（被跳过）");

		System.out.println("✓ 第一个 MessagesModelHook beforeModel 调用次数: " + messagesHookBeforeCount.get());
		System.out.println("✓ ModelHook beforeModel 调用次数: " + modelHookBeforeCount.get());
		System.out.println("✓ 第二个 MessagesModelHook beforeModel 调用次数: " + secondMessagesHookBeforeCount.get());
		System.out.println("✓ 成功验证 JumpTo End（混合 hooks）：后续 hooks 被正确跳过");
	}

	/**
	 * Test 6: Verify MessagesModelHook and ModelHook can work together
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
			return new AgentCommand(newMessages, UpdatePolicy.REPLACE);
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
			return new AgentCommand(newMessages, UpdatePolicy.APPEND);
		}
	}

	/**
	 * MessagesModelHook implementation that jumps to end
	 */
	@HookPositions({HookPosition.BEFORE_MODEL})
	private static class JumpToEndMessagesHook extends MessagesModelHook {
		private final String name;
		private final AtomicInteger beforeModelCallCount;
		private final AtomicInteger afterModelCallCount;

		public JumpToEndMessagesHook(String name, AtomicInteger beforeModelCallCount,
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
		public List<JumpTo> canJumpTo() {
			return List.of(JumpTo.end);
		}

		@Override
		public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
			beforeModelCallCount.incrementAndGet();
			System.out.println("JumpToEndMessagesHook.beforeModel called - jumping to end");
			// Return jumpTo end to skip subsequent hooks and model call
			return new AgentCommand(JumpTo.end, previousMessages);
		}

		@Override
		public AgentCommand afterModel(List<Message> previousMessages, RunnableConfig config) {
			afterModelCallCount.incrementAndGet();
			System.out.println("JumpToEndMessagesHook.afterModel called");
			return new AgentCommand(previousMessages);
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

	/**
	 * Test 7: Verify JumpTo.end works in afterModel hook when agent has tools
	 * This test addresses the bug where JumpTo in afterModel was ignored when tools were configured
	 */
	@Test
	public void testJumpToEndInAfterModelWithTools() throws Exception {
		AtomicInteger afterModelCallCount = new AtomicInteger(0);
		AtomicInteger toolCallCount = new AtomicInteger(0);

		// Hook that only overrides afterModel and uses JumpTo.end
		@HookPositions({HookPosition.AFTER_MODEL})
		class AfterModelOnlyJumpHook extends MessagesModelHook {
			@Override
			public String getName() {
				return "after_model_jump_hook";
			}

			@Override
			public List<JumpTo> canJumpTo() {
				return List.of(JumpTo.end);
			}

			@Override
			public AgentCommand afterModel(List<Message> previousMessages, RunnableConfig config) {
				afterModelCallCount.incrementAndGet();
				System.out.println("AfterModelOnlyJumpHook.afterModel called - jumping to end");
				// Return JumpTo.end to skip tool execution and end immediately
				return new AgentCommand(JumpTo.end, previousMessages);
			}
		}

		AfterModelOnlyJumpHook hook = new AfterModelOnlyJumpHook();

		// Create a simple test tool
		ToolCallback testTool = FunctionToolCallback.builder("test_tool", args -> {
					toolCallCount.incrementAndGet();
					System.out.println("Test tool called - this should NOT happen!");
					return "Tool executed";
				})
				.description("A test tool")
				.inputType(String.class)
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("test-agent-after-model-jump-with-tools")
				.model(chatModel)
				.tools(List.of(testTool))
				.hooks(List.of(hook))
				.saver(new MemorySaver())
				.build();

		System.out.println("\n=== 测试 afterModel 中的 JumpTo.end（配置了工具）===");

		List<Message> messages = new ArrayList<>();
		messages.add(new UserMessage("你好，请简单介绍一下自己。"));

		Optional<OverAllState> result = agent.invoke(messages);

		assertTrue(result.isPresent(), "结果应该存在");
		assertTrue(afterModelCallCount.get() > 0, "afterModel 应该被调用");

		// Key verification: tool should NOT be called because JumpTo.end should skip it
		assertEquals(0, toolCallCount.get(), "工具不应该被调用（因为 JumpTo.end 直接结束了）");

		System.out.println("afterModel 调用次数: " + afterModelCallCount.get());
		System.out.println("工具调用次数: " + toolCallCount.get());
		System.out.println("成功验证 afterModel 中的 JumpTo.end 在有工具配置时正常工作");
	}

	/**
	 * Test 8: Verify JumpTo.model works in afterModel hook when agent has tools
	 */
	@Test
	public void testJumpToModelInAfterModelWithTools() throws Exception {
		AtomicInteger afterModelCallCount = new AtomicInteger(0);
		AtomicInteger modelCallCount = new AtomicInteger(0);
		AtomicInteger toolCallCount = new AtomicInteger(0);

		// Hook that only overrides afterModel and uses JumpTo.model on first call
		@HookPositions({HookPosition.AFTER_MODEL})
		class AfterModelJumpToModelHook extends MessagesModelHook {
			@Override
			public String getName() {
				return "after_model_jump_to_model_hook";
			}

			@Override
			public List<JumpTo> canJumpTo() {
				return List.of(JumpTo.model, JumpTo.end);
			}

			@Override
			public AgentCommand afterModel(List<Message> previousMessages, RunnableConfig config) {
				afterModelCallCount.incrementAndGet();
				System.out.println("AfterModelJumpToModelHook.afterModel called, count: " + afterModelCallCount.get());

				// On first call, jump back to model; on second call, end
				if (afterModelCallCount.get() == 1) {
					System.out.println("First call - jumping back to model");
					return new AgentCommand(JumpTo.model, previousMessages);
				} else {
					System.out.println("Second call - ending");
					return new AgentCommand(JumpTo.end, previousMessages);
				}
			}
		}

		AfterModelJumpToModelHook hook = new AfterModelJumpToModelHook();

		// Create a test tool (required to trigger makeModelToTools routing)
		ToolCallback testTool = FunctionToolCallback.builder("test_tool", args -> {
					toolCallCount.incrementAndGet();
					System.out.println("Test tool called");
					return "Tool executed";
				})
				.description("A test tool")
				.inputType(String.class)
				.build();

		// Track model calls
		ChatModel trackingChatModel = new ChatModel() {
			@Override
			public ChatResponse call(Prompt prompt) {
				modelCallCount.incrementAndGet();
				System.out.println("Model called, count: " + modelCallCount.get());
				return chatModel.call(prompt);
			}

			@Override
			public ChatOptions getDefaultOptions() {
				return chatModel.getDefaultOptions();
			}
		};

		ReactAgent agent = ReactAgent.builder()
				.name("test-agent-after-model-jump-to-model")
				.model(trackingChatModel)
				.tools(List.of(testTool))
				.hooks(List.of(hook))
				.saver(new MemorySaver())
				.build();

		System.out.println("\n=== 测试 afterModel 中的 JumpTo.model ===");

		List<Message> messages = new ArrayList<>();
		messages.add(new UserMessage("你好"));

		Optional<OverAllState> result = agent.invoke(messages);

		assertTrue(result.isPresent(), "结果应该存在");
		assertEquals(2, afterModelCallCount.get(), "afterModel 应该被调用 2 次（第一次跳回模型，第二次结束）");
		assertEquals(2, modelCallCount.get(), "模型应该被调用 2 次（第一次正常，第二次因为 JumpTo.model）");
		// Tools should not be called because JumpTo redirects flow before tool execution
		assertEquals(0, toolCallCount.get(), "工具不应该被调用（因为 JumpTo 直接跳转了）");

		System.out.println("afterModel 调用次数: " + afterModelCallCount.get());
		System.out.println("模型调用次数: " + modelCallCount.get());
		System.out.println("工具调用次数: " + toolCallCount.get());
		System.out.println("成功验证 afterModel 中的 JumpTo.model 在有工具配置时正常工作");
	}
}

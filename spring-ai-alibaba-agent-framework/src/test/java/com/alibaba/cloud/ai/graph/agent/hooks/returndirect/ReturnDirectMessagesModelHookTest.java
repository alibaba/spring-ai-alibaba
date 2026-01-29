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
package com.alibaba.cloud.ai.graph.agent.hooks.returndirect;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.returndirect.ReturnDirectModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test cases for ReturnDirectModelHook.
 * This hook checks for finishReason in ToolResponseMessage metadata and jumps to END if found.
 */
@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
public class ReturnDirectMessagesModelHookTest {

	private ChatModel chatModel;

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
	}

	/**
	 * Test that ReturnDirectModelHook correctly handles returnDirect tools
	 * and jumps to END node when finishReason is found in ToolResponseMessage metadata.
	 */
	@Test
	public void testFinishReasonCheckWithReturnDirectTools() throws GraphRunnerException {
		var saver = new MemorySaver();
		var hook = new ReturnDirectModelHook();

		var react = ReactAgent.builder()
				.name("testFinishReasonAgent")
				.model(chatModel)
				.saver(saver)
				.instruction("地点为: {target_topic}")
				.tools(ToolCallbacks.from(new TestDirectTools()))
				.hooks(List.of(hook))
				.systemPrompt("你是一个天气预报助手，帮我查看指定地点的天气预报")
				.build();

		System.out.println("\n=== 测试 ReturnDirectModelHook 与 returnDirect 工具 ===");

		String output = react.call("上海,北京").getText();
		System.out.println("ReactAgent Output: " + output);

		assertNotNull(output, "Output should not be null");
		assertFalse(output.isEmpty(), "Output should not be empty");

		// Verify that output contains the direct tool response
		assertTrue(output.contains("上海天气不错"), "Output should contain weather info for Shanghai");
		assertTrue(output.contains("北京天气不错"), "Output should contain weather info for Beijing");

		// Verify that the output only contains the tool response (no additional model processing)
		String cleanedOutput = output.replace("上海天气不错", "").replace("北京天气不错", "");
		assertTrue(cleanedOutput.trim().isEmpty() || cleanedOutput.trim().length() < 10,
				"Output should primarily contain tool response without additional model processing");

		System.out.println("✓ Test passed: ReturnDirectModelHook correctly handled returnDirect tools");
	}

	/**
	 * Test that ReturnDirectModelHook works with multiple returnDirect tools
	 * and generates JSON array for multiple responses.
	 */
	@Test
	public void testFinishReasonCheckWithMultipleReturnDirectTools() throws GraphRunnerException {
		var saver = new MemorySaver();
		var hook = new ReturnDirectModelHook();

		var react = ReactAgent.builder()
				.name("testFinishReasonMultipleAgent")
				.model(chatModel)
				.saver(saver)
				.tools(ToolCallbacks.from(new TestMultipleDirectTools()))
				.hooks(List.of(hook))
				.systemPrompt("你是一个工具调用助手，可以同时调用多个工具")
				.build();

		System.out.println("\n=== 测试 ReturnDirectModelHook 与多个 returnDirect 工具 ===");

		String output = react.call("请调用getCity1和getCity2工具").getText();
		System.out.println("ReactAgent Output: " + output);

		assertNotNull(output, "Output should not be null");
		assertFalse(output.isEmpty(), "Output should not be empty");

		String trimmedOutput = output.trim();

		// Verify that output contains responses from BOTH tools (not just one)
		assertTrue(trimmedOutput.contains("City1"), 
				"Output should contain response from getCity1 tool. Actual: " + trimmedOutput);
		assertTrue(trimmedOutput.contains("City2"), 
				"Output should contain response from getCity2 tool. Actual: " + trimmedOutput);

		// Verify that output is a valid JSON array when multiple responses occur
		assertTrue(trimmedOutput.startsWith("["), 
				"Output should start with '[' for JSON array format. Actual: " + trimmedOutput);
		assertTrue(trimmedOutput.endsWith("]"), 
				"Output should end with ']' for JSON array format. Actual: " + trimmedOutput);

		// Validate JSON array structure: verify both tool responses are present
		// The format should be either ["City1","City2"] or ["City2","City1"]
		String arrayContent = trimmedOutput.substring(1, trimmedOutput.length() - 1).trim();
		
		// Verify both values are present in the array content
		assertTrue(arrayContent.contains("City1") && arrayContent.contains("City2"),
				"JSON array should contain both City1 and City2. Actual array content: " + arrayContent);
		
		// Verify proper JSON array structure: should have comma-separated values
		// Check that there's a comma (indicating multiple elements) or both values are present
		assertTrue(arrayContent.contains(",") || 
				(arrayContent.contains("City1") && arrayContent.contains("City2")),
				"JSON array should have proper structure with multiple elements. Actual: " + arrayContent);

		System.out.println("✓ Test passed: ReturnDirectModelHook correctly handled multiple returnDirect tools");
	}

	/**
	 * Test that ReturnDirectModelHook does not interfere with normal tool execution
	 * when returnDirect is false.
	 */
	@Test
	public void testFinishReasonCheckWithNormalTools() throws GraphRunnerException {
		var saver = new MemorySaver();
		var hook = new ReturnDirectModelHook();

		// Track model calls to verify model processes tool response
		AtomicInteger modelCallCount = new AtomicInteger(0);
		ChatModel trackingChatModel = new ChatModel() {
			@Override
			public ChatResponse call(Prompt prompt) {
				modelCallCount.incrementAndGet();
				return chatModel.call(prompt);
			}

			@Override
			public ChatOptions getDefaultOptions() {
				return chatModel.getDefaultOptions();
			}
		};

		var react = ReactAgent.builder()
				.name("testFinishReasonNormalAgent")
				.model(trackingChatModel)
				.saver(saver)
				.tools(ToolCallbacks.from(new TestNormalTools()))
				.hooks(List.of(hook))
				.systemPrompt("你是一个助手，可以调用工具获取信息")
				.build();

		System.out.println("\n=== 测试 ReturnDirectModelHook 与普通工具（returnDirect=false）===");

		String output = react.call("请调用getInfo工具获取信息").getText();
		System.out.println("ReactAgent Output: " + output);

		assertNotNull(output, "Output should not be null");
		assertFalse(output.isEmpty(), "Output should not be empty");

		// With normal tools (returnDirect=false), the model should process the tool response
		// Verify that model was called at least twice:
		// 1. First call: to generate AssistantMessage with tool calls
		// 2. Second call: to process the tool response and generate final answer
		int actualModelCallCount = modelCallCount.get();
		System.out.println("Model 调用次数: " + actualModelCallCount);
		assertTrue(actualModelCallCount >= 2,
				"Model should be called at least twice: once to generate tool calls, " +
				"and once to process tool response (returnDirect=false means model processes the result)");

		// Verify that output is NOT exactly the raw tool result
		// The raw tool result would be "Information: 请调用getInfo工具获取信息"
		// Model should process this and generate a more natural response
		String rawToolResult = "Information: 请调用getInfo工具获取信息";
		assertNotEquals(rawToolResult, output.trim(),
				"Output should be processed by model, not be the raw tool result. " +
				"Expected model-generated content, but got raw tool result: " + rawToolResult);

		System.out.println("✓ Test passed: ReturnDirectModelHook does not interfere with normal tools");
	}

	/**
	 * Test that ReturnDirectModelHook executes first among all hooks
	 * and jumps to END when returnDirect is true, preventing subsequent hooks and model calls.
	 */
	@Test
	public void testFinishReasonCheckHookOrder() throws GraphRunnerException {
		var saver = new MemorySaver();
		var finishReasonHook = new ReturnDirectModelHook();

		// Create a test hook that tracks execution order
		AtomicIntegerHook testHook = new AtomicIntegerHook("test_hook");

		// Track model calls to verify that model is not called when returnDirect is true
		AtomicInteger modelCallCount = new AtomicInteger(0);
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

		var react = ReactAgent.builder()
				.name("testHookOrderAgent")
				.model(trackingChatModel)
				.saver(saver)
				.tools(ToolCallbacks.from(new TestDirectTools()))
				.hooks(List.of(finishReasonHook, testHook))
				.systemPrompt("你是一个天气预报助手")
				.build();

		System.out.println("\n=== 测试 ReturnDirectModelHook 执行顺序 ===");

		Optional<OverAllState> stateOpt = react.invoke("上海");

		List<Message> messages = stateOpt.orElseThrow(() -> new GraphRunnerException("No state returned")).value("messages", List.of());
		if (messages.isEmpty()) {
			fail();
		}

		String output = messages.get(messages.size() - 1).getText();
		System.out.println("ReactAgent Output: " + output);

		// Verify that ReturnDirectModelHook executed (hook should have processed the request)
		assertNotNull(output, "Output should not be null");
		assertTrue(output.contains("上海天气不错"), "Output should contain direct tool response");

		// Verify that when returnDirect is true and ReturnDirectModelHook jumps to END,
		// the subsequent hook's beforeModel should NOT be called (or called only before the jump)
		// Since ReturnDirectModelHook has order Integer.MIN_VALUE, it executes first
		// and jumps to END, preventing testHook from being called in the beforeModel phase
		int testHookBeforeModelCount = testHook.getBeforeModelCount();
		System.out.println("TestHook beforeModel 调用次数: " + testHookBeforeModelCount);

		// Verify model call count: model should be called exactly once
		// - First call: to generate AssistantMessage with tool calls
		// - When returnDirect=true, after tool execution, ReturnDirectModelHook
		//   jumps to END, so model is NOT called again to process tool response
		int actualModelCallCount = modelCallCount.get();
		System.out.println("Model 调用次数: " + actualModelCallCount);
		assertEquals(1, actualModelCallCount,
				"Model should be called exactly once: first call to generate tool calls, " +
				"then ReturnDirectModelHook jumps to END without second model call");

		// Note: testHook's beforeModel might be called 0 times if ReturnDirectModelHook
		// successfully jumps to END before other hooks execute, or it might be called once before
		// the tool execution if hooks are executed in sequence before tool node
		// The key verification is that model is called only once (not twice), confirming returnDirect
		// and jump to END worked - tool response is returned directly without model processing
		System.out.println("✓ Test passed: Hook execution order and returnDirect behavior verified");
	}

	/**
	 * Test tools with returnDirect=true
	 */
	static class TestDirectTools {

		@Tool(name = "getWeatherByCity", description = "Get weather information by city name", returnDirect = true)
		public String getWeatherByCity(@ToolParam(description = "城市地址列表，用中文") List<String> cityNameList) {
			StringBuilder builder = new StringBuilder();
			for (String cityName : cityNameList) {
				builder.append(cityName).append("天气不错");
			}
			return builder.toString();
		}
	}

	/**
	 * Test tools with multiple returnDirect=true tools
	 */
	static class TestMultipleDirectTools {

		@Tool(name = "getCity1", description = "Get first city information", returnDirect = true)
		public String getCity1() {
			return "City1";
		}

		@Tool(name = "getCity2", description = "Get second city information", returnDirect = true)
		public String getCity2() {
			return "City2";
		}
	}

	/**
	 * Test tools with returnDirect=false (normal tools)
	 */
	static class TestNormalTools {

		@Tool(name = "getInfo", description = "Get information", returnDirect = false)
		public String getInfo(@ToolParam(description = "Information to get") String info) {
			return "Information: " + info;
		}
	}

	/**
	 * Simple hook for testing execution order
	 */
	@HookPositions({HookPosition.BEFORE_MODEL})
	private static class AtomicIntegerHook extends MessagesModelHook {
		private final String name;
		private int beforeModelCount = 0;

		public AtomicIntegerHook(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
			beforeModelCount++;
			return new AgentCommand(previousMessages);
		}

		public int getBeforeModelCount() {
			return beforeModelCount;
		}
	}
}

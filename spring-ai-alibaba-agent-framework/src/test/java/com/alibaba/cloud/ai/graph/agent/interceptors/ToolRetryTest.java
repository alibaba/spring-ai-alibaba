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
package com.alibaba.cloud.ai.graph.agent.interceptors;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolretry.ToolRetryInterceptor;
import com.alibaba.cloud.ai.graph.agent.tools.PoetTool;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class ToolRetryTest {

	private ChatModel chatModel;

	private static CompileConfig getCompileConfig() {
		SaverConfig saverConfig = SaverConfig.builder()
				.register(new MemorySaver())
				.build();
		CompileConfig compileConfig = CompileConfig.builder().saverConfig(saverConfig).build();
		return compileConfig;
	}

	@BeforeEach
	void setUp() {
		// Create DashScopeApi instance using the API key from environment variable
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();
		// Create DashScope ChatModel instance
		this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
	}

	@Test
	public void testToolRetryOnFailure() throws Exception {
		

		// Create a tool that fails first time but succeeds on retry
		FailingTool failingTool = new FailingTool(1); // Fail once
		ToolCallback failingToolCallback = FunctionToolCallback.builder("failing_tool", failingTool)
				.description("A tool that fails initially but succeeds on retry")
				.inputType(String.class)
				.build();

		// Create tool retry interceptor
		ToolRetryInterceptor toolRetryInterceptor = ToolRetryInterceptor.builder()
				.maxRetries(3)
				.initialDelay(100)
				.backoffFactor(2.0)
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("retry_test_agent")
				.model(chatModel)
				.tools(failingToolCallback)
				.interceptors(toolRetryInterceptor)
				.saver(new MemorySaver())
				.toolExecutionExceptionProcessor(DefaultToolExecutionExceptionProcessor.builder().alwaysThrow(true).build())
				.build();

		try {
			Optional<OverAllState> result = agent.invoke("请使用failing_tool工具处理这个请求");

			// Assert that the result is present
			assertTrue(result.isPresent(), "Agent result should be present");

			// Verify the tool was called multiple times (original + retry)
			assertTrue(failingTool.getCallCount() >= 2,
					"Failing tool should be called at least twice (original + 1 retry)");

			// Verify it eventually succeeded
			assertTrue(failingTool.hasSucceeded(),
					"Tool should eventually succeed after retry");

		}
		catch (java.util.concurrent.CompletionException e) {
			e.printStackTrace();
			fail("ReactAgent execution failed: " + e.getMessage());
		}
	}

	@Test
	public void testToolRetryMaxRetriesExceeded() throws Exception {
		

		// Create a tool that always fails
		FailingTool alwaysFailingTool = new FailingTool(10); // Fail 10 times
		ToolCallback failingToolCallback = FunctionToolCallback.builder("always_failing_tool", alwaysFailingTool)
				.description("A tool that always fails")
				.inputType(String.class)
				.build();

		// Create tool retry interceptor with limited retries
		ToolRetryInterceptor toolRetryInterceptor = ToolRetryInterceptor.builder()
				.maxRetries(2)
				.initialDelay(50)
				.onFailure(ToolRetryInterceptor.OnFailureBehavior.RAISE)
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("max_retry_agent")
				.model(chatModel)
				.tools(failingToolCallback)
				.interceptors(toolRetryInterceptor)
				.saver(new MemorySaver())
				.toolExecutionExceptionProcessor(DefaultToolExecutionExceptionProcessor.builder().alwaysThrow(true).build())
				.build();

		try {
			Optional<OverAllState> result = agent.invoke("请使用always_failing_tool工具处理这个请求");

			// Assert that the result is present even though tool failed
			assertTrue(result.isPresent(), "Agent result should be present");

			// Verify the tool was called exactly maxRetries + 1 times
			assertEquals(3, alwaysFailingTool.getCallCount(),
					"Tool should be called maxRetries + 1 times (1 original + 2 retries)");

			// Verify it did not succeed
			assertFalse(alwaysFailingTool.hasSucceeded(),
					"Tool should not succeed when it always fails");

		}
		catch (RuntimeException e) {
			// This is expected when RETURN_ERROR behavior is used
			assertTrue(e.getMessage().contains("Tool call failed after 3 attempts"));
		}
	}

	@Test
	public void testToolRetryWithSpecificTools() throws Exception {
		

		// Create a failing tool
		FailingTool failingTool = new FailingTool(1);
		ToolCallback failingToolCallback = FunctionToolCallback.builder("specific_tool", failingTool)
				.description("A specific tool to retry")
				.inputType(String.class)
				.build();

		// Create a normal tool that should not be retried
		PoetTool poetTool = new PoetTool();
		ToolCallback poetToolCallback = PoetTool.createPoetToolCallback("poem", poetTool);

		// Create tool retry interceptor for specific tool only
		ToolRetryInterceptor toolRetryInterceptor = ToolRetryInterceptor.builder()
				.maxRetries(2)
				.initialDelay(50)
				.toolName("specific_tool")
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("specific_retry_agent")
				.model(chatModel)
				.tools(failingToolCallback, poetToolCallback)
				.interceptors(toolRetryInterceptor)
				.saver(new MemorySaver())
				.build();

		try {
			Optional<OverAllState> result = agent.invoke("请使用specific_tool工具处理这个请求");

			// Assert that the result is present
			assertTrue(result.isPresent(), "Agent result should be present");

			// Verify the specific tool was retried
			assertTrue(failingTool.getCallCount() >= 2,
					"Specific tool should be retried");

		}
		catch (java.util.concurrent.CompletionException e) {
			// Expected if tool keeps failing
			assertTrue(failingTool.getCallCount() > 1,
					"Tool should have been retried before final failure");
		}
	}

	/**
	 * A test tool that fails a specified number of times before succeeding.
	 */
	static class FailingTool implements BiFunction<String, ToolContext, String> {
		private final AtomicInteger callCount = new AtomicInteger(0);
		private final AtomicInteger failCount;
		private boolean succeeded = false;

		public FailingTool(int timesToFail) {
			this.failCount = new AtomicInteger(timesToFail);
		}

		@Override
		public String apply(String input, ToolContext context) {
			int currentCall = callCount.incrementAndGet();

			if (failCount.get() > 0) {
				failCount.decrementAndGet();
				throw new RuntimeException("Tool intentionally failed (attempt " + currentCall + ")");
			}

			succeeded = true;
			return "Tool succeeded on attempt " + currentCall;
		}

		public int getCallCount() {
			return callCount.get();
		}

		public boolean hasSucceeded() {
			return succeeded;
		}
	}
}


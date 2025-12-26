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

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.modelretry.ModelRetryInterceptor;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitExceededException;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real integration test: Verify actual compatibility between ModelRetryInterceptor and ModelCallLimitHook
 * 
 * This test simulates the complete call flow:
 * 1. Hook.beforeModel() - Check limits
 * 2. Interceptor.interceptModel() - Execute retries and update counters
 * 3. Hook.afterModel() - Update counters
 */
class ModelRetryWithHookIntegrationTest {

	@Test
	void testRetryWithHookIntegration_RetriesCountTowardsLimit() throws Exception {
		// Configuration: max 3 attempts, limit 6 calls
		ModelRetryInterceptor interceptor = ModelRetryInterceptor.builder()
			.maxAttempts(3)
			.initialDelay(10)
			.build();

		ModelCallLimitHook hook = ModelCallLimitHook.builder()
			.runLimit(6)
			.exitBehavior(ModelCallLimitHook.ExitBehavior.ERROR)
			.build();

		RunnableConfig config = RunnableConfig.builder().build();
		AtomicInteger totalApiCalls = new AtomicInteger(0);

		// Simulate 3 rounds of calls
		for (int round = 1; round <= 3; round++) {
			final int currentRound = round;
			
			// 1. Hook beforeModel - Check limits
			OverAllState state = new OverAllState(Map.of());
			hook.beforeModel(state, config).get();

			// 2. Interceptor interceptModel - Execute retries
			Map<String, Object> requestContext = new HashMap<>();
			requestContext.put(ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY, config);

			ModelCallHandler handler = request -> {
				int count = totalApiCalls.incrementAndGet();
				// Round 1: Retry 1 time then succeed (2 calls total)
				// Round 2: Retry 2 times then succeed (3 calls total)
				// Round 3: Succeed immediately (1 call total)
				if (currentRound == 1 && count < 2) {
					throw new RuntimeException("I/O error: connection timeout");
				} else if (currentRound == 2 && count < 4) {
					throw new RuntimeException("I/O error: connection timeout");
				}
				return ModelResponse.of(new AssistantMessage("Success round " + currentRound));
			};

			ModelRequest request = ModelRequest.builder()
				.context(requestContext)
				.build();

			ModelResponse response = interceptor.interceptModel(request, handler);
			assertNotNull(response);
			assertEquals("Success round " + currentRound, ((AssistantMessage) response.getMessage()).getText());

			// 3. Hook afterModel - Update counters
			hook.afterModel(state, config).get();
		}

		// Verify total API call count: 2 + 3 + 1 = 6
		assertEquals(6, totalApiCalls.get(), "Total API calls should be 6");

		// Verify Hook's counter
		int hookRunCount = (int) config.context().get("__model_call_limit_run_count__");
		
		// Hook count = 3 rounds + Interceptor retries (1 + 2 + 0 = 3)
		// Total: 3 + 3 = 6
		assertEquals(6, hookRunCount, "Hook count should include all retries");

		// Verify limit is reached - next call should throw exception
		OverAllState state = new OverAllState(Map.of());
		assertThrows(ModelCallLimitExceededException.class, () -> {
			hook.beforeModel(state, config).get();
		}, "Should throw exception after reaching limit");
	}

	@Test
	void testRetryWithHookIntegration_HookCountsAllRetries() throws Exception {
		// Configuration: max 5 attempts, limit 10 calls
		ModelRetryInterceptor interceptor = ModelRetryInterceptor.builder()
			.maxAttempts(5)
			.initialDelay(10)
			.build();

		ModelCallLimitHook hook = ModelCallLimitHook.builder()
			.runLimit(10)
			.exitBehavior(ModelCallLimitHook.ExitBehavior.ERROR)
			.build();

		RunnableConfig config = RunnableConfig.builder().build();
		AtomicInteger totalApiCalls = new AtomicInteger(0);

		// Simulate 2 rounds of calls
		for (int round = 1; round <= 2; round++) {
			final int currentRound = round;
			
			// 1. Hook beforeModel
			OverAllState state = new OverAllState(Map.of());
			hook.beforeModel(state, config).get();

			// 2. Interceptor interceptModel
			Map<String, Object> requestContext = new HashMap<>();
			requestContext.put(ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY, config);

			ModelCallHandler handler = request -> {
				int count = totalApiCalls.incrementAndGet();
				// Round 1: Fail 4 times then succeed (5 calls total)
				// Round 2: Fail 4 times then succeed (5 calls total)
				if (count % 5 != 0) {
					throw new RuntimeException("Connection timeout");
				}
				return ModelResponse.of(new AssistantMessage("Success"));
			};

			ModelRequest request = ModelRequest.builder()
				.context(requestContext)
				.build();

			ModelResponse response = interceptor.interceptModel(request, handler);
			assertNotNull(response);

			// 3. Hook afterModel
			hook.afterModel(state, config).get();
		}

		// Verify total API call count: 5 + 5 = 10
		assertEquals(10, totalApiCalls.get(), "Total API calls should be 10");

		// Verify Hook's counter
		int hookRunCount = (int) config.context().get("__model_call_limit_run_count__");
		
		// Hook count = 2 rounds + Interceptor retries (4 + 4 = 8)
		// Total: 2 + 8 = 10
		assertEquals(10, hookRunCount, "Hook should count all actual API calls (including retries)");

		// Verify limit is reached
		OverAllState state = new OverAllState(Map.of());
		assertThrows(ModelCallLimitExceededException.class, () -> {
			hook.beforeModel(state, config).get();
		}, "Should throw exception after reaching limit");
	}

	@Test
	void testRetryWithHookIntegration_NoRetryDoesNotInflateCount() throws Exception {
		// Configuration: max 3 attempts, limit 3 calls
		ModelRetryInterceptor interceptor = ModelRetryInterceptor.builder()
			.maxAttempts(3)
			.initialDelay(10)
			.build();

		ModelCallLimitHook hook = ModelCallLimitHook.builder()
			.runLimit(3)
			.exitBehavior(ModelCallLimitHook.ExitBehavior.ERROR)
			.build();

		RunnableConfig config = RunnableConfig.builder().build();
		AtomicInteger totalApiCalls = new AtomicInteger(0);

		// Simulate 3 rounds of calls, each succeeds immediately (no retries)
		for (int round = 1; round <= 3; round++) {
			// 1. Hook beforeModel
			OverAllState state = new OverAllState(Map.of());
			hook.beforeModel(state, config).get();

			// 2. Interceptor interceptModel - Succeed immediately
			Map<String, Object> requestContext = new HashMap<>();
			requestContext.put(ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY, config);

			ModelCallHandler handler = request -> {
				totalApiCalls.incrementAndGet();
				return ModelResponse.of(new AssistantMessage("Success immediately"));
			};

			ModelRequest request = ModelRequest.builder()
				.context(requestContext)
				.build();

			ModelResponse response = interceptor.interceptModel(request, handler);
			assertNotNull(response);

			// 3. Hook afterModel
			hook.afterModel(state, config).get();
		}

		// Verify total API call count: 3 (1 per round, no retries)
		assertEquals(3, totalApiCalls.get(), "Without retries, API call count should equal loop count");

		// Verify Hook's counter
		int hookRunCount = (int) config.context().get("__model_call_limit_run_count__");
		
		// Hook count = 3 rounds + 0 retries = 3
		assertEquals(3, hookRunCount, "Without retries, Hook count should equal loop count");

		// Verify limit is reached
		OverAllState state = new OverAllState(Map.of());
		assertThrows(ModelCallLimitExceededException.class, () -> {
			hook.beforeModel(state, config).get();
		}, "Should throw exception after reaching limit");
	}

	@Test
	void testRetryWithHookIntegration_HookStopsBeforeMaxRetries() throws Exception {
		// Configuration: Interceptor max 5 attempts, but Hook limits to 3 calls
		ModelRetryInterceptor interceptor = ModelRetryInterceptor.builder()
			.maxAttempts(5)
			.initialDelay(10)
			.build();

		ModelCallLimitHook hook = ModelCallLimitHook.builder()
			.runLimit(3)
			.exitBehavior(ModelCallLimitHook.ExitBehavior.ERROR)
			.build();

		RunnableConfig config = RunnableConfig.builder().build();
		AtomicInteger totalApiCalls = new AtomicInteger(0);

		// Round 1: Retry 1 time then succeed (2 calls total)
		{
			OverAllState state = new OverAllState(Map.of());
			hook.beforeModel(state, config).get();

			Map<String, Object> requestContext = new HashMap<>();
			requestContext.put(ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY, config);

			ModelCallHandler handler = request -> {
				int count = totalApiCalls.incrementAndGet();
				if (count < 2) {
					throw new RuntimeException("I/O error");
				}
				return ModelResponse.of(new AssistantMessage("Success"));
			};

			ModelRequest request = ModelRequest.builder()
				.context(requestContext)
				.build();

			interceptor.interceptModel(request, handler);
			hook.afterModel(state, config).get();
		}

		// Verify state after round 1
		int hookRunCountAfterRound1 = (int) config.context().get("__model_call_limit_run_count__");
		assertEquals(2, hookRunCountAfterRound1, "After round 1, Hook count should be 2 (1 original + 1 retry)");

		// Round 2: Try to call, but should be blocked by Hook in beforeModel
		{
			OverAllState state = new OverAllState(Map.of());
			
			// Hook should throw exception because limit is already reached (2) + 1 more would exceed runLimit=3
			assertThrows(ModelCallLimitExceededException.class, () -> {
				hook.beforeModel(state, config).get();
			}, "After reaching limit, Hook should block calls in beforeModel phase");
		}

		// Verify total API call count: only 2 from round 1
		assertEquals(2, totalApiCalls.get(), "Hook limit should prevent subsequent calls");
	}
}


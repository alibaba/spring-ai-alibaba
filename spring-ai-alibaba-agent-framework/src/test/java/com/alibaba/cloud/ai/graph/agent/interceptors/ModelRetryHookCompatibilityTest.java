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

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.modelretry.ModelRetryInterceptor;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify ModelRetryInterceptor compatibility with ModelCallLimitHook
 */
class ModelRetryHookCompatibilityTest {
	
	private static final String THREAD_COUNT_KEY = "__model_call_limit_thread_count__";
	private static final String RUN_COUNT_KEY = "__model_call_limit_run_count__";

	@Test
	void testRetryUpdatesHookCounters() {
		ModelRetryInterceptor interceptor = ModelRetryInterceptor.builder()
			.maxAttempts(3)
			.initialDelay(10)
			.build();

		// Create mock RunnableConfig with counters
		RunnableConfig config = RunnableConfig.builder().build();
		config.context().put(THREAD_COUNT_KEY, 0);
		config.context().put(RUN_COUNT_KEY, 0);

		// Add config to request context
		Map<String, Object> requestContext = new HashMap<>();
		requestContext.put(ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY, config);

		AtomicInteger attemptCount = new AtomicInteger(0);
		
		ModelCallHandler handler = request -> {
			int count = attemptCount.incrementAndGet();
			if (count < 3) {
				throw new RuntimeException("I/O error: connection timeout");
			}
			return ModelResponse.of(new AssistantMessage("Success on retry"));
		};

		ModelRequest request = ModelRequest.builder()
			.context(requestContext)
			.build();
			
		ModelResponse response = interceptor.interceptModel(request, handler);

		// Verify response
		assertEquals("Success on retry", ((AssistantMessage) response.getMessage()).getText());
		
		// Verify attempts
		assertEquals(3, attemptCount.get(), "Should have made 3 attempts");
		
		// Verify Hook counters were updated for retries (2 retries)
		int finalThreadCount = (int) config.context().get(THREAD_COUNT_KEY);
		int finalRunCount = (int) config.context().get(RUN_COUNT_KEY);
		
		assertEquals(2, finalThreadCount, "Thread count should be incremented by 2 (for 2 retries)");
		assertEquals(2, finalRunCount, "Run count should be incremented by 2 (for 2 retries)");
	}

	@Test
	void testNoRetryDoesNotUpdateCounters() {
		ModelRetryInterceptor interceptor = ModelRetryInterceptor.builder()
			.maxAttempts(3)
			.initialDelay(10)
			.build();

		// Create mock RunnableConfig with counters
		RunnableConfig config = RunnableConfig.builder().build();
		config.context().put(THREAD_COUNT_KEY, 5);
		config.context().put(RUN_COUNT_KEY, 5);

		// Add config to request context
		Map<String, Object> requestContext = new HashMap<>();
		requestContext.put(ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY, config);

		ModelCallHandler handler = request -> 
			ModelResponse.of(new AssistantMessage("Success immediately"));

		ModelRequest request = ModelRequest.builder()
			.context(requestContext)
			.build();
			
		ModelResponse response = interceptor.interceptModel(request, handler);

		// Verify response
		assertEquals("Success immediately", ((AssistantMessage) response.getMessage()).getText());
		
		// Verify Hook counters were NOT updated (no retries)
		int finalThreadCount = (int) config.context().get(THREAD_COUNT_KEY);
		int finalRunCount = (int) config.context().get(RUN_COUNT_KEY);
		
		assertEquals(5, finalThreadCount, "Thread count should remain unchanged (no retries)");
		assertEquals(5, finalRunCount, "Run count should remain unchanged (no retries)");
	}

	@Test
	void testCountersIncrementCorrectlyForEachRetry() {
		ModelRetryInterceptor interceptor = ModelRetryInterceptor.builder()
			.maxAttempts(5)
			.initialDelay(10)
			.build();

		// Create mock RunnableConfig with counters
		RunnableConfig config = RunnableConfig.builder().build();
		config.context().put(THREAD_COUNT_KEY, 10);
		config.context().put(RUN_COUNT_KEY, 10);

		// Add config to request context
		Map<String, Object> requestContext = new HashMap<>();
		requestContext.put(ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY, config);

		AtomicInteger attemptCount = new AtomicInteger(0);
		
		ModelCallHandler handler = request -> {
			int count = attemptCount.incrementAndGet();
			if (count < 5) {
				throw new RuntimeException("Connection timeout");
			}
			return ModelResponse.of(new AssistantMessage("Success"));
		};

		ModelRequest request = ModelRequest.builder()
			.context(requestContext)
			.build();
			
		ModelResponse response = interceptor.interceptModel(request, handler);

		// Verify response
		assertEquals("Success", ((AssistantMessage) response.getMessage()).getText());
		
		// Verify attempts
		assertEquals(5, attemptCount.get(), "Should have made 5 attempts");
		
		// Verify Hook counters: started at 10, added 4 retries
		int finalThreadCount = (int) config.context().get(THREAD_COUNT_KEY);
		int finalRunCount = (int) config.context().get(RUN_COUNT_KEY);
		
		assertEquals(14, finalThreadCount, "Thread count should be 10 + 4 retries = 14");
		assertEquals(14, finalRunCount, "Run count should be 10 + 4 retries = 14");
	}

	@Test
	void testInterceptorWorksWithoutConfig() {
		// Test that interceptor still works even if config is not provided
		ModelRetryInterceptor interceptor = ModelRetryInterceptor.builder()
			.maxAttempts(2)
			.initialDelay(10)
			.build();

		AtomicInteger attemptCount = new AtomicInteger(0);
		
		ModelCallHandler handler = request -> {
			int count = attemptCount.incrementAndGet();
			if (count < 2) {
				throw new RuntimeException("I/O error");
			}
			return ModelResponse.of(new AssistantMessage("Success"));
		};

		// Request without config
		ModelRequest request = ModelRequest.builder().build();
			
		ModelResponse response = interceptor.interceptModel(request, handler);

		// Should still work, just won't update counters
		assertEquals("Success", ((AssistantMessage) response.getMessage()).getText());
		assertEquals(2, attemptCount.get());
	}

	@Test
	void testExceptionInCounterUpdateDoesNotBreakRetry() {
		ModelRetryInterceptor interceptor = ModelRetryInterceptor.builder()
			.maxAttempts(3)
			.initialDelay(10)
			.build();

		// Create config with invalid counter type (will cause exception)
		RunnableConfig config = RunnableConfig.builder().build();
		config.context().put(THREAD_COUNT_KEY, "invalid"); // String instead of Integer
		config.context().put(RUN_COUNT_KEY, "invalid");

		Map<String, Object> requestContext = new HashMap<>();
		requestContext.put(ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY, config);

		AtomicInteger attemptCount = new AtomicInteger(0);
		
		ModelCallHandler handler = request -> {
			int count = attemptCount.incrementAndGet();
			if (count < 2) {
				throw new RuntimeException("timeout");
			}
			return ModelResponse.of(new AssistantMessage("Success"));
		};

		ModelRequest request = ModelRequest.builder()
			.context(requestContext)
			.build();
			
		// Should not throw exception even if counter update fails
		ModelResponse response = interceptor.interceptModel(request, handler);
		
		assertEquals("Success", ((AssistantMessage) response.getMessage()).getText());
		assertEquals(2, attemptCount.get());
	}
}


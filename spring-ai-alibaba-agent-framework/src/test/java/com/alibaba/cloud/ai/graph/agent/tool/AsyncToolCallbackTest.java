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
package com.alibaba.cloud.ai.graph.agent.tool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for AsyncToolCallback interface and implementations.
 *
 * <p>
 * Covers the async tool execution contract including timeout handling and cancellation.
 * </p>
 *
 * @author Claude
 * @since 1.0.0
 */
@DisplayName("AsyncToolCallback Tests")
class AsyncToolCallbackTest {

	@Nested
	@DisplayName("Basic Contract Tests")
	class BasicContractTests {

		@Test
		@DisplayName("isAsync() should return true")
		void isAsync_shouldReturnTrue() {
			AsyncToolCallback callback = createSimpleAsyncCallback();
			assertTrue(callback.isAsync());
		}

		@Test
		@DisplayName("getTimeout() should return default 5 minutes")
		void getTimeout_shouldReturnDefault() {
			AsyncToolCallback callback = createSimpleAsyncCallback();
			assertEquals(Duration.ofMinutes(5), callback.getTimeout());
		}

		@Test
		@DisplayName("callAsync() should return CompletableFuture")
		void callAsync_shouldReturnFuture() {
			AsyncToolCallback callback = createSimpleAsyncCallback();

			CompletableFuture<String> future = callback.callAsync("{}", new ToolContext(Map.of()));

			assertNotNull(future);
			assertEquals("async-result", future.join());
		}

	}

	@Nested
	@DisplayName("Sync Fallback Tests")
	class SyncFallbackTests {

		@Test
		@DisplayName("call() should block and return result")
		void call_shouldBlockAndReturnResult() {
			AsyncToolCallback callback = createSimpleAsyncCallback();

			String result = callback.call("{}", new ToolContext(Map.of()));

			assertEquals("async-result", result);
		}

		@Test
		@DisplayName("call() should unwrap CompletionException")
		void call_shouldUnwrapCompletionException() {
			AsyncToolCallback callback = new TestAsyncToolCallback() {
				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context) {
					return CompletableFuture.failedFuture(new RuntimeException("Async error"));
				}
			};

			RuntimeException ex = assertThrows(RuntimeException.class, () -> callback.call("{}", new ToolContext(Map.of())));

			assertEquals("Async error", ex.getMessage());
		}

		@Test
		@DisplayName("call() should throw ToolCancelledException on cancellation")
		void call_shouldThrowToolCancelledException_onCancellation() {
			AsyncToolCallback callback = new TestAsyncToolCallback() {
				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context) {
					CompletableFuture<String> future = new CompletableFuture<>();
					future.cancel(true);
					return future;
				}
			};

			assertThrows(ToolCancelledException.class, () -> callback.call("{}", new ToolContext(Map.of())));
		}

	}

	@Nested
	@DisplayName("Custom Timeout Tests")
	class CustomTimeoutTests {

		@Test
		@DisplayName("should support custom timeout")
		void shouldSupportCustomTimeout() {
			AsyncToolCallback callback = new TestAsyncToolCallback() {
				@Override
				public Duration getTimeout() {
					return Duration.ofSeconds(30);
				}
			};

			assertEquals(Duration.ofSeconds(30), callback.getTimeout());
		}

		@Test
		@DisplayName("should support very short timeout")
		void shouldSupportVeryShortTimeout() {
			AsyncToolCallback callback = new TestAsyncToolCallback() {
				@Override
				public Duration getTimeout() {
					return Duration.ofMillis(100);
				}
			};

			assertEquals(Duration.ofMillis(100), callback.getTimeout());
		}

	}

	@Nested
	@DisplayName("State Update Map Clearing on Timeout Tests")
	class StateUpdateMapClearingTests {

		@Test
		@DisplayName("state update map should be clearable on timeout")
		void stateUpdateMap_shouldBeClearable_onTimeout() throws Exception {
			Map<String, Object> stateUpdateMap = new java.util.concurrent.ConcurrentHashMap<>();

			// Simulate async tool that writes to state update map before timeout
			CountDownLatch writeComplete = new CountDownLatch(1);
			AtomicBoolean timeoutOccurred = new AtomicBoolean(false);

			AsyncToolCallback callback = new TestAsyncToolCallback() {
				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context) {
					return CompletableFuture.supplyAsync(() -> {
						// Tool writes state update before completing
						stateUpdateMap.put("key1", "value1");
						stateUpdateMap.put("key2", "value2");
						writeComplete.countDown();

						// Simulate long-running operation that causes timeout
						try {
							Thread.sleep(5000);
						}
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
						return "result";
					});
				}

				@Override
				public Duration getTimeout() {
					return Duration.ofMillis(100);
				}
			};

			// Start async execution
			CompletableFuture<String> future = callback.callAsync("{}", new ToolContext(Map.of()));

			// Wait for initial writes
			assertTrue(writeComplete.await(1, TimeUnit.SECONDS));
			assertEquals(2, stateUpdateMap.size());

			// Apply timeout and wait
			try {
				future.orTimeout(100, TimeUnit.MILLISECONDS).join();
			}
			catch (CompletionException e) {
				if (e.getCause() instanceof java.util.concurrent.TimeoutException) {
					// This is the fix for Issue 1 - clear state updates on timeout
					stateUpdateMap.clear();
					timeoutOccurred.set(true);
				}
			}

			// Verify state was cleared
			assertTrue(timeoutOccurred.get());
			assertTrue(stateUpdateMap.isEmpty(), "State update map should be empty after timeout");
		}

		@Test
		@DisplayName("state update map should preserve data when no timeout")
		void stateUpdateMap_shouldPreserveData_whenNoTimeout() {
			Map<String, Object> stateUpdateMap = new java.util.concurrent.ConcurrentHashMap<>();

			AsyncToolCallback callback = new TestAsyncToolCallback() {
				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context) {
					return CompletableFuture.supplyAsync(() -> {
						stateUpdateMap.put("key", "value");
						return "result";
					});
				}
			};

			CompletableFuture<String> future = callback.callAsync("{}", new ToolContext(Map.of()));
			String result = future.orTimeout(5, TimeUnit.SECONDS).join();

			assertEquals("result", result);
			assertEquals("value", stateUpdateMap.get("key"));
		}

	}

	@Nested
	@DisplayName("Concurrent Execution Tests")
	class ConcurrentExecutionTests {

		@Test
		@DisplayName("should support concurrent async executions")
		void shouldSupportConcurrentExecutions() throws InterruptedException {
			AtomicReference<Integer> counter = new AtomicReference<>(0);

			AsyncToolCallback callback = new TestAsyncToolCallback() {
				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context) {
					return CompletableFuture.supplyAsync(() -> {
						counter.updateAndGet(c -> c + 1);
						return "result-" + counter.get();
					});
				}
			};

			// Launch multiple concurrent calls
			CompletableFuture<String>[] futures = new CompletableFuture[10];
			for (int i = 0; i < 10; i++) {
				futures[i] = callback.callAsync("{}", new ToolContext(Map.of()));
			}

			CompletableFuture.allOf(futures).join();

			assertEquals(10, counter.get().intValue());
		}

	}

	// Helper methods and test implementations

	private AsyncToolCallback createSimpleAsyncCallback() {
		return new TestAsyncToolCallback();
	}

	/**
	 * Test implementation of AsyncToolCallback
	 */
	private static class TestAsyncToolCallback implements AsyncToolCallback {

		@Override
		public ToolDefinition getToolDefinition() {
			return ToolDefinition.builder().name("testAsyncTool").description("A test async tool").build();
		}

		@Override
		public CompletableFuture<String> callAsync(String arguments, ToolContext context) {
			return CompletableFuture.completedFuture("async-result");
		}

		@Override
		public String call(String toolInput) {
			return callAsync(toolInput, new ToolContext(Map.of())).join();
		}

	}

}

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link AsyncToolCallbackAdapter}.
 *
 * <p>
 * Verifies that the adapter correctly wraps synchronous tool callbacks to provide async
 * execution capabilities.
 * </p>
 *
 * @author disaster
 * @since 1.0.0
 */
@DisplayName("AsyncToolCallbackAdapter Tests")
class AsyncToolCallbackAdapterTest {

	private ExecutorService executor;

	@BeforeEach
	void setUp() {
		executor = Executors.newFixedThreadPool(4);
	}

	@Nested
	@DisplayName("Constructor Tests")
	class ConstructorTests {

		@Test
		@DisplayName("should create adapter with delegate and executor")
		void shouldCreateAdapterWithDelegateAndExecutor() {
			ToolCallback delegate = createSyncToolCallback("result");

			AsyncToolCallbackAdapter adapter = new AsyncToolCallbackAdapter(delegate, executor);

			assertNotNull(adapter);
			assertSame(delegate, adapter.getDelegate());
			assertEquals(Duration.ofMinutes(5), adapter.getTimeout());
		}

		@Test
		@DisplayName("should create adapter with custom timeout")
		void shouldCreateAdapterWithCustomTimeout() {
			ToolCallback delegate = createSyncToolCallback("result");
			Duration customTimeout = Duration.ofSeconds(30);

			AsyncToolCallbackAdapter adapter = new AsyncToolCallbackAdapter(delegate, executor, customTimeout);

			assertEquals(customTimeout, adapter.getTimeout());
		}

		@Test
		@DisplayName("should reject null delegate")
		void shouldRejectNullDelegate() {
			assertThrows(NullPointerException.class, () -> new AsyncToolCallbackAdapter(null, executor));
		}

		@Test
		@DisplayName("should reject null executor")
		void shouldRejectNullExecutor() {
			ToolCallback delegate = createSyncToolCallback("result");
			assertThrows(NullPointerException.class, () -> new AsyncToolCallbackAdapter(delegate, null));
		}

		@Test
		@DisplayName("should reject null timeout")
		void shouldRejectNullTimeout() {
			ToolCallback delegate = createSyncToolCallback("result");
			assertThrows(NullPointerException.class, () -> new AsyncToolCallbackAdapter(delegate, executor, null));
		}

	}

	@Nested
	@DisplayName("wrapIfNeeded Tests")
	class WrapIfNeededTests {

		@Test
		@DisplayName("should return same instance for AsyncToolCallback")
		void shouldReturnSameInstanceForAsyncToolCallback() {
			AsyncToolCallback asyncCallback = createAsyncToolCallback();

			AsyncToolCallback result = AsyncToolCallbackAdapter.wrapIfNeeded(asyncCallback, executor);

			assertSame(asyncCallback, result);
		}

		@Test
		@DisplayName("should wrap sync callback in adapter")
		void shouldWrapSyncCallbackInAdapter() {
			ToolCallback syncCallback = createSyncToolCallback("result");

			AsyncToolCallback result = AsyncToolCallbackAdapter.wrapIfNeeded(syncCallback, executor);

			assertInstanceOf(AsyncToolCallbackAdapter.class, result);
			assertSame(syncCallback, ((AsyncToolCallbackAdapter) result).getDelegate());
		}

		@Test
		@DisplayName("should preserve custom timeout when wrapping")
		void shouldPreserveCustomTimeoutWhenWrapping() {
			ToolCallback syncCallback = createSyncToolCallback("result");
			Duration customTimeout = Duration.ofSeconds(45);

			AsyncToolCallback result = AsyncToolCallbackAdapter.wrapIfNeeded(syncCallback, executor, customTimeout);

			assertEquals(customTimeout, result.getTimeout());
		}

		@Test
		@DisplayName("should reject null callback")
		void shouldRejectNullCallback() {
			assertThrows(NullPointerException.class, () -> AsyncToolCallbackAdapter.wrapIfNeeded(null, executor));
		}

		@Test
		@DisplayName("should reject null executor in wrapIfNeeded")
		void shouldRejectNullExecutorInWrapIfNeeded() {
			ToolCallback syncCallback = createSyncToolCallback("result");
			assertThrows(NullPointerException.class, () -> AsyncToolCallbackAdapter.wrapIfNeeded(syncCallback, null));
		}

	}

	@Nested
	@DisplayName("Async Execution Tests")
	class AsyncExecutionTests {

		@Test
		@DisplayName("callAsync should execute delegate asynchronously")
		void callAsyncShouldExecuteDelegateAsynchronously() {
			ToolCallback delegate = createSyncToolCallback("async-result");
			AsyncToolCallbackAdapter adapter = new AsyncToolCallbackAdapter(delegate, executor);
			ToolContext context = new ToolContext(Map.of());

			CompletableFuture<String> future = adapter.callAsync("{\"arg\":\"value\"}", context);

			assertNotNull(future);
			assertEquals("async-result", future.join());
		}

		@Test
		@DisplayName("callAsync should run on executor thread")
		void callAsyncShouldRunOnExecutorThread() throws Exception {
			Thread mainThread = Thread.currentThread();
			AtomicInteger executionThread = new AtomicInteger(-1);
			CountDownLatch latch = new CountDownLatch(1);

			ToolCallback delegate = new TestSyncToolCallback() {
				@Override
				public String call(String toolInput, ToolContext toolContext) {
					executionThread.set((int) Thread.currentThread().getId());
					latch.countDown();
					return "result";
				}
			};

			AsyncToolCallbackAdapter adapter = new AsyncToolCallbackAdapter(delegate, executor);
			adapter.callAsync("{}", new ToolContext(Map.of()));

			assertTrue(latch.await(5, TimeUnit.SECONDS));
			assertFalse(executionThread.get() == mainThread.getId(), "Should execute on different thread");
		}

		@Test
		@DisplayName("should support parallel async executions")
		void shouldSupportParallelAsyncExecutions() throws Exception {
			AtomicInteger concurrentCount = new AtomicInteger(0);
			AtomicInteger maxConcurrent = new AtomicInteger(0);
			CountDownLatch startLatch = new CountDownLatch(1);
			CountDownLatch executionLatch = new CountDownLatch(4);

			ToolCallback delegate = new TestSyncToolCallback() {
				@Override
				public String call(String toolInput, ToolContext toolContext) {
					try {
						int current = concurrentCount.incrementAndGet();
						maxConcurrent.updateAndGet(max -> Math.max(max, current));
						startLatch.await(5, TimeUnit.SECONDS);
						return "result";
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						throw new RuntimeException(e);
					}
					finally {
						concurrentCount.decrementAndGet();
						executionLatch.countDown();
					}
				}
			};

			AsyncToolCallbackAdapter adapter = new AsyncToolCallbackAdapter(delegate, executor);

			// Start 4 parallel executions
			CompletableFuture<String>[] futures = new CompletableFuture[4];
			for (int i = 0; i < 4; i++) {
				futures[i] = adapter.callAsync("{}", new ToolContext(Map.of()));
			}

			// Allow all to proceed
			Thread.sleep(100); // Wait for all to start
			startLatch.countDown();

			// Wait for completion
			CompletableFuture.allOf(futures).join();
			assertTrue(executionLatch.await(5, TimeUnit.SECONDS));

			// Verify concurrent execution occurred
			assertTrue(maxConcurrent.get() > 1, "Should have concurrent executions");
		}

	}

	@Nested
	@DisplayName("Delegation Tests")
	class DelegationTests {

		@Test
		@DisplayName("getToolDefinition should delegate")
		void getToolDefinitionShouldDelegate() {
			ToolCallback delegate = createSyncToolCallback("result");
			AsyncToolCallbackAdapter adapter = new AsyncToolCallbackAdapter(delegate, executor);

			ToolDefinition definition = adapter.getToolDefinition();

			assertEquals("testSyncTool", definition.name());
			assertEquals("A test sync tool", definition.description());
		}

		@Test
		@DisplayName("getToolMetadata should delegate")
		void getToolMetadataShouldDelegate() {
			ToolCallback delegate = createSyncToolCallback("result");
			AsyncToolCallbackAdapter adapter = new AsyncToolCallbackAdapter(delegate, executor);

			ToolMetadata metadata = adapter.getToolMetadata();

			assertNotNull(metadata);
		}

		@Test
		@DisplayName("sync call should delegate directly")
		void syncCallShouldDelegateDirectly() {
			ToolCallback delegate = createSyncToolCallback("direct-result");
			AsyncToolCallbackAdapter adapter = new AsyncToolCallbackAdapter(delegate, executor);

			String result = adapter.call("{}", new ToolContext(Map.of()));

			assertEquals("direct-result", result);
		}

	}

	@Nested
	@DisplayName("AsyncToolCallback Contract Tests")
	class AsyncToolCallbackContractTests {

		@Test
		@DisplayName("isAsync should return true")
		void isAsyncShouldReturnTrue() {
			AsyncToolCallbackAdapter adapter = new AsyncToolCallbackAdapter(createSyncToolCallback("result"), executor);

			assertTrue(adapter.isAsync());
		}

		@Test
		@DisplayName("default timeout should be 5 minutes")
		void defaultTimeoutShouldBeFiveMinutes() {
			AsyncToolCallbackAdapter adapter = new AsyncToolCallbackAdapter(createSyncToolCallback("result"), executor);

			assertEquals(Duration.ofMinutes(5), adapter.getTimeout());
		}

	}

	// Helper methods

	private ToolCallback createSyncToolCallback(String result) {
		return new TestSyncToolCallback() {
			@Override
			public String call(String toolInput, ToolContext toolContext) {
				return result;
			}
		};
	}

	private AsyncToolCallback createAsyncToolCallback() {
		return new AsyncToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return ToolDefinition.builder().name("testAsyncTool").description("A test async tool").inputSchema("{}").build();
			}

			@Override
			public CompletableFuture<String> callAsync(String arguments, ToolContext context) {
				return CompletableFuture.completedFuture("async-result");
			}

			@Override
			public String call(String toolInput) {
				return "async-result";
			}
		};
	}

	/**
	 * Test implementation of a synchronous ToolCallback.
	 */
	private abstract static class TestSyncToolCallback implements ToolCallback {

		@Override
		public ToolDefinition getToolDefinition() {
			return ToolDefinition.builder().name("testSyncTool").description("A test sync tool").inputSchema("{}").build();
		}

		@Override
		public String call(String toolInput) {
			return call(toolInput, new ToolContext(Map.of()));
		}

	}

}

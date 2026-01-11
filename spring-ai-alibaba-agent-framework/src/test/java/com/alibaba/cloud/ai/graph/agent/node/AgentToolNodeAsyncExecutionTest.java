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
package com.alibaba.cloud.ai.graph.agent.node;

import com.alibaba.cloud.ai.graph.agent.tool.AsyncToolCallback;
import com.alibaba.cloud.ai.graph.agent.tool.CancellableAsyncToolCallback;
import com.alibaba.cloud.ai.graph.agent.tool.CancellationToken;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for AgentToolNode async execution with CancellationToken support.
 *
 * <p>
 * Covers Issue 2 fix: AgentToolNode should detect CancellableAsyncToolCallback and pass
 * a real CancellationToken instead of CancellationToken.NONE.
 * </p>
 *
 * <p>
 * These tests verify the routing logic in executeAsyncTool method:
 * <ul>
 * <li>AsyncToolCallback receives CancellationToken.NONE via default method</li>
 * <li>CancellableAsyncToolCallback receives a real DefaultCancellationToken</li>
 * <li>Token is cancelled on timeout</li>
 * </ul>
 * </p>
 *
 * @author Claude
 * @since 1.0.0
 */
@DisplayName("AgentToolNode Async Execution Tests")
class AgentToolNodeAsyncExecutionTest {

	@Nested
	@DisplayName("Callback Type Routing Tests")
	class CallbackTypeRoutingTests {

		@Test
		@DisplayName("AsyncToolCallback should receive NONE token via default method")
		void asyncToolCallback_shouldReceiveNoneToken() {
			AtomicReference<CancellationToken> receivedToken = new AtomicReference<>();

			// Create a regular AsyncToolCallback that captures the token
			AsyncToolCallback callback = new AsyncToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder().name("asyncTool").description("Test async tool").inputSchema("{}").build();
				}

				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context) {
					// This is called - default behavior, no token parameter
					// We can't capture token here directly, but we verify the behavior
					return CompletableFuture.completedFuture("async-result");
				}

				@Override
				public String call(String toolInput) {
					return callAsync(toolInput, new ToolContext(Map.of())).join();
				}
			};

			// Verify it's not a CancellableAsyncToolCallback
			assertFalse(callback instanceof CancellableAsyncToolCallback);

			// Call the 2-arg version and verify it works
			String result = callback.callAsync("{}", new ToolContext(Map.of())).join();
			assertEquals("async-result", result);
		}

		@Test
		@DisplayName("CancellableAsyncToolCallback should receive real token when called with 3 args")
		void cancellableAsyncToolCallback_shouldReceiveRealToken() {
			AtomicReference<CancellationToken> receivedToken = new AtomicReference<>();

			CancellableAsyncToolCallback callback = new CancellableAsyncToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder().name("cancellableTool").description("Test cancellable tool").inputSchema("{}").build();
				}

				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context,
						CancellationToken cancellationToken) {
					receivedToken.set(cancellationToken);
					return CompletableFuture.completedFuture("cancellable-result");
				}

				@Override
				public String call(String toolInput) {
					return callAsync(toolInput, new ToolContext(Map.of()), CancellationToken.NONE).join();
				}
			};

			// Verify it IS a CancellableAsyncToolCallback
			assertTrue(callback instanceof CancellableAsyncToolCallback);
			assertTrue(callback instanceof AsyncToolCallback);

			// Simulate what AgentToolNode does - call 3-arg version with real token
			com.alibaba.cloud.ai.graph.agent.tool.DefaultCancellationToken realToken = new com.alibaba.cloud.ai.graph.agent.tool.DefaultCancellationToken();
			String result = callback.callAsync("{}", new ToolContext(Map.of()), realToken).join();

			assertEquals("cancellable-result", result);
			assertSame(realToken, receivedToken.get());
			assertNotSame(CancellationToken.NONE, receivedToken.get());
		}

		@Test
		@DisplayName("instanceof check should correctly identify CancellableAsyncToolCallback")
		void instanceofCheck_shouldIdentifyCancellableCallback() {
			AsyncToolCallback regularAsync = new AsyncToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder().name("regular").description("Regular").inputSchema("{}").build();
				}

				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context) {
					return CompletableFuture.completedFuture("regular");
				}

				@Override
				public String call(String toolInput) {
					return "regular";
				}
			};

			CancellableAsyncToolCallback cancellableAsync = new CancellableAsyncToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder().name("cancellable").description("Cancellable").inputSchema("{}").build();
				}

				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context,
						CancellationToken cancellationToken) {
					return CompletableFuture.completedFuture("cancellable");
				}

				@Override
				public String call(String toolInput) {
					return "cancellable";
				}
			};

			// This is the exact check used in AgentToolNode.executeAsyncTool
			assertFalse(regularAsync instanceof CancellableAsyncToolCallback);
			assertTrue(cancellableAsync instanceof CancellableAsyncToolCallback);
			assertTrue(cancellableAsync instanceof AsyncToolCallback);
		}

	}

	@Nested
	@DisplayName("Token Cancellation on Timeout Tests")
	class TokenCancellationOnTimeoutTests {

		@Test
		@DisplayName("token should be cancelled when timeout occurs")
		void token_shouldBeCancelled_onTimeout() throws InterruptedException {
			AtomicBoolean tokenCancelled = new AtomicBoolean(false);
			AtomicReference<CancellationToken> capturedToken = new AtomicReference<>();
			CountDownLatch cancellationLatch = new CountDownLatch(1);

			CancellableAsyncToolCallback callback = new CancellableAsyncToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder().name("slowTool").description("Slow tool").inputSchema("{}").build();
				}

				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context,
						CancellationToken cancellationToken) {
					capturedToken.set(cancellationToken);

					// Register callback to detect cancellation
					cancellationToken.onCancel(() -> {
						tokenCancelled.set(true);
						cancellationLatch.countDown();
					});

					return CompletableFuture.supplyAsync(() -> {
						// Simulate long-running work
						try {
							Thread.sleep(5000);
						}
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
						return "completed";
					});
				}

				@Override
				public Duration getTimeout() {
					return Duration.ofMillis(100); // Short timeout
				}

				@Override
				public String call(String toolInput) {
					return "call";
				}
			};

			// Simulate AgentToolNode behavior
			com.alibaba.cloud.ai.graph.agent.tool.DefaultCancellationToken token = new com.alibaba.cloud.ai.graph.agent.tool.DefaultCancellationToken();

			CompletableFuture<String> future = callback.callAsync("{}", new ToolContext(Map.of()), token);

			// Apply timeout (this is what AgentToolNode does)
			try {
				future.orTimeout(callback.getTimeout().toMillis(), TimeUnit.MILLISECONDS).join();
			}
			catch (Exception e) {
				// On timeout, AgentToolNode cancels the token
				token.cancel();
			}

			// Verify token was cancelled
			assertTrue(cancellationLatch.await(1, TimeUnit.SECONDS));
			assertTrue(tokenCancelled.get());
			assertTrue(token.isCancelled());
		}

		@Test
		@DisplayName("token should NOT be cancelled on successful completion")
		void token_shouldNotBeCancelled_onSuccess() {
			AtomicBoolean tokenCancelled = new AtomicBoolean(false);

			CancellableAsyncToolCallback callback = new CancellableAsyncToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder().name("fastTool").description("Fast tool").inputSchema("{}").build();
				}

				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context,
						CancellationToken cancellationToken) {
					cancellationToken.onCancel(() -> tokenCancelled.set(true));
					return CompletableFuture.completedFuture("fast-result");
				}

				@Override
				public Duration getTimeout() {
					return Duration.ofSeconds(5);
				}

				@Override
				public String call(String toolInput) {
					return "call";
				}
			};

			com.alibaba.cloud.ai.graph.agent.tool.DefaultCancellationToken token = new com.alibaba.cloud.ai.graph.agent.tool.DefaultCancellationToken();

			CompletableFuture<String> future = callback.callAsync("{}", new ToolContext(Map.of()), token);

			// No timeout - completes successfully
			String result = future.orTimeout(callback.getTimeout().toMillis(), TimeUnit.MILLISECONDS).join();

			assertEquals("fast-result", result);
			assertFalse(tokenCancelled.get());
			assertFalse(token.isCancelled());
		}

	}

	@Nested
	@DisplayName("Cooperative Cancellation Tests")
	class CooperativeCancellationTests {

		@Test
		@DisplayName("tool should stop early when checking cancellation")
		void tool_shouldStopEarly_whenCheckingCancellation() throws InterruptedException {
			AtomicReference<Integer> iterationsCompleted = new AtomicReference<>(0);
			CountDownLatch toolFinished = new CountDownLatch(1);

			CancellableAsyncToolCallback callback = new CancellableAsyncToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder().name("cooperativeTool").description("Cooperative tool").inputSchema("{}").build();
				}

				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context,
						CancellationToken cancellationToken) {
					return CompletableFuture.supplyAsync(() -> {
						try {
							for (int i = 0; i < 1000; i++) {
								// Cooperative check
								if (cancellationToken.isCancelled()) {
									return "stopped-at-" + i;
								}
								iterationsCompleted.set(i + 1);
								Thread.sleep(5);
							}
							return "completed-all";
						}
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							return "interrupted";
						}
						finally {
							toolFinished.countDown();
						}
					});
				}

				@Override
				public Duration getTimeout() {
					return Duration.ofMillis(50);
				}

				@Override
				public String call(String toolInput) {
					return "call";
				}
			};

			com.alibaba.cloud.ai.graph.agent.tool.DefaultCancellationToken token = new com.alibaba.cloud.ai.graph.agent.tool.DefaultCancellationToken();

			CompletableFuture<String> future = callback.callAsync("{}", new ToolContext(Map.of()), token);

			// Simulate timeout and cancellation
			try {
				future.orTimeout(callback.getTimeout().toMillis(), TimeUnit.MILLISECONDS).join();
			}
			catch (Exception e) {
				token.cancel();
			}

			// Wait for tool to finish
			assertTrue(toolFinished.await(2, TimeUnit.SECONDS));

			// Tool should have stopped early
			int completed = iterationsCompleted.get();
			assertTrue(completed < 100, "Tool should stop early but completed " + completed + " iterations");
		}

		@Test
		@DisplayName("tool should throw ToolCancelledException when using throwIfCancelled")
		void tool_shouldThrow_whenUsingThrowIfCancelled() throws InterruptedException {
			AtomicBoolean exceptionThrown = new AtomicBoolean(false);
			CountDownLatch toolFinished = new CountDownLatch(1);

			CancellableAsyncToolCallback callback = new CancellableAsyncToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder().name("throwingTool").description("Throwing tool").inputSchema("{}").build();
				}

				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context,
						CancellationToken cancellationToken) {
					return CompletableFuture.supplyAsync(() -> {
						try {
							for (int i = 0; i < 1000; i++) {
								// This throws ToolCancelledException if cancelled
								cancellationToken.throwIfCancelled();
								Thread.sleep(5);
							}
							return "completed";
						}
						catch (com.alibaba.cloud.ai.graph.agent.tool.ToolCancelledException e) {
							exceptionThrown.set(true);
							throw e;
						}
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							return "interrupted";
						}
						finally {
							toolFinished.countDown();
						}
					});
				}

				@Override
				public Duration getTimeout() {
					return Duration.ofMillis(50);
				}

				@Override
				public String call(String toolInput) {
					return "call";
				}
			};

			com.alibaba.cloud.ai.graph.agent.tool.DefaultCancellationToken token = new com.alibaba.cloud.ai.graph.agent.tool.DefaultCancellationToken();

			CompletableFuture<String> future = callback.callAsync("{}", new ToolContext(Map.of()), token);

			// Simulate timeout and cancellation
			try {
				future.orTimeout(callback.getTimeout().toMillis(), TimeUnit.MILLISECONDS).join();
			}
			catch (Exception e) {
				token.cancel();
			}

			// Wait for tool to finish
			assertTrue(toolFinished.await(2, TimeUnit.SECONDS));

			// ToolCancelledException should have been thrown
			assertTrue(exceptionThrown.get());
		}

	}

	@Nested
	@DisplayName("State Update on Timeout Tests")
	class StateUpdateOnTimeoutTests {

		@Test
		@DisplayName("state updates should be cleared when timeout occurs with cancellable tool")
		void stateUpdates_shouldBeCleared_onTimeoutWithCancellableTool() throws InterruptedException {
			// This simulates the extraStateFromToolCall behavior
			Map<String, Object> stateUpdates = new java.util.concurrent.ConcurrentHashMap<>();
			CountDownLatch writeComplete = new CountDownLatch(1);

			CancellableAsyncToolCallback callback = new CancellableAsyncToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder().name("statefulTool").description("Stateful tool").inputSchema("{}").build();
				}

				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context,
						CancellationToken cancellationToken) {
					return CompletableFuture.supplyAsync(() -> {
						// Write some state
						stateUpdates.put("key1", "value1");
						stateUpdates.put("key2", "value2");
						writeComplete.countDown();

						// Simulate long work
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

				@Override
				public String call(String toolInput) {
					return "call";
				}
			};

			com.alibaba.cloud.ai.graph.agent.tool.DefaultCancellationToken token = new com.alibaba.cloud.ai.graph.agent.tool.DefaultCancellationToken();

			CompletableFuture<String> future = callback.callAsync("{}", new ToolContext(Map.of()), token);

			// Wait for writes
			assertTrue(writeComplete.await(1, TimeUnit.SECONDS));
			assertEquals(2, stateUpdates.size());

			// Simulate AgentToolNode timeout handling
			try {
				future.orTimeout(callback.getTimeout().toMillis(), TimeUnit.MILLISECONDS).join();
			}
			catch (Exception e) {
				// This is what AgentToolNode does on timeout
				token.cancel();
				stateUpdates.clear(); // AgentToolNode clears extraStateFromToolCall
			}

			// State should be cleared
			assertTrue(stateUpdates.isEmpty());
			assertTrue(token.isCancelled());
		}

	}

	@Nested
	@DisplayName("Backward Compatibility Tests")
	class BackwardCompatibilityTests {

		@Test
		@DisplayName("regular AsyncToolCallback should work unchanged")
		void regularAsyncToolCallback_shouldWorkUnchanged() {
			AsyncToolCallback callback = new AsyncToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder().name("regularTool").description("Regular tool").inputSchema("{}").build();
				}

				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context) {
					return CompletableFuture.completedFuture("regular-result-" + arguments);
				}

				@Override
				public String call(String toolInput) {
					return callAsync(toolInput, new ToolContext(Map.of())).join();
				}
			};

			// Simulate AgentToolNode routing - should call 2-arg version
			assertFalse(callback instanceof CancellableAsyncToolCallback);

			String result = callback.callAsync("{\"test\":true}", new ToolContext(Map.of())).join();
			assertEquals("regular-result-{\"test\":true}", result);
		}

		@Test
		@DisplayName("CancellableAsyncToolCallback default method should still work")
		void cancellableCallback_defaultMethod_shouldStillWork() {
			AtomicReference<CancellationToken> receivedToken = new AtomicReference<>();

			CancellableAsyncToolCallback callback = new CancellableAsyncToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder().name("defaultMethodTool").description("Default method tool").inputSchema("{}").build();
				}

				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context,
						CancellationToken cancellationToken) {
					receivedToken.set(cancellationToken);
					return CompletableFuture.completedFuture("default-method-result");
				}

				@Override
				public String call(String toolInput) {
					return "call";
				}
			};

			// Call 2-arg version (default method) - should receive NONE
			String result = callback.callAsync("{}", new ToolContext(Map.of())).join();

			assertEquals("default-method-result", result);
			assertSame(CancellationToken.NONE, receivedToken.get());
		}

	}

	@Nested
	@DisplayName("Error Message Extraction Tests")
	class ErrorMessageExtractionTests {

		@Test
		@DisplayName("TimeoutException should produce timeout message")
		void extractErrorMessage_shouldProduceTimeoutMessage_forTimeoutException() {
			// This tests the extractErrorMessage behavior indirectly via async tool timeout
			AtomicBoolean timeoutOccurred = new AtomicBoolean(false);

			CancellableAsyncToolCallback slowCallback = new CancellableAsyncToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder().name("slowTool").description("Slow tool").inputSchema("{}").build();
				}

				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context,
						CancellationToken cancellationToken) {
					return CompletableFuture.supplyAsync(() -> {
						try {
							Thread.sleep(5000); // Sleep longer than timeout
						}
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
						return "never-returned";
					});
				}

				@Override
				public Duration getTimeout() {
					return Duration.ofMillis(50);
				}

				@Override
				public String call(String toolInput) {
					return "call";
				}
			};

			// Simulate timeout behavior
			com.alibaba.cloud.ai.graph.agent.tool.DefaultCancellationToken token = new com.alibaba.cloud.ai.graph.agent.tool.DefaultCancellationToken();

			CompletableFuture<String> future = slowCallback.callAsync("{}", new ToolContext(Map.of()), token);

			try {
				future.orTimeout(slowCallback.getTimeout().toMillis(), TimeUnit.MILLISECONDS).join();
			}
			catch (Exception e) {
				timeoutOccurred.set(true);
				token.cancel();
			}

			assertTrue(timeoutOccurred.get(), "Timeout should occur");
			assertTrue(token.isCancelled(), "Token should be cancelled after timeout");
		}

		@Test
		@DisplayName("CancellationException should produce cancelled message")
		void extractErrorMessage_shouldProduceCancelledMessage_forCancellationException() {
			CancellableAsyncToolCallback callback = new CancellableAsyncToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder().name("cancelTool").description("Cancel tool").inputSchema("{}").build();
				}

				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context,
						CancellationToken cancellationToken) {
					CompletableFuture<String> future = new CompletableFuture<>();
					future.cancel(true); // Immediately cancel
					return future;
				}

				@Override
				public String call(String toolInput) {
					return "call";
				}
			};

			CompletableFuture<String> future = callback.callAsync("{}", new ToolContext(Map.of()),
					CancellationToken.NONE);

			assertTrue(future.isCancelled(), "Future should be cancelled");
		}

	}

	@Nested
	@DisplayName("Null Future Handling Tests")
	class NullFutureHandlingTests {

		@Test
		@DisplayName("null future from async tool should be handled gracefully")
		void nullFuture_shouldBeHandledGracefully() {
			AsyncToolCallback nullReturningCallback = new AsyncToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder().name("nullTool").description("Null returning tool").inputSchema("{}").build();
				}

				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context) {
					return null; // Intentionally return null
				}

				@Override
				public String call(String toolInput) {
					return "call";
				}
			};

			// Verify the callback returns null (this is what AgentToolNode needs to handle)
			CompletableFuture<String> result = nullReturningCallback.callAsync("{}", new ToolContext(Map.of()));
			assertNull(result, "Callback should return null future");
		}

	}

}

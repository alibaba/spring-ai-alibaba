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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for CancellableAsyncToolCallback interface and CancellationToken integration.
 *
 * <p>
 * Covers Issue 2 fix: CancellationToken should be properly passed to tools that implement
 * CancellableAsyncToolCallback.
 * </p>
 *
 * @author Claude
 * @since 1.0.0
 */
@DisplayName("CancellableAsyncToolCallback Tests")
class CancellableAsyncToolCallbackTest {

	@Nested
	@DisplayName("Interface Contract Tests")
	class InterfaceContractTests {

		@Test
		@DisplayName("should extend AsyncToolCallback")
		void shouldExtendAsyncToolCallback() {
			CancellableAsyncToolCallback callback = createSimpleCancellableCallback();
			assertTrue(callback instanceof AsyncToolCallback);
		}

		@Test
		@DisplayName("default callAsync(args, ctx) should delegate to 3-arg version with NONE token")
		void defaultCallAsync_shouldDelegateWithNoneToken() {
			AtomicReference<CancellationToken> receivedToken = new AtomicReference<>();

			CancellableAsyncToolCallback callback = new TestCancellableCallback() {
				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context,
						CancellationToken cancellationToken) {
					receivedToken.set(cancellationToken);
					return CompletableFuture.completedFuture("result");
				}
			};

			// Call the 2-arg version (default method)
			callback.callAsync("{}", new ToolContext(Map.of())).join();

			// Should receive CancellationToken.NONE
			assertSame(CancellationToken.NONE, receivedToken.get());
		}

		@Test
		@DisplayName("3-arg callAsync should receive actual token when called directly")
		void threeArgCallAsync_shouldReceiveActualToken() {
			AtomicReference<CancellationToken> receivedToken = new AtomicReference<>();
			DefaultCancellationToken realToken = new DefaultCancellationToken();

			CancellableAsyncToolCallback callback = new TestCancellableCallback() {
				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context,
						CancellationToken cancellationToken) {
					receivedToken.set(cancellationToken);
					return CompletableFuture.completedFuture("result");
				}
			};

			// Call the 3-arg version directly
			callback.callAsync("{}", new ToolContext(Map.of()), realToken).join();

			// Should receive the actual token
			assertSame(realToken, receivedToken.get());
			assertNotSame(CancellationToken.NONE, receivedToken.get());
		}

	}

	@Nested
	@DisplayName("Cancellation Behavior Tests")
	class CancellationBehaviorTests {

		@Test
		@DisplayName("tool should be able to check cancellation status")
		void tool_shouldCheckCancellationStatus() throws InterruptedException {
			AtomicBoolean sawCancellation = new AtomicBoolean(false);
			CountDownLatch toolStarted = new CountDownLatch(1);
			CountDownLatch cancellationChecked = new CountDownLatch(1);

			DefaultCancellationToken token = new DefaultCancellationToken();

			CancellableAsyncToolCallback callback = new TestCancellableCallback() {
				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context,
						CancellationToken cancellationToken) {
					return CompletableFuture.supplyAsync(() -> {
						toolStarted.countDown();

						// Wait for external cancellation
						try {
							Thread.sleep(100);
						}
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}

						// Check cancellation
						sawCancellation.set(cancellationToken.isCancelled());
						cancellationChecked.countDown();

						return "result";
					});
				}
			};

			// Start the tool
			CompletableFuture<String> future = callback.callAsync("{}", new ToolContext(Map.of()), token);

			// Wait for tool to start
			assertTrue(toolStarted.await(1, TimeUnit.SECONDS));

			// Cancel the token
			token.cancel();

			// Wait for tool to check cancellation
			assertTrue(cancellationChecked.await(1, TimeUnit.SECONDS));

			// Tool should have seen the cancellation
			assertTrue(sawCancellation.get());
		}

		@Test
		@DisplayName("tool should be able to throw on cancellation check")
		void tool_shouldThrowOnCancellationCheck() {
			DefaultCancellationToken token = new DefaultCancellationToken();
			token.cancel(); // Pre-cancel

			CancellableAsyncToolCallback callback = new TestCancellableCallback() {
				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context,
						CancellationToken cancellationToken) {
					return CompletableFuture.supplyAsync(() -> {
						// This should throw ToolCancelledException
						cancellationToken.throwIfCancelled();
						return "result";
					});
				}
			};

			CompletableFuture<String> future = callback.callAsync("{}", new ToolContext(Map.of()), token);

			// Wait for completion and check it completed exceptionally
			try {
				future.join();
				// Should not reach here
				assertTrue(false, "Expected exception to be thrown");
			}
			catch (Exception e) {
				// Should complete exceptionally with ToolCancelledException wrapped in CompletionException
				assertTrue(e instanceof java.util.concurrent.CompletionException);
				assertTrue(e.getCause() instanceof ToolCancelledException);
			}
		}

		@Test
		@DisplayName("onCancel callback should be invoked when token is cancelled")
		void onCancelCallback_shouldBeInvoked() throws InterruptedException {
			AtomicBoolean callbackInvoked = new AtomicBoolean(false);
			CountDownLatch callbackLatch = new CountDownLatch(1);

			DefaultCancellationToken token = new DefaultCancellationToken();

			CancellableAsyncToolCallback callback = new TestCancellableCallback() {
				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context,
						CancellationToken cancellationToken) {
					// Register cleanup callback
					cancellationToken.onCancel(() -> {
						callbackInvoked.set(true);
						callbackLatch.countDown();
					});

					return CompletableFuture.supplyAsync(() -> {
						try {
							Thread.sleep(5000); // Long-running operation
						}
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
						return "result";
					});
				}
			};

			// Start the tool
			callback.callAsync("{}", new ToolContext(Map.of()), token);

			// Cancel
			token.cancel();

			// Callback should be invoked
			assertTrue(callbackLatch.await(1, TimeUnit.SECONDS));
			assertTrue(callbackInvoked.get());
		}

	}

	@Nested
	@DisplayName("CancellationToken.NONE Tests")
	class NoneTokenTests {

		@Test
		@DisplayName("NONE token should never be cancelled")
		void noneToken_shouldNeverBeCancelled() {
			assertFalse(CancellationToken.NONE.isCancelled());
		}

		@Test
		@DisplayName("NONE token throwIfCancelled should not throw")
		void noneToken_throwIfCancelled_shouldNotThrow() {
			// Should not throw
			CancellationToken.NONE.throwIfCancelled();
		}

		@Test
		@DisplayName("NONE token onCancel should be no-op")
		void noneToken_onCancel_shouldBeNoOp() {
			AtomicBoolean callbackInvoked = new AtomicBoolean(false);

			// Register callback - should be ignored
			CancellationToken.NONE.onCancel(() -> callbackInvoked.set(true));

			// Callback should never be invoked
			assertFalse(callbackInvoked.get());
		}

	}

	@Nested
	@DisplayName("DefaultCancellationToken Tests")
	class DefaultCancellationTokenTests {

		@Test
		@DisplayName("should start uncancelled")
		void shouldStartUncancelled() {
			DefaultCancellationToken token = new DefaultCancellationToken();
			assertFalse(token.isCancelled());
		}

		@Test
		@DisplayName("cancel() should set cancelled state")
		void cancel_shouldSetCancelledState() {
			DefaultCancellationToken token = new DefaultCancellationToken();

			token.cancel();

			assertTrue(token.isCancelled());
		}

		@Test
		@DisplayName("cancel() should be idempotent")
		void cancel_shouldBeIdempotent() {
			DefaultCancellationToken token = new DefaultCancellationToken();
			AtomicReference<Integer> callCount = new AtomicReference<>(0);

			token.onCancel(() -> callCount.updateAndGet(c -> c + 1));

			// Cancel multiple times
			token.cancel();
			token.cancel();
			token.cancel();

			// Callback should only be invoked once
			assertEquals(1, callCount.get().intValue());
		}

		@Test
		@DisplayName("onCancel callback registered after cancel should be invoked immediately")
		void onCancelAfterCancel_shouldInvokeImmediately() {
			DefaultCancellationToken token = new DefaultCancellationToken();
			token.cancel();

			AtomicBoolean callbackInvoked = new AtomicBoolean(false);
			token.onCancel(() -> callbackInvoked.set(true));

			assertTrue(callbackInvoked.get());
		}

		@Test
		@DisplayName("linkedTo should create token that cancels when future is cancelled")
		void linkedTo_shouldCancelWhenFutureIsCancelled() throws InterruptedException {
			CompletableFuture<String> future = new CompletableFuture<>();
			DefaultCancellationToken token = DefaultCancellationToken.linkedTo(future);

			AtomicBoolean cancelled = new AtomicBoolean(false);
			CountDownLatch latch = new CountDownLatch(1);
			token.onCancel(() -> {
				cancelled.set(true);
				latch.countDown();
			});

			// Cancel the future
			future.cancel(true);

			// Token should be cancelled
			assertTrue(latch.await(1, TimeUnit.SECONDS));
			assertTrue(cancelled.get());
			assertTrue(token.isCancelled());
		}

	}

	@Nested
	@DisplayName("Timeout Scenario Tests")
	class TimeoutScenarioTests {

		@Test
		@DisplayName("simulated timeout should trigger cancellation callback")
		void simulatedTimeout_shouldTriggerCancellationCallback() throws InterruptedException {
			AtomicBoolean cleanupPerformed = new AtomicBoolean(false);
			CountDownLatch cleanupLatch = new CountDownLatch(1);

			DefaultCancellationToken token = new DefaultCancellationToken();

			CancellableAsyncToolCallback callback = new TestCancellableCallback() {
				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context,
						CancellationToken cancellationToken) {
					// Register cleanup
					cancellationToken.onCancel(() -> {
						cleanupPerformed.set(true);
						cleanupLatch.countDown();
					});

					return CompletableFuture.supplyAsync(() -> {
						// Simulate long-running work
						for (int i = 0; i < 100; i++) {
							if (cancellationToken.isCancelled()) {
								return "cancelled";
							}
							try {
								Thread.sleep(50);
							}
							catch (InterruptedException e) {
								Thread.currentThread().interrupt();
								return "interrupted";
							}
						}
						return "completed";
					});
				}

				@Override
				public Duration getTimeout() {
					return Duration.ofMillis(100);
				}
			};

			// Start the tool
			CompletableFuture<String> future = callback.callAsync("{}", new ToolContext(Map.of()), token);

			// Simulate what AgentToolNode does on timeout
			try {
				future.orTimeout(100, TimeUnit.MILLISECONDS).join();
			}
			catch (Exception e) {
				// Timeout occurred - cancel the token (this is what AgentToolNode now does)
				token.cancel();
			}

			// Cleanup should be performed
			assertTrue(cleanupLatch.await(1, TimeUnit.SECONDS));
			assertTrue(cleanupPerformed.get());
		}

		@Test
		@DisplayName("tool should stop gracefully when checking cancellation")
		void tool_shouldStopGracefully_whenCheckingCancellation() throws InterruptedException {
			AtomicReference<Integer> iterationsCompleted = new AtomicReference<>(0);
			CountDownLatch toolFinished = new CountDownLatch(1);

			DefaultCancellationToken token = new DefaultCancellationToken();

			CancellableAsyncToolCallback callback = new TestCancellableCallback() {
				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context,
						CancellationToken cancellationToken) {
					return CompletableFuture.supplyAsync(() -> {
						try {
							for (int i = 0; i < 1000; i++) {
								if (cancellationToken.isCancelled()) {
									return "stopped-at-" + i;
								}
								iterationsCompleted.set(i);
								Thread.sleep(10);
							}
							return "completed";
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
			};

			// Start the tool
			CompletableFuture<String> future = callback.callAsync("{}", new ToolContext(Map.of()), token);

			// Let it run for a bit
			Thread.sleep(50);

			// Cancel
			token.cancel();

			// Wait for tool to finish
			assertTrue(toolFinished.await(1, TimeUnit.SECONDS));

			// Tool should have stopped early (not completed all 1000 iterations)
			assertTrue(iterationsCompleted.get() < 100, "Tool should have stopped early, but completed " + iterationsCompleted.get() + " iterations");
		}

	}

	// Helper methods and test implementations

	private CancellableAsyncToolCallback createSimpleCancellableCallback() {
		return new TestCancellableCallback();
	}

	/**
	 * Test implementation of CancellableAsyncToolCallback
	 */
	private static class TestCancellableCallback implements CancellableAsyncToolCallback {

		@Override
		public ToolDefinition getToolDefinition() {
			return ToolDefinition.builder().name("testCancellableTool").description("A test cancellable tool").build();
		}

		@Override
		public CompletableFuture<String> callAsync(String arguments, ToolContext context,
				CancellationToken cancellationToken) {
			return CompletableFuture.completedFuture("cancellable-result");
		}

		@Override
		public String call(String toolInput) {
			return callAsync(toolInput, new ToolContext(Map.of()), CancellationToken.NONE).join();
		}

	}

}

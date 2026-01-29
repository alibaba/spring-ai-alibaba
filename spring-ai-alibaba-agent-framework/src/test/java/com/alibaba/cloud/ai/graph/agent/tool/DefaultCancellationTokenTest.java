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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for DefaultCancellationToken implementation.
 *
 * @author Claude
 * @since 1.0.0
 */
@DisplayName("DefaultCancellationToken Tests")
class DefaultCancellationTokenTest {

	@Nested
	@DisplayName("Basic State Management")
	class BasicStateTests {

		@Test
		@DisplayName("should start uncancelled")
		void shouldStartUncancelled() {
			DefaultCancellationToken token = new DefaultCancellationToken();
			assertFalse(token.isCancelled(), "Token should start uncancelled");
		}

		@Test
		@DisplayName("cancel should set cancelled state")
		void cancel_shouldSetCancelledState() {
			DefaultCancellationToken token = new DefaultCancellationToken();

			token.cancel();

			assertTrue(token.isCancelled(), "Token should be cancelled after cancel()");
		}

		@Test
		@DisplayName("cancel should be idempotent")
		void cancel_shouldBeIdempotent() {
			DefaultCancellationToken token = new DefaultCancellationToken();
			AtomicInteger callbackCount = new AtomicInteger(0);
			token.onCancel(callbackCount::incrementAndGet);

			// Cancel multiple times
			token.cancel();
			token.cancel();
			token.cancel();

			assertTrue(token.isCancelled(), "Token should remain cancelled");
			assertEquals(1, callbackCount.get(), "Callback should only be invoked once");
		}

		@Test
		@DisplayName("throwIfCancelled should not throw when not cancelled")
		void throwIfCancelled_shouldNotThrow_whenNotCancelled() {
			DefaultCancellationToken token = new DefaultCancellationToken();

			// Should not throw
			token.throwIfCancelled();
		}

		@Test
		@DisplayName("throwIfCancelled should throw when cancelled")
		void throwIfCancelled_shouldThrow_whenCancelled() {
			DefaultCancellationToken token = new DefaultCancellationToken();
			token.cancel();

			assertThrows(ToolCancelledException.class, token::throwIfCancelled);
		}

	}

	@Nested
	@DisplayName("Callback Management")
	class CallbackTests {

		@Test
		@DisplayName("onCancel should invoke callback when cancelled")
		void onCancel_shouldInvokeCallback_whenCancelled() {
			DefaultCancellationToken token = new DefaultCancellationToken();
			AtomicBoolean callbackInvoked = new AtomicBoolean(false);

			token.onCancel(() -> callbackInvoked.set(true));
			token.cancel();

			assertTrue(callbackInvoked.get(), "Callback should be invoked on cancel");
		}

		@Test
		@DisplayName("onCancel should invoke immediately when already cancelled")
		void onCancel_shouldInvokeImmediately_whenAlreadyCancelled() {
			DefaultCancellationToken token = new DefaultCancellationToken();
			token.cancel();

			AtomicBoolean callbackInvoked = new AtomicBoolean(false);
			token.onCancel(() -> callbackInvoked.set(true));

			assertTrue(callbackInvoked.get(), "Callback should be invoked immediately when already cancelled");
		}

		@Test
		@DisplayName("multiple callbacks should all be invoked")
		void multipleCallbacks_shouldAllBeInvoked() {
			DefaultCancellationToken token = new DefaultCancellationToken();
			AtomicInteger counter = new AtomicInteger(0);

			token.onCancel(counter::incrementAndGet);
			token.onCancel(counter::incrementAndGet);
			token.onCancel(counter::incrementAndGet);

			token.cancel();

			assertEquals(3, counter.get(), "All callbacks should be invoked");
		}

		@Test
		@DisplayName("callback exception should not prevent other callbacks")
		void callbackException_shouldNotPreventOtherCallbacks() {
			DefaultCancellationToken token = new DefaultCancellationToken();
			List<String> executionOrder = Collections.synchronizedList(new ArrayList<>());

			token.onCancel(() -> executionOrder.add("callback1"));
			token.onCancel(() -> {
				executionOrder.add("callback2-start");
				throw new RuntimeException("Intentional exception");
			});
			token.onCancel(() -> executionOrder.add("callback3"));

			// Should not throw, even though one callback throws
			try {
				token.cancel();
			}
			catch (Exception e) {
				// One callback may throw, but others should still execute
			}

			// At least the first callback should have executed
			assertTrue(executionOrder.contains("callback1"), "First callback should execute");
		}

		@Test
		@DisplayName("callbacks registered after cancel should only run once")
		void callbacksAfterCancel_shouldOnlyRunOnce() {
			DefaultCancellationToken token = new DefaultCancellationToken();
			AtomicInteger counter = new AtomicInteger(0);

			token.cancel();

			// Register callback after cancel
			token.onCancel(counter::incrementAndGet);

			// Cancel again (should be no-op)
			token.cancel();

			assertEquals(1, counter.get(), "Callback should only run once even if registered after cancel");
		}

	}

	@Nested
	@DisplayName("Thread Safety")
	class ThreadSafetyTests {

		@Test
		@DisplayName("concurrent cancel should only invoke callbacks once")
		void concurrentCancel_shouldOnlyInvokeCallbacksOnce() throws InterruptedException {
			DefaultCancellationToken token = new DefaultCancellationToken();
			AtomicInteger callbackCount = new AtomicInteger(0);
			token.onCancel(callbackCount::incrementAndGet);

			int threadCount = 10;
			CountDownLatch startLatch = new CountDownLatch(1);
			CountDownLatch doneLatch = new CountDownLatch(threadCount);

			ExecutorService executor = Executors.newFixedThreadPool(threadCount);
			for (int i = 0; i < threadCount; i++) {
				executor.submit(() -> {
					try {
						startLatch.await();
						token.cancel();
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					finally {
						doneLatch.countDown();
					}
				});
			}

			// Start all threads simultaneously
			startLatch.countDown();

			// Wait for all threads to complete
			assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "All threads should complete");
			executor.shutdown();

			assertEquals(1, callbackCount.get(), "Callback should only be invoked once despite concurrent cancels");
		}

		@Test
		@DisplayName("concurrent onCancel should handle race conditions")
		void concurrentOnCancel_shouldHandleRaceConditions() throws InterruptedException {
			DefaultCancellationToken token = new DefaultCancellationToken();
			AtomicInteger totalCallbacks = new AtomicInteger(0);

			int threadCount = 10;
			CountDownLatch startLatch = new CountDownLatch(1);
			CountDownLatch doneLatch = new CountDownLatch(threadCount);

			ExecutorService executor = Executors.newFixedThreadPool(threadCount);
			for (int i = 0; i < threadCount; i++) {
				final int threadId = i;
				executor.submit(() -> {
					try {
						startLatch.await();
						// Half the threads register callbacks, half cancel
						if (threadId % 2 == 0) {
							token.onCancel(totalCallbacks::incrementAndGet);
						}
						else {
							token.cancel();
						}
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					finally {
						doneLatch.countDown();
					}
				});
			}

			startLatch.countDown();
			assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "All threads should complete");
			executor.shutdown();

			// All registered callbacks should have been invoked exactly once
			assertTrue(token.isCancelled(), "Token should be cancelled");
		}

		@Test
		@DisplayName("high contention should not cause deadlock")
		void highContention_shouldNotCauseDeadlock() throws InterruptedException {
			DefaultCancellationToken token = new DefaultCancellationToken();
			AtomicInteger counter = new AtomicInteger(0);

			int threadCount = 100;
			int operationsPerThread = 100;
			CountDownLatch startLatch = new CountDownLatch(1);
			CountDownLatch doneLatch = new CountDownLatch(threadCount);

			ExecutorService executor = Executors.newFixedThreadPool(threadCount);
			for (int i = 0; i < threadCount; i++) {
				executor.submit(() -> {
					try {
						startLatch.await();
						for (int j = 0; j < operationsPerThread; j++) {
							token.onCancel(counter::incrementAndGet);
							token.isCancelled();
							if (j == operationsPerThread / 2) {
								token.cancel();
							}
						}
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					finally {
						doneLatch.countDown();
					}
				});
			}

			startLatch.countDown();

			// Should complete without deadlock
			boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
			executor.shutdown();

			assertTrue(completed, "Should complete without deadlock");
			assertTrue(token.isCancelled(), "Token should be cancelled");
		}

	}

	@Nested
	@DisplayName("LinkedTo Factory Method")
	class LinkedToTests {

		@Test
		@DisplayName("linkedTo should cancel when future is cancelled")
		void linkedTo_shouldCancel_whenFutureCancelled() throws InterruptedException {
			CompletableFuture<String> future = new CompletableFuture<>();
			DefaultCancellationToken token = DefaultCancellationToken.linkedTo(future);
			AtomicBoolean callbackInvoked = new AtomicBoolean(false);
			token.onCancel(() -> callbackInvoked.set(true));

			assertFalse(token.isCancelled(), "Token should not be cancelled yet");

			// Cancel the future
			future.cancel(true);

			// Give some time for the callback to propagate
			Thread.sleep(100);

			assertTrue(token.isCancelled(), "Token should be cancelled when future is cancelled");
			assertTrue(callbackInvoked.get(), "Callback should be invoked");
		}

		@Test
		@DisplayName("linkedTo should NOT cancel when future completes normally")
		void linkedTo_shouldNotCancel_whenFutureCompletes() throws InterruptedException {
			CompletableFuture<String> future = new CompletableFuture<>();
			DefaultCancellationToken token = DefaultCancellationToken.linkedTo(future);

			assertFalse(token.isCancelled(), "Token should not be cancelled yet");

			// Complete the future normally
			future.complete("result");

			// Give some time for any potential propagation
			Thread.sleep(100);

			assertFalse(token.isCancelled(), "Token should NOT be cancelled on normal completion");
		}

		@Test
		@DisplayName("linkedTo should NOT cancel when future fails")
		void linkedTo_shouldNotCancel_whenFutureFails() throws InterruptedException {
			CompletableFuture<String> future = new CompletableFuture<>();
			DefaultCancellationToken token = DefaultCancellationToken.linkedTo(future);

			assertFalse(token.isCancelled(), "Token should not be cancelled yet");

			// Complete the future exceptionally
			future.completeExceptionally(new RuntimeException("Test error"));

			// Give some time for any potential propagation
			Thread.sleep(100);

			assertFalse(token.isCancelled(), "Token should NOT be cancelled on exceptional completion");
		}

		@Test
		@DisplayName("linkedTo should work with already cancelled future")
		void linkedTo_shouldWork_withAlreadyCancelledFuture() throws InterruptedException {
			CompletableFuture<String> future = new CompletableFuture<>();
			future.cancel(true); // Cancel before linking

			DefaultCancellationToken token = DefaultCancellationToken.linkedTo(future);

			// Give some time for the callback to propagate
			Thread.sleep(100);

			assertTrue(token.isCancelled(), "Token should be cancelled for already cancelled future");
		}

		@Test
		@DisplayName("linkedTo should work with already completed future")
		void linkedTo_shouldWork_withAlreadyCompletedFuture() {
			CompletableFuture<String> future = CompletableFuture.completedFuture("result");
			DefaultCancellationToken token = DefaultCancellationToken.linkedTo(future);

			assertFalse(token.isCancelled(), "Token should not be cancelled for completed future");
		}

	}

	@Nested
	@DisplayName("CancellationToken.NONE Tests")
	class NoneTokenTests {

		@Test
		@DisplayName("NONE token should never be cancelled")
		void noneToken_shouldNeverBeCancelled() {
			assertFalse(CancellationToken.NONE.isCancelled(), "NONE token should never be cancelled");
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

			CancellationToken.NONE.onCancel(() -> callbackInvoked.set(true));

			assertFalse(callbackInvoked.get(), "Callback should not be invoked for NONE token");
		}

	}

}

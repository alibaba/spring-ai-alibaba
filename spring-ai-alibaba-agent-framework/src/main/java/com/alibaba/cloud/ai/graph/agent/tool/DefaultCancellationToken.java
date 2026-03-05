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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default implementation of CancellationToken.
 *
 * <p>This implementation is thread-safe and supports:</p>
 * <ul>
 *   <li>Multiple callbacks registration</li>
 *   <li>Immediate callback execution if already cancelled when registered</li>
 *   <li>Linking to CompletableFuture for automatic cancellation propagation</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * DefaultCancellationToken token = new DefaultCancellationToken();
 *
 * // Register cleanup callback
 * token.onCancel(() -> cleanupResources());
 *
 * // Start async operation
 * CompletableFuture<String> future = tool.callAsync(args, ctx, token);
 *
 * // Cancel if needed
 * token.cancel();
 * }</pre>
 *
 * @author disaster
 * @since 1.0.0
 * @see CancellationToken
 */
public class DefaultCancellationToken implements CancellationToken {

	private static final class CancellationCallback {

		private final Runnable delegate;

		private final AtomicBoolean executed = new AtomicBoolean(false);

		private CancellationCallback(Runnable delegate) {
			this.delegate = delegate;
		}

		private void runOnce() {
			if (executed.compareAndSet(false, true)) {
				delegate.run();
			}
		}

	}

	private final AtomicBoolean cancelled = new AtomicBoolean(false);

	private final List<CancellationCallback> callbacks = new CopyOnWriteArrayList<>();

	@Override
	public boolean isCancelled() {
		return cancelled.get();
	}

	@Override
	public void onCancel(Runnable callback) {
		CancellationCallback cancellationCallback = new CancellationCallback(callback);
		callbacks.add(cancellationCallback);
		// If already cancelled, run immediately
		if (cancelled.get()) {
			cancellationCallback.runOnce();
		}
	}

	/**
	 * Requests cancellation and notifies all registered callbacks.
	 *
	 * <p>This method is idempotent - calling it multiple times has the same
	 * effect as calling it once. Callbacks are only invoked on the first call.</p>
	 */
	public void cancel() {
		if (cancelled.compareAndSet(false, true)) {
			callbacks.forEach(CancellationCallback::runOnce);
		}
	}

	/**
	 * Creates a cancellation token linked to a CompletableFuture.
	 * The token will be cancelled when the future is cancelled.
	 *
	 * <p>Note: This creates a one-way binding. Cancelling the token does not
	 * cancel the future. To cancel the future, call {@code future.cancel(true)}.</p>
	 *
	 * @param future the future to link to
	 * @return a new cancellation token that will be cancelled when the future is
	 * cancelled
	 */
	public static DefaultCancellationToken linkedTo(CompletableFuture<?> future) {
		DefaultCancellationToken token = new DefaultCancellationToken();
		future.whenComplete((result, error) -> {
			if (future.isCancelled()) {
				token.cancel();
			}
		});
		return token;
	}

}

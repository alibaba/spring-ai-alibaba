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

/**
 * Token for cooperative cancellation of tool execution.
 * Tools should periodically check {@link #isCancelled()} and stop gracefully.
 *
 * <p>This interface follows the cooperative cancellation pattern, where the tool
 * is responsible for checking the cancellation status and responding appropriately.
 * This allows tools to clean up resources and complete partial work before stopping.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * public CompletableFuture<String> callAsync(String args, ToolContext ctx, CancellationToken token) {
 *     return CompletableFuture.supplyAsync(() -> {
 *         for (int i = 0; i < 100; i++) {
 *             token.throwIfCancelled(); // Check for cancellation
 *             // Do work...
 *         }
 *         return "result";
 *     });
 * }
 * }</pre>
 *
 * @author disaster
 * @since 1.0.0
 * @see DefaultCancellationToken
 * @see CancellableAsyncToolCallback
 */
public interface CancellationToken {

	/**
	 * Checks if cancellation has been requested.
	 * @return true if cancelled
	 */
	boolean isCancelled();

	/**
	 * Throws ToolCancelledException if cancelled.
	 * @throws ToolCancelledException if cancellation was requested
	 */
	default void throwIfCancelled() throws ToolCancelledException {
		if (isCancelled()) {
			throw new ToolCancelledException("Tool execution was cancelled");
		}
	}

	/**
	 * Registers a callback to be invoked when cancellation is requested.
	 *
	 * <p>If cancellation has already been requested, the callback may be invoked
	 * immediately (implementation-dependent).</p>
	 *
	 * @param callback the callback to run on cancellation
	 */
	void onCancel(Runnable callback);

	/**
	 * A no-op cancellation token that is never cancelled.
	 *
	 * <p>This instance is used as a default when cancellation support is not needed.
	 * Since this token can never be cancelled, {@link #onCancel(Runnable)} is a no-op
	 * and callbacks registered with it will never be invoked.</p>
	 *
	 * <p>Use {@link DefaultCancellationToken} if you need actual cancellation support.</p>
	 */
	CancellationToken NONE = new CancellationToken() {
		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public void onCancel(Runnable callback) {
			// No-op: This token can never be cancelled, so callbacks are never invoked.
			// This is intentional - use DefaultCancellationToken if cancellation support
			// is needed.
		}
	};

}

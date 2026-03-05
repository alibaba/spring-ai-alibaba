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

import org.springframework.ai.chat.model.ToolContext;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for cancellable asynchronous tool callbacks.
 * Tools implementing this interface can respond to cancellation requests.
 *
 * <p>This interface extends {@link AsyncToolCallback} to add support for cooperative
 * cancellation. Tools should periodically check the {@link CancellationToken} and
 * stop gracefully when cancellation is requested.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * public class CancellableTool implements CancellableAsyncToolCallback {
 *
 *     @Override
 *     public ToolDefinition getToolDefinition() {
 *         return ToolDefinition.builder()
 *             .name("cancellableTool")
 *             .description("A tool that supports cancellation")
 *             .build();
 *     }
 *
 *     @Override
 *     public CompletableFuture<String> callAsync(String arguments, ToolContext context,
 *             CancellationToken cancellationToken) {
 *         return CompletableFuture.supplyAsync(() -> {
 *             for (int i = 0; i < 100; i++) {
 *                 // Check for cancellation periodically
 *                 cancellationToken.throwIfCancelled();
 *
 *                 // Do some work
 *                 processItem(i);
 *             }
 *             return "completed";
 *         });
 *     }
 * }
 * }</pre>
 *
 * @author disaster
 * @since 1.0.0
 * @see AsyncToolCallback
 * @see CancellationToken
 */
public interface CancellableAsyncToolCallback extends AsyncToolCallback {

	/**
	 * Asynchronously executes the tool with cancellation support.
	 *
	 * <p>Implementations should periodically check the cancellation token using
	 * {@link CancellationToken#isCancelled()} or {@link CancellationToken#throwIfCancelled()}
	 * and stop gracefully when cancellation is requested.</p>
	 *
	 * @param arguments the tool arguments as JSON string
	 * @param context the tool context
	 * @param cancellationToken token to check for cancellation
	 * @return a CompletableFuture with the result
	 */
	CompletableFuture<String> callAsync(String arguments, ToolContext context, CancellationToken cancellationToken);

	/**
	 * Default implementation that delegates to the cancellable version with
	 * {@link CancellationToken#NONE}.
	 *
	 * @param arguments the tool arguments as JSON string
	 * @param context the tool context
	 * @return a CompletableFuture with the result
	 */
	@Override
	default CompletableFuture<String> callAsync(String arguments, ToolContext context) {
		return callAsync(arguments, context, CancellationToken.NONE);
	}

}

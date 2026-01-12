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

import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Interface for asynchronous tool callbacks.
 * Extends StateAwareToolCallback to support state injection.
 *
 * <p>Tools implementing this interface can perform long-running operations
 * without blocking the main execution thread. The {@link #callAsync} method
 * returns a {@link CompletableFuture} that completes when the operation finishes.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * public class LongRunningTool implements AsyncToolCallback {
 *
 *     @Override
 *     public ToolDefinition getToolDefinition() {
 *         return ToolDefinition.builder()
 *             .name("longRunningTool")
 *             .description("A tool that performs long-running operations")
 *             .build();
 *     }
 *
 *     @Override
 *     public CompletableFuture<String> callAsync(String arguments, ToolContext context) {
 *         return CompletableFuture.supplyAsync(() -> {
 *             // Long-running operation
 *             try {
 *                 Thread.sleep(10000);
 *             } catch (InterruptedException e) {
 *                 Thread.currentThread().interrupt();
 *                 throw new RuntimeException("Processing interrupted", e);
 *             }
 *             return "result";
 *         });
 *     }
 *
 *     @Override
 *     public Duration getTimeout() {
 *         return Duration.ofMinutes(10);
 *     }
 * }
 * }</pre>
 *
 * @author disaster
 * @since 1.0.0
 * @see StateAwareToolCallback
 * @see CancellableAsyncToolCallback
 */
public interface AsyncToolCallback extends StateAwareToolCallback {

	/**
	 * Asynchronously executes the tool with the given arguments.
	 *
	 * @param arguments the tool arguments as JSON string
	 * @param context the tool execution context
	 * @return a CompletableFuture that will complete with the result string
	 */
	CompletableFuture<String> callAsync(String arguments, ToolContext context);

	/**
	 * Synchronous fallback implementation.
	 * Blocks until the async operation completes.
	 *
	 * <p>This method unwraps {@link CompletionException} to expose the original
	 * exception, making error handling more intuitive for callers.</p>
	 *
	 * @param arguments the tool arguments as JSON string
	 * @param context the tool execution context
	 * @return the result string
	 * @throws RuntimeException if the async operation fails
	 * @throws ToolCancelledException if the operation was cancelled
	 */
	@Override
	default String call(String arguments, ToolContext context) {
		try {
			return callAsync(arguments, context).join();
		}
		catch (CompletionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException re) {
				throw re;
			}
			if (cause instanceof Error err) {
				throw err;
			}
			throw new RuntimeException("Async tool execution failed", cause);
		}
		catch (CancellationException e) {
			throw new ToolCancelledException("Tool execution was cancelled", e);
		}
	}

	/**
	 * Returns whether this tool executes asynchronously.
	 * @return true (always async for this interface)
	 */
	default boolean isAsync() {
		return true;
	}

	/**
	 * Returns the timeout duration for this tool execution.
	 * @return the timeout duration, defaults to 5 minutes
	 */
	default Duration getTimeout() {
		return Duration.ofMinutes(5);
	}

}

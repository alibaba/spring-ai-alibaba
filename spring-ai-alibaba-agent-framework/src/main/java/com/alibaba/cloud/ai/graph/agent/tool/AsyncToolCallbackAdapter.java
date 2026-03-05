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
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Adapter that wraps synchronous {@link ToolCallback} implementations to provide
 * asynchronous execution capabilities.
 *
 * <p>This adapter enables existing synchronous tools (such as {@code @Tool} annotated methods,
 * {@code FunctionToolCallback}, {@code MethodToolCallback}, and MCP tools) to benefit from
 * the async execution infrastructure without requiring user code changes.</p>
 *
 * <p>The adapter follows the LangChain pattern of running synchronous tools in an executor
 * to achieve parallel execution. This is especially useful when multiple independent tools
 * need to execute concurrently.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Wrap a sync tool for async execution
 * ToolCallback syncTool = getSomeSyncTool();
 * AsyncToolCallback asyncTool = AsyncToolCallbackAdapter.wrapIfNeeded(syncTool, executor);
 *
 * // Use the wrapped tool in async context
 * CompletableFuture<String> future = asyncTool.callAsync(args, context);
 * }</pre>
 *
 * <p><b>Note:</b> Wrapped sync tools cannot be truly cancelled since they are blocking
 * operations. The adapter provides async semantics but cancellation will only take effect
 * after the synchronous operation completes.</p>
 *
 * @author disaster
 * @since 1.0.0
 * @see AsyncToolCallback
 * @see ToolCallback
 */
public class AsyncToolCallbackAdapter implements AsyncToolCallback {

	private final ToolCallback delegate;

	private final Executor executor;

	private final Duration timeout;

	/**
	 * Creates a new adapter with the default timeout of 5 minutes.
	 * @param delegate the synchronous tool callback to wrap
	 * @param executor the executor to use for async execution
	 */
	public AsyncToolCallbackAdapter(ToolCallback delegate, Executor executor) {
		this(delegate, executor, Duration.ofMinutes(5));
	}

	/**
	 * Creates a new adapter with a custom timeout.
	 * @param delegate the synchronous tool callback to wrap
	 * @param executor the executor to use for async execution
	 * @param timeout the timeout duration for tool execution
	 */
	public AsyncToolCallbackAdapter(ToolCallback delegate, Executor executor, Duration timeout) {
		this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
		this.executor = Objects.requireNonNull(executor, "executor must not be null");
		this.timeout = Objects.requireNonNull(timeout, "timeout must not be null");
	}

	/**
	 * Asynchronously executes the wrapped synchronous tool.
	 *
	 * <p>The synchronous tool's {@code call} method is executed in the configured
	 * executor, allowing it to run concurrently with other tools.</p>
	 * @param arguments the tool arguments as JSON string
	 * @param context the tool execution context
	 * @return a CompletableFuture that will complete with the result string
	 */
	@Override
	public CompletableFuture<String> callAsync(String arguments, ToolContext context) {
		return CompletableFuture.supplyAsync(() -> delegate.call(arguments, context), executor);
	}

	@Override
	public Duration getTimeout() {
		return timeout;
	}

	@Override
	public ToolDefinition getToolDefinition() {
		return delegate.getToolDefinition();
	}

	@Override
	public ToolMetadata getToolMetadata() {
		return delegate.getToolMetadata();
	}

	@Override
	public String call(String toolInput) {
		return delegate.call(toolInput);
	}

	@Override
	public String call(String toolInput, ToolContext toolContext) {
		return delegate.call(toolInput, toolContext);
	}

	/**
	 * Returns the wrapped delegate callback.
	 * @return the original synchronous tool callback
	 */
	public ToolCallback getDelegate() {
		return delegate;
	}

	/**
	 * Wraps a tool callback in an async adapter if it's not already async.
	 *
	 * <p>If the callback already implements {@link AsyncToolCallback}, it is returned
	 * directly without wrapping. Otherwise, a new {@link AsyncToolCallbackAdapter} is
	 * created to wrap the synchronous callback.</p>
	 * @param callback the tool callback to potentially wrap
	 * @param executor the executor to use for async execution
	 * @return an AsyncToolCallback (either the original or a wrapped adapter)
	 */
	public static AsyncToolCallback wrapIfNeeded(ToolCallback callback, Executor executor) {
		return wrapIfNeeded(callback, executor, Duration.ofMinutes(5));
	}

	/**
	 * Wraps a tool callback in an async adapter if it's not already async.
	 *
	 * <p>If the callback already implements {@link AsyncToolCallback}, it is returned
	 * directly without wrapping. Otherwise, a new {@link AsyncToolCallbackAdapter} is
	 * created to wrap the synchronous callback.</p>
	 * @param callback the tool callback to potentially wrap
	 * @param executor the executor to use for async execution
	 * @param timeout the timeout duration for wrapped tool execution
	 * @return an AsyncToolCallback (either the original or a wrapped adapter)
	 */
	public static AsyncToolCallback wrapIfNeeded(ToolCallback callback, Executor executor, Duration timeout) {
		Objects.requireNonNull(callback, "callback must not be null");
		Objects.requireNonNull(executor, "executor must not be null");
		Objects.requireNonNull(timeout, "timeout must not be null");

		if (callback instanceof AsyncToolCallback async) {
			return async;
		}
		return new AsyncToolCallbackAdapter(callback, executor, timeout);
	}

}

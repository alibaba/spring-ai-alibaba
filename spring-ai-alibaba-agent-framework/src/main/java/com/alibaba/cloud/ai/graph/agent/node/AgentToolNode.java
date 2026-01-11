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

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.NodeActionWithConfig;
import com.alibaba.cloud.ai.graph.internal.node.ParallelNode;
import com.alibaba.cloud.ai.graph.state.RemoveByHash;
import com.alibaba.cloud.ai.graph.agent.interceptor.InterceptorChain;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import com.alibaba.cloud.ai.graph.agent.tool.AsyncToolCallback;
import com.alibaba.cloud.ai.graph.agent.tool.CancellableAsyncToolCallback;
import com.alibaba.cloud.ai.graph.agent.tool.DefaultCancellationToken;
import com.alibaba.cloud.ai.graph.agent.tool.StateAwareToolCallback;
import com.alibaba.cloud.ai.graph.agent.tool.ToolCancelledException;
import com.alibaba.cloud.ai.graph.agent.tool.ToolStateCollector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.alibaba.cloud.ai.graph.RunnableConfig.AGENT_TOOL_NAME;
import static com.alibaba.cloud.ai.graph.agent.DefaultBuilder.POSSIBLE_LLM_TOOL_NAME_CHANGE_WARNING;
import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY;
import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_STATE_CONTEXT_KEY;
import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_STATE_FOR_UPDATE_CONTEXT_KEY;
import static com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver.THREAD_ID_DEFAULT;

/**
 * Node that executes tool calls from assistant messages.
 *
 * <p>
 * Supports both sequential and parallel tool execution. When parallel execution is
 * enabled, multiple tools can run concurrently up to the configured limit.
 * </p>
 *
 * <p>
 * Async tools implementing {@link AsyncToolCallback} are automatically detected and
 * executed asynchronously with proper timeout handling.
 * </p>
 *
 * @author disaster
 * @since 1.0.0
 * @see AsyncToolCallback
 * @see ToolStateCollector
 */
public class AgentToolNode implements NodeActionWithConfig {

	private static final Logger logger = LoggerFactory.getLogger(AgentToolNode.class);

	private final String agentName;

	private final boolean enableActingLog;

	private final boolean parallelToolExecution;

	private final int maxParallelTools;

	private final Duration toolExecutionTimeout;

	private List<ToolCallback> toolCallbacks;

	private Map<String, Object> toolContext;

	private List<ToolInterceptor> toolInterceptors = new ArrayList<>();

	private ToolCallbackResolver toolCallbackResolver;

	private ToolExecutionExceptionProcessor toolExecutionExceptionProcessor;

	public AgentToolNode(Builder builder) {
		this.agentName = builder.agentName;
		this.enableActingLog = builder.enableActingLog;
		this.toolCallbackResolver = builder.toolCallbackResolver;
		this.toolCallbacks = builder.toolCallbacks;
		this.toolContext = builder.toolContext;
		this.toolExecutionExceptionProcessor = builder.toolExecutionExceptionProcessor;
		this.parallelToolExecution = builder.parallelToolExecution;
		this.maxParallelTools = builder.maxParallelTools;
		this.toolExecutionTimeout = builder.toolExecutionTimeout;
	}

	public void setToolCallbacks(List<ToolCallback> toolCallbacks) {
		this.toolCallbacks = toolCallbacks;
	}

	public void setToolInterceptors(List<ToolInterceptor> toolInterceptors) {
		this.toolInterceptors = toolInterceptors;
	}

	void setToolCallbackResolver(ToolCallbackResolver toolCallbackResolver) {
		this.toolCallbackResolver = toolCallbackResolver;
	}

	public List<ToolCallback> getToolCallbacks() {
		return toolCallbacks;
	}

	@Override
	public Map<String, Object> apply(OverAllState state, RunnableConfig config) throws Exception {
		List<Message> messages = (List<Message>) state.value("messages").orElseThrow();
		Message lastMessage = messages.get(messages.size() - 1);

		if (lastMessage instanceof AssistantMessage assistantMessage) {
			List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();

			if (enableActingLog) {
				logger.info("[ThreadId {}] Agent {} acting with {} tools.", config.threadId().orElse(THREAD_ID_DEFAULT),
						agentName, toolCalls.size());
			}

			// Choose execution mode based on configuration
			if (parallelToolExecution && toolCalls.size() > 1) {
				return executeToolCallsParallel(toolCalls, state, config);
			}
			else {
				return executeToolCallsSequential(toolCalls, state, config);
			}
		}
		else if (lastMessage instanceof ToolResponseMessage toolResponseMessage) {
			return handlePartialToolResponses(toolResponseMessage, messages, state, config);
		}
		else {
			throw new IllegalStateException("Last message is neither an AssistantMessage nor a ToolResponseMessage");
		}
	}

	/**
	 * Sequential execution of tool calls (original behavior).
	 */
	private Map<String, Object> executeToolCallsSequential(List<AssistantMessage.ToolCall> toolCalls,
			OverAllState state, RunnableConfig config) {

		Map<String, Object> updatedState = new HashMap<>();
		Map<String, Object> extraStateFromToolCall = new HashMap<>();
		List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();

		for (AssistantMessage.ToolCall toolCall : toolCalls) {
			ToolCallResponse response = executeToolCallWithInterceptors(toolCall, state, config,
					extraStateFromToolCall);
			toolResponses.add(response.toToolResponse());
		}

		ToolResponseMessage toolResponseMessage = ToolResponseMessage.builder().responses(toolResponses).build();

		if (enableActingLog) {
			logger.info("[ThreadId {}] Agent {} acting returned: {}", config.threadId().orElse(THREAD_ID_DEFAULT),
					agentName, toolResponseMessage);
		}

		updatedState.put("messages", toolResponseMessage);
		updatedState.putAll(extraStateFromToolCall);
		return updatedState;
	}

	/**
	 * Parallel execution of tool calls with concurrency limiting.
	 *
	 * <p>
	 * <b>Important:</b> The state snapshot created via {@code state.snapShot()} is a
	 * shallow copy. Mutable objects within the state (like Lists) are shared. Tools
	 * should NOT directly modify collections retrieved from the state. Use the
	 * {@code extraStateFromToolCall} map to write updates.
	 * </p>
	 * @param toolCalls the tool calls to execute
	 * @param state the current state (will be snapshot for thread safety)
	 * @param config the runtime configuration
	 * @return the merged state updates from all tool executions
	 */
	private Map<String, Object> executeToolCallsParallel(List<AssistantMessage.ToolCall> toolCalls, OverAllState state,
			RunnableConfig config) {

		Executor executor = getToolExecutor(config);

		// Create state snapshot for thread safety
		// Note: snapShot() is a shallow copy, shared mutable objects (like messages List)
		// should not be directly modified by tools
		OverAllState stateSnapshot = state.snapShot().orElse(state);

		ToolStateCollector stateCollector = new ToolStateCollector(toolCalls.size(), state.keyStrategies());

		// Use AtomicReferenceArray + CAS to prevent race condition between
		// successful completion and timeout/error handling
		AtomicReferenceArray<ToolCallResponse> orderedResponses = new AtomicReferenceArray<>(toolCalls.size());
		List<Throwable> failures = java.util.Collections.synchronizedList(new ArrayList<>());

		// Use Semaphore to limit concurrency
		Semaphore semaphore = new Semaphore(maxParallelTools);

		List<CompletableFuture<Void>> futures = IntStream.range(0, toolCalls.size()).mapToObj(index -> {
			AssistantMessage.ToolCall toolCall = toolCalls.get(index);
			Map<String, Object> toolSpecificUpdate = stateCollector.createToolUpdateMap(index);

			return CompletableFuture.runAsync(() -> {
				try {
					// Acquire permit to limit concurrent executions
					semaphore.acquire();
					try {
						ToolCallResponse response = executeToolCallWithInterceptors(toolCall, stateSnapshot, config,
								toolSpecificUpdate);
						// CAS: only set if still null (not already timed out)
						orderedResponses.compareAndSet(index, null, response);
					}
					finally {
						semaphore.release();
					}
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					failures.add(e);
					// CAS: only set error if still null
					orderedResponses.compareAndSet(index, null,
							ToolCallResponse.error(toolCall.id(), toolCall.name(), "Tool execution was interrupted"));
				}
			}, executor)
				.orTimeout(toolExecutionTimeout.toMillis(), TimeUnit.MILLISECONDS)
				.exceptionally(ex -> {
					// CAS: only set error if still null (tool hasn't completed successfully)
					Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
					ToolCallResponse errorResponse = ToolCallResponse.error(toolCall.id(), toolCall.name(),
							extractErrorMessage(cause));
					if (orderedResponses.compareAndSet(index, null, errorResponse)) {
						if (cause instanceof TimeoutException) {
							stateCollector.discardToolUpdateMap(index);
						}
						failures.add(ex);
					}
					return null;
				});
		}).toList();

		// Wait for all tools to complete
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

		// Build result - collect responses from AtomicReferenceArray
		Map<String, Object> updatedState = new HashMap<>();
		List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();
		for (int i = 0; i < orderedResponses.length(); i++) {
			ToolCallResponse response = orderedResponses.get(i);
			if (response == null) {
				// Fallback: create error response for missing result (should not happen normally)
				AssistantMessage.ToolCall toolCall = toolCalls.get(i);
				response = ToolCallResponse.error(toolCall.id(), toolCall.name(),
						"Tool execution did not produce a response");
				logger.warn("Tool {} at index {} has null response, using error fallback", toolCall.name(), i);
			}
			toolResponses.add(response.toToolResponse());
		}

		updatedState.put("messages", ToolResponseMessage.builder().responses(toolResponses).build());
		updatedState.putAll(stateCollector.mergeAll());

		if (enableActingLog) {
			logger.info("[ThreadId {}] Agent {} parallel tool execution completed. {} tools, {} failures.",
					config.threadId().orElse(THREAD_ID_DEFAULT), agentName, toolCalls.size(), failures.size());
		}

		return updatedState;
	}

	/**
	 * Handle partial tool responses (ToolResponseMessage branch). Supports both parallel
	 * and sequential execution of remaining tools.
	 */
	private Map<String, Object> handlePartialToolResponses(ToolResponseMessage toolResponseMessage,
			List<Message> messages, OverAllState state, RunnableConfig config) {

		if (messages.size() < 2) {
			throw new IllegalStateException("Cannot find AssistantMessage before ToolResponseMessage");
		}

		Message secondLastMessage = messages.get(messages.size() - 2);
		if (!(secondLastMessage instanceof AssistantMessage assistantMessage)) {
			throw new IllegalStateException("Message before ToolResponseMessage is not an AssistantMessage");
		}

		List<ToolResponseMessage.ToolResponse> existingResponses = toolResponseMessage.getResponses();
		Set<String> executedToolIds = existingResponses.stream()
			.map(ToolResponseMessage.ToolResponse::id)
			.collect(Collectors.toSet());

		// Filter out tools that haven't been executed yet
		List<AssistantMessage.ToolCall> remainingToolCalls = assistantMessage.getToolCalls()
			.stream()
			.filter(tc -> !executedToolIds.contains(tc.id()))
			.toList();

		if (remainingToolCalls.isEmpty()) {
			// All tools have been executed - return empty map to avoid duplicate append
			// (toolResponseMessage is already in the messages list)
			return Map.of();
		}

		if (enableActingLog) {
			logger.info("[ThreadId {}] Agent {} acting with {} tools ({} already completed).",
					config.threadId().orElse(THREAD_ID_DEFAULT), agentName, assistantMessage.getToolCalls().size(),
					existingResponses.size());
		}

		// Execute remaining tools (supports parallel)
		Map<String, Object> newResults;
		if (parallelToolExecution && remainingToolCalls.size() > 1) {
			newResults = executeToolCallsParallel(remainingToolCalls, state, config);
		}
		else {
			newResults = executeToolCallsSequential(remainingToolCalls, state, config);
		}

		// Merge existing responses with new responses
		ToolResponseMessage newToolResponseMessage = (ToolResponseMessage) newResults.get("messages");
		List<ToolResponseMessage.ToolResponse> allResponses = new ArrayList<>(existingResponses);
		allResponses.addAll(newToolResponseMessage.getResponses());

		// Build final result
		Map<String, Object> updatedState = new HashMap<>(newResults);
		List<Object> newMessages = new ArrayList<>();
		newMessages.add(ToolResponseMessage.builder().responses(allResponses).build());
		newMessages.add(new RemoveByHash<>(toolResponseMessage));
		updatedState.put("messages", newMessages);

		if (enableActingLog) {
			logger.info("[ThreadId {}] Agent {} acting successfully returned.", config.threadId().orElse(THREAD_ID_DEFAULT),
					agentName);
		}

		return updatedState;
	}

	/**
	 * Execute a tool call with interceptor chain support. Supports both sync and async
	 * tool callbacks.
	 */
	private ToolCallResponse executeToolCallWithInterceptors(AssistantMessage.ToolCall toolCall, OverAllState state,
			RunnableConfig config, Map<String, Object> extraStateFromToolCall) {

		// Create ToolCallRequest
		ToolCallRequest request = ToolCallRequest.builder()
			.toolCall(toolCall)
			.context(config.metadata().orElse(new HashMap<>()))
			.build();

		// Create base handler that actually executes the tool
		ToolCallHandler baseHandler = req -> {
			ToolCallback toolCallback = resolve(req.getToolName());

			if (toolCallback == null) {
				logger.warn(POSSIBLE_LLM_TOOL_NAME_CHANGE_WARNING, req.getToolName());
				throw new IllegalStateException("No ToolCallback found for tool name: " + req.getToolName());
			}

			if (enableActingLog) {
				logger.info("[ThreadId {}] Agent {} acting, executing tool {}.",
						config.threadId().orElse(THREAD_ID_DEFAULT), agentName, req.getToolName());
			}

			Map<String, Object> toolContextMap = new HashMap<>(toolContext);
			toolContextMap.putAll(req.getContext());

			// Handle tools that need state injection:
			// - StateAwareToolCallback (including AsyncToolCallback)
			// - FunctionToolCallback
			// - MethodToolCallback
			if (toolCallback instanceof StateAwareToolCallback || toolCallback instanceof FunctionToolCallback<?, ?>
					|| toolCallback instanceof MethodToolCallback) {
				toolContextMap.putAll(Map.of(AGENT_STATE_CONTEXT_KEY, state, AGENT_CONFIG_CONTEXT_KEY, config,
						AGENT_STATE_FOR_UPDATE_CONTEXT_KEY, extraStateFromToolCall));
			}

			// Route to async or sync execution based on callback type
			return executeToolByType(toolCallback, req, toolContextMap, config, extraStateFromToolCall);
		};

		// Chain interceptors if any
		ToolCallHandler chainedHandler = InterceptorChain.chainToolInterceptors(toolInterceptors, baseHandler);

		// Execute the chained handler
		return chainedHandler.call(request);
	}

	/**
	 * Routes tool execution based on callback type.
	 */
	private ToolCallResponse executeToolByType(ToolCallback toolCallback, ToolCallRequest request,
			Map<String, Object> toolContextMap, RunnableConfig config, Map<String, Object> extraStateFromToolCall) {

		if (toolCallback instanceof AsyncToolCallback async) {
			return executeAsyncTool(async, request, toolContextMap, config, extraStateFromToolCall);
		}
		else {
			return executeSyncTool(toolCallback, request, toolContextMap, config);
		}
	}

	/**
	 * Execute an async tool with timeout handling.
	 *
	 * <p>
	 * Supports {@link CancellableAsyncToolCallback} for cooperative cancellation. When a
	 * tool implements this interface, a real cancellation token is created and passed to
	 * the tool. On timeout, the token is cancelled to allow the tool to stop gracefully.
	 * </p>
	 */
	private ToolCallResponse executeAsyncTool(AsyncToolCallback callback, ToolCallRequest request,
			Map<String, Object> toolContextMap, RunnableConfig config, Map<String, Object> extraStateFromToolCall) {

		ToolContext context = new ToolContext(toolContextMap);

		// Create cancellation token for cancellable tools
		DefaultCancellationToken cancellationToken = null;

		try {
			CompletableFuture<String> future;

			// Route based on callback type - use real token for cancellable tools
			if (callback instanceof CancellableAsyncToolCallback cancellable) {
				cancellationToken = new DefaultCancellationToken();
				future = cancellable.callAsync(request.getArguments(), context, cancellationToken);
			}
			else {
				future = callback.callAsync(request.getArguments(), context);
			}

			if (future == null) {
				return ToolCallResponse.error(request.getToolCallId(), request.getToolName(),
						"Async tool returned null future");
			}

			String result = future.orTimeout(callback.getTimeout().toMillis(), TimeUnit.MILLISECONDS).join();

			if (enableActingLog) {
				logger.info("[ThreadId {}] Agent {} acting, async tool {} finished",
						config.threadId().orElse(THREAD_ID_DEFAULT), agentName, request.getToolName());
				if (logger.isDebugEnabled()) {
					logger.debug("Tool {} returned: {}", request.getToolName(), result);
				}
			}

			return ToolCallResponse.of(request.getToolCallId(), request.getToolName(), result);
		}
		catch (CompletionException e) {
			Throwable cause = e.getCause() != null ? e.getCause() : e;

			// Clear state updates on timeout to prevent stale data from being merged
			if (cause instanceof TimeoutException) {
				// Cancel the token to notify the tool to stop gracefully
				if (cancellationToken != null) {
					cancellationToken.cancel();
				}
				extraStateFromToolCall.clear();
				logger.warn("Async tool {} timed out, discarding any state updates", request.getToolName());
			}

			if (cause instanceof ToolExecutionException toolExecutionException) {
				logger.error("Async tool {} execution failed, handling with processor: {}", request.getToolName(),
						toolExecutionExceptionProcessor.getClass().getName(), toolExecutionException);
				String result = toolExecutionExceptionProcessor.process(toolExecutionException);
				return ToolCallResponse.of(request.getToolCallId(), request.getToolName(), result);
			}

			logger.error("Async tool {} execution failed: {}", request.getToolName(), cause.getMessage(), cause);
			return ToolCallResponse.error(request.getToolCallId(), request.getToolName(), extractErrorMessage(cause));
		}
		catch (CancellationException e) {
			logger.error("Async tool {} execution was cancelled", request.getToolName(), e);
			return ToolCallResponse.error(request.getToolCallId(), request.getToolName(), extractErrorMessage(e));
		}
		catch (Exception e) {
			logger.error("Async tool {} execution failed: {}", request.getToolName(), e.getMessage(), e);
			return ToolCallResponse.error(request.getToolCallId(), request.getToolName(), extractErrorMessage(e));
		}
	}

	/**
	 * Execute a sync tool with exception handling.
	 */
	private ToolCallResponse executeSyncTool(ToolCallback callback, ToolCallRequest request,
			Map<String, Object> toolContextMap, RunnableConfig config) {

		ToolContext context = new ToolContext(toolContextMap);

		try {
			String result = callback.call(request.getArguments(), context);

			if (enableActingLog) {
				logger.info("[ThreadId {}] Agent {} acting, tool {} finished",
						config.threadId().orElse(THREAD_ID_DEFAULT), agentName, request.getToolName());
				if (logger.isDebugEnabled()) {
					logger.debug("Tool {} returned: {}", request.getToolName(), result);
				}
			}

			return ToolCallResponse.of(request.getToolCallId(), request.getToolName(), result);
		}
		catch (ToolExecutionException e) {
			logger.error("Tool {} execution failed, handling with processor: {}", request.getToolName(),
					toolExecutionExceptionProcessor.getClass().getName(), e);
			String result = toolExecutionExceptionProcessor.process(e);
			return ToolCallResponse.of(request.getToolCallId(), request.getToolName(), result);
		}
		catch (Exception e) {
			logger.error("Tool {} execution failed: {}", request.getToolName(), e.getMessage(), e);
			return ToolCallResponse.error(request.getToolCallId(), request.getToolName(), e);
		}
	}

	/**
	 * Extract a user-friendly error message from an exception.
	 */
	private String extractErrorMessage(Throwable error) {
		if (error instanceof TimeoutException) {
			return "Tool execution timed out";
		}
		if (error instanceof CancellationException) {
			return "Tool execution was cancelled";
		}
		if (error instanceof ToolCancelledException) {
			return "Tool execution was cancelled";
		}
		if (error instanceof InterruptedException) {
			Thread.currentThread().interrupt();
			return "Tool execution was interrupted";
		}
		return error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
	}

	/**
	 * Get the executor for tool execution from config or use default.
	 */
	private Executor getToolExecutor(RunnableConfig config) {
		return ParallelNode.getExecutor(config, AGENT_TOOL_NAME);
	}

	private ToolCallback resolve(String toolName) {
		return toolCallbacks.stream()
			.filter(callback -> callback.getToolDefinition().name().equals(toolName))
			.findFirst()
			.orElseGet(() -> toolCallbackResolver == null ? null : toolCallbackResolver.resolve(toolName));
	}

	public String getName() {
		return AGENT_TOOL_NAME;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String agentName;

		private boolean enableActingLog;

		private boolean parallelToolExecution = false;

		private int maxParallelTools = 5;

		private Duration toolExecutionTimeout = Duration.ofMinutes(5);

		private List<ToolCallback> toolCallbacks = new ArrayList<>();

		private Map<String, Object> toolContext = new HashMap<>();

		private ToolCallbackResolver toolCallbackResolver;

		private ToolExecutionExceptionProcessor toolExecutionExceptionProcessor;

		private Builder() {
		}

		public Builder agentName(String agentName) {
			this.agentName = agentName;
			return this;
		}

		public Builder enableActingLog(boolean enableActingLog) {
			this.enableActingLog = enableActingLog;
			return this;
		}

		/**
		 * Enable parallel execution of multiple tool calls. When enabled, tools will
		 * execute concurrently up to {@link #maxParallelTools} limit.
		 * @param parallel true to enable parallel execution
		 * @return this builder
		 */
		public Builder parallelToolExecution(boolean parallel) {
			this.parallelToolExecution = parallel;
			return this;
		}

		/**
		 * Set the maximum number of tools to execute in parallel. This limits concurrent
		 * tool executions to prevent resource exhaustion.
		 * @param max the maximum parallel tools (default: 5)
		 * @return this builder
		 * @throws IllegalArgumentException if max is less than 1
		 */
		public Builder maxParallelTools(int max) {
			if (max < 1) {
				throw new IllegalArgumentException("maxParallelTools must be at least 1");
			}
			this.maxParallelTools = max;
			return this;
		}

		/**
		 * Set the timeout for each tool execution.
		 * @param timeout the timeout duration (default: 5 minutes)
		 * @return this builder
		 */
		public Builder toolExecutionTimeout(Duration timeout) {
			this.toolExecutionTimeout = timeout;
			return this;
		}

		public Builder toolCallbacks(List<ToolCallback> toolCallbacks) {
			this.toolCallbacks = toolCallbacks;
			return this;
		}

		public Builder toolCallbackResolver(ToolCallbackResolver toolCallbackResolver) {
			this.toolCallbackResolver = toolCallbackResolver;
			return this;
		}

		public Builder toolContext(Map<String, Object> toolContext) {
			this.toolContext = new HashMap<>(toolContext);
			return this;
		}

		public Builder toolExecutionExceptionProcessor(
				ToolExecutionExceptionProcessor toolExecutionExceptionProcessor) {
			this.toolExecutionExceptionProcessor = toolExecutionExceptionProcessor;
			return this;
		}

		public AgentToolNode build() {
			Objects.requireNonNull(toolExecutionTimeout, "toolExecutionTimeout must not be null");
			Objects.requireNonNull(toolExecutionExceptionProcessor, "toolExecutionExceptionProcessor must not be null");
			return new AgentToolNode(this);
		}

	}

}

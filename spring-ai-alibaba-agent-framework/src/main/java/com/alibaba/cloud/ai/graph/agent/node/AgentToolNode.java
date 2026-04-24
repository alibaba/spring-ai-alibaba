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
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallExecutionContext;
import com.alibaba.cloud.ai.graph.state.RemoveByHash;
import com.alibaba.cloud.ai.graph.agent.interceptor.InterceptorChain;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import com.alibaba.cloud.ai.graph.agent.tool.AsyncToolCallback;
import com.alibaba.cloud.ai.graph.agent.tool.AsyncToolCallbackAdapter;
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
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.alibaba.cloud.ai.graph.RunnableConfig.AGENT_TOOL_NAME;
import static com.alibaba.cloud.ai.graph.agent.DefaultBuilder.POSSIBLE_LLM_TOOL_NAME_CHANGE_WARNING;
import static com.alibaba.cloud.ai.graph.agent.hook.returndirect.ReturnDirectConstants.FINISH_REASON_METADATA_KEY;
import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY;
import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_STATE_CONTEXT_KEY;
import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_STATE_FOR_UPDATE_CONTEXT_KEY;
import static com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver.THREAD_ID_DEFAULT;
import static org.springframework.ai.model.tool.ToolExecutionResult.FINISH_REASON;

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

	private final boolean wrapSyncToolsAsAsync;

	private List<ToolCallback> toolCallbacks;

	private List<ToolCallbackProvider> toolCallbackProviders = new ArrayList<>();

	private Map<String, Object> toolContext;

	private List<ToolInterceptor> toolInterceptors = new ArrayList<>();

	private ToolCallbackResolver toolCallbackResolver;

	private ToolExecutionExceptionProcessor toolExecutionExceptionProcessor;

	public AgentToolNode(Builder builder) {
		this.agentName = builder.agentName;
		this.enableActingLog = builder.enableActingLog;
		this.toolCallbackResolver = builder.toolCallbackResolver;
		this.toolCallbacks = builder.toolCallbacks;
		this.toolCallbackProviders = builder.toolCallbackProviders != null ? builder.toolCallbackProviders : new ArrayList<>();
		this.toolContext = builder.toolContext;
		this.toolExecutionExceptionProcessor = builder.toolExecutionExceptionProcessor;
		this.parallelToolExecution = builder.parallelToolExecution;
		this.maxParallelTools = builder.maxParallelTools;
		this.toolExecutionTimeout = builder.toolExecutionTimeout;
		this.wrapSyncToolsAsAsync = builder.wrapSyncToolsAsAsync;
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

	public List<ToolCallbackProvider> getToolCallbackProviders() {
		return toolCallbackProviders;
	}

	@Override
	public Map<String, Object> apply(OverAllState state, RunnableConfig config) throws Exception {
		// Dynamically resolve all tools (static + providers)
		List<ToolCallback> currentTools = resolveAllTools();
		
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
				return executeToolCallsParallel(toolCalls, state, config, currentTools);
			}
			else {
				return executeToolCallsSequential(toolCalls, state, config, currentTools);
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
	 *
	 * <p>
	 * Each tool gets its own isolated state update map. This prevents a subsequent tool's
	 * timeout from clearing state updates from previously successful tools. The isolation
	 * is achieved by:
	 * <ol>
	 * <li>Creating a new {@code ConcurrentHashMap} for each tool execution</li>
	 * <li>Immediately merging successful updates into {@code mergedUpdates}</li>
	 * <li>If a tool times out, only its isolated map is cleared (line 549), not the
	 * already-merged data</li>
	 * </ol>
	 * </p>
	 *
	 * <p>
	 * This behavior is now consistent with parallel execution mode, which uses
	 * {@link ToolStateCollector} for per-tool state isolation.
	 * </p>
	 *
	 * <h3>State Merge Semantics</h3>
	 * <p>
	 * Sequential mode uses <b>last-write-wins</b> semantics for state updates. When
	 * multiple tools write to the same key, the last tool's value overwrites previous
	 * values. This preserves the original behavior from before parallel execution was
	 * introduced.
	 * </p>
	 * <p>
	 * Note: This differs from parallel execution mode, which respects
	 * {@link com.alibaba.cloud.ai.graph.KeyStrategy} for merge operations. If you
	 * need deterministic merge behavior (e.g., APPEND for lists), use parallel execution
	 * mode instead.
	 * </p>
	 *
	 * @see #executeToolCallsParallel(List, OverAllState, RunnableConfig)
	 */
	/**
	 * Dynamically resolve all tools from static toolCallbacks and toolCallbackProviders.
	 * This method is called on each tool execution to support dynamic tool discovery (e.g., MCP).
	 *
	 * @return combined list of all available tools
	 */
	private List<ToolCallback> resolveAllTools() {
		List<ToolCallback> allTools = new ArrayList<>(this.toolCallbacks);
		
		// Dynamically get tools from providers
		for (ToolCallbackProvider provider : toolCallbackProviders) {
			ToolCallback[] providerTools = provider.getToolCallbacks();
			if (providerTools != null && providerTools.length > 0) {
				allTools.addAll(List.of(providerTools));
			}
		}
		
		return allTools;
	}

	private Map<String, Object> executeToolCallsSequential(List<AssistantMessage.ToolCall> toolCalls,
			OverAllState state, RunnableConfig config, List<ToolCallback> currentTools) {

		Map<String, Object> updatedState = new HashMap<>();
		Map<String, Object> mergedUpdates = new HashMap<>(); // Accumulated results from successful tools
		List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();

		Boolean returnDirect = null;
		for (AssistantMessage.ToolCall toolCall : toolCalls) {
			// Each tool gets its own isolated update map
			// If this tool times out, clear() only affects this map, not mergedUpdates
			Map<String, Object> toolSpecificUpdate = new ConcurrentHashMap<>();
			ToolCallResponse response = executeToolCallWithInterceptors(toolCall, state, config, toolSpecificUpdate,
					false, null, -1, currentTools);
			toolResponses.add(response.toToolResponse());
			returnDirect = shouldReturnDirect(toolCall, returnDirect, config, currentTools);
			// Merge immediately - subsequent timeout clear() won't affect already-merged data
			mergedUpdates.putAll(toolSpecificUpdate);
		}

		ToolResponseMessage.Builder builder = ToolResponseMessage.builder()
				.responses(toolResponses);
		if (returnDirect != null && returnDirect) {
			builder.metadata(Map.of(FINISH_REASON_METADATA_KEY, FINISH_REASON));
		}
		ToolResponseMessage toolResponseMessage = builder.build();

		if (enableActingLog) {
			logger.info("[ThreadId {}] Agent {} acting returned: {}", config.threadId().orElse(THREAD_ID_DEFAULT),
					agentName, toolResponseMessage);
		}

		updatedState.put("messages", toolResponseMessage);
		updatedState.putAll(mergedUpdates);
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
	 *
	 * <h3>State Merge Semantics</h3>
	 * <p>
	 * Parallel mode uses {@link ToolStateCollector} which respects the
	 * {@link com.alibaba.cloud.ai.graph.KeyStrategy} configured for each state key.
	 * This provides deterministic merge behavior even when multiple tools write to the
	 * same key concurrently:
	 * <ul>
	 * <li>Keys with {@code APPEND} strategy: values from all tools are appended</li>
	 * <li>Keys with {@code REPLACE} strategy: last value wins (non-deterministic order)</li>
	 * </ul>
	 * </p>
	 * <p>
	 * Note: This differs from sequential execution mode, which uses simple last-write-wins
	 * semantics via {@code Map.putAll()}. The difference is intentional - parallel mode
	 * was designed with KeyStrategy support from the start, while sequential mode preserves
	 * the original behavior for backward compatibility.
	 * </p>
	 *
	 * @param toolCalls the tool calls to execute
	 * @param state the current state (will be snapshot for thread safety)
	 * @param config the runtime configuration
	 * @return the merged state updates from all tool executions
	 * @see ToolStateCollector
	 * @see #executeToolCallsSequential(List, OverAllState, RunnableConfig)
	 */
	private Map<String, Object> executeToolCallsParallel(List<AssistantMessage.ToolCall> toolCalls, OverAllState state,
			RunnableConfig config, List<ToolCallback> currentTools) {

		// Log debug message when wrapSyncToolsAsAsync is enabled but ignored in parallel
		// mode
		if (wrapSyncToolsAsAsync && logger.isDebugEnabled()) {
			logger.debug(
					"Parallel execution mode: wrapSyncToolsAsAsync is ignored to avoid executor starvation. "
							+ "Sync tools will execute directly within the parallel runAsync context.");
		}

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

		// Pre-create cancellation tokens for cancellable tools so they can be accessed
		// from the exceptionally handler when outer timeout triggers
		Map<Integer, DefaultCancellationToken> cancellationTokens = new ConcurrentHashMap<>();

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
								toolSpecificUpdate, true, cancellationTokens, index, currentTools);
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
							// Cancel the tool's cancellation token to notify it to stop gracefully
							DefaultCancellationToken token = cancellationTokens.get(index);
							if (token != null) {
								token.cancel();
								logger.debug("Cancelled tool {} at index {} due to outer timeout",
										toolCall.name(), index);
							}
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
		Boolean returnDirect = null;
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
			returnDirect = shouldReturnDirect(toolCalls.get(i), returnDirect, config, currentTools);
		}

		ToolResponseMessage.Builder builder = ToolResponseMessage.builder().responses(toolResponses);
		if (returnDirect != null && returnDirect) {
			builder.metadata(Map.of(FINISH_REASON_METADATA_KEY, FINISH_REASON));
		}
		updatedState.put("messages", builder.build());
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
		// Dynamically resolve all tools for partial response handling
		List<ToolCallback> currentTools = resolveAllTools();
		
		if (parallelToolExecution && remainingToolCalls.size() > 1) {
			newResults = executeToolCallsParallel(remainingToolCalls, state, config, currentTools);
		}
		else {
			newResults = executeToolCallsSequential(remainingToolCalls, state, config, currentTools);
		}

		// Merge existing responses with new responses
		ToolResponseMessage newToolResponseMessage = (ToolResponseMessage) newResults.get("messages");
		List<ToolResponseMessage.ToolResponse> allResponses = new ArrayList<>(existingResponses);
		allResponses.addAll(newToolResponseMessage.getResponses());

		// Compute returnDirect from all tool calls (existing + remaining)
		Boolean returnDirect = null;
		for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
			returnDirect = shouldReturnDirect(toolCall, returnDirect, config, currentTools);
		}

		// Build final result
		Map<String, Object> updatedState = new HashMap<>(newResults);
		List<Object> newMessages = new ArrayList<>();
		ToolResponseMessage.Builder builder = ToolResponseMessage.builder().responses(allResponses);
		if (returnDirect != null && returnDirect) {
			builder.metadata(Map.of(FINISH_REASON_METADATA_KEY, FINISH_REASON));
		}
		newMessages.add(builder.build());
		newMessages.add(new RemoveByHash<>(toolResponseMessage));
		updatedState.put("messages", newMessages);

		if (enableActingLog) {
			logger.info("[ThreadId {}] Agent {} acting successfully returned.", config.threadId().orElse(THREAD_ID_DEFAULT),
					agentName);
		}

		return updatedState;
	}

	private Boolean shouldReturnDirect(AssistantMessage.ToolCall toolCall, Boolean returnDirect, RunnableConfig config, List<ToolCallback> currentTools) {
		ToolCallback toolCallback = resolve(toolCall.name(), config, currentTools);
		if (toolCallback == null) {
			return returnDirect;
		}
		if (returnDirect == null) {
			returnDirect = toolCallback.getToolMetadata().returnDirect();
		}
		else {
			returnDirect = returnDirect && toolCallback.getToolMetadata().returnDirect();
		}
		return returnDirect;
	}

	/**
	 * Execute a tool call with interceptor chain support. Supports both sync and async
	 * tool callbacks.
	 * @param toolCall the tool call to execute
	 * @param state the current state
	 * @param config the runtime configuration
	 * @param extraStateFromToolCall map to collect state updates from tool execution
	 * @param inParallelExecution true if called from parallel execution context, false
	 * for sequential
	 * @return the tool call response
	 */
	private ToolCallResponse executeToolCallWithInterceptors(AssistantMessage.ToolCall toolCall, OverAllState state,
			RunnableConfig config, Map<String, Object> extraStateFromToolCall, boolean inParallelExecution) {
		return executeToolCallWithInterceptors(toolCall, state, config, extraStateFromToolCall, inParallelExecution,
				null, -1, resolveAllTools());
	}

	/**
	 * Execute a tool call with interceptor chain support, with optional cancellation
	 * token tracking for parallel execution.
	 * @param toolCall the tool call to execute
	 * @param state the current state
	 * @param config the runtime configuration
	 * @param extraStateFromToolCall map to collect state updates from tool execution
	 * @param inParallelExecution true if called from parallel execution context, false
	 * for sequential
	 * @param cancellationTokens optional map to store cancellation tokens for parallel
	 * execution (may be null)
	 * @param toolIndex the index of this tool in the parallel execution (used as key in
	 * cancellationTokens)
	 * @param currentTools the dynamically resolved tools to use
	 * @return the tool call response
	 */
	private ToolCallResponse executeToolCallWithInterceptors(AssistantMessage.ToolCall toolCall, OverAllState state,
			RunnableConfig config, Map<String, Object> extraStateFromToolCall, boolean inParallelExecution,
			Map<Integer, DefaultCancellationToken> cancellationTokens, int toolIndex, List<ToolCallback> currentTools) {

		// Create ToolCallRequest
		ToolCallRequest request = ToolCallRequest.builder()
				.toolCall(toolCall)
				.context(config.metadata().orElse(new HashMap<>()))
				.executionContext(new ToolCallExecutionContext(config, state))
				.build();

		// Create base handler that actually executes the tool
		ToolCallHandler baseHandler = req -> {
			ToolCallback toolCallback = resolve(req.getToolName(), config, currentTools);

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
			return executeToolByType(toolCallback, req, toolContextMap, config, extraStateFromToolCall,
					inParallelExecution, cancellationTokens, toolIndex);
		};

		// Chain interceptors if any
		ToolCallHandler chainedHandler = InterceptorChain.chainToolInterceptors(toolInterceptors, baseHandler);

		// Execute the chained handler
		return chainedHandler.call(request);
	}

	/**
	 * Routes tool execution based on callback type.
	 *
	 * <p>
	 * When {@code wrapSyncToolsAsAsync} is enabled and we are NOT in parallel execution
	 * mode, synchronous tools are automatically wrapped using
	 * {@link AsyncToolCallbackAdapter} to enable async execution. This allows all tools
	 * (including {@code @Tool} annotated methods, {@code FunctionToolCallback},
	 * {@code MethodToolCallback}, and MCP tools) to benefit from the async execution
	 * infrastructure without requiring user code changes.
	 * </p>
	 *
	 * <p>
	 * <b>Important:</b> In parallel execution mode ({@code inParallelExecution=true}),
	 * the {@code wrapSyncToolsAsAsync} option is ignored. This is because the outer
	 * {@code runAsync} in {@code executeToolCallsParallel} already provides concurrency,
	 * and wrapping sync tools would cause executor starvation - the outer task occupies
	 * a thread while waiting for the inner wrapped task to complete, which needs the
	 * same thread pool.
	 * </p>
	 * @param toolCallback the tool callback to execute
	 * @param request the tool call request
	 * @param toolContextMap the tool context
	 * @param config the runtime configuration
	 * @param extraStateFromToolCall map to collect state updates
	 * @param inParallelExecution true if called from parallel execution context
	 * @return the tool call response
	 */
	private ToolCallResponse executeToolByType(ToolCallback toolCallback, ToolCallRequest request,
			Map<String, Object> toolContextMap, RunnableConfig config, Map<String, Object> extraStateFromToolCall,
			boolean inParallelExecution) {
		return executeToolByType(toolCallback, request, toolContextMap, config, extraStateFromToolCall,
				inParallelExecution, null, -1);
	}

	/**
	 * Routes tool execution based on callback type, with optional cancellation token
	 * tracking.
	 * @param toolCallback the tool callback to execute
	 * @param request the tool call request
	 * @param toolContextMap the tool context
	 * @param config the runtime configuration
	 * @param extraStateFromToolCall map to collect state updates
	 * @param inParallelExecution true if called from parallel execution context
	 * @param cancellationTokens optional map to store cancellation tokens for parallel
	 * execution
	 * @param toolIndex the index of this tool in the parallel execution
	 * @return the tool call response
	 */
	private ToolCallResponse executeToolByType(ToolCallback toolCallback, ToolCallRequest request,
			Map<String, Object> toolContextMap, RunnableConfig config, Map<String, Object> extraStateFromToolCall,
			boolean inParallelExecution, Map<Integer, DefaultCancellationToken> cancellationTokens, int toolIndex) {

		if (toolCallback instanceof AsyncToolCallback async) {
			return executeAsyncTool(async, request, toolContextMap, config, extraStateFromToolCall, cancellationTokens,
					toolIndex);
		}
		else if (wrapSyncToolsAsAsync && !inParallelExecution) {
			// Wrap sync tool as async for unified async execution
			// Only in sequential mode - parallel mode already has concurrency from outer
			// runAsync
			// Wrapping in parallel mode would cause executor starvation (deadlock)
			Executor executor = getToolExecutor(config);
			AsyncToolCallback wrappedAsync = AsyncToolCallbackAdapter.wrapIfNeeded(toolCallback, executor,
					toolExecutionTimeout);
			return executeAsyncTool(wrappedAsync, request, toolContextMap, config, extraStateFromToolCall,
					cancellationTokens, toolIndex);
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
		return executeAsyncTool(callback, request, toolContextMap, config, extraStateFromToolCall, null, -1);
	}

	/**
	 * Execute an async tool with timeout handling and optional external cancellation
	 * token tracking.
	 *
	 * <p>
	 * Supports {@link CancellableAsyncToolCallback} for cooperative cancellation. When a
	 * tool implements this interface, a real cancellation token is created and passed to
	 * the tool. On timeout, the token is cancelled to allow the tool to stop gracefully.
	 * </p>
	 *
	 * <p>
	 * When called from parallel execution context, the cancellation token is also stored
	 * in the provided {@code cancellationTokens} map so that the outer timeout handler
	 * can cancel it if needed.
	 * </p>
	 * @param callback the async tool callback to execute
	 * @param request the tool call request
	 * @param toolContextMap the tool context
	 * @param config the runtime configuration
	 * @param extraStateFromToolCall map to collect state updates from tool execution
	 * @param cancellationTokens optional map to store cancellation tokens for parallel
	 * execution (may be null)
	 * @param toolIndex the index of this tool in the parallel execution (used as key in
	 * cancellationTokens)
	 * @return the tool call response
	 */
	private ToolCallResponse executeAsyncTool(AsyncToolCallback callback, ToolCallRequest request,
			Map<String, Object> toolContextMap, RunnableConfig config, Map<String, Object> extraStateFromToolCall,
			Map<Integer, DefaultCancellationToken> cancellationTokens, int toolIndex) {

		ToolContext context = new ToolContext(toolContextMap);

		// Create cancellation token for cancellable tools
		DefaultCancellationToken cancellationToken = null;

		try {
			CompletableFuture<String> future;

			// Route based on callback type - use real token for cancellable tools
			if (callback instanceof CancellableAsyncToolCallback cancellable) {
				cancellationToken = new DefaultCancellationToken();
				// Store the token in the external map so outer timeout handler can cancel it
				if (cancellationTokens != null && toolIndex >= 0) {
					cancellationTokens.put(toolIndex, cancellationToken);
				}
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
				return ToolCallResponse.error(request.getToolCallId(), request.getToolName(), extractErrorMessage(cause));
			}
			else if (cause instanceof ToolExecutionException toolExecutionException) {
				logger.error("Async tool {} execution failed, handling with processor: {}", request.getToolName(),
						toolExecutionExceptionProcessor.getClass().getName(), toolExecutionException);
				String result = toolExecutionExceptionProcessor.process(toolExecutionException);
				return ToolCallResponse.of(request.getToolCallId(), request.getToolName(), result);
			}
			else {
				logger.error("Async tool {} execution failed: {}", request.getToolName(), cause.getMessage(), cause);
				return ToolCallResponse.error(request.getToolCallId(), request.getToolName(), extractErrorMessage(cause));
			}
		}
		catch (CancellationException e) {
			logger.warn("Async tool {} execution was cancelled", request.getToolName(), e);
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
		if (error == null) {
			return "Unknown error";
		}
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

	private ToolCallback resolve(String toolName, RunnableConfig config, List<ToolCallback> currentTools) {
		if (currentTools != null) {
			Optional<ToolCallback> fromNode = currentTools.stream()
				.filter(callback -> callback.getToolDefinition().name().equals(toolName))
				.findFirst();
			if (fromNode.isPresent()) {
				return fromNode.get();
			}
		}
		// dynamic tool callbacks from config metadata (set by AgentLlmNode / ModelInterceptor)
		ToolCallback fromDynamic = resolveFromConfigMetadata(toolName, config);
		if (fromDynamic != null) {
			return fromDynamic;
		}
		return toolCallbackResolver == null ? null : toolCallbackResolver.resolve(toolName);
	}

	@SuppressWarnings("unchecked")
	private ToolCallback resolveFromConfigMetadata(String toolName, RunnableConfig config) {
		return Optional.ofNullable(config.context().get(RunnableConfig.DYNAMIC_TOOL_CALLBACKS_METADATA_KEY))
			.filter(v -> v instanceof List)
			.map(v -> (List<ToolCallback>) v)
			.flatMap(list -> list.stream()
				.filter(tc -> tc != null && toolName.equals(tc.getToolDefinition().name()))
				.findFirst())
			.orElse(null);
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

		private boolean wrapSyncToolsAsAsync = false;

		private List<ToolCallback> toolCallbacks = new ArrayList<>();
		
		private List<ToolCallbackProvider> toolCallbackProviders;

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

		/**
		 * Enable automatic wrapping of synchronous tools as async.
		 *
		 * <p>
		 * When enabled, synchronous tools (such as {@code @Tool} annotated methods,
		 * {@code FunctionToolCallback}, {@code MethodToolCallback}, and MCP tools) are
		 * automatically wrapped using {@link AsyncToolCallbackAdapter} to enable async
		 * execution. This allows all tools to benefit from the async execution
		 * infrastructure without requiring user code changes.
		 * </p>
		 *
		 * <p>
		 * This is especially useful when combined with {@link #parallelToolExecution(boolean)}
		 * to enable parallel execution of multiple tool calls.
		 * </p>
		 * @param wrap true to enable automatic wrapping (default: false)
		 * @return this builder
		 * @see AsyncToolCallbackAdapter
		 */
		public Builder wrapSyncToolsAsAsync(boolean wrap) {
			this.wrapSyncToolsAsAsync = wrap;
			return this;
		}

		public Builder toolCallbacks(List<ToolCallback> toolCallbacks) {
			this.toolCallbacks = toolCallbacks;
			return this;
		}
		
		public Builder toolCallbackProviders(List<ToolCallbackProvider> toolCallbackProviders) {
			this.toolCallbackProviders = toolCallbackProviders;
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
			if (toolExecutionExceptionProcessor == null) {
				toolExecutionExceptionProcessor = DefaultToolExecutionExceptionProcessor.builder()
						.alwaysThrow(false)
						.build();			}
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

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

import com.alibaba.cloud.ai.graph.agent.context.ToolResultArchive;

import com.alibaba.cloud.ai.graph.GraphLifecycleListener;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.NodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.NodePause;
import com.alibaba.cloud.ai.graph.agent.AgentTool;
import com.alibaba.cloud.ai.graph.agent.interceptor.InterceptorChain;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallExecutionContext;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.tool.ToolCallResults;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import com.alibaba.cloud.ai.graph.agent.tool.AsyncToolCallback;
import com.alibaba.cloud.ai.graph.agent.tool.CancellableAsyncToolCallback;
import com.alibaba.cloud.ai.graph.agent.tool.CancellationToken;
import com.alibaba.cloud.ai.graph.agent.tool.DeferredToolCatalog;
import com.alibaba.cloud.ai.graph.agent.tool.DefaultCancellationToken;
import com.alibaba.cloud.ai.graph.agent.tool.StateAwareToolCallback;
import com.alibaba.cloud.ai.graph.agent.tool.StreamingToolCallback;
import com.alibaba.cloud.ai.graph.agent.tool.ToolCancelledException;
import com.alibaba.cloud.ai.graph.agent.tool.ToolStateCollector;
import com.alibaba.cloud.ai.graph.internal.node.ParallelNode;
import com.alibaba.cloud.ai.graph.state.RemoveByHash;
import com.alibaba.cloud.ai.graph.streaming.RunEvent;
import com.alibaba.cloud.ai.graph.streaming.RunEventContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.part.ToolResultPart;
import org.springframework.ai.model.tool.ToolCallConfirmationPolicy;
import org.springframework.ai.model.tool.ToolCallDecisions;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static com.alibaba.cloud.ai.graph.RunnableConfig.AGENT_TOOL_NAME;
import static com.alibaba.cloud.ai.graph.agent.DefaultBuilder.POSSIBLE_LLM_TOOL_NAME_CHANGE_WARNING;
import static com.alibaba.cloud.ai.graph.agent.hook.returndirect.ReturnDirectConstants.FINISH_REASON_METADATA_KEY;
import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY;
import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_STATE_CONTEXT_KEY;
import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_STATE_FOR_UPDATE_CONTEXT_KEY;
import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_TOOL_CALL_ID_CONTEXT_KEY;
import static com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver.THREAD_ID_DEFAULT;
import static org.springframework.ai.model.tool.DefaultToolCallingManager.DEFAULT_MAX_CALLS_PER_TOOL;
import static org.springframework.ai.model.tool.DefaultToolCallingManager.DEFAULT_MAX_TOTAL_TOOL_CALLS;
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

	private final ToolResultArchive toolResultArchive;

	private final DeferredToolCatalog deferredToolCatalog;

	private final String budgetStateKey;
	private final String pendingAgentToolsStateKey;
	private final int maxCallsPerTool;
	private final int maxTotalToolCalls;

	private final boolean enableActingLog;

	private final boolean parallelToolExecution;

	private final int maxParallelTools;

	private final Duration toolExecutionTimeout;

	private final Executor toolExecutor;

	private final boolean wrapSyncToolsAsAsync;

	private volatile List<ToolCallback> toolCallbacks;

	private Map<String, Object> toolContext;

	private List<ToolInterceptor> toolInterceptors = new ArrayList<>();

	private ToolCallbackResolver toolCallbackResolver;

	private ToolExecutionExceptionProcessor toolExecutionExceptionProcessor;

	public AgentToolNode(Builder builder) {
		this.agentName = builder.agentName;
		this.toolResultArchive = builder.toolResultArchive;
		this.deferredToolCatalog = builder.deferredToolCatalog;
		this.budgetStateKey = "_TOOL_CALL_BUDGET_:" + Objects.toString(agentName, "");
		this.pendingAgentToolsStateKey = "_PENDING_AGENT_TOOLS_:" + Objects.toString(agentName, "");
		this.maxCallsPerTool = builder.maxCallsPerTool;
		this.maxTotalToolCalls = builder.maxTotalToolCalls;
		this.enableActingLog = builder.enableActingLog;
		this.toolCallbackResolver = builder.toolCallbackResolver;
		this.toolCallbacks = builder.toolCallbacks == null ? List.of() : List.copyOf(builder.toolCallbacks);
		this.toolContext = builder.toolContext;
		this.toolExecutionExceptionProcessor = builder.toolExecutionExceptionProcessor;
		this.parallelToolExecution = builder.parallelToolExecution;
		this.maxParallelTools = builder.maxParallelTools;
		this.toolExecutionTimeout = builder.toolExecutionTimeout;
		this.toolExecutor = builder.toolExecutor;
		this.wrapSyncToolsAsAsync = builder.wrapSyncToolsAsAsync;
	}

	public void setToolCallbacks(List<ToolCallback> toolCallbacks) {
		this.toolCallbacks = toolCallbacks == null ? List.of() : List.copyOf(toolCallbacks);
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

	private static final ToolCallConfirmationPolicy CONFIRMATION_POLICY = ToolCallConfirmationPolicy.fromToolMetadata();

	private Predicate<ToolCallRequest> configuredToolReview = request -> false;

	public void setConfiguredToolReview(Predicate<ToolCallRequest> configuredToolReview) {
		this.configuredToolReview = configuredToolReview;
	}

	public boolean requiresConfirmation(AssistantMessage.ToolCall call, RunnableConfig config) {
		ToolCallback callback = resolve(call.name(), config);
		return callback != null && CONFIRMATION_POLICY.requiresConfirmation(call, callback.getToolMetadata());
	}

	public boolean requiresConfirmation(AssistantMessage message, AssistantMessage.ToolCall call, RunnableConfig config,
			boolean explicitlyConfigured) {
		ToolCallback callback = resolve(message, call.name(), config);
		if (RequestToolScope.isScoped(message) && callback == null) {
			return false;
		}
		return explicitlyConfigured
				|| (callback != null && CONFIRMATION_POLICY.requiresConfirmation(call, callback.getToolMetadata()));
	}

	@Override
	public Map<String, Object> apply(OverAllState state, RunnableConfig config) throws Exception {
		return executeBatch(state, config, CancellationToken.NONE).block();
	}

	public CompletableFuture<Map<String, Object>> applyAsync(OverAllState state, RunnableConfig config) {
		var cancellation = new DefaultCancellationToken();
		return executeBatch(state, config, cancellation).doOnCancel(cancellation::cancel).toFuture();
	}

	private Mono<Map<String, Object>> executeBatch(OverAllState state, RunnableConfig config,
			CancellationToken cancellation) {
		return Mono.using(AtomicReference<AssistantMessage>::new, assistant -> Mono.defer(() -> {
			cancellation.throwIfCancelled();
			List<Message> messages = (List<Message>) state.value("messages").orElseThrow();
			Message lastMessage = messages.get(messages.size() - 1);
			if (lastMessage instanceof AssistantMessage message) {
				assistant.set(message);
			}
			else if (lastMessage instanceof ToolResponseMessage && messages.size() > 1
					&& messages.get(messages.size() - 2) instanceof AssistantMessage message) {
				assistant.set(message);
			}
			else {
				throw new IllegalStateException("No assistant tool call batch found");
			}

			var batch = assistant.get();
			var budget = new ToolCallBudget(state);
			var continuations = AgentToolContinuations.load(state, pendingAgentToolsStateKey, config);
			var execution = new ToolExecution(name -> resolve(batch, name, config), budget, continuations, cancellation);
			var settled = lastMessage instanceof ToolResponseMessage tool ? tool : null;
			var existing = settled == null ? List.<ToolResultPart>of() : settled.getResults();
			var completed = ToolCallResults.settledKeys(batch, existing);
			var keys = ToolCallDecisions.toolCallKeys(batch);
			var calls = batch.getToolCalls();
			var remaining = IntStream.range(0, calls.size())
					.filter(i -> !completed.contains(keys.get(i)) && !continuations.isWaiting(keys.get(i)))
					.mapToObj(i -> new ToolDispatch(keys.get(i), calls.get(i), i)).toList();

			var results = executeToolCalls(remaining, state, config, execution);
			if (toolResultArchive != null) {
				results = results.publishOn(toolScheduler(config));
			}
			return results.map(outcomes -> finishBatch(state, config, settled, execution, existing, keys, outcomes));
		}), assistant -> {
			HumanInTheLoopHook.clearConfirmedCalls(config);
			if (assistant.get() != null) {
				RequestToolScope.clear(assistant.get(), config);
			}
		});
	}

	private Mono<ToolBatch> executeToolCalls(List<ToolDispatch> calls, OverAllState state,
			RunnableConfig config, ToolExecution execution) {
		if (calls.isEmpty()) {
			return Mono.just(new ToolBatch(Map.of(), Map.of()));
		}
		if (enableActingLog) {
			logger.info("[ThreadId {}] Agent {} acting with {} tools.", config.threadId().orElse(THREAD_ID_DEFAULT),
					agentName, calls.size());
		}

		boolean parallel = parallelToolExecution && calls.size() > 1;
		// Sequential tools replace earlier updates. Parallel tools merge in model call order
		// using the graph's key strategies.
		var collector = new ToolStateCollector(calls.size(), parallel ? state.keyStrategies() : Map.of());
		var scheduler = toolScheduler(config);
		long submitted = System.nanoTime();
		return Flux.range(0, calls.size()).flatMap(index -> {
			var dispatch = calls.get(index);
			var toolCall = dispatch.call();
			var toolUpdates = collector.createToolUpdateMap(index);
			Runnable discard = () -> collector.discardToolUpdateMap(index);
			var result = Mono.defer(() -> executeToolCallWithInterceptors(dispatch,
					parallel ? state.snapShot().orElseThrow() : state, config, toolUpdates, scheduler, execution, discard));
			if (parallel) {
				// Include admission and executor queue time in the parallel tool budget.
				var remaining = toolExecutionTimeout.minusNanos(System.nanoTime() - submitted);
				result = remaining.isZero() || remaining.isNegative()
						? Mono.error(new TimeoutException()) : result.timeout(remaining);
			}
			result = result.onErrorResume(TimeoutException.class, error -> {
				discard.run();
				return Mono.just(ToolOutcome.completed(
						ToolCallResponse.error(toolCall.id(), toolCall.name(), extractErrorMessage(error))));
			}).onErrorResume(RejectedExecutionException.class, error -> {
				discard.run();
				return Mono.just(ToolOutcome.completed(
						ToolCallResponse.error(toolCall.id(), toolCall.name(), "Tool executor is overloaded")));
			});
			if (parallel) {
				result = result.onErrorResume(Exception.class, error -> Mono.just(ToolOutcome.completed(
						ToolCallResponse.error(toolCall.id(), toolCall.name(), extractErrorMessage(error)))));
			}
			return result.map(outcome -> Map.entry(dispatch.key(), outcome));
		}, parallel ? maxParallelTools : 1)
				.collectMap(Map.Entry::getKey, Map.Entry::getValue)
				.map(outcomes -> new ToolBatch(outcomes, collector.mergeAll()));
	}

	/**
	 * Combines results in model call order and checkpoints settled siblings on a pause.
	 */
	private Map<String, Object> finishBatch(OverAllState state, RunnableConfig config, ToolResponseMessage settled,
			ToolExecution execution, List<ToolResultPart> existing, List<String> keys, ToolBatch batch) {
		var continuations = execution.continuations();
		execution.cancellation().throwIfCancelled();
		var pending = new LinkedHashMap<String, AgentToolContinuations.PausedCall>();
		var responses = new ArrayList<>(existing);
		Boolean returnDirect = continuations.allReturnDirect();
		for (var key : keys) {
			var outcome = batch.outcomes().get(key);
			var pause = outcome == null ? continuations.pending().get(key) : outcome.pause();
			if (pause != null) {
				pending.put(key, pause);
			}
			else if (outcome != null) {
				var response = outcome.response();
				boolean direct = shouldReturnDirect(response, execution);
				var result = ToolCallResults.withCallKey(response.toToolResultPart(), key);
				responses.add(this.toolResultArchive != null && !direct
						? this.toolResultArchive.archive(config, this.agentName, key, result) : result);
				returnDirect = !Boolean.FALSE.equals(returnDirect) && direct;
			}
		}

		var updates = new HashMap<>(batch.updates());
		if (pending.isEmpty()) {
			HumanInTheLoopHook.removeConfirmedCalls(state, agentName, updates);
		}
		if (!pending.isEmpty() || continuations.hasSavedState()) {
			updates.put(pendingAgentToolsStateKey, pending.isEmpty() ? OverAllState.MARK_FOR_REMOVAL
					: continuations.save(pending, returnDirect));
		}
		if (settled == null || responses.size() != existing.size()) {
			var metadata = settled == null ? new HashMap<String, Object>() : new HashMap<>(settled.getMetadata());
			if (pending.isEmpty() && Boolean.TRUE.equals(returnDirect)) {
				metadata.put(FINISH_REASON_METADATA_KEY, FINISH_REASON);
			}
			var orderedResponses = settled == null ? responses : ToolCallResults.inCallOrder(keys, responses);
			var response = ToolResponseMessage.builder().results(orderedResponses)
					.metadata(metadata).build();
			if (settled != null || !pending.isEmpty()) {
				updates.put("messages", settled == null ? response : List.of(response, RemoveByHash.of(settled)));
			}
			else {
				updates.putIfAbsent("messages", response);
			}
		}
		var result = execution.budget().finish(updates);
		if (pending.isEmpty()) {
			return result;
		}
		var interruption = AgentToolContinuations.interruption(pending.values().stream()
				.map(AgentToolContinuations.PausedCall::interruption).toList());
		return new NodePause(interruption).withUpdates(result);
	}
	private boolean shouldReturnDirect(ToolCallResponse response, ToolExecution execution) {
		ToolCallback callback = response.isError() ? null : execution.resolve().apply(response.getToolName());
		return callback != null && callback.getToolMetadata().returnDirect();
	}

	private Mono<ToolOutcome> executeToolCallWithInterceptors(ToolDispatch dispatch, OverAllState state,
			RunnableConfig config, Map<String, Object> extraStateFromToolCall, Scheduler scheduler,
			ToolExecution execution, Runnable discardUpdates) {

		var toolCall = dispatch.call();
		var pause = new AtomicReference<AgentToolContinuations.PausedCall>();
		var pauseSignal = new AtomicReference<AgentTool.Interrupted>();
		// Create ToolCallRequest
		ToolCallRequest request = ToolCallRequest.builder()
				.toolCall(toolCall)
				.context(config.metadata().orElse(new HashMap<>()))
				.executionContext(new ToolCallExecutionContext(config, state))
				.build();

		// Create base handler that actually executes the tool
		Function<ToolCallRequest, Mono<ToolCallResponse>> invoke = req -> Mono.defer(() -> {
			execution.cancellation().throwIfCancelled();
			if (pauseSignal.get() != null) {
				throw pauseSignal.get();
			}
			var continuation = execution.continuations().continuation(dispatch.key(), req.getToolName(), req.getArguments());
			ToolCallback toolCallback = execution.resolve().apply(req.getToolName());
			if (continuation != null && !AgentTool.isAgentTool(toolCallback)) {
				throw new IllegalStateException("Paused agent tool '" + req.getToolName() + "' is unavailable");
			}

			if (toolCallback == null) {
				logger.warn(POSSIBLE_LLM_TOOL_NAME_CHANGE_WARNING, req.getToolName());
				return Mono.just(ToolCallResponse.builder()
					.content("Tool not available: " + req.getToolName())
					.toolName(req.getToolName())
					.toolCallId(req.getToolCallId())
					.status("error")
					.metadata(Map.of("error", true, "unresolvedToolName", req.getToolName()))
					.build());
			}

			if (continuation == null && requiresUnresolvedConfirmation(dispatch, req, toolCallback, state, config)) {
				return Mono.just(ToolCallResponse.error(req.getToolCallId(), req.getToolName(), "Tool call requires confirmation"));
			}

			String limitMessage = continuation == null ? execution.budget().reserve(req.getToolName()) : null;
			if (limitMessage != null) {
				return Mono.just(ToolCallResponse.error(req.getToolCallId(), req.getToolName(), limitMessage));
			}

			if (enableActingLog) {
				logger.info("[ThreadId {}] Agent {} acting, executing tool {}.",
						config.threadId().orElse(THREAD_ID_DEFAULT), agentName, req.getToolName());
			}

			Map<String, Object> toolContextMap = new HashMap<>(toolContext);
			toolContextMap.putAll(req.getContext());
			toolContextMap.remove(AgentTool.CONTINUATION_CONTEXT_KEY);
			if (continuation != null) {
				toolContextMap.put(AgentTool.CONTINUATION_CONTEXT_KEY, continuation);
			}

			// Handle tools that need state injection:
			// - StateAwareToolCallback (including AsyncToolCallback)
			// - FunctionToolCallback
			// - MethodToolCallback
			if (toolCallback instanceof StateAwareToolCallback || toolCallback instanceof FunctionToolCallback<?, ?>
					|| toolCallback instanceof MethodToolCallback) {
				toolContextMap.putAll(Map.of(AGENT_STATE_CONTEXT_KEY, state, AGENT_CONFIG_CONTEXT_KEY, config,
						AGENT_STATE_FOR_UPDATE_CONTEXT_KEY, extraStateFromToolCall));
				toolContextMap.put(AGENT_TOOL_CALL_ID_CONTEXT_KEY, dispatch.key());
			}

			var scope = config.eventContext().map(events -> events.child(RunEvent.ScopeKind.TOOL, req.getToolName()))
					.orElse(null);
			var scopedConfig = scope == null ? config : config.withEventContext(scope);
			var toolEvents = new ToolEvents(scope, req, dispatch.index());
			if (scope != null) {
				toolContextMap.put(AGENT_CONFIG_CONTEXT_KEY, scopedConfig);
				toolEvents.emit(RunEvent.Type.TOOL_STARTED, scope.options().includeToolArguments() ? req.getArguments() : null);
			}

			var result = Mono.defer(() -> executeTool(toolCallback, req, toolContextMap, scopedConfig, scheduler,
					discardUpdates, toolEvents)).doOnError(AgentTool.Interrupted.class, interrupted -> {
						pause.set(AgentToolContinuations.PausedCall.from(interrupted, req.getToolName(), req.getArguments()));
						pauseSignal.set(interrupted);
					});
			return scope == null ? result : result
					.doOnSuccess(response -> {
						boolean cancelled = execution.cancellation().isCancelled() || Thread.currentThread().isInterrupted();
						boolean failed = response.isError() || toolEvents.failure != null;
						if ((failed || cancelled) && toolCallback instanceof StreamingToolCallback) {
							discardUpdates.run();
						}
						toolEvents.finish(cancelled ? RunEvent.Type.TOOL_CANCELLED
								: failed ? RunEvent.Type.TOOL_FAILED : RunEvent.Type.TOOL_COMPLETED,
								cancelled ? null : toolEvents.failure == null ? response.getResult() : toolEvents.failure);
					})
					.doOnError(error -> {
						if (error instanceof AgentTool.Interrupted interrupted) {
							toolEvents.finish(RunEvent.Type.TOOL_PAUSED, interrupted.interruption().interruptionId().orElse(null));
						}
						else {
							toolEvents.finish(RunEvent.Type.TOOL_FAILED, RunEventContext.failure(error));
						}
					})
					.doOnCancel(() -> toolEvents.finish(RunEvent.Type.TOOL_CANCELLED, null));
		});

		var interceptors = toolInterceptors;
		var response = interceptors.isEmpty() ? invoke.apply(request)
				: Mono.fromCallable(() -> InterceptorChain.chainToolInterceptors(interceptors,
						req -> invoke.apply(req).block()).call(request))
						.subscribeOn(ToolExecutionSchedulers.interceptors());
		return response.map(value -> {
			if (pause.get() == null) {
				return ToolOutcome.completed(value);
			}
			discardUpdates.run();
			return new ToolOutcome(null, pause.get());
		}).onErrorResume(RuntimeException.class, error -> {
			if (pause.get() == null) {
				return Mono.error(error);
			}
			discardUpdates.run();
			return Mono.just(new ToolOutcome(null, pause.get()));
		});
	}

	private boolean requiresUnresolvedConfirmation(ToolDispatch dispatch, ToolCallRequest request,
			ToolCallback callback, OverAllState state, RunnableConfig config) {
		var call = dispatch.call();
		var requested = new AssistantMessage.ToolCall(request.getToolCallId(), call.type(), request.getToolName(),
				request.getArguments());
		var priorApproval = HumanInTheLoopHook.matchesSavedApproval(state, agentName, dispatch.key(), requested);
		if (priorApproval.isPresent()) {
			return !priorApproval.get();
		}
		boolean metadataRequiresReview = CONFIRMATION_POLICY.requiresConfirmation(requested, callback.getToolMetadata());
		if (call.equals(requested)) {
			return metadataRequiresReview;
		}
		return metadataRequiresReview || configuredToolReview.test(ToolCallRequest.builder(request)
				.executionContext(new ToolCallExecutionContext(config, state)).build());
	}

	private Mono<ToolCallResponse> executeTool(ToolCallback callback, ToolCallRequest request,
			Map<String, Object> toolContextMap, RunnableConfig config, Scheduler scheduler, Runnable discardUpdates,
			ToolEvents events) {
		if (callback instanceof AsyncToolCallback async) {
			return executeAsyncTool(async, request, new ToolContext(toolContextMap), discardUpdates, events);
		}
		var result = Mono.fromCallable(() -> executeSyncTool(callback, request, toolContextMap, config, events))
				.subscribeOn(scheduler);
		return wrapSyncToolsAsAsync ? result.timeout(toolExecutionTimeout) : result;
	}

	private Mono<ToolCallResponse> executeAsyncTool(AsyncToolCallback callback, ToolCallRequest request,
			ToolContext context, Runnable discardUpdates, ToolEvents events) {
		return Mono.defer(() -> {
			var token = new DefaultCancellationToken();
			CompletableFuture<String> future;
			if (callback instanceof StreamingToolCallback streaming) {
				future = streaming.callAsync(request.getArguments(), context, token, events::delta);
			}
			else if (callback instanceof CancellableAsyncToolCallback cancellable) {
				future = cancellable.callAsync(request.getArguments(), context, token);
			}
			else {
				future = callback.callAsync(request.getArguments(), context);
			}
			if (future == null) {
				return Mono.just(ToolCallResponse.error(request.getToolCallId(), request.getToolName(),
						"Async tool returned null future"));
			}
			var timeout = AgentTool.isAgentTool(callback) ? toolExecutionTimeout : callback.getTimeout();
			return Mono.fromFuture(future)
					.doOnCancel(token::cancel)
					.timeout(timeout)
					.map(Optional::of)
					.defaultIfEmpty(Optional.empty())
					.map(result -> ToolCallResponse.success(request.getToolCallId(), request.getToolName(),
							result.orElse(null)));
		}).onErrorResume(Exception.class, error -> {
			var cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
			if (cause instanceof AgentTool.Interrupted) {
				return Mono.error(cause);
			}
			if (cause instanceof TimeoutException) {
				discardUpdates.run();
				logger.warn("Async tool {} timed out, discarding its state updates", request.getToolName());
			}
			if (cause instanceof ToolExecutionException toolError) {
				events.failed(toolError);
				String result = toolExecutionExceptionProcessor.process(toolError);
				return Mono.just(ToolCallResponse.of(request.getToolCallId(), request.getToolName(), result));
			}
			return Mono.just(ToolCallResponse.error(request.getToolCallId(), request.getToolName(),
					extractErrorMessage(cause)));
		});
	}

	/**
	 * Execute a sync tool with exception handling.
	 */
	private ToolCallResponse executeSyncTool(ToolCallback callback, ToolCallRequest request,
			Map<String, Object> toolContextMap, RunnableConfig config, ToolEvents events) {

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

			return ToolCallResponse.success(request.getToolCallId(), request.getToolName(), result);
		}
		catch (AgentTool.Interrupted e) {
			throw e;
		}
		catch (ToolExecutionException e) {
			events.failed(e);
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

	private Scheduler toolScheduler(RunnableConfig config) {
		return config.metadata(ParallelNode.formatNodeId(AGENT_TOOL_NAME))
				.filter(Executor.class::isInstance)
				.map(Executor.class::cast)
				.or(() -> config.metadata(AGENT_TOOL_NAME)
						.filter(Executor.class::isInstance).map(Executor.class::cast))
				.or(() -> Optional.ofNullable(toolExecutor))
				.or(() -> config.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY)
						.filter(Executor.class::isInstance).map(Executor.class::cast))
				.map(Schedulers::fromExecutor)
				.orElseGet(ToolExecutionSchedulers::blockingTools);
	}

	private ToolCallback resolve(AssistantMessage message, String toolName, RunnableConfig config) {
		if (toolName == null) {
			return null;
		}
		return RequestToolScope.isScoped(message)
				? RequestToolScope.resolve(message, agentName, toolName, config, () -> resolveBound(toolName))
				: resolve(toolName, config);
	}

	private ToolCallback resolveBound(String toolName) {
		ToolCallback registered = resolveRegistered(toolName);
		return registered == null && this.deferredToolCatalog != null
				? this.deferredToolCatalog.resolve(toolName) : registered;
	}

	private ToolCallback resolveRegistered(String toolName) {
		return toolCallbacks.stream()
				.filter(callback -> callback.getToolDefinition().name().equals(toolName))
				.findFirst().orElse(null);
	}

	private ToolCallback resolve(String toolName, RunnableConfig config) {
		ToolCallback registered = resolveRegistered(toolName);
		if (registered != null) {
			return registered;
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

	private record ToolExecution(Function<String, ToolCallback> resolve, ToolCallBudget budget,
			AgentToolContinuations continuations, CancellationToken cancellation) {
	}

	private static final class ToolEvents {

		private final RunEventContext scope;
		private final ToolCallRequest request;
		private final int index;
		private final AtomicBoolean finished = new AtomicBoolean();
		private RunEvent.Failure failure;

		private ToolEvents(RunEventContext scope, ToolCallRequest request, int index) {
			this.scope = scope;
			this.request = request;
			this.index = index;
		}

		private void emit(RunEvent.Type type, Object value) {
			scope.emit(type, payload(value));
		}

		private void finish(RunEvent.Type type, Object value) {
			if (finished.compareAndSet(false, true)) {
				try {
					emit(type, value);
				}
				finally {
					scope.close();
				}
			}
		}

		private Mono<Void> delta(Object value) {
			return scope == null ? Mono.empty() : scope.emitReactive(RunEvent.Type.TOOL_DELTA, payload(value));
		}

		private void failed(Throwable cause) {
			if (scope != null) {
				failure = RunEventContext.failure(cause);
			}
		}

		private RunEvent.Tool payload(Object value) {
			return new RunEvent.Tool(request.getToolCallId(), index, request.getToolName(), value);
		}
	}

	private record ToolDispatch(String key, AssistantMessage.ToolCall call, int index) {
	}

	private record ToolOutcome(ToolCallResponse response, AgentToolContinuations.PausedCall pause) {

		static ToolOutcome completed(ToolCallResponse response) {
			return new ToolOutcome(response, null);
		}

	}

	private record ToolBatch(Map<String, ToolOutcome> outcomes, Map<String, Object> updates) {
	}

	/** Counts calls that reached dispatch, independently of the model's message history. */
	private final class ToolCallBudget {

		private final String executionId;
		private final Map<String, Integer> counts = new HashMap<>();
		private int total;
		private boolean closed;

		ToolCallBudget(OverAllState state) {
			executionId = state.<String>value(GraphLifecycleListener.EXECUTION_ID_KEY).orElse("");
			var saved = state.<Map<?, ?>>value(budgetStateKey).orElse(Map.of());
			if (executionId.equals(saved.get("executionId")) && saved.get("counts") instanceof Map<?, ?> previous) {
				previous.forEach((name, count) -> {
					if (name instanceof String toolName && count instanceof Number number) {
						counts.put(toolName, number.intValue());
					}
				});
			}
			total = counts.values().stream().mapToInt(Integer::intValue).sum();
		}

		synchronized String reserve(String name) {
			if (closed) {
				return "Tool execution batch has already completed";
			}
			int count = counts.getOrDefault(name, 0);
			if (count >= maxCallsPerTool) {
				return "Tool call limit (%d) exceeded for tool '%s' in this turn".formatted(maxCallsPerTool, name);
			}
			if (total >= maxTotalToolCalls) {
				return "Total tool call limit (%d) exceeded in this turn".formatted(maxTotalToolCalls);
			}
			counts.put(name, count + 1);
			total++;
			return null;
		}

		synchronized Map<String, Object> finish(Map<String, Object> updates) {
			closed = true;
			if (counts.isEmpty()) {
				return updates;
			}
			Map<String, Object> withBudget = new HashMap<>(updates);
			withBudget.put(budgetStateKey, Map.of("executionId", executionId, "counts", Map.copyOf(counts)));
			return withBudget;
		}
	}

	public static class Builder {

		private String agentName;

		private ToolResultArchive toolResultArchive;

		private DeferredToolCatalog deferredToolCatalog;

		private boolean enableActingLog;

		private boolean parallelToolExecution = false;

		private int maxParallelTools = 5;

		private int maxCallsPerTool = DEFAULT_MAX_CALLS_PER_TOOL;
		private int maxTotalToolCalls = DEFAULT_MAX_TOTAL_TOOL_CALLS;

		private Duration toolExecutionTimeout = Duration.ofMinutes(5);

		private Executor toolExecutor;

		private boolean wrapSyncToolsAsAsync = false;

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

		public Builder toolResultArchive(ToolResultArchive toolResultArchive) {
			this.toolResultArchive = toolResultArchive;
			return this;
		}

		public Builder deferredToolCatalog(DeferredToolCatalog deferredToolCatalog) {
			this.deferredToolCatalog = deferredToolCatalog;
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
		public Builder toolCallLimits(int maxCallsPerTool, int maxTotalToolCalls) {
			if (maxCallsPerTool <= 0 || maxTotalToolCalls <= 0) {
				throw new IllegalArgumentException("Tool call limits must be greater than zero");
			}
			this.maxCallsPerTool = maxCallsPerTool;
			this.maxTotalToolCalls = maxTotalToolCalls;
			return this;
		}

		public Builder toolExecutionTimeout(Duration timeout) {
			this.toolExecutionTimeout = timeout;
			return this;
		}

		/**
		 * Sets an externally managed executor for blocking tool callbacks.
		 * @param executor the executor for blocking tool callbacks
		 * @return this builder
		 */
		public Builder toolExecutor(Executor executor) {
			this.toolExecutor = Objects.requireNonNull(executor, "executor must not be null");
			return this;
		}

		/**
		 * Applies the tool execution timeout to synchronous callbacks in sequential mode.
		 * @param wrap whether to time out sequential synchronous callbacks
		 * @return this builder
		 */
		public Builder wrapSyncToolsAsAsync(boolean wrap) {
			this.wrapSyncToolsAsAsync = wrap;
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

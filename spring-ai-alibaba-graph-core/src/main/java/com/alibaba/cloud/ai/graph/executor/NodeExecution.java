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
package com.alibaba.cloud.ai.graph.executor;

import com.alibaba.cloud.ai.graph.GraphRunnerContext;
import com.alibaba.cloud.ai.graph.GraphLifecycleListener;
import com.alibaba.cloud.ai.graph.NodeExecutionPolicy;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.TaskAttemptEvent;
import com.alibaba.cloud.ai.graph.TaskAttemptEvent.Phase;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.internal.ParallelBranchPath;
import com.alibaba.cloud.ai.graph.streaming.GraphFlux;
import com.alibaba.cloud.ai.graph.streaming.ParallelGraphFlux;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

final class NodeExecution {

	private static final Logger log = LoggerFactory.getLogger(NodeExecution.class);

	private NodeExecution() {
	}

	static Mono<Map<String, Object>> run(GraphRunnerContext context, String nodeId, String taskId,
			AsyncNodeActionWithConfig action, OverAllState state, RunnableConfig config) {
		var policy = context.getCompiledGraph().compileConfig.nodePolicy(nodeId);
		var trace = taskTrace(context, nodeId, taskId, config);
		if (policy.isPresent() && !(action instanceof ParallelBranchPath.Action)) {
			return configured(context, nodeId, taskId, action, state, config, policy.get(), trace);
		}
		var taskConfig = taskConfig(context, config, taskId, trace);
		var result = Mono.defer(() -> Mono.fromFuture(action.apply(state, taskConfig)));
		if (taskConfig != config) {
			result = result.doOnNext(update -> copyContext(taskConfig, config));
		}
		return track(trace, 1, taskConfig, result);
	}

	private static RunnableConfig taskConfig(GraphRunnerContext context, RunnableConfig config, String taskId, TaskTrace trace) {
		boolean assignId = context.getCompiledGraph().compileConfig.taskCheckpoints()
				&& config.taskId().filter(taskId::equals).isEmpty();
		if (!assignId && trace == null) {
			return config;
		}
		var builder = RunnableConfig.builder(config);
		if (assignId) {
			builder.addMetadata(RunnableConfig.TASK_ID_METADATA_KEY, taskId)
					.removeMetadata(RunnableConfig.TASK_ATTEMPT_METADATA_KEY);
		}
		if (trace != null) {
			builder.addMetadata(RunnableConfig.TASK_TRACE_METADATA_KEY, trace);
		}
		return builder.build();
	}

	private static Mono<Map<String, Object>> configured(GraphRunnerContext context, String nodeId, String taskId,
			AsyncNodeActionWithConfig action, OverAllState state, RunnableConfig config, NodeExecutionPolicy policy,
			TaskTrace trace) {
		return Mono.defer(() -> {
			var journal = new ParallelStepJournal(context);
			var attempts = new AtomicInteger(journal.attempts(taskId));
			Mono<AttemptResult> execute = Mono.defer(() -> {
				int attempt = attempts.incrementAndGet();
				journal.saveAttempt(taskId, attempt);
				var builder = RunnableConfig.builder(config)
						.addMetadata(RunnableConfig.TASK_ID_METADATA_KEY, taskId)
						.addMetadata(RunnableConfig.TASK_ATTEMPT_METADATA_KEY, attempt);
				if (trace != null) {
					builder.addMetadata(RunnableConfig.TASK_TRACE_METADATA_KEY, trace);
				}
				var attemptConfig = builder.build();
				long started = System.nanoTime();
				var invocation = Mono.defer(() -> Mono.fromFuture(action.apply(state.snapShot().orElseThrow(),
						attemptConfig)));
				if (policy.blocking()) {
					invocation = invocation.subscribeOn(Schedulers.boundedElastic());
				}
				if (policy.timeout() != null) {
					invocation = invocation.timeout(policy.timeout());
				}
				invocation = track(trace, attempt, attemptConfig, invocation);
				if (policy.maxAttempts() > 1) {
					invocation = invocation.onErrorMap(policy.retryOn(), ActionFailure::new);
				}
				return invocation.map(update -> new AttemptResult(update, attemptConfig, started));
			});
			if (policy.maxAttempts() > 1) {
				execute = execute.retryWhen(Retry.fixedDelay(policy.maxAttempts() - 1, policy.retryDelay())
						.filter(ActionFailure.class::isInstance).onRetryExhaustedThrow((spec, signal) -> signal.failure()))
						.onErrorMap(ActionFailure.class, Throwable::getCause);
			}
			return execute.map(result -> {
				copyContext(result.config(), config);
				return prepareStreams(result.update(), nodeId, result.started(), policy);
			});
		});
	}

	private static void copyContext(RunnableConfig source, RunnableConfig target) {
		target.context().clear();
		target.context().putAll(source.context());
	}

	private static Mono<Map<String, Object>> track(TaskTrace trace, int attempt,
			RunnableConfig config, Mono<Map<String, Object>> result) {
		if (trace == null) {
			return result;
		}
		return Mono.defer(() -> {
			trace.emit(config, attempt, Phase.STARTED, null);
			return result.doOnSuccess(update -> trace.emit(config, attempt, Phase.RESULT_READY, null))
					.doOnError(error -> trace.emit(config, attempt, Phase.FAILED, error))
					.doOnCancel(() -> trace.emit(config, attempt, Phase.CANCELLED, null));
		});
	}

	private static TaskTrace taskTrace(GraphRunnerContext context, String nodeId, String taskId, RunnableConfig config) {
		var graph = context.getCompiledGraph();
		var localListeners = graph.compileConfig.lifecycleListeners();
		var enclosing = context.getConfig().metadata(RunnableConfig.TASK_TRACE_METADATA_KEY)
				.filter(TaskTrace.class::isInstance).map(TaskTrace.class::cast).orElse(null);
		if (enclosing == null && localListeners.isEmpty()) {
			return null;
		}
		var path = new ArrayList<String>();
		var listeners = new LinkedHashSet<GraphLifecycleListener>();
		String parent = context.getConfig().taskId().orElse(null);
		if (enclosing != null) {
			path.addAll(enclosing.path());
			listeners.addAll(enclosing.listeners());
			parent = enclosing.taskId();
		}
		listeners.addAll(localListeners);
		String graphName = Objects.toString(graph.stateGraph.getName(), "");
		path.add(graphName);
		if (!nodeId.equals(context.getCurrentNodeId())) {
			path.add(context.getCurrentNodeId());
			parent = context.branchTaskId(0, context.getCurrentNodeId());
		}
		path.add(nodeId);
		String agent = config.metadata(RunnableConfig.AGENT_NAME_KEY).filter(String.class::isInstance)
				.map(String.class::cast).orElse(graphName);
		return new TaskTrace(nodeId, taskId, parent, agent, List.copyOf(path), List.copyOf(listeners));
	}

	private record TaskTrace(String nodeId, String taskId, String parentTaskId, String agent, List<String> path,
			List<GraphLifecycleListener> listeners) {

		void emit(RunnableConfig config, int attempt, Phase phase, Throwable failure) {
			var event = new TaskAttemptEvent(nodeId, taskId, parentTaskId, attempt, phase, failure, agent, path);
			for (var listener : listeners) {
				try {
					listener.onTaskAttempt(event, config);
				}
				catch (Exception error) {
					log.warn("Graph task listener failed for node '{}'", event.nodeId(), error);
				}
			}
		}

	}

	private record AttemptResult(Map<String, Object> update, RunnableConfig config, long started) {
	}

	private static final class ActionFailure extends RuntimeException {

		ActionFailure(Throwable cause) {
			super(cause);
		}

	}

	private static Map<String, Object> prepareStreams(Map<String, Object> update, String nodeId, long started,
			NodeExecutionPolicy policy) {
		if (policy.timeout() == null && !policy.blocking()) {
			return update;
		}
		var result = new LinkedHashMap<String, Object>(update.size());
		update.forEach((key, value) -> {
			if (value instanceof Flux<?> stream) {
				result.put(key, prepareStream(stream, nodeId, started, policy));
			}
			else if (value instanceof GraphFlux<?> stream) {
				result.put(key, prepareStream(stream, nodeId, started, policy));
			}
			else if (value instanceof ParallelGraphFlux streams) {
				var prepared = streams.getGraphFluxes().stream()
						.<GraphFlux<?>>map(stream -> prepareStream(stream, nodeId, started, policy)).toList();
				result.put(key, ParallelGraphFlux.of(prepared, streams.getMetadata()));
			}
			else {
				result.put(key, value);
			}
		});
		return result;
	}

	private static <T> GraphFlux<T> prepareStream(GraphFlux<T> stream, String nodeId, long started,
			NodeExecutionPolicy policy) {
		return stream.withFlux(prepareStream(stream.getFlux(), nodeId, started, policy));
	}

	private static <T> Flux<T> prepareStream(Flux<T> stream, String nodeId, long started, NodeExecutionPolicy policy) {
		if (policy.blocking()) {
			stream = stream.subscribeOn(Schedulers.boundedElastic());
		}
		if (policy.timeout() != null) {
			var source = stream;
			stream = Flux.defer(() -> {
				var first = remaining(policy.timeout(), started);
				if (first.isZero()) {
					return Flux.error(new TimeoutException("Node '" + nodeId + "' timed out"));
				}
				return source.timeout(Mono.delay(first), ignored -> Mono.delay(remaining(policy.timeout(), started)));
			});
		}
		return stream;
	}

	private static Duration remaining(Duration timeout, long started) {
		Duration remaining = timeout.minusNanos(System.nanoTime() - started);
		return remaining.isNegative() ? Duration.ZERO : remaining;
	}

}

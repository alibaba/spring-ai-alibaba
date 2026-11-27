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
package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver.ExecutionLease;
import com.alibaba.cloud.ai.graph.executor.MainGraphExecutor;
import com.alibaba.cloud.ai.graph.internal.ParallelBranchPath;
import com.alibaba.cloud.ai.graph.streaming.RunEvent;
import com.alibaba.cloud.ai.graph.streaming.RunEventContext;

import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver.THREAD_ID_DEFAULT;

/**
 * A reactive graph execution engine based on Project Reactor. This class has been
 * refactored to use OOP principles (inheritance, polymorphism, encapsulation) for better
 * separation of concerns and improved readability.
 */
public class GraphRunner {

	private static final Set<ActiveRun> ACTIVE_DURABLE_RUNS = ConcurrentHashMap.newKeySet();

	private final CompiledGraph compiledGraph;

	private final RunnableConfig config;

	private final ParallelBranchPath branchPath;

	private final boolean nodeOutputsRequested;

	private final AtomicReference<Object> resultValue = new AtomicReference<>();

	// Handler for main execution flow - demonstrates encapsulation
	private final MainGraphExecutor mainGraphExecutor;

	public GraphRunner(CompiledGraph compiledGraph, RunnableConfig config) {
		this(compiledGraph, config, null);
	}

	GraphRunner(CompiledGraph compiledGraph, RunnableConfig config, ParallelBranchPath branchPath) {
		this(compiledGraph, config, branchPath, true);
	}

	GraphRunner(CompiledGraph compiledGraph, RunnableConfig config, ParallelBranchPath branchPath,
			boolean nodeOutputsRequested) {
		this.compiledGraph = compiledGraph;
		this.config = config;
		this.branchPath = branchPath;
		this.nodeOutputsRequested = nodeOutputsRequested;
		// Initialize the main execution handler - demonstrates encapsulation
		this.mainGraphExecutor = new MainGraphExecutor();
	}

	public Flux<GraphResponse<NodeOutput>> run(OverAllState initialState) {
		return config.eventContext().map(parent -> Flux.defer(() -> {
			String agent = config.eventAgentName();
			var kind = branchPath != null ? RunEvent.ScopeKind.BRANCH
					: agent == null ? RunEvent.ScopeKind.GRAPH : RunEvent.ScopeKind.AGENT;
			var scope = parent.child(kind,
					agent == null ? compiledGraph.stateGraph.getName() : agent);
			var scopedConfig = RunnableConfig.builder(config).eventContext(scope).eventAgentName(null).build();
			scope.emit(RunEvent.Type.RUN_STARTED, lifecycle(scopedConfig));
			return runConfigured(initialState, scopedConfig)
					.doOnError(error -> scope.emit(RunEvent.Type.RUN_FAILED, RunEventContext.failure(error)))
					.doOnCancel(() -> scope.emit(RunEvent.Type.RUN_CANCELLED, lifecycle(scopedConfig)))
					.doFinally(signal -> scope.close());
		})).orElseGet(() -> runConfigured(initialState, config));
	}

	private Flux<GraphResponse<NodeOutput>> runConfigured(OverAllState initialState, RunnableConfig runConfig) {
		if (compiledGraph.compileConfig.taskCheckpoints()) {
			var saver = compiledGraph.compileConfig.checkpointSaver();
			if (saver.isPresent()) {
				return Flux.using(() -> claim(saver.get(), runConfig),
						claimed -> execute(initialState, claimed.lease().configure(runConfig)), ClaimedRun::close);
			}
		}
		return execute(initialState, runConfig);
	}

	static ClaimedRun claim(BaseCheckpointSaver saver, RunnableConfig config) throws Exception {
		var scope = new ActiveRun(saver.executionScope(), config.threadId().orElse(THREAD_ID_DEFAULT));
		if (!ACTIVE_DURABLE_RUNS.add(scope)) {
			throw new IllegalStateException("A durable graph execution is already active for thread '" + scope.threadId() + "'");
		}
		try {
			return new ClaimedRun(scope, saver.acquireExecutionLease(config));
		}
		catch (Exception | Error failure) {
			ACTIVE_DURABLE_RUNS.remove(scope);
			throw failure;
		}
	}

	private Flux<GraphResponse<NodeOutput>> execute(OverAllState initialState, RunnableConfig runConfig) {
		return Flux.defer(() -> {
			try {
				GraphRunnerContext context = new GraphRunnerContext(initialState, runConfig, compiledGraph, branchPath,
						nodeOutputsRequested);
				var runResult = new AtomicReference<Object>();
				// Delegate to the main execution handler, then expand step continuations
				// iteratively (depth-first). Executors emit a continuation marker as the
				// last element of each step instead of recursively nesting the next step
				// with concatWith(Flux.defer(...)), which grew the reactive operator chain
				// by one layer per executed node and blew the stack on long-running loops
				// (issue #4594). expandDeep drains continuations through a single operator,
				// keeping the subscriber depth constant while preserving emission order.
				var execution = mainGraphExecutor.execute(context, runResult)
					.expandDeep(response -> response.hasContinuation()
							? Flux.defer(() -> response.getContinuation().get()) : Flux.empty())
					.filter(response -> !response.hasContinuation())
					.doFinally(signal -> resultValue.set(runResult.get()));
				return runConfig.eventContext().map(events -> observe(execution, context, events)).orElse(execution);
			}
			catch (Exception e) {
				return Flux.error(e);
			}
		});
	}

	private Flux<GraphResponse<NodeOutput>> observe(Flux<GraphResponse<NodeOutput>> source,
			GraphRunnerContext context, RunEventContext events) {
		var outcome = new AtomicReference<Object>();
		return source.takeUntil(GraphResponse::isError).doOnNext(response -> {
			if (response.isError()) {
				outcome.set(response.getOutput().handle((output, error) -> error).join());
			}
			else if (response.isDone()) {
				outcome.set(response.resultValue().orElse(null));
			}
		}).doOnComplete(() -> {
			Object result = outcome.get();
			if (result instanceof Throwable error) {
				events.emit(RunEvent.Type.RUN_FAILED, RunEventContext.failure(error));
			}
			else if (result instanceof InterruptionMetadata interruption) {
				events.emit(RunEvent.Type.RUN_PAUSED,
						new RunEvent.Paused(interruption, context.getConfig().checkPointId().orElse(null)));
			}
			else {
				events.completed(context.getCurrentStateData());
			}
		});
	}

	private static RunEvent.Lifecycle lifecycle(RunnableConfig config) {
		var interruption = config.metadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY)
				.filter(InterruptionMetadata.class::isInstance).map(InterruptionMetadata.class::cast)
				.flatMap(InterruptionMetadata::interruptionId).orElse(null);
		return new RunEvent.Lifecycle(config.threadId().orElse(null), config.checkPointId().orElse(null), interruption);
	}

	public Optional<Object> resultValue() {
		return Optional.ofNullable(resultValue.get());
	}

	private record ActiveRun(Object storageScope, String threadId) {
	}

	record ClaimedRun(ActiveRun scope, ExecutionLease lease) implements AutoCloseable {

		@Override
		public void close() {
			try {
				lease.close();
			}
			finally {
				ACTIVE_DURABLE_RUNS.remove(scope);
			}
		}

	}

}

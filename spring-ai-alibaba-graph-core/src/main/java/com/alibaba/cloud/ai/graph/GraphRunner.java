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

import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver.ExecutionLease;
import com.alibaba.cloud.ai.graph.executor.MainGraphExecutor;
import com.alibaba.cloud.ai.graph.internal.ParallelBranchPath;

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

	private final AtomicReference<Object> resultValue = new AtomicReference<>();

	// Handler for main execution flow - demonstrates encapsulation
	private final MainGraphExecutor mainGraphExecutor;

	public GraphRunner(CompiledGraph compiledGraph, RunnableConfig config) {
		this(compiledGraph, config, null);
	}

	GraphRunner(CompiledGraph compiledGraph, RunnableConfig config, ParallelBranchPath branchPath) {
		this.compiledGraph = compiledGraph;
		this.config = config;
		this.branchPath = branchPath;
		// Initialize the main execution handler - demonstrates encapsulation
		this.mainGraphExecutor = new MainGraphExecutor();
	}

	public Flux<GraphResponse<NodeOutput>> run(OverAllState initialState) {
		if (compiledGraph.compileConfig.taskCheckpoints()) {
			var saver = compiledGraph.compileConfig.checkpointSaver();
			if (saver.isPresent()) {
				return Flux.using(() -> claim(saver.get(), config),
						claimed -> execute(initialState, claimed.lease().configure(config)), ClaimedRun::close);
			}
		}
		return execute(initialState, config);
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
				GraphRunnerContext context = new GraphRunnerContext(initialState, runConfig, compiledGraph, branchPath);
				var runResult = new AtomicReference<Object>();
				// Delegate to the main execution handler, then expand step continuations
				// iteratively (depth-first). Executors emit a continuation marker as the
				// last element of each step instead of recursively nesting the next step
				// with concatWith(Flux.defer(...)), which grew the reactive operator chain
				// by one layer per executed node and blew the stack on long-running loops
				// (issue #4594). expandDeep drains continuations through a single operator,
				// keeping the subscriber depth constant while preserving emission order.
				return mainGraphExecutor.execute(context, runResult)
					.expandDeep(response -> response.hasContinuation()
							? Flux.defer(() -> response.getContinuation().get()) : Flux.empty())
					.filter(response -> !response.hasContinuation())
					.doFinally(signal -> resultValue.set(runResult.get()));
			}
			catch (Exception e) {
				return Flux.error(e);
			}
		});
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

/*
 * Copyright 2024-2025 the original author or authors.
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

import com.alibaba.cloud.ai.graph.executor.MainGraphExecutor;

import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A reactive graph execution engine based on Project Reactor. This class has been
 * refactored to use OOP principles (inheritance, polymorphism, encapsulation) for better
 * separation of concerns and improved readability.
 */
public class GraphRunner {

	private final CompiledGraph compiledGraph;

	private final OverAllState initialState;

	private final RunnableConfig config;

	private final AtomicReference<Object> resultValue = new AtomicReference<>();

	// Handler for main execution flow - demonstrates encapsulation
	private final MainGraphExecutor mainGraphExecutor;

	public GraphRunner(CompiledGraph compiledGraph, OverAllState initialState, RunnableConfig config) {
		this.compiledGraph = compiledGraph;
		this.initialState = initialState;
		this.config = config;
		// Initialize the main execution handler - demonstrates encapsulation
		this.mainGraphExecutor = new MainGraphExecutor();
	}

	public Flux<GraphResponse<NodeOutput>> run() {
		return Flux.defer(() -> {
			try {
				GraphRunnerContext context = new GraphRunnerContext(initialState, config, compiledGraph);
				// Delegate to the main execution handler - demonstrates polymorphism
				return mainGraphExecutor.execute(context, resultValue);
			}
			catch (Exception e) {
				return Flux.error(e);
			}
		});
	}

	public Optional<Object> resultValue() {
		return Optional.ofNullable(resultValue.get());
	}

}

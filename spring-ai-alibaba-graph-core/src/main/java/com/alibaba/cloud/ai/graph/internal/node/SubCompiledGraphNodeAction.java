/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.graph.internal.node;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.utils.TypeRef;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;

/**
 * Represents an action to perform a subgraph on a given state with a specific
 * configuration.
 *
 * <p>
 * This record encapsulates the behavior required to execute a compiled graph using a
 * provided state. It implements the {@link AsyncNodeActionWithConfig} interface, ensuring
 * that the execution is handled asynchronously with the ability to configure settings.
 * </p>
 *
 * {@link OverAllState}.
 *
 * @param subGraph sub graph instance
 * @see CompiledGraph
 * @see AsyncNodeActionWithConfig
 */
public record SubCompiledGraphNodeAction(String nodeId, CompileConfig parentCompileConfig,
		CompiledGraph subGraph) implements AsyncNodeActionWithConfig {
	public String subGraphId() {
		return format("subgraph_%s", nodeId);
	}

	public String resumeSubGraphId() {
		return format("resume_%s", subGraphId());
	}

	/**
	 * Executes the given graph with the provided state and configuration.
	 * @param state The current state of the system, containing input data and
	 * intermediate results.
	 * @param config The configuration for the graph execution.
	 * @return A {@link CompletableFuture} that will complete with a result of type
	 * {@code Map<String, Object>}. If an exception occurs during execution, the future
	 * will be completed exceptionally.
	 */
	@Override
	public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
		final boolean resumeSubgraph = config.metadata(resumeSubGraphId(), new TypeRef<Boolean>() {
		}).orElse(false);

		RunnableConfig subGraphRunnableConfig = config;
		var parentSaver = parentCompileConfig.checkpointSaver();
		var subGraphSaver = subGraph.compileConfig.checkpointSaver();

		if (subGraphSaver.isPresent()) {
			if (parentSaver.isEmpty()) {
				return CompletableFuture
					.failedFuture(new IllegalStateException("Missing CheckpointSaver in parent graph!"));
			}

			// Check saver are the same instance
			if (parentSaver.get() == subGraphSaver.get()) {
				subGraphRunnableConfig = RunnableConfig.builder()
					.threadId(config.threadId()
						.map(threadId -> format("%s_%s", threadId, subGraphId()))
						.orElseGet(this::subGraphId))
					.build();
			}
		}

		final CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();

		try {
			if (resumeSubgraph) {
				subGraphRunnableConfig = subGraph.updateState(subGraphRunnableConfig, state.data());
			}

			var fluxStream = subGraph.fluxDataStream(state, subGraphRunnableConfig);

			future.complete(Map.of(format("%s_%s", subGraphId(), UUID.randomUUID()), fluxStream));

		}
		catch (Exception e) {

			future.completeExceptionally(e);
		}

		return future;
	}
}

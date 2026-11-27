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
import com.alibaba.cloud.ai.graph.OverAllStateBuilder;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.childConfig;
import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.outputKeyToParent;
import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.resumeSubGraphId;
import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.shouldResume;

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
		CompiledGraph subGraph, UnaryOperator<RunnableConfig> configure) implements AsyncNodeActionWithConfig, ResumableSubGraphAction {

	public SubCompiledGraphNodeAction(String nodeId, CompileConfig parentCompileConfig, CompiledGraph subGraph) {
		this(nodeId, parentCompileConfig, subGraph, UnaryOperator.identity());
	}

	public String getResumeSubGraphId() {
		return resumeSubGraphId(nodeId);
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
		try {
			var subGraphRunnableConfig = configure.apply(
					childConfig(nodeId, config, parentCompileConfig, subGraph.compileConfig).build());
			if (shouldResume(nodeId, config)) {
				if (config.taskId().isEmpty()) {
					subGraphRunnableConfig = subGraph.updateState(subGraphRunnableConfig, state.data());
				}
				return CompletableFuture.completedFuture(Map.of(outputKeyToParent(nodeId),
						subGraph.graphResponseStream(Map.of(), subGraphRunnableConfig)));
			}
			var childState = OverAllStateBuilder.builder().withData(state.data()).withStore(state.getStore())
					.withKeyStrategies(subGraph.getKeyStrategyMap()).build();
			return CompletableFuture.completedFuture(Map.of(outputKeyToParent(nodeId),
					subGraph.graphResponseStream(childState, subGraphRunnableConfig)));
		}
		catch (Exception e) {
			return CompletableFuture.failedFuture(e);
		}
	}
}

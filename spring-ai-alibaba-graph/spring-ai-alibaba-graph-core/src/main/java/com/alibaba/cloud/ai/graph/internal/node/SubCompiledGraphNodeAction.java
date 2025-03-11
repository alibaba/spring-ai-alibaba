package com.alibaba.cloud.ai.graph.internal.node;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.state.AgentState;

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
public record SubCompiledGraphNodeAction(CompiledGraph subGraph) implements AsyncNodeActionWithConfig {

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
		CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();

		try {
			final Map<String, Object> input = (subGraph.compileConfig.checkpointSaver().isPresent()) ? Map.of()
					: state.data();

			var generator = subGraph.stream(input, config);

			future.complete(Map.of("_subgraph", generator));

		}
		catch (Exception e) {

			future.completeExceptionally(e);
		}

		return future;
	}
}
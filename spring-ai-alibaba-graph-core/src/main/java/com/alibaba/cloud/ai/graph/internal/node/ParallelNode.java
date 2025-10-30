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
package com.alibaba.cloud.ai.graph.internal.node;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.streaming.GraphFlux;
import com.alibaba.cloud.ai.graph.streaming.ParallelGraphFlux;
import reactor.core.publisher.Flux;
import com.alibaba.cloud.ai.graph.utils.LifeListenerUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.graph.StateGraph.NODE_AFTER;
import static com.alibaba.cloud.ai.graph.StateGraph.NODE_BEFORE;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class ParallelNode extends Node {

	public static final String PARALLEL_PREFIX = "__PARALLEL__";

	public static String formatNodeId(String nodeId) {
		return format("%s(%s)", PARALLEL_PREFIX, requireNonNull(nodeId, "nodeId cannot be null!"));
	}

	public record AsyncParallelNodeAction(String nodeId, List<AsyncNodeActionWithConfig> actions,
			List<String> actionNodeIds, Map<String, KeyStrategy> channels, CompileConfig compileConfig)
			implements AsyncNodeActionWithConfig {

		private CompletableFuture<Map<String, Object>> evalNodeActionSync(AsyncNodeActionWithConfig action,
				String actualNodeId, OverAllState state, RunnableConfig config) {
			LifeListenerUtil.processListenersLIFO(actualNodeId,
					new LinkedBlockingDeque<>(compileConfig.lifecycleListeners()), state.data(), config, NODE_BEFORE,
					null);
			return action.apply(state, config)
				.whenComplete((stringObjectMap, throwable) -> LifeListenerUtil.processListenersLIFO(actualNodeId,
						new LinkedBlockingDeque<>(compileConfig.lifecycleListeners()), state.data(), config, NODE_AFTER,
						throwable));
		}

	private CompletableFuture<Map<String, Object>> evalNodeActionAsync(AsyncNodeActionWithConfig action,
			String actualNodeId, OverAllState state, RunnableConfig config, Executor executor) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return evalNodeActionSync(action, actualNodeId, state, config).join();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
	}, executor);
	}

	@Override
	public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
		List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();
		for (int i = 0; i < actions.size(); i++) {
			AsyncNodeActionWithConfig action = actions.get(i);
			String actualNodeId = actionNodeIds.get(i);

			CompletableFuture<Map<String, Object>> future = config.metadata(nodeId)
				.filter(value -> value instanceof Executor)
				.map(Executor.class::cast)
				.map(executor -> evalNodeActionAsync(action, actualNodeId, state, config, executor))
				.orElseGet(() -> evalNodeActionSync(action, actualNodeId, state, config));

			futures.add(future);
		}

		// Wait for all tasks to complete
		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(v -> {
			// Collect all results
			List<Map<String, Object>> results = futures.stream()
				.map(CompletableFuture::join)
				.collect(Collectors.toList());

			return processParallelResults(results, state, actions);
		});
	}

		/**
		 * Process parallel execution results, handling GraphFlux, traditional Flux, and regular objects.
		 * Priority: GraphFlux > traditional Flux > regular objects
		 */
		private Map<String, Object> processParallelResults(List<Map<String, Object>> results,
														   OverAllState state, List<AsyncNodeActionWithConfig> actionList) {

			// Check if any result contains GraphFlux or traditional Flux
			List<GraphFlux<?>> graphFluxList = new ArrayList<>();
			List<String> graphFluxNodeIds = new ArrayList<>();

			// Collect non-streaming state
			Map<String, Object> mergedState = new HashMap<>();
			// First pass: collect GraphFlux and traditional Flux instances
			for (int i = 0; i < results.size(); i++) {
				Map<String, Object> result = results.get(i);
				AsyncNodeActionWithConfig action = actionList.get(i);
				String effectiveNodeId = generateEffectiveNodeId(action, i);

				for (Map.Entry<String, Object> entry : result.entrySet()) {
					Object value = entry.getValue();

					if (value instanceof GraphFlux) {
						GraphFlux<?> graphFlux = (GraphFlux<?>) value;
						// Use GraphFlux's own nodeId, or generate one if not set properly
						String graphFluxNodeId = graphFlux.getNodeId() != null ?
								graphFlux.getNodeId() : effectiveNodeId;

						// Create new GraphFlux with correct nodeId if needed
						if (!graphFluxNodeId.equals(graphFlux.getNodeId())) {
							@SuppressWarnings("unchecked")
							GraphFlux<Object> castedFlux = (GraphFlux<Object>) graphFlux;
							@SuppressWarnings("unchecked")
							GraphFlux<Object> newGraphFlux = GraphFlux.of(graphFluxNodeId, entry.getKey(), 
									castedFlux.getFlux(), castedFlux.getMapResult(), castedFlux.getChunkResult());
							graphFlux = newGraphFlux;
						}

						graphFluxList.add(graphFlux);
						graphFluxNodeIds.add(graphFluxNodeId);
					} else if (value instanceof Flux flux) {
						// Traditional Flux - wrap it in GraphFlux for unified processing
						GraphFlux<Object> graphFlux = GraphFlux.of(effectiveNodeId, entry.getKey(), flux, null, null);
						graphFluxList.add(graphFlux);
					} else {
						// Regular object - add to merged state
						Map<String, Object> singleEntryMap = Map.of(entry.getKey(), value);
						mergedState = OverAllState.updateState(mergedState, singleEntryMap, channels);
					}
				}
			}

			// Handle the results based on what we found
			if (!graphFluxList.isEmpty()) {
				// We have GraphFlux instances - create ParallelGraphFlux with node identity preservation
				ParallelGraphFlux parallelGraphFlux = ParallelGraphFlux.of(graphFluxList);

				mergedState.put("__parallel_graph_flux__", parallelGraphFlux);
				return mergedState;
			}  else {
				Map<String, Object> initialState = new HashMap<>();
				// No streaming output, directly merge all results
				return results.stream()
						.reduce(initialState,
								(result, actionResult) -> OverAllState.updateState(result, actionResult, channels));
			}
		}

		/**
		 * Generate effective node ID for parallel execution.
		 * This ensures each parallel branch has a unique and traceable identifier.
		 */
		private String generateEffectiveNodeId(AsyncNodeActionWithConfig action, int index) {
			// Try to extract meaningful identifier from action
			String actionClass = action.getClass().getSimpleName();
			return String.format("%s_parallel_%d_%s", nodeId, index, actionClass);
		}
	}

	public ParallelNode(String id, List<AsyncNodeActionWithConfig> actions, List<String> actionNodeIds,
			Map<String, KeyStrategy> channels, CompileConfig compileConfig) {
		super(formatNodeId(id),
				(config) -> new AsyncParallelNodeAction(formatNodeId(id), actions, actionNodeIds, channels, compileConfig));
	}

	@Override
	public final boolean isParallel() {
		return true;
	}

}

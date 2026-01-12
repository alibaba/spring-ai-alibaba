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
package com.alibaba.cloud.ai.graph.internal.node;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.internal.edge.EdgeCondition;
import com.alibaba.cloud.ai.graph.streaming.GraphFlux;
import com.alibaba.cloud.ai.graph.streaming.ParallelGraphFlux;
import com.alibaba.cloud.ai.graph.utils.LifeListenerUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.graph.StateGraph.NODE_AFTER;
import static com.alibaba.cloud.ai.graph.StateGraph.NODE_BEFORE;
import static com.alibaba.cloud.ai.graph.internal.node.ParallelNode.formatNodeId;

/**
 * A parallel node that dynamically determines which nodes to execute in parallel
 * based on the runtime result of a conditional edge action.
 * 
 * This node is used when a conditional edge action returns multiple target nodes,
 * allowing for dynamic parallel execution based on runtime conditions.
 */
public class ConditionalParallelNode extends Node {

	private static final Logger logger = LoggerFactory.getLogger(ConditionalParallelNode.class);

	public ConditionalParallelNode(
			String sourceNodeId,
			EdgeCondition edgeCondition,
			Map<String, Node.ActionFactory> nodeFactories,
			Map<String, KeyStrategy> channels,
			CompileConfig compileConfig) {
		super(formatNodeId(sourceNodeId), createActionFactory(sourceNodeId, edgeCondition, nodeFactories, channels, compileConfig));
	}

	private static ActionFactory createActionFactory(
			String sourceNodeId,
			EdgeCondition edgeCondition,
			Map<String, Node.ActionFactory> nodeFactories,
			Map<String, KeyStrategy> channels,
			CompileConfig compileConfig) {
		return (config) -> new ConditionalParallelNodeAction(
				formatNodeId(sourceNodeId),
				sourceNodeId,
				edgeCondition,
				nodeFactories,
				channels,
				compileConfig);
	}

	public record ConditionalParallelNodeAction(
			String nodeId,
			String sourceNodeId,
			EdgeCondition edgeCondition,
			Map<String, Node.ActionFactory> nodeFactories,
			Map<String, KeyStrategy> keyStrategyMap,
			CompileConfig compileConfig)
			implements AsyncNodeActionWithConfig {

		@Override
		public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
			// ConditionalParallelNode is only created for multi-command actions in CompiledGraph,
			// so edgeCondition.isMultiCommand() is always true here
			var multiAction = edgeCondition.multiAction();
			if (multiAction == null) {
				logger.error("Expected multi-command action but got null. This should not happen.");
				return CompletableFuture.completedFuture(Map.of());
			}
			
			return multiAction.apply(state, config)
					.thenCompose(multiCommand -> {
						// Map nodes through edgeCondition.mappings()
						List<String> targetNodeIds = multiCommand.gotoNodes().stream()
								.map(route -> {
									String mappedNode = edgeCondition.mappings().get(route);
									if (mappedNode == null) {
										logger.warn("No mapping found for route '{}' in conditional edge, using route as node ID", route);
										return route;
									}
									return mappedNode;
								})
								.collect(Collectors.toList());
						
						Map<String, Object> commandUpdate = multiCommand.update();
						
						// If only one node, execute it directly
						if (targetNodeIds.size() == 1) {
							return executeSingleNode(targetNodeIds.get(0), state, config, commandUpdate);
						}
						
						// Multiple nodes - execute in parallel
						return executeParallelNodes(targetNodeIds, state, config, commandUpdate);
					});
		}

		private CompletableFuture<Map<String, Object>> executeSingleNode(
				String nodeId,
				OverAllState state,
				RunnableConfig config,
				Map<String, Object> commandUpdate) {
			Node.ActionFactory factory = nodeFactories.get(nodeId);
			if (factory == null) {
				logger.error("Node factory not found for node: {}", nodeId);
				return CompletableFuture.completedFuture(commandUpdate);
			}

			try {
				AsyncNodeActionWithConfig action = factory.apply(compileConfig);
				OverAllState stateSnapshot = state.snapShot().orElse(new OverAllState());
				// Apply command updates to state snapshot
				if (!commandUpdate.isEmpty()) {
					stateSnapshot.updateStateWithKeyStrategies(commandUpdate, keyStrategyMap);
				}

				return evalNodeActionSync(action, nodeId, stateSnapshot, config)
						.thenApply(result -> {
							// Merge command updates with node result
							Map<String, Object> merged = new HashMap<>(commandUpdate);
							merged.putAll(result);
							return merged;
						});
			} catch (Exception e) {
				logger.error("Error executing single node {}: {}", nodeId, e.getMessage(), e);
				return CompletableFuture.completedFuture(commandUpdate);
			}
		}

		private CompletableFuture<Map<String, Object>> executeParallelNodes(
				List<String> targetNodeIds,
				OverAllState state,
				RunnableConfig config,
				Map<String, Object> commandUpdate) {
			
			// Build actions and actionNodeIds dynamically
			List<AsyncNodeActionWithConfig> actions = new ArrayList<>();
			List<String> actionNodeIds = new ArrayList<>();

			for (String targetNodeId : targetNodeIds) {
				Node.ActionFactory factory = nodeFactories.get(targetNodeId);
				if (factory == null) {
					logger.warn("Node factory not found for node: {}, skipping", targetNodeId);
					continue;
				}

				try {
					AsyncNodeActionWithConfig action = factory.apply(compileConfig);
					actions.add(action);
					actionNodeIds.add(targetNodeId);
				} catch (Exception e) {
					logger.error("Error creating action for node {}: {}", targetNodeId, e.getMessage(), e);
				}
			}

			if (actions.isEmpty()) {
				logger.warn("No valid actions found for parallel execution");
				return CompletableFuture.completedFuture(commandUpdate);
			}

			// Execute all actions in parallel
			// Core logic aligned with ParallelNode.AsyncParallelNodeAction.apply()
			List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();
			for (int i = 0; i < actions.size(); i++) {
				AsyncNodeActionWithConfig action = actions.get(i);
				String actualNodeId = actionNodeIds.get(i);

				// Create a defensive copy of the state for each parallel action
				// This prevents race conditions if actions modify the state in-place
				OverAllState stateSnapshot = state.snapShot().orElse(new OverAllState());
				
				// Apply command updates to state snapshot (specific to ConditionalParallelNode)
				if (!commandUpdate.isEmpty()) {
					stateSnapshot.updateStateWithKeyStrategies(commandUpdate, keyStrategyMap);
				}

				// First try to get node-specific executor, then default executor, finally use DEFAULT_EXECUTOR
				// Use nodeId (ConditionalParallelNode's formatted ID) for executor configuration, same as ParallelNode
				Executor executor = ParallelNode.getExecutor(config, this.nodeId);

				CompletableFuture<Map<String, Object>> future = evalNodeActionAsync(
						action, actualNodeId, stateSnapshot, config, executor);
				futures.add(future);
			}

			// Wait for all tasks to complete
			return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(v -> {
				// Collect all results
				List<Map<String, Object>> results = futures.stream()
						.map(CompletableFuture::join)
						.collect(Collectors.toList());

				// Process parallel results (same as ParallelNode)
				Map<String, Object> mergedResult = processParallelResults(results, state, actions);
				
				// Merge with command updates (specific to ConditionalParallelNode)
				Map<String, Object> finalResult = new HashMap<>(commandUpdate);
				finalResult.putAll(mergedResult);
				
				return finalResult;
			});
		}

		private CompletableFuture<Map<String, Object>> evalNodeActionSync(
				AsyncNodeActionWithConfig action,
				String actualNodeId,
				OverAllState state,
				RunnableConfig config) {
			LifeListenerUtil.processListenersLIFO(actualNodeId,
					new LinkedBlockingDeque<>(compileConfig.lifecycleListeners()), state.data(), config, NODE_BEFORE,
					null);
			return action.apply(state, config)
					.whenComplete((result, throwable) -> LifeListenerUtil.processListenersLIFO(actualNodeId,
							new LinkedBlockingDeque<>(compileConfig.lifecycleListeners()), state.data(), config,
							NODE_AFTER,
							throwable));
		}

		private CompletableFuture<Map<String, Object>> evalNodeActionAsync(
				AsyncNodeActionWithConfig action,
				String actualNodeId,
				OverAllState state,
				RunnableConfig config,
				Executor executor) {

			if (executor instanceof ThreadPoolExecutor threadPoolExecutor) {
				logger.debug("Thread pool metrics - Active threads: {}, Pool size: {}, Queue size: {}, Completed tasks: {}",
						threadPoolExecutor.getActiveCount(),
						threadPoolExecutor.getPoolSize(),
						threadPoolExecutor.getQueue().size(),
						threadPoolExecutor.getCompletedTaskCount());
			}

			logger.debug("Submitting task for node {} to executor", actualNodeId);
			return CompletableFuture.supplyAsync(() -> {
				try {
					logger.debug("Executing task for node {} in thread {}", actualNodeId, Thread.currentThread().getName());
					return evalNodeActionSync(action, actualNodeId, state, config).join();
				} catch (Exception e) {
					logger.error("Error executing task for node {}", actualNodeId, e);
					throw new RuntimeException(e);
				}
			}, executor);
		}

		/**
		 * Process parallel execution results, handling GraphFlux, traditional Flux, and regular objects.
		 * Priority: GraphFlux > traditional Flux > regular objects
		 */
		private Map<String, Object> processParallelResults(
				List<Map<String, Object>> results,
				OverAllState state,
				List<AsyncNodeActionWithConfig> actionList) {

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
						String graphFluxNodeId = graphFlux.getNodeId() != null ? graphFlux.getNodeId() : effectiveNodeId;

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
						mergedState = OverAllState.updateState(mergedState, singleEntryMap, keyStrategyMap);
					}
				}
			}

			// Handle the results based on what we found
			if (!graphFluxList.isEmpty()) {
				// We have GraphFlux instances - create ParallelGraphFlux
				ParallelGraphFlux parallelGraphFlux = ParallelGraphFlux.of(graphFluxList);
				mergedState.put("__parallel_graph_flux__", parallelGraphFlux);
				return mergedState;
			} else {
				// No streaming output, directly merge all results
				Map<String, Object> initialState = new HashMap<>();
				return results.stream()
						.reduce(initialState,
								(result, actionResult) -> OverAllState.updateState(result, actionResult, keyStrategyMap));
			}
		}

		/**
		 * Generate effective node ID for parallel execution.
		 */
		private String generateEffectiveNodeId(AsyncNodeActionWithConfig action, int index) {
			String actionClass = action.getClass().getSimpleName();
			return String.format("%s_conditional_parallel_%d_%s", nodeId, index, actionClass);
		}
	}

	@Override
	public final boolean isParallel() {
		return true;
	}
}


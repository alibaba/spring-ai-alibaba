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
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.streaming.AsyncGeneratorUtils;
import com.alibaba.cloud.ai.graph.utils.LifeListenerUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Function;
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
			Map<String, KeyStrategy> channels, CompileConfig compileConfig) implements AsyncNodeActionWithConfig {

		@SuppressWarnings("unchecked")
		private CompletableFuture<Map<String, Object>> evalNodeActionSync(AsyncNodeActionWithConfig action,
				OverAllState state, RunnableConfig config) {
			LifeListenerUtil.processListenersLIFO(nodeId, new LinkedBlockingDeque<>(compileConfig.lifecycleListeners()),
					state.data(), config, NODE_BEFORE, null);
			return action.apply(state, config)
				.whenComplete((stringObjectMap, throwable) -> LifeListenerUtil.processListenersLIFO(nodeId,
						new LinkedBlockingDeque<>(compileConfig.lifecycleListeners()), state.data(), config, NODE_AFTER,
						throwable));
		}

		private CompletableFuture<Map<String, Object>> evalNodeActionAsync(AsyncNodeActionWithConfig action,
				OverAllState state, RunnableConfig config, Executor executor) {
			return CompletableFuture.supplyAsync(() -> {
				try {
					return evalNodeActionSync(action, state, config).join();
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}, executor);
		}

		@Override
		public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
			Function<AsyncNodeActionWithConfig, CompletableFuture<Map<String, Object>>> evalNodeAction = config
				.metadata(nodeId)
				.filter(value -> value instanceof Executor)
				.map(Executor.class::cast)
				.map(executor -> (Function<AsyncNodeActionWithConfig, CompletableFuture<Map<String, Object>>>) action -> evalNodeActionAsync(
						action, state, config, executor))
				.orElseGet(() -> action -> evalNodeActionSync(action, state, config));

			// Execute all actions in parallel
			List<CompletableFuture<Map<String, Object>>> futures = actions.stream().map(evalNodeAction).toList();

			// Wait for all tasks to complete
			return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(v -> {
				// Collect all results
				List<Map<String, Object>> results = futures.stream()
					.map(CompletableFuture::join)
					.collect(Collectors.toList());

				// Check if any result contains streaming output
				boolean hasGenerator = results.stream()
					.flatMap(map -> map.values().stream())
					.anyMatch(value -> value instanceof AsyncGenerator);

				if (hasGenerator) {
					// If there is any streaming output, create a new AsyncGenerator to
					// wrap all streaming outputs
					List<AsyncGenerator<NodeOutput>> generators = new ArrayList<>();
					Map<String, Object> mergedState = new HashMap<>();
					mergedState.putAll(state.data());
					for (Map<String, Object> result : results) {
						Map<String, Object> nonGeneratorEntries = result.entrySet()
							.stream()
							.filter(e -> !(e.getValue() instanceof AsyncGenerator))
							.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

						mergedState = OverAllState.updateState(mergedState, nonGeneratorEntries, channels);

						// Collect all streaming outputs
						result.entrySet()
							.stream()
							.filter(e -> e.getValue() instanceof AsyncGenerator)
							.forEach(e -> generators.add((AsyncGenerator<NodeOutput>) e.getValue()));
					}

					// If there are streaming outputs, merge them into one streaming
					// output
					if (!generators.isEmpty()) {
						// Create a merged AsyncGenerator
						AsyncGenerator<NodeOutput> mergedGenerator = AsyncGeneratorUtils
							.createMergedGenerator(generators, state.keyStrategies());
						mergedState.put("__merged_stream__", mergedGenerator);
					}

					return mergedState;
				}
				else {
					// No streaming output, directly merge all results
					return results.stream()
						.reduce(state.data(),
								(result, actionResult) -> OverAllState.updateState(result, actionResult, channels));
				}
			});
		}
	}

	public ParallelNode(String id, List<AsyncNodeActionWithConfig> actions, Map<String, KeyStrategy> channels,
			CompileConfig compileConfig) {
		super(formatNodeId(id),
				(config) -> new AsyncParallelNodeAction(formatNodeId(id), actions, channels, compileConfig));
	}

	@Override
	public final boolean isParallel() {
		return true;
	}

}

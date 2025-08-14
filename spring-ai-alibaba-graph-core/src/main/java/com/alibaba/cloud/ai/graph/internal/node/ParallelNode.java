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
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.async.internal.reactive.GeneratorSubscriber;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class ParallelNode extends Node {

	public static final String PARALLEL_PREFIX = "__PARALLEL__";

	public static String formatNodeId(String nodeId) {
		return format("%s(%s)", PARALLEL_PREFIX, requireNonNull(nodeId, "nodeId cannot be null!"));
	}

	public record AsyncParallelNodeAction(String nodeId, List<AsyncNodeActionWithConfig> actions,
			Map<String, KeyStrategy> channels, CompileConfig compileConfig) implements AsyncNodeActionWithConfig {

		@Override
		public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
			// 使用线程安全的ConcurrentHashMap来避免并发问题
			Map<String, Object> partialMergedStates = new java.util.concurrent.ConcurrentHashMap<>();
			Map<String, Object> asyncGenerators = new java.util.concurrent.ConcurrentHashMap<>();

			// 获取配置中的自定义执行器，如果没有则使用默认的ForkJoinPool.commonPool()
			Executor executor = config.metadata(this.nodeId)
				.filter(obj -> obj instanceof Executor)
				.map(obj -> (Executor) obj)
				.orElse(ForkJoinPool.commonPool());

			CompletableFuture<?>[] futures = actions.stream()
				.map((Function<AsyncNodeActionWithConfig, CompletableFuture<?>>) action -> {
					// 使用线程池异步执行每个action
					return CompletableFuture.supplyAsync(() -> action.apply(state, config), executor)
						.thenCompose(Function.identity())
						.thenApply(partialState -> {
							partialState.forEach((key, value) -> {
								if (value instanceof AsyncGenerator<?> || value instanceof GeneratorSubscriber) {
									((List) asyncGenerators.computeIfAbsent(key, k -> new ArrayList<>())).add(value);
								}
								else {
									// 修复：使用KeyStrategy正确合并状态，而不是直接覆盖
									KeyStrategy strategy = channels.get(key);
									if (strategy != null) {
										// 使用原子操作来确保线程安全
										partialMergedStates.compute(key,
												(k, existingValue) -> strategy.apply(existingValue, value));
									}
									else {
										// 如果没有配置KeyStrategy，使用默认的替换策略
										partialMergedStates.put(key, value);
									}
								}
							});
							return action;
						});
				})
				.toArray(CompletableFuture[]::new);

			return CompletableFuture.allOf(futures).thenApplyAsync((p) -> {
				// 在所有并行节点完成后，统一更新状态
				if (!CollectionUtils.isEmpty(partialMergedStates)) {
					state.updateState(partialMergedStates);
				}
				return CollectionUtils.isEmpty(asyncGenerators) ? state.data() : asyncGenerators;
			}, executor);
		}

	}

	public ParallelNode(String id, List<AsyncNodeActionWithConfig> actions, Map<String, KeyStrategy> channels,
			CompileConfig compileConfig) {
		super(format("%s(%s)", PARALLEL_PREFIX, id),
				(config) -> new AsyncParallelNodeAction(format("%s(%s)", PARALLEL_PREFIX, id), actions, channels,
						compileConfig));
	}

	@Override
	public final boolean isParallel() {
		return true;
	}

}

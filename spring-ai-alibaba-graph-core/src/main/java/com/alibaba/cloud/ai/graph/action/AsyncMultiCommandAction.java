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
package com.alibaba.cloud.ai.graph.action;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.opentelemetry.context.Context;

/**
 * Interface for actions that can return multiple target nodes for parallel execution.
 * This is used when a conditional edge needs to route to multiple nodes simultaneously.
 * 
 * @see AsyncCommandAction for single-node routing
 * @see MultiCommand for the return type
 */
public interface AsyncMultiCommandAction extends BiFunction<OverAllState, RunnableConfig, CompletableFuture<MultiCommand>> {

	/**
	 * Creates an AsyncMultiCommandAction from a synchronous MultiCommandAction.
	 * 
	 * @param syncAction the synchronous action
	 * @return an AsyncMultiCommandAction wrapping the sync action
	 */
	static AsyncMultiCommandAction node_async(MultiCommandAction syncAction) {
		return (state, config) -> {
			Context context = Context.current();
			var result = new CompletableFuture<MultiCommand>();
			try {
				result.complete(syncAction.apply(state, config));
			} catch (Exception e) {
				result.completeExceptionally(e);
			}
			return result;
		};
	}

	/**
	 * Creates an AsyncMultiCommandAction from a function that returns a list of node IDs.
	 * 
	 * @param action a function that returns a list of node IDs
	 * @return an AsyncMultiCommandAction wrapping the function
	 */
	static AsyncMultiCommandAction of(Function<OverAllState, CompletableFuture<List<String>>> action) {
		return (state, config) -> action.apply(state)
				.thenApply(nodeIds -> new MultiCommand(nodeIds));
	}

	/**
	 * Creates an AsyncMultiCommandAction from a BiFunction that returns a list of node IDs.
	 * 
	 * @param action a BiFunction that returns a list of node IDs
	 * @return an AsyncMultiCommandAction wrapping the function
	 */
	static AsyncMultiCommandAction of(BiFunction<OverAllState, RunnableConfig, CompletableFuture<List<String>>> action) {
		return (state, config) -> action.apply(state, config)
				.thenApply(MultiCommand::new);
	}
}


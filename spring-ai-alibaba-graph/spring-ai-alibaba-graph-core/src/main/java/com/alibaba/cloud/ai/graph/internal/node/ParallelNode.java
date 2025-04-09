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

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;

public class ParallelNode extends Node {

	public static final String PARALLEL_PREFIX = "__PARALLEL__";

	record AsyncParallelNodeAction(List<AsyncNodeActionWithConfig> actions,
			Map<String, KeyStrategy> channels) implements AsyncNodeActionWithConfig {

		@Override
		public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
			var futures = actions.stream().map(action -> action.apply(state, config).thenApply(partialState -> {
				state.updateState(partialState);
				return action;
			}))
				// .map( future -> supplyAsync(future::join) )
				.toList()
				.toArray(new CompletableFuture[0]);
			return CompletableFuture.allOf(futures).thenApply((p) -> state.data());
		}

	}

	public ParallelNode(String id, List<AsyncNodeActionWithConfig> actions, Map<String, KeyStrategy> channels) {
		super(format("%s(%s)", PARALLEL_PREFIX, id), (config) -> new AsyncParallelNodeAction(actions, channels));
	}

	@Override
	public final boolean isParallel() {
		return true;
	}

}

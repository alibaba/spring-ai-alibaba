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
package com.alibaba.cloud.ai.graph.action;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public interface AsyncNodeActionWithConfig
		extends BiFunction<OverAllState, RunnableConfig, CompletableFuture<Map<String, Object>>> {

	/**
	 * Applies this action to the given agent state.
	 * @param state the agent state
	 * @return a CompletableFuture representing the result of the action
	 */
	CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config);

	static AsyncNodeActionWithConfig node_async(NodeActionWithConfig syncAction) {
		return (state, config) -> {
			CompletableFuture<Map<String, Object>> result = new CompletableFuture<>();
			try {
				result.complete(syncAction.apply(state, config));
			}
			catch (Exception e) {
				result.completeExceptionally(e);
			}
			return result;
		};
	}

	/**
	 * Adapts a simple AsyncNodeAction to an AsyncNodeActionWithConfig.
	 * @param action the simple AsyncNodeAction to be adapted
	 * @return an AsyncNodeActionWithConfig that wraps the given AsyncNodeAction
	 */
	static AsyncNodeActionWithConfig of(AsyncNodeAction action) {
		return (t, config) -> action.apply(t);
	}

}

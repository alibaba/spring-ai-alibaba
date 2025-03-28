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

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Represents an asynchronous edge action that operates on an agent state and returns a
 * new route.
 *
 */
@FunctionalInterface
public interface AsyncEdgeAction extends Function<OverAllState, CompletableFuture<String>> {

	/**
	 * Applies this action to the given agent state.
	 * @param t the agent state
	 * @return a CompletableFuture representing the result of the action
	 */
	CompletableFuture<String> apply(OverAllState t);

	/**
	 * Creates an asynchronous edge action from a synchronous edge action.
	 * @param syncAction the synchronous edge action
	 * @return an asynchronous edge action
	 */
	static AsyncEdgeAction edge_async(EdgeAction syncAction) {
		return t -> {
			CompletableFuture<String> result = new CompletableFuture<>();
			try {
				result.complete(syncAction.apply(t));
			}
			catch (Exception e) {
				result.completeExceptionally(e);
			}
			return result;
		};
	}

}

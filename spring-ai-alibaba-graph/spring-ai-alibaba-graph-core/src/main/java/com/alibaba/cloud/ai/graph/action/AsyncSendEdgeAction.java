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
package com.alibaba.cloud.ai.graph.action;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.Send;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * The interface Async send edge action.
 */
@FunctionalInterface
public interface AsyncSendEdgeAction extends Function<OverAllState, CompletableFuture<Send>> {

	/**
	 * Applies this action to the given agent state.
	 * @param t the agent state
	 * @return a CompletableFuture representing the result of the action
	 */
	CompletableFuture<Send> apply(OverAllState t);

	/**
	 * Creates an asynchronous edge action from a synchronous edge action.
	 * @param syncAction the synchronous edge action
	 * @return an asynchronous edge action
	 */
	static AsyncSendEdgeAction edge_async(SendEdgeAction syncAction) {
		return t -> {
			CompletableFuture<Send> result = new CompletableFuture<>();
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

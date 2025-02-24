package com.alibaba.cloud.ai.graph.action;

import com.alibaba.cloud.ai.graph.OverAllState;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Represents an asynchronous node action that operates on an agent state and returns
 * state update.
 *
 */
@FunctionalInterface
public interface AsyncNodeAction extends Function<OverAllState, CompletableFuture<Map<String, Object>>> {

	/**
	 * Applies this action to the given agent state.
	 * @param t the agent state
	 * @return a CompletableFuture representing the result of the action
	 */
	CompletableFuture<Map<String, Object>> apply(OverAllState t);

	/**
	 * Creates an asynchronous node action from a synchronous node action.
	 * @param syncAction the synchronous node action
	 * @return an asynchronous node action
	 */
	static AsyncNodeAction node_async(NodeAction syncAction) {
		return t -> {
			CompletableFuture<Map<String, Object>> result = new CompletableFuture<>();
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

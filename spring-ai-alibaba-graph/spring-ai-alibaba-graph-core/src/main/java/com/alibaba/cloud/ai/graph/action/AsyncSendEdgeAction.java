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

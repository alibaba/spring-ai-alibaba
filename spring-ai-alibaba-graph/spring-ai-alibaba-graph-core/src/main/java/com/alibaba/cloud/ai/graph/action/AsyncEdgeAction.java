package com.alibaba.cloud.ai.graph.action;

import com.alibaba.cloud.ai.graph.state.NodeState;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Represents an asynchronous edge action that operates on an agent state and returns a
 * new route.
 *
 */
@FunctionalInterface
public interface AsyncEdgeAction extends Function<NodeState, CompletableFuture<String>> {

	/**
	 * Applies this action to the given agent state.
	 * @param t the agent state
	 * @return a CompletableFuture representing the result of the action
	 */
	CompletableFuture<String> apply(NodeState t);

	/**
	 * Creates an asynchronous edge action from a synchronous edge action.
	 * @param syncAction the synchronous edge action
	 * @param <S> the type of the agent state
	 * @return an asynchronous edge action
	 */
	static <S extends NodeState> AsyncEdgeAction edge_async(EdgeAction syncAction) {
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

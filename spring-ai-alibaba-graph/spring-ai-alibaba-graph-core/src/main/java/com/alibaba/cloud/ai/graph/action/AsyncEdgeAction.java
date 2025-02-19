package com.alibaba.cloud.ai.graph.action;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.alibaba.cloud.ai.graph.state.AgentState;

/**
 * Represents an asynchronous edge action that operates on an agent state and returns a
 * new route.
 *
 * @param <S> the type of the agent state
 */
@FunctionalInterface
public interface AsyncEdgeAction<S extends AgentState> extends Function<S, CompletableFuture<String>> {

	/**
	 * Applies this action to the given agent state.
	 * @param t the agent state
	 * @return a CompletableFuture representing the result of the action
	 */
	CompletableFuture<String> apply(S t);

	/**
	 * Creates an asynchronous edge action from a synchronous edge action.
	 * @param syncAction the synchronous edge action
	 * @param <S> the type of the agent state
	 * @return an asynchronous edge action
	 */
	static <S extends AgentState> AsyncEdgeAction<S> edge_async(EdgeAction<S> syncAction) {
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

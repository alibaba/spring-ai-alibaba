package com.alibaba.cloud.ai.graph.internal.node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.state.AgentState;
import com.alibaba.cloud.ai.graph.state.Channel;

import static java.lang.String.format;

public class ParallelNode<State extends AgentState> extends Node<State> {

	public static final String PARALLEL_PREFIX = "__PARALLEL__";

	record AsyncParallelNodeAction<State extends AgentState>(List<AsyncNodeActionWithConfig<State>> actions,
			Map<String, Channel<?>> channels) implements AsyncNodeActionWithConfig<State> {

		@Override
		public CompletableFuture<Map<String, Object>> apply(State state, RunnableConfig config) {
			final var partialMergedStates = new HashMap<String, Object>();
			var futures = actions.stream().map(action -> action.apply(state, config).thenApply(partialState -> {
				var updatedState = AgentState.updateState(partialMergedStates, partialState, channels);
				partialMergedStates.putAll(updatedState);
				return action;
			}))
				// .map( future -> supplyAsync(future::join) )
				.toList()
				.toArray(new CompletableFuture[0]);
			return CompletableFuture.allOf(futures).thenApply((p) -> partialMergedStates);
		}

	}

	public ParallelNode(String id, List<AsyncNodeActionWithConfig<State>> actions, Map<String, Channel<?>> channels) {
		super(format("%s(%s)", PARALLEL_PREFIX, id), (config) -> new AsyncParallelNodeAction<>(actions, channels));
	}

	@Override
	public final boolean isParallel() {
		return true;
	}

}

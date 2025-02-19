package com.alibaba.cloud.ai.graph.state;

import lombok.NonNull;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;

import static java.lang.String.format;

public final class StateSnapshot<State extends AgentState> extends NodeOutput<State> {

	private final RunnableConfig config;

	public String next() {
		return config.nextNode().orElse(null);
	}

	public RunnableConfig config() {
		return config;
	}

	/**
	 * @deprecated Use {@link #config()} instead.
	 */
	@Deprecated
	public RunnableConfig getConfig() {
		return config();
	}

	/**
	 * @deprecated Use {@link #next()} instead.
	 */
	@Deprecated
	public String getNext() {
		return next();
	}

	private StateSnapshot(@NonNull String node, @NonNull State state, @NonNull RunnableConfig config) {
		super(node, state);
		this.config = config;
	}

	@Override
	public String toString() {

		return format("StateSnapshot{node=%s, state=%s, config=%s}", node(), state(), config());
	}

	public static <State extends AgentState> StateSnapshot<State> of(Checkpoint checkpoint, RunnableConfig config,
			AgentStateFactory<State> factory) {

		RunnableConfig newConfig = RunnableConfig.builder(config)
			.checkPointId(checkpoint.getId())
			.nextNode(checkpoint.getNextNodeId())
			.build();
		return new StateSnapshot<>(checkpoint.getNodeId(), factory.apply(checkpoint.getState()), newConfig);
	}

}

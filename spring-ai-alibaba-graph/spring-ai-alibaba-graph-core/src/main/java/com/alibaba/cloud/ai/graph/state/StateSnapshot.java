package com.alibaba.cloud.ai.graph.state;

import com.alibaba.cloud.ai.graph.OverAllState;
import lombok.NonNull;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;

import static java.lang.String.format;

public final class StateSnapshot extends NodeOutput {

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

	private StateSnapshot(@NonNull String node, @NonNull OverAllState state, @NonNull RunnableConfig config) {
		super(node, state);
		this.config = config;
	}

	@Override
	public String toString() {

		return format("StateSnapshot{node=%s, state=%s, config=%s}", node(), state(), config());
	}

	public static  StateSnapshot of(Checkpoint checkpoint, RunnableConfig config,
			AgentStateFactory<OverAllState> factory) {

		RunnableConfig newConfig = RunnableConfig.builder(config)
			.checkPointId(checkpoint.getId())
			.nextNode(checkpoint.getNextNodeId())
			.build();
		return new StateSnapshot(checkpoint.getNodeId(), factory.apply(checkpoint.getState()), newConfig);
	}

}

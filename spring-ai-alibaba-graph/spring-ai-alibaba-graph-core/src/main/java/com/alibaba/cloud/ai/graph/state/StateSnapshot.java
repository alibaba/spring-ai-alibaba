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

	public static StateSnapshot of(Checkpoint checkpoint, RunnableConfig config,
			AgentStateFactory<OverAllState> factory) {

		RunnableConfig newConfig = RunnableConfig.builder(config)
			.checkPointId(checkpoint.getId())
			.nextNode(checkpoint.getNextNodeId())
			.build();
		return new StateSnapshot(checkpoint.getNodeId(), factory.apply(checkpoint.getState()), newConfig);
	}

}

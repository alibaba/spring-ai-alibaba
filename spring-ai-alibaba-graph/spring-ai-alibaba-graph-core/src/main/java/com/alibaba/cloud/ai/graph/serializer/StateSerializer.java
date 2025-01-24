package com.alibaba.cloud.ai.graph.serializer;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;
import lombok.NonNull;
import com.alibaba.cloud.ai.graph.state.NodeState;

import java.io.IOException;
import java.util.Map;

public abstract class StateSerializer implements Serializer<OverAllState> {

	private final AgentStateFactory stateFactory;

	protected StateSerializer(@NonNull AgentStateFactory stateFactory) {
		this.stateFactory = stateFactory;
	}

	public final AgentStateFactory stateFactory() {
		return stateFactory;
	}

	public final OverAllState stateOf(@NonNull Map<String, Object> data) {
		return stateFactory.apply(data);
	}

	public final OverAllState cloneObject(@NonNull Map<String, Object> data) throws IOException, ClassNotFoundException {
		return cloneObject(stateFactory().apply(data));
	}

}

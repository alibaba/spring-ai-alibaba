package com.alibaba.cloud.ai.graph.serializer;

import java.io.IOException;
import java.util.Map;

import lombok.NonNull;
import com.alibaba.cloud.ai.graph.state.AgentState;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;

public abstract class StateSerializer<State extends AgentState> implements Serializer<State> {

	private final AgentStateFactory<State> stateFactory;

	protected StateSerializer(@NonNull AgentStateFactory<State> stateFactory) {
		this.stateFactory = stateFactory;
	}

	public final AgentStateFactory<State> stateFactory() {
		return stateFactory;
	}

	public final State stateOf(@NonNull Map<String, Object> data) {
		return stateFactory.apply(data);
	}

	public final State cloneObject(@NonNull Map<String, Object> data) throws IOException, ClassNotFoundException {
		return cloneObject(stateFactory().apply(data));
	}

}

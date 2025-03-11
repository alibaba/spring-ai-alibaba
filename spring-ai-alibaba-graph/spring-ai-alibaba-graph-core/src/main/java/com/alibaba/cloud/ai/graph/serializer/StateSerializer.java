package com.alibaba.cloud.ai.graph.serializer;

import java.io.IOException;
import java.util.Map;

import com.alibaba.cloud.ai.graph.OverAllState;
import lombok.NonNull;
import com.alibaba.cloud.ai.graph.state.AgentState;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;

public abstract class StateSerializer<T> implements Serializer<T> {

	private final AgentStateFactory<T> stateFactory;

	protected StateSerializer(@NonNull AgentStateFactory<T> stateFactory) {
		this.stateFactory = stateFactory;
	}

	public final AgentStateFactory<T> stateFactory() {
		return stateFactory;
	}

	public final T stateOf(@NonNull Map<String, Object> data) {
		return stateFactory.apply(data);
	}

	public final T cloneObject(@NonNull Map<String, Object> data) throws IOException, ClassNotFoundException {
		return cloneObject(stateFactory().apply(data));
	}

}

package com.alibaba.cloud.ai.graph.internal.node;

import lombok.NonNull;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.SubGraphNode;
import com.alibaba.cloud.ai.graph.state.AgentState;

public class SubCompiledGraphNode<State extends AgentState> extends Node<State> implements SubGraphNode<State> {

	private final CompiledGraph<State> subGraph;

	public SubCompiledGraphNode(@NonNull String id, @NonNull CompiledGraph<State> subGraph) {
		super(id, (config) -> new SubCompiledGraphNodeAction<>(subGraph));
		this.subGraph = subGraph;
	}

	public StateGraph<State> subGraph() {
		return subGraph.stateGraph;
	}

}

package com.alibaba.cloud.ai.graph.internal.node;

import lombok.NonNull;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.SubGraphNode;
import com.alibaba.cloud.ai.graph.state.AgentState;

public class SubStateGraphNode<State extends AgentState> extends Node<State> implements SubGraphNode<State> {

	private final StateGraph<State> subGraph;

	public SubStateGraphNode(@NonNull String id, @NonNull StateGraph<State> subGraph) {
		super(id);
		this.subGraph = subGraph;
	}

	public StateGraph<State> subGraph() {
		return subGraph;
	}

	public String formatId(String nodeId) {
		return SubGraphNode.formatId(id(), nodeId);
	}

}

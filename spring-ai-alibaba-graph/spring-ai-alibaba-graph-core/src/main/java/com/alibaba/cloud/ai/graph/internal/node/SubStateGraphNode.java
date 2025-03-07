package com.alibaba.cloud.ai.graph.internal.node;

import lombok.NonNull;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.SubGraphNode;
import com.alibaba.cloud.ai.graph.state.AgentState;

public class SubStateGraphNode extends Node implements SubGraphNode {

	private final StateGraph subGraph;

	public SubStateGraphNode(@NonNull String id, @NonNull StateGraph subGraph) {
		super(id);
		this.subGraph = subGraph;
	}

	public StateGraph subGraph() {
		return subGraph;
	}

	public String formatId(String nodeId) {
		return SubGraphNode.formatId(id(), nodeId);
	}

}

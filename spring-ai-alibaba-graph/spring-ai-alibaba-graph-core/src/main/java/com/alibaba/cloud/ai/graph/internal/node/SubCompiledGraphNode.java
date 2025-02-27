package com.alibaba.cloud.ai.graph.internal.node;

import lombok.NonNull;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.SubGraphNode;
import com.alibaba.cloud.ai.graph.state.AgentState;

public class SubCompiledGraphNode extends Node implements SubGraphNode {

	private final CompiledGraph subGraph;

	public SubCompiledGraphNode(@NonNull String id, @NonNull CompiledGraph subGraph) {
		super(id, (config) -> new SubCompiledGraphNodeAction(subGraph));
		this.subGraph = subGraph;
	}

	public StateGraph subGraph() {
		return subGraph.stateGraph;
	}

}

package com.alibaba.cloud.ai.graph.action;

import com.alibaba.cloud.ai.graph.state.NodeState;
import com.alibaba.cloud.ai.graph.NodeActionDescriptor;

import java.util.Map;

@FunctionalInterface
public interface NodeAction {

	Map<String, Object> apply(NodeState t) throws Exception;

	default NodeActionDescriptor getNodeActionDescriptor() {
		return NodeActionDescriptor.EMPTY;
	}

}

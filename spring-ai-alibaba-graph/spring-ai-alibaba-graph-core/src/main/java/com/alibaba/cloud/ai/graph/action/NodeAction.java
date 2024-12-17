package com.alibaba.cloud.ai.graph.action;

import com.alibaba.cloud.ai.graph.NodeActionDescriptor;
import com.alibaba.cloud.ai.graph.state.AgentState;

import java.util.Map;

@FunctionalInterface
public interface NodeAction<T extends AgentState> {

	Map<String, Object> apply(T t) throws Exception;

	default NodeActionDescriptor getNodeAttributes(){
		return NodeActionDescriptor.EMPTY;
	}

}

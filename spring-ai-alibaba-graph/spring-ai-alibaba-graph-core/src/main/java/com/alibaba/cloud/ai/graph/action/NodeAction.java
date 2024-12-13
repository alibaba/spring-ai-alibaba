package com.alibaba.cloud.ai.graph.action;

import com.alibaba.cloud.ai.graph.state.NodeState;

import java.util.Map;

@FunctionalInterface
public interface NodeAction {

	Map<String, Object> apply(NodeState t) throws Exception;

}

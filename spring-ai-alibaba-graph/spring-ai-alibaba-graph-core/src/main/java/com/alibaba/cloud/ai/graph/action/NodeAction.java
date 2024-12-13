package com.alibaba.cloud.ai.graph.action;

import com.alibaba.cloud.ai.graph.state.NodeState;

import java.util.Map;

@FunctionalInterface
public interface NodeAction<T extends NodeState> {

	Map<String, Object> apply(T t) throws Exception;

}

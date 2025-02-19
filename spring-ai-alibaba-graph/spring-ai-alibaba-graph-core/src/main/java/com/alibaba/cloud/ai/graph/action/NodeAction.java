package com.alibaba.cloud.ai.graph.action;

import java.util.Map;

import com.alibaba.cloud.ai.graph.state.AgentState;

@FunctionalInterface
public interface NodeAction<T extends AgentState> {

	Map<String, Object> apply(T t) throws Exception;

}

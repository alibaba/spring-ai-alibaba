package com.alibaba.cloud.ai.graph.action;

import java.util.Map;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.state.AgentState;

@FunctionalInterface
public interface NodeActionWithConfig<S extends AgentState> {

	Map<String, Object> apply(S t, RunnableConfig config) throws Exception;

}

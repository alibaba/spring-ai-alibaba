package com.alibaba.cloud.ai.graph.action;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.state.NodeState;

import java.util.Map;

@FunctionalInterface
public interface NodeActionWithConfig<S extends NodeState> {

	Map<String, Object> apply(S t, RunnableConfig config) throws Exception;

}

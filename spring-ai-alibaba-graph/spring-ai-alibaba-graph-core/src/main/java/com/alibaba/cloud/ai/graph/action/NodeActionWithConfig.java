package com.alibaba.cloud.ai.graph.action;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.state.NodeState;

import java.util.Map;

@FunctionalInterface
public interface NodeActionWithConfig {

	Map<String, Object> apply(NodeState t, RunnableConfig config) throws Exception;

}

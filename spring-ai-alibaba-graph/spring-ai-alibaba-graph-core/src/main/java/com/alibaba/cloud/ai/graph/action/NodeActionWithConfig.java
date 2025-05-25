package com.alibaba.cloud.ai.graph.action;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.OverAllState;

import java.util.Map;

@FunctionalInterface
public interface NodeActionWithConfig {

	Map<String, Object> apply(OverAllState t, RunnableConfig config) throws Exception;

}

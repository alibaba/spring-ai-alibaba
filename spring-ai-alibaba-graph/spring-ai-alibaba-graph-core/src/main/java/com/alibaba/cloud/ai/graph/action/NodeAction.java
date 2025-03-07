package com.alibaba.cloud.ai.graph.action;

import com.alibaba.cloud.ai.graph.OverAllState;

import java.util.Map;

@FunctionalInterface
public interface NodeAction {

	Map<String, Object> apply(OverAllState t) throws Exception;

}

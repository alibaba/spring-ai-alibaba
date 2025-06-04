package com.alibaba.cloud.ai.graph.action;


import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;

@FunctionalInterface
public interface CommandAction {
    Command apply(OverAllState state, RunnableConfig config) throws Exception;
}
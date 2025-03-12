package com.alibaba.cloud.ai.graph.action;

import com.alibaba.cloud.ai.graph.Command;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;

@FunctionalInterface
public interface CommandNodeActionWithConfig {

	Command apply(OverAllState t, RunnableConfig config) throws Exception;

}

package com.alibaba.cloud.ai.graph.action;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.Send;

/**
 * The interface Send edge action.
 */
@FunctionalInterface
public interface SendEdgeAction {
    /**
     * Applies this action to the given agent state.
     *
     * @param t the agent state
     * @return a result of the action
     * @throws Exception if an error occurs during the action
     */
    Send apply(OverAllState t) throws Exception;
}

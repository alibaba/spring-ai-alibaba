package com.alibaba.cloud.ai.graph.action;

import com.alibaba.cloud.ai.graph.OverAllState;

/**
 * Represents an edge action that operates on an agent state and returns a result.
 *
 */
@FunctionalInterface
public interface EdgeAction {

	/**
	 * Applies this action to the given agent state.
	 * @param t the agent state
	 * @return a result of the action
	 * @throws Exception if an error occurs during the action
	 */
	String apply(OverAllState t) throws Exception;

}

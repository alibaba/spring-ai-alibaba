package com.alibaba.cloud.ai.graph;

import lombok.Value;
import lombok.experimental.Accessors;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.state.AgentState;

import java.util.Map;

/**
 * Represents a condition associated with an edge in a graph.
 *
 * @param <S> the type of the state associated with the edge
 */
@Value
@Accessors(fluent = true)
class EdgeCondition<S extends AgentState> {

	/**
	 * The action to be performed asynchronously when the edge condition is met.
	 */
	AsyncEdgeAction<S> action;

	/**
	 * A map of string key-value pairs representing additional mappings for the edge
	 * condition.
	 */
	Map<String, String> mappings;

}

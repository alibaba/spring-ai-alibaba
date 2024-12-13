package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * Represents a condition associated with an edge in a graph.
 *
 */
@Value
@Accessors(fluent = true)
class EdgeCondition {

	/**
	 * The action to be performed asynchronously when the edge condition is met.
	 */
	AsyncEdgeAction action;

	/**
	 * A map of string key-value pairs representing additional mappings for the edge
	 * condition.
	 */
	Map<String, String> mappings;

}

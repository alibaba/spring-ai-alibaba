package com.alibaba.cloud.ai.graph.internal.edge;

import java.util.Map;

import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.state.AgentState;

import static java.lang.String.format;

/**
 * Represents a condition associated with an edge in a graph.
 *
 * @param action The action to be performed asynchronously when the edge condition is met.
 * @param mappings A map of string key-value pairs representing additional mappings for
 * the edge condition.
 */
public record EdgeCondition(AsyncEdgeAction action, Map<String, String> mappings) {

	@Override
	public String toString() {
		return format("EdgeCondition[ %s, mapping=%s", action != null ? "action" : "null", mappings);
	}

}

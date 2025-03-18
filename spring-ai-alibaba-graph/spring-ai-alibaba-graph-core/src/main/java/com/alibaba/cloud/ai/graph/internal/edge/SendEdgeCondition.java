package com.alibaba.cloud.ai.graph.internal.edge;

import com.alibaba.cloud.ai.graph.action.AsyncSendEdgeAction;

import java.util.Map;

/**
 * The type Send edge condition.
 */
public class SendEdgeCondition extends EdgeCondition {

	private AsyncSendEdgeAction asyncSendEdgeAction;

	/**
	 * Represents a condition associated with an edge in a graph.
	 * @param action The action to be performed asynchronously when the edge condition is
	 * met.
	 * @param mappings A map of string key-value pairs representing additional mappings
	 * for the edge condition.
	 */
	public SendEdgeCondition(AsyncSendEdgeAction action, Map<String, String> mappings) {
		super(null, mappings);
		this.asyncSendEdgeAction = action;
	}

	/**
	 * Send edge action async send edge action.
	 * @return the async send edge action
	 */
	public AsyncSendEdgeAction sendEdgeAction() {
		return asyncSendEdgeAction;
	}

}

package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.state.AgentState;

import static java.lang.String.format;

/**
 * Defines the interface for a sub-graph node in an agent's state graph.
 */
public interface SubGraphNode {

	String PREFIX_FORMAT = "%s-%s";

	/**
	 * node identifier
	 * @return the unique identifier for the node.
	 */
	String id();

	/**
	 * Returns a subgraph of the current state graph containing all reachable states from
	 * the current state.
	 * @return {@code StateGraph<State>} representation of the subgraph.
	 */
	StateGraph subGraph();

	default String formatId(String nodeId) {
		return format(PREFIX_FORMAT, id(), nodeId);
	}

	/**
	 * Formats the given {@code subGraphNodeId} and {@code nodeId} into a single string
	 * using a predefined prefix.
	 * @param subGraphNodeId The ID of the sub-graph node.
	 * @param nodeId The ID of the node.
	 * @return A formatted string combining the prefix with the provided IDs.
	 */
	static String formatId(String subGraphNodeId, String nodeId) {
		return format(PREFIX_FORMAT, subGraphNodeId, nodeId);
	}

}
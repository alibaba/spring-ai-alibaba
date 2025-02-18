package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.state.NodeState;

import static java.lang.String.format;

/**
 * Represents the output of a node in a graph.
 *
 * @param <> the type of the state associated with the node output
 */
public class NodeOutput {

	public static NodeOutput of(String node, OverAllState state) {
		return new NodeOutput(node, state);
	}

	/**
	 * The identifier of the node.
	 */
	private final String node;

	/**
	 * The state associated with the node.
	 */
	private final OverAllState state;

	public String node() {
		return node;
	}

	public OverAllState state() {
		return state;
	}

	/**
	 * @deprecated Use {@link #state()} instead.
	 */
	@Deprecated
	public OverAllState getState() {
		return state();
	}

	protected NodeOutput(String node, OverAllState state) {
		this.node = node;
		this.state = state;
	}

	@Override
	public String toString() {
		return format("NodeOutput{node=%s, state=%s}", node(), state());
	}

}

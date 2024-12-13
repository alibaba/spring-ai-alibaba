package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.state.NodeState;

import static java.lang.String.format;

/**
 * Represents the output of a node in a graph.
 *
 * @param <State> the type of the state associated with the node output
 */
public class NodeOutput<State extends NodeState> {

	public static <State extends NodeState> NodeOutput<State> of(String node, State state) {
		return new NodeOutput<>(node, state);
	}

	/**
	 * The identifier of the node.
	 */
	private final String node;

	/**
	 * The state associated with the node.
	 */
	private final State state;

	public String node() {
		return node;
	}

	public State state() {
		return state;
	}

	/**
	 * @deprecated Use {@link #state()} instead.
	 */
	@Deprecated
	public State getState() {
		return state();
	}

	protected NodeOutput(String node, State state) {
		this.node = node;
		this.state = state;
	}

	@Override
	public String toString() {
		return format("NodeOutput{node=%s, state=%s}", node(), state());
	}

}

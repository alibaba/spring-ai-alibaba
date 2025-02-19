package com.alibaba.cloud.ai.graph;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import com.alibaba.cloud.ai.graph.state.AgentState;

import static java.lang.String.format;

/**
 * Represents the output of a node in a graph.
 *
 * @param <State> the type of the state associated with the node output
 */
public class NodeOutput<State extends AgentState> {

	public static <State extends AgentState> NodeOutput<State> of(String node, State state) {
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

	/**
	 * If the output is from a subgraph.
	 */
	@Setter(AccessLevel.PROTECTED)
	@Getter
	private boolean subGraph = false;

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

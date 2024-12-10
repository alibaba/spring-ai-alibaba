package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.state.AgentState;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * Represents a node in a graph with a unique identifier and an associated action.
 *
 * @param <State> the type of the state associated with the node
 */
@Value
@Accessors(fluent = true)
class Node<State extends AgentState> {

	/**
	 * The unique identifier for the node.
	 */
	String id;

	/**
	 * The action to be performed asynchronously by the node.
	 */
	AsyncNodeActionWithConfig<State> action;

	public Node( String id ) {
		this.id = id;
		this.action = null;

	}

	public Node( String id, AsyncNodeAction<State> action ) {
		this.id = id;
		this.action = AsyncNodeActionWithConfig.of(action);
	}

	public Node( String id, AsyncNodeActionWithConfig<State> action ) {
		this.id = id;
		this.action = action;
	}

	/**
	 * Checks if this node is equal to another object.
	 * @param o the object to compare with
	 * @return true if this node is equal to the specified object, false otherwise
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Node<?> node = (Node<?>) o;
		return Objects.equals(id, node.id);
	}

	/**
	 * Returns the hash code value for this node.
	 * @return the hash code value for this node
	 */
	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

}

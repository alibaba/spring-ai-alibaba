package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.state.NodeState;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * Represents an edge in a graph with a source ID and a target value.
 *
 * @param <State> the type of the state associated with the edge
 */
@Value
@Accessors(fluent = true)
class Edge<State extends NodeState> {

	/**
	 * The ID of the source node.
	 */
	String sourceId;

	/**
	 * The target value associated with the edge.
	 */
	EdgeValue<State> target;

	/**
	 * Checks if this edge is equal to another object.
	 * @param o the object to compare with
	 * @return true if this edge is equal to the specified object, false otherwise
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Edge<?> node = (Edge<?>) o;
		return Objects.equals(sourceId, node.sourceId);
	}

	/**
	 * Returns the hash code value for this edge.
	 * @return the hash code value for this edge
	 */
	@Override
	public int hashCode() {
		return Objects.hash(sourceId);
	}

}

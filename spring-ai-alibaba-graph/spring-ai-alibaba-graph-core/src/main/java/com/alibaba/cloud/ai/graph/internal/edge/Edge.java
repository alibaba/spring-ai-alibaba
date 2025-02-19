package com.alibaba.cloud.ai.graph.internal.edge;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.NonNull;
import com.alibaba.cloud.ai.graph.GraphStateException;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.internal.node.Node;
import com.alibaba.cloud.ai.graph.state.AgentState;

import static java.lang.String.format;
import static com.alibaba.cloud.ai.graph.StateGraph.START;

/**
 * Represents an edge in a graph with a source ID and a target value.
 *
 * @param <State> the type of the state associated with the edge
 * @param sourceId The ID of the source node.
 * @param targets The targets value associated with the edge.
 */
public record Edge<State extends AgentState>(String sourceId, List<EdgeValue<State>> targets) {

	public Edge(String sourceId, EdgeValue<State> target) {
		this(sourceId, List.of(target));
	}

	public Edge(String id) {
		this(id, List.of());
	}

	public boolean isParallel() {
		return targets.size() > 1;
	}

	public EdgeValue<State> target() {
		if (isParallel()) {
			throw new IllegalStateException(format("Edge '%s' is parallel", sourceId));
		}
		return targets.get(0);
	}

	public boolean anyMatchByTargetId(String targetId) {
		return targets().stream()
			.anyMatch(v -> (v.id() != null) ? Objects.equals(v.id(), targetId)
					: v.value().mappings().containsValue(targetId)

			);
	}

	public Edge<State> withSourceAndTargetIdsUpdated(Node<State> node, Function<String, String> newSourceId,
			Function<String, EdgeValue<State>> newTarget) {

		var newTargets = targets().stream().map(t -> t.withTargetIdsUpdated(newTarget)).toList();
		return new Edge<>(newSourceId.apply(sourceId), newTargets);

	}

	public void validate(@NonNull StateGraph.Nodes<State> nodes) throws GraphStateException {
		if (!Objects.equals(sourceId(), START) && !nodes.anyMatchById(sourceId())) {
			throw StateGraph.Errors.missingNodeReferencedByEdge.exception(sourceId());
		}

		if (isParallel()) { // check for duplicates targets
			Set<String> duplicates = targets.stream()
				.collect(Collectors.groupingBy(EdgeValue::id, Collectors.counting())) // Group
																						// by
																						// element
																						// and
																						// count
																						// occurrences
				.entrySet()
				.stream()
				.filter(entry -> entry.getValue() > 1) // Filter elements with more than
														// one occurrence
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
			if (!duplicates.isEmpty()) {
				throw StateGraph.Errors.duplicateEdgeTargetError.exception(sourceId(), duplicates);
			}
		}

		for (EdgeValue<State> target : targets) {
			validate(target, nodes);
		}

	}

	private void validate(EdgeValue<State> target, StateGraph.Nodes<State> nodes) throws GraphStateException {
		if (target.id() != null) {
			if (!Objects.equals(target.id(), StateGraph.END) && !nodes.anyMatchById(target.id())) {
				throw StateGraph.Errors.missingNodeReferencedByEdge.exception(target.id());
			}
		}
		else if (target.value() != null) {
			for (String nodeId : target.value().mappings().values()) {
				if (!Objects.equals(nodeId, StateGraph.END) && !nodes.anyMatchById(nodeId)) {
					throw StateGraph.Errors.missingNodeInEdgeMapping.exception(sourceId(), nodeId);
				}
			}
		}
		else {
			throw StateGraph.Errors.invalidEdgeTarget.exception(sourceId());
		}

	}

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

package com.alibaba.cloud.ai.graph.internal.edge;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.alibaba.cloud.ai.graph.state.AgentState;

/**
 * @param <State>
 * @param id The unique identifier for the edge value.
 * @param value The condition associated with the edge value.
 */
public record EdgeValue<State extends AgentState>(String id, EdgeCondition<State> value) {

	public EdgeValue(String id) {
		this(id, null);
	}

	public EdgeValue(EdgeCondition<State> value) {
		this(null, value);
	}

	EdgeValue<State> withTargetIdsUpdated(Function<String, EdgeValue<State>> target) {
		if (id != null) {
			return target.apply(id);
		}

		var newMappings = value.mappings().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> {
			var v = target.apply(e.getValue());
			return (v.id() != null) ? v.id() : e.getValue();
		}));

		return new EdgeValue<>(null, new EdgeCondition<>(value.action(), newMappings));

	}

}

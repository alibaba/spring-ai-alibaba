package com.alibaba.cloud.ai.graph.state;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.ofNullable;

/**
 * Represents the state of an agent with a map of data.
 */
public class NodeState {

	public static final String INPUT = "input";

	public static final String OUTPUT = "output";

	public static final String SUB_GRAPH = "_subgraph";

	private final java.util.Map<String, Object> data;

	/**
	 * Constructs an AgentState with the given initial data.
	 * @param initData the initial data for the agent state
	 */
	public NodeState(Map<String, Object> initData) {
		this.data = new HashMap<>(initData);
	}

	/**
	 * Returns an unmodifiable view of the data map.
	 * @return an unmodifiable map of the data
	 */
	public final java.util.Map<String, Object> data() {
		return unmodifiableMap(data);
	}

	public Optional<String> input() {
		return value(INPUT);
	}

	public Optional<String> outcome() {
		return value(OUTPUT);
	}

	/**
	 * Retrieves the value associated with the given key, if present.
	 * @param key the key whose associated value is to be returned
	 * @param <T> the type of the value
	 * @return an Optional containing the value if present, otherwise an empty Optional
	 */
	public final <T> Optional<T> value(String key) {
		return ofNullable((T) data().get(key));
	}

	public final <T> T value(String key, T defaultValue) {
		return (T) value(key).orElse(defaultValue);
	}

	public final <T> T value(String key, Supplier<T> defaultProvider) {
		return (T) value(key).orElseGet(defaultProvider);
	}

	/**
	 * Merges the current state with a partial state and returns a new state.
	 * @param partialState the partial state to merge with
	 * @return a new state resulting from the merge
	 * @deprecated use {@link #updateState(NodeState, Map)}
	 */
	@Deprecated
	public final Map<String, Object> mergeWith(Map<String, Object> partialState) {
		return updateState(data(), partialState);
	}

	/**
	 * Returns a string representation of the agent state.
	 * @return a string representation of the data map
	 */
	@Override
	public String toString() {
		return data.toString();
	}

	/**
	 * Merges the current value with the new value using the appropriate merge function.
	 * @param currentValue the current value
	 * @param newValue the new value
	 * @return the merged value
	 */
	private static Object mergeFunction(Object currentValue, Object newValue) {
		return newValue;
	}

	/**
	 * Updates a state with the provided partial state. The merge function is used to
	 * merge the current state value with the new value.
	 * @param state the current state
	 * @param partialState the partial state to update from
	 * @return the updated state
	 * @throws NullPointerException if state is null
	 */
	public static Map<String, Object> updateState(Map<String, Object> state, Map<String, Object> partialState) {
		Objects.requireNonNull(state, "state cannot be null");
		if (partialState == null || partialState.isEmpty()) {
			return state;
		}

		return Stream.concat(state.entrySet().stream(), partialState.entrySet().stream())
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, NodeState::mergeFunction));
	}

	/**
	 * Updates a state with the provided partial state. The merge function is used to
	 * merge the current state value with the new value.
	 * @param state the current state
	 * @param partialState the partial state to update from
	 * @return the updated state
	 * @throws NullPointerException if state is null
	 */
	public static Map<String, Object> updateState(NodeState state, Map<String, Object> partialState) {
		return updateState(state.data(), partialState);
	}

}

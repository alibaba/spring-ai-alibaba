package com.alibaba.cloud.ai.graph.state;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.ofNullable;

/**
 * Represents the state of an agent with a map of data.
 */
public class AgentState {

	private final java.util.Map<String, Object> data;

	/**
	 * Constructs an AgentState with the given initial data.
	 * @param initData the initial data for the agent state
	 */
	public AgentState(Map<String, Object> initData) {
		this.data = new HashMap<>(initData);
	}

	/**
	 * Returns an unmodifiable view of the data map.
	 * @return an unmodifiable map of the data
	 */
	public final java.util.Map<String, Object> data() {
		return unmodifiableMap(data);
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
	 * Retrieves or creates an AppendableValue associated with the given key.
	 * @param key the key whose associated AppendableValue is to be returned or created
	 * @param <T> the type of the value
	 * @return an AppendableValue associated with the given key
	 * @deprecated use {@link Channel} instead
	 */
	@Deprecated
	public final <T> AppendableValue<T> appendableValue(String key) {
		Object value = this.data.get(key);

		if (value instanceof AppendableValue) {
			return (AppendableValue<T>) value;
		}
		if (value instanceof Collection) {
			return new AppendableValueRW<>((Collection<T>) value);
		}
		AppendableValueRW<T> rw = new AppendableValueRW<>();
		if (value != null) {
			rw.append(value);
		}
		this.data.put(key, rw);
		return rw;
	}

	/**
	 * Merges the current state with a partial state and returns a new state.
	 * @param partialState the partial state to merge with
	 * @return a new state resulting from the merge
	 * @deprecated use {@link #updateState(AgentState, Map, Map)}
	 */
	@Deprecated
	public final Map<String, Object> mergeWith(Map<String, Object> partialState, Map<String, Channel<?>> channels) {
		return updateState(data(), partialState, channels);
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
		if (currentValue instanceof AppendableValueRW<?>) {
			((AppendableValueRW<?>) currentValue).append(newValue);
			return currentValue;
		}
		return newValue;
	}

	private static Map<String, Object> updatePartialStateFromSchema(Map<String, Object> state,
			Map<String, Object> partialState, Map<String, Channel<?>> channels) {
		if (channels == null || channels.isEmpty()) {
			return partialState;
		}
		return partialState.entrySet().stream().map(entry -> {

			Channel<?> channel = channels.get(entry.getKey());
			if (channel != null) {
				Object newValue = channel.update(entry.getKey(), state.get(entry.getKey()), entry.getValue());
				return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), newValue);
			}

			return entry;
		}).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	/**
	 * Updates a state with the provided partial state. The merge function is used to
	 * merge the current state value with the new value.
	 * @param state the current state
	 * @param partialState the partial state to update from
	 * @param channels the channels used to update the partial state if necessary
	 * @return the updated state
	 * @throws NullPointerException if state is null
	 */
	public static Map<String, Object> updateState(Map<String, Object> state, Map<String, Object> partialState,
			Map<String, Channel<?>> channels) {
		Objects.requireNonNull(state, "state cannot be null");
		if (partialState == null || partialState.isEmpty()) {
			return state;
		}

		Map<String, Object> updatedPartialState = updatePartialStateFromSchema(state, partialState, channels);

		return Stream.concat(state.entrySet().stream(), updatedPartialState.entrySet().stream())
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, AgentState::mergeFunction));
	}

	/**
	 * Updates a state with the provided partial state. The merge function is used to
	 * merge the current state value with the new value.
	 * @param state the current state
	 * @param partialState the partial state to update from
	 * @param channels the channels used to update the partial state if necessary
	 * @return the updated state
	 * @throws NullPointerException if state is null
	 */
	public static Map<String, Object> updateState(AgentState state, Map<String, Object> partialState,
			Map<String, Channel<?>> channels) {
		return updateState(state.data(), partialState, channels);
	}

}

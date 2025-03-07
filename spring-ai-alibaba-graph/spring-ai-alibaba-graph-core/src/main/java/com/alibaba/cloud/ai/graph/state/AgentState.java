/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.graph.state;

import java.util.AbstractMap;
import java.util.Collection;
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
public class AgentState {

	private final Map<String, Object> data;

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
	public final Map<String, Object> data() {
		return unmodifiableMap(data);
	}

	/**
	 * Retrieves the value associated with the given key, if present.
	 * @param key the key whose associated value is to be returned
	 * @param <T> the type of the value
	 * @return an Optional containing the value if present, otherwise an empty Optional
	 */
	@SuppressWarnings("unchecked")
	public final <T> Optional<T> value(String key) {
		return ofNullable((T) data().get(key));
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
	 * Updates the partial state from a schema using channels.
	 * @param state The current state as a map of key-value pairs.
	 * @param partialState The partial state to be updated.
	 * @param channels A map of channel names to their implementations.
	 * @return An updated version of the partial state after applying the schema and
	 * channels.
	 */
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

	/**
	 * Merges the current value with the new value using the appropriate merge function.
	 * @param currentValue the current value
	 * @param newValue the new value
	 * @return the merged value
	 */
	@Deprecated
	private static Object mergeFunction(Object currentValue, Object newValue) {
		if (currentValue instanceof AppendableValueRW<?>) {
			((AppendableValueRW<?>) currentValue).append(newValue);
			return currentValue;
		}
		return newValue;
	}

	/**
	 * Returns the value associated with the specified key or a default value if the key
	 * is not present.
	 * @param key The key whose associated value is to be returned.
	 * @param defaultValue The value to use if no entry for the specified key is found.
	 * @param <T> the type of the value
	 * @return The value to which the specified key is mapped, or {@code defaultValue} if
	 * this map contains no mapping for the key.
	 * @deprecated This method is deprecated and may be removed in future versions.
	 */
	@Deprecated
	public final <T> T value(String key, T defaultValue) {
		return this.<T>value(key).orElse(defaultValue);
	}

	/**
	 * Returns the value associated with the given key or a default value if no such key
	 * exists.
	 * @param key The key to retrieve the value for.
	 * @param defaultProvider A provider function that returns the default value if the
	 * key is not found.
	 * @param <T> the type of the value
	 * @return The value associated with the key, or the default value provided by
	 * {@code defaultProvider}.
	 */
	@Deprecated
	public final <T> T value(String key, Supplier<T> defaultProvider) {
		return this.<T>value(key).orElseGet(defaultProvider);
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
	 * @param channels the channels used to update the partial state if necessary
	 * @return a new state resulting from the merge
	 * @deprecated use {@link #updateState(AgentState, Map, Map)}
	 */
	@Deprecated
	public final Map<String, Object> mergeWith(Map<String, Object> partialState, Map<String, Channel<?>> channels) {
		return updateState(data(), partialState, channels);
	}

}
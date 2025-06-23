/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph;

import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import static com.alibaba.cloud.ai.graph.utils.CollectionsUtils.entryOf;
import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.ofNullable;

/**
 * Represents the overall state of a graph or workflow execution.
 *
 * <p>
 * {@link OverAllState} acts as a central container for holding and managing shared data
 * across nodes in a graph-based processing pipeline. It supports key-value data storage
 * with customizable update strategies ({@link KeyStrategy}) per key, allowing flexible
 * merging or replacement logic. This class is serializable, making it suitable for
 * persistence, checkpointing, or cross-node communication.
 * </p>
 *
 * <h2>Main Features</h2>
 * <ul>
 * <li><strong>Data Management:</strong> Stores arbitrary typed data using a map
 * structure.</li>
 * <li><strong>Key Strategy Support:</strong> Associates each key with a strategy to
 * control how new values are merged with existing ones.</li>
 * <li><strong>Resume Mode:</strong> Supports a resume flag indicating whether the state
 * is used for resuming execution.</li>
 * <li><strong>Immutable Views:</strong> Provides unmodifiable views of data and
 * strategies via {@link #data()} and {@link #keyStrategies()}.</li>
 * <li><strong>Snapshots:</strong> Allows creating snapshots of the current state via
 * {@link #snapShot()}.</li>
 * <li><strong>Human Feedback:</strong> Supports integration of human feedback and
 * interruption messages during execution.</li>
 * </ul>
 *
 * <h2>Construction Options</h2>
 * <ul>
 * <li>Default constructor: registers the default input key and replace strategy
 * automatically.</li>
 * <li>Constructor with data: initializes from an existing map of state data.</li>
 * <li>Constructor with resume flag: creates a state marked for resuming execution.</li>
 * <li>Full constructor: allows full customization of data, strategies, and resume
 * status.</li>
 * </ul>
 *
 * <h2>Thread Safety</h2> This class is not thread-safe. External synchronization is
 * required if accessed concurrently.
 *
 * @author disaster
 * @since 1.0.0.1
 */
public final class OverAllState implements Serializable {

	/**
	 * Internal map storing the actual state data. All get/set operations on state values
	 * go through this map.
	 */
	private final Map<String, Object> data;

	/**
	 * Mapping of keys to their respective update strategies. Determines how values for
	 * each key should be merged or updated.
	 */
	private final Map<String, KeyStrategy> keyStrategies;

	/**
	 * Indicates whether this state is being used to resume a previously interrupted
	 * execution. If true, certain initialization steps may be skipped.
	 */
	private Boolean resume;

	/**
	 * Holds optional human feedback information provided during execution. May be null if
	 * no feedback was given.
	 */
	private HumanFeedback humanFeedback;

	/**
	 * Optional message indicating that the execution was interrupted. If non-null,
	 * indicates that the graph should halt or handle the interruption.
	 */
	private String interruptMessage;

	/**
	 * The default key used for standard input injection into the state. Typically used
	 * when initializing the state with user or external input.
	 */
	public static final String DEFAULT_INPUT_KEY = "input";

	/**
	 * Reset.
	 */
	public void reset() {
		this.data.clear();
	}

	/**
	 * Snap shot optional.
	 * @return the optional
	 */
	public Optional<OverAllState> snapShot() {
		return Optional.of(new OverAllState(new HashMap<>(this.data), new HashMap<>(this.keyStrategies), this.resume));
	}

	/**
	 * Instantiates a new Over all state.
	 * @param resume the is resume
	 */
	public OverAllState(boolean resume) {
		this.data = new HashMap<>();
		this.keyStrategies = new HashMap<>();
		this.resume = resume;
	}

	/**
	 * Instantiates a new Over all state.
	 * @param data the data
	 */
	public OverAllState(Map<String, Object> data) {
		this.data = new HashMap<>(data);
		this.keyStrategies = new HashMap<>();
		this.resume = false;
	}

	/**
	 * Instantiates a new Over all state.
	 */
	public OverAllState() {
		this.data = new HashMap<>();
		this.keyStrategies = new HashMap<>();
		this.registerKeyAndStrategy(OverAllState.DEFAULT_INPUT_KEY, new ReplaceStrategy());
		this.resume = false;
	}

	/**
	 * Instantiates a new Over all state.
	 * @param data the data
	 * @param keyStrategies the key strategies
	 * @param resume the resume
	 */
	protected OverAllState(Map<String, Object> data, Map<String, KeyStrategy> keyStrategies, Boolean resume) {
		this.data = data;
		this.keyStrategies = keyStrategies;
		this.registerKeyAndStrategy(OverAllState.DEFAULT_INPUT_KEY, new ReplaceStrategy());
		this.resume = resume;
	}

	/**
	 * Interrupt message string.
	 * @return the string
	 */
	public String interruptMessage() {
		return interruptMessage;
	}

	/**
	 * Sets interrupt message.
	 * @param interruptMessage the interrupt message
	 */
	public void setInterruptMessage(String interruptMessage) {
		this.interruptMessage = interruptMessage;
	}

	/**
	 * With human feedback.
	 * @param humanFeedback the human feedback
	 */
	public void withHumanFeedback(HumanFeedback humanFeedback) {
		this.humanFeedback = humanFeedback;
	}

	/**
	 * Human feedback human feedback.
	 * @return the human feedback
	 */
	public HumanFeedback humanFeedback() {
		return this.humanFeedback;
	}

	/**
	 * Copy with resume over all state.
	 * @return the over all state
	 */
	public OverAllState copyWithResume() {
		return new OverAllState(this.data, this.keyStrategies, true);
	}

	/**
	 * With resume.
	 */
	public void withResume() {
		this.resume = true;
	}

	/**
	 * Without resume.
	 */
	public void withoutResume() {
		this.resume = false;
	}

	/**
	 * Is resume boolean.
	 * @return the boolean
	 */
	public boolean isResume() {
		return this.resume;
	}

	/**
	 * Clears all data in the current state, leaving key strategies, resume flag, and
	 * human feedback intact.
	 */
	public void clear() {
		this.data.clear();
	}

	/**
	 * Replaces the current state's contents with the provided state.
	 * <p>
	 * This method effectively copies all data, key strategies, resume flag, and human
	 * feedback from the provided state to this state.
	 * @param overAllState the state to copy from
	 */
	public void cover(OverAllState overAllState) {
		this.keyStrategies.clear();
		this.keyStrategies.putAll(overAllState.keyStrategies());
		this.data.clear();
		this.data.putAll(overAllState.data());
		this.resume = overAllState.resume;
		this.humanFeedback = overAllState.humanFeedback;
	}

	/**
	 * Inputs over all state.
	 * @param input the input
	 * @return the over all state
	 */
	public OverAllState input(Map<String, Object> input) {
		if (input == null) {
			withResume();
			return this;
		}

		if (CollectionUtils.isEmpty(input)) {
			return this;
		}

		Map<String, KeyStrategy> keyStrategies = keyStrategies();
		input.keySet().stream().filter(key -> keyStrategies.containsKey(key)).forEach(key -> {
			this.data.put(key, keyStrategies.get(key).apply(value(key, null), input.get(key)));
		});
		return this;
	}

	/**
	 * Add key and strategy over all state.
	 * @param key the key
	 * @param strategy the strategy
	 * @return the over all state
	 */
	public OverAllState registerKeyAndStrategy(String key, KeyStrategy strategy) {
		this.keyStrategies.put(key, strategy);
		return this;
	}

	/**
	 * Register key and strategy over all state.
	 * @param keyStrategies the key strategies
	 * @return the over all state
	 */
	public OverAllState registerKeyAndStrategy(Map<String, KeyStrategy> keyStrategies) {
		this.keyStrategies.putAll(keyStrategies);
		return this;
	}

	/**
	 * Is contain strategy boolean.
	 * @param key the key
	 * @return the boolean
	 */
	public boolean containStrategy(String key) {
		return this.keyStrategies.containsKey(key);
	}

	/**
	 * Update state map.
	 * @param partialState the partial state
	 * @return the map
	 */
	public Map<String, Object> updateState(Map<String, Object> partialState) {
		Map<String, KeyStrategy> keyStrategies = keyStrategies();
		partialState.keySet().stream().filter(key -> keyStrategies.containsKey(key)).forEach(key -> {
			this.data.put(key, keyStrategies.get(key).apply(value(key, null), partialState.get(key)));
		});
		return data();
	}

	/**
	 * Updates the internal state based on a schema-defined strategy.
	 * <p>
	 * This method first validates the input state, then updates the partial state
	 * according to the provided key strategies. The updated state is formed by merging
	 * the original state and the modified partial state, removing any null values in the
	 * process. The resulting entries are then used to update the internal data map.
	 * @param state the base state to update; must not be null
	 * @param partialState the partial state containing updates; may be null or empty
	 * @param keyStrategies the mapping of keys to update strategies; used to transform
	 * values
	 */
	public void updateStateBySchema(Map<String, Object> state, Map<String, Object> partialState,
			Map<String, KeyStrategy> keyStrategies) {
		updateState(updateState(state, partialState, keyStrategies));
	}

	/**
	 * Key verify boolean.
	 * @return the boolean
	 */
	protected boolean keyVerify() {
		return hasCommonKey(this.data, getKeyStrategies());
	}

	private Map<?, ?> getKeyStrategies() {
		return this.keyStrategies;
	}

	private boolean hasCommonKey(Map<?, ?> map1, Map<?, ?> map2) {
		Set<?> keys1 = map1.keySet();
		for (Object key : map2.keySet()) {
			if (keys1.contains(key)) {
				return true;
			}
		}
		return false;
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
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, OverAllState::mergeFunction));
	}

	/**
	 * Update state map.
	 * @param state the state
	 * @param partialState the partial state
	 * @param keyStrategies the key strategies
	 * @return the map
	 */
	public static Map<String, Object> updateState(Map<String, Object> state, Map<String, Object> partialState,
			Map<String, KeyStrategy> keyStrategies) {
		Objects.requireNonNull(state, "state cannot be null");
		if (partialState == null || partialState.isEmpty()) {
			return state;
		}

		Map<String, Object> updatedPartialState = updatePartialStateFromSchema(state, partialState, keyStrategies);

		return Stream.concat(state.entrySet().stream(), updatedPartialState.entrySet().stream())
			.collect(toMapRemovingNulls(Map.Entry::getKey, Map.Entry::getValue, (currentValue, newValue) -> newValue));
	}

	/**
	 * Updates the partial state from a schema using channels.
	 * @param state The current state as a map of key-value pairs.
	 * @param partialState The partial state to be updated.
	 * @param keyStrategies A map of channel names to their implementations.
	 * @return An updated version of the partial state after applying the schema and
	 * channels.
	 */
	private static Map<String, Object> updatePartialStateFromSchema(Map<String, Object> state,
			Map<String, Object> partialState, Map<String, KeyStrategy> keyStrategies) {
		if (keyStrategies == null || keyStrategies.isEmpty()) {
			return partialState;
		}
		return partialState.entrySet().stream().map(entry -> {

			KeyStrategy channel = keyStrategies.get(entry.getKey());
			if (channel != null) {
				Object newValue = channel.apply(state.get(entry.getKey()), entry.getValue());
				return entryOf(entry.getKey(), newValue);
			}

			return entry;
		}).collect(toMapAllowingNulls(Map.Entry::getKey, Map.Entry::getValue));
	}

	private static <T, K, U> Collector<T, ?, Map<K, U>> toMapRemovingNulls(Function<? super T, ? extends K> keyMapper,
			Function<? super T, ? extends U> valueMapper, BinaryOperator<U> mergeFunction) {
		return Collector.of(HashMap::new, (map, element) -> {
			K key = keyMapper.apply(element);
			U value = valueMapper.apply(element);
			if (value == null) {
				map.remove(key);
			}
			else {
				map.merge(key, value, mergeFunction);
			}
		}, (map1, map2) -> {
			map2.forEach((key, value) -> {
				if (value != null) {
					map1.merge(key, value, mergeFunction);
				}
			});
			return map1;
		}, Collector.Characteristics.UNORDERED);
	}

	private static <T, K, U> Collector<T, ?, Map<K, U>> toMapAllowingNulls(Function<? super T, ? extends K> keyMapper,
			Function<? super T, ? extends U> valueMapper) {
		return Collector.of(HashMap::new,
				(map, element) -> map.put(keyMapper.apply(element), valueMapper.apply(element)), (map1, map2) -> {
					map1.putAll(map2);
					return map1;
				}, Collector.Characteristics.UNORDERED);
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
	 * Key strategies map.
	 * @return the map
	 */
	public Map<String, KeyStrategy> keyStrategies() {
		return keyStrategies;
	}

	/**
	 * Data map.
	 * @return the map
	 */
	public final Map<String, Object> data() {
		return unmodifiableMap(data);
	}

	/**
	 * Value optional.
	 * @param <T> the type parameter
	 * @param key the key
	 * @return the optional
	 */
	public final <T> Optional<T> value(String key) {
		return ofNullable((T) data().get(key));
	}

	/**
	 * Value optional.
	 * @param <T> the type parameter
	 * @param key the key
	 * @param type the type
	 * @return the optional
	 */
	public final <T> Optional<T> value(String key, Class<T> type) {
		if (type != null) {
			return ofNullable(type.cast(data().get(key)));
		}
		return value(key);
	}

	/**
	 * Value t.
	 * @param <T> the type parameter
	 * @param key the key
	 * @param defaultValue the default value
	 * @return the t
	 */
	public final <T> T value(String key, T defaultValue) {
		return (T) value(key).orElse(defaultValue);
	}

	/**
	 * The type Human feedback.
	 */
	public static class HumanFeedback implements Serializable {

		private Map<String, Object> data;

		private String nextNodeId;

		private String currentNodeId;

		/**
		 * Instantiates a new Human feedback.
		 * @param data the data
		 * @param nextNodeId the next node id
		 */
		public HumanFeedback(Map<String, Object> data, String nextNodeId) {
			this.data = data;
			this.nextNodeId = nextNodeId;
		}

		/**
		 * Data map.
		 * @return the map
		 */
		public Map<String, Object> data() {
			return data;
		}

		/**
		 * Next node id string.
		 * @return the string
		 */
		public String nextNodeId() {
			return nextNodeId;
		}

		/**
		 * Sets data.
		 * @param data the data
		 */
		public void setData(Map<String, Object> data) {
			this.data = data;
		}

		/**
		 * Sets next node id.
		 * @param nextNodeId the next node id
		 */
		public void setNextNodeId(String nextNodeId) {
			this.nextNodeId = nextNodeId;
		}

	}

	@Override
	public String toString() {
		return "OverAllState{" + "data=" + data + ", resume=" + resume + ", humanFeedback=" + humanFeedback
				+ ", interruptMessage='" + interruptMessage + '\'' + '}';
	}

}

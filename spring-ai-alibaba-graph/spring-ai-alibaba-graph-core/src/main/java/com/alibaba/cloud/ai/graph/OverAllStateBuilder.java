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

import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * A builder class for constructing {@link OverAllState} instances with a fluent API.
 *
 * <p>
 * {@link OverAllStateBuilder} provides a convenient way to set up the initial state of an
 * {@link OverAllState} object, including data, key strategies, and resume flag, using a
 * step-by-step, chainable method interface.
 * </p>
 *
 * <h2>Key Features</h2>
 * <ul>
 * <li><strong>Fluent Interface:</strong> Supports method chaining for clean and readable
 * state construction.</li>
 * <li><strong>Data Configuration:</strong> Allows setting both single and multiple
 * key-value pairs into the state data.</li>
 * <li><strong>Strategy Registration:</strong> Enables registration of update strategies
 * per key (see {@link KeyStrategy}).</li>
 * <li><strong>Resume Mode:</strong> Provides configuration for marking the state as
 * resumable.</li>
 * <li><strong>Default Handling:</strong> Automatically ensures the default input key
 * ("input") is registered with a replace strategy if not explicitly set.</li>
 * </ul>
 *
 * <h2>Usage Example</h2> <pre>{@code
 * OverAllState state = OverAllStateBuilder.builder()
 *     .putData("input", "Hello World")
 *     .withKeyStrategy("input", new ReplaceStrategy())
 *     .setResume(true)
 *     .build();
 * }</pre>
 *
 * <p>
 * This class follows the Builder design pattern, separating the construction of a complex
 * object from its representation, allowing the same construction process to create
 * different representations.
 * </p>
 *
 * @author disaster
 * @since 1.0.0.1
 */
public class OverAllStateBuilder {

	private Map<String, Object> data = new HashMap<>();

	private Map<String, KeyStrategy> keyStrategies = new HashMap<>();

	private Boolean resume = false;

	/**
	 * Initializes a builder with default configuration.
	 * @return A new builder instance
	 */
	public static OverAllStateBuilder builder() {
		return new OverAllStateBuilder();
	}

	private OverAllStateBuilder() {
	}

	/**
	 * Adds a single state data entry.
	 * @param key The key
	 * @param value The value
	 * @return this for chained method calls
	 */
	public OverAllStateBuilder putData(String key, Object value) {
		data.put(key, value);
		return this;
	}

	/**
	 * Adds multiple state entries at once.
	 * @param dataMap A map containing the state data to add
	 * @return this for chained method calls
	 */
	public OverAllStateBuilder withData(Map<String, Object> dataMap) {
		if (dataMap == null) {
			setResume(true);
			return this;
		}

		if (CollectionUtils.isEmpty(dataMap)) {
			return this;
		}

		data.putAll(dataMap);
		return this;
	}

	/**
	 * Registers a key and its associated strategy.
	 * @param key The key to register
	 * @param strategy The strategy to associate with the key
	 * @return this for chained method calls
	 */
	public OverAllStateBuilder withKeyStrategy(String key, KeyStrategy strategy) {
		keyStrategies.put(key, strategy);
		return this;
	}

	/**
	 * Registers multiple keys and their strategies in one operation.
	 * @param strategiesMap A map of keys to strategies
	 * @return this for chained method calls
	 */
	public OverAllStateBuilder withKeyStrategies(Map<String, KeyStrategy> strategiesMap) {
		if (strategiesMap != null) {
			keyStrategies.putAll(strategiesMap);
		}
		return this;
	}

	/**
	 * Configures whether the state should be marked as resumable.
	 * @param resume true to enable resume mode, false otherwise
	 * @return this for chained method calls
	 */
	public OverAllStateBuilder setResume(boolean resume) {
		this.resume = resume;
		return this;
	}

	/**
	 * Constructs and returns a fully configured OverAllState instance.
	 * @return A new OverAllState instance with the configured settings
	 */
	public OverAllState build() {
		OverAllState state = new OverAllState(new HashMap<>(data), new HashMap<>(keyStrategies), resume);
		// If no input key is registered, apply the default key and replace strategy
		if (!state.containStrategy(OverAllState.DEFAULT_INPUT_KEY)) {
			state.registerKeyAndStrategy(OverAllState.DEFAULT_INPUT_KEY, new ReplaceStrategy());
		}
		return state;
	}

}

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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Builder for creating and managing key strategies for graph node outputs. Provides a
 * fluent API for configuring strategies with sensible defaults.
 * <p>
 * This builder allows you to define how keys should be handled when storing or retrieving
 * values in the graph's state. You can set specific strategies for individual keys, use
 * pattern matching, or apply default behaviors.
 * </p>
 *
 * @author disaster
 * @since 1.0.0.1
 */
public class KeyStrategyFactoryBuilder {

	private final Map<String, KeyStrategy> strategies = new HashMap<>();

	private KeyStrategy defaultStrategy = KeyStrategy.REPLACE;

	/**
	 * Add a strategy for a specific key
	 * @param key The key to associate with the strategy
	 * @param strategy The strategy to use for this key
	 * @return The builder instance
	 */
	public KeyStrategyFactoryBuilder addStrategy(String key, KeyStrategy strategy) {
		strategies.put(key, strategy);
		return this;
	}

	public KeyStrategyFactoryBuilder addStrategy(String key) {
		strategies.put(key, defaultStrategy);
		return this;
	}

	/**
	 * Add multiple strategies at once
	 * @param strategiesMap Map of key to strategy mappings
	 * @return The builder instance
	 */
	public KeyStrategyFactoryBuilder addStrategies(Map<String, KeyStrategy> strategiesMap) {
		strategies.putAll(strategiesMap);
		return this;
	}

	/**
	 * Set the default strategy to use when no specific strategy is found
	 * @param defaultStrategy The default strategy
	 * @return The builder instance
	 */
	public KeyStrategyFactoryBuilder defaultStrategy(KeyStrategy defaultStrategy) {
		this.defaultStrategy = defaultStrategy;
		return this;
	}

	/**
	 * Add a custom strategy with a function
	 * @param key The key to associate with the strategy
	 * @param strategyFunction The function that creates the strategy
	 * @return The builder instance
	 */
	public KeyStrategyFactoryBuilder addCustomStrategy(String key, Function<String, KeyStrategy> strategyFunction) {
		strategies.put(key, strategyFunction.apply(key));
		return this;
	}

	/**
	 * Add strategies based on key patterns using regex
	 * @param pattern The regex pattern to match keys
	 * @param strategy The strategy to apply to matching keys
	 * @return The builder instance
	 */
	public KeyStrategyFactoryBuilder addPatternStrategy(String pattern, KeyStrategy strategy) {
		Pattern regex = Pattern.compile(pattern);
		return addCustomStrategy(pattern, key -> {
			if (regex.matcher(key).matches()) {
				return strategy;
			}
			return null; // Will be filtered out
		});
	}

	/**
	 * Add strategies for keys that match a predicate
	 * @param predicate The predicate to test keys
	 * @param strategy The strategy to apply to matching keys
	 * @return The builder instance
	 */
	public KeyStrategyFactoryBuilder addPredicateStrategy(Predicate<String> predicate, KeyStrategy strategy) {
		return addCustomStrategy("predicate_" + predicate.hashCode(), key -> {
			if (predicate.test(key)) {
				return strategy;
			}
			return null; // Will be filtered out
		});
	}

	/**
	 * Add strategies for keys ending with specific suffixes
	 * @param suffix The suffix to match
	 * @param strategy The strategy to apply
	 * @return The builder instance
	 */
	public KeyStrategyFactoryBuilder addSuffixStrategy(String suffix, KeyStrategy strategy) {
		return addPredicateStrategy(key -> key.endsWith(suffix), strategy);
	}

	/**
	 * Add strategies for keys starting with specific prefixes
	 * @param prefix The prefix to match
	 * @param strategy The strategy to apply
	 * @return The builder instance
	 */
	public KeyStrategyFactoryBuilder addPrefixStrategy(String prefix, KeyStrategy strategy) {
		return addPredicateStrategy(key -> key.startsWith(prefix), strategy);
	}

	/**
	 * Add strategies for keys containing specific substrings
	 * @param substring The substring to match
	 * @param strategy The strategy to apply
	 * @return The builder instance
	 */
	public KeyStrategyFactoryBuilder addContainsStrategy(String substring, KeyStrategy strategy) {
		return addPredicateStrategy(key -> key.contains(substring), strategy);
	}

	/**
	 * Remove a strategy for a specific key
	 * @param key The key to remove strategy for
	 * @return The builder instance
	 */
	public KeyStrategyFactoryBuilder removeStrategy(String key) {
		strategies.remove(key);
		return this;
	}

	/**
	 * Clear all strategies
	 * @return The builder instance
	 */
	public KeyStrategyFactoryBuilder clearStrategies() {
		strategies.clear();
		return this;
	}

	/**
	 * Get the current number of strategies
	 * @return The number of strategies
	 */
	public int getStrategyCount() {
		return strategies.size();
	}

	/**
	 * Check if a strategy exists for the given key
	 * @param key The key to check
	 * @return true if strategy exists, false otherwise
	 */
	public boolean hasStrategy(String key) {
		return strategies.containsKey(key);
	}

	/**
	 * Get a copy of all current strategies
	 * @return Map of all strategies
	 */
	public Map<String, KeyStrategy> getStrategies() {
		return new HashMap<>(strategies);
	}

	/**
	 * Build the KeyStrategyFactory
	 * @return A new KeyStrategyFactory instance
	 */
	public KeyStrategyFactory build() {
		return () -> new HashMap<>(strategies);
	}

}

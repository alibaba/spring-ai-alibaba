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
package com.alibaba.cloud.ai.graph.agent.flow.strategy;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry for FlowGraphStrategy implementations. This allows for dynamic registration
 * and lookup of graph building strategies, enabling extensibility without modifying core
 * code.
 *
 * <p>
 * The registry is thread-safe and supports runtime registration of new strategies.
 * </p>
 */
public class FlowGraphBuildingStrategyRegistry {

	private static final FlowGraphBuildingStrategyRegistry INSTANCE = new FlowGraphBuildingStrategyRegistry();

	private final Map<String, FlowGraphBuildingStrategy> strategies = new ConcurrentHashMap<>();

	private FlowGraphBuildingStrategyRegistry() {
		// Initialize with default strategies
		registerDefaultStrategies();
	}

	/**
	 * Gets the singleton instance of the registry.
	 * @return the registry instance
	 */
	public static FlowGraphBuildingStrategyRegistry getInstance() {
		return INSTANCE;
	}

	/**
	 * Registers a new graph building strategy.
	 * @param strategy the strategy to register
	 * @throws IllegalArgumentException if strategy is null or type is already registered
	 */
	public void registerStrategy(FlowGraphBuildingStrategy strategy) {
		if (strategy == null) {
			throw new IllegalArgumentException("Strategy cannot be null");
		}

		String type = strategy.getStrategyType();
		if (type == null || type.trim().isEmpty()) {
			throw new IllegalArgumentException("Strategy type cannot be null or empty");
		}

		if (strategies.containsKey(type)) {
			throw new IllegalArgumentException("Strategy type '" + type + "' is already registered");
		}

		strategies.put(type, strategy);
	}

	/**
	 * Gets a strategy by type.
	 * @param type the strategy type
	 * @return the strategy implementation
	 * @throws IllegalArgumentException if no strategy is found for the type
	 */
	public FlowGraphBuildingStrategy getStrategy(String type) {
		if (type == null || type.trim().isEmpty()) {
			throw new IllegalArgumentException("Strategy type cannot be null or empty");
		}

		FlowGraphBuildingStrategy strategy = strategies.get(type);
		if (strategy == null) {
			throw new IllegalArgumentException("No strategy registered for type: " + type);
		}

		return strategy;
	}

	/**
	 * Checks if a strategy is registered for the given type.
	 * @param type the strategy type
	 * @return true if a strategy is registered, false otherwise
	 */
	public boolean hasStrategy(String type) {
		return type != null && strategies.containsKey(type);
	}

	/**
	 * Gets all registered strategy types.
	 * @return a set of all registered strategy types
	 */
	public Set<String> getRegisteredTypes() {
		return Set.copyOf(strategies.keySet());
	}

	/**
	 * Unregisters a strategy (mainly for testing purposes).
	 * @param type the strategy type to unregister
	 * @return the unregistered strategy, or null if none was found
	 */
	public FlowGraphBuildingStrategy unregisterStrategy(String type) {
		return strategies.remove(type);
	}

	/**
	 * Clears all registered strategies (mainly for testing purposes).
	 */
	public void clear() {
		strategies.clear();
	}

	/**
	 * Registers the default built-in strategies.
	 */
	private void registerDefaultStrategies() {
		// Default strategies will be registered here
		// This allows the system to work without explicit registration
		registerStrategy(new SequentialGraphBuildingStrategy());
		registerStrategy(new RoutingGraphBuildingStrategy());
		registerStrategy(new ParallelGraphBuildingStrategy());
		registerStrategy(new ConditionalGraphBuildingStrategy());
		registerStrategy(new LoopGraphBuildingStrategy());
	}

}

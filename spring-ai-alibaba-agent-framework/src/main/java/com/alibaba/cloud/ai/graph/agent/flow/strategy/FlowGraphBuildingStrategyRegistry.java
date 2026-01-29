/*
 * Copyright 2024-2026 the original author or authors.
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

import com.alibaba.cloud.ai.graph.agent.flow.enums.FlowAgentEnum;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

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

	private final Map<String, Supplier<FlowGraphBuildingStrategy>> strategyFactories = new ConcurrentHashMap<>();

	private FlowGraphBuildingStrategyRegistry() {
		// Initialize with default strategies (each createStrategy() returns a new instance)
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
	 * Registers a new graph building strategy (same instance returned each time).
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

		if (strategyFactories.containsKey(type)) {
			throw new IllegalArgumentException("Strategy type '" + type + "' is already registered");
		}

		strategyFactories.put(type, () -> strategy);
	}

	/**
	 * Registers a strategy factory. Each call to {@link #createStrategy(String)} or
	 * {@link #getStrategy(String)} will use the factory to obtain a strategy instance.
	 * @param type the strategy type
	 * @param factory the factory that creates strategy instances
	 * @throws IllegalArgumentException if type or factory is null, or type is already registered
	 */
	public void registerStrategy(String type, Supplier<FlowGraphBuildingStrategy> factory) {
		if (type == null || type.trim().isEmpty()) {
			throw new IllegalArgumentException("Strategy type cannot be null or empty");
		}
		if (factory == null) {
			throw new IllegalArgumentException("Strategy factory cannot be null");
		}
		if (strategyFactories.containsKey(type)) {
			throw new IllegalArgumentException("Strategy type '" + type + "' is already registered");
		}
		strategyFactories.put(type, factory);
	}

	/**
	 * Creates a new strategy instance by type. For built-in strategies a new instance is
	 * returned each time; for strategies registered via {@link #registerStrategy(FlowGraphBuildingStrategy)}
	 * the same instance is returned.
	 * @param type the strategy type
	 * @return a strategy implementation
	 * @throws IllegalArgumentException if no strategy is found for the type
	 */
	public FlowGraphBuildingStrategy createStrategy(String type) {
		if (type == null || type.trim().isEmpty()) {
			throw new IllegalArgumentException("Strategy type cannot be null or empty");
		}

		Supplier<FlowGraphBuildingStrategy> factory = strategyFactories.get(type);
		if (factory == null) {
			throw new IllegalArgumentException("No strategy registered for type: " + type);
		}

		return factory.get();
	}

	/**
	 * Gets a strategy by type (delegates to the registered factory).
	 * @param type the strategy type
	 * @return the strategy implementation
	 * @throws IllegalArgumentException if no strategy is found for the type
	 */
	public FlowGraphBuildingStrategy getStrategy(String type) {
		return createStrategy(type);
	}

	/**
	 * Checks if a strategy is registered for the given type.
	 * @param type the strategy type
	 * @return true if a strategy is registered, false otherwise
	 */
	public boolean hasStrategy(String type) {
		return type != null && strategyFactories.containsKey(type);
	}

	/**
	 * Gets all registered strategy types.
	 * @return a set of all registered strategy types
	 */
	public Set<String> getRegisteredTypes() {
		return Set.copyOf(strategyFactories.keySet());
	}

	/**
	 * Unregisters a strategy (mainly for testing purposes).
	 * @param type the strategy type to unregister
	 * @return the unregistered strategy factory, or null if none was found
	 */
	public Supplier<FlowGraphBuildingStrategy> unregisterStrategy(String type) {
		return strategyFactories.remove(type);
	}

	/**
	 * Clears all registered strategies (mainly for testing purposes).
	 */
	public void clear() {
		strategyFactories.clear();
	}

	/**
	 * Registers the default built-in strategies. Each {@link #createStrategy(String)}
	 * call returns a new strategy instance for these types.
	 */
	private void registerDefaultStrategies() {
		registerStrategy(FlowAgentEnum.SEQUENTIAL.getType(), SequentialGraphBuildingStrategy::new);
		registerStrategy(FlowAgentEnum.ROUTING.getType(), RoutingGraphBuildingStrategy::new);
		registerStrategy(FlowAgentEnum.PARALLEL.getType(), ParallelGraphBuildingStrategy::new);
		registerStrategy(FlowAgentEnum.CONDITIONAL.getType(), ConditionalGraphBuildingStrategy::new);
		registerStrategy(FlowAgentEnum.LOOP.getType(), LoopGraphBuildingStrategy::new);
		registerStrategy(FlowAgentEnum.SUPERVISOR.getType(), SupervisorGraphBuildingStrategy::new);
	}

}

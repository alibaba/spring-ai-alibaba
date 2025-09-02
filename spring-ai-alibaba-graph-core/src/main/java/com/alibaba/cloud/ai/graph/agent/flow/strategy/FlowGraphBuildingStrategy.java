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

import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

/**
 * Strategy interface for building StateGraphs for different FlowAgent types. This design
 * allows for extensible graph building without modifying existing code.
 *
 * <p>
 * Each FlowAgent type should have its own strategy implementation that knows how to
 * construct the appropriate graph structure.
 * </p>
 */
public interface FlowGraphBuildingStrategy {

	/**
	 * Builds a StateGraph based on the provided configuration.
	 * @param config the graph configuration containing all necessary parameters
	 * @return the constructed StateGraph
	 * @throws GraphStateException if graph construction fails
	 */
	StateGraph buildGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException;

	/**
	 * Returns the type identifier for this strategy. This is used for registration and
	 * lookup purposes.
	 * @return the strategy type identifier
	 */
	String getStrategyType();

	/**
	 * Validates that the configuration contains all required parameters for this
	 * strategy.
	 * @param config the configuration to validate
	 * @throws IllegalArgumentException if validation fails
	 */
	default void validateConfig(FlowGraphBuilder.FlowGraphConfig config) {
		if (config == null) {
			throw new IllegalArgumentException("Configuration cannot be null");
		}
		if (config.getName() == null || config.getName().trim().isEmpty()) {
			throw new IllegalArgumentException("Graph name must be provided");
		}
		if (config.getKeyStrategyFactory() == null) {
			throw new IllegalArgumentException("KeyStrategyFactory must be provided");
		}
		if (config.getRootAgent() == null) {
			throw new IllegalArgumentException("Root agent must be provided");
		}
	}

}

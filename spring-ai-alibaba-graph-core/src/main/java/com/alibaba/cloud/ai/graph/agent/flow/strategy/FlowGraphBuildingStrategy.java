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
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.FlowAgent;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.Map;

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
		if (config.getRootAgent() == null) {
			throw new IllegalArgumentException("Root agent must be provided");
		}
		if (config.getKeyStrategyFactory() == null) {
			// Generate a new KeyStrategyFactory based on agent keys
			KeyStrategyFactory generatedFactory = generateKeyStrategyFactory(config);
			config.setKeyStrategyFactory(generatedFactory);
		}
	}

	/**
	 * Generates a KeyStrategyFactory based on the root agent and sub-agents.
	 * @param config the configuration containing agents
	 * @return the generated KeyStrategyFactory
	 */
	default KeyStrategyFactory generateKeyStrategyFactory(FlowGraphBuilder.FlowGraphConfig config) {
		return () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();

			// Process root agent
			BaseAgent rootAgent = config.getRootAgent();
			if (rootAgent != null) {
				// Add root agent's inputKeys if it's a FlowAgent
				if (rootAgent instanceof FlowAgent flowAgent) {
					if (flowAgent.inputKeys() != null) {
						for (String inputKey : flowAgent.inputKeys()) {
							if (inputKey != null) {
								keyStrategyMap.put(inputKey, new AppendStrategy());
							}
						}
					}

					// Merge inputKeysWithStrategy if present
					if (flowAgent.inputKeysWithStrategy() != null) {
						Map<String, KeyStrategy> inputKeyStrategies = flowAgent.inputKeysWithStrategy().apply();
						if (inputKeyStrategies != null) {
							keyStrategyMap.putAll(inputKeyStrategies);
						}
					}
				}

				// Add root agent's outputKey
				if (rootAgent.outputKey() != null) {
					keyStrategyMap.put(rootAgent.outputKey(), new AppendStrategy());
				}

				// Merge outputKeyWithStrategy if present
				if (rootAgent.outputKeyWithStrategy() != null) {
					Map<String, KeyStrategy> outputKeyStrategies = rootAgent.outputKeyWithStrategy().apply();
					if (outputKeyStrategies != null) {
						keyStrategyMap.putAll(outputKeyStrategies);
					}
				}
			}

			// Process sub-agents
			if (config.getSubAgents() != null) {
				for (BaseAgent subAgent : config.getSubAgents()) {
					if (subAgent instanceof ReactAgent) {
						// ReactAgent: only handle outputKey
						if (subAgent.outputKey() != null) {
							keyStrategyMap.put(subAgent.outputKey(), new AppendStrategy());
						}

						// Merge outputKeyWithStrategy if present
						if (subAgent.outputKeyWithStrategy() != null) {
							Map<String, KeyStrategy> outputKeyStrategies = subAgent.outputKeyWithStrategy().apply();
							if (outputKeyStrategies != null) {
								keyStrategyMap.putAll(outputKeyStrategies);
							}
						}
					} else if (subAgent instanceof FlowAgent flowAgent) {
						// FlowAgent: handle both inputKeys and outputKey
						if (flowAgent.inputKeys() != null) {
							for (String inputKey : flowAgent.inputKeys()) {
								if (inputKey != null) {
									keyStrategyMap.put(inputKey, new AppendStrategy());
								}
							}
						}

						// Merge inputKeysWithStrategy if present
						if (flowAgent.inputKeysWithStrategy() != null) {
							Map<String, KeyStrategy> inputKeyStrategies = flowAgent.inputKeysWithStrategy().apply();
							if (inputKeyStrategies != null) {
								keyStrategyMap.putAll(inputKeyStrategies);
							}
						}

						if (flowAgent.outputKey() != null) {
							keyStrategyMap.put(flowAgent.outputKey(), new AppendStrategy());
						}

						// Merge outputKeyWithStrategy if present
						if (flowAgent.outputKeyWithStrategy() != null) {
							Map<String, KeyStrategy> outputKeyStrategies = flowAgent.outputKeyWithStrategy().apply();
							if (outputKeyStrategies != null) {
								keyStrategyMap.putAll(outputKeyStrategies);
							}
						}
					}
				}
			}

			return keyStrategyMap;
		};
	}

}

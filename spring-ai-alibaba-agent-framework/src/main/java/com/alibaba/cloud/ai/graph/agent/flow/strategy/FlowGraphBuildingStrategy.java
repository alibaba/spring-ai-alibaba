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

import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.Agent;
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
			config.keyStrategyFactory(generatedFactory);
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
			KeyStrategy defaultStrategy = new ReplaceStrategy();

			// Process sub-agents
			if (config.getSubAgents() != null) {
				for (Agent subAgent : config.getSubAgents()) {
					processAgentKeyStrategies(subAgent, keyStrategyMap, defaultStrategy);
				}
			}

			keyStrategyMap.put("messages", new AppendStrategy());

			return keyStrategyMap;
		};
	}

	/**
	 * Recursively processes key strategies for an agent and its sub-agents.
	 * @param agent the agent to process
	 * @param keyStrategyMap the map to populate with key strategies
	 * @param defaultStrategy the default strategy to use when none is specified
	 */
	default void processAgentKeyStrategies(Agent agent, Map<String, KeyStrategy> keyStrategyMap, KeyStrategy defaultStrategy) {
		if (agent instanceof ReactAgent reactAgent) {
			// ReactAgent: only handle outputKey
			processOutputKey(reactAgent.getOutputKey(), reactAgent.getOutputKeyStrategy(), keyStrategyMap, defaultStrategy);
		} else if (agent instanceof FlowAgent flowAgent) {
			// FlowAgent: recursively process sub-agents
			if (flowAgent.subAgents() != null) {
				for (Agent subAgent : flowAgent.subAgents()) {
					processAgentKeyStrategies(subAgent, keyStrategyMap, defaultStrategy);
				}
			}
		}
	}

	/**
	 * Processes output key and strategy for an agent.
	 * @param outputKey the output key to process
	 * @param outputKeyStrategy the strategy for the output key
	 * @param keyStrategyMap the map to populate
	 * @param defaultStrategy the default strategy to use when none is specified
	 */
	default void processOutputKey(String outputKey, KeyStrategy outputKeyStrategy, Map<String, KeyStrategy> keyStrategyMap, KeyStrategy defaultStrategy) {
		if (outputKey != null) {
			if (outputKeyStrategy != null) {
				keyStrategyMap.put(outputKey, outputKeyStrategy);
			} else {
				keyStrategyMap.put(outputKey, defaultStrategy);
			}
		}
	}

	static void addSubAgentNode(Agent subAgent, StateGraph newGraph) throws GraphStateException {
		if (subAgent instanceof FlowAgent flowAgent) {
			newGraph.addNode(flowAgent.name(), flowAgent.asStateGraph());
		} else if (subAgent instanceof BaseAgent baseAgent) {
			newGraph.addNode(baseAgent.name(), baseAgent.asNode(baseAgent.isIncludeContents(), baseAgent.isReturnReasoningContents()));
		} else {
			throw new IllegalArgumentException(subAgent.getClass().getName() + " only supports FlowAgent and BaseAgent types");
		}
	}

}

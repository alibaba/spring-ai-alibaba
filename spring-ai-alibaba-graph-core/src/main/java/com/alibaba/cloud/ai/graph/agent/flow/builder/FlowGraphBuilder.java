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
package com.alibaba.cloud.ai.graph.agent.flow.builder;

import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.flow.strategy.FlowGraphBuildingStrategyRegistry;
import com.alibaba.cloud.ai.graph.agent.flow.strategy.FlowGraphBuildingStrategy;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.springframework.ai.chat.model.ChatModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A centralized builder for constructing StateGraphs for different FlowAgent types. This
 * class encapsulates the graph construction logic, making it reusable and easier to
 * maintain.
 */
public class FlowGraphBuilder {

	/**
	 * Generic graph builder that delegates to specific strategy implementations.
	 * @param strategyType the type of strategy to use for building
	 * @param config the configuration for graph building
	 * @return the constructed StateGraph
	 * @throws GraphStateException if graph construction fails
	 */
	public static StateGraph buildGraph(String strategyType, FlowGraphConfig config) throws GraphStateException {
		FlowGraphBuildingStrategy strategy = FlowGraphBuildingStrategyRegistry.getInstance().getStrategy(strategyType);
		strategy.validateConfig(config);
		return strategy.buildGraph(config);
	}

	/**
	 * Configuration class for graph building parameters.
	 */
	public static class FlowGraphConfig {

		private String name;

		private KeyStrategyFactory keyStrategyFactory;

		private BaseAgent rootAgent;

		private List<BaseAgent> subAgents;

		private Map<String, BaseAgent> conditionalAgents;

		private ChatModel chatModel;

		private Map<String, Object> customProperties = new HashMap<>();

		// Getters and setters
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public KeyStrategyFactory getKeyStrategyFactory() {
			return keyStrategyFactory;
		}

		public void setKeyStrategyFactory(KeyStrategyFactory keyStrategyFactory) {
			this.keyStrategyFactory = keyStrategyFactory;
		}

		public BaseAgent getRootAgent() {
			return rootAgent;
		}

		public void setRootAgent(BaseAgent rootAgent) {
			this.rootAgent = rootAgent;
		}

		public List<BaseAgent> getSubAgents() {
			return subAgents;
		}

		public void setSubAgents(List<BaseAgent> subAgents) {
			this.subAgents = subAgents;
		}

		public Map<String, BaseAgent> getConditionalAgents() {
			return conditionalAgents;
		}

		public void setConditionalAgents(Map<String, BaseAgent> conditionalAgents) {
			this.conditionalAgents = conditionalAgents;
		}

		public ChatModel getChatModel() {
			return chatModel;
		}

		public void setChatModel(ChatModel chatModel) {
			this.chatModel = chatModel;
		}

		// Builder methods
		public static FlowGraphConfig builder() {
			return new FlowGraphConfig();
		}

		public FlowGraphConfig name(String name) {
			this.name = name;
			return this;
		}

		public FlowGraphConfig keyStrategyFactory(KeyStrategyFactory factory) {
			this.keyStrategyFactory = factory;
			return this;
		}

		public FlowGraphConfig rootAgent(BaseAgent agent) {
			this.rootAgent = agent;
			return this;
		}

		public FlowGraphConfig subAgents(List<BaseAgent> agents) {
			this.subAgents = agents;
			return this;
		}

		public FlowGraphConfig conditionalAgents(Map<String, BaseAgent> agents) {
			this.conditionalAgents = agents;
			return this;
		}

		public FlowGraphConfig chatModel(ChatModel model) {
			this.chatModel = model;
			return this;
		}

		public FlowGraphConfig customProperty(String key, Object value) {
			this.customProperties.put(key, value);
			return this;
		}

		public Object getCustomProperty(String key) {
			return this.customProperties.get(key);
		}

		public Map<String, Object> getCustomProperties() {
			return Map.copyOf(this.customProperties);
		}

	}

}

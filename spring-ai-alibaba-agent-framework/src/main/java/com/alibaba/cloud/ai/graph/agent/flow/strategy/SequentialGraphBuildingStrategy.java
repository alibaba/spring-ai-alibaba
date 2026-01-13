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

import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.FlowAgent;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.enums.FlowAgentEnum;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;


/**
 * Strategy for building sequential execution graphs. In a sequential graph, agents are
 * connected in a linear chain where each agent's output becomes the input for the next
 * agent.
 */
public class SequentialGraphBuildingStrategy extends AbstractFlowGraphBuildingStrategy {

	@Override
	protected void buildCoreGraph(FlowGraphBuilder.FlowGraphConfig config)
			throws GraphStateException {
		validateSequentialConfig(config);

		// Add beforeModel hooks
		String currentNodeName = this.rootAgent.name();
		if (!this.beforeModelHooks.isEmpty()) {
			currentNodeName = addBeforeModelHookNodesToGraph(this.graph, this.rootAgent.name(), this.beforeModelHooks);
		}

		// Process sub-agents sequentially
		Agent currentAgent = this.rootAgent;
		for (Agent subAgent : config.getSubAgents()) {
			FlowGraphBuildingStrategy.addSubAgentNode(subAgent, this.graph);
			this.graph.addEdge(currentAgent.name(), subAgent.name());
			currentAgent = subAgent;
		}

		// Add afterModel hooks if present
		if (!this.afterModelHooks.isEmpty()) {
			String afterModelNodeName = addAfterModelHookNodesToGraph(this.graph, currentAgent.name(), this.afterModelHooks);
			currentNodeName = afterModelNodeName;
		} else {
			currentNodeName = currentAgent.name();
		}

		// Connect the last node to exit node
		this.graph.addEdge(currentNodeName, this.exitNode);
	}

	@Override
	public String getStrategyType() {
		return FlowAgentEnum.SEQUENTIAL.getType();
	}

	@Override
	public void validateConfig(FlowGraphBuilder.FlowGraphConfig config) {
		super.validateConfig(config);
		validateSequentialConfig(config);
	}

	/**
	 * Validates sequential-specific configuration requirements.
	 * @param config the configuration to validate
	 * @throws IllegalArgumentException if validation fails
	 */
	private void validateSequentialConfig(FlowGraphBuilder.FlowGraphConfig config) {
		if (config.getSubAgents() == null || config.getSubAgents().isEmpty()) {
			throw new IllegalArgumentException("Sequential flow requires at least one sub-agent");
		}

		// Ensure root agent is a FlowAgent for input key access
		if (!(config.getRootAgent() instanceof FlowAgent)) {
			throw new IllegalArgumentException("Sequential flow requires root agent to be a FlowAgent");
		}
	}

}

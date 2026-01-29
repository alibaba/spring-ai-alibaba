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
import com.alibaba.cloud.ai.graph.agent.flow.node.TransparentNode;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

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

		// Determine the start node for sequential execution
		// If there are beforeModel hooks, they will be connected to rootAgent.name() by the template method
		// Otherwise, rootAgent.name() will be the entry point
		String sequentialStartNode = getRootAgent().name();

		// Add a transparent node as the sequential start point
		this.graph.addNode(sequentialStartNode, node_async(new TransparentNode()));

		// Connect beforeModel hooks to the sequential start node if they exist
		if (!this.beforeModelHooks.isEmpty()) {
			connectBeforeModelHookEdges(this.graph, sequentialStartNode, this.beforeModelHooks);
		}

		// Process sub-agents sequentially
		Agent currentAgent = getRootAgent();
		for (Agent subAgent : config.getSubAgents()) {
			FlowGraphBuildingStrategy.addSubAgentNode(subAgent, this.graph);
			this.graph.addEdge(currentAgent.name(), subAgent.name());
			currentAgent = subAgent;
		}

		// Add afterModel hooks if present
		String finalNode;
		if (!this.afterModelHooks.isEmpty()) {
			finalNode = connectAfterModelHookEdges(this.graph, currentAgent.name(), this.afterModelHooks);
		} else {
			finalNode = currentAgent.name();
		}

		// Connect the last node to exit node
		this.graph.addEdge(finalNode, this.exitNode);
	}

	@Override
	protected void connectBeforeModelHooks() throws GraphStateException {
		// Sequential strategy already handles hook connections in buildCoreGraph
		// Override with empty implementation to avoid duplicate connections
	}

	@Override
	protected void connectAfterModelHooks() throws GraphStateException {
		// Sequential strategy already handles hook connections in buildCoreGraph
		// Override with empty implementation to avoid duplicate connections
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

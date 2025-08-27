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
import com.alibaba.cloud.ai.graph.agent.flow.agent.FlowAgent;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.enums.FlowAgentEnum;
import com.alibaba.cloud.ai.graph.agent.flow.node.TransparentNode;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * Strategy for building sequential execution graphs. In a sequential graph, agents are
 * connected in a linear chain where each agent's output becomes the input for the next
 * agent.
 */
public class SequentialGraphBuildingStrategy implements FlowGraphBuildingStrategy {

	@Override
	public StateGraph buildGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException {
		validateConfig(config);
		validateSequentialConfig(config);

		StateGraph graph = new StateGraph(config.getName(), config.getKeyStrategyFactory());
		BaseAgent rootAgent = config.getRootAgent();

		// Add root transparent node
		graph.addNode(rootAgent.name(),
				node_async(new TransparentNode(rootAgent.outputKey(), ((FlowAgent) rootAgent).inputKey())));

		// Add starting edge
		graph.addEdge(START, rootAgent.name());

		// Process sub-agents sequentially
		BaseAgent currentAgent = rootAgent;
		for (BaseAgent subAgent : config.getSubAgents()) {
			// Add the current sub-agent as a node
			graph.addNode(subAgent.name(), subAgent.asAsyncNodeAction(currentAgent.outputKey(), subAgent.outputKey()));
			graph.addEdge(currentAgent.name(), subAgent.name());
			currentAgent = subAgent;
		}

		// Connect the last agent to END
		graph.addEdge(currentAgent.name(), END);

		return graph;
	}

	@Override
	public String getStrategyType() {
		return FlowAgentEnum.SEQUENTIAL.getType();
	}

	@Override
	public void validateConfig(FlowGraphBuilder.FlowGraphConfig config) {
		FlowGraphBuildingStrategy.super.validateConfig(config);
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

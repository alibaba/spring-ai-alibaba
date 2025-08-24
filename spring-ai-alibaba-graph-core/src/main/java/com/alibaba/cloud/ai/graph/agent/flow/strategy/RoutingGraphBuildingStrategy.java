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

import java.util.HashMap;
import java.util.Map;

import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.FlowAgent;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.enums.FlowAgentEnum;
import com.alibaba.cloud.ai.graph.agent.flow.node.RoutingEdgeAction;
import com.alibaba.cloud.ai.graph.agent.flow.node.TransparentNode;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * Strategy for building LLM-based routing graphs. In a routing graph, an LLM decides
 * which sub-agent should handle the task based on the input content and agent
 * capabilities.
 */
public class RoutingGraphBuildingStrategy implements FlowGraphBuildingStrategy {

	@Override
	public StateGraph buildGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException {
		validateConfig(config);
		validateRoutingConfig(config);

		StateGraph graph = new StateGraph(config.getName(), config.getKeyStrategyFactory());
		BaseAgent rootAgent = config.getRootAgent();

		// Add root transparent node
		graph.addNode(rootAgent.name(),
				node_async(new TransparentNode(rootAgent.outputKey(), ((FlowAgent) rootAgent).inputKey())));

		// Add starting edge
		graph.addEdge(START, rootAgent.name());

		// Process sub-agents for routing
		Map<String, String> edgeRoutingMap = new HashMap<>();
		for (BaseAgent subAgent : config.getSubAgents()) {
			// Add the current sub-agent as a node
			graph.addNode(subAgent.name(), subAgent.asAsyncNodeAction(rootAgent.outputKey(), subAgent.outputKey()));
			edgeRoutingMap.put(subAgent.name(), subAgent.name());

			// Connect sub-agents to END (unless they are FlowAgents with their own
			// sub-agents)
			if (subAgent instanceof FlowAgent subFlowAgent) {
				if (subFlowAgent.subAgents() == null || subFlowAgent.subAgents().isEmpty()) {
					graph.addEdge(subAgent.name(), END);
				}
			}
			else {
				graph.addEdge(subAgent.name(), END);
			}
		}

		// Connect parent to sub-agents via conditional routing
		graph.addConditionalEdges(rootAgent.name(),
				new RoutingEdgeAction(config.getChatModel(), rootAgent, config.getSubAgents()), edgeRoutingMap);

		return graph;
	}

	@Override
	public String getStrategyType() {
		return FlowAgentEnum.ROUTING.getType();
	}

	@Override
	public void validateConfig(FlowGraphBuilder.FlowGraphConfig config) {
		FlowGraphBuildingStrategy.super.validateConfig(config);
		validateRoutingConfig(config);
	}

	/**
	 * Validates routing-specific configuration requirements.
	 * @param config the configuration to validate
	 * @throws IllegalArgumentException if validation fails
	 */
	private void validateRoutingConfig(FlowGraphBuilder.FlowGraphConfig config) {
		if (config.getSubAgents() == null || config.getSubAgents().isEmpty()) {
			throw new IllegalArgumentException("Routing flow requires at least one sub-agent");
		}

		if (config.getChatModel() == null) {
			throw new IllegalArgumentException("Routing flow requires a ChatModel for decision making");
		}

		// Ensure root agent is a FlowAgent for input key access
		if (!(config.getRootAgent() instanceof FlowAgent)) {
			throw new IllegalArgumentException("Routing flow requires root agent to be a FlowAgent");
		}
	}

}

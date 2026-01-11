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

import java.util.HashMap;
import java.util.Map;

import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.FlowAgent;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.enums.FlowAgentEnum;
import com.alibaba.cloud.ai.graph.agent.flow.node.RoutingNode;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

/**
 * Strategy for building LLM-based routing graphs. In a routing graph, an LLM decides
 * which sub-agent should handle the task based on the input content and agent
 * capabilities.
 * 
 * <p>This strategy extends AbstractFlowGraphBuildingStrategy and customizes hook handling:
 * beforeModel/afterModel hooks wrap around the RoutingNode instead of the root agent.</p>
 */
public class RoutingGraphBuildingStrategy extends AbstractFlowGraphBuildingStrategy {

	/**
	 * Override to connect beforeAgent hooks directly to root transparent node,
	 * skipping beforeModel hooks (which will be handled around RoutingNode).
	 */
	@Override
	protected String determineNextNodeAfterBeforeAgentHooks() {
		// Always connect to root transparent node
		return this.rootAgent.name();
	}

	/**
	 * Override to determine entry node without considering beforeModel hooks,
	 * since those hooks are handled around RoutingNode in buildCoreGraph().
	 */
	@Override
	protected String determineEntryNodeForGraph() {
		// Don't consider beforeModel hooks for entry node (they're handled around RoutingNode)
		if (!this.beforeAgentHooks.isEmpty()) {
			return this.beforeAgentHooks.get(0).getName() + ".before";
		}
		return this.rootAgent.name();
	}

	@Override
	protected void buildCoreGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException {
		validateRoutingConfig(config);

		// Step 1: Add routing node (where LLM makes routing decision)
		String routingNodeName = rootAgent.name() + "_routing";
		graph.addNode(routingNodeName, new RoutingNode(config.getChatModel(), rootAgent, config.getSubAgents()));

		// Step 2: Add beforeModel hooks around RoutingNode and connect
		String firstBeforeModelNode = addBeforeModelHookNodesToGraph(graph, routingNodeName, beforeModelHooks);
		graph.addEdge(rootAgent.name(), firstBeforeModelNode);

		// Step 3: Add afterModel hooks after RoutingNode
		String routingExitNode = addAfterModelHookNodesToGraph(graph, routingNodeName, afterModelHooks);

		// Step 4: Process sub-agents for routing
		Map<String, String> edgeRoutingMap = new HashMap<>();
		for (Agent subAgent : config.getSubAgents()) {
			// Add the current sub-agent as a node
			FlowGraphBuildingStrategy.addSubAgentNode(subAgent, graph);
			edgeRoutingMap.put(subAgent.name(), subAgent.name());
			// Connect sub-agents to exitNode (afterAgent hooks or END)
			graph.addEdge(subAgent.name(), this.exitNode);
		}

		// Step 5: Connect routing exit to sub-agents via conditional routing
		// The routing decision is stored in state by RoutingNode
		AsyncEdgeAction routingDecisionAction = state -> {
			String decision = (String) state.value(RoutingNode.getRoutingDecisionKey()).orElse(null);
			if (decision == null) {
				throw new IllegalStateException("Routing decision not found in state");
			}
			return java.util.concurrent.CompletableFuture.completedFuture(decision);
		};
		graph.addConditionalEdges(routingExitNode, routingDecisionAction, edgeRoutingMap);
	}

	@Override
	public String getStrategyType() {
		return FlowAgentEnum.ROUTING.getType();
	}

	@Override
	public void validateConfig(FlowGraphBuilder.FlowGraphConfig config) {
		super.validateConfig(config);
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

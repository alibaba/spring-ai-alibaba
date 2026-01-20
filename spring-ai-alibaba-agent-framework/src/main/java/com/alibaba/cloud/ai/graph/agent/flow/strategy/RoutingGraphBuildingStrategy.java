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

import com.alibaba.cloud.ai.graph.action.AsyncMultiCommandAction;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.FlowAgent;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.enums.FlowAgentEnum;
import com.alibaba.cloud.ai.graph.agent.flow.node.RoutingEdgeAction;
import com.alibaba.cloud.ai.graph.agent.flow.node.TransparentNode;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

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
		graph.addNode(routingNodeName, node_async((state) -> {
			// This is a transparent node that just passes through
			// The actual routing logic is handled by the parallel conditional edges
			return Map.of();
		}));

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

		// Step 5: Add parallel conditional edges for routing
		// This allows routing to one or multiple sub-agents in parallel
		// Note: We use addParallelConditionalEdges here because routingExitNode is the exit
		// from afterModel hooks, not a new node. For simpler cases without hooks, you can use:
		// graph.addNode(nodeId, AsyncMultiCommandAction.node_async(action), mappings)
		RoutingNode routingNode = new RoutingNode(config.getChatModel(), rootAgent, config.getSubAgents());
		graph.addParallelConditionalEdges(
				routingExitNode,
				AsyncMultiCommandAction.node_async(routingNode),
				edgeRoutingMap
		);
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

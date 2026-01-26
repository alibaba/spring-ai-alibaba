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
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.FlowAgent;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.enums.FlowAgentEnum;
import com.alibaba.cloud.ai.graph.agent.flow.node.RoutingNode;
import com.alibaba.cloud.ai.graph.agent.flow.node.TransparentNode;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

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


	@Override
	protected void buildCoreGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException {
		validateRoutingConfig(config);

		// Step 0: Add root agent node (transparent node)
		graph.addNode(rootAgent.name(), node_async(new TransparentNode()));

		// Step 1: Add routing node (where LLM makes routing decision)
		String routingNodeName = rootAgent.name() + "_routing";
		graph.addNode(routingNodeName, node_async((state) -> {
			// This is a transparent node that just passes through
			// The actual routing logic is handled by the parallel conditional edges
			return Map.of();
		}));

		// Step 2: Connect beforeModel hooks to routing node (if any)
		// The hook nodes are already added by parent class, we just need to connect edges
		String firstBeforeModelNode = routingNodeName;
		if (!beforeModelHooks.isEmpty()) {
			firstBeforeModelNode = connectBeforeModelHookEdges(graph, routingNodeName, beforeModelHooks);
		}
		graph.addEdge(rootAgent.name(), firstBeforeModelNode);

		// Step 3: Connect afterModel hooks after routing node (if any)
		// The hook nodes are already added by parent class, we just need to connect edges
		String routingExitNode = routingNodeName;
		if (!afterModelHooks.isEmpty()) {
			routingExitNode = connectAfterModelHookEdges(graph, routingNodeName, afterModelHooks);
		}

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
		RoutingNode routingNode = new RoutingNode(config.getChatModel(), rootAgent, config.getSubAgents());
		graph.addParallelConditionalEdges(
				routingExitNode,
				AsyncMultiCommandAction.node_async(routingNode),
				edgeRoutingMap
		);
	}

	@Override
	protected void connectBeforeModelHooks() throws GraphStateException {
		// Routing strategy already handles hook connections in buildCoreGraph
		// Override with empty implementation to avoid duplicate connections
	}

	@Override
	protected void connectAfterModelHooks() throws GraphStateException {
		// Routing strategy already handles hook connections in buildCoreGraph
		// Override with empty implementation to avoid duplicate connections
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

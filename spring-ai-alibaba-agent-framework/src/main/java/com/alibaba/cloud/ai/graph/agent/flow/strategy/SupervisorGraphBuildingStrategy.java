package com.alibaba.cloud.ai.graph.agent.flow.strategy;
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

import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.FlowAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SupervisorAgent;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.enums.FlowAgentEnum;
import com.alibaba.cloud.ai.graph.agent.flow.node.SupervisorEdgeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;

/**
 * Strategy for building supervisor-based routing graphs. In a supervisor graph, an LLM
 * decides which sub-agent should handle the task, and sub-agents always return to the
 * supervisor after completion. The supervisor can then either route to another sub-agent
 * or mark the task as complete (END).
 */
public class SupervisorGraphBuildingStrategy extends AbstractFlowGraphBuildingStrategy {

	@Override
	protected void buildCoreGraph(FlowGraphBuilder.FlowGraphConfig config)
			throws GraphStateException {
		validateSupervisorConfig(config);

		SupervisorAgent supervisorRootAgent = (SupervisorAgent) this.rootAgent;

		// Add and chain beforeModel hooks
		if (!this.beforeModelHooks.isEmpty()) {
			addBeforeModelHookNodesToGraph(this.graph, this.rootAgent.name(), this.beforeModelHooks);
		}

		// Add afterModel hook nodes and get the routing target
		String afterModelNodeName = addAfterModelHookNodesToGraph(this.graph, this.rootAgent.name(), this.afterModelHooks);

		// Process sub-agents for routing
		Map<String, String> edgeRoutingMap = new HashMap<>();
		for (Agent subAgent : config.getSubAgents()) {
			FlowGraphBuildingStrategy.addSubAgentNode(subAgent, this.graph);
			edgeRoutingMap.put(subAgent.name(), subAgent.name());
			// Sub-agent returns to the entry node (could be beforeAgent/beforeModel hook)
			this.graph.addEdge(subAgent.name(), this.entryNode);
		}

		// Add END as a possible routing destination
		edgeRoutingMap.put(END, END);

		// Connect supervisor to routing logic
		String routingSourceNode = this.afterModelHooks.isEmpty() ? this.rootAgent.name() : afterModelNodeName;
		this.graph.addConditionalEdges(routingSourceNode,
				new SupervisorEdgeAction(config.getChatModel(), supervisorRootAgent, config.getSubAgents()), edgeRoutingMap);
	}

	@Override
	public String getStrategyType() {
		return FlowAgentEnum.SUPERVISOR.getType();
	}

	@Override
	public void validateConfig(FlowGraphBuilder.FlowGraphConfig config) {
		super.validateConfig(config);
		validateSupervisorConfig(config);
	}

	/**
	 * Validates supervisor-specific configuration requirements.
	 * @param config the configuration to validate
	 * @throws IllegalArgumentException if validation fails
	 */
	private void validateSupervisorConfig(FlowGraphBuilder.FlowGraphConfig config) {
		if (config.getSubAgents() == null || config.getSubAgents().isEmpty()) {
			throw new IllegalArgumentException("Supervisor flow requires at least one sub-agent");
		}

		if (config.getChatModel() == null) {
			throw new IllegalArgumentException("Supervisor flow requires a ChatModel for decision making");
		}

		// Ensure root agent is a FlowAgent for input key access
		if (!(config.getRootAgent() instanceof FlowAgent)) {
			throw new IllegalArgumentException("Supervisor flow requires root agent to be a FlowAgent");
		}
	}

}

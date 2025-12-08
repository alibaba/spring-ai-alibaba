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
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.FlowAgent;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.enums.FlowAgentEnum;
import com.alibaba.cloud.ai.graph.agent.flow.node.SupervisorEdgeAction;
import com.alibaba.cloud.ai.graph.agent.flow.node.TransparentNode;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * Strategy for building supervisor-based routing graphs. In a supervisor graph, an LLM
 * decides which sub-agent should handle the task, and sub-agents always return to the
 * supervisor after completion. The supervisor can then either route to another sub-agent
 * or mark the task as complete (END).
 */
public class SupervisorGraphBuildingStrategy implements FlowGraphBuildingStrategy {

	@Override
	public StateGraph buildGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException {
		validateConfig(config);
		validateSupervisorConfig(config);

		StateGraph graph = config.getStateSerializer() != null
				? new StateGraph(config.getName(), config.getKeyStrategyFactory(), config.getStateSerializer())
				: new StateGraph(config.getName(), config.getKeyStrategyFactory());
		Agent rootAgent = config.getRootAgent();

		// Add root transparent node
		graph.addNode(rootAgent.name(), node_async(new TransparentNode()));

		// Add starting edge
		graph.addEdge(START, rootAgent.name());

		// Process sub-agents for routing
		Map<String, String> edgeRoutingMap = new HashMap<>();
		for (Agent subAgent : config.getSubAgents()) {
			// Add the current sub-agent as a node
			FlowGraphBuildingStrategy.addSubAgentNode(subAgent, graph);
			edgeRoutingMap.put(subAgent.name(), subAgent.name());
			// Connect sub-agents back to supervisor (not to END)
			graph.addEdge(subAgent.name(), rootAgent.name());
		}

		// Add END as a possible routing destination
		edgeRoutingMap.put(END, END);

		// Connect parent to sub-agents or END via conditional routing
		graph.addConditionalEdges(rootAgent.name(),
				new SupervisorEdgeAction(config.getChatModel(), rootAgent, config.getSubAgents()), edgeRoutingMap);

		return graph;
	}

	@Override
	public String getStrategyType() {
		return FlowAgentEnum.SUPERVISOR.getType();
	}

	@Override
	public void validateConfig(FlowGraphBuilder.FlowGraphConfig config) {
		FlowGraphBuildingStrategy.super.validateConfig(config);
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


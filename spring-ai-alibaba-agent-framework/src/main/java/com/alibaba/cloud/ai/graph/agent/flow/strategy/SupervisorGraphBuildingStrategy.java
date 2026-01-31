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
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.FlowAgent;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.enums.FlowAgentEnum;
import com.alibaba.cloud.ai.graph.agent.flow.node.MainAgentNodeAction;
import com.alibaba.cloud.ai.graph.agent.flow.node.MainAgentToSupervisorEdgeAction;
import com.alibaba.cloud.ai.graph.agent.flow.node.SupervisorNodeFromState;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.cloud.ai.graph.action.AsyncMultiCommandAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

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

		Agent rootAgent = getRootAgent();
		Agent mainAgent = config.getMainAgent();
		// First node is mainAgent via MainAgentNodeAction (proxy), no asNode()
		if (mainAgent instanceof ReactAgent reactAgent) {
			this.graph.addNode(rootAgent.name(), AsyncNodeActionWithConfig.node_async(new MainAgentNodeAction(reactAgent, config.getSubAgents())));
		}
		else {
			throw new IllegalArgumentException("Supervisor mainAgent must be a ReactAgent, got: " + (mainAgent != null ? mainAgent.getClass().getName() : "null"));
		}

		// Process sub-agents for routing
		Map<String, String> edgeRoutingMap = new HashMap<>();
		for (Agent subAgent : config.getSubAgents()) {
			FlowGraphBuildingStrategy.addSubAgentNode(subAgent, this.graph);
			edgeRoutingMap.put(subAgent.name(), subAgent.name());
			this.graph.addEdge(subAgent.name(), this.entryNode);
		}

		buildCoreGraphWithMainAgent(config, rootAgent, edgeRoutingMap);
	}

	/**
	 * Builds the supervisor graph: first node is mainAgent (at rootAgent.name()); conditional
	 * edge from mainAgent to SupervisorNode or END; SupervisorNode returns MultiCommand from
	 * mainAgent output and routes to subAgents in parallel.
	 */
	private void buildCoreGraphWithMainAgent(FlowGraphBuilder.FlowGraphConfig config,
			Agent rootAgent, Map<String, String> edgeRoutingMap) throws GraphStateException {
		String mainAgentNodeName = rootAgent.name();
		String supervisorNodeName = rootAgent.name() + "_supervisor";
		String routingKey = config.getCustomProperty("supervisor.routingKey") != null
				? String.valueOf(config.getCustomProperty("supervisor.routingKey"))
				: SupervisorNodeFromState.SUPERVISOR_NEXT_KEY;

		// SupervisorNode (add before edges that reference it)
		this.graph.addNode(supervisorNodeName, node_async(state -> Map.of()));

		// 2.1 Conditional edge from mainAgentNode to SupervisorNode or END
		MainAgentToSupervisorEdgeAction mainAgentToSupervisor = new MainAgentToSupervisorEdgeAction(routingKey, supervisorNodeName);
		Map<String, String> mainAgentEdgeMap = new HashMap<>();
		mainAgentEdgeMap.put(END, this.exitNode);
		mainAgentEdgeMap.put(supervisorNodeName, supervisorNodeName);
		this.graph.addConditionalEdges(mainAgentNodeName, mainAgentToSupervisor, mainAgentEdgeMap);

		// 2.2 Conditional edges from SupervisorNode to subAgents (MultiCommand from mainAgent output)
		SupervisorNodeFromState supervisorNodeFromState = new SupervisorNodeFromState(routingKey, config.getSubAgents(), this.entryNode);
		this.graph.addParallelConditionalEdges(supervisorNodeName,
				AsyncMultiCommandAction.node_async(supervisorNodeFromState),
				edgeRoutingMap);
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

		if (config.getMainAgent() == null) {
			throw new IllegalArgumentException("Supervisor flow requires mainAgent (ReactAgent)");
		}

		// Ensure root agent is a FlowAgent for input key access
		if (!(config.getRootAgent() instanceof FlowAgent)) {
			throw new IllegalArgumentException("Supervisor flow requires root agent to be a FlowAgent");
		}
	}

}

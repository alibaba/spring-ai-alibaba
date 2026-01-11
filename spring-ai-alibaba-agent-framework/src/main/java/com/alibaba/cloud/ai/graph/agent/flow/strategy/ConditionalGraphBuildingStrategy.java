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

import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.FlowAgent;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.enums.FlowAgentEnum;
import com.alibaba.cloud.ai.graph.agent.flow.node.ConditionEvaluator;
import com.alibaba.cloud.ai.graph.agent.flow.node.ConditionEvaluatorAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * Strategy for building conditional execution graphs. In a conditional graph, the
 * execution path is determined by evaluating conditions based on the current state, and
 * different agents are selected accordingly.
 */
public class ConditionalGraphBuildingStrategy extends AbstractFlowGraphBuildingStrategy {

	@Override
	protected void buildCoreGraph(FlowGraphBuilder.FlowGraphConfig config)
			throws GraphStateException {
		validateConditionalConfig(config);

		// Add beforeModel hooks
		String conditionSourceNode = this.rootAgent.name();
		if (!this.beforeModelHooks.isEmpty()) {
			conditionSourceNode = addBeforeModelHookNodesToGraph(this.graph, this.rootAgent.name(), this.beforeModelHooks);
		}

		// Add condition evaluator node
		String conditionNodeName = this.rootAgent.name() + "_condition";
		this.graph.addNode(conditionNodeName, node_async(new ConditionEvaluator()));
		this.graph.addEdge(conditionSourceNode, conditionNodeName);

		// Add afterModel hooks if present for condition node
		String routingSourceNode = conditionNodeName;
		if (!this.afterModelHooks.isEmpty()) {
			routingSourceNode = addAfterModelHookNodesToGraph(this.graph, conditionNodeName, this.afterModelHooks);
		}

		// Process conditional agents
		Map<String, String> conditionRoutingMap = new HashMap<>();
		for (Map.Entry<String, Agent> entry : config.getConditionalAgents().entrySet()) {
			String condition = entry.getKey();
			Agent subAgent = entry.getValue();

			FlowGraphBuildingStrategy.addSubAgentNode(subAgent, this.graph);

			conditionRoutingMap.put(condition, subAgent.name());

			// Connect agent to exit node
			this.graph.addEdge(subAgent.name(), this.exitNode);
		}

		// Add default exit node condition if no conditions match
		conditionRoutingMap.put("default", this.exitNode);

		// Connect routing source node to agents via conditional routing
		this.graph.addConditionalEdges(routingSourceNode, new ConditionEvaluatorAction(), conditionRoutingMap);
	}

	@Override
	public String getStrategyType() {
		return FlowAgentEnum.CONDITIONAL.getType();
	}

	@Override
	public void validateConfig(FlowGraphBuilder.FlowGraphConfig config) {
		super.validateConfig(config);
		validateConditionalConfig(config);
	}

	/**
	 * Validates conditional-specific configuration requirements.
	 * @param config the configuration to validate
	 * @throws IllegalArgumentException if validation fails
	 */
	private void validateConditionalConfig(FlowGraphBuilder.FlowGraphConfig config) {
		if (config.getConditionalAgents() == null || config.getConditionalAgents().isEmpty()) {
			throw new IllegalArgumentException("Conditional flow requires at least one conditional agent mapping");
		}

		// Ensure root agent is a FlowAgent for input key access
		if (!(config.getRootAgent() instanceof FlowAgent)) {
			throw new IllegalArgumentException("Conditional flow requires root agent to be a FlowAgent");
		}

		// Validate that all condition keys are non-empty
		for (String condition : config.getConditionalAgents().keySet()) {
			if (condition == null || condition.trim().isEmpty()) {
				throw new IllegalArgumentException("Condition keys cannot be null or empty");
			}
		}
	}

}

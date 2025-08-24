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
import com.alibaba.cloud.ai.graph.agent.flow.node.ConditionEvaluator;
import com.alibaba.cloud.ai.graph.agent.flow.node.ConditionEvaluatorAction;
import com.alibaba.cloud.ai.graph.agent.flow.node.TransparentNode;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * Strategy for building conditional execution graphs. In a conditional graph, the
 * execution path is determined by evaluating conditions based on the current state, and
 * different agents are selected accordingly.
 */
public class ConditionalGraphBuildingStrategy implements FlowGraphBuildingStrategy {

	@Override
	public StateGraph buildGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException {
		validateConfig(config);
		validateConditionalConfig(config);

		StateGraph graph = new StateGraph(config.getName(), config.getKeyStrategyFactory());
		BaseAgent rootAgent = config.getRootAgent();

		// Add root transparent node
		graph.addNode(rootAgent.name(),
				node_async(new TransparentNode(rootAgent.outputKey(), ((FlowAgent) rootAgent).inputKey())));

		// Add starting edge
		graph.addEdge(START, rootAgent.name());

		// Add condition evaluator node
		String conditionNodeName = rootAgent.name() + "_condition";
		graph.addNode(conditionNodeName, node_async(new ConditionEvaluator()));
		graph.addEdge(rootAgent.name(), conditionNodeName);

		// Process conditional agents
		Map<String, String> conditionRoutingMap = new HashMap<>();
		for (Map.Entry<String, BaseAgent> entry : config.getConditionalAgents().entrySet()) {
			String condition = entry.getKey();
			BaseAgent agent = entry.getValue();

			// Add the conditional agent as a node
			graph.addNode(agent.name(), agent.asAsyncNodeAction(rootAgent.outputKey(), agent.outputKey()));
			conditionRoutingMap.put(condition, agent.name());

			// Connect agent to END
			graph.addEdge(agent.name(), END);
		}

		// Add default END condition if no conditions match
		conditionRoutingMap.put("default", END);

		// Connect condition node to agents via conditional routing
		graph.addConditionalEdges(conditionNodeName, new ConditionEvaluatorAction(), conditionRoutingMap);

		return graph;
	}

	@Override
	public String getStrategyType() {
		return FlowAgentEnum.CONDITIONAL.getType();
	}

	@Override
	public void validateConfig(FlowGraphBuilder.FlowGraphConfig config) {
		FlowGraphBuildingStrategy.super.validateConfig(config);
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

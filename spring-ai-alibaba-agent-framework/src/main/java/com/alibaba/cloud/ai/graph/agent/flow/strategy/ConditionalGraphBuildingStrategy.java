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

import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.FlowAgent;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.enums.FlowAgentEnum;
import com.alibaba.cloud.ai.graph.agent.flow.node.ConditionEvaluator;
import com.alibaba.cloud.ai.graph.agent.flow.node.ConditionEvaluatorAction;
import com.alibaba.cloud.ai.graph.agent.flow.node.TransparentNode;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * Strategy for building conditional execution graphs. In a conditional graph, the
 * execution path is determined by evaluating conditions based on the current state, and
 * different agents are selected accordingly.
 */
public class ConditionalGraphBuildingStrategy extends AbstractFlowGraphBuildingStrategy {

	@Override
	protected void buildCoreGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException {
		validateConditionalConfig(config);

		Agent rootAgent = getRootAgent();

		// Add root transparent node as the conditional start point
		String conditionalStartNode = rootAgent.name();
		this.graph.addNode(conditionalStartNode, node_async(new TransparentNode()));

		// Connect beforeModel hooks to the conditional start node if they exist
		if (!this.beforeModelHooks.isEmpty()) {
			connectBeforeModelHookEdges(this.graph, conditionalStartNode, this.beforeModelHooks);
		}

		// Add condition evaluator node
		String conditionNodeName = rootAgent.name() + "_condition";
		this.graph.addNode(conditionNodeName, node_async(new ConditionEvaluator()));
		this.graph.addEdge(rootAgent.name(), conditionNodeName);

		// Build condition routing map for all branches
		Map<String, String> conditionRoutingMap = new HashMap<>();
		
		// Add all sub-agent nodes and register them in routing map
		for (Map.Entry<String, Agent> entry : config.getConditionalAgents().entrySet()) {
			String condition = entry.getKey();
			Agent subAgent = entry.getValue();
			FlowGraphBuildingStrategy.addSubAgentNode(subAgent, this.graph);
			conditionRoutingMap.put(condition, subAgent.name());
		}
		
		// IMPORTANT: Handle afterModel hooks here in buildCoreGraph instead of connectAfterModelHooks()
		// Reason: Conditional routing requires knowing the convergence point when setting up the routing map.
		// All branches must converge to the same point (first afterModel hook or exitNode) to ensure
		// hooks execute only once regardless of which branch is taken.
		String convergencePoint;
		if (!this.afterModelHooks.isEmpty()) {
			// Use the first afterModel hook as convergence point
			convergencePoint = Hook.getFullHookName(this.afterModelHooks.get(0)) + ".afterModel";
			
			// Chain afterModel hooks in sequence (only once for all branches)
			String prevHook = convergencePoint;
			for (int i = 1; i < this.afterModelHooks.size(); i++) {
				String currentHook = Hook.getFullHookName(this.afterModelHooks.get(i)) + ".afterModel";
				this.graph.addEdge(prevHook, currentHook);
				prevHook = currentHook;
			}
			
			// Connect last hook to exit node
			this.graph.addEdge(prevHook, this.exitNode);
		} else {
			convergencePoint = this.exitNode;
		}
		
		// Connect all branches to the convergence point
		for (Agent subAgent : config.getConditionalAgents().values()) {
			this.graph.addEdge(subAgent.name(), convergencePoint);
		}

		// Add default exit condition if no conditions match
		conditionRoutingMap.put("default", this.exitNode);

		// Connect condition node to agents via conditional routing
		this.graph.addConditionalEdges(conditionNodeName, new ConditionEvaluatorAction(), conditionRoutingMap);
	}

	@Override
	protected void connectBeforeModelHooks() throws GraphStateException {
		// Empty override: beforeModel hooks are already connected in buildCoreGraph
		// to ensure proper integration with conditional routing logic
	}

	@Override
	protected void connectAfterModelHooks() throws GraphStateException {
		// Empty override: afterModel hooks must be handled in buildCoreGraph because:
		// 1. Conditional routing needs to know the convergence point when setting up routes
		// 2. All branches must converge to the same hook chain (not separate chains per branch)
		// 3. Hooks should execute only once, not once per branch
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

/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.agent.agentscope.flow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.action.AsyncMultiCommandAction;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.node.RoutingMergeNode;
import com.alibaba.cloud.ai.graph.agent.flow.node.TransparentNode;
import com.alibaba.cloud.ai.graph.agent.flow.strategy.AbstractFlowGraphBuildingStrategy;
import com.alibaba.cloud.ai.graph.agent.flow.strategy.FlowGraphBuildingStrategy;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import io.agentscope.core.model.Model;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * Strategy for building routing graphs that use AgentScope {@link Model} instead of Spring AI
 * ChatModel. Reuses the same graph structure as the framework's {@link
 * com.alibaba.cloud.ai.graph.agent.flow.strategy.RoutingGraphBuildingStrategy} (root →
 * routing → parallel sub-agents → merge → exit), and reuses {@link TransparentNode}, {@link
 * FlowGraphBuildingStrategy#addSubAgentNode}, and key strategy logic; only the routing and merge
 * nodes are AgentScope Model-based.
 */
public class AgentScopeRoutingGraphBuildingStrategy extends AbstractFlowGraphBuildingStrategy {

	/** Strategy type for registration with {@link com.alibaba.cloud.ai.graph.agent.flow.strategy.FlowGraphBuildingStrategyRegistry}. */
	public static final String AGENT_SCOPE_ROUTING_TYPE = "AGENT_SCOPE_ROUTING";

	@Override
	protected void buildCoreGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException {
		validateAgentScopeRoutingConfig(config);

		Model model = (Model) config.getCustomProperty("agentScopeModel");
		AgentScopeRoutingAgent scopeRoutingAgent = (AgentScopeRoutingAgent) config.getRootAgent();
		String systemPrompt = scopeRoutingAgent.getSystemPrompt();

		// Reuse: root transparent node
		graph.addNode(rootAgent.name(), node_async(new TransparentNode()));

		String routingNodeName = rootAgent.name() + "_routing";
		graph.addNode(routingNodeName, node_async((state) -> Map.of()));

		String firstBeforeModelNode = routingNodeName;
		if (!beforeModelHooks.isEmpty()) {
			firstBeforeModelNode = connectBeforeModelHookEdges(graph, routingNodeName, beforeModelHooks);
		}
		graph.addEdge(rootAgent.name(), firstBeforeModelNode);

		String routingExitNode = routingNodeName;
		if (!afterModelHooks.isEmpty()) {
			routingExitNode = connectAfterModelHookEdges(graph, routingNodeName, afterModelHooks);
		}

		String mergeNodeName = rootAgent.name() + "_merge";
		List<BaseAgent> baseAgentList = new ArrayList<>(config.getSubAgents().size());
		for (Agent subAgent : config.getSubAgents()) {
			if (!(subAgent instanceof BaseAgent)) {
				throw new IllegalArgumentException("Routing sub-agents must be BaseAgent for merge support");
			}
			baseAgentList.add((BaseAgent) subAgent);
		}
		graph.addNode(mergeNodeName, node_async(new AgentScopeRoutingMergeNode(model, baseAgentList)));

		Map<String, String> edgeRoutingMap = new HashMap<>();
		for (Agent subAgent : config.getSubAgents()) {
			com.alibaba.cloud.ai.graph.agent.flow.strategy.FlowGraphBuildingStrategy.addSubAgentNode(subAgent, graph);
			edgeRoutingMap.put(subAgent.name(), subAgent.name());
			graph.addEdge(subAgent.name(), mergeNodeName);
		}
		graph.addEdge(mergeNodeName, this.exitNode);

		AgentScopeRoutingNode routingNode = new AgentScopeRoutingNode(model, rootAgent, config.getSubAgents(), systemPrompt);
		graph.addParallelConditionalEdges(
				routingExitNode,
				AsyncMultiCommandAction.node_async(routingNode),
				edgeRoutingMap);
	}

	@Override
	protected void connectBeforeModelHooks() throws GraphStateException {
	}

	@Override
	protected void connectAfterModelHooks() throws GraphStateException {
	}

	@Override
	public String getStrategyType() {
		return AGENT_SCOPE_ROUTING_TYPE;
	}

	@Override
	public KeyStrategyFactory generateKeyStrategyFactory(FlowGraphBuilder.FlowGraphConfig config) {
		KeyStrategyFactory parent = super.generateKeyStrategyFactory(config);
		return () -> {
			Map<String, com.alibaba.cloud.ai.graph.KeyStrategy> strategies = new HashMap<>(parent.apply());
			strategies.put(RoutingMergeNode.DEFAULT_MERGED_OUTPUT_KEY, new ReplaceStrategy());
			return strategies;
		};
	}

	@Override
	public void validateConfig(FlowGraphBuilder.FlowGraphConfig config) {
		super.validateConfig(config);
		validateAgentScopeRoutingConfig(config);
	}

	private void validateAgentScopeRoutingConfig(FlowGraphBuilder.FlowGraphConfig config) {
		if (config.getSubAgents() == null || config.getSubAgents().isEmpty()) {
			throw new IllegalArgumentException("AgentScope routing flow requires at least one sub-agent");
		}
		if (config.getCustomProperty("agentScopeModel") == null) {
			throw new IllegalArgumentException("AgentScope routing flow requires agentScopeModel in config custom properties");
		}
		if (!(config.getRootAgent() instanceof AgentScopeRoutingAgent)) {
			throw new IllegalArgumentException("AgentScope routing flow requires root agent to be AgentScopeRoutingAgent");
		}
	}
}

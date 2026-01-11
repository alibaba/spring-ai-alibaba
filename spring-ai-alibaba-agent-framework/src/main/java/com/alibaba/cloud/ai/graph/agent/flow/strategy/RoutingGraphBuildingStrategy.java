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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.FlowAgent;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.enums.FlowAgentEnum;
import com.alibaba.cloud.ai.graph.agent.flow.node.RoutingNode;
import com.alibaba.cloud.ai.graph.agent.flow.node.TransparentNode;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesAgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * Strategy for building LLM-based routing graphs. In a routing graph, an LLM decides
 * which sub-agent should handle the task based on the input content and agent
 * capabilities.
 */
public class RoutingGraphBuildingStrategy implements FlowGraphBuildingStrategy {

	@Override
	public StateGraph buildGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException {
		validateConfig(config);
		validateRoutingConfig(config);

		StateGraph graph = config.getStateSerializer() != null
				? new StateGraph(config.getName(), config.getKeyStrategyFactory(), config.getStateSerializer())
				: new StateGraph(config.getName(), config.getKeyStrategyFactory());
		Agent rootAgent = config.getRootAgent();

		// Get hooks from config
		List<Hook> hooks = config.getHooks() != null ? config.getHooks() : new ArrayList<>();

		// Initialize hooks
		Set<String> hookNames = new HashSet<>();
		for (Hook hook : hooks) {
			if (!hookNames.add(Hook.getFullHookName(hook))) {
				throw new IllegalArgumentException("Duplicate hook instances found");
			}
			hook.setAgentName(rootAgent.name());
			// Note: We don't set hook.setAgent() here because rootAgent is not a ReactAgent
		}

		// Categorize hooks by position
		List<Hook> beforeAgentHooks = filterHooksByPosition(hooks, HookPosition.BEFORE_AGENT);
		List<Hook> afterAgentHooks = filterHooksByPosition(hooks, HookPosition.AFTER_AGENT);
		List<Hook> beforeModelHooks = filterHooksByPosition(hooks, HookPosition.BEFORE_MODEL);
		List<Hook> afterModelHooks = filterHooksByPosition(hooks, HookPosition.AFTER_MODEL);

		// Add hook nodes for beforeAgent hooks
		for (Hook hook : beforeAgentHooks) {
			if (hook instanceof AgentHook agentHook) {
				graph.addNode(Hook.getFullHookName(hook) + ".before", agentHook::beforeAgent);
			} else if (hook instanceof MessagesAgentHook messagesAgentHook) {
				graph.addNode(Hook.getFullHookName(hook) + ".before", MessagesAgentHook.beforeAgentAction(messagesAgentHook));
			}
		}

		// Add hook nodes for afterAgent hooks
		for (Hook hook : afterAgentHooks) {
			if (hook instanceof AgentHook agentHook) {
				graph.addNode(Hook.getFullHookName(hook) + ".after", agentHook::afterAgent);
			} else if (hook instanceof MessagesAgentHook messagesAgentHook) {
				graph.addNode(Hook.getFullHookName(hook) + ".after", MessagesAgentHook.afterAgentAction(messagesAgentHook));
			}
		}

		// Add hook nodes for beforeModel hooks
		for (Hook hook : beforeModelHooks) {
			if (hook instanceof ModelHook modelHook) {
				graph.addNode(Hook.getFullHookName(hook) + ".beforeModel", modelHook::beforeModel);
			} else if (hook instanceof MessagesModelHook messagesModelHook) {
				graph.addNode(Hook.getFullHookName(hook) + ".beforeModel", MessagesModelHook.beforeModelAction(messagesModelHook));
			}
		}

		// Add hook nodes for afterModel hooks
		for (Hook hook : afterModelHooks) {
			if (hook instanceof ModelHook modelHook) {
				graph.addNode(Hook.getFullHookName(hook) + ".afterModel", modelHook::afterModel);
			} else if (hook instanceof MessagesModelHook messagesModelHook) {
				graph.addNode(Hook.getFullHookName(hook) + ".afterModel", MessagesModelHook.afterModelAction(messagesModelHook));
			}
		}

		// Add root transparent node (entry point)
		graph.addNode(rootAgent.name(), node_async(new TransparentNode()));

		// Add routing node (where LLM makes routing decision)
		String routingNodeName = rootAgent.name() + "_routing";
		graph.addNode(routingNodeName, new RoutingNode(config.getChatModel(), rootAgent, config.getSubAgents()));

		// Determine node flow
		String entryNode = determineEntryNode(rootAgent.name(), beforeAgentHooks, beforeModelHooks);
		String routingEntryNode = determineRoutingEntryNode(routingNodeName, beforeModelHooks);
		String routingExitNode = determineRoutingExitNode(routingNodeName, afterModelHooks);
		String exitNode = determineExitNode(afterAgentHooks);

		// Add starting edge
		graph.addEdge(START, entryNode);

		// Setup hook edges
		setupHookEdges(graph, rootAgent.name(), routingNodeName, beforeAgentHooks, afterAgentHooks, 
					   beforeModelHooks, afterModelHooks, entryNode, routingEntryNode, routingExitNode, exitNode);

		// Process sub-agents for routing
		Map<String, String> edgeRoutingMap = new HashMap<>();
		for (Agent subAgent : config.getSubAgents()) {
			// Add the current sub-agent as a node
			FlowGraphBuildingStrategy.addSubAgentNode(subAgent, graph);
			edgeRoutingMap.put(subAgent.name(), subAgent.name());
			// Connect sub-agents to END
			graph.addEdge(subAgent.name(), END);
		}

		// Connect routing exit to sub-agents via conditional routing
		// The routing decision is stored in state by RoutingNode
		AsyncEdgeAction routingDecisionAction = state -> {
			String decision = (String) state.value(RoutingNode.getRoutingDecisionKey()).orElse(null);
			if (decision == null) {
				throw new IllegalStateException("Routing decision not found in state");
			}
			return java.util.concurrent.CompletableFuture.completedFuture(decision);
		};
		graph.addConditionalEdges(routingExitNode, routingDecisionAction, edgeRoutingMap);

		return graph;
	}

	/**
	 * Filter hooks by their position
	 */
	private static List<Hook> filterHooksByPosition(List<Hook> hooks, HookPosition position) {
		return hooks.stream()
				.filter(hook -> {
					HookPosition[] positions = hook.getHookPositions();
					return Arrays.asList(positions).contains(position);
				})
				.toList();
	}

	/**
	 * Determine the entry node based on hooks
	 */
	private static String determineEntryNode(String rootAgentName, List<Hook> agentHooks, List<Hook> modelHooks) {
		if (!agentHooks.isEmpty()) {
			return Hook.getFullHookName(agentHooks.get(0)) + ".before";
		} else if (!modelHooks.isEmpty()) {
			return Hook.getFullHookName(modelHooks.get(0)) + ".beforeModel";
		} else {
			return rootAgentName;
		}
	}

	/**
	 * Determine the routing entry node (where routing decision starts)
	 */
	private static String determineRoutingEntryNode(String routingNodeName, List<Hook> modelHooks) {
		if (!modelHooks.isEmpty()) {
			return Hook.getFullHookName(modelHooks.get(0)) + ".beforeModel";
		} else {
			return routingNodeName;
		}
	}

	/**
	 * Determine the routing exit node (after routing decision and after-model hooks)
	 */
	private static String determineRoutingExitNode(String routingNodeName, List<Hook> modelHooks) {
		if (!modelHooks.isEmpty()) {
			return Hook.getFullHookName(modelHooks.get(modelHooks.size() - 1)) + ".afterModel";
		} else {
			return routingNodeName;
		}
	}

	/**
	 * Determine the exit node
	 */
	private static String determineExitNode(List<Hook> agentHooks) {
		if (!agentHooks.isEmpty()) {
			return Hook.getFullHookName(agentHooks.get(agentHooks.size() - 1)) + ".after";
		} else {
			return END;
		}
	}

	/**
	 * Setup edges between hooks and routing node
	 */
	private static void setupHookEdges(
			StateGraph graph,
			String rootAgentName,
			String routingNodeName,
			List<Hook> beforeAgentHooks,
			List<Hook> afterAgentHooks,
			List<Hook> beforeModelHooks,
			List<Hook> afterModelHooks,
			String entryNode,
			String routingEntryNode,
			String routingExitNode,
			String exitNode) throws GraphStateException {

		// Connect beforeAgent hooks
		if (!beforeAgentHooks.isEmpty()) {
			for (int i = 0; i < beforeAgentHooks.size() - 1; i++) {
				String currentHook = Hook.getFullHookName(beforeAgentHooks.get(i)) + ".before";
				String nextHook = Hook.getFullHookName(beforeAgentHooks.get(i + 1)) + ".before";
				graph.addEdge(currentHook, nextHook);
			}
			// Last beforeAgent hook to root agent transparent node
			String lastBeforeAgentHook = Hook.getFullHookName(beforeAgentHooks.get(beforeAgentHooks.size() - 1)) + ".before";
			graph.addEdge(lastBeforeAgentHook, rootAgentName);
		}

		// Connect root agent transparent node to routing entry
		graph.addEdge(rootAgentName, routingEntryNode);

		// Connect beforeModel hooks
		if (!beforeModelHooks.isEmpty()) {
			for (int i = 0; i < beforeModelHooks.size() - 1; i++) {
				String currentHook = Hook.getFullHookName(beforeModelHooks.get(i)) + ".beforeModel";
				String nextHook = Hook.getFullHookName(beforeModelHooks.get(i + 1)) + ".beforeModel";
				graph.addEdge(currentHook, nextHook);
			}
			// Last beforeModel hook to routing node
			String lastBeforeModelHook = Hook.getFullHookName(beforeModelHooks.get(beforeModelHooks.size() - 1)) + ".beforeModel";
			graph.addEdge(lastBeforeModelHook, routingNodeName);
		}

		// Connect routing node to after-model hooks (or routing exit)
		if (!afterModelHooks.isEmpty()) {
			graph.addEdge(routingNodeName, Hook.getFullHookName(afterModelHooks.get(0)) + ".afterModel");

			// Connect afterModel hooks
			for (int i = 0; i < afterModelHooks.size() - 1; i++) {
				String currentHook = Hook.getFullHookName(afterModelHooks.get(i)) + ".afterModel";
				String nextHook = Hook.getFullHookName(afterModelHooks.get(i + 1)) + ".afterModel";
				graph.addEdge(currentHook, nextHook);
			}
		}

		// Note: The routingExitNode connects to sub-agents via conditional edges in buildGraph()
		// afterAgent hooks are not used in routing flow since we go directly to sub-agents
	}

	@Override
	public String getStrategyType() {
		return FlowAgentEnum.ROUTING.getType();
	}

	@Override
	public void validateConfig(FlowGraphBuilder.FlowGraphConfig config) {
		FlowGraphBuildingStrategy.super.validateConfig(config);
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

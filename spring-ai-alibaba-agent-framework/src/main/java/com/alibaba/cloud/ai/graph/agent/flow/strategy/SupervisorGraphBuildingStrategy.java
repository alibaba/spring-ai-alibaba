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

import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.Prioritized;
import com.alibaba.cloud.ai.graph.agent.flow.agent.FlowAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SupervisorAgent;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.enums.FlowAgentEnum;
import com.alibaba.cloud.ai.graph.agent.flow.node.SupervisorEdgeAction;
import com.alibaba.cloud.ai.graph.agent.flow.node.TransparentNode;
import com.alibaba.cloud.ai.graph.agent.hook.*;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesAgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import java.util.*;
import java.util.stream.Collectors;

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
		SupervisorAgent rootAgent = (SupervisorAgent) config.getRootAgent();

		// Filter hooks by position from config
		List<Hook> beforeAgentHooks = filterHooksByPosition(config.getHooks(), HookPosition.BEFORE_AGENT);
		List<Hook> afterAgentHooks = filterHooksByPosition(config.getHooks(), HookPosition.AFTER_AGENT);
		List<Hook> beforeModelHooks = filterHooksByPosition(config.getHooks(), HookPosition.BEFORE_MODEL);
		List<Hook> afterModelHooks = filterHooksByPosition(config.getHooks(), HookPosition.AFTER_MODEL);

		// Fallback to rootAgent with TransparentNode
		graph.addNode(rootAgent.name(), node_async(new TransparentNode()));

		// Add beforeAgent hook nodes to graph
		addBeforeAgentHookNodesToGraph(graph, beforeAgentHooks);

		// Add beforeModel hook nodes to graph and get the first node name
		String firstNodeName = addBeforeModelHookNodesToGraph(graph, rootAgent, beforeModelHooks, beforeAgentHooks);

		// Add starting edge - connect to the first node (could be hook or rootAgent)
		graph.addEdge(START, firstNodeName);

		// Add afterModel hook nodes and get the routing target
		String afterModelNodeName = addAfterModelHookNodesToGraph(graph, rootAgent, afterModelHooks);

		// Add afterAgent hook nodes to graph
		addAfterAgentHookNodesToGraph(graph, afterAgentHooks);

		// Determine the exit node (could be afterAgent hook or END)
		String exitNode = determineExitNode(afterAgentHooks);

		// Process sub-agents for routing
		Map<String, String> edgeRoutingMap = new HashMap<>();
		for (Agent subAgent : config.getSubAgents()) {
			FlowGraphBuildingStrategy.addSubAgentNode(subAgent, graph);
			edgeRoutingMap.put(subAgent.name(), subAgent.name());
			// Sub-agent returns to the first node (could be beforeAgent/beforeModel hook)
			graph.addEdge(subAgent.name(), firstNodeName);
		}

		// Add END as a possible routing destination
		edgeRoutingMap.put(END, END);

		// Connect supervisor to routing logic
		String routingSourceNode = afterModelHooks.isEmpty() ? rootAgent.name() : afterModelNodeName;
		graph.addConditionalEdges(routingSourceNode,
				new SupervisorEdgeAction(config.getChatModel(), rootAgent, config.getSubAgents()), edgeRoutingMap);

		// Connect afterAgent hooks if present
		if (!afterAgentHooks.isEmpty()) {
			// Chain afterAgent hooks in sequence and connect to END
			chainAfterAgentHooks(graph, afterAgentHooks);
		}

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
	 * Adds beforeAgent hook nodes to the graph.
	 * @param graph the state graph to add nodes to
	 * @param beforeAgentHooks the list of beforeAgent hooks
	 * @throws GraphStateException if graph construction fails
	 */
	private void addBeforeAgentHookNodesToGraph(StateGraph graph, List<Hook> beforeAgentHooks) throws GraphStateException {
		for (Hook hook : beforeAgentHooks) {
			String hookNodeName = hook.getName() + ".before";

			if (hook instanceof AgentHook agentHook) {
				graph.addNode(hookNodeName, agentHook::beforeAgent);
			} else if (hook instanceof MessagesAgentHook messagesAgentHook) {
				graph.addNode(hookNodeName, MessagesAgentHook.beforeAgentAction(messagesAgentHook));
			}
		}
	}

	/**
	 * Adds beforeModel hook nodes to the graph and connects them in sequence.
	 * Returns the name of the first node (either the first hook or the supervisor node).
	 * @param graph the state graph to add nodes to
	 * @param supervisorNode the supervisor node
	 * @param beforeModelHooks the list of beforeModel hooks
	 * @param beforeAgentHooks the list of beforeAgent hooks
	 * @return the name of the first node in the execution chain
	 * @throws GraphStateException if graph construction fails
	 */
	private String addBeforeModelHookNodesToGraph(StateGraph graph, Agent supervisorNode, List<Hook> beforeModelHooks, List<Hook> beforeAgentHooks) throws GraphStateException {
		String firstNodeName = supervisorNode.name();

		// Determine the entry point based on hooks
		if (!beforeAgentHooks.isEmpty()) {
			firstNodeName = beforeAgentHooks.get(0).getName() + ".before";
			// Chain beforeAgent hooks
			chainBeforeAgentHooks(graph, beforeAgentHooks, beforeModelHooks, supervisorNode);
		} else if (!beforeModelHooks.isEmpty()) {
			firstNodeName = beforeModelHooks.get(0).getName() + ".beforeModel";
		}

		// Add and chain beforeModel hooks
		if (!beforeModelHooks.isEmpty()) {
			String prevHookNodeName = null;
			for (Hook hook : beforeModelHooks) {
				String hookNodeName = hook.getName() + ".beforeModel";

				if (hook instanceof ModelHook modelHook) {
					if (hook instanceof InterruptionHook interruptionHook) {
						graph.addNode(Hook.getFullHookName(hook) + ".beforeModel", interruptionHook);
					} else {
						graph.addNode(Hook.getFullHookName(hook) + ".beforeModel", modelHook::beforeModel);
					}
				} else if (hook instanceof MessagesModelHook messagesModelHook) {
					graph.addNode(Hook.getFullHookName(hook) + ".beforeModel", MessagesModelHook.beforeModelAction(messagesModelHook));
				}

				// Connect hook nodes
				if (prevHookNodeName != null) {
					graph.addEdge(prevHookNodeName, hookNodeName);
				}
				prevHookNodeName = hookNodeName;
			}

			// Connect the last beforeModel hook to the supervisor node
			graph.addEdge(prevHookNodeName, supervisorNode.name());
		}

		return firstNodeName;
	}

	/**
	 * Chains beforeAgent hooks in sequence and connects to beforeModel hooks or supervisor.
	 * @param graph the state graph
	 * @param beforeAgentHooks the list of beforeAgent hooks
	 * @param beforeModelHooks the list of beforeModel hooks
	 * @param supervisorNode the supervisor node
	 * @throws GraphStateException if graph construction fails
	 */
	private void chainBeforeAgentHooks(StateGraph graph, List<Hook> beforeAgentHooks, List<Hook> beforeModelHooks, Agent supervisorNode) throws GraphStateException {
		String nextNode;
		if (!beforeModelHooks.isEmpty()) {
			nextNode = beforeModelHooks.get(0).getName() + ".beforeModel";
		} else {
			nextNode = supervisorNode.name();
		}

		for (int i = 0; i < beforeAgentHooks.size(); i++) {
			Hook hook = beforeAgentHooks.get(i);
			String hookNodeName = hook.getName() + ".before";

			if (i < beforeAgentHooks.size() - 1) {
				// Connect to next beforeAgent hook
				String nextHookName = beforeAgentHooks.get(i + 1).getName() + ".before";
				graph.addEdge(hookNodeName, nextHookName);
			} else {
				// Last beforeAgent hook connects to beforeModel or supervisor
				graph.addEdge(hookNodeName, nextNode);
			}
		}
	}

	/**
	 * Adds afterAgent hook nodes to the graph.
	 * @param graph the state graph to add nodes to
	 * @param afterAgentHooks the list of afterAgent hooks
	 * @throws GraphStateException if graph construction fails
	 */
	private void addAfterAgentHookNodesToGraph(StateGraph graph, List<Hook> afterAgentHooks) throws GraphStateException {
		for (Hook hook : afterAgentHooks) {
			String hookNodeName = hook.getName() + ".after";

			if (hook instanceof AgentHook agentHook) {
				graph.addNode(hookNodeName, agentHook::afterAgent);
			} else if (hook instanceof MessagesAgentHook messagesAgentHook) {
				graph.addNode(hookNodeName, MessagesAgentHook.afterAgentAction(messagesAgentHook));
			}
		}
	}

	/**
	 * Adds afterModel hook nodes to the graph and connects them in sequence.
	 * Returns the name of the routing target node.
	 * @param graph the state graph to add nodes to
	 * @param supervisorNode the supervisor node
	 * @param afterModelHooks the list of afterModel hooks
	 * @return the name of the routing target node
	 * @throws GraphStateException if graph construction fails
	 */
	private String addAfterModelHookNodesToGraph(StateGraph graph, Agent supervisorNode, List<Hook> afterModelHooks) throws GraphStateException {
		if (afterModelHooks.isEmpty()) {
			return supervisorNode.name();
		}

		// Create a node for each afterModel hook
		String prevHookNodeName = null;
		String lastHookNodeName = null;
		for (Hook hook : afterModelHooks) {
			String hookNodeName = hook.getName() + ".afterModel";

			if (hook instanceof ModelHook modelHook) {
				if (hook instanceof InterruptionHook interruptionHook) {
					graph.addNode(Hook.getFullHookName(hook) + ".beforeModel", interruptionHook);
				} else {
					graph.addNode(Hook.getFullHookName(hook) + ".beforeModel", modelHook::beforeModel);
				}
			} else if (hook instanceof MessagesModelHook messagesModelHook) {
				graph.addNode(Hook.getFullHookName(hook) + ".beforeModel", MessagesModelHook.beforeModelAction(messagesModelHook));
			}

			// Connect hook nodes
			if (prevHookNodeName == null) {
				// Connect supervisor to the first afterModel hook
				graph.addEdge(supervisorNode.name(), hookNodeName);
			} else {
				graph.addEdge(prevHookNodeName, hookNodeName);
			}
			prevHookNodeName = hookNodeName;
			lastHookNodeName = hookNodeName;
		}

		// Return the last afterModel hook node name
		return lastHookNodeName;
	}

	/**
	 * Chains afterAgent hooks in reverse order (from last to first) and connects to END.
	 * @param graph the state graph
	 * @param afterAgentHooks the list of afterAgent hooks
	 * @throws GraphStateException if graph construction fails
	 */
	private void chainAfterAgentHooks(StateGraph graph, List<Hook> afterAgentHooks) throws GraphStateException {
		if (afterAgentHooks.isEmpty()) {
			return;
		}

		// Connect first afterAgent hook to END
		String firstHookName = afterAgentHooks.get(0).getName() + ".after";
		graph.addEdge(firstHookName, END);

		// Chain remaining hooks in reverse order
		for (int i = afterAgentHooks.size() - 1; i > 0; i--) {
			Hook currentHook = afterAgentHooks.get(i);
			Hook prevHook = afterAgentHooks.get(i - 1);
			String currentHookName = currentHook.getName() + ".after";
			String prevHookName = prevHook.getName() + ".after";
			graph.addEdge(currentHookName, prevHookName);
		}
	}

	/**
	 * Determines the exit node based on afterAgent hooks.
	 * @param afterAgentHooks the list of afterAgent hooks
	 * @return the name of the exit node
	 */
	private String determineExitNode(List<Hook> afterAgentHooks) {
		if (!afterAgentHooks.isEmpty()) {
			return afterAgentHooks.get(afterAgentHooks.size() - 1).getName() + ".after";
		}
		return END;
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

	/**
	 * Filter hooks by their position based on @HookPositions annotation.
	 * A hook will be included if its getHookPositions() contains the specified position.
	 * If a hook implements Prioritized interface, it will be sorted by its order.
	 * Hooks that don't implement Prioritized will maintain their original order.
	 *
	 * @param hooks the list of hooks to filter
	 * @param position the position to filter by
	 * @return list of hooks that should execute at the specified position
	 */
	private static List<Hook> filterHooksByPosition(List<? extends Hook> hooks, HookPosition position) {
		List<Hook> filtered = hooks.stream()
				.filter(hook -> {
					HookPosition[] positions = hook.getHookPositions();
					return Arrays.asList(positions).contains(position);
				})
				.collect(Collectors.toList());

		// Separate hooks that implement Prioritized from those that don't
		List<Hook> prioritizedHooks = new ArrayList<>();
		List<Hook> nonPrioritizedHooks = new ArrayList<>();

		for (Hook hook : filtered) {
			if (hook instanceof Prioritized) {
				prioritizedHooks.add(hook);
			} else {
				nonPrioritizedHooks.add(hook);
			}
		}

		// Sort prioritized hooks by their order
		prioritizedHooks.sort((h1, h2) -> Integer.compare(
				((Prioritized) h1).getOrder(),
				((Prioritized) h2).getOrder()));

		// Combine: prioritized hooks first (sorted), then non-prioritized hooks (original order)
		List<Hook> result = new ArrayList<>(prioritizedHooks);
		result.addAll(nonPrioritizedHooks);

		return result;
	}
}


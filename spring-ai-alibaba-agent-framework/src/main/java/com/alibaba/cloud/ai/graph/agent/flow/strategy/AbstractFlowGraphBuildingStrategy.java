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
import com.alibaba.cloud.ai.graph.agent.Prioritized;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.node.TransparentNode;
import com.alibaba.cloud.ai.graph.agent.hook.*;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesAgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * Abstract base class for FlowGraphBuildingStrategy implementations.
 * Provides common Hook processing logic and template method pattern to eliminate
 * boilerplate code across all strategy types.
 *
 * <p>
 * This class uses the Template Method pattern: the {@link #buildGraph(FlowGraphBuilder.FlowGraphConfig)}
 * method handles all common setup (graph creation, hook filtering, node addition, etc.),
 * while subclasses only need to implement {@link #buildCoreGraph(FlowGraphBuilder.FlowGraphConfig)} to define
 * their specific graph structure.
 * </p>
 *
 * @author panhaojun
 * @since 2025-01-07
 */
public abstract class AbstractFlowGraphBuildingStrategy implements FlowGraphBuildingStrategy {

	/**
	 * The state graph being built. Accessible by subclasses.
	 */
	protected StateGraph graph;

	/**
	 * The root agent of the flow. Accessible by subclasses.
	 */
	protected Agent rootAgent;

	/**
	 * Hooks that execute before the agent. Accessible by subclasses.
	 */
	protected List<Hook> beforeAgentHooks;

	/**
	 * Hooks that execute after the agent. Accessible by subclasses.
	 */
	protected List<Hook> afterAgentHooks;

	/**
	 * Hooks that execute before the model. Accessible by subclasses.
	 */
	protected List<Hook> beforeModelHooks;

	/**
	 * Hooks that execute after the model. Accessible by subclasses.
	 */
	protected List<Hook> afterModelHooks;

	/**
	 * The entry node name of the graph. Accessible by subclasses.
	 */
	protected String entryNode;

	/**
	 * The exit node name of the graph. Accessible by subclasses.
	 */
	protected String exitNode;

	/**
	 * Template method that handles all common graph building logic.
	 * Subclasses should NOT override this method. Instead, implement {@link #buildCoreGraph(FlowGraphBuilder.FlowGraphConfig)}.
	 *
	 * @param config the flow graph configuration
	 * @return the constructed state graph
	 * @throws GraphStateException if graph construction fails
	 */
	@Override
	public final StateGraph buildGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException {
		// Step 1: Create StateGraph
		this.graph = config.getStateSerializer() != null
				? new StateGraph(config.getName(), config.getKeyStrategyFactory(), config.getStateSerializer())
				: new StateGraph(config.getName(), config.getKeyStrategyFactory());

		this.rootAgent = config.getRootAgent();

		// Step 2: Filter hooks by position
		this.beforeAgentHooks = filterHooksByPosition(config.getHooks(), HookPosition.BEFORE_AGENT);
		this.afterAgentHooks = filterHooksByPosition(config.getHooks(), HookPosition.AFTER_AGENT);
		this.beforeModelHooks = filterHooksByPosition(config.getHooks(), HookPosition.BEFORE_MODEL);
		this.afterModelHooks = filterHooksByPosition(config.getHooks(), HookPosition.AFTER_MODEL);

		// Step 3: Add root transparent node
		this.graph.addNode(this.rootAgent.name(), node_async(new TransparentNode()));

		// Step 4: Add hook nodes
		addBeforeAgentHookNodesToGraph(this.graph, this.beforeAgentHooks);
		addAfterAgentHookNodesToGraph(this.graph, this.afterAgentHooks);

		// Step 5: Determine entry and exit nodes
		// Allow subclasses to customize entry/exit node determination
		this.entryNode = determineEntryNodeForGraph();
		this.exitNode = determineExitNodeForGraph();

		// Step 6: Add starting edge
		this.graph.addEdge(START, this.entryNode);

		// Step 7: Chain beforeAgent hooks if present
		if (!this.beforeAgentHooks.isEmpty()) {
			String nextNode = determineNextNodeAfterBeforeAgentHooks();
			chainBeforeAgentHooks(this.graph, this.beforeAgentHooks, nextNode);
		}

		// Step 8: Delegate to subclass for core graph building
		// Subclasses can choose to handle model hooks themselves by overriding shouldAutoHandleModelHooks()
		buildCoreGraph(config);

		// Step 9: Chain afterAgent hooks if present
		if (!this.afterAgentHooks.isEmpty()) {
			chainAfterAgentHooks(this.graph, this.afterAgentHooks);
		}

		return this.graph;
	}

	/**
	 * Determines the next node to connect after beforeAgent hooks.
	 * Subclasses can override this to customize the connection behavior.
	 * <p>
	 * Default behavior: connects to the first beforeModel hook if present, otherwise to rootAgent.
	 * <p>
	 * For routing strategies, this might return rootAgent name directly since model hooks
	 * are handled around the RoutingNode instead.
	 *
	 * @return the name of the next node to connect to
	 */
	protected String determineNextNodeAfterBeforeAgentHooks() {
		if (!this.beforeModelHooks.isEmpty()) {
			return Hook.getFullHookName(this.beforeModelHooks.get(0)) + ".beforeModel";
		}
		return this.rootAgent.name();
	}

	/**
	 * Determines the entry node for the entire graph.
	 * Subclasses can override this to customize entry node determination.
	 * <p>
	 * Default behavior: uses beforeAgent hooks if present, otherwise beforeModel hooks, 
	 * otherwise rootAgent.
	 * <p>
	 * For routing strategies that handle model hooks differently, this can be overridden
	 * to exclude beforeModel hooks from entry node consideration.
	 *
	 * @return the name of the entry node
	 */
	protected String determineEntryNodeForGraph() {
		return determineEntryNode(this.rootAgent, this.beforeAgentHooks, this.beforeModelHooks);
	}

	/**
	 * Determines the exit node for the entire graph.
	 * Subclasses can override this to customize exit node determination.
	 * <p>
	 * Default behavior: uses the last afterAgent hook if present, otherwise END.
	 *
	 * @return the name of the exit node
	 */
	protected String determineExitNodeForGraph() {
		return determineExitNode(this.afterAgentHooks);
	}

	/**
	 * Abstract method that subclasses must implement to build their specific graph structure.
	 * All common setup (graph creation, hook filtering, node addition, etc.) is already done.
	 * Subclasses can directly access protected fields: graph, rootAgent, beforeModelHooks,
	 * afterModelHooks, entryNode, exitNode.
	 *
	 * @param config the flow graph configuration
	 * @throws GraphStateException if graph construction fails
	 */
	protected abstract void buildCoreGraph(FlowGraphBuilder.FlowGraphConfig config)
			throws GraphStateException;

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
	protected static List<Hook> filterHooksByPosition(List<? extends Hook> hooks, HookPosition position) {
		if (hooks == null || hooks.isEmpty()) {
			return new ArrayList<>();
		}

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

	/**
	 * Adds beforeAgent hook nodes to the graph.
	 *
	 * @param graph the state graph to add nodes to
	 * @param beforeAgentHooks the list of beforeAgent hooks
	 * @throws GraphStateException if graph construction fails
	 */
	protected void addBeforeAgentHookNodesToGraph(StateGraph graph, List<Hook> beforeAgentHooks)
			throws GraphStateException {
		for (Hook hook : beforeAgentHooks) {
			String hookNodeName = Hook.getFullHookName(hook) + ".before";

			if (hook instanceof AgentHook agentHook) {
				graph.addNode(hookNodeName, agentHook::beforeAgent);
			} else if (hook instanceof MessagesAgentHook messagesAgentHook) {
				graph.addNode(hookNodeName, MessagesAgentHook.beforeAgentAction(messagesAgentHook));
			}
		}
	}

	/**
	 * Adds afterAgent hook nodes to the graph.
	 *
	 * @param graph the state graph to add nodes to
	 * @param afterAgentHooks the list of afterAgent hooks
	 * @throws GraphStateException if graph construction fails
	 */
	protected void addAfterAgentHookNodesToGraph(StateGraph graph, List<Hook> afterAgentHooks)
			throws GraphStateException {
		for (Hook hook : afterAgentHooks) {
			String hookNodeName = Hook.getFullHookName(hook) + ".after";

			if (hook instanceof AgentHook agentHook) {
				graph.addNode(hookNodeName, agentHook::afterAgent);
			} else if (hook instanceof MessagesAgentHook messagesAgentHook) {
				graph.addNode(hookNodeName, MessagesAgentHook.afterAgentAction(messagesAgentHook));
			}
		}
	}

	/**
	 * Adds beforeModel hook nodes to the graph and connects them in sequence.
	 * Returns the name of the first node in the chain.
	 *
	 * @param graph the state graph to add nodes to
	 * @param defaultFirstNode the default first node if no beforeModel hooks exist
	 * @param beforeModelHooks the list of beforeModel hooks
	 * @return the name of the first node in the execution chain
	 * @throws GraphStateException if graph construction fails
	 */
	protected String addBeforeModelHookNodesToGraph(StateGraph graph, String defaultFirstNode,
			List<Hook> beforeModelHooks) throws GraphStateException {
		if (beforeModelHooks.isEmpty()) {
			return defaultFirstNode;
		}

		String firstNodeName = Hook.getFullHookName(beforeModelHooks.get(0)) + ".beforeModel";
		String prevHookNodeName = null;

		for (Hook hook : beforeModelHooks) {
			String hookNodeName = Hook.getFullHookName(hook) + ".beforeModel";

			if (hook instanceof ModelHook modelHook) {
				if (hook instanceof InterruptionHook interruptionHook) {
					graph.addNode(hookNodeName, interruptionHook);
				} else {
					graph.addNode(hookNodeName, modelHook::beforeModel);
				}
			} else if (hook instanceof MessagesModelHook messagesModelHook) {
				graph.addNode(hookNodeName,
						MessagesModelHook.beforeModelAction(messagesModelHook));
			}

			// Connect hook nodes
			if (prevHookNodeName != null) {
				graph.addEdge(prevHookNodeName, hookNodeName);
			}
			prevHookNodeName = hookNodeName;
		}

		// Connect the last beforeModel hook to the default node
		graph.addEdge(prevHookNodeName, defaultFirstNode);

		return firstNodeName;
	}

	/**
	 * Adds afterModel hook nodes to the graph and connects them in sequence.
	 * Returns the name of the last node in the chain.
	 *
	 * @param graph the state graph to add nodes to
	 * @param sourceNode the source node to connect from
	 * @param afterModelHooks the list of afterModel hooks
	 * @return the name of the last node in the chain
	 * @throws GraphStateException if graph construction fails
	 */
	protected String addAfterModelHookNodesToGraph(StateGraph graph, String sourceNode,
			List<Hook> afterModelHooks) throws GraphStateException {
		if (afterModelHooks.isEmpty()) {
			return sourceNode;
		}

		String prevHookNodeName = null;
		String lastHookNodeName = null;

		for (Hook hook : afterModelHooks) {
			String hookNodeName = Hook.getFullHookName(hook) + ".afterModel";

			if (hook instanceof ModelHook modelHook) {
				if (hook instanceof InterruptionHook interruptionHook) {
					graph.addNode(hookNodeName, interruptionHook);
				} else {
					graph.addNode(hookNodeName, modelHook::afterModel);
				}
			} else if (hook instanceof MessagesModelHook messagesModelHook) {
				graph.addNode(hookNodeName,
						MessagesModelHook.afterModelAction(messagesModelHook));
			}

			// Connect hook nodes
			if (prevHookNodeName == null) {
				// Connect source to the first afterModel hook
				graph.addEdge(sourceNode, hookNodeName);
			} else {
				graph.addEdge(prevHookNodeName, hookNodeName);
			}
			prevHookNodeName = hookNodeName;
			lastHookNodeName = hookNodeName;
		}

		return lastHookNodeName;
	}

	/**
	 * Chains beforeAgent hooks in sequence and connects to the next node.
	 *
	 * @param graph the state graph
	 * @param beforeAgentHooks the list of beforeAgent hooks
	 * @param nextNode the next node to connect to
	 * @throws GraphStateException if graph construction fails
	 */
	protected void chainBeforeAgentHooks(StateGraph graph, List<Hook> beforeAgentHooks,
			String nextNode) throws GraphStateException {
		for (int i = 0; i < beforeAgentHooks.size(); i++) {
			Hook hook = beforeAgentHooks.get(i);
			String hookNodeName = Hook.getFullHookName(hook) + ".before";

			if (i < beforeAgentHooks.size() - 1) {
				// Connect to next beforeAgent hook
				String nextHookName = Hook.getFullHookName(beforeAgentHooks.get(i + 1)) + ".before";
				graph.addEdge(hookNodeName, nextHookName);
			} else {
				// Last beforeAgent hook connects to next node
				graph.addEdge(hookNodeName, nextNode);
			}
		}
	}

	/**
	 * Chains afterAgent hooks in sequence and connects to END.
	 *
	 * @param graph the state graph
	 * @param afterAgentHooks the list of afterAgent hooks
	 * @throws GraphStateException if graph construction fails
	 */
	protected void chainAfterAgentHooks(StateGraph graph, List<Hook> afterAgentHooks)
			throws GraphStateException {
		if (afterAgentHooks.isEmpty()) {
			return;
		}

		// Connect first afterAgent hook to END
		String firstHookName = Hook.getFullHookName(afterAgentHooks.get(0)) + ".after";
		graph.addEdge(firstHookName, END);

		// Chain remaining hooks in reverse order
		for (int i = afterAgentHooks.size() - 1; i > 0; i--) {
			Hook currentHook = afterAgentHooks.get(i);
			Hook prevHook = afterAgentHooks.get(i - 1);
			String currentHookName = Hook.getFullHookName(currentHook) + ".after";
			String prevHookName = Hook.getFullHookName(prevHook) + ".after";
			graph.addEdge(currentHookName, prevHookName);
		}
	}

	/**
	 * Determines the exit node based on afterAgent hooks.
	 *
	 * @param afterAgentHooks the list of afterAgent hooks
	 * @return the name of the exit node
	 */
	protected String determineExitNode(List<Hook> afterAgentHooks) {
		if (!afterAgentHooks.isEmpty()) {
			return Hook.getFullHookName(afterAgentHooks.get(afterAgentHooks.size() - 1)) + ".after";
		}
		return END;
	}

	/**
	 * Determines the entry node based on beforeAgent and beforeModel hooks.
	 *
	 * @param rootAgent the root agent
	 * @param beforeAgentHooks the list of beforeAgent hooks
	 * @param beforeModelHooks the list of beforeModel hooks
	 * @return the name of the entry node
	 */
	protected String determineEntryNode(Agent rootAgent, List<Hook> beforeAgentHooks,
			List<Hook> beforeModelHooks) {
		if (!beforeAgentHooks.isEmpty()) {
			return Hook.getFullHookName(beforeAgentHooks.get(0)) + ".before";
		} else if (!beforeModelHooks.isEmpty()) {
			return Hook.getFullHookName(beforeModelHooks.get(0)) + ".beforeModel";
		}
		return rootAgent.name();
	}

}

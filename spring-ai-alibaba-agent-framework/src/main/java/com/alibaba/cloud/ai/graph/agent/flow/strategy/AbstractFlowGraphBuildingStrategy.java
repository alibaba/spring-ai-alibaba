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
import java.util.Comparator;
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
 * @author haojun.phj (Jackie)
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
		// 1. Create StateGraph and initialize root agent
		this.graph = config.getStateSerializer() != null
				? new StateGraph(config.getName(), config.getKeyStrategyFactory(), config.getStateSerializer())
				: new StateGraph(config.getName(), config.getKeyStrategyFactory());
		this.rootAgent = config.getRootAgent();

		// 2. Filter and categorize hooks by position
		this.beforeAgentHooks = filterHooksByPosition(config.getHooks(), HookPosition.BEFORE_AGENT);
		this.afterAgentHooks = filterHooksByPosition(config.getHooks(), HookPosition.AFTER_AGENT);
		this.beforeModelHooks = filterHooksByPosition(config.getHooks(), HookPosition.BEFORE_MODEL);
		this.afterModelHooks = filterHooksByPosition(config.getHooks(), HookPosition.AFTER_MODEL);

		// 3. Add all hook nodes to graph (edges will be connected later)
		addBeforeAgentHookNodesToGraph(this.graph, this.beforeAgentHooks);
		addAfterAgentHookNodesToGraph(this.graph, this.afterAgentHooks);
		addBeforeModelHookNodes(this.graph, this.beforeModelHooks);
		addAfterModelHookNodes(this.graph, this.afterModelHooks);

		// 4. Determine entry and exit nodes, then add starting edge
		this.entryNode = determineEntryNode(getRootAgent(), this.beforeAgentHooks, this.beforeModelHooks);
		this.exitNode = determineExitNode(this.afterAgentHooks);
		this.graph.addEdge(START, this.entryNode);

		// 5. Build core graph structure (subclass-specific logic)
		buildCoreGraph(config);

		// 6. Connect beforeModel hooks (can be overridden by subclasses)
		// These must be connected before beforeAgent/afterAgent hooks because
		// beforeAgent hooks may need to connect to beforeModel hooks
		connectBeforeModelHooks();

		// 7. Connect afterModel hooks (can be overridden by subclasses)
		connectAfterModelHooks();

		// 8. Connect beforeAgent hooks (rarely needs to be overridden)
		connectBeforeAgentHooks();

		// 9. Connect afterAgent hooks (rarely needs to be overridden)
		connectAfterAgentHooks();

		return this.graph;
	}

	protected Agent getRootAgent() {
		return this.rootAgent;
	}

	/**
	 * Abstract method that subclasses must implement to build their specific graph structure.
	 * All common setup (graph creation, hook filtering, node addition, etc.) is already done.
	 * Subclasses can directly access protected fields: graph, rootAgent, beforeModelHooks,
	 * afterModelHooks, entryNode, exitNode.
	 *
	 * <p>
	 * Note: Subclasses should NOT call connectBeforeModelHookEdges() or connectAfterModelHookEdges()
	 * in this method. Instead, override the hook methods (connectBeforeModelHooks(), connectAfterModelHooks())
	 * if custom hook connection logic is needed.
	 * </p>
	 *
	 * @param config the flow graph configuration
	 * @throws GraphStateException if graph construction fails
	 */
	protected void buildCoreGraph(FlowGraphBuilder.FlowGraphConfig config)
			throws GraphStateException {
		this.graph.addNode(getRootAgent().name(), node_async(new TransparentNode()));
	}

	/**
	 * Connects beforeModel hook edges. Subclasses can override to customize behavior.
	 * Default implementation connects hooks in sequence to rootAgent.name().
	 *
	 * <p>
	 * This method is called after buildCoreGraph() and before connectAfterModelHooks().
	 * Subclasses that handle hook connections in buildCoreGraph() should override this
	 * method with an empty implementation to avoid duplicate edge connections.
	 * </p>
	 *
	 * @throws GraphStateException if graph construction fails
	 */
	protected void connectBeforeModelHooks() throws GraphStateException {
		if (!this.beforeModelHooks.isEmpty()) {
			connectBeforeModelHookEdges(this.graph, getRootAgent().name(), this.beforeModelHooks);
		}
	}

	/**
	 * Connects afterModel hook edges. Subclasses can override to customize behavior.
	 * Default implementation connects hooks in sequence from rootAgent.name().
	 *
	 * <p>
	 * This method is called after connectBeforeModelHooks() and before connectBeforeAgentHooks().
	 * Subclasses that handle hook connections in buildCoreGraph() should override this
	 * method with an empty implementation to avoid duplicate edge connections.
	 * </p>
	 *
	 * @throws GraphStateException if graph construction fails
	 */
	protected void connectAfterModelHooks() throws GraphStateException {
		if (!this.afterModelHooks.isEmpty()) {
			connectAfterModelHookEdges(this.graph, getRootAgent().name(), this.afterModelHooks);
		}
	}

	/**
	 * Connects beforeAgent hook edges. Subclasses rarely need to override this.
	 * Default implementation chains beforeAgent hooks and connects to the next node
	 * (either the first beforeModel hook or rootAgent).
	 *
	 * <p>
	 * This method is called after connectAfterModelHooks() and before connectAfterAgentHooks().
	 * </p>
	 *
	 * @throws GraphStateException if graph construction fails
	 */
	protected void connectBeforeAgentHooks() throws GraphStateException {
		if (!this.beforeAgentHooks.isEmpty()) {
			String nextNode = !this.beforeModelHooks.isEmpty() ?
					Hook.getFullHookName(this.beforeModelHooks.get(0)) + ".beforeModel"
					: getRootAgent().name();
			chainBeforeAgentHooks(this.graph, this.beforeAgentHooks, nextNode);
		}
	}

	/**
	 * Connects afterAgent hook edges. Subclasses rarely need to override this.
	 * Default implementation chains afterAgent hooks and connects to END.
	 *
	 * <p>
	 * This method is called after connectBeforeAgentHooks() as the final step
	 * of hook connection.
	 * </p>
	 *
	 * @throws GraphStateException if graph construction fails
	 */
	protected void connectAfterAgentHooks() throws GraphStateException {
		if (!this.afterAgentHooks.isEmpty()) {
			chainAfterAgentHooks(this.graph, this.afterAgentHooks);
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
		prioritizedHooks.sort(Comparator.comparingInt(h -> ((Prioritized) h).getOrder()));

		// Combine: prioritized hooks first (sorted), then non-prioritized hooks (original order)
		List<Hook> result = new ArrayList<>(prioritizedHooks);
		result.addAll(nonPrioritizedHooks);

		return result;
	}

	/**
	 * Adds beforeModel hook nodes to the graph without connecting edges.
	 * This method only creates the nodes; edge connections should be handled by subclasses
	 * using {@link #connectBeforeModelHookEdges(StateGraph, String, List)}.
	 *
	 * @param graph the state graph to add nodes to
	 * @param beforeModelHooks the list of beforeModel hooks
	 * @throws GraphStateException if graph construction fails
	 */
	private void addBeforeModelHookNodes(StateGraph graph, List<Hook> beforeModelHooks)
			throws GraphStateException {
		for (Hook hook : beforeModelHooks) {
			if (hook instanceof ModelHook modelHook) {
				if (hook instanceof InterruptionHook interruptionHook) {
					graph.addNode(Hook.getFullHookName(hook) + ".beforeModel", interruptionHook);
				} else {
					graph.addNode(Hook.getFullHookName(hook) + ".beforeModel", modelHook::beforeModel);
				}
			} else if (hook instanceof MessagesModelHook messagesModelHook) {
				graph.addNode(Hook.getFullHookName(hook) + ".beforeModel",
						MessagesModelHook.beforeModelAction(messagesModelHook));
			}
		}
	}

	/**
	 * Adds afterModel hook nodes to the graph without connecting edges.
	 * This method only creates the nodes; edge connections should be handled by subclasses
	 * using {@link #connectAfterModelHookEdges(StateGraph, String, List)}.
	 *
	 * @param graph the state graph to add nodes to
	 * @param afterModelHooks the list of afterModel hooks
	 * @throws GraphStateException if graph construction fails
	 */
	private void addAfterModelHookNodes(StateGraph graph, List<Hook> afterModelHooks)
			throws GraphStateException {
		for (Hook hook : afterModelHooks) {
			if (hook instanceof ModelHook modelHook) {
				if (hook instanceof InterruptionHook interruptionHook) {
					graph.addNode(Hook.getFullHookName(hook) + ".afterModel", interruptionHook);
				} else {
					graph.addNode(Hook.getFullHookName(hook) + ".afterModel", modelHook::afterModel);
				}
			} else if (hook instanceof MessagesModelHook messagesModelHook) {
				graph.addNode(Hook.getFullHookName(hook) + ".afterModel",
						MessagesModelHook.afterModelAction(messagesModelHook));
			}
		}
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
			String hookNodeName = hook.getName() + ".before";

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
			String hookNodeName = hook.getName() + ".after";

			if (hook instanceof AgentHook agentHook) {
				graph.addNode(hookNodeName, agentHook::afterAgent);
			} else if (hook instanceof MessagesAgentHook messagesAgentHook) {
				graph.addNode(hookNodeName, MessagesAgentHook.afterAgentAction(messagesAgentHook));
			}
		}
	}

	/**
	 * Connects beforeModel hook nodes in sequence and returns the first node name.
	 * Note: The hook nodes should already be added to the graph by {@link #addBeforeModelHookNodes(StateGraph, List)}.
	 * This method only handles edge connections.
	 *
	 * @param graph the state graph to add edges to
	 * @param defaultFirstNode the default first node if no beforeModel hooks exist
	 * @param beforeModelHooks the list of beforeModel hooks
	 * @return the name of the first node in the execution chain
	 * @throws GraphStateException if graph construction fails
	 */
	protected String connectBeforeModelHookEdges(StateGraph graph, String defaultFirstNode,
												 List<Hook> beforeModelHooks) throws GraphStateException {
		if (beforeModelHooks.isEmpty()) {
			return defaultFirstNode;
		}

		String firstNodeName = Hook.getFullHookName(beforeModelHooks.get(0)) + ".beforeModel";
		String prevHookNodeName = null;

		for (Hook hook : beforeModelHooks) {
			String hookNodeName = Hook.getFullHookName(hook) + ".beforeModel";

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
	 * Connects afterModel hook nodes in sequence and returns the last node name.
	 * Note: The hook nodes should already be added to the graph by {@link #addAfterModelHookNodes(StateGraph, List)}.
	 * This method only handles edge connections.
	 *
	 * @param graph the state graph to add edges to
	 * @param sourceNode the source node to connect from
	 * @param afterModelHooks the list of afterModel hooks
	 * @return the name of the last node in the chain
	 * @throws GraphStateException if graph construction fails
	 */
	protected String connectAfterModelHookEdges(StateGraph graph, String sourceNode,
												List<Hook> afterModelHooks) throws GraphStateException {
		if (afterModelHooks.isEmpty()) {
			return sourceNode;
		}

		String prevHookNodeName = null;
		String lastHookNodeName = null;

		for (Hook hook : afterModelHooks) {
			String hookNodeName = Hook.getFullHookName(hook) + ".afterModel";

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
			String hookNodeName = hook.getName() + ".before";

			if (i < beforeAgentHooks.size() - 1) {
				// Connect to next beforeAgent hook
				String nextHookName = beforeAgentHooks.get(i + 1).getName() + ".before";
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
	 *
	 * @param afterAgentHooks the list of afterAgent hooks
	 * @return the name of the exit node
	 */
	protected String determineExitNode(List<Hook> afterAgentHooks) {
		if (!afterAgentHooks.isEmpty()) {
			return afterAgentHooks.get(afterAgentHooks.size() - 1).getName() + ".after";
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
			return beforeAgentHooks.get(0).getName() + ".before";
		} else if (!beforeModelHooks.isEmpty()) {
			return Hook.getFullHookName(beforeModelHooks.get(0)) + ".beforeModel";
		}
		return rootAgent.name();
	}

}

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

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LoopAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.loop.LoopStrategy;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.enums.FlowAgentEnum;
import com.alibaba.cloud.ai.graph.agent.flow.node.TransparentNode;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * Converts a LoopAgent into its corresponding StateGraph.
 * <p>
 * Structure of the loop graph: START -> LoopInitLoop -> LoopDispatchNode (condition met -> SubAgentNode -> LoopDispatchNode; condition not met -> END)
 * </p>
 *
 * @author vlsmb
 * @since 2025/8/25
 */
public class LoopGraphBuildingStrategy extends AbstractFlowGraphBuildingStrategy {

	@Override
	protected void buildCoreGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException {
		Agent rootAgent = getRootAgent();
		LoopStrategy loopStrategy = (LoopStrategy) config.getCustomProperty(LoopAgent.LOOP_STRATEGY);
		Agent subAgent = config.getSubAgents().get(0);

		// Add root transparent node as the loop start point
		this.graph.addNode(rootAgent.name(), node_async(new TransparentNode()));

		// Add loop initialization and dispatch nodes
		this.graph.addNode(loopStrategy.loopInitNodeName(), node_async(loopStrategy::loopInit));
		this.graph.addEdge(rootAgent.name(), loopStrategy.loopInitNodeName());

		this.graph.addNode(loopStrategy.loopDispatchNodeName(), node_async(loopStrategy::loopDispatch));
		this.graph.addEdge(loopStrategy.loopInitNodeName(), loopStrategy.loopDispatchNodeName());

		// Add sub-agent node
		this.graph.addNode(subAgent.name(), subAgent.getGraph());

		// Connect beforeModel hooks in the loop (execute before EVERY iteration)
		String loopEntryNode;
		if (!this.beforeModelHooks.isEmpty()) {
			loopEntryNode = connectBeforeModelHookEdges(this.graph, subAgent.name(), this.beforeModelHooks);
		}
		else {
			loopEntryNode = subAgent.name();
		}

		// Connect afterModel hooks in the loop (execute after EVERY iteration)
		String afterSubAgentNode;
		if (!this.afterModelHooks.isEmpty()) {
			afterSubAgentNode = connectAfterModelHookEdges(this.graph, subAgent.name(), this.afterModelHooks);
		}
		else {
			afterSubAgentNode = subAgent.name();
		}

		// Connect back to loop dispatch for next iteration
		this.graph.addEdge(afterSubAgentNode, loopStrategy.loopDispatchNodeName());

		// Add conditional edges for loop control
		this.graph.addConditionalEdges(loopStrategy.loopDispatchNodeName(), edge_async(state -> {
			Boolean shouldContinue = state.value(loopStrategy.loopFlagKey(), false);
			return shouldContinue ? "continue" : "break";
		}), Map.of("continue", loopEntryNode, "break", this.exitNode));
	}

	/**
	 * Connects beforeAgent hooks directly to the loop processor.
	 * In loop scenarios, beforeAgent hooks execute once before the loop starts,
	 * not before each iteration (which is handled by beforeModel hooks).
	 */
	@Override
	protected void connectBeforeAgentHooks() throws GraphStateException {
		if (!this.beforeAgentHooks.isEmpty()) {
			chainBeforeAgentHooks(this.graph, this.beforeAgentHooks, getRootAgent().name());
		}
	}

	/**
	 * Empty implementation - beforeModel hooks are connected within the loop in buildCoreGraph.
	 * This prevents the default behavior of connecting them outside the loop.
	 */
	@Override
	protected void connectBeforeModelHooks() throws GraphStateException {
		// Intentionally empty - hooks are connected in buildCoreGraph
	}

	/**
	 * Empty implementation - afterModel hooks are connected within the loop in buildCoreGraph.
	 * This prevents the default behavior of connecting them outside the loop.
	 */
	@Override
	protected void connectAfterModelHooks() throws GraphStateException {
		// Intentionally empty - hooks are connected in buildCoreGraph
	}

	@Override
	public String getStrategyType() {
		return FlowAgentEnum.LOOP.getType();
	}

	@Override
	public KeyStrategyFactory generateKeyStrategyFactory(FlowGraphBuilder.FlowGraphConfig config) {
		KeyStrategyFactory baseFactory = super.generateKeyStrategyFactory(config);
		LoopStrategy loopStrategy = (LoopStrategy) config.getCustomProperty(LoopAgent.LOOP_STRATEGY);

		return () -> {
			Map<String, KeyStrategy> baseStrategies = baseFactory.apply();
			Map<String, KeyStrategy> loopStrategies = loopStrategy.tempKeys()
					.stream()
					.collect(Collectors.toMap(key -> key, key -> new ReplaceStrategy(), (k1, k2) -> k1));

			return Stream.of(baseStrategies, loopStrategies)
					.flatMap(map -> map.entrySet().stream())
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k1, k2) -> k1));
		};
	}

	/**
	 * Validates loop-specific configuration requirements.
	 * - Loop strategy must be provided and be an instance of LoopStrategy
	 * - Exactly one sub-agent must be provided (loops execute a single agent repeatedly)
	 */
	@Override
	public void validateConfig(FlowGraphBuilder.FlowGraphConfig config) {
		super.validateConfig(config);

		// Validate loop strategy
		Object loopStrategyObj = config.getCustomProperty(LoopAgent.LOOP_STRATEGY);
		if (!(loopStrategyObj instanceof LoopStrategy)) {
			throw new IllegalArgumentException(
					"Loop flow requires a valid LoopStrategy. Got: " + (loopStrategyObj != null
							? loopStrategyObj.getClass().getName() : "null"));
		}

		// Validate sub-agent count
		List<Agent> subAgents = config.getSubAgents();
		if (subAgents == null || subAgents.size() != 1) {
			throw new IllegalArgumentException(
					"Loop flow requires exactly one sub-agent. Got: " + (subAgents != null ? subAgents.size() : 0));
		}
	}
}

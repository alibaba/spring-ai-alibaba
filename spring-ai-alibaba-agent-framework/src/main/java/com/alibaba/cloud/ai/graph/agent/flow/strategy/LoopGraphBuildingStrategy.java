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

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LoopAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.loop.LoopStrategy;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.enums.FlowAgentEnum;
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
	protected void buildCoreGraph(FlowGraphBuilder.FlowGraphConfig config)
			throws GraphStateException {

		// Add beforeModel hooks
		String loopStartNode = this.rootAgent.name();
		if (!this.beforeModelHooks.isEmpty()) {
			loopStartNode = addBeforeModelHookNodesToGraph(this.graph, this.rootAgent.name(), this.beforeModelHooks);
		}

		// Build loop graph based on loopStrategy
		LoopStrategy loopStrategy = (LoopStrategy) config.getCustomProperty(LoopAgent.LOOP_STRATEGY);
		this.graph.addNode(loopStrategy.loopInitNodeName(), node_async(loopStrategy::loopInit));
		this.graph.addEdge(loopStartNode, loopStrategy.loopInitNodeName());

		this.graph.addNode(loopStrategy.loopDispatchNodeName(), node_async(loopStrategy::loopDispatch));
		this.graph.addEdge(loopStrategy.loopInitNodeName(), loopStrategy.loopDispatchNodeName());

		Agent subAgent = config.getSubAgents().get(0);
		this.graph.addNode(subAgent.name(), subAgent.getGraph());

		// Add afterModel hooks if present for loop dispatch
		String loopExitNode = this.exitNode;
		if (!this.afterModelHooks.isEmpty()) {
			String afterModelNodeName = addAfterModelHookNodesToGraph(this.graph, loopStrategy.loopDispatchNodeName(), this.afterModelHooks);
			this.graph.addConditionalEdges(afterModelNodeName, edge_async(
					state -> {
						Boolean value = state.value(loopStrategy.loopFlagKey(), false);
						return value ? "continue" : "break";
					}
			), Map.of("continue", subAgent.name(), "break", loopExitNode));
		} else {
			this.graph.addConditionalEdges(loopStrategy.loopDispatchNodeName(), edge_async(
					state -> {
						Boolean value = state.value(loopStrategy.loopFlagKey(), false);
						return value ? "continue" : "break";
					}
			), Map.of("continue", subAgent.name(), "break", loopExitNode));
		}

		this.graph.addEdge(subAgent.name(), loopStrategy.loopDispatchNodeName());
	}

	@Override
	public String getStrategyType() {
		return FlowAgentEnum.LOOP.getType();
	}

	@Override
	public KeyStrategyFactory generateKeyStrategyFactory(FlowGraphBuilder.FlowGraphConfig config) {
		KeyStrategyFactory factory = super.generateKeyStrategyFactory(config);
		return () -> {
			Map<String, KeyStrategy> map1 = factory.apply();
			LoopStrategy loopStrategy = (LoopStrategy) config.getCustomProperty(LoopAgent.LOOP_STRATEGY);
			Map<String, KeyStrategy> map2 = loopStrategy.tempKeys().stream()
					.collect(Collectors.toMap(
							k -> k,
							k -> new ReplaceStrategy(),
							(k1, k2) -> k1
					));
			return Stream.of(map1, map2).flatMap(m -> m.entrySet().stream())
					.collect(Collectors.toMap(
							Map.Entry::getKey,
							Map.Entry::getValue,
							(k1, k2) -> k1
					));
		};
	}

	@Override
	public void validateConfig(FlowGraphBuilder.FlowGraphConfig config) {
		super.validateConfig(config);
		Object object = config.getCustomProperty(LoopAgent.LOOP_STRATEGY);
		if(!(object instanceof LoopStrategy)) {
			throw new IllegalArgumentException("loopStrategy must be an instance of LoopStrategy");
		}
		List<Agent> subAgents = config.getSubAgents();
		if(subAgents.size() != 1) {
			throw new IllegalArgumentException("loopAgent must have only one subAgent");
		}
	}
}

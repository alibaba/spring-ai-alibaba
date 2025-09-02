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

import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.KeyStrategyFactoryBuilder;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LoopAgent;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.enums.FlowAgentEnum;
import com.alibaba.cloud.ai.graph.agent.flow.node.TransparentNode;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public class LoopGraphBuildingStrategy implements FlowGraphBuildingStrategy {

	private String generateTempInput(BaseAgent agent) {
		return agent.name() + "__input";
	}

	private String generateTempOutput(BaseAgent agent) {
		return agent.name() + "__output";
	}

	private String generateBodyName(String agentName, int idx) {
		return agentName + "__loop_body" + idx;
	}

	@Override
	public StateGraph buildGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException {
		String agentName = config.getName();

		// Special case: if the loop body has no sub-agents, return an empty StateGraph
		// directly
		if (config.getSubAgents() == null || config.getSubAgents().isEmpty()) {
			return new StateGraph(agentName, config.getKeyStrategyFactory()).addEdge(START, END);
		}

		// Get LoopConfig
		LoopAgent.LoopConfig loopConfig = (LoopAgent.LoopConfig) config.getCustomProperty(LoopAgent.LOOP_CONFIG_KEY);

		// Combine the StrategyFactory of Start, loop body, and End into one
		KeyStrategyFactory strategyFactory = new KeyStrategyFactoryBuilder()
			.addStrategies(config.getKeyStrategyFactory().apply())
			.addStrategies(loopConfig.loopMode().getLoopTempKeyStrategyFactory(agentName).apply())
			.addStrategies(config.getSubAgents()
				.stream()
				.flatMap(agent -> Stream.of(generateTempInput(agent), generateTempOutput(agent)))
				.collect(Collectors.toMap(k -> k, v -> new ReplaceStrategy(), (v1, v2) -> v2)))
			.build();
		StateGraph stateGraph = new StateGraph(agentName, strategyFactory);

		// Define node names
		String bodyStartNodeName = agentName + "__loop_body_start__";
		String bodyEndNodeName = agentName + "__loop_body_end__";
		String startNodeName = agentName + "__loop_start__";
		String endNodeName = agentName + "__loop_end__";

		// Add nodes
		stateGraph.addNode(startNodeName, node_async(loopConfig.loopMode().getStartAction(loopConfig)));

		// Expand sub-agent nodes
		String lastOutput = generateTempInput(config.getSubAgents().get(0));
		stateGraph.addNode(bodyStartNodeName,
				node_async(new TransparentNode(lastOutput, LoopAgent.LoopMode.iteratorItemKey(agentName))));
		for (int i = 0; i < config.getSubAgents().size(); i++) {
			String thisOutput = generateTempOutput(config.getSubAgents().get(i));
			stateGraph.addNode(generateBodyName(agentName, i),
					config.getSubAgents().get(i).asAsyncNodeAction(lastOutput, thisOutput));
			lastOutput = thisOutput;
		}
		stateGraph.addNode(bodyEndNodeName,
				node_async(new TransparentNode(LoopAgent.LoopMode.iteratorResultKey(agentName), lastOutput)));

		stateGraph.addNode(endNodeName, node_async(loopConfig.loopMode().getEndAction(loopConfig)));

		// Add conditional edges to control the loop flow
		stateGraph.addEdge(START, startNodeName).addConditionalEdges(startNodeName, edge_async((state -> {
			Boolean flag = state.value(LoopAgent.LoopMode.loopStartFlagKey(agentName), false);
			return flag ? "true" : "false";
		})), Map.of("true", bodyStartNodeName, "false", END));

		// Expand sub-agent edges
		stateGraph.addEdge(bodyStartNodeName, generateBodyName(agentName, 0));
		for (int i = 1; i < config.getSubAgents().size(); i++) {
			stateGraph.addEdge(generateBodyName(agentName, i - 1), generateBodyName(agentName, i));
		}
		stateGraph.addEdge(generateBodyName(agentName, config.getSubAgents().size() - 1), bodyEndNodeName);

		stateGraph.addEdge(bodyEndNodeName, endNodeName).addEdge(endNodeName, startNodeName);

		return stateGraph;
	}

	@Override
	public String getStrategyType() {
		return FlowAgentEnum.LOOP.getType();
	}

	@Override
	public void validateConfig(FlowGraphBuilder.FlowGraphConfig config) {
		FlowGraphBuildingStrategy.super.validateConfig(config);
		Object loopConfigObj = config.getCustomProperty(LoopAgent.LOOP_CONFIG_KEY);
		if (!(loopConfigObj instanceof LoopAgent.LoopConfig)) {
			throw new IllegalArgumentException("loopConfig is not LoopConfig");
		}
		((LoopAgent.LoopConfig) loopConfigObj).validate();
	}

}

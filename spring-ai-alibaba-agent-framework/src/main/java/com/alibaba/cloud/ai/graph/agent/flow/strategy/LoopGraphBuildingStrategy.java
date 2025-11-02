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
import com.alibaba.cloud.ai.graph.StateGraph;
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

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
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
public class LoopGraphBuildingStrategy implements FlowGraphBuildingStrategy {

    @Override
    public StateGraph buildGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException {
        validateConfig(config);
        StateGraph graph = new StateGraph(config.getName(), config.getKeyStrategyFactory());
        Agent rootAgent = config.getRootAgent();

        // Add root transparent node
        graph.addNode(rootAgent.name(), node_async(new TransparentNode()));
        // Add starting edge
        graph.addEdge(START, rootAgent.name());


        // Build loop graph based on loopStrategy
        LoopStrategy loopStrategy = (LoopStrategy) config.getCustomProperty(LoopAgent.LOOP_STRATEGY);
        graph.addNode(loopStrategy.loopInitNodeName(), node_async(loopStrategy::loopInit));
        graph.addEdge(rootAgent.name(), loopStrategy.loopInitNodeName());

        graph.addNode(loopStrategy.loopDispatchNodeName(), node_async(loopStrategy::loopDispatch));
        graph.addEdge(loopStrategy.loopInitNodeName(), loopStrategy.loopDispatchNodeName());

        Agent subAgent = config.getSubAgents().get(0);
        graph.addNode(subAgent.name(), subAgent.getGraph());
        graph.addConditionalEdges(loopStrategy.loopDispatchNodeName(), edge_async(
                state -> {
                    Boolean value = state.value(loopStrategy.loopFlagKey(), false);
                    return value ? "continue" : "break";
                }
        ), Map.of("continue", subAgent.name(), "break", END));

        graph.addEdge(subAgent.name(), loopStrategy.loopDispatchNodeName());

        return graph;
    }

    @Override
    public String getStrategyType() {
        return FlowAgentEnum.LOOP.getType();
    }

    @Override
    public KeyStrategyFactory generateKeyStrategyFactory(FlowGraphBuilder.FlowGraphConfig config) {
        KeyStrategyFactory factory = FlowGraphBuildingStrategy.super.generateKeyStrategyFactory(config);
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
        FlowGraphBuildingStrategy.super.validateConfig(config);
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

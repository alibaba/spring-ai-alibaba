/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.manus2.configuration;

import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.service.AgentService;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.service.DynamicAgentLoader;
import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import com.alibaba.cloud.ai.example.manus.planning.PlanningFactory;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.tool.PlanningTool;
import com.alibaba.cloud.ai.example.manus2.dispatchers.HumanDispatcher;
import com.alibaba.cloud.ai.example.manus2.nodes.*;
import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.example.manus.contants.NodeConstants.*;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Component
public class JmanusConfiguration {

    @Autowired
    private DynamicAgentLoader dynamicAgentLoader;
    @Autowired
    private PlanningFactory planningFactory;
    @Autowired
    private PlanExecutionRecorder planExecutionRecorder;
    @Autowired
    private AgentService agentService;
    @Autowired
    private LlmService llmService;

    @Bean
    public CompiledGraph compiledGraph(ChatClient.Builder chatClientBuilder) throws Exception {

        OverAllStateFactory overAllStateFactory = () -> {
            OverAllState state = new OverAllState();
            state.registerKeyAndStrategy(PLAN_ID,new ReplaceStrategy());
            state.registerKeyAndStrategy(MESSAGES, new AppendStrategy());
            state.registerKeyAndStrategy(CURRENT_PLAN, new ReplaceStrategy());
            state.registerKeyAndStrategy("feedback",new ReplaceStrategy());
            state.registerKeyAndStrategy("coordinator_next_node", new ReplaceStrategy());
            state.registerKeyAndStrategy("planner_next_node", new ReplaceStrategy());
            state.registerKeyAndStrategy("human_next_node", new ReplaceStrategy());
            state.registerKeyAndStrategy("research_team_next_node", new ReplaceStrategy());

            return state;
        };

        StateGraph stateGraph = new StateGraph("Jmanus", overAllStateFactory)
                .addNode(PLANNER_ID, node_async(initPlannerNode(chatClientBuilder)))
                .addNode(EXECUTOR_ID, node_async(initExecutorNode(chatClientBuilder)))
                .addNode(HUMAN_ID, node_async(new HumanFeedbackNode()))
                .addNode(FINALIZER_ID, node_async(new FinalizerNode()))

                .addEdge(StateGraph.START, PLANNER_ID)
                .addEdge(PLANNER_ID, HUMAN_ID)
                .addConditionalEdges(HUMAN_ID, edge_async(new HumanDispatcher()),
                        Map.of(PLANNER_ID,PLANNER_ID,
                                EXECUTOR_ID,EXECUTOR_ID))
                .addEdge(EXECUTOR_ID, FINALIZER_ID)
                .addEdge(FINALIZER_ID, StateGraph.END);

        return stateGraph.compile(
                CompileConfig.builder().saverConfig(SaverConfig.builder().register(SaverConstant.MEMORY, new MemorySaver()).build()).build()
        );
    }

    private NodeAction initExecutorNode(ChatClient.Builder chatClientBuilder) {
        // Add all dynamic agents from the database
        List<DynamicAgentEntity> allAgents = dynamicAgentLoader.getAllAgents();

        return new ExecutorNode(allAgents,planExecutionRecorder,agentService,chatClientBuilder,planningFactory);
    }

    private NodeAction initPlannerNode(ChatClient.Builder chatClientBuilder) throws IOException {

        // Add all dynamic agents from the database
        List<DynamicAgentEntity> allAgents = dynamicAgentLoader.getAllAgents();

        return new PlannerNode(allAgents, chatClientBuilder , planningFactory.toolCallbackMap(null),new PlanningTool());
    }


}

package com.alibaba.cloud.ai.example.manus2.configuration;

import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.service.AgentService;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.service.DynamicAgentLoader;
import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import com.alibaba.cloud.ai.example.manus.planning.PlanningFactory;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.tool.PlanningTool;
import com.alibaba.cloud.ai.example.manus2.dispatchers.HumanDispatcher;
import com.alibaba.cloud.ai.example.manus2.dispatchers.PlannerDispatcher;
import com.alibaba.cloud.ai.example.manus2.nodes.*;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.OverAllStateFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    public StateGraph stateGraph(ChatClient.Builder chatClientBuilder) throws Exception {

        OverAllStateFactory overAllStateFactory = () -> {
            OverAllState state = new OverAllState();
            state.registerKeyAndStrategy(PLAN_ID,new ReplaceStrategy());
            state.registerKeyAndStrategy(MESSAGES, new ReplaceStrategy());
            state.registerKeyAndStrategy(CURRENT_PLAN, new ReplaceStrategy());
            state.registerKeyAndStrategy("feedback",new ReplaceStrategy());
            state.registerKeyAndStrategy("coordinator_next_node", new ReplaceStrategy());
            state.registerKeyAndStrategy("planner_next_node", new ReplaceStrategy());
            state.registerKeyAndStrategy("human_next_node", new ReplaceStrategy());
            state.registerKeyAndStrategy("research_team_next_node", new ReplaceStrategy());

            return state;
        };

        StateGraph stateGraph = new StateGraph("Jmanus", overAllStateFactory)
//                .addNode(COORDINATOR_ID, node_async(new CoordinatorNode()))
                .addNode(PLANNER_ID, node_async(initPlannerNode(chatClientBuilder)))
                .addNode(EXECUTOR_ID, node_async(initExecutorNode(chatClientBuilder)))
                .addNode(HUMAN_ID, node_async(new HumanNode()))
                .addNode(FINALIZER_ID, node_async(new FinalizerNode()))

                .addEdge(StateGraph.START, PLANNER_ID)
                .addEdge(PLANNER_ID, HUMAN_ID)
                .addConditionalEdges(HUMAN_ID, edge_async(new HumanDispatcher()),
                        Map.of(PLANNER_ID,PLANNER_ID,
                                EXECUTOR_ID,EXECUTOR_ID))
                .addEdge(EXECUTOR_ID, FINALIZER_ID)
                .addEdge(FINALIZER_ID, StateGraph.END);

        return stateGraph;
    }

    private NodeAction initExecutorNode(ChatClient.Builder chatClientBuilder) {
        // Add all dynamic agents from the database
        List<DynamicAgentEntity> allAgents = dynamicAgentLoader.getAllAgents();

        return new ExecutorNode(allAgents,planExecutionRecorder,agentService,chatClientBuilder);
    }

    private NodeAction initPlannerNode(ChatClient.Builder chatClientBuilder) throws IOException {


        // Add all dynamic agents from the database
        List<DynamicAgentEntity> allAgents = dynamicAgentLoader.getAllAgents();

        return new PlannerNode(allAgents, chatClientBuilder , planningFactory.toolCallbackMap(null),new PlanningTool());
    }


}

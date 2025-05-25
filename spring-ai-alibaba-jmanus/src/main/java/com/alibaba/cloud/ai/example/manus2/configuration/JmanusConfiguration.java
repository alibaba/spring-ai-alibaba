package com.alibaba.cloud.ai.example.manus2.configuration;

import com.alibaba.cloud.ai.example.manus.contants.NodeConstants.CoordinatorConstants;
import com.alibaba.cloud.ai.example.manus.contants.NodeConstants.ExecutorConstants;
import com.alibaba.cloud.ai.example.manus.contants.NodeConstants.FinalizerConstants;
import com.alibaba.cloud.ai.example.manus.contants.NodeConstants.PlannerConstants;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.service.DynamicAgentLoader;
import com.alibaba.cloud.ai.example.manus.planning.PlanningFactory;
import com.alibaba.cloud.ai.example.manus2.dispatchers.PlannerDispatcher;
import com.alibaba.cloud.ai.example.manus2.nodes.CoordinatorNode;
import com.alibaba.cloud.ai.example.manus2.nodes.PlannerNode;
import com.alibaba.cloud.ai.example.manus2.nodes.ExecutorNode;
import com.alibaba.cloud.ai.example.manus2.nodes.FinalizerNode;
import com.alibaba.cloud.ai.graph.GraphStateException;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.OverAllStateFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.node.HumanNode;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.example.manus.contants.NodeConstants.Human;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Component
public class JmanusConfiguration {

    @Autowired
    private DynamicAgentLoader dynamicAgentLoader;
    @Autowired
    private PlanningFactory planningFactory;

    @Value("${classpath:prompts/planner.md}")
    private org.springframework.core.io.Resource plannerPrompt;


    @Bean
    public StateGraph stateGraph(ChatClient.Builder chatClientBuilder) throws Exception {

        OverAllStateFactory overAllStateFactory = () -> {
            OverAllState overAllState = new OverAllState();
            OverAllState state = new OverAllState();
            state.registerKeyAndStrategy("coordinator_next_node", new ReplaceStrategy());
            state.registerKeyAndStrategy("planner_next_node", new ReplaceStrategy());
            state.registerKeyAndStrategy("human_next_node", new ReplaceStrategy());
            state.registerKeyAndStrategy("research_team_next_node", new ReplaceStrategy());

            return state;
        };

        StateGraph stateGraph = new StateGraph("Jmanus", overAllStateFactory)
                .addNode(CoordinatorConstants.NODE_ID, node_async(new CoordinatorNode()))
                .addNode(PlannerConstants.NODE_ID, node_async(initPlannerNode(chatClientBuilder)))
                .addNode(ExecutorConstants.NODE_ID, node_async(new ExecutorNode()))
                .addNode(Human, node_async(new HumanNode()))
                .addNode(FinalizerConstants.NODE_ID, node_async(new FinalizerNode()))

                .addEdge(StateGraph.START, CoordinatorConstants.NODE_ID)
                .addEdge(CoordinatorConstants.NODE_ID, PlannerConstants.NODE_ID)
                .addConditionalEdges(PlannerConstants.NODE_ID, edge_async(new PlannerDispatcher()),
                        Map.of(ExecutorConstants.NODE_ID, ExecutorConstants.NODE_ID,
                                Human, Human,
                                FinalizerConstants.NODE_ID, FinalizerConstants.NODE_ID))
                .addEdge(ExecutorConstants.NODE_ID, PlannerConstants.NODE_ID)
                .addEdge(Human, PlannerConstants.NODE_ID)
                .addEdge(FinalizerConstants.NODE_ID, StateGraph.END);

        return stateGraph;
    }

    private NodeAction initPlannerNode(ChatClient.Builder chatClientBuilder) throws IOException {

        String systemPrompt = plannerPrompt.getContentAsString(StandardCharsets.UTF_8);

        // Add all dynamic agents from the database
        List<DynamicAgentEntity> allAgents = dynamicAgentLoader.getAllAgents();

        return new PlannerNode(systemPrompt, allAgents, chatClientBuilder , planningFactory.toolCallbackMap(null));
    }


}

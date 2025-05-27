package com.alibaba.cloud.ai.example.manus2.controller;

import com.alibaba.cloud.ai.example.manus.contants.NodeConstants;
import com.alibaba.cloud.ai.example.manus2.nodes.HumanNode;
import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manus/graph")
public class GraphManusController {

    private final CompiledGraph compiledGraph;

    @Autowired
    public GraphManusController(@Qualifier("stateGraph") StateGraph stateGraph) throws GraphStateException {
        this.compiledGraph = stateGraph.compile();
    }

    @GetMapping("/chat")
    public Map<String, Object> chat(@RequestParam(value = "query", defaultValue = "你好") String query,
                                   @RequestParam(value = "plan_id", required = false) int planId) {
        UserMessage userMessage = new UserMessage(query);
        Map<String, Object> objectMap = Map.of("messages", List.of(userMessage),
                "planId",planId);
        if (planId != 0) {
            RunnableConfig runnableConfig = RunnableConfig.builder().threadId(String.valueOf(planId)).build();
            var resultFuture = compiledGraph.invoke(objectMap, runnableConfig);
            return resultFuture.get().data();
        } else {
            var resultFuture = compiledGraph.invoke(objectMap);
            return resultFuture.get().data();
        }
    }

    @GetMapping("/chat/feedback")
    public Map<String, Object> resume(@RequestParam(value = "plan_id", required = true) int planId,
                                     @RequestParam(value = "feedback", required = true) String feedback) {
        RunnableConfig runnableConfig = RunnableConfig.builder().threadId(String.valueOf(planId)).build();
        Map<String, Object> objectMap = Map.of("feedback", feedback);

        StateSnapshot stateSnapshot = compiledGraph.getState(runnableConfig);
        OverAllState state = stateSnapshot.state();
        state.withResume();
        state.withHumanFeedback(new OverAllState.HumanFeedback(objectMap, NodeConstants.HUMAN_ID));

        var resultFuture = compiledGraph.invoke(state, runnableConfig);
        return resultFuture.get().data();
    }
}

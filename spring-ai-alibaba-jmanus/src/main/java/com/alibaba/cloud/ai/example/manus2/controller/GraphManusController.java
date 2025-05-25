package com.alibaba.cloud.ai.example.manus2.controller;

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
                                   @RequestParam(value = "thread_id", required = false) int threadId) {
        UserMessage userMessage = new UserMessage(query);
        Map<String, Object> objectMap = Map.of("messages", List.of(userMessage));

        if (threadId != 0) {
            RunnableConfig runnableConfig = RunnableConfig.builder().threadId(String.valueOf(threadId)).build();
            var resultFuture = compiledGraph.invoke(objectMap, runnableConfig);
            return resultFuture.get().data();
        } else {
            var resultFuture = compiledGraph.invoke(objectMap);
            return resultFuture.get().data();
        }
    }

    @GetMapping("/chat/resume")
    public Map<String, Object> resume(@RequestParam(value = "thread_id", required = true) int threadId,
                                     @RequestParam(value = "feed_back", required = true) String feedBack) {
        RunnableConfig runnableConfig = RunnableConfig.builder().threadId(String.valueOf(threadId)).build();
        Map<String, Object> objectMap = Map.of("feed_back", feedBack);

        StateSnapshot stateSnapshot = compiledGraph.getState(runnableConfig);
        OverAllState state = stateSnapshot.state();
        state.withResume();
        state.withHumanFeedback(new OverAllState.HumanFeedback(objectMap, "planner"));

        var resultFuture = compiledGraph.invoke(objectMap, runnableConfig);
        return resultFuture.get().data();
    }
}

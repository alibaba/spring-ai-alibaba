package com.alibaba.cloud.ai.example.manus2.controller;

import com.alibaba.cloud.ai.example.manus.contants.NodeConstants;
import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/manus/graph")
public class GraphManusController {

    private final CompiledGraph compiledGraph;

    @Autowired
    public GraphManusController(@Qualifier("compiledGraph") CompiledGraph compiledGraph) {
        this.compiledGraph = compiledGraph;
    }

    @GetMapping("/chat")
    public Map<String, Object> chat(@RequestParam(value = "query", defaultValue = "你好") String query,
                                   @RequestParam(value = "plan_id", required = false) String planId) {
        UserMessage userMessage = new UserMessage(query);
        Map<String, Object> objectMap = Map.of("messages", List.of(userMessage),
                "planId", planId);

        RunnableConfig runnableConfig = RunnableConfig.builder()
                .threadId(String.valueOf(planId))
                .build();

        var resultFuture = compiledGraph.invoke(objectMap, runnableConfig);
        return resultFuture.get().data();
    }

    @GetMapping("/chat/feedback")
    public Map<String, Object> resume(@RequestParam(value = "plan_id", required = true) String planId,
                                     @RequestParam(value = "feedback", required = true) String feedback) {
        RunnableConfig runnableConfig = RunnableConfig.builder()
                .threadId(planId)
                .build();

        OverAllState.HumanFeedback humanFeedback = new OverAllState.HumanFeedback(
                Map.of("feedback", feedback), NodeConstants.HUMAN_ID);

        Optional<OverAllState> resumed = compiledGraph.resume(humanFeedback, runnableConfig);

        if (resumed.isPresent()) {
            return resumed.get().data();
        }
        throw new RuntimeException();

    }
}

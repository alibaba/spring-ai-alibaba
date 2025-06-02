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

        return resumed.get().data();

    }
}

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
package com.alibaba.cloud.ai.example.graph.stream;

import com.alibaba.cloud.ai.example.graph.stream.node.LLmNode;
import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.fastjson.JSON;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.bsc.async.AsyncGenerator;
import org.bsc.async.AsyncGeneratorQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static java.util.Arrays.asList;

@RestController
@RequestMapping("/http-stream")
public class HttpStreamController {

    private StateGraph workflow;
    @Autowired
    private LLmNode lLmNode;

    @PostConstruct
    public void init() throws GraphStateException {
        // 定义工作流
        // 创建状态和策略
        workflow = new StateGraph(() -> new OverAllState()
                .registerKeyAndStrategy("llm_result",new AppendStrategy())
                .registerKeyAndStrategy("messages", new AppendStrategy()))
                .addNode("collectInput", node_async(s -> {
                    // 处理输入
                    String input = s.value("input", "");
                    return Map.of("messages", "Received: " + input);
                }))
                .addNode("processData", node_async(lLmNode))
                .addNode("generateResponse", node_async(s -> {
                    // 生成最终响应
                    int count = s.value("count", 0);
                    return Map.of("messages", "Response generated (processed " + count + " items)");
                }))
                .addEdge(START, "collectInput")
                .addEdge("collectInput", "processData")
                .addEdge("processData", "generateResponse")
                .addEdge("generateResponse", END);

    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(HttpServletRequest request, @RequestBody Map<String, Object> inputData) throws Exception {
        CompiledGraph compiledGraph = workflow.compile();
        String threadId = UUID.randomUUID().toString();

        Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();

        AsyncGenerator<NodeOutput> generator = compiledGraph.stream(inputData, RunnableConfig.builder().threadId(threadId).build());

        generator.forEachAsync(output -> {
                    try {
                        System.out.println("output = " + output);
                        if (output instanceof StreamingOutput){
                            StreamingOutput streamingOutput = (StreamingOutput) output;
                            sink.tryEmitNext(ServerSentEvent.builder(JSON.toJSONString(streamingOutput.chunk())).build());
                        }else {
                            sink.tryEmitNext(ServerSentEvent.builder(JSON.toJSONString(output.state().value("messages"))).build());
                        }
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }).thenRun(() -> sink.tryEmitComplete())
                .exceptionally(ex -> {
                    sink.tryEmitError(ex);
                    return null;
                });

        return sink.asFlux()
                .doOnCancel(() -> System.out.println("Client disconnected from stream"))
                .doOnError(e -> System.err.println("Error occurred during streaming: " + e));
    }

}

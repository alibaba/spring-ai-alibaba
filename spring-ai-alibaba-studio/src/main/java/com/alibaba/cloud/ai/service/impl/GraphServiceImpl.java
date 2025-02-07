/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.service.impl;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.PersistentConfig;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.serializer.plain_text.PlainTextStateSerializer;
import com.alibaba.cloud.ai.graph.state.NodeState;
import com.alibaba.cloud.ai.param.GraphStreamParam;
import com.alibaba.cloud.ai.service.GraphService;
import com.alibaba.cloud.ai.graph.InitData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bsc.async.AsyncGenerator;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class GraphServiceImpl implements GraphService {

    private final ObjectMapper objectMapper;

    private final StateGraph stateGraph;

    private InitData initData;

    private final Map<PersistentConfig, CompiledGraph> graphCache = new ConcurrentHashMap<>();

    public static final Map<String, Object> USER_INPUT = new HashMap<>();

    public GraphServiceImpl(ObjectMapper objectMapper, InitData initData, StateGraph stateGraph) {
        this.objectMapper = objectMapper;
        this.initData = initData;
        this.stateGraph = stateGraph;
    }

    @Override
    public InitData init() {
        return initData;
    }

    @Override
    public Flux<ServerSentEvent<String>> stream(GraphStreamParam param, InputStream inputStream) throws Exception {
        var threadId = param.getThread();
        var resume = param.isResume();
        var persistentConfig = new PersistentConfig(param.getSessionId(), threadId);
        var compiledGraph = graphCache.get(persistentConfig);

        final Map<String, Object> dataMap;
        if (resume && stateGraph.getStateSerializer() instanceof PlainTextStateSerializer textSerializer) {
            dataMap = textSerializer.read(new InputStreamReader(inputStream)).data();
        } else {
            dataMap = objectMapper.readValue(inputStream, new TypeReference<>() {});
        }

        AsyncGenerator<? extends NodeOutput> generator;
        if (resume) {
            log.trace("RESUME REQUEST PREPARE");

            if (compiledGraph == null) {
                throw new IllegalStateException("Missing CompiledGraph in session!");
            }

            var checkpointId = param.getCheckpoint();
            var node = param.getNode();
            var config = RunnableConfig.builder()
                    .threadId(threadId)
                    .checkPointId(checkpointId)
                    .build();
            var stateSnapshot = compiledGraph.getState(config);

            config = stateSnapshot.config();
            log.trace("RESUME UPDATE STATE FORM {} USING CONFIG {}\n{}", node, config, dataMap);
            config = compiledGraph.updateState(config, dataMap, node);
            log.trace("RESUME REQUEST STREAM {}", config);

            generator = compiledGraph.streamSnapshots(null, config);
        } else {
            log.trace("dataMap: {}", dataMap);
            if (compiledGraph == null) {
                compiledGraph = stateGraph.compile(compileConfig(persistentConfig));
                graphCache.put(persistentConfig, compiledGraph);
            }
            generator = compiledGraph.streamSnapshots(dataMap, runnableConfig(persistentConfig));
        }

        Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();
        Flux<ServerSentEvent<String>> flux = sink.asFlux();
        generator
                .forEachAsync(s -> {
                    try {
                        if (s.state().data().containsKey(NodeState.SUB_GRAPH)) {
                            CompiledGraph.AsyncNodeGenerator<NodeOutput> subGenerator =
                                    (CompiledGraph.AsyncNodeGenerator)
                                            s.state().data().get(NodeState.SUB_GRAPH);
                            subGenerator.forEach(subS -> {
                                try {
                                    NodeOutput output = subGenerator.buildNodeOutput(subGenerator.getCurrentNodeId());
                                    sink.tryEmitNext(ServerSentEvent.<String>builder()
                                            .id(threadId)
                                            .data(objectMapper.writeValueAsString(output))
                                            .build());
                                } catch (IOException e) {
                                    log.warn("error serializing state", e);
                                } catch (Exception e) {
                                    log.warn("error state", e);
                                    sink.tryEmitError(e);
                                }
                            });
                        } else {
                            sink.tryEmitNext(ServerSentEvent.<String>builder()
                                    .id(threadId)
                                    .data(objectMapper.writeValueAsString(s))
                                    .build());
                        }
                    } catch (IOException e) {
                        log.warn("error state", e);
                        sink.tryEmitError(e);
                    }
                })
                .whenComplete((unused, throwable) -> {
                    if (throwable != null) {
                        log.error("Error streaming", throwable);
                        sink.tryEmitError(throwable);
                    } else {
                        sink.tryEmitComplete();
                    }
                });

        return flux;
    }

    @Override
    public void userInput(Map<String, Object> dataMap) {
        USER_INPUT.putAll(dataMap);
        synchronized (USER_INPUT) {
            USER_INPUT.notify();
        }
    }

    private CompileConfig compileConfig(PersistentConfig config) {
        return CompileConfig.builder()
                .saverConfig(SaverConfig.builder()
                        .register(SaverConstant.MEMORY, new MemorySaver())
                        .build()) // .stateSerializer(stateSerializer)
                .build();
    }

    RunnableConfig runnableConfig(PersistentConfig config) {
        return RunnableConfig.builder().threadId(config.threadId()).build();
    }
}

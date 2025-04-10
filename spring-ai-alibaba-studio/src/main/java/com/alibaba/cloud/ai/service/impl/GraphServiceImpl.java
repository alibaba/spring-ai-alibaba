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
package com.alibaba.cloud.ai.service.impl;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.GraphStateException;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.PersistentConfig;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.serializer.plain_text.PlainTextStateSerializer;
import com.alibaba.cloud.ai.param.GraphStreamParam;
import com.alibaba.cloud.ai.service.GraphService;
import com.alibaba.cloud.ai.graph.GraphInitData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bsc.async.AsyncGenerator;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.ofNullable;

@Service
@Slf4j
public class GraphServiceImpl implements GraphService, ApplicationContextAware {

	private final ObjectMapper objectMapper;

	private final Map<String, Map<PersistentConfig, CompiledGraph>> graphCache = new ConcurrentHashMap<>();

	private Map<String, StateGraph> stateGraphMap;

	public GraphServiceImpl(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public Map<String, StateGraph> getStateGraphs() {
		if (stateGraphMap == null) {
			stateGraphMap = new ConcurrentHashMap<>();
			applicationContext.getBeansOfType(StateGraph.class).forEach((_k, v) -> {
				if (v.getName() != null) {
					stateGraphMap.put(v.getName(), v);
				}
				else {
					stateGraphMap.put(String.valueOf(v.hashCode()), v);
				}
			});
			applicationContext.getBeansOfType(CompiledGraph.class).forEach((_k, v) -> {
				if (v.stateGraph.getName() != null) {
					stateGraphMap.put(v.stateGraph.getName(), v.stateGraph);
				}
				else {
					stateGraphMap.put(String.valueOf(v.hashCode()), v.stateGraph);
				}
			});
		}
		return stateGraphMap;
	}

	@Override
	public GraphInitData getPrintableGraphData(String name) throws GraphStateException {
		List<GraphInitData.ArgumentMetadata> inputArgs = new ArrayList<>();
		inputArgs
			.add(new GraphInitData.ArgumentMetadata(name, GraphInitData.ArgumentMetadata.ArgumentType.STRING, true));

		CompiledGraph compiledGraph = stateGraphMap.get(name).compile();
		var graph = compiledGraph.getGraph(GraphRepresentation.Type.MERMAID, name, false);
		return new GraphInitData(name, graph.content(), inputArgs);
	}

	/**
	 * Serializes the output to the given writer.
	 * @param threadId the ID of the thread.
	 * @param output the output to serialize.
	 */
	private String serializeOutput(String threadId, NodeOutput output) {
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("[ \"").append(threadId).append("\",\n");
			var outputAsString = objectMapper.writeValueAsString(output);
			sb.append(outputAsString).append("\n]");
			return sb.toString();
		}
		catch (IOException e) {
			log.error("error serializing state", e);
			return "";
		}
	}

	@Override
	public Flux<ServerSentEvent<String>> stream(String name, GraphStreamParam param, InputStream inputStream)
			throws Exception {
		var threadId = param.getThread();
		var resume = param.isResume();
		var persistentConfig = new PersistentConfig(param.getSessionId(), threadId);
		var stateGraph = stateGraphMap.get(name);
		var compiledGraph = graphCache.get(name).get(persistentConfig);

		final Map<String, Object> dataMap;
		if (resume && stateGraph.getStateSerializer() instanceof PlainTextStateSerializer textSerializer) {
			dataMap = textSerializer.read(new InputStreamReader(inputStream)).data();
		}
		else {
			dataMap = objectMapper.readValue(inputStream, new TypeReference<>() {
			});
		}

		AsyncGenerator<? extends NodeOutput> generator = null;

		if (resume) {
			log.trace("RESUME REQUEST PREPARE");

			if (compiledGraph == null) {
				throw new IllegalStateException("Missing CompiledGraph in session!");
			}

			var checkpointId = param.getCheckpoint();
			var node = param.getNode();
			var config = RunnableConfig.builder().threadId(threadId).checkPointId(checkpointId).build();

			var stateSnapshot = compiledGraph.getState(config);

			config = stateSnapshot.config();

			log.trace("RESUME UPDATE STATE FORM {} USING CONFIG {}\n{}", node, config, dataMap);

			config = compiledGraph.updateState(config, dataMap, node);

			log.trace("RESUME REQUEST STREAM {}", config);

			generator = compiledGraph.streamSnapshots(null, config);
		}
		else {
			log.trace("dataMap: {}", dataMap);

			if (compiledGraph == null) {
				compiledGraph = stateGraph.compile(compileConfig(persistentConfig));
				Map<PersistentConfig, CompiledGraph> compiledGraphMap = graphCache.computeIfAbsent(name,
						k -> new ConcurrentHashMap<>());
				compiledGraphMap.put(persistentConfig, compiledGraph);
			}

			generator = compiledGraph.streamSnapshots(dataMap, runnableConfig(persistentConfig));
		}

		Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();
		Flux<ServerSentEvent<String>> flux = sink.asFlux();
		generator.forEachAsync(s -> {
			try {
				String output = serializeOutput(threadId, s);
				sink.tryEmitNext(ServerSentEvent.builder(output).build());
				TimeUnit.SECONDS.sleep(1);
			}
			catch (InterruptedException e) {
				throw new CompletionException(e);
			}
		}).thenAccept(v -> sink.tryEmitComplete()).exceptionally(e -> {
			log.error("Error streaming", e);
			sink.tryEmitError(e);
			return null;
		});

		return flux;
	}

	private CompileConfig compileConfig(PersistentConfig config) {
		return CompileConfig.builder()
			.saverConfig(SaverConfig.builder().register(SaverConstant.MEMORY, new MemorySaver()).build()) // .stateSerializer(stateSerializer)
			.build();
	}

	RunnableConfig runnableConfig(PersistentConfig config) {
		return RunnableConfig.builder().threadId(config.threadId()).build();
	}

	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

}

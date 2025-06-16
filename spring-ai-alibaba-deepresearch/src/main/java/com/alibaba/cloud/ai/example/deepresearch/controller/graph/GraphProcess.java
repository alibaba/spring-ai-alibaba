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

package com.alibaba.cloud.ai.example.deepresearch.controller.graph;

import com.alibaba.cloud.ai.example.deepresearch.model.req.ChatRequest;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author yingzi
 * @since 2025/6/6 15:05
 */

public class GraphProcess {

	private static final Logger logger = LoggerFactory.getLogger(GraphProcess.class);

	private final ExecutorService executor = Executors.newFixedThreadPool(10);

	private CompiledGraph compiledGraph;

	public GraphProcess(CompiledGraph compiledGraph) {
		this.compiledGraph = compiledGraph;
	}

	public void handleHumanFeedback(ChatRequest chatRequest, Map<String, Object> objectMap,
			RunnableConfig runnableConfig, Sinks.Many<ServerSentEvent<String>> sink) throws GraphRunnerException {
		objectMap.put("feed_back", chatRequest.interruptFeedback());
		StateSnapshot stateSnapshot = compiledGraph.getState(runnableConfig);
		OverAllState state = stateSnapshot.state();
		state.withResume();
		state.withHumanFeedback(new OverAllState.HumanFeedback(objectMap, "research_team"));
		AsyncGenerator<NodeOutput> resultFuture = compiledGraph.streamFromInitialNode(state, runnableConfig);
		processStream(resultFuture, sink);
	}

	public void processStream(AsyncGenerator<NodeOutput> generator, Sinks.Many<ServerSentEvent<String>> sink) {
		executor.submit(() -> {
			generator.forEachAsync(output -> {
				try {
					// logger.info("output = {}", output);
					String nodeName = output.node();
					String content;
					if (output instanceof StreamingOutput streamingOutput) {
						content = JSON.toJSONString(Map.of(nodeName, streamingOutput.chunk()));
						logger.info("Streaming output from node {}: {}", nodeName, streamingOutput.chunk());
					}
					else {
						JSONObject nodeOutput = new JSONObject();
						nodeOutput.put("data", output.state().data());
						nodeOutput.put("node", nodeName);
						content = JSON.toJSONString(nodeOutput);
					}
					sink.tryEmitNext(ServerSentEvent.builder(content).build());
				}
				catch (Exception e) {
					logger.error("Error processing output", e);
					throw new CompletionException(e);
				}
			}).thenAccept(v -> {
				// 正常完成
				sink.tryEmitComplete();
			}).exceptionally(e -> {
				logger.error("Error in stream processing", e);
				sink.tryEmitError(e);
				return null;
			});
		});
	}

}

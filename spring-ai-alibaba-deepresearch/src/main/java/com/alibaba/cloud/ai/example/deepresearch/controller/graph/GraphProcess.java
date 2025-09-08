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

import com.alibaba.cloud.ai.example.deepresearch.model.enums.NodeNameEnum;
import com.alibaba.cloud.ai.example.deepresearch.model.enums.StreamNodePrefixEnum;
import com.alibaba.cloud.ai.example.deepresearch.model.req.ChatRequest;
import com.alibaba.cloud.ai.example.deepresearch.model.req.GraphId;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Sinks;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Optional;
import java.util.concurrent.Future;

/**
 * @author yingzi
 * @since 2025/6/6 15:05
 */

public class GraphProcess {

	private final ConcurrentHashMap<String, Integer> sessionCountMap = new ConcurrentHashMap<>();

	private final ConcurrentHashMap<GraphId, Future<?>> graphTaskFutureMap = new ConcurrentHashMap<>();

	private static final Logger logger = LoggerFactory.getLogger(GraphProcess.class);

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	// Message sent to frontend when a task is interrupted
	public static final String TASK_STOPPED_MESSAGE_TEMPLATE = "{\"nodeName\": \"__END__\",\"graphId\": %s, \"displayTitle\": \"结束\", \"content\": { \"reason\": \"%s\"}} ";

	// Number of threads must be greater than 2
	private final ExecutorService executor = Executors.newFixedThreadPool(10);

	private final CompiledGraph compiledGraph;

	public GraphProcess(CompiledGraph compiledGraph) {
		this.compiledGraph = compiledGraph;
	}

	public GraphId createNewGraphId(String sessionId) {
		if (StringUtils.isEmpty(sessionId)) {
			throw new IllegalArgumentException("Session Id is empty");
		}
		int count = sessionCountMap.merge(sessionId, 1, Integer::sum);
		return new GraphId(sessionId, String.format("%s-%d", sessionId, count));
	}

	public void handleHumanFeedback(GraphId graphId, ChatRequest chatRequest, Map<String, Object> objectMap,
			RunnableConfig runnableConfig, Sinks.Many<ServerSentEvent<String>> sink) throws GraphRunnerException {
		objectMap.put("feed_back", chatRequest.interruptFeedback());
		StateSnapshot stateSnapshot = compiledGraph.getState(runnableConfig);
		OverAllState state = stateSnapshot.state();
		state.withResume();
		state.withHumanFeedback(new OverAllState.HumanFeedback(objectMap, "research_team"));
		AsyncGenerator<NodeOutput> resultFuture = compiledGraph.streamFromInitialNode(state, runnableConfig);
		processStream(graphId, resultFuture, sink);
	}

	private String safeObjectToJson(Object object) {
		try {
			return OBJECT_MAPPER.writeValueAsString(object);
		}
		catch (JsonProcessingException e) {
			logger.error("JSON processing error: {}", e.getMessage());
			return "{}";
		}
	}

	public void processStream(GraphId graphId, AsyncGenerator<NodeOutput> generator,
			Sinks.Many<ServerSentEvent<String>> sink) {
		final String graphIdStr = this.safeObjectToJson(graphId);
		// Create a task and stop graph execution upon interruption
		Future<?> future = executor.submit(() -> {
			AsyncGenerator.Data<NodeOutput> next;

			while (true) {
				NodeOutput output;
				// Launch a separate thread for iterating over the generator
				Future<AsyncGenerator.Data<NodeOutput>> nextFuture = executor.submit(generator::next);
				try {
					next = nextFuture.get();
					if (next.isDone()) {
						break;
					}
					// Get NodeOutput
					output = next.getData().get();
				}
				catch (CancellationException | ExecutionException e) {
					logger.error("Error in stream processing", e);
					sink.tryEmitNext(
							ServerSentEvent.builder(String.format(TASK_STOPPED_MESSAGE_TEMPLATE, graphIdStr, "服务异常"))
								.build());
					sink.tryEmitError(e);
					return;
				}
				catch (InterruptedException e) {
					logger.info("Stopped by user.");
					sink.tryEmitNext(
							ServerSentEvent.builder(String.format(TASK_STOPPED_MESSAGE_TEMPLATE, graphIdStr, "用户终止"))
								.build());
					sink.tryEmitComplete();
					return;
				}

				String nodeName = output.node();
				String content;
				if (output instanceof StreamingOutput streamingOutput) {
					logger.debug("Streaming output from node {}: {}, {}", nodeName,

							streamingOutput.chatResponse().getResult().getOutput().getText(), graphId);

					content = buildLLMNodeContent(nodeName, graphId, streamingOutput, output);
				}
				else {
					logger.debug("Normal output from node {}: {}", nodeName, output.state().value("messages"));
					content = buildNormalNodeContent(graphId, nodeName, output);
				}
				if (StringUtils.isNotEmpty(content)) {
					sink.tryEmitNext(ServerSentEvent.builder(content).build());
				}

				// Check if the task has been interrupted
				if (Thread.currentThread().isInterrupted()) {
					logger.info("Stopped by user at node: {}", nodeName);
					sink.tryEmitNext(
							ServerSentEvent.builder(String.format(TASK_STOPPED_MESSAGE_TEMPLATE, graphIdStr, "用户终止"))
								.build());
					sink.tryEmitComplete();
					return;
				}
			}

			// Task completed successfully
			logger.info("Stream processing completed.");
			sink.tryEmitComplete();
			// Remove form the Map of task
			graphTaskFutureMap.remove(graphId);
		});
		// Put into Map
		Future<?> oldFuture = graphTaskFutureMap.put(graphId, future);
		Optional.ofNullable(oldFuture).ifPresent((f) -> {
			if (!f.isDone()) {
				logger.warn("A task with the same GraphId {} is still running!", graphId);
			}
		});
	}

	/**
	 * Terminates a running graph
	 * @param graphId graphId
	 * @return whether the operation succeeded
	 */
	public boolean stopGraph(GraphId graphId) {
		Future<?> future = this.graphTaskFutureMap.remove(graphId);
		if (future == null) {
			return false;
		}
		if (future.isDone()) {
			return true;
		}
		return future.cancel(true);
	}

	private String buildLLMNodeContent(String nodeName, GraphId graphId, StreamingOutput streamingOutput,
			NodeOutput output) {
		StreamNodePrefixEnum prefixEnum = StreamNodePrefixEnum.match(nodeName);
		if (prefixEnum == null) {
			return "";
		}
		String stepTitle = (String) output.state().value(nodeName + "_step_title").orElse("");
		String finishReason = Optional.ofNullable(streamingOutput.chatResponse())
			.map(ChatResponse::getResult)
			.map(Generation::getMetadata)
			.map(ChatGenerationMetadata::getFinishReason)
			.orElse("");
		Map<String, Serializable> response = Map.of(nodeName,
				streamingOutput.chatResponse().getResult().getOutput().getText(), "step_title", stepTitle, "visible",
				prefixEnum.isVisible(), "finishReason", finishReason, "graphId", graphId);

		return this.safeObjectToJson(response);
	}

	private record NodeResponse(String nodeName, GraphId graphId, String displayTitle, Object content,
			Object siteInformation) {
	}

	private String buildNormalNodeContent(GraphId graphId, String nodeName, NodeOutput output) {
		NodeNameEnum nodeEnum = NodeNameEnum.fromNodeName(nodeName);
		if (nodeEnum == null) {
			return "";
		}
		Object content;
		// Different nodes provide varying content to the frontend
		content = switch (nodeEnum) {
			case START -> {
				String query = output.state().data().get("query").toString();
				yield Map.of("query", query);
			}
			case COORDINATOR -> output.state().data().get("deep_research");
			case REWRITE_MULTI_QUERY, HUMAN_FEEDBACK, END -> output.state().data();
			case PLANNER -> output.state().data().get("planner_content");
			case RESEARCH_TEAM -> {
				String researchTeamContent = (String) output.state().data().get("research_team_content");
				yield StringUtils.equals(researchTeamContent, NodeNameEnum.REPORTER.nodeName());
			}
			case REPORTER -> output.state().data().get("final_report");
			default -> "";
		};
		Object site_information = output.state().value("site_information").orElse("");
		String displayTitle = nodeEnum.displayTitle();
		if (StringUtils.isEmpty(displayTitle)
				|| (Objects.equals(content, "") && Objects.equals(site_information, ""))) {
			return "";
		}
		NodeResponse response = new NodeResponse(nodeName, graphId, displayTitle, content, site_information);
		try {
			return OBJECT_MAPPER.writeValueAsString(response);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to serialize NodeResponse", e);
		}
	}

}

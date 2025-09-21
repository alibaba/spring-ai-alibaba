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

package com.alibaba.cloud.ai.example.deepresearch.controller;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.cloud.ai.example.deepresearch.config.DeepResearchProperties;
import com.alibaba.cloud.ai.example.deepresearch.controller.graph.GraphProcess;
import com.alibaba.cloud.ai.example.deepresearch.controller.request.ChatRequestProcess;
import com.alibaba.cloud.ai.example.deepresearch.model.ApiResponse;
import com.alibaba.cloud.ai.example.deepresearch.model.req.ChatRequest;
import com.alibaba.cloud.ai.example.deepresearch.model.req.FeedbackRequest;
import com.alibaba.cloud.ai.example.deepresearch.model.req.GraphId;
import com.alibaba.cloud.ai.example.deepresearch.util.SearchBeanUtil;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverEnum;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.observation.GraphObservationLifecycleListener;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yingzi
 * @since 2025/5/17 19:27
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/chat")
public class ChatController {

	private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

	private final CompiledGraph compiledGraph;

	private final GraphProcess graphProcess;

	private final SearchBeanUtil searchBeanUtil;

	@Autowired
	public ChatController(@Qualifier("deepResearch") StateGraph stateGraph, SearchBeanUtil searchBeanUtil,
			ObjectProvider<ObservationRegistry> observationRegistry, DeepResearchProperties deepResearchProperties)
			throws GraphStateException {
		SaverConfig saverConfig = SaverConfig.builder()
			.register(SaverEnum.MEMORY.getValue(), new MemorySaver())
			.build();
		this.compiledGraph = stateGraph.compile(CompileConfig.builder()
			.saverConfig(saverConfig)
			.interruptBefore("human_feedback")
			.withLifecycleListener(new GraphObservationLifecycleListener(
					observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP)))
			.build());
		this.compiledGraph.setMaxIterations(deepResearchProperties.getMaxIterations());
		this.searchBeanUtil = searchBeanUtil;
		this.graphProcess = new GraphProcess(this.compiledGraph);
		logger.info("ChatController initialized with graph maxIterations: {}",
				deepResearchProperties.getMaxIterations());
	}

	/**
	 * SSE (Server-Sent Events) endpoint for chat streaming.
	 *
	 * Accepts a ChatRequest and returns a Flux that streams chat responses as
	 * ServerSentEvent<String>. Supports both initial questions and human feedback
	 * handling.
	 */
	@PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> chatStream(@RequestBody(required = false) ChatRequest chatRequest)
			throws GraphRunnerException, IllegalArgumentException {
		chatRequest = ChatRequestProcess.getDefaultChatRequest(chatRequest, searchBeanUtil);
		if (searchBeanUtil.getSearchService(chatRequest.searchEngine()).isEmpty()) {
			throw new IllegalArgumentException("Search Engine not available.");
		}

		// 创建线程ID
		GraphId graphId = graphProcess.createNewGraphId(chatRequest.sessionId());
		chatRequest = ChatRequestProcess.updateThreadId(chatRequest, graphId.threadId());

		RunnableConfig runnableConfig = RunnableConfig.builder().threadId(chatRequest.threadId()).build();

		Map<String, Object> objectMap = new HashMap<>();
		// Create a unicast sink to emit ServerSentEvents
		Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();

		// Handle human feedback if auto-accept is disabled and feedback is provided
		if (!chatRequest.autoAcceptPlan() && StringUtils.hasText(chatRequest.interruptFeedback())) {
			graphProcess.handleHumanFeedback(graphId, chatRequest, objectMap, runnableConfig, sink);
		}
		// First question
		else {
			ChatRequestProcess.initializeObjectMap(chatRequest, objectMap);
			logger.info("init inputs: {}", objectMap);
			AsyncGenerator<NodeOutput> resultFuture = compiledGraph.stream(objectMap, runnableConfig);
			graphProcess.processStream(graphId, resultFuture, sink);
		}

		return sink.asFlux()
			.doOnCancel(() -> logger.info("Client disconnected from stream"))
			.onErrorResume(throwable -> {
				logger.error("Error occurred during streaming", throwable);
				return Mono.just(ServerSentEvent.<String>builder()
					.event("error")
					.data("Error occurred during streaming: " + throwable.getMessage())
					.build());
			});
	}

	@PostMapping("/stop")
	public ApiResponse<String> stopGraph(@RequestBody GraphId graphId) {
		return graphProcess.stopGraph(graphId) ? ApiResponse.success(graphId.threadId())
				: ApiResponse.error("Failure", graphId.threadId());
	}

	@PostMapping(value = "/resume", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> resume(@RequestBody(required = false) FeedbackRequest humanFeedback)
			throws GraphRunnerException {
		RunnableConfig runnableConfig = RunnableConfig.builder().threadId(humanFeedback.threadId()).build();
		Map<String, Object> objectMap = new HashMap<>();
		objectMap.put("feedback", humanFeedback.feedback());
		objectMap.put("feedback_content", humanFeedback.feedbackContent());

		// Create a unicast sink to emit ServerSentEvents
		Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();
		GraphProcess graphProcess = new GraphProcess(this.compiledGraph);

		StateSnapshot stateSnapshot = compiledGraph.getState(runnableConfig);
		OverAllState state = stateSnapshot.state();
		state.withResume();
		state.withHumanFeedback(new OverAllState.HumanFeedback(objectMap, "research_team"));

		AsyncGenerator<NodeOutput> resultFuture = compiledGraph.streamFromInitialNode(state, runnableConfig);
		graphProcess.processStream(new GraphId(humanFeedback.sessionId(), humanFeedback.threadId()), resultFuture,
				sink);

		return sink.asFlux()
			.doOnCancel(() -> logger.info("Client disconnected from stream"))
			.doOnError(e -> logger.error("Error occurred during streaming", e));
	}

}

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

import com.alibaba.cloud.ai.example.deepresearch.controller.graph.GraphProcess;
import com.alibaba.cloud.ai.example.deepresearch.controller.request.ChatRequestProcess;
import com.alibaba.cloud.ai.example.deepresearch.model.req.ChatRequest;
import com.alibaba.cloud.ai.example.deepresearch.model.req.FeedbackRequest;
import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yingzi
 * @since 2025/5/17 19:27
 */
@RestController
@RequestMapping("/deep-research")
public class DeepResearchController {

	private static final Logger logger = LoggerFactory.getLogger(DeepResearchController.class);

	private final CompiledGraph compiledGraph;

	@Autowired
	public DeepResearchController(@Qualifier("deepResearch") StateGraph stateGraph) throws GraphStateException {
		SaverConfig saverConfig = SaverConfig.builder().register(SaverConstant.MEMORY, new MemorySaver()).build();
		this.compiledGraph = stateGraph
			.compile(CompileConfig.builder().saverConfig(saverConfig).interruptBefore("human_feedback").build());
	}

	/**
	 * SSE (Server-Sent Events) endpoint for chat streaming.
	 *
	 * Accepts a ChatRequest and returns a Flux that streams chat responses as
	 * ServerSentEvent<String>. Supports both initial questions and human feedback
	 * handling.
	 */
	@RequestMapping(value = "/chat/stream", method = RequestMethod.POST, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> chatStream(@RequestBody(required = false) ChatRequest chatRequest)
			throws GraphRunnerException {
		chatRequest = ChatRequestProcess.getDefaultChatRequest(chatRequest);
		RunnableConfig runnableConfig = RunnableConfig.builder().threadId(chatRequest.threadId()).build();

		Map<String, Object> objectMap = new HashMap<>();
		// Create a unicast sink to emit ServerSentEvents
		Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();

		GraphProcess graphProcess = new GraphProcess(this.compiledGraph);
		// Handle human feedback if auto-accept is disabled and feedback is provided
		if (!chatRequest.autoAcceptPlan() && StringUtils.hasText(chatRequest.interruptFeedback())) {
			graphProcess.handleHumanFeedback(chatRequest, objectMap, runnableConfig, sink);
		}
		// First question
		else {
			ChatRequestProcess.initializeObjectMap(chatRequest, objectMap);
			logger.info("init inputs: {}", objectMap);
			AsyncGenerator<NodeOutput> resultFuture = compiledGraph.stream(objectMap, runnableConfig);
			graphProcess.processStream(resultFuture, sink);
		}

		return sink.asFlux()
			.doOnCancel(() -> logger.info("Client disconnected from stream"))
			.doOnError(e -> logger.error("Error occurred during streaming", e));
	}

	@RequestMapping(value = "/chat/resume", method = RequestMethod.POST, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> resume(@RequestBody(required = false) FeedbackRequest humanFeedback)
			throws GraphRunnerException {
		RunnableConfig runnableConfig = RunnableConfig.builder().threadId(humanFeedback.threadId()).build();
		Map<String, Object> objectMap = new HashMap<>();
		objectMap.put("feed_back", humanFeedback.feedBack());
		objectMap.put("feed_back_content", humanFeedback.feedBackContent());

		// Create a unicast sink to emit ServerSentEvents
		Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();
		GraphProcess graphProcess = new GraphProcess(this.compiledGraph);

		StateSnapshot stateSnapshot = compiledGraph.getState(runnableConfig);
		OverAllState state = stateSnapshot.state();
		state.withResume();
		state.withHumanFeedback(new OverAllState.HumanFeedback(objectMap, "research_team"));

		AsyncGenerator<NodeOutput> resultFuture = compiledGraph.streamFromInitialNode(state, runnableConfig);
		graphProcess.processStream(resultFuture, sink);

		return sink.asFlux()
			.doOnCancel(() -> logger.info("Client disconnected from stream"))
			.doOnError(e -> logger.error("Error occurred during streaming", e));
	}

}

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

import com.alibaba.cloud.ai.example.deepresearch.model.ChatRequest;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.alibaba.fastjson.JSON;
import org.bsc.async.AsyncGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author yingzi
 * @since 2025/5/17 19:27
 */
@RestController
@RequestMapping("/deep-research")
public class DeepResearchController {

	private static final Logger logger = LoggerFactory.getLogger(DeepResearchController.class);

	private final CompiledGraph compiledGraph;

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	@Autowired
	public DeepResearchController(@Qualifier("deepResearch") StateGraph stateGraph) throws GraphStateException {
		this.compiledGraph = stateGraph.compile();
	}

	/**
	 * Creates a default ChatRequest instance or set some default value for an instance.
	 */
	private ChatRequest getDefaultChatRequest(ChatRequest chatRequest) {
		if (chatRequest == null) {
			return new ChatRequest(Collections.emptyList(), "__default__", 1, 3, true, null, true, false,
					Collections.emptyMap(), "草莓蛋糕怎么做呀。");
		}
		else {
			return new ChatRequest(chatRequest.messages() == null ? Collections.emptyList() : chatRequest.messages(),
					StringUtils.hasText(chatRequest.threadId()) ? chatRequest.threadId() : "__default__",
					chatRequest.maxPlanIterations() == null ? 1 : chatRequest.maxPlanIterations(),
					chatRequest.maxStepNum() == null ? 3 : chatRequest.maxStepNum(),
					chatRequest.autoAcceptPlan() == null || chatRequest.autoAcceptPlan(),
					chatRequest.interruptFeedback(),
					chatRequest.enableBackgroundInvestigation() == null || chatRequest.enableBackgroundInvestigation(),
					chatRequest.debug() != null && chatRequest.debug(),
					chatRequest.mcpSettings() == null ? Collections.emptyMap() : chatRequest.mcpSettings(),
					StringUtils.hasText(chatRequest.query()) ? chatRequest.query() : "草莓蛋糕怎么做呀。");
		}
	}

	/**
	 * SSE (Server-Sent Events) endpoint for chat streaming.
	 *
	 * Accepts a ChatRequest and returns a Flux that streams chat responses as
	 * ServerSentEvent<String>. Supports both initial questions and human feedback
	 * handling.
	 */
	@RequestMapping(value = "/chat/stream", method = RequestMethod.POST, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> chatStream(@RequestBody(required = false) ChatRequest chatRequest) {
		chatRequest = getDefaultChatRequest(chatRequest);
		RunnableConfig runnableConfig = RunnableConfig.builder()
			.threadId(String.valueOf(chatRequest.threadId()))
			.build();

		Map<String, Object> objectMap = new HashMap<>();
		// Create a unicast sink to emit ServerSentEvents
		Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();

		// Handle human feedback if auto-accept is disabled and feedback is provided
		if (!chatRequest.autoAcceptPlan() && StringUtils.hasText(chatRequest.interruptFeedback())) {
			handleHumanFeedback(chatRequest, objectMap, runnableConfig, sink);
		}
		// First question
		else {
			initializeObjectMap(chatRequest, objectMap);
			logger.info("init inputs: {}", objectMap);
			AsyncGenerator<NodeOutput> resultFuture = compiledGraph.stream(objectMap, runnableConfig);
			processStream(resultFuture, sink);
		}

		return sink.asFlux()
			.doOnCancel(() -> logger.info("Client disconnected from stream"))
			.doOnError(e -> logger.error("Error occurred during streaming", e));
	}

	@GetMapping("/chat")
	public Map<String, Object> chat(@RequestParam(value = "query", defaultValue = "草莓蛋糕怎么做呀") String query,
			@RequestParam(value = "enable_background_investigation",
					defaultValue = "true") boolean enableBackgroundInvestigation,
			@RequestParam(value = "auto_accepted_plan", defaultValue = "true") boolean autoAcceptedPlan,
			@RequestParam(value = "thread_id", required = false, defaultValue = "0") Integer threadId) {
		UserMessage userMessage = new UserMessage(query);
		Map<String, Object> objectMap = Map.of("enable_background_investigation", enableBackgroundInvestigation,
				"auto_accepted_plan", autoAcceptedPlan, "messages", List.of(userMessage));

		if (threadId != 0) {
			RunnableConfig runnableConfig = RunnableConfig.builder().threadId(String.valueOf(threadId)).build();
			var resultFuture = compiledGraph.invoke(objectMap, runnableConfig);
			return resultFuture.get().data();
		}
		else {
			var resultFuture = compiledGraph.invoke(objectMap);
			return resultFuture.get().data();
		}
	}

	@GetMapping("/chat/resume")
	public Map<String, Object> resume(@RequestParam(value = "thread_id", required = true) int threadId,
			@RequestParam(value = "feed_back", required = true) String feedBack,
			@RequestParam(value = "feed_back_content", required = false) String feedBackConent) {
		RunnableConfig runnableConfig = RunnableConfig.builder().threadId(String.valueOf(threadId)).build();
		Map<String, Object> objectMap = Map.of("feed_back", feedBack);
		if ("n".equals(feedBack)) {
			if (StringUtils.hasLength(feedBackConent)) {
				objectMap.put("feed_back_content", feedBackConent);
			}
			else {
				throw new RuntimeException("feed_back_content is required when feed_back is n");
			}
		}

		StateSnapshot stateSnapshot = compiledGraph.getState(runnableConfig);
		OverAllState state = stateSnapshot.state();
		state.withResume();
		state.withHumanFeedback(new OverAllState.HumanFeedback(objectMap, "research_team"));

		var resultFuture = compiledGraph.invoke(objectMap, runnableConfig);

		return resultFuture.get().data();
	}

	private void handleHumanFeedback(ChatRequest chatRequest, Map<String, Object> objectMap,
			RunnableConfig runnableConfig, Sinks.Many<ServerSentEvent<String>> sink) {
		objectMap.put("feed_back", chatRequest.interruptFeedback());
		StateSnapshot stateSnapshot = compiledGraph.getState(runnableConfig);
		OverAllState state = stateSnapshot.state();
		state.withResume();
		state.withHumanFeedback(new OverAllState.HumanFeedback(objectMap, "research_team"));
		AsyncGenerator<NodeOutput> resultFuture = compiledGraph.stream(state, runnableConfig);
		processStream(resultFuture, sink);
	}

	private void initializeObjectMap(ChatRequest chatRequest, Map<String, Object> objectMap) {
		objectMap.put("thread_id", chatRequest.threadId());
		objectMap.put("enable_background_investigation", chatRequest.enableBackgroundInvestigation());
		objectMap.put("auto_accepted_plan", chatRequest.autoAcceptPlan());
		objectMap.put("messages", List.of(new UserMessage(chatRequest.query())));
		objectMap.put("plan_iterations", 0);
		objectMap.put("max_step_num", chatRequest.maxStepNum());
		objectMap.put("current_plan", null);
		objectMap.put("final_report", "");
		objectMap.put("mcp_settings", chatRequest.mcpSettings());
	}

	private void processStream(AsyncGenerator<NodeOutput> generator, Sinks.Many<ServerSentEvent<String>> sink) {
		executor.submit(() -> {
			generator.forEachAsync(output -> {
				try {
					Map<String, Object> data = output.state().data();
					System.out.println("data = " + data);
					Object messages = data.get("messages");
					sink.tryEmitNext(ServerSentEvent.builder(JSON.toJSONString(messages)).build());
				}
				catch (Exception e) {
					throw new CompletionException(e);
				}
			}).thenAccept(v -> {
				// 正常完成
				sink.tryEmitComplete();
			}).exceptionally(e -> {
				sink.tryEmitError(e);
				return null;
			});
		});
	}

}

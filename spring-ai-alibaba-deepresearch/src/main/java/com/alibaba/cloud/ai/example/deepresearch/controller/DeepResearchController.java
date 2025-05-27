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
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.alibaba.cloud.ai.example.deepresearch.model.ChatRequest;
import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;

import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.bsc.async.AsyncGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

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
		this.compiledGraph = stateGraph.compile();
	}

	@PostMapping("/chat/stream")
	public Flux<Map<String, Object>> chatStream(@RequestBody ChatRequest chatRequest) {
		RunnableConfig runnableConfig = RunnableConfig.builder().threadId(String.valueOf(chatRequest.threadId())).build();
		Map<String, Object> objectMap = new HashMap<>();
		// 人类反馈消息
		if (!chatRequest.autoAcceptPlan() && StringUtils.hasText(chatRequest.interruptFeedback())) {
			objectMap.put("feed_back", chatRequest.interruptFeedback());
			StateSnapshot stateSnapshot = compiledGraph.getState(runnableConfig);
			OverAllState state = stateSnapshot.state();
			state.withResume();
			state.withHumanFeedback(new OverAllState.HumanFeedback(objectMap, "research_team"));
			AsyncGenerator<NodeOutput> resultFuture = compiledGraph.stream(state, runnableConfig);
			Stream<OverAllState> overAllStateStream = resultFuture.stream().map(NodeOutput::state);
			return Flux.fromStream(overAllStateStream).map(overAllState -> {
				Map<String, Object> result = new HashMap<>();
				result.put("thread_id", chatRequest.threadId());
				result.put("data", overAllState.data());
				return result;
			});
		}
		// 首次提问
		objectMap.put("thread_id", chatRequest.threadId());
		objectMap.put("enable_background_investigation", chatRequest.enableBackgroundInvestigation());
		objectMap.put("auto_accepted_plan", chatRequest.autoAcceptPlan());
		objectMap.put("messages", chatRequest.messages());
		objectMap.put("plan_iterations", 0);
		objectMap.put("max_step_num", chatRequest.maxStepNum());
		objectMap.put("current_plan", null);
		objectMap.put("final_report", "");
		logger.info("init inputs: {}", objectMap);

		objectMap.put("mcp_settings", chatRequest.mcpSettings());
		Stream<OverAllState> overAllStateStream = compiledGraph.stream(objectMap, runnableConfig).stream().map(NodeOutput::state);
		return Flux.fromStream(overAllStateStream).map(overAllState -> {
			Map<String, Object> result = new HashMap<>();
			result.put("thread_id", chatRequest.threadId());
			result.put("data", overAllState.data());
			return result;
		});	}


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
			@RequestParam(value = "feed_back", required = true) String feedBack) {
		RunnableConfig runnableConfig = RunnableConfig.builder().threadId(String.valueOf(threadId)).build();
		Map<String, Object> objectMap = Map.of("feed_back", feedBack);

		StateSnapshot stateSnapshot = compiledGraph.getState(runnableConfig);
		OverAllState state = stateSnapshot.state();
		state.withResume();
		state.withHumanFeedback(new OverAllState.HumanFeedback(objectMap, "research_team"));

		var resultFuture = compiledGraph.invoke(objectMap, runnableConfig);

		return resultFuture.get().data();
	}

}

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

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author yingzi
 * @date 2025/5/17 19:27
 */
@RestController
@RequestMapping("/deep-research")
public class DeepResearchController {

	private final CompiledGraph compiledGraph;

	@Autowired
	public DeepResearchController(@Qualifier("deepResearch") StateGraph stateGraph) throws GraphStateException {
		this.compiledGraph = stateGraph.compile();
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

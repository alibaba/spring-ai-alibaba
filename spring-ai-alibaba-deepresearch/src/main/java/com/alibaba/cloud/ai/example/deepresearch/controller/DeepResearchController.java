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

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphStateException;
import com.alibaba.cloud.ai.graph.StateGraph;
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
			@RequestParam(value = "enable_background_planning",
					defaultValue = "true") boolean enableBackgroundPlanning) {
		UserMessage userMessage = new UserMessage(query);
		var resultFuture = compiledGraph.invoke(Map.of("enable_background_investigation", enableBackgroundInvestigation,
				"auto_accepted_plan", enableBackgroundPlanning, "messages", List.of(userMessage)));
		var result = resultFuture.get();
		return result.data();
	}

}

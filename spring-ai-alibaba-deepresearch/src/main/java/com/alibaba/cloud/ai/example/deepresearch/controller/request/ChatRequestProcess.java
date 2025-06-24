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

package com.alibaba.cloud.ai.example.deepresearch.controller.request;

import com.alibaba.cloud.ai.example.deepresearch.model.req.ChatRequest;
import com.alibaba.cloud.ai.example.deepresearch.tool.SearchBeanUtil;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Map;

/**
 * @author yingzi
 * @since 2025/6/6 15:06
 */

public class ChatRequestProcess {

	/**
	 * Creates a default ChatRequest instance or set some default value for an instance.
	 */
	public static ChatRequest getDefaultChatRequest(ChatRequest chatRequest, SearchBeanUtil searchBeanUtil) {
		if (chatRequest == null) {
			return new ChatRequest("__default__", 1, 3, true, null, true, Collections.emptyMap(), "草莓蛋糕怎么做呀。",
					searchBeanUtil.getFirstAvailableSearch().orElse(null));
		}
		else {
			return new ChatRequest(StringUtils.hasText(chatRequest.threadId()) ? chatRequest.threadId() : "__default__",
					chatRequest.maxPlanIterations() == null ? 1 : chatRequest.maxPlanIterations(),
					chatRequest.maxStepNum() == null ? 3 : chatRequest.maxStepNum(),
					chatRequest.autoAcceptPlan() == null || chatRequest.autoAcceptPlan(),
					chatRequest.interruptFeedback(),
					chatRequest.enableBackgroundInvestigation() == null || chatRequest.enableBackgroundInvestigation(),
					chatRequest.mcpSettings() == null ? Collections.emptyMap() : chatRequest.mcpSettings(),
					StringUtils.hasText(chatRequest.query()) ? chatRequest.query() : "草莓蛋糕怎么做呀",
					chatRequest.searchEngine() == null ? searchBeanUtil.getFirstAvailableSearch().orElse(null)
							: chatRequest.searchEngine());
		}
	}

	public static void initializeObjectMap(ChatRequest chatRequest, Map<String, Object> objectMap) {
		objectMap.put("thread_id", chatRequest.threadId());
		objectMap.put("enable_background_investigation", chatRequest.enableBackgroundInvestigation());
		objectMap.put("auto_accepted_plan", chatRequest.autoAcceptPlan());
		objectMap.put("query", chatRequest.query());
		objectMap.put("max_step_num", chatRequest.maxStepNum());
		objectMap.put("max_plan_iterations", chatRequest.maxPlanIterations());
		objectMap.put("mcp_settings", chatRequest.mcpSettings());
		objectMap.put("search_engine", chatRequest.searchEngine());
	}

}

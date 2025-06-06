package com.alibaba.cloud.ai.example.deepresearch.controller.request;

import com.alibaba.cloud.ai.example.deepresearch.model.ChatRequest;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author yingzi
 * @date 2025/6/6 15:06
 */

public class ChatRequestProcess {

	/**
	 * Creates a default ChatRequest instance or set some default value for an instance.
	 */
	public static ChatRequest getDefaultChatRequest(ChatRequest chatRequest) {
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

	public static void initializeObjectMap(ChatRequest chatRequest, Map<String, Object> objectMap) {
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

	public static Map<String, Object> getStringObjectMap(String feedBack, String feedBackContent) {
		Map<String, Object> objectMap;
		if ("n".equals(feedBack)) {
			if (StringUtils.hasLength(feedBackContent)) {
				objectMap = Map.of("feed_back", feedBack, "feed_back_content", feedBackContent);
			}
			else {
				throw new RuntimeException("feed_back_content is required when feed_back is n");
			}
		}
		else if ("y".equals(feedBack)) {
			objectMap = Map.of("feed_back", feedBack);
		}
		else {
			throw new RuntimeException("feed_back should be y or n");
		}
		return objectMap;
	}

}

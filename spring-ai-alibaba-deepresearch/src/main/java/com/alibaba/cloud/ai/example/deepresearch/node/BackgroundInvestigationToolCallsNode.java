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

package com.alibaba.cloud.ai.example.deepresearch.node;

import com.alibaba.cloud.ai.example.deepresearch.model.BackgroundInvestigationType;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Allen Hu
 * @date 2025/05/24
 */

public class BackgroundInvestigationToolCallsNode implements BackgroundInvestigationNodeAction {

	private static final Logger logger = LoggerFactory.getLogger(BackgroundInvestigationToolCallsNode.class);

	private final ChatClient chatClient;

	private final ToolCallback[] toolCallbacks;

	public BackgroundInvestigationToolCallsNode(ChatClient chatClient, ToolCallback[] toolCallbacks) {
		this.chatClient = chatClient;
		this.toolCallbacks = toolCallbacks;
	}

	@Override
	public BackgroundInvestigationType of() {
		return BackgroundInvestigationType.TOOL_CALLS;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("background investigation node is running.");
		List<Message> messages = state.value("messages", List.class)
			.map(obj -> new ArrayList<>((List<Message>) obj))
			.orElseGet(ArrayList::new);
		Message lastMessage = messages.isEmpty() ? null : messages.get(messages.size() - 1);
		String query = lastMessage.getText();

		String completion = chatClient.prompt(query)
			.options(ToolCallingChatOptions.builder().toolCallbacks(toolCallbacks).build())
			.call()
			.content();

		logger.info("✅ 调查结果: {}", completion);

		Map<String, Object> resultMap = new HashMap<>();
		resultMap.put("background_investigation_results", Lists.newArrayList(completion));
		return resultMap;
	}

}

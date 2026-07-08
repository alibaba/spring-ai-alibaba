/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.utils;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates whether a persisted message history can be safely used as the base
 * for a new user input.
 */
public final class MessageSequenceValidator {

	private MessageSequenceValidator() {
	}

	public static boolean isCheckpointReadyForNewInput(Map<String, Object> state) {
		Object messages = state.get("messages");
		if (!(messages instanceof List<?> messageList) || messageList.isEmpty()) {
			return true;
		}

		Set<String> pendingToolCallIds = new LinkedHashSet<>();
		boolean waitingForAssistantAfterToolResponse = false;

		for (Object item : messageList) {
			if (!(item instanceof Message)) {
				continue;
			}
			if (item instanceof AssistantMessage assistantMessage) {
				if (!pendingToolCallIds.isEmpty()) {
					return false;
				}
				waitingForAssistantAfterToolResponse = false;
				if (assistantMessage.hasToolCalls()) {
					pendingToolCallIds = toolCallIds(assistantMessage);
					if (pendingToolCallIds.size() != assistantMessage.getToolCalls().size()) {
						return false;
					}
				}
			}
			else if (item instanceof ToolResponseMessage toolResponseMessage) {
				if (pendingToolCallIds.isEmpty() || waitingForAssistantAfterToolResponse) {
					return false;
				}
				if (!removeToolResponseIds(pendingToolCallIds, toolResponseMessage)) {
					return false;
				}
				waitingForAssistantAfterToolResponse = pendingToolCallIds.isEmpty();
			}
			else if (!pendingToolCallIds.isEmpty() || waitingForAssistantAfterToolResponse) {
				return false;
			}
		}

		return pendingToolCallIds.isEmpty() && !waitingForAssistantAfterToolResponse;
	}

	private static Set<String> toolCallIds(AssistantMessage assistantMessage) {
		Set<String> ids = new LinkedHashSet<>();
		for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
			if (StringUtils.hasText(toolCall.id())) {
				ids.add(toolCall.id());
			}
		}
		return ids;
	}

	private static boolean removeToolResponseIds(Set<String> pendingToolCallIds,
			ToolResponseMessage toolResponseMessage) {
		for (ToolResponseMessage.ToolResponse response : toolResponseMessage.getResponses()) {
			if (!StringUtils.hasText(response.id()) || !pendingToolCallIds.remove(response.id())) {
				return false;
			}
		}
		return true;
	}

}

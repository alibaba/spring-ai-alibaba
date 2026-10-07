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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MessageSequenceValidator {

	private MessageSequenceValidator() {
	}

	public static boolean isCheckpointReadyForNewInput(Map<String, Object> state) {
		Object messages = state.get("messages");
		if (!(messages instanceof List<?> messageList)) {
			return true;
		}
		return isReadyForNewInput(messageList);
	}

	private static boolean isReadyForNewInput(List<?> messages) {
		Set<String> pendingToolCallIds = new HashSet<>();
		boolean waitingForAssistantAfterTool = false;
		for (Object item : messages) {
			if (!(item instanceof Message message)) {
				continue;
			}
			if (message instanceof AssistantMessage assistantMessage) {
				if (!pendingToolCallIds.isEmpty()) {
					return false;
				}
				waitingForAssistantAfterTool = false;
				if (assistantMessage.hasToolCalls()) {
					for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
						pendingToolCallIds.add(toolCall.id());
					}
				}
			}
			else if (message instanceof ToolResponseMessage toolResponseMessage) {
				if (pendingToolCallIds.isEmpty()) {
					return false;
				}
				for (ToolResponseMessage.ToolResponse response : toolResponseMessage.getResponses()) {
					if (!pendingToolCallIds.remove(response.id())) {
						return false;
					}
				}
				waitingForAssistantAfterTool = pendingToolCallIds.isEmpty();
			}
			else if (!pendingToolCallIds.isEmpty() || waitingForAssistantAfterTool) {
				return false;
			}
		}
		return pendingToolCallIds.isEmpty() && !waitingForAssistantAfterTool;
	}

}

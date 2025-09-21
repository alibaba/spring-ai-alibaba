/*
 * Copyright 2024-2025 the original author or authors.
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

package com.alibaba.cloud.ai.observation.model.semconv;

import static org.springframework.ai.chat.messages.MessageType.ASSISTANT;
import static org.springframework.ai.chat.messages.MessageType.SYSTEM;
import static org.springframework.ai.chat.messages.MessageType.TOOL;
import static org.springframework.ai.chat.messages.MessageType.USER;

import com.alibaba.cloud.ai.observation.model.semconv.InputOutputModel.ChatMessage;
import com.alibaba.cloud.ai.observation.model.semconv.InputOutputModel.MessagePart;
import com.alibaba.cloud.ai.observation.model.semconv.InputOutputModel.OutputMessage;
import com.alibaba.cloud.ai.observation.model.semconv.InputOutputModel.RoleEnum;
import com.alibaba.cloud.ai.observation.model.semconv.InputOutputModel.TextPart;
import com.alibaba.cloud.ai.observation.model.semconv.InputOutputModel.ToolCallRequestPart;
import com.alibaba.cloud.ai.observation.model.semconv.InputOutputModel.ToolCallResponsePart;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.util.StringUtils;

public final class InputOutputUtils {

	public static ChatMessage convertFromMessage(Message message) {
		String role = getRole(message.getMessageType());
		List<MessagePart> messageParts = new ArrayList<>();
		if (StringUtils.hasText(message.getText())) {
			messageParts.add(new TextPart("text", message.getText()));
		}
		if (message instanceof AssistantMessage && ((AssistantMessage) message).hasToolCalls()) {
			for (ToolCall toolCall : ((AssistantMessage) message).getToolCalls()) {
				messageParts
					.add(new ToolCallRequestPart("tool_call", toolCall.id(), toolCall.name(), toolCall.arguments()));
			}
		}
		else if (message instanceof ToolResponseMessage && !((ToolResponseMessage) message).getResponses().isEmpty()) {
			for (ToolResponse response : ((ToolResponseMessage) message).getResponses()) {
				messageParts
					.add(new ToolCallResponsePart("tool_call_response", response.id(), response.responseData()));
			}
		}
		return new ChatMessage(role, messageParts);
	}

	public static OutputMessage convertFromGeneration(Generation generation) {
		String finishReason = generation.getMetadata().getFinishReason();
		String role = getRole(generation.getOutput().getMessageType());
		List<MessagePart> messageParts = new ArrayList<>();
		if (generation.getOutput().hasToolCalls()) {
			for (ToolCall toolCall : generation.getOutput().getToolCalls()) {
				messageParts
					.add(new ToolCallRequestPart("tool_call", toolCall.id(), toolCall.name(), toolCall.arguments()));
			}
		}
		if (StringUtils.hasText(generation.getOutput().getText())) {
			messageParts.add(new TextPart("text", generation.getOutput().getText()));
		}
		return new OutputMessage(role, messageParts, finishReason);
	}

	private static String getRole(MessageType messageType) {
		if (messageType == USER) {
			return RoleEnum.USER.value;
		}
		else if (messageType == TOOL) {
			return RoleEnum.TOOL.value;
		}
		else if (messageType == ASSISTANT) {
			return RoleEnum.ASSISTANT.value;
		}
		else if (messageType == SYSTEM) {
			return RoleEnum.SYSTEM.value;
		}
		else {
			return RoleEnum.UNKNOWN.value;
		}
	}

}

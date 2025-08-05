package com.alibaba.cloud.ai.observation.model.semconv;

import static org.springframework.ai.chat.messages.MessageType.ASSISTANT;
import static org.springframework.ai.chat.messages.MessageType.SYSTEM;
import static org.springframework.ai.chat.messages.MessageType.TOOL;
import static org.springframework.ai.chat.messages.MessageType.USER;

import com.alibaba.cloud.ai.observation.model.semconv.InputOutputModel.ChatMessage;
import com.alibaba.cloud.ai.observation.model.semconv.InputOutputModel.MessagePart;
import com.alibaba.cloud.ai.observation.model.semconv.InputOutputModel.OutputMessage;
import com.alibaba.cloud.ai.observation.model.semconv.InputOutputModel.TextMessagePart;
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
			messageParts.add(new TextMessagePart("text", message.getText()));
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
			messageParts.add(new TextMessagePart("text", generation.getOutput().getText()));
		}
		return new OutputMessage(role, messageParts, finishReason);
	}

	private static String getRole(MessageType messageType) {
		if (messageType == USER) {
			return "user";
		}
		else if (messageType == TOOL) {
			return "tool";
		}
		else if (messageType == ASSISTANT) {
			return "assistant";
		}
		else if (messageType == SYSTEM) {
			return "system";
		}
		else {
			return "unknown";
		}
	}

}

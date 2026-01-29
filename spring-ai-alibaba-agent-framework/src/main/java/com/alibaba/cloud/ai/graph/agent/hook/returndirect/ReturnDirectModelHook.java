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
package com.alibaba.cloud.ai.graph.agent.hook.returndirect;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.Prioritized;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.agent.hook.returndirect.ReturnDirectConstants.FINISH_REASON_METADATA_KEY;
import static org.springframework.ai.model.tool.ToolExecutionResult.FINISH_REASON;

/**
 * MessagesModelHook that checks for FINISH_REASON in ToolResponseMessage metadata.
 * If found, generates an AssistantMessage and jumps to END node.
 * This hook is designed to execute first among all hooks.
 */
@HookPositions({HookPosition.BEFORE_MODEL})
public class ReturnDirectModelHook extends MessagesModelHook {

	@Override
	public String getName() {
		return "finish_reason_check_messages_model_hook";
	}

	@Override
	public int getOrder() {
		return Prioritized.HIGHEST_PRECEDENCE;
	}

	@Override
	public List<JumpTo> canJumpTo() {
		return List.of(JumpTo.end);
	}

	@Override
	public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
		// Check the last message - if it's a ToolResponseMessage, continue processing
		if (previousMessages.isEmpty()) {
			return new AgentCommand(previousMessages);
		}

		Message lastMessage = previousMessages.get(previousMessages.size() - 1);
		if (!(lastMessage instanceof ToolResponseMessage toolResponseMessage)) {
			// Last message is not a ToolResponseMessage, return normally
			return new AgentCommand(previousMessages);
		}

		// Check if ToolResponseMessage has FINISH_REASON in its metadata
		// FINISH_REASON is set in ToolResponseMessage metadata by AgentToolNode when returnDirect=true
		boolean returnDirect = false;
		Map<String, Object> metadata = toolResponseMessage.getMetadata();
		if (metadata.containsKey(FINISH_REASON_METADATA_KEY)) {
			Object finishReason = metadata.get(FINISH_REASON_METADATA_KEY);
			if (FINISH_REASON.equals(finishReason)) {
				returnDirect = true;
			}
		}

		if (returnDirect) {
			// Generate AssistantMessage from ToolResponseMessage
			String generatedText = generateAssistantMessageText(toolResponseMessage);
			AssistantMessage newAssistantMessage = AssistantMessage.builder()
					.content(generatedText)
					.build();

			// Create new messages list with the generated AssistantMessage
			List<Message> newMessages = new ArrayList<>(previousMessages);
			newMessages.add(newAssistantMessage);

			// Return with JumpTo.end to jump to END node
			return new AgentCommand(JumpTo.end, newMessages);
		}

		// No FINISH_REASON found, return normally
		return new AgentCommand(previousMessages);
	}

	/**
	 * Generate AssistantMessage text from ToolResponseMessage responses.
	 * If there's only one response, returns its responseData directly.
	 * If there are multiple responses, generates a JSON array.
	 */
	private String generateAssistantMessageText(ToolResponseMessage toolResponseMessage) {
		List<ToolResponseMessage.ToolResponse> responses = toolResponseMessage.getResponses();
		if (responses.isEmpty()) {
			return "";
		} else if (responses.size() == 1) {
			// If there's only one response, use responseData directly
			// Handle null responseData: return empty string for single response
			return responses.get(0).responseData();
		} else {
			// If there are multiple responses, generate a JSON array
			StringBuilder jsonArray = new StringBuilder("[");
			for (int i = 0; i < responses.size(); i++) {
				if (i > 0) {
					jsonArray.append(",");
				}
				String responseData = responses.get(i).responseData();
				// Handle null responseData: treat as JSON null
				if (responseData == null) {
					jsonArray.append("null");
				} else {
					String trimmed = responseData.trim();
					// If responseData is already in JSON format, add it directly; otherwise add it as a string
					if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
						jsonArray.append(responseData);
					} else {
						// Escape JSON string
						jsonArray.append("\"").append(escapeJsonString(responseData)).append("\"");
					}
				}
			}
			jsonArray.append("]");
			return jsonArray.toString();
		}
	}

	/**
	 * Escape special characters in a JSON string.
	 */
	private String escapeJsonString(String str) {
		if (str == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (char c : str.toCharArray()) {
			switch (c) {
				case '"':
					sb.append("\\\"");
					break;
				case '\\':
					sb.append("\\\\");
					break;
				case '\b':
					sb.append("\\b");
					break;
				case '\f':
					sb.append("\\f");
					break;
				case '\n':
					sb.append("\\n");
					break;
				case '\r':
					sb.append("\\r");
					break;
				case '\t':
					sb.append("\\t");
					break;
				default:
					if (c < 0x20) {
						sb.append(String.format("\\u%04x", (int) c));
					} else {
						sb.append(c);
					}
					break;
			}
		}
		return sb.toString();
	}
}

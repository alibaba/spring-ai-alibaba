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
package com.alibaba.cloud.ai.graph.agent.hook;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.List;

/**
 * Functional interface for counting tokens in messages.
 *
 * <p>Implementations of this interface provide different strategies for
 * estimating token counts in conversation messages.</p>
 *
 * Example:
 * TokenCounter counter = messages -> {
 *     int total = 0;
 *     for (Message msg : messages) {
 *         total += msg.getText().length() / 4;
 *     }
 *     return total;
 * };
 */
@FunctionalInterface
public interface TokenCounter {
	int DEFAULT_CHARS_PER_TOKEN = 4;

	static TokenCounter approximateMsgCounter() {
		return approximateMsgCounter(DEFAULT_CHARS_PER_TOKEN);
	}

	/**
	 * Creates a token counter with a custom character-to-token ratio.
	 * Handles ToolResponseMessage (processing ToolResponse) and AssistantMessage (processing ToolCall).
	 *
	 * @param charsPerToken The average number of characters per token
	 * @return A token counter using the specified ratio
	 */
	static TokenCounter approximateMsgCounter(int charsPerToken) {
		return messages -> {
			int total = 0;
			for (Message msg : messages) {
				// Handle ToolResponseMessage - count tokens in tool responses
				if (msg instanceof ToolResponseMessage toolResponseMessage) {
					for (ToolResponseMessage.ToolResponse response : toolResponseMessage.getResponses()) {
						// Count tokens in tool response data
						total += response.responseData().length() / charsPerToken;
					}
				}
				// Handle AssistantMessage - count tokens in tool calls
				else if (msg instanceof AssistantMessage assistantMessage) {
					// Count tokens in regular text content
					if (msg.getText() != null) {
						total += msg.getText().length() / charsPerToken;
					}
					// Count tokens in tool calls
					for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
						// Count tokens in tool arguments (JSON string)
						total += toolCall.arguments().length() / charsPerToken;
					}
				}
				// Handle other message types
				else if (msg.getText() != null) {
					total += msg.getText().length() / charsPerToken;
				}
			}
			return total;
		};
	}

	/**
	 * Count the approximate number of tokens in the given messages.
	 *
	 * @param messages The list of messages to count tokens for
	 * @return The estimated token count
	 */
	int countTokens(List<Message> messages);
}


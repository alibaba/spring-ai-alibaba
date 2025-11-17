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
package com.alibaba.cloud.ai.graph.agent.interceptor.contextediting;

import com.alibaba.cloud.ai.graph.agent.hook.TokenCounter;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context editing interceptor that clears older tool results once the conversation
 * grows beyond a configurable token threshold.
 *
 * This mirrors Anthropic's context editing capabilities by managing tool result
 * history to stay within token limits.
 *
 * Example:
 * ContextEditingInterceptor interceptor = ContextEditingInterceptor.builder()
 *     .trigger(100000)
 *     .keep(3)
 *     .clearAtLeast(1000)
 *     .build();
 */
public class ContextEditingInterceptor extends ModelInterceptor {

	private static final Logger log = LoggerFactory.getLogger(ContextEditingInterceptor.class);
	private static final String DEFAULT_PLACEHOLDER = "[cleared]";

	private final int trigger;
	private final int clearAtLeast;
	private final int keep;
	private final boolean clearToolInputs;
	private final Set<String> excludeTools;
	private final String placeholder;
	private final TokenCounter tokenCounter;

	private ContextEditingInterceptor(Builder builder) {
		this.trigger = builder.trigger;
		this.clearAtLeast = builder.clearAtLeast;
		this.keep = builder.keep;
		this.clearToolInputs = builder.clearToolInputs;
		this.excludeTools = builder.excludeTools != null
				? new HashSet<>(builder.excludeTools)
				: new HashSet<>();
		this.placeholder = builder.placeholder;
		this.tokenCounter = builder.tokenCounter;
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
		List<Message> messages = new ArrayList<>(request.getMessages());

		int tokens = tokenCounter.countTokens(messages);

		if (tokens <= trigger) {
			// Token count is below trigger, no editing needed
			return handler.call(request);
		}

		log.info("Token count {} exceeds trigger {}, clearing tool results", tokens, trigger);

		// Find tool messages that can be cleared
		List<ClearableToolMessage> candidates = findClearableCandidates(messages);

		if (candidates.isEmpty()) {
			log.debug("No tool messages to clear");
			return handler.call(request);
		}

		int clearedTokens = 0;
		Set<Integer> indicesToClear = new HashSet<>();

		// Clear tool results until we meet the clearAtLeast threshold
		for (ClearableToolMessage candidate : candidates) {
			if (clearedTokens >= clearAtLeast) {
				break;
			}

			indicesToClear.add(candidate.index);
			clearedTokens += candidate.estimatedTokens;
		}

		// Build updated message list
		List<Message> updatedMessages = new ArrayList<>();
		for (int i = 0; i < messages.size(); i++) {
			Message msg = messages.get(i);

			if (indicesToClear.contains(i)) {
				if (msg instanceof ToolResponseMessage) {
					ToolResponseMessage toolMsg = (ToolResponseMessage) msg;
					List<ToolResponseMessage.ToolResponse> clearedResponses = new ArrayList<>();

					for (ToolResponseMessage.ToolResponse resp : toolMsg.getResponses()) {
						clearedResponses.add(new ToolResponseMessage.ToolResponse(
								resp.id(), resp.name(), placeholder));
					}

					updatedMessages.add(new ToolResponseMessage(clearedResponses, toolMsg.getMetadata()));
				}
				else if (msg instanceof AssistantMessage) {
					AssistantMessage assistantMsg = (AssistantMessage) msg;
					List<AssistantMessage.ToolCall> clearedToolCalls = new ArrayList<>();

					// Clear tool call arguments by replacing with placeholder
					if (assistantMsg.getToolCalls() != null) {
						for (AssistantMessage.ToolCall toolCall : assistantMsg.getToolCalls()) {
							clearedToolCalls.add(new AssistantMessage.ToolCall(
									toolCall.id(), toolCall.type(), toolCall.name(), placeholder));
						}
					}

					// Create new AssistantMessage with cleared tool calls
					AssistantMessage clearedAssistantMsg = new AssistantMessage(
							assistantMsg.getText(),
							assistantMsg.getMetadata(),
							clearedToolCalls
					);
					updatedMessages.add(clearedAssistantMsg);
				}
			}
			else {
				updatedMessages.add(msg);
			}
		}

		if (clearedTokens > 0) {
			log.info("Cleared approximately {} tokens from {} tool messages",
					clearedTokens, indicesToClear.size());

			// Create a new request with updated messages
			ModelRequest updatedRequest = ModelRequest.builder(request)
					.messages(updatedMessages)
					.build();

			return handler.call(updatedRequest);
		}

		return handler.call(request);
	}

	private List<ClearableToolMessage> findClearableCandidates(List<Message> messages) {
		List<ClearableToolMessage> candidates = new ArrayList<>();

		// Find all tool messages
		for (int i = 0; i < messages.size(); i++) {
			Message msg = messages.get(i);

			if (msg instanceof ToolResponseMessage toolMsg) {

				// Check if already cleared
				boolean alreadyCleared = false;
				for (ToolResponseMessage.ToolResponse resp : toolMsg.getResponses()) {
					if (placeholder.equals(resp.responseData())) {
						alreadyCleared = true;
						break;
					}
				}

				if (alreadyCleared) {
					continue;
				}

				// Check if tool is excluded
				boolean excluded = false;
				for (ToolResponseMessage.ToolResponse resp : toolMsg.getResponses()) {
					if (excludeTools.contains(resp.name())) {
						excluded = true;
						break;
					}
				}

				if (excluded) {
					continue;
				}

				int tokens = TokenCounter.approximateMsgCounter().countTokens(List.of(toolMsg));
				candidates.add(new ClearableToolMessage(i, tokens));
			}
			else if (msg instanceof AssistantMessage assistantMsg) {

				// Check if message has tool calls
				if (assistantMsg.getToolCalls().isEmpty()) {
					continue;
				}

				// Check if already cleared (tool calls have placeholder as arguments)
				boolean alreadyCleared = false;
				for (AssistantMessage.ToolCall toolCall : assistantMsg.getToolCalls()) {
					if (placeholder.equals(toolCall.arguments())) {
						alreadyCleared = true;
						break;
					}
				}

				if (alreadyCleared) {
					continue;
				}

				// Check if tool is excluded
				boolean excluded = false;
				for (AssistantMessage.ToolCall toolCall : assistantMsg.getToolCalls()) {
					if (excludeTools.contains(toolCall.name())) {
						excluded = true;
						break;
					}
				}

				if (excluded) {
					continue;
				}

				int tokens = TokenCounter.approximateMsgCounter().countTokens(List.of(assistantMsg));
				candidates.add(new ClearableToolMessage(i, tokens));
			}
		}

		// Sort oldest first, exclude the most recent 'keep' messages
		if (candidates.size() > keep) {
			candidates = candidates.subList(0, candidates.size() - keep);
		}
		else {
			candidates.clear();
		}

		return candidates;
	}


	@Override
	public String getName() {
		return "ContextEditing";
	}

	private static class ClearableToolMessage {
		final int index;
		final int estimatedTokens;

		ClearableToolMessage(int index, int estimatedTokens) {
			this.index = index;
			this.estimatedTokens = estimatedTokens;
		}
	}

	public static class Builder {
		private int trigger = 100000;
		private int clearAtLeast = 0;
		private int keep = 3;
		private boolean clearToolInputs = false;
		private Set<String> excludeTools;
		private String placeholder = DEFAULT_PLACEHOLDER;
		private TokenCounter tokenCounter = TokenCounter.approximateMsgCounter();

		public Builder trigger(int trigger) {
			this.trigger = trigger;
			return this;
		}

		public Builder clearAtLeast(int clearAtLeast) {
			this.clearAtLeast = clearAtLeast;
			return this;
		}

		public Builder keep(int keep) {
			this.keep = keep;
			return this;
		}

		public Builder clearToolInputs(boolean clearToolInputs) {
			this.clearToolInputs = clearToolInputs;
			return this;
		}

		public Builder excludeTools(Set<String> excludeTools) {
			this.excludeTools = excludeTools;
			return this;
		}

		public Builder excludeTools(String... toolNames) {
			this.excludeTools = new HashSet<>(Arrays.asList(toolNames));
			return this;
		}

		public Builder placeholder(String placeholder) {
			this.placeholder = placeholder;
			return this;
		}

		public Builder tokenCounter(TokenCounter tokenCounter) {
			this.tokenCounter = tokenCounter;
			return this;
		}

		public ContextEditingInterceptor build() {
			return new ContextEditingInterceptor(this);
		}
	}
}


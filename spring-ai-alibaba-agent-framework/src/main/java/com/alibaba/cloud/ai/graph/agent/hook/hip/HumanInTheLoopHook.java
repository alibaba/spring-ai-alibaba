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
package com.alibaba.cloud.ai.graph.agent.hook.hip;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.InterruptableAction;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback.FeedbackResult;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.state.RemoveByHash;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@HookPositions(HookPosition.AFTER_MODEL)
public class HumanInTheLoopHook implements ModelHook, AsyncNodeActionWithConfig, InterruptableAction {
	private static final Logger log = LoggerFactory.getLogger(HumanInTheLoopHook.class);

	private Map<String, ToolConfig> approvalOn;

	private HumanInTheLoopHook(Builder builder) {
		this.approvalOn = new HashMap<>(builder.approvalOn);
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
		return afterModel(state, config);
	}

	@Override
	public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
		Optional<Object> feedback = config.metadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY);
		InterruptionMetadata interruptionMetadata = (InterruptionMetadata) feedback.orElseThrow(() -> new RuntimeException("Human feedback is required but not provided in RuntimeConfig."));

		List<Message> messages = new ArrayList<>((List<Message>) state.value("messages").orElse(new ArrayList<>()));
		Message lastMessage = messages.get(messages.size() - 1);

		if (lastMessage instanceof AssistantMessage) {
			AssistantMessage assistantMessage = (AssistantMessage) lastMessage;

			List<AssistantMessage.ToolCall> newToolCalls = new ArrayList<>();

			List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();
			ToolResponseMessage rejectedMessage = new ToolResponseMessage(responses);

			for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
				Optional<ToolFeedback> toolFeedbackOpt = interruptionMetadata.toolFeedbacks().stream()
						.filter(tf -> tf.getName().equals(toolCall.name()))
						.findFirst();

				if (toolFeedbackOpt.isPresent()) {
					ToolFeedback toolFeedback = toolFeedbackOpt.get();
					FeedbackResult result = toolFeedback.getResult();

					if (result == FeedbackResult.APPROVED) {
						newToolCalls.add(toolCall);
					}
					else if (result == FeedbackResult.EDITED) {
						AssistantMessage.ToolCall editedToolCall = new AssistantMessage.ToolCall(toolCall.id(), toolCall.type(), toolCall.name(), toolFeedback.getArguments());
						newToolCalls.add(editedToolCall);
					}
					else if (result == FeedbackResult.REJECTED) {
						ToolResponseMessage.ToolResponse response = new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(), String.format("Tool call request for %s has been rejected by human. The reason for why this tool is rejected and the suggestion for next possible tool choose is listed as below:\n %s.", toolFeedback.getName(), toolFeedback.getDescription()));
						responses.add(response);
					}
				}
				else {
					// If no feedback is provided for a tool that requires approval, treat it as approved to continue.
					newToolCalls.add(toolCall);
				}
			}

			Map<String, Object> updates = new HashMap<>();
			List<Object> newMessages = new ArrayList<>();

			if (!rejectedMessage.getResponses().isEmpty()) {
				newMessages.add(rejectedMessage);
			}

			if (!newToolCalls.isEmpty()) {
				// Replace the last message with the new assistant message containing updated tool calls
				newMessages.add(new AssistantMessage(assistantMessage.getText(), assistantMessage.getMetadata(), newToolCalls, assistantMessage.getMedia()));
				newMessages.add(new RemoveByHash<>(assistantMessage));
			}

			updates.put("messages", newMessages);
			return CompletableFuture.completedFuture(updates);
		}
		else {
			log.warn("Last message is not an AssistantMessage, cannot process human feedback.");
		}

		return CompletableFuture.completedFuture(Map.of());
	}

	@Override
	public Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state, RunnableConfig config) {
		Optional<Object> feedback = config.metadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY);
		if (feedback.isPresent()) {
			if (!validateFeedback((InterruptionMetadata) feedback.get())) {
				return feedback.map(f -> {
					throw new IllegalArgumentException("Invalid human feedback: " + f);
				});// TODO, throw exception?
			}
			return Optional.empty();
		}

		List<Message> messages = (List<Message>) state.value("messages").orElse(new ArrayList<>());
		Message lastMessage = messages.get(messages.size() - 1);

		if (lastMessage instanceof AssistantMessage) {
			// 2. If last message is AssistantMessage
			AssistantMessage assistantMessage = (AssistantMessage) lastMessage;
			for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
				if (approvalOn.containsKey(toolCall.name())) {
					ToolConfig toolConfig = approvalOn.get(toolCall.name());
					String description = toolConfig.getDescription();
					String content = "The AI is requesting to use the tool: " + toolCall.name() + ".\n"
							+ (description != null ? ("Description: " + description + "\n") : "")
							+ "With the following arguments: " + toolCall.arguments() + "\n"
							+ "Do you approve?";
					// TODO, create a designated tool metadata field in InterruptionMetadata?
					InterruptionMetadata interruptionMetadata = InterruptionMetadata.builder(getName(), state)
							.addToolFeedback(InterruptionMetadata.ToolFeedback.builder().id(toolCall.id())
									.name(toolCall.name()).description(content).arguments(toolCall.arguments()).build())
							.build();
					return Optional.of(interruptionMetadata);
				}
			}
		}

		throw new RuntimeException();
	}

	private boolean validateFeedback(InterruptionMetadata feedback) {
		if (feedback == null || feedback.toolFeedbacks() == null || feedback.toolFeedbacks().isEmpty()) {
			return false;
		}

		List<InterruptionMetadata.ToolFeedback> toolFeedbacks = feedback.toolFeedbacks();

		// 1. Ensure each ToolFeedback's result is not empty
		for (InterruptionMetadata.ToolFeedback toolFeedback : toolFeedbacks) {
			if (toolFeedback.getResult() == null) {
				return false;
			}
		}

		// 2. Ensure ToolFeedback count matches approvalOn count and all names are in approvalOn
		if (toolFeedbacks.size() != approvalOn.size()) {
			return false;
		}
		for (InterruptionMetadata.ToolFeedback toolFeedback : toolFeedbacks) {
			if (!approvalOn.containsKey(toolFeedback.getName())) {
				return false;
			}
		}

		return true;
	}

	@Override
	public String getName() {
		return "HIP";
	}

	@Override
	public List<JumpTo> canJumpTo() {
		return List.of();
	}

	public static class Builder {
		private Map<String, ToolConfig> approvalOn = new HashMap<>();

		public Builder approvalOn(String toolName, ToolConfig toolConfig) {
			this.approvalOn.put(toolName, toolConfig);
			return this;
		}

		public Builder approvalOn(String toolName, String description) {
			ToolConfig config = new ToolConfig();
			config.setDescription(description);
			this.approvalOn.put(toolName, config);
			return this;
		}

		public Builder approvalOn(Map<String, ToolConfig> approvalOn) {
			this.approvalOn.putAll(approvalOn);
			return this;
		}

		public HumanInTheLoopHook build() {
			return new HumanInTheLoopHook(this);
		}
	}

}

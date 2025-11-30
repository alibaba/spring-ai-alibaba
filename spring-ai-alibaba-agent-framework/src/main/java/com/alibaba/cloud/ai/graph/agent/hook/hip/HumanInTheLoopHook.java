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
import com.alibaba.cloud.ai.graph.utils.TypeRef;

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
public class HumanInTheLoopHook extends ModelHook implements AsyncNodeActionWithConfig, InterruptableAction {
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
		Optional<InterruptionMetadata> feedback = config.getMetadataAndRemove(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, new TypeRef<InterruptionMetadata>() { });
		InterruptionMetadata interruptionMetadata = feedback.orElse(null);

		if (interruptionMetadata == null) {
			log.debug("No human feedback found in the runnable config metadata, no tool to execute or none needs feedback.");
			return CompletableFuture.completedFuture(Map.of());
		}

		AssistantMessage assistantMessage = getLastAssistantMessage(state);

		if (assistantMessage != null) {

			if (!assistantMessage.hasToolCalls()) {
				log.info("Found human feedback but last AssistantMessage has no tool calls, nothing to process for human feedback.");
				return CompletableFuture.completedFuture(Map.of());
			}

			List<AssistantMessage.ToolCall> newToolCalls = new ArrayList<>();

			List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();
			ToolResponseMessage rejectedMessage = ToolResponseMessage.builder().responses(responses).build();

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
				newMessages.add(AssistantMessage.builder()
					.content(assistantMessage.getText())
					.properties(assistantMessage.getMetadata())
					.toolCalls(newToolCalls)
					.media(assistantMessage.getMedia())
					.build());
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
		AssistantMessage lastMessage = getLastAssistantMessage(state);

		if (lastMessage == null || !lastMessage.hasToolCalls()) {
			return Optional.empty();
		}

		Optional<Object> feedback = config.metadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY);
		if (feedback.isPresent()) {
			if (!(feedback.get() instanceof InterruptionMetadata)) {
				throw new IllegalArgumentException("Human feedback metadata must be of type InterruptionMetadata.");
			}

			if (!validateFeedback((InterruptionMetadata) feedback.get(), lastMessage.getToolCalls())) {
				return buildInterruptionMetadata(state, lastMessage);
			}
			return Optional.empty();
		}

		// 2. If last message is AssistantMessage
		return buildInterruptionMetadata(state, lastMessage);
	}

	private static AssistantMessage getLastAssistantMessage(OverAllState state) {
		List<Message> messages = (List<Message>) state.value("messages").orElse(List.of());

		AssistantMessage lastMessage = null;
		for (int i = messages.size() - 1; i >= 0; i--) {
			Message msg = messages.get(i);
			if (msg instanceof AssistantMessage assistantMessage) {
				// If the next element (i+1) is a ToolResponseMessage, return empty(tools already executed)
				if (i + 1 < messages.size() && messages.get(i + 1) instanceof ToolResponseMessage) {
					break;
				}
				// If the next element is not a ToolResponseMessage, assign and continue
				lastMessage = assistantMessage;
				break;
			}
		}
		return lastMessage;
	}

	private Optional<InterruptionMetadata> buildInterruptionMetadata(OverAllState state, AssistantMessage lastMessage) {
		boolean needsInterruption = false;
		InterruptionMetadata.Builder builder = InterruptionMetadata.builder(getName(), state);
		for (AssistantMessage.ToolCall toolCall : lastMessage.getToolCalls()) {
			if (approvalOn.containsKey(toolCall.name())) {
				ToolConfig toolConfig = approvalOn.get(toolCall.name());
				String description = toolConfig.getDescription();
				String content = "The AI is requesting to use the tool: " + toolCall.name() + ".\n"
						+ (description != null ? ("Description: " + description + "\n") : "")
						+ "With the following arguments: " + toolCall.arguments() + "\n"
						+ "Do you approve?";
				// TODO, create a designated tool metadata field in InterruptionMetadata?
				builder.addToolFeedback(ToolFeedback.builder().id(toolCall.id())
								.name(toolCall.name()).description(content).arguments(toolCall.arguments()).build())
						.build();
				needsInterruption = true;
			}
		}
		return needsInterruption ? Optional.of(builder.build()) : Optional.empty();
	}

	private boolean validateFeedback(InterruptionMetadata feedback, List<AssistantMessage.ToolCall> toolCalls) {
		if (feedback == null || feedback.toolFeedbacks() == null || feedback.toolFeedbacks().isEmpty()) {
			return false;
		}

		List<InterruptionMetadata.ToolFeedback> toolFeedbacks = feedback.toolFeedbacks();

		// 1. Ensure each ToolFeedback's result is not empty
		for (InterruptionMetadata.ToolFeedback toolFeedback : toolFeedbacks) {
			if (toolFeedback.getResult() == null) {
				log.warn("No tool feedback provided, continue to wait for human input.");
				return false;
			}
		}

		// 2. Ensure ToolFeedback count matches approvalOn count and all names are in approvalOn
		if (toolFeedbacks.size() != toolCalls.size()) {
			log.warn("Only {} tool feedbacks provided, but {} tool calls need approval, continue to wait for human input.", toolFeedbacks.size(), toolCalls.size());
			return false;
		}
		for (InterruptionMetadata.ToolFeedback toolFeedback : toolFeedbacks) {
			if (!approvalOn.containsKey(toolFeedback.getName())) {
				log.warn("Tool feedback for tool {} is not expected(not in the tool executing list), continue to wait for human input.", toolFeedback.getName());
				return false;
			}
		}

		return true;
	}

	@Override
	public String getName() {
		return "HITL";
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

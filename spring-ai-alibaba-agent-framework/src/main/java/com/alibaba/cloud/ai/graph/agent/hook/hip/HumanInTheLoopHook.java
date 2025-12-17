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
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
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
    public static final String HITL_NODE_NAME = "HITL";
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
						.filter(tf -> tf.getId().equals(toolCall.id()))
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
						newToolCalls.add(toolCall);
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

			// ToolResponseMessages must be added after AssistantMessage
			if (!rejectedMessage.getResponses().isEmpty()) {
				newMessages.add(rejectedMessage);
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
		InterruptionMetadata.Builder builder = InterruptionMetadata.builder(Hook.getFullHookName(this), state);
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
			} else {
				builder.addToolsAutomaticallyApproved(toolCall);
			}
		}
		return needsInterruption ? Optional.of(builder.build()) : Optional.empty();
	}

    private boolean validateFeedback(InterruptionMetadata feedback, List<AssistantMessage.ToolCall> toolCalls) {
        if (feedback == null || feedback.toolFeedbacks() == null || feedback.toolFeedbacks().isEmpty()) {
            return false;
        }

        List<InterruptionMetadata.ToolFeedback> toolFeedbacks = feedback.toolFeedbacks();

        // 1. Tool calls in this step that actually require human approval (names defined in approvalOn)
        List<AssistantMessage.ToolCall> toolCallsNeedingApproval = toolCalls.stream()
                .filter(tc -> approvalOn.containsKey(tc.name()))
                .toList();

        // If no tool calls in this step require human approval, validation is trivially satisfied
        if (toolCallsNeedingApproval.isEmpty()) {
            return true;
        }

        // 2. For each tool call requiring approval, ensure corresponding feedback exists and its result is non-null
        for (AssistantMessage.ToolCall call : toolCallsNeedingApproval) {
            InterruptionMetadata.ToolFeedback matchedFeedback = toolFeedbacks.stream()
                    .filter(tf -> tf.getName().equals(call.name())
                            // Also validate id if ToolFeedback contains id field
                            && call.id().equals(tf.getId()))
                    .findFirst()
                    .orElse(null);

            if (matchedFeedback == null) {
                log.warn("Missing feedback for tool {} (id={}); waiting for human input.",
                        call.name(), call.id());
                return false;
            }

            // Ensure the feedback result is provided
            if (matchedFeedback.getResult() == null) {
                log.warn("Feedback result for tool {} (id={}) is null; waiting for human input.",
                        call.name(), call.id());
                return false;
            }
        }

        // 3. Optional: log unexpected or extra feedback entries that do not match any pending approval tool
        for (InterruptionMetadata.ToolFeedback tf : toolFeedbacks) {
            boolean matched = toolCallsNeedingApproval.stream()
                    .anyMatch(call -> call.name().equals(tf.getName()) && call.id().equals(tf.getId()));
            if (!matched) {
                log.warn("Ignoring unexpected tool feedback: name={}, id={}", tf.getName(), tf.getId());
            }
        }







        return true;
    }

	@Override
	public String getName() {
		return HITL_NODE_NAME;
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

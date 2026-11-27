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
import com.alibaba.cloud.ai.graph.agent.tool.ToolCallResults;
import com.alibaba.cloud.ai.graph.state.RemoveByHash;
import com.alibaba.cloud.ai.graph.utils.TypeRef;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.part.MessagePart;
import org.springframework.ai.chat.messages.part.TextPart;
import org.springframework.ai.chat.messages.part.ToolCallPart;
import org.springframework.ai.chat.messages.part.ToolResultPart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.tool.ToolCallDecisions;

@HookPositions(HookPosition.AFTER_MODEL)
public class HumanInTheLoopHook extends ModelHook implements AsyncNodeActionWithConfig, InterruptableAction {
	private static final Logger log = LoggerFactory.getLogger(HumanInTheLoopHook.class);
    public static final String HITL_NODE_NAME = "HITL";
	private static final String CONFIRMED_CALLS = HumanInTheLoopHook.class.getName() + ".confirmedCalls";

	private final Map<String, ToolConfig> approvalOn;

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
		clearConfirmedCalls(config);
		Optional<InterruptionMetadata> feedback = config.getMetadataAndRemove(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, new TypeRef<InterruptionMetadata>() { });
		InterruptionMetadata interruptionMetadata = feedback.orElse(null);

		if (interruptionMetadata == null) {
			log.debug("No human feedback found in the runnable config metadata, no tool to execute or none needs feedback.");
			return CompletableFuture.completedFuture(Map.of());
		}

		AssistantMessage assistantMessage = getLastAssistantMessage(state);

		if (assistantMessage == null || !assistantMessage.hasToolCalls()) {
			return CompletableFuture.completedFuture(Map.of());
		}

		List<AssistantMessage.ToolCall> confirmed = new ArrayList<>();
		List<MessagePart> parts = new ArrayList<>();
		List<ToolResultPart> rejected = new ArrayList<>();
		boolean edited = false;
		var keys = ToolCallDecisions.toolCallKeys(assistantMessage).iterator();
		for (MessagePart part : assistantMessage.getParts()) {
			if (part instanceof ToolCallPart callPart) {
				String key = keys.next();
				AssistantMessage.ToolCall call = callPart.toolCall();
				if (!requiresConfirmation(call, config)) {
					parts.add(part);
					continue;
				}
				ToolFeedback decision = interruptionMetadata.toolFeedbacks().stream()
						.filter(item -> matches(key, call, item))
						.findFirst()
						.orElse(null);
				if (decision != null && decision.getResult() == FeedbackResult.EDITED) {
					AssistantMessage.ToolCall replacement = new AssistantMessage.ToolCall(call.id(), call.type(),
							call.name(), decision.getArguments());
					part = new ToolCallPart(replacement, callPart.payload(), callPart.attributes());
					edited = true;
					confirmed.add(replacement);
				}
				else if (decision != null && decision.getResult() == FeedbackResult.APPROVED) {
					confirmed.add(call);
				}
				else if (decision != null && decision.getResult() == FeedbackResult.REJECTED) {
					String reason = "Tool call request for " + call.name() + " was rejected by a human. "
							+ decision.getDescription();
					var result = new ToolResultPart(call.id(), call.name(), List.of(TextPart.of(reason)), true, null,
							Map.of());
					rejected.add(ToolCallResults.withCallKey(result, key));
				}
			}
			parts.add(part);
		}

		config.context().put(CONFIRMED_CALLS, List.copyOf(confirmed));
		List<Object> updates = new ArrayList<>();
		if (edited) {
			updates.add(new RemoveByHash<>(assistantMessage));
			updates.add(AssistantMessage.builder().parts(parts).properties(assistantMessage.getMetadata()).build());
		}
		if (!rejected.isEmpty()) {
			updates.add(ToolResponseMessage.builder().results(rejected).build());
		}
		return CompletableFuture.completedFuture(updates.isEmpty() ? Map.of() : Map.of("messages", updates));
	}

	@Override
	public Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state, RunnableConfig config) {
		AssistantMessage lastMessage = getLastAssistantMessage(state);

		if (lastMessage == null || !lastMessage.hasToolCalls()) {
			return Optional.empty();
		}

		var keys = ToolCallDecisions.toolCallKeys(lastMessage).iterator();
		Map<String, AssistantMessage.ToolCall> pending = new LinkedHashMap<>();
		List<AssistantMessage.ToolCall> automaticallyApproved = new ArrayList<>();
		for (var call : lastMessage.getToolCalls()) {
			String key = keys.next();
			if (requiresConfirmation(call, config)) {
				pending.put(key, call);
			}
			else {
				automaticallyApproved.add(call);
			}
		}
		if (pending.isEmpty()) {
			return Optional.empty();
		}
		Optional<Object> feedback = config.metadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY);
		if (feedback.isPresent()) {
			if (!(feedback.get() instanceof InterruptionMetadata metadata)) {
				throw new IllegalArgumentException("Human feedback metadata must be of type InterruptionMetadata.");
			}
			if (pending.entrySet().stream().allMatch(entry -> hasDecision(entry.getKey(), entry.getValue(), metadata))) {
				return Optional.empty();
			}
		}
		return Optional.of(buildInterruptionMetadata(state, pending, automaticallyApproved));
	}

	private boolean requiresConfirmation(AssistantMessage.ToolCall call, RunnableConfig config) {
		return approvalOn.containsKey(call.name())
				|| (getAgent() != null && getAgent().requiresToolConfirmation(call, config));
	}

	private static boolean hasDecision(String key, AssistantMessage.ToolCall call, InterruptionMetadata metadata) {
		List<ToolFeedback> decisions = metadata.toolFeedbacks().stream()
				.filter(decision -> matches(key, call, decision)).toList();
		return decisions.size() == 1 && decisions.get(0).getResult() != null;
	}

	private static boolean matches(String key, AssistantMessage.ToolCall call, ToolFeedback decision) {
		return key.equals(decision.getId()) && call.name().equals(decision.getName());
	}

	public static boolean isConfirmed(RunnableConfig config, AssistantMessage.ToolCall call) {
		return config.context().get(CONFIRMED_CALLS) instanceof List<?> calls && calls.contains(call);
	}

	public static void clearConfirmedCalls(RunnableConfig config) {
		config.context().remove(CONFIRMED_CALLS);
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

	private InterruptionMetadata buildInterruptionMetadata(OverAllState state,
			Map<String, AssistantMessage.ToolCall> pending, List<AssistantMessage.ToolCall> automaticallyApproved) {
		InterruptionMetadata.Builder builder = InterruptionMetadata.builder(Hook.getFullHookName(this), state)
				.toolsAutomaticallyApproved(automaticallyApproved);
		pending.forEach((key, toolCall) -> {
			ToolConfig toolConfig = approvalOn.get(toolCall.name());
			String description = toolConfig == null ? null : toolConfig.getDescription();
			String content = "The AI is requesting to use the tool: " + toolCall.name() + ".\n"
					+ (description != null ? ("Description: " + description + "\n") : "")
					+ "With the following arguments: " + toolCall.arguments() + "\n"
					+ "Do you approve?";
			builder.addToolFeedback(ToolFeedback.builder().id(key)
					.name(toolCall.name()).description(content).arguments(toolCall.arguments()).build());
		});
		return builder.build();
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

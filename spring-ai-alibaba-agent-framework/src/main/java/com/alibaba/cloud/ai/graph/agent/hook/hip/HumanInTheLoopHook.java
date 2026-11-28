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

import com.alibaba.cloud.ai.graph.KeyStrategy;
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
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallExecutionContext;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.permission.ToolPermissionPolicy.Decision;
import com.alibaba.cloud.ai.graph.agent.tool.ToolCallResults;
import com.alibaba.cloud.ai.graph.state.RemoveByHash;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.part.MessagePart;
import org.springframework.ai.chat.messages.part.TextPart;
import org.springframework.ai.chat.messages.part.ToolCallPart;
import org.springframework.ai.chat.messages.part.ToolResultPart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.ai.model.tool.ToolCallDecisions;

@HookPositions(HookPosition.AFTER_MODEL)
public class HumanInTheLoopHook extends ModelHook implements AsyncNodeActionWithConfig, InterruptableAction {
	public static final String HITL_NODE_NAME = "HITL";

	/** Interruption metadata listing the call keys denied by policy before review. */
	public static final String DENIED_TOOL_CALL_KEYS = "permissionDeniedToolCallKeys";

	private static final ObjectReader ARGUMENTS_READER =
			new ObjectMapper().reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

	private static final String REVIEWED_CALLS = HumanInTheLoopHook.class.getName() + ".reviewedCalls";

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
		Map<String, Object> stateUpdate = new HashMap<>();
		removeConfirmedCalls(state, getAgentName(), stateUpdate);
		if (!(config.context().remove(REVIEWED_CALLS) instanceof ReviewedCalls reviewed)) {
			return CompletableFuture.completedFuture(stateUpdate);
		}

		AssistantMessage assistantMessage = getLastAssistantMessage(state);

		if (assistantMessage == null || !assistantMessage.hasToolCalls()) {
			return CompletableFuture.completedFuture(stateUpdate);
		}

		Map<String, AssistantMessage.ToolCall> confirmed = new HashMap<>();
		List<MessagePart> parts = new ArrayList<>();
		List<ToolResultPart> settled = new ArrayList<>();
		boolean edited = false;
		var keys = ToolCallDecisions.toolCallKeys(assistantMessage).iterator();
		for (MessagePart part : assistantMessage.getParts()) {
			if (part instanceof ToolCallPart callPart) {
				String key = keys.next();
				AssistantMessage.ToolCall call = callPart.toolCall();
				if (reviewed.denied().contains(key)) {
					var result = ToolCallResponse.permissionDenied(call.id(), call.name()).toToolResultPart();
					settled.add(ToolCallResults.withCallKey(result, key));
				}
				ToolFeedback decision = reviewed.decisions().get(key);
				if (decision != null) {
					switch (decision.getResult()) {
						case EDITED -> {
							var replacement = new AssistantMessage.ToolCall(call.id(), call.type(), call.name(),
									decision.getArguments());
							part = new ToolCallPart(replacement, callPart.payload(), callPart.attributes());
							edited = true;
							confirmed.put(key, replacement);
						}
						case APPROVED -> confirmed.put(key, call);
						case REJECTED, RESPONDED -> {
							boolean rejected = decision.getResult() == FeedbackResult.REJECTED;
							String content = rejected
									? "Tool call request for " + call.name() + " was rejected by a human. "
											+ Objects.toString(decision.getDescription(), "")
									: decision.getResponse();
							var result = new ToolResultPart(call.id(), call.name(), List.of(TextPart.of(content)),
									rejected, null, Map.of());
							settled.add(ToolCallResults.withCallKey(result, key));
						}
					}
				}
			}
			parts.add(part);
		}

		if (!confirmed.isEmpty()) {
			config.context().put(CONFIRMED_CALLS, Map.copyOf(confirmed));
			Map<String, Map<String, String>> saved = new HashMap<>();
			confirmed.forEach((key, call) -> saved.put(key, confirmationFields(call)));
			stateUpdate.put(confirmedCallsStateKey(getAgentName()), saved);
		}
		List<Object> updates = new ArrayList<>();
		if (edited) {
			updates.add(new RemoveByHash<>(assistantMessage));
			updates.add(AssistantMessage.builder().parts(parts).properties(assistantMessage.getMetadata()).build());
		}
		if (!settled.isEmpty()) {
			updates.add(ToolResponseMessage.builder().results(settled).build());
		}
		if (!updates.isEmpty()) {
			stateUpdate.put("messages", updates);
		}
		return CompletableFuture.completedFuture(stateUpdate);
	}

	@Override
	public Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state, RunnableConfig config) {
		return interrupt(nodeId, state, config, Optional.empty());
	}

	@Override
	public Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state, RunnableConfig config,
			Optional<InterruptionMetadata> pending) {
		config.context().remove(REVIEWED_CALLS);
		AssistantMessage lastMessage = getLastAssistantMessage(state);
		if (lastMessage == null || !lastMessage.hasToolCalls()) {
			if (pending.isPresent()) {
				throw new IllegalArgumentException("The pending review has no matching assistant tool calls");
			}
			return Optional.empty();
		}

		var request = pending.orElseGet(() -> buildInterruptionMetadata(lastMessage, state, config));
		Set<String> denied = deniedToolCallKeys(request);
		if (request.toolFeedbacks().isEmpty()) {
			if (!denied.isEmpty()) {
				config.context().put(REVIEWED_CALLS, new ReviewedCalls(Map.of(), denied));
			}
			return Optional.empty();
		}
		checkPendingCalls(lastMessage, request, denied);
		Object feedback = config.metadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY).orElse(null);
		if (feedback == null || "placeholder".equals(feedback)) {
			return Optional.of(request);
		}
		if (!(feedback instanceof InterruptionMetadata review)) {
			throw new IllegalArgumentException("Human feedback metadata must be of type InterruptionMetadata");
		}
		if (review.interruptionId().isPresent()) {
			if (pending.isEmpty()) {
				return Optional.of(request);
			}
			if (!review.interruptionId().equals(request.interruptionId())) {
				throw new IllegalArgumentException("No pending interruption with ID '" + review.interruptionId().orElseThrow() + "'");
			}
		}

		var decisions = validateDecisions(request, review);
		if (decisions.size() != request.toolFeedbacks().size()) {
			return Optional.of(request);
		}
		config.metadata().orElseThrow().remove(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY);
		config.context().put(REVIEWED_CALLS, new ReviewedCalls(decisions, denied));
		return Optional.empty();
	}

	private static Set<String> deniedToolCallKeys(InterruptionMetadata request) {
		return request.metadata(DENIED_TOOL_CALL_KEYS).filter(List.class::isInstance)
				.map(value -> Set.copyOf(((List<?>) value).stream().map(String.class::cast).toList()))
				.orElseGet(Set::of);
	}

	private static void checkPendingCalls(AssistantMessage message, InterruptionMetadata request, Set<String> denied) {
		var calls = message.getToolCalls();
		var keys = ToolCallDecisions.toolCallKeys(message);
		Map<String, ToolFeedback> actions = new HashMap<>();
		request.toolFeedbacks().forEach(action -> actions.put(action.getId(), action));
		List<AssistantMessage.ToolCall> automaticallyApproved = new ArrayList<>();
		Set<String> unmatchedDenied = new HashSet<>(denied);
		for (int i = 0; i < calls.size(); i++) {
			var call = calls.get(i);
			var action = actions.remove(keys.get(i));
			if (action == null && !unmatchedDenied.remove(keys.get(i))) {
				automaticallyApproved.add(call);
			}
			else if (action != null && (!call.name().equals(action.getName())
					|| !Objects.equals(call.arguments(), action.getArguments()))) {
				throw new IllegalArgumentException("Tool call '" + keys.get(i) + "' changed after review was requested");
			}
		}
		if (!actions.isEmpty() || !unmatchedDenied.isEmpty()
				|| !automaticallyApproved.equals(request.getToolsAutomaticallyApproved())) {
			throw new IllegalArgumentException("The tool call batch changed after review was requested");
		}
	}

	private static Map<String, ToolFeedback> validateDecisions(InterruptionMetadata request, InterruptionMetadata review) {
		Map<String, ToolFeedback> actions = new HashMap<>();
		request.toolFeedbacks().forEach(action -> actions.put(action.getId(), action));
		Map<String, ToolFeedback> decisions = new HashMap<>();
		for (var decision : review.toolFeedbacks()) {
			String id = decision.getId();
			var action = actions.get(id);
			if (action == null || !action.getName().equals(decision.getName())) {
				throw new IllegalArgumentException("No tool review request matches call '" + id + "'");
			}
			if (decisions.putIfAbsent(id, decision) != null) {
				throw new IllegalArgumentException("Duplicate decision for tool call '" + id + "'");
			}
			var result = decision.getResult();
			if (result == null || !action.getAllowedDecisions().contains(result)) {
				throw new IllegalArgumentException("Decision " + result + " is not allowed for tool '" + action.getName() + "'");
			}
			if (result == FeedbackResult.EDITED) {
				validateEditedArguments(id, decision.getArguments());
			}
			if (result == FeedbackResult.RESPONDED && (decision.getResponse() == null || decision.getResponse().isBlank())) {
				throw new IllegalArgumentException("A human response is required for tool call '" + id + "'");
			}
		}
		return decisions;
	}

	private static void validateEditedArguments(String id, String arguments) {
		String message = "Edited arguments for tool call '" + id + "' must contain valid JSON";
		if (arguments == null || arguments.isBlank()) {
			throw new IllegalArgumentException(message);
		}
		try {
			ARGUMENTS_READER.readTree(arguments);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalArgumentException(message);
		}
	}

	private InterruptionMetadata buildInterruptionMetadata(AssistantMessage lastMessage, OverAllState state,
			RunnableConfig config) {
		var keys = ToolCallDecisions.toolCallKeys(lastMessage).iterator();
		Map<String, AssistantMessage.ToolCall> pending = new LinkedHashMap<>();
		List<AssistantMessage.ToolCall> automaticallyApproved = new ArrayList<>();
		List<String> denied = new ArrayList<>();
		for (var call : lastMessage.getToolCalls()) {
			String key = keys.next();
			var request = ToolCallRequest.builder().toolCall(call).context(config.metadata().orElseGet(Map::of))
					.executionContext(new ToolCallExecutionContext(config, state)).build();
			Decision decision = getAgent() == null ? Decision.ALLOW : getAgent().toolPermission(lastMessage, request, config);
			if (decision == Decision.DENY) {
				denied.add(key);
			}
			else if (requiresConfirmation(lastMessage, call, request, config, decision)) {
				pending.put(key, call);
			}
			else {
				automaticallyApproved.add(call);
			}
		}
		return buildInterruptionMetadata(state, pending, automaticallyApproved, denied);
	}

	private boolean requiresConfirmation(AssistantMessage message, AssistantMessage.ToolCall call,
			ToolCallRequest request, RunnableConfig config, Decision decision) {
		boolean explicitlyConfigured = decision == Decision.REVIEW || requiresConfiguredReview(request);
		return getAgent() != null ? getAgent().requiresToolConfirmation(message, call, config, explicitlyConfigured)
				: explicitlyConfigured;
	}

	public boolean requiresConfiguredReview(ToolCallRequest request) {
		var policy = approvalOn.get(request.getToolName());
		return policy != null && policy.requiresReview(request);
	}

	public static boolean isConfirmed(RunnableConfig config, AssistantMessage.ToolCall call) {
		return config.context().get(CONFIRMED_CALLS) instanceof Map<?, ?> calls && calls.containsValue(call);
	}

	/** Reports whether a saved approval matches the call, or empty if the call has no approval. */
	public static Optional<Boolean> matchesSavedApproval(OverAllState state, String agentName, String key,
			AssistantMessage.ToolCall call) {
		return state.<Map<String, Map<String, String>>>value(confirmedCallsStateKey(agentName))
				.map(calls -> calls.get(key)).map(confirmation -> confirmation.equals(confirmationFields(call)));
	}

	private static Map<String, String> confirmationFields(AssistantMessage.ToolCall call) {
		var values = new HashMap<String, String>();
		values.put("id", call.id());
		values.put("type", call.type());
		values.put("name", call.name());
		values.put("arguments", call.arguments());
		return values;
	}

	public static void removeConfirmedCalls(OverAllState state, String agentName, Map<String, Object> updates) {
		String key = confirmedCallsStateKey(agentName);
		if (state.value(key).isPresent()) {
			updates.put(key, OverAllState.MARK_FOR_REMOVAL);
		}
	}

	private static String confirmedCallsStateKey(String agentName) {
		return CONFIRMED_CALLS + "." + agentName;
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
			Map<String, AssistantMessage.ToolCall> pending, List<AssistantMessage.ToolCall> automaticallyApproved,
			List<String> denied) {
		InterruptionMetadata.Builder builder = InterruptionMetadata.builder(Hook.getFullHookName(this), state)
				.toolsAutomaticallyApproved(automaticallyApproved);
		if (!denied.isEmpty()) {
			builder.addMetadata(DENIED_TOOL_CALL_KEYS, List.copyOf(denied));
		}
		if (!pending.isEmpty()) {
			builder.interruptionId(UUID.randomUUID().toString());
		}
		pending.forEach((key, toolCall) -> {
			ToolConfig toolConfig = approvalOn.get(toolCall.name());
			String description = toolConfig == null ? null : toolConfig.getDescription();
			String content = "The AI is requesting to use the tool: " + toolCall.name() + ".\n"
					+ (description != null ? ("Description: " + description + "\n") : "")
					+ "With the following arguments: " + toolCall.arguments() + "\n"
					+ "Do you approve?";
			builder.addToolFeedback(ToolFeedback.builder().id(key)
					.name(toolCall.name()).description(content).arguments(toolCall.arguments())
					.allowedDecisions(toolConfig == null ? ToolFeedback.DEFAULT_ALLOWED_DECISIONS
							: toolConfig.getAllowedDecisions()).build());
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

	@Override
	public Map<String, KeyStrategy> getKeyStrategys() {
		return Map.of(confirmedCallsStateKey(getAgentName()), new ReplaceStrategy());
	}

	private record ReviewedCalls(Map<String, ToolFeedback> decisions, Set<String> denied) {
	}

	public static class Builder {
		private Map<String, ToolConfig> approvalOn = new HashMap<>();

		public Builder approvalOn(String toolName, ToolConfig toolConfig) {
			this.approvalOn.put(Objects.requireNonNull(toolName), Objects.requireNonNull(toolConfig));
			return this;
		}

		public Builder approvalOn(String toolName, String description) {
			return approvalOn(toolName, ToolConfig.builder().description(description).build());
		}

		public Builder approvalOn(Map<String, ToolConfig> approvalOn) {
			approvalOn.forEach(this::approvalOn);
			return this;
		}

		public HumanInTheLoopHook build() {
			return new HumanInTheLoopHook(this);
		}
	}

}

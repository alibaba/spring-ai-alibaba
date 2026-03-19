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
package com.alibaba.cloud.ai.graph.agent.hook.unknowntool;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.Prioritized;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.serializer.AgentInstructionMessage;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Guards the ReAct loop when the model keeps requesting unknown tools.
 */
@HookPositions({ HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL })
public class UnknownToolGuardHook extends MessagesModelHook {

	/**
	 * Context key used to count consecutive rounds whose last tool execution result is an
	 * "all tool calls unknown" response. The count is reset as soon as the loop returns to
	 * a normal tool response or a non-tool message.
	 */
	private static final String CONSECUTIVE_UNKNOWN_TOOL_COUNT_CONTEXT_KEY =
			"__unknown_tool_guard_consecutive_count__";

	/**
	 * Context key indicating that the guard has switched the current turn into
	 * final-answer mode. In this mode the model should stop calling tools and answer the
	 * user directly.
	 */
	private static final String FINAL_ANSWER_MODE_CONTEXT_KEY =
			"__unknown_tool_guard_final_answer_mode__";

	/**
	 * Context key for counting how many times the model still emitted tool calls after the
	 * guard had already entered final-answer mode.
	 */
	private static final String FINAL_ANSWER_ATTEMPT_COUNT_CONTEXT_KEY =
			"__unknown_tool_guard_final_answer_attempt_count__";

	/**
	 * Default threshold for entering final-answer mode. The model gets one chance to learn
	 * from the unknown-tool feedback, and switches strategy after the second consecutive
	 * unknown-tool round.
	 */
	private static final int DEFAULT_MAX_CONSECUTIVE_UNKNOWN_TOOL_CALLS = 2;

	/**
	 * Maximum number of consecutive "final-answer only" retries before the guard falls back
	 * to scheme B and forcibly terminates the loop with a direct answer path.
	 */
	private static final int MAX_FINAL_ANSWER_ATTEMPTS = 2;

	/**
	 * Interceptor used only for final-answer mode. It strips tool exposure from the model
	 * request so the model can no longer continue the tool-calling loop.
	 */
	private static final ModelInterceptor FINAL_ANSWER_INTERCEPTOR = new UnknownToolFinalAnswerInterceptor();

	/**
	 * Configurable threshold for how many consecutive unknown-tool rounds are tolerated
	 * before switching from self-repair mode to direct-answer mode.
	 */
	private final int maxConsecutiveUnknownToolCalls;

	/**
	 * Optional custom instruction / fallback text supplied by the builder. When configured,
	 * it overrides the built-in final-answer guidance and terminal fallback message.
	 */
	private final String terminationMessage;

	private UnknownToolGuardHook(Builder builder) {
		this.maxConsecutiveUnknownToolCalls = builder.maxConsecutiveUnknownToolCalls;
		this.terminationMessage = builder.terminationMessage;
	}

	public static UnknownToolGuardHook create() {
		return builder().build();
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public String getName() {
		return "unknown_tool_guard";
	}

	@Override
	public int getOrder() {
		return Prioritized.HIGHEST_PRECEDENCE + 100;
	}

	@Override
	public List<JumpTo> canJumpTo() {
		return List.of(JumpTo.model, JumpTo.end);
	}

	@Override
	public List<ModelInterceptor> getModelInterceptors() {
		return List.of(FINAL_ANSWER_INTERCEPTOR);
	}

	@Override
	public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
		if (previousMessages.isEmpty()) {
			resetState(config);
			return new AgentCommand(previousMessages);
		}

		if (isFinalAnswerInstruction(previousMessages.get(previousMessages.size() - 1))) {
			return new AgentCommand(previousMessages);
		}

		Message lastMessage = previousMessages.get(previousMessages.size() - 1);
		if (!(lastMessage instanceof ToolResponseMessage toolResponseMessage)) {
			resetState(config);
			return new AgentCommand(previousMessages);
		}

		if (!isAllToolCallsUnknown(toolResponseMessage.getMetadata())) {
			resetState(config);
			return new AgentCommand(previousMessages);
		}

		int consecutiveCount = getConsecutiveCount(config) + 1;
		config.context().put(CONSECUTIVE_UNKNOWN_TOOL_COUNT_CONTEXT_KEY, consecutiveCount);
		if (consecutiveCount < maxConsecutiveUnknownToolCalls) {
			return new AgentCommand(previousMessages);
		}

		List<String> requestedToolNames = getMetadataStringList(toolResponseMessage.getMetadata(),
				UnknownToolGuardConstants.REQUESTED_TOOL_NAMES_METADATA_KEY);
		List<String> availableToolNames = getMetadataStringList(toolResponseMessage.getMetadata(),
				UnknownToolGuardConstants.AVAILABLE_TOOL_NAMES_METADATA_KEY);
		List<Message> newMessages = new ArrayList<>(previousMessages);
		newMessages.add(createFinalAnswerInstructionMessage(
				buildFinalAnswerInstruction(consecutiveCount, requestedToolNames, availableToolNames)));
		enterFinalAnswerMode(config);
		return new AgentCommand(newMessages);
	}

	@Override
	public AgentCommand afterModel(List<Message> previousMessages, RunnableConfig config) {
		if (!isFinalAnswerMode(config) || previousMessages.isEmpty()) {
			return new AgentCommand(previousMessages);
		}

		Message lastMessage = previousMessages.get(previousMessages.size() - 1);
		if (!(lastMessage instanceof AssistantMessage assistantMessage)) {
			resetState(config);
			return new AgentCommand(previousMessages);
		}

		if (assistantMessage.getToolCalls() == null || assistantMessage.getToolCalls().isEmpty()) {
			resetState(config);
			return new AgentCommand(previousMessages);
		}

		int attemptCount = getFinalAnswerAttemptCount(config) + 1;
		config.context().put(FINAL_ANSWER_ATTEMPT_COUNT_CONTEXT_KEY, attemptCount);
		if (attemptCount >= MAX_FINAL_ANSWER_ATTEMPTS) {
			List<Message> newMessages = new ArrayList<>(previousMessages.subList(0, previousMessages.size() - 1));
			newMessages.add(new AssistantMessage(buildFallbackAnswerMessage()));
			resetState(config);
			return new AgentCommand(JumpTo.end, newMessages);
		}

		List<Message> newMessages = new ArrayList<>(previousMessages.subList(0, previousMessages.size() - 1));
		newMessages.add(createFinalAnswerInstructionMessage(buildRetryFinalAnswerInstruction(attemptCount)));
		return new AgentCommand(JumpTo.model, newMessages);
	}

	private boolean isAllToolCallsUnknown(Map<String, Object> metadata) {
		return metadata.get(UnknownToolGuardConstants.ALL_TOOL_CALLS_UNKNOWN_METADATA_KEY) instanceof Boolean value
				&& value;
	}

	private int getConsecutiveCount(RunnableConfig config) {
		Object value = config.context().get(CONSECUTIVE_UNKNOWN_TOOL_COUNT_CONTEXT_KEY);
		return value instanceof Number number ? number.intValue() : 0;
	}

	private int getFinalAnswerAttemptCount(RunnableConfig config) {
		Object value = config.context().get(FINAL_ANSWER_ATTEMPT_COUNT_CONTEXT_KEY);
		return value instanceof Number number ? number.intValue() : 0;
	}

	private boolean isFinalAnswerMode(RunnableConfig config) {
		return Boolean.TRUE.equals(config.context().get(FINAL_ANSWER_MODE_CONTEXT_KEY));
	}

	private void enterFinalAnswerMode(RunnableConfig config) {
		config.context().put(FINAL_ANSWER_MODE_CONTEXT_KEY, true);
		config.context().put(FINAL_ANSWER_ATTEMPT_COUNT_CONTEXT_KEY, 0);
		config.context().remove(CONSECUTIVE_UNKNOWN_TOOL_COUNT_CONTEXT_KEY);
	}

	private void resetState(RunnableConfig config) {
		config.context().remove(CONSECUTIVE_UNKNOWN_TOOL_COUNT_CONTEXT_KEY);
		config.context().remove(FINAL_ANSWER_MODE_CONTEXT_KEY);
		config.context().remove(FINAL_ANSWER_ATTEMPT_COUNT_CONTEXT_KEY);
	}

	private AgentInstructionMessage createFinalAnswerInstructionMessage(String text) {
		return AgentInstructionMessage.builder()
				.text(text)
				.metadata(Map.of(UnknownToolGuardConstants.FINAL_ANSWER_INSTRUCTION_METADATA_KEY, true))
				.build();
	}

	private boolean isFinalAnswerInstruction(Message message) {
		return message instanceof AgentInstructionMessage instructionMessage
				&& Boolean.TRUE.equals(instructionMessage.getMetadata()
						.get(UnknownToolGuardConstants.FINAL_ANSWER_INSTRUCTION_METADATA_KEY));
	}

	private List<String> getMetadataStringList(Map<String, Object> metadata, String key) {
		Object value = metadata.get(key);
		if (value instanceof List<?> list) {
			return list.stream().filter(String.class::isInstance).map(String.class::cast).toList();
		}
		return List.of();
	}

	private String buildFinalAnswerInstruction(int consecutiveCount, List<String> requestedToolNames,
			List<String> availableToolNames) {
		if (StringUtils.hasText(terminationMessage)) {
			return terminationMessage;
		}

		String requested = requestedToolNames.isEmpty() ? "[]" : requestedToolNames.toString();
		String available = availableToolNames.isEmpty() ? "[]" : availableToolNames.toString();
		return "You requested unknown tools for " + consecutiveCount
				+ " consecutive rounds. Requested tools: " + requested + ". Available tools: " + available
				+ ". Tool calling is now disabled for this turn to avoid an infinite loop. Do not call any tool again. "
				+ "Answer the user directly with the current context, and briefly explain any limitation if necessary.";
	}

	private String buildRetryFinalAnswerInstruction(int attemptCount) {
		return "Tool calling is already disabled because of repeated unknown tool requests. This is retry #"
				+ attemptCount
				+ ". Do not call any tool. Answer the user directly right now using the available conversation context only.";
	}

	private String buildFallbackAnswerMessage() {
		if (StringUtils.hasText(terminationMessage)) {
			return terminationMessage;
		}
		return "I could not continue with tool calls because the requested tools were unavailable, and I was still unable to produce a direct answer without tools.";
	}

	public static final class Builder {

		/**
		 * See {@link UnknownToolGuardHook#maxConsecutiveUnknownToolCalls}.
		 */
		private int maxConsecutiveUnknownToolCalls = DEFAULT_MAX_CONSECUTIVE_UNKNOWN_TOOL_CALLS;

		/**
		 * See {@link UnknownToolGuardHook#terminationMessage}.
		 */
		private String terminationMessage;

		private Builder() {
		}

		public Builder maxConsecutiveUnknownToolCalls(int maxConsecutiveUnknownToolCalls) {
			if (maxConsecutiveUnknownToolCalls < 1) {
				throw new IllegalArgumentException("maxConsecutiveUnknownToolCalls must be at least 1");
			}
			this.maxConsecutiveUnknownToolCalls = maxConsecutiveUnknownToolCalls;
			return this;
		}

		public Builder terminationMessage(String terminationMessage) {
			this.terminationMessage = terminationMessage;
			return this;
		}

		public UnknownToolGuardHook build() {
			return new UnknownToolGuardHook(this);
		}

	}

}

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
package com.alibaba.cloud.ai.graph.agent.hook.toolexecutionfailure;

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
 * Guards the ReAct loop when tool execution keeps failing across rounds.
 *
 * <p>The guard works in two phases:</p>
 * <ol>
 * <li>Allow the model to self-repair after receiving a structured tool-execution failure.</li>
 * <li>If consecutive rounds still fail, switch to a tool-disabled final-answer mode and
 * ask the model to answer directly.</li>
 * </ol>
 */
@HookPositions({ HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL })
public class ToolExecutionFailureGuardHook extends MessagesModelHook {

	private static final String CONSECUTIVE_FAILURE_COUNT_CONTEXT_KEY =
			"__tool_execution_failure_guard_consecutive_count__";

	private static final String FINAL_ANSWER_MODE_CONTEXT_KEY =
			"__tool_execution_failure_guard_final_answer_mode__";

	private static final int DEFAULT_MAX_CONSECUTIVE_EXECUTION_FAILURE_ROUNDS = 2;

	private static final ModelInterceptor FINAL_ANSWER_INTERCEPTOR =
			new ToolExecutionFailureFinalAnswerInterceptor();

	private final int maxConsecutiveExecutionFailureRounds;

	private final String terminationMessage;

	private ToolExecutionFailureGuardHook(Builder builder) {
		this.maxConsecutiveExecutionFailureRounds = builder.maxConsecutiveExecutionFailureRounds;
		this.terminationMessage = builder.terminationMessage;
	}

	public static ToolExecutionFailureGuardHook create() {
		return builder().build();
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public String getName() {
		return "tool_execution_failure_guard";
	}

	@Override
	public int getOrder() {
		return Prioritized.HIGHEST_PRECEDENCE + 110;
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

		if (!isAllToolCallsFailed(toolResponseMessage.getMetadata())) {
			resetState(config);
			return new AgentCommand(previousMessages);
		}

		int consecutiveCount = getConsecutiveCount(config) + 1;
		config.context().put(CONSECUTIVE_FAILURE_COUNT_CONTEXT_KEY, consecutiveCount);
		if (consecutiveCount < maxConsecutiveExecutionFailureRounds) {
			return new AgentCommand(previousMessages);
		}

		List<String> failedToolNames = getMetadataStringList(toolResponseMessage.getMetadata(),
				ToolExecutionFailureGuardConstants.FAILED_TOOL_NAMES_METADATA_KEY);
		List<String> failureTypes = getMetadataStringList(toolResponseMessage.getMetadata(),
				ToolExecutionFailureGuardConstants.FAILURE_TYPES_METADATA_KEY);
		List<Message> newMessages = new ArrayList<>(previousMessages);
		newMessages.add(createFinalAnswerInstructionMessage(
				buildFinalAnswerInstruction(consecutiveCount, failedToolNames, failureTypes)));
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

		if (!assistantMessage.hasToolCalls()) {
			resetState(config);
			return new AgentCommand(previousMessages);
		}

		List<Message> newMessages = new ArrayList<>(previousMessages.subList(0, previousMessages.size() - 1));
		newMessages.add(new AssistantMessage(buildFallbackAnswerMessage()));
		resetState(config);
		return new AgentCommand(JumpTo.end, newMessages);
	}

	private boolean isAllToolCallsFailed(Map<String, Object> metadata) {
		return metadata.get(ToolExecutionFailureGuardConstants.ALL_TOOL_CALLS_FAILED_METADATA_KEY) instanceof Boolean value
				&& value;
	}

	private int getConsecutiveCount(RunnableConfig config) {
		Object value = config.context().get(CONSECUTIVE_FAILURE_COUNT_CONTEXT_KEY);
		return value instanceof Number number ? number.intValue() : 0;
	}

	private boolean isFinalAnswerMode(RunnableConfig config) {
		return Boolean.TRUE.equals(config.context().get(FINAL_ANSWER_MODE_CONTEXT_KEY));
	}

	private void enterFinalAnswerMode(RunnableConfig config) {
		config.context().put(FINAL_ANSWER_MODE_CONTEXT_KEY, true);
		config.context().remove(CONSECUTIVE_FAILURE_COUNT_CONTEXT_KEY);
	}

	private void resetState(RunnableConfig config) {
		config.context().remove(CONSECUTIVE_FAILURE_COUNT_CONTEXT_KEY);
		config.context().remove(FINAL_ANSWER_MODE_CONTEXT_KEY);
	}

	private AgentInstructionMessage createFinalAnswerInstructionMessage(String text) {
		return AgentInstructionMessage.builder()
				.text(text)
				.metadata(Map.of(ToolExecutionFailureGuardConstants.FINAL_ANSWER_INSTRUCTION_METADATA_KEY, true))
				.build();
	}

	private boolean isFinalAnswerInstruction(Message message) {
		return message instanceof AgentInstructionMessage instructionMessage
				&& Boolean.TRUE.equals(instructionMessage.getMetadata()
						.get(ToolExecutionFailureGuardConstants.FINAL_ANSWER_INSTRUCTION_METADATA_KEY));
	}

	private List<String> getMetadataStringList(Map<String, Object> metadata, String key) {
		Object value = metadata.get(key);
		if (value instanceof List<?> list) {
			return list.stream().filter(String.class::isInstance).map(String.class::cast).toList();
		}
		return List.of();
	}

	private String buildFinalAnswerInstruction(int consecutiveCount, List<String> failedToolNames,
			List<String> failureTypes) {
		if (StringUtils.hasText(terminationMessage)) {
			return terminationMessage;
		}

		String failedTools = failedToolNames.isEmpty() ? "[]" : failedToolNames.toString();
		String failureTypeText = failureTypes.isEmpty() ? "[]" : failureTypes.toString();
		return "Tool execution failed for " + consecutiveCount + " consecutive rounds. Failed tools: "
				+ failedTools + ". Failure types: " + failureTypeText
				+ ". Tool calling is now disabled for this turn to avoid an infinite loop. Do not call any tool again. "
				+ "Answer the user directly with the current context, and briefly explain any limitation if necessary.";
	}

	private String buildFallbackAnswerMessage() {
		if (StringUtils.hasText(terminationMessage)) {
			return terminationMessage;
		}
		return "I could not continue with tool calls because tool execution kept failing, and I was still unable to produce a direct answer without tools.";
	}

	public static final class Builder {

		private int maxConsecutiveExecutionFailureRounds = DEFAULT_MAX_CONSECUTIVE_EXECUTION_FAILURE_ROUNDS;

		private String terminationMessage;

		private Builder() {
		}

		public Builder maxConsecutiveExecutionFailureRounds(int maxConsecutiveExecutionFailureRounds) {
			if (maxConsecutiveExecutionFailureRounds < 1) {
				throw new IllegalArgumentException("maxConsecutiveExecutionFailureRounds must be at least 1");
			}
			this.maxConsecutiveExecutionFailureRounds = maxConsecutiveExecutionFailureRounds;
			return this;
		}

		public Builder terminationMessage(String terminationMessage) {
			this.terminationMessage = terminationMessage;
			return this;
		}

		public ToolExecutionFailureGuardHook build() {
			return new ToolExecutionFailureGuardHook(this);
		}

	}

}


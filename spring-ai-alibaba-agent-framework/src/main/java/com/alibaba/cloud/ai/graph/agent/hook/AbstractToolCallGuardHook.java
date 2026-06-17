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
package com.alibaba.cloud.ai.graph.agent.hook;

import com.alibaba.cloud.ai.graph.RunnableConfig;
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
 * Abstract base class for tool call guard hooks that prevent infinite loops when the
 * model repeatedly emits tool calls that cannot be completed successfully.
 *
 * <p>
 * Subclasses implement the specific detection logic and messaging for different types
 * of tool call failures:
 * </p>
 * <ul>
 * <li>UnknownToolGuardHook - tool not found in agent's available tools</li>
 * <li>ToolExecutionFailureGuardHook - tool exists but execution fails</li>
 * </ul>
 *
 * <p>
 * The guard works in two phases:
 * </p>
 * <ol>
 * <li>Allow the model to self-repair after receiving a failure response. Subclasses
 * decide how many additional retries are allowed through {@link #getMaxSelfRepairRetries()}.</li>
 * <li>If consecutive rounds still fail after those retries are exhausted, switch to a tool-disabled final-answer mode
 * and ask the model to answer directly.</li>
 * </ol>
 */
public abstract class AbstractToolCallGuardHook extends MessagesModelHook {

	/**
	 * This hook can either continue to the next model turn or terminate the agent once a
	 * fallback answer has been produced.
	 * @return the jump targets supported by this guard hook
	 */
	@Override
	public final List<JumpTo> canJumpTo() {
		return List.of(JumpTo.model, JumpTo.end);
	}

	/**
	 * Runs before each model invocation to evaluate the previous round.
	 * <p>
	 * The method only escalates when the last message is a {@link ToolResponseMessage} and that
	 * response indicates this guard's failure type. In that case it increments the consecutive
	 * failure counter and, once retries are exhausted, appends a synthetic final-answer instruction
	 * that will be seen by the next model call.
	 * </p>
	 * <p>
	 * Any non-failure round resets the guard state so future failures start from a clean counter.
	 * </p>
	 * @param previousMessages the messages accumulated so far, including the previous tool response
	 * @param config the runnable config whose context stores the guard state
	 * @return either the unchanged messages, or a new list with an injected final-answer instruction
	 */
	@Override
	public final AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
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
		config.context().put(getConsecutiveCountContextKey(), consecutiveCount);
		if (consecutiveCount <= getMaxSelfRepairRetries()) {
			return new AgentCommand(previousMessages);
		}

		List<Message> newMessages = new ArrayList<>(previousMessages);
		newMessages.add(createFinalAnswerInstructionMessage(
				buildFinalAnswerInstruction(consecutiveCount, toolResponseMessage)));
		enterFinalAnswerMode(config);
		return new AgentCommand(newMessages);
	}

	/**
	 * Runs after the model has produced the current turn.
	 * <p>
	 * This method only becomes active after {@link #beforeModel(List, RunnableConfig)} has entered
	 * final-answer mode. At that point the model is expected to answer directly without any tool
	 * calls. If the model complies, the guard state is cleared. If it still emits tool calls, the
	 * hook replaces that output with a fallback answer and jumps to {@link JumpTo#end}.
	 * </p>
	 * @param previousMessages the current message list, including the latest assistant output
	 * @param config the runnable config whose context stores the guard state
	 * @return either the unchanged messages, or a terminating fallback answer
	 */
	@Override
	public final AgentCommand afterModel(List<Message> previousMessages, RunnableConfig config) {
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

	/**
	 * Return unique hook name for logging/debugging.
	 * @return the hook name
	 */
	public abstract String getName();

	/**
	 * Return hook execution order priority.
	 * @return the hook order
	 */
	public abstract int getOrder();

	/**
	 * Return model interceptors used by this hook.
	 * <p>
	 * Concrete guards typically provide an interceptor that recognizes the synthetic final-answer
	 * instruction injected by {@link #beforeModel(List, RunnableConfig)} and disables tool exposure
	 * for that specific model turn.
	 * </p>
	 * @return the model interceptors
	 */
	public abstract List<ModelInterceptor> getModelInterceptors();

	/**
	 * Get the metadata key used to detect that all tool calls in the previous tool round failed
	 * for this guard's failure category.
	 * @return the metadata key
	 */
	protected abstract String getMetadataKeyForAllFailures();

	/**
	 * Get the metadata key used to mark the synthetic {@link AgentInstructionMessage} injected when
	 * the hook enters final-answer mode.
	 * @return the metadata key
	 */
	protected abstract String getFinalAnswerInstructionMetadataKey();

	/**
	 * Get the runnable-context key that stores the consecutive failure counter for this guard.
	 * @return the context key
	 */
	protected abstract String getConsecutiveCountContextKey();

	/**
	 * Get the runnable-context key that stores whether the guard has entered final-answer mode.
	 * @return the context key
	 */
	protected abstract String getFinalAnswerModeContextKey();

	/**
	 * Build the instruction appended when the retry budget has been exhausted.
	 * <p>
	 * This text is not sent to the user directly. Instead, it is injected as a synthetic
	 * {@link AgentInstructionMessage} so the next model turn knows why tools have been disabled and
	 * how it should answer.
	 * </p>
	 * @param consecutiveCount the number of consecutive failures
	 * @param toolResponseMessage the tool response message containing failure metadata
	 * @return the instruction message text
	 */
	protected abstract String buildFinalAnswerInstruction(int consecutiveCount,
			ToolResponseMessage toolResponseMessage);

	/**
	 * Build the fallback answer returned to the user when the model still calls tools in
	 * final-answer mode.
	 * <p>
	 * This message is only used as a last resort when the model ignored the synthetic final-answer
	 * instruction and attempted another tool round anyway.
	 * </p>
	 * @return the fallback answer message
	 */
	protected abstract String buildFallbackAnswerMessage();

	/**
	 * Get how many additional self-repair retries are allowed after the first failed round
	 * before entering final-answer mode.
	 * <p>
	 * Example: {@code 0} means the hook escalates immediately after the first failure round;
	 * {@code 1} means one additional self-repair attempt is allowed before escalation.
	 * </p>
	 * @return the maximum self-repair retries allowed
	 */
	protected abstract int getMaxSelfRepairRetries();

	/**
	 * Get an optional custom instruction injected into the synthetic final-answer turn.
	 * @return the custom final-answer instruction, or null if not set
	 */
	protected abstract String getCustomFinalAnswerInstruction();

	/**
	 * Get an optional custom fallback reply returned to the user when the model still
	 * emits tool calls in final-answer mode.
	 * @return the custom fallback answer message, or null if not set
	 */
	protected abstract String getCustomFallbackAnswerMessage();

	/**
	 * Check whether a custom final-answer instruction is configured.
	 * @return true when a non-blank custom instruction is present
	 */
	protected final boolean hasCustomFinalAnswerInstruction() {
		return StringUtils.hasText(getCustomFinalAnswerInstruction());
	}

	/**
	 * Resolve the final-answer instruction text, preferring a custom instruction when configured.
	 * @param defaultMessage the built-in final-answer instruction
	 * @return the custom instruction if configured, otherwise {@code defaultMessage}
	 */
	protected final String getCustomFinalAnswerInstructionOrDefault(String defaultMessage) {
		return hasCustomFinalAnswerInstruction() ? getCustomFinalAnswerInstruction() : defaultMessage;
	}

	/**
	 * Check whether a custom fallback answer message is configured.
	 * @return true when a non-blank custom fallback answer message is present
	 */
	protected final boolean hasCustomFallbackAnswerMessage() {
		return StringUtils.hasText(getCustomFallbackAnswerMessage());
	}

	/**
	 * Resolve the fallback reply text, preferring a custom fallback answer when configured.
	 * @param defaultMessage the built-in fallback reply
	 * @return the custom fallback answer if configured, otherwise {@code defaultMessage}
	 */
	protected final String getCustomFallbackAnswerMessageOrDefault(String defaultMessage) {
		return hasCustomFallbackAnswerMessage() ? getCustomFallbackAnswerMessage() : defaultMessage;
	}

	/**
	 * Utility to extract String list from metadata with type safety.
	 * @param metadata the metadata map
	 * @param key the metadata key
	 * @return the string list, or empty list if not found or invalid type
	 */
	protected List<String> getMetadataStringList(Map<String, Object> metadata, String key) {
		Object value = metadata.get(key);
		if (value instanceof List<?> list) {
			return list.stream().filter(String.class::isInstance).map(String.class::cast).toList();
		}
		return List.of();
	}

	private boolean isAllToolCallsFailed(Map<String, Object> metadata) {
		// Check guard-specific flag first (all failures are of this guard's type)
		if (metadata.get(getMetadataKeyForAllFailures()) instanceof Boolean value && value) {
			return true;
		}
		// Fall back to the unified flag that covers mixed-failure scenarios
		// e.g. some unknown-tool + some execution-failure in the same batch.
		// Only activate if this guard's error type is present in the batch.
		if (metadata.get(ToolCallGuardConstants.ALL_TOOL_CALLS_ERRORED_METADATA_KEY) instanceof Boolean allErrored
				&& allErrored) {
			return hasOwnErrorTypePresent(metadata);
		}
		return false;
	}

	/**
	 * Check whether failures of this guard's specific type are present in the metadata.
	 * Subclasses can override this if the default metadata key check is insufficient.
	 * @param metadata the metadata map from the ToolResponseMessage
	 * @return true if this guard's error type is represented in the response batch
	 */
	protected boolean hasOwnErrorTypePresent(Map<String, Object> metadata) {
		// Default: if the guard-specific "all" flag exists (even as false), there are
		// failures of this type in the batch
		return metadata.containsKey(getMetadataKeyForAllFailures());
	}

	private int getConsecutiveCount(RunnableConfig config) {
		Object value = config.context().get(getConsecutiveCountContextKey());
		return value instanceof Number number ? number.intValue() : 0;
	}

	private boolean isFinalAnswerMode(RunnableConfig config) {
		return Boolean.TRUE.equals(config.context().get(getFinalAnswerModeContextKey()));
	}

	private void enterFinalAnswerMode(RunnableConfig config) {
		config.context().put(getFinalAnswerModeContextKey(), true);
		config.context().remove(getConsecutiveCountContextKey());
	}

	private void resetState(RunnableConfig config) {
		config.context().remove(getConsecutiveCountContextKey());
		config.context().remove(getFinalAnswerModeContextKey());
	}

	private AgentInstructionMessage createFinalAnswerInstructionMessage(String text) {
		return AgentInstructionMessage.builder()
				.text(text)
				.metadata(Map.of(getFinalAnswerInstructionMetadataKey(), true))
				.build();
	}

	private boolean isFinalAnswerInstruction(Message message) {
		return message instanceof AgentInstructionMessage instructionMessage
				&& Boolean.TRUE.equals(instructionMessage.getMetadata()
						.get(getFinalAnswerInstructionMetadataKey()));
	}

}

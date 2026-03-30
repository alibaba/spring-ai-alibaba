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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for tool call guard hooks that prevent infinite loops when the
 * model repeatedly requests tools that fail.
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
 * <li>Allow the model to self-repair after receiving a failure response.</li>
 * <li>If consecutive rounds still fail, switch to a tool-disabled final-answer mode
 * and ask the model to answer directly.</li>
 * </ol>
 *
 * <p>
 * Once final-answer mode is entered, the next model turn is expected to answer
 * directly. If the model still insists on emitting tool calls, the guard
 * terminates immediately with a fallback answer instead of spending extra tokens
 * on another retry.
 * </p>
 */
public abstract class AbstractToolCallGuardHook extends MessagesModelHook {

	@Override
	public final List<JumpTo> canJumpTo() {
		return List.of(JumpTo.model, JumpTo.end);
	}

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
		if (consecutiveCount < getMaxConsecutiveFailures()) {
			return new AgentCommand(previousMessages);
		}

		List<Message> newMessages = new ArrayList<>(previousMessages);
		newMessages.add(createFinalAnswerInstructionMessage(
				buildFinalAnswerInstruction(consecutiveCount, toolResponseMessage)));
		enterFinalAnswerMode(config);
		return new AgentCommand(newMessages);
	}

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
	 * Return list of model interceptors for this hook.
	 * @return the model interceptors
	 */
	public abstract List<ModelInterceptor> getModelInterceptors();

	/**
	 * Get metadata key used in ToolResponseMessage to detect if all tool calls failed.
	 * @return the metadata key
	 */
	protected abstract String getMetadataKeyForAllFailures();

	/**
	 * Get metadata key used in AgentInstructionMessage to mark final-answer mode.
	 * @return the metadata key
	 */
	protected abstract String getFinalAnswerInstructionMetadataKey();

	/**
	 * Get context key for consecutive failure counter.
	 * @return the context key
	 */
	protected abstract String getConsecutiveCountContextKey();

	/**
	 * Get context key for final-answer mode flag.
	 * @return the context key
	 */
	protected abstract String getFinalAnswerModeContextKey();

	/**
	 * Build instruction message when entering final-answer mode. Subclass extracts needed
	 * metadata from ToolResponseMessage using protected utility methods.
	 * @param consecutiveCount the number of consecutive failures
	 * @param toolResponseMessage the tool response message containing failure metadata
	 * @return the instruction message text
	 */
	protected abstract String buildFinalAnswerInstruction(int consecutiveCount,
			ToolResponseMessage toolResponseMessage);

	/**
	 * Build fallback answer when model still calls tools in final-answer mode.
	 * @return the fallback answer message
	 */
	protected abstract String buildFallbackAnswerMessage();

	/**
	 * Get threshold for entering final-answer mode.
	 * @return the maximum consecutive failures allowed
	 */
	protected abstract int getMaxConsecutiveFailures();

	/**
	 * Get optional custom termination message.
	 * @return the custom termination message, or null if not set
	 */
	protected abstract String getTerminationMessage();

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

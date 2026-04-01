/*
 * Copyright 2024-2026 original author or authors.
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

import com.alibaba.cloud.ai.graph.agent.Prioritized;
import com.alibaba.cloud.ai.graph.agent.hook.AbstractToolCallGuardHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;

import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.List;

/**
 * Guards the ReAct loop when tool execution keeps failing across rounds.
 *
 * <p>The guard works in two phases:</p>
 * <ol>
 * <li>Allow the model to self-repair after receiving a structured tool-execution failure.</li>
 * <li>If consecutive rounds still fail, switch to a tool-disabled final-answer mode and
 * ask the model to answer directly.</li>
 * </ol>
 *
 * <p>Configure the guard with {@link Builder#maxSelfRepairRetries(int)}.
 * A value of {@code 1} means that after the first failed tool-execution round, the model
 * gets one additional self-repair attempt before the guard switches to final-answer mode.</p>
 */
@HookPositions({ HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL })
public class ToolExecutionFailureGuardHook extends AbstractToolCallGuardHook {

	private static final int DEFAULT_MAX_SELF_REPAIR_RETRIES = 1;

	private static final ModelInterceptor FINAL_ANSWER_INTERCEPTOR =
			new ToolExecutionFailureFinalAnswerInterceptor();

	private final int maxSelfRepairRetries;

	private final String customFinalAnswerInstruction;

	private final String customFallbackAnswerMessage;

	private ToolExecutionFailureGuardHook(Builder builder) {
		this.maxSelfRepairRetries = builder.maxSelfRepairRetries;
		this.customFinalAnswerInstruction = builder.customFinalAnswerInstruction;
		this.customFallbackAnswerMessage = builder.customFallbackAnswerMessage;
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
	public List<ModelInterceptor> getModelInterceptors() {
		return List.of(FINAL_ANSWER_INTERCEPTOR);
	}

	@Override
	protected String getMetadataKeyForAllFailures() {
		return ToolExecutionFailureGuardConstants.ALL_TOOL_CALLS_FAILED_METADATA_KEY;
	}

	@Override
	protected String getFinalAnswerInstructionMetadataKey() {
		return ToolExecutionFailureGuardConstants.FINAL_ANSWER_INSTRUCTION_METADATA_KEY;
	}

	@Override
	protected String getConsecutiveCountContextKey() {
		return "__tool_execution_failure_guard_consecutive_count__";
	}

	@Override
	protected String getFinalAnswerModeContextKey() {
		return "__tool_execution_failure_guard_final_answer_mode__";
	}

	@Override
	protected int getMaxSelfRepairRetries() {
		return maxSelfRepairRetries;
	}

	@Override
	protected String getCustomFinalAnswerInstruction() {
		return customFinalAnswerInstruction;
	}

	@Override
	protected String getCustomFallbackAnswerMessage() {
		return customFallbackAnswerMessage;
	}

	@Override
	protected String buildFinalAnswerInstruction(int consecutiveCount, ToolResponseMessage toolResponseMessage) {
		List<String> failedToolNames = getMetadataStringList(toolResponseMessage.getMetadata(),
				ToolExecutionFailureGuardConstants.FAILED_TOOL_NAMES_METADATA_KEY);
		List<String> failureTypes = getMetadataStringList(toolResponseMessage.getMetadata(),
				ToolExecutionFailureGuardConstants.FAILURE_TYPES_METADATA_KEY);

		String failedTools = failedToolNames.isEmpty() ? "[]" : failedToolNames.toString();
		String failureTypeText = failureTypes.isEmpty() ? "[]" : failureTypes.toString();
		return getCustomFinalAnswerInstructionOrDefault(
				"Tool execution failed for " + consecutiveCount + " consecutive rounds. Failed tools: "
						+ failedTools + ". Failure types: " + failureTypeText
						+ ". Tool calling is now disabled for this turn to avoid an infinite loop. Do not call any tool again. "
						+ "Answer the user directly with the current context, and briefly explain any limitation if necessary.");
	}

	@Override
	protected String buildFallbackAnswerMessage() {
		return getCustomFallbackAnswerMessageOrDefault(
				"I had to stop calling tools because tool execution kept failing repeatedly, " +
						"and I could not safely complete your request without them in this turn. " +
						"Would you like me to continue with a best-effort answer based on the current context, " +
						"or would you prefer to adjust the tool setup and try again?");
	}

	public static final class Builder {

		private int maxSelfRepairRetries = DEFAULT_MAX_SELF_REPAIR_RETRIES;

		private String customFinalAnswerInstruction;

		private String customFallbackAnswerMessage;

		private Builder() {
		}

		/**
		 * Configure how many additional self-repair retries are allowed after the first
		 * failed tool-execution round.
		 * <p>
		 * Examples:
		 * </p>
		 * <ul>
		 * <li>{@code 0}: enter final-answer mode immediately after the first failed round</li>
		 * <li>{@code 1}: allow one self-repair retry after the first failed round (default)</li>
		 * <li>{@code 2}: allow two self-repair retries after the first failed round</li>
		 * </ul>
		 * @param maxSelfRepairRetries the number of additional self-repair retries, must be at least 0
		 * @return this builder
		 */
		public Builder maxSelfRepairRetries(int maxSelfRepairRetries) {
			if (maxSelfRepairRetries < 0) {
				throw new IllegalArgumentException("maxSelfRepairRetries must be at least 0");
			}
			this.maxSelfRepairRetries = maxSelfRepairRetries;
			return this;
		}

		public Builder customFinalAnswerInstruction(String customFinalAnswerInstruction) {
			this.customFinalAnswerInstruction = customFinalAnswerInstruction;
			return this;
		}

		public Builder customFallbackAnswerMessage(String customFallbackAnswerMessage) {
			this.customFallbackAnswerMessage = customFallbackAnswerMessage;
			return this;
		}

		public ToolExecutionFailureGuardHook build() {
			return new ToolExecutionFailureGuardHook(this);
		}

	}

}

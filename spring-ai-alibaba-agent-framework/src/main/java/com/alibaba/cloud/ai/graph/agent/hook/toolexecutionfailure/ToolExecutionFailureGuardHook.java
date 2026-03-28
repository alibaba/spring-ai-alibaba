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
import org.springframework.util.StringUtils;

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
 */
@HookPositions({ HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL })
public class ToolExecutionFailureGuardHook extends AbstractToolCallGuardHook {

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
	protected int getMaxConsecutiveFailures() {
		return maxConsecutiveExecutionFailureRounds;
	}

	@Override
	protected String getTerminationMessage() {
		return terminationMessage;
	}

	@Override
	protected String buildFinalAnswerInstruction(int consecutiveCount, ToolResponseMessage toolResponseMessage) {
		if (StringUtils.hasText(terminationMessage)) {
			return terminationMessage;
		}

		List<String> failedToolNames = getMetadataStringList(toolResponseMessage.getMetadata(),
				ToolExecutionFailureGuardConstants.FAILED_TOOL_NAMES_METADATA_KEY);
		List<String> failureTypes = getMetadataStringList(toolResponseMessage.getMetadata(),
				ToolExecutionFailureGuardConstants.FAILURE_TYPES_METADATA_KEY);

		String failedTools = failedToolNames.isEmpty() ? "[]" : failedToolNames.toString();
		String failureTypeText = failureTypes.isEmpty() ? "[]" : failureTypes.toString();
		return "Tool execution failed for " + consecutiveCount + " consecutive rounds. Failed tools: "
				+ failedTools + ". Failure types: " + failureTypeText
				+ ". Tool calling is now disabled for this turn to avoid an infinite loop. Do not call any tool again. "
				+ "Answer the user directly with the current context, and briefly explain any limitation if necessary.";
	}

	@Override
	protected String buildFallbackAnswerMessage() {
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

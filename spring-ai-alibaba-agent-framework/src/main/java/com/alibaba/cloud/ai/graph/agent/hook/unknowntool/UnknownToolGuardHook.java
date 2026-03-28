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

import com.alibaba.cloud.ai.graph.agent.Prioritized;
import com.alibaba.cloud.ai.graph.agent.hook.AbstractToolCallGuardHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;

import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Guards the ReAct loop when the model keeps requesting unknown tools.
 *
 * <p>
 * The guard works in two phases:
 * </p>
 * <ol>
 * <li>Allow the model to self-repair after receiving an unknown-tool error.</li>
 * <li>If unknown-tool rounds keep happening, switch to a tool-disabled final-answer mode
 * and ask the model to answer directly.</li>
 * </ol>
 *
 * <p>
 * Once final-answer mode is entered, the next model turn is expected to answer directly.
 * If the model still insists on emitting tool calls, the guard terminates immediately with
 * a fallback answer instead of spending extra tokens on another retry.
 * </p>
 */
@HookPositions({ HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL })
public class UnknownToolGuardHook extends AbstractToolCallGuardHook {

	private static final int DEFAULT_MAX_CONSECUTIVE_UNKNOWN_TOOL_CALLS = 2;

	private static final ModelInterceptor FINAL_ANSWER_INTERCEPTOR = new UnknownToolFinalAnswerInterceptor();

	private final int maxConsecutiveUnknownToolCalls;

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
	public List<ModelInterceptor> getModelInterceptors() {
		return List.of(FINAL_ANSWER_INTERCEPTOR);
	}

	@Override
	protected String getMetadataKeyForAllFailures() {
		return UnknownToolGuardConstants.ALL_TOOL_CALLS_UNKNOWN_METADATA_KEY;
	}

	@Override
	protected String getFinalAnswerInstructionMetadataKey() {
		return UnknownToolGuardConstants.FINAL_ANSWER_INSTRUCTION_METADATA_KEY;
	}

	@Override
	protected String getConsecutiveCountContextKey() {
		return "__unknown_tool_guard_consecutive_count__";
	}

	@Override
	protected String getFinalAnswerModeContextKey() {
		return "__unknown_tool_guard_final_answer_mode__";
	}

	@Override
	protected int getMaxConsecutiveFailures() {
		return maxConsecutiveUnknownToolCalls;
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

		List<String> requestedToolNames = getMetadataStringList(toolResponseMessage.getMetadata(),
				UnknownToolGuardConstants.REQUESTED_TOOL_NAMES_METADATA_KEY);
		List<String> availableToolNames = getMetadataStringList(toolResponseMessage.getMetadata(),
				UnknownToolGuardConstants.AVAILABLE_TOOL_NAMES_METADATA_KEY);

		String requested = requestedToolNames.isEmpty() ? "[]" : requestedToolNames.toString();
		String available = availableToolNames.isEmpty() ? "[]" : availableToolNames.toString();
		return "You requested unknown tools for " + consecutiveCount
				+ " consecutive rounds. Requested tools: " + requested + ". Available tools: " + available
				+ ". Tool calling is now disabled for this turn to avoid an infinite loop. Do not call any tool again. "
				+ "Answer the user directly with the current context, and briefly explain any limitation if necessary.";
	}

	@Override
	protected String buildFallbackAnswerMessage() {
		if (StringUtils.hasText(terminationMessage)) {
			return terminationMessage;
		}
		return "I could not continue with tool calls because the requested tools were unavailable, and I was still unable to produce a direct answer without tools.";
	}

	public static final class Builder {

		private int maxConsecutiveUnknownToolCalls = DEFAULT_MAX_CONSECUTIVE_UNKNOWN_TOOL_CALLS;

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

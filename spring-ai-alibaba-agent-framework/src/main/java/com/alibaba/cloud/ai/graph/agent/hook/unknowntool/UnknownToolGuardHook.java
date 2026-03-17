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

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Stops the ReAct loop when the model keeps requesting unknown tools.
 */
@HookPositions({ HookPosition.BEFORE_MODEL })
public class UnknownToolGuardHook extends MessagesModelHook {

	private static final String CONSECUTIVE_UNKNOWN_TOOL_COUNT_CONTEXT_KEY =
			"__unknown_tool_guard_consecutive_count__";

	private static final int DEFAULT_MAX_CONSECUTIVE_UNKNOWN_TOOL_CALLS = 2;

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
	public List<JumpTo> canJumpTo() {
		return List.of(JumpTo.end);
	}

	@Override
	public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
		if (previousMessages.isEmpty()) {
			resetCounter(config);
			return new AgentCommand(previousMessages);
		}

		Message lastMessage = previousMessages.get(previousMessages.size() - 1);
		if (!(lastMessage instanceof ToolResponseMessage toolResponseMessage)) {
			resetCounter(config);
			return new AgentCommand(previousMessages);
		}

		if (!isAllToolCallsUnknown(toolResponseMessage.getMetadata())) {
			resetCounter(config);
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
		newMessages.add(AssistantMessage.builder()
				.content(buildTerminationMessage(consecutiveCount, requestedToolNames, availableToolNames))
				.build());
		resetCounter(config);
		return new AgentCommand(JumpTo.end, newMessages);
	}

	private boolean isAllToolCallsUnknown(Map<String, Object> metadata) {
		return metadata.get(UnknownToolGuardConstants.ALL_TOOL_CALLS_UNKNOWN_METADATA_KEY) instanceof Boolean value
				&& value;
	}

	private int getConsecutiveCount(RunnableConfig config) {
		Object value = config.context().get(CONSECUTIVE_UNKNOWN_TOOL_COUNT_CONTEXT_KEY);
		return value instanceof Number number ? number.intValue() : 0;
	}

	private void resetCounter(RunnableConfig config) {
		config.context().remove(CONSECUTIVE_UNKNOWN_TOOL_COUNT_CONTEXT_KEY);
	}

	private List<String> getMetadataStringList(Map<String, Object> metadata, String key) {
		Object value = metadata.get(key);
		if (value instanceof List<?> list) {
			return list.stream().filter(String.class::isInstance).map(String.class::cast).toList();
		}
		return List.of();
	}

	private String buildTerminationMessage(int consecutiveCount, List<String> requestedToolNames,
			List<String> availableToolNames) {
		if (StringUtils.hasText(terminationMessage)) {
			return terminationMessage;
		}

		String requested = requestedToolNames.isEmpty() ? "[]" : requestedToolNames.toString();
		String available = availableToolNames.isEmpty() ? "[]" : availableToolNames.toString();
		return "The model requested unknown tools for " + consecutiveCount
				+ " consecutive rounds, so tool calling has been stopped to avoid an infinite loop. "
				+ "Requested tools: " + requested + ". Available tools: " + available
				+ ". Please answer directly with the current context or choose one of the available tools in a new run.";
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

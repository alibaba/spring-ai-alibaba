/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.hook.toolcalllimit;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Hook that tracks and limits tool call counts.
 *
 * This hook monitors the number of tool calls made during agent execution
 * and can terminate the agent when specified limits are reached.
 */
@HookPositions({HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL})
public class ToolCallLimitHook extends ModelHook {

	private static final String THREAD_COUNT_KEY_PREFIX = "__tool_call_limit_thread_count__";
	private static final String RUN_COUNT_KEY_PREFIX = "__tool_call_limit_run_count__";

	private final String toolName; // null means track all tools
	private final Integer threadLimit;
	private final Integer runLimit;
	private final ExitBehavior exitBehavior;

	private ToolCallLimitHook(Builder builder) {
		this.toolName = builder.toolName;
		this.threadLimit = builder.threadLimit;
		this.runLimit = builder.runLimit;
		this.exitBehavior = builder.exitBehavior;
	}

	public static Builder builder() {
		return new Builder();
	}

	private String getThreadCountKey() {
		String trackKey = toolName != null ? toolName : "__all__";
		return THREAD_COUNT_KEY_PREFIX + "_" + trackKey;
	}

	private String getRunCountKey() {
		String trackKey = toolName != null ? toolName : "__all__";
		return RUN_COUNT_KEY_PREFIX + "_" + trackKey;
	}

	@Override
	public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
		// Read current counts from context
		int threadCount = config.context().containsKey(getThreadCountKey())
				? (int) config.context().get(getThreadCountKey()) : 0;
		int runCount = config.context().containsKey(getRunCountKey())
				? (int) config.context().get(getRunCountKey()) : 0;

		boolean threadLimitExceeded = threadLimit != null && threadCount >= threadLimit;
		boolean runLimitExceeded = runLimit != null && runCount >= runLimit;

		if (threadLimitExceeded || runLimitExceeded) {
			if (exitBehavior == ExitBehavior.ERROR) {
				throw new ToolCallLimitExceededException(
						threadCount,
						runCount,
						threadLimit,
						runLimit,
						toolName
				);
			}
			else if (exitBehavior == ExitBehavior.END) {
				String message = buildLimitExceededMessage(threadCount, runCount, threadLimit, runLimit, toolName);
				List<Message> messages = new ArrayList<>((List<Message>) state.value("messages")
						.orElse(new ArrayList<>()));
				messages.add(new AssistantMessage(message));

				Map<String, Object> updates = new HashMap<>();
				updates.put("messages", messages);
				return CompletableFuture.completedFuture(updates);
			}
		}

		return CompletableFuture.completedFuture(Map.of());
	}

	@Override
	public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
		// Count new tool calls from the latest AI message
		List<Message> messages = (List<Message>) state.value("messages").orElse(List.of());
		if (messages.isEmpty()) {
			return CompletableFuture.completedFuture(Map.of());
		}

		// Get the last message (should be an AssistantMessage with tool calls)
		Message lastMessage = messages.get(messages.size() - 1);
		int newCalls = 0;

		if (lastMessage instanceof AssistantMessage) {
			AssistantMessage aiMessage = (AssistantMessage) lastMessage;
			if (aiMessage.getToolCalls() != null) {
				if (toolName == null) {
					// Count all tool calls
					newCalls = aiMessage.getToolCalls().size();
				} else {
					// Count only calls to the specified tool
					for (AssistantMessage.ToolCall toolCall : aiMessage.getToolCalls()) {
						if (toolName.equals(toolCall.name())) {
							newCalls++;
						}
					}
				}
			}
		}

		// Increment counters if there are new tool calls
		if (newCalls > 0) {
			// Read current counts from context
			int threadCount = config.context().containsKey(getThreadCountKey())
					? (int) config.context().get(getThreadCountKey()) : 0;
			int runCount = config.context().containsKey(getRunCountKey())
					? (int) config.context().get(getRunCountKey()) : 0;

			// Update context with incremented counts
			config.context().put(getThreadCountKey(), threadCount + newCalls);
			config.context().put(getRunCountKey(), runCount + newCalls);
		}

		return CompletableFuture.completedFuture(Map.of());
	}

	private String buildLimitExceededMessage(int threadCount, int runCount,
			Integer threadLimit, Integer runLimit, String toolName) {
		String toolDesc = toolName != null ? "'" + toolName + "' tool call" : "Tool call";
		List<String> exceededLimits = new ArrayList<>();

		if (threadLimit != null && threadCount >= threadLimit) {
			exceededLimits.add(String.format("thread limit (%d/%d)", threadCount, threadLimit));
		}
		if (runLimit != null && runCount >= runLimit) {
			exceededLimits.add(String.format("run limit (%d/%d)", runCount, runLimit));
		}

		return toolDesc + " limits exceeded: " + String.join(", ", exceededLimits);
	}

	@Override
	public String getName() {
		return toolName != null ? "ToolCallLimit[" + toolName + "]" : "ToolCallLimit[all]";
	}

	@Override
	public List<JumpTo> canJumpTo() {
		if (exitBehavior == ExitBehavior.END) {
			return List.of(JumpTo.end);
		}
		return List.of();
	}

	public enum ExitBehavior {
		END,
		ERROR
	}

	public static class Builder {
		private String toolName;
		private Integer threadLimit;
		private Integer runLimit;
		private ExitBehavior exitBehavior = ExitBehavior.END;

		public Builder toolName(String toolName) {
			this.toolName = toolName;
			return this;
		}

		public Builder threadLimit(Integer threadLimit) {
			this.threadLimit = threadLimit;
			return this;
		}

		public Builder runLimit(Integer runLimit) {
			this.runLimit = runLimit;
			return this;
		}

		public Builder exitBehavior(ExitBehavior exitBehavior) {
			this.exitBehavior = exitBehavior;
			return this;
		}

		public ToolCallLimitHook build() {
			if (threadLimit == null && runLimit == null) {
				throw new IllegalArgumentException("At least one limit must be specified (threadLimit or runLimit)");
			}
			return new ToolCallLimitHook(this);
		}
	}
}

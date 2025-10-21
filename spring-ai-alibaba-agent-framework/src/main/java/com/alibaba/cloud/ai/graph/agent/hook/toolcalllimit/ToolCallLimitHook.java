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
import com.alibaba.cloud.ai.graph.agent.hook.AfterModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Hook that tracks tool call counts and enforces limits.
 *
 * This hook monitors the number of tool calls made during agent execution
 * and can terminate the agent when specified limits are reached.
 */
public class ToolCallLimitHook extends AfterModelHook {

	private final String toolName; // null means track all tools
	private final Integer threadLimit;
	private final Integer runLimit;
	private final ExitBehavior exitBehavior;

	private final Map<String, Integer> threadToolCallCount = new HashMap<>();
	private final Map<String, Integer> runToolCallCount = new HashMap<>();

	private ToolCallLimitHook(Builder builder) {
		this.toolName = builder.toolName;
		this.threadLimit = builder.threadLimit;
		this.runLimit = builder.runLimit;
		this.exitBehavior = builder.exitBehavior;
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
		List<Message> messages = (List<Message>) state.value("messages").orElse(new ArrayList<>());

		// Count tool calls in messages
		String trackKey = toolName != null ? toolName : "__all__";
		int newCalls = countToolCallsInMessages(messages, toolName);

		threadToolCallCount.put(trackKey, threadToolCallCount.getOrDefault(trackKey, 0) + newCalls);
		runToolCallCount.put(trackKey, runToolCallCount.getOrDefault(trackKey, 0) + newCalls);

		int threadCount = threadToolCallCount.getOrDefault(trackKey, 0);
		int runCount = runToolCallCount.getOrDefault(trackKey, 0);

		// Check if limits are exceeded
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
				List<Message> updatedMessages = new ArrayList<>(messages);
				updatedMessages.add(new AssistantMessage(message));

				Map<String, Object> updates = new HashMap<>();
				updates.put("messages", updatedMessages);
				return CompletableFuture.completedFuture(updates);
			}
		}

		return CompletableFuture.completedFuture(Map.of());
	}

	private int countToolCallsInMessages(List<Message> messages, String toolName) {
		int count = 0;
		for (Message message : messages) {
			if (message instanceof AssistantMessage) {
				AssistantMessage aiMessage = (AssistantMessage) message;
				if (aiMessage.getToolCalls() != null) {
					if (toolName == null) {
						count += aiMessage.getToolCalls().size();
					}
					else {
						for (AssistantMessage.ToolCall toolCall : aiMessage.getToolCalls()) {
							if (toolName.equals(toolCall.name())) {
								count++;
							}
						}
					}
				}
			}
		}
		return count;
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

	public void resetRunCount() {
		runToolCallCount.clear();
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


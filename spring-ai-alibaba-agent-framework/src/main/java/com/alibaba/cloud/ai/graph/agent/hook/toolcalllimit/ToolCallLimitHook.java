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
package com.alibaba.cloud.ai.graph.agent.hook.toolcalllimit;

import com.alibaba.cloud.ai.graph.GraphLifecycleListener;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.tool.ToolCallResults;
import com.alibaba.cloud.ai.graph.state.RemoveByHash;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.part.TextPart;
import org.springframework.ai.chat.messages.part.ToolResultPart;
import org.springframework.ai.model.tool.ToolCallDecisions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
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
		return THREAD_COUNT_KEY_PREFIX + getAgentName() + "_" + getName();
	}

	private String getRunCountKey() {
		return RUN_COUNT_KEY_PREFIX + getAgentName() + "_" + getName();
	}

	@Override
	public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
		long threadCount = state.<Number>value(getThreadCountKey()).map(Number::longValue).orElse(0L);
		long runCount = runCount(state);
		if ((threadLimit != null && threadCount >= threadLimit) || (runLimit != null && runCount >= runLimit)) {
			return CompletableFuture.completedFuture(limitExceeded(threadCount, runCount, List.of()));
		}

		return CompletableFuture.completedFuture(Map.of());
	}

	@Override
	public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
		List<Message> messages = (List<Message>) state.value("messages").orElse(List.of());
		if (messages.isEmpty()) {
			return CompletableFuture.completedFuture(Map.of());
		}

		Message lastMessage = messages.get(messages.size() - 1);
		ToolResponseMessage settled = lastMessage instanceof ToolResponseMessage response ? response : null;
		if (settled != null && messages.size() > 1) {
			lastMessage = messages.get(messages.size() - 2);
		}
		if (!(lastMessage instanceof AssistantMessage assistant)) {
			return CompletableFuture.completedFuture(Map.of());
		}

		Set<String> completed = settled == null ? Set.of() : ToolCallResults.settledKeys(assistant, settled.getResults());
		List<String> keys = ToolCallDecisions.toolCallKeys(assistant);
		var calls = assistant.getToolCalls();
		Map<String, AssistantMessage.ToolCall> pending = new LinkedHashMap<>();
		for (int i = 0; i < keys.size(); i++) {
			if (!completed.contains(keys.get(i))) {
				pending.put(keys.get(i), calls.get(i));
			}
		}
		long newCalls = pending.values().stream().filter(call -> toolName == null || toolName.equals(call.name())).count();
		if (newCalls == 0) {
			return CompletableFuture.completedFuture(Map.of());
		}

		long threadCount = state.<Number>value(getThreadCountKey()).map(Number::longValue).orElse(0L);
		long runCount = runCount(state);
		long proposedThreadCount = threadCount + newCalls;
		long proposedRunCount = runCount + newCalls;
		if ((threadLimit != null && proposedThreadCount > threadLimit)
				|| (runLimit != null && proposedRunCount > runLimit)) {
			HumanInTheLoopHook.clearConfirmedCalls(config);
			var updates = limitExceeded(proposedThreadCount, proposedRunCount,
					deniedResults(keys, settled, pending, buildLimitExceededMessage(proposedThreadCount, proposedRunCount,
							threadLimit, runLimit, toolName)));
			HumanInTheLoopHook.removeConfirmedCalls(state, getAgentName(), updates);
			return CompletableFuture.completedFuture(updates);
		}

		return CompletableFuture.completedFuture(Map.of(
				getThreadCountKey(), proposedThreadCount,
				getRunCountKey(), Map.of("executionId", executionId(state), "count", proposedRunCount)));
	}

	private String executionId(OverAllState state) {
		return state.<String>value(GraphLifecycleListener.EXECUTION_ID_KEY).orElse("");
	}

	private long runCount(OverAllState state) {
		Map<?, ?> counter = state.<Map<?, ?>>value(getRunCountKey()).orElse(Map.of());
		if (executionId(state).equals(counter.get("executionId")) && counter.get("count") instanceof Number count) {
			return count.longValue();
		}
		return 0;
	}

	@Override
	public int getOrder() {
		// AFTER_MODEL runs in reverse priority, after approval has settled denied calls.
		return Integer.MIN_VALUE;
	}

	private List<Object> deniedResults(List<String> keys, ToolResponseMessage settled,
			Map<String, AssistantMessage.ToolCall> pending, String reason) {
		List<Object> updates = new ArrayList<>();
		List<ToolResultPart> results = new ArrayList<>();
		if (settled != null) {
			updates.add(new RemoveByHash<>(settled));
			results.addAll(settled.getResults());
		}
		pending.forEach((key, call) -> {
			var result = new ToolResultPart(call.id(), call.name(), List.of(TextPart.of(reason)), true, null, Map.of());
			results.add(ToolCallResults.withCallKey(result, key));
		});
		updates.add(ToolResponseMessage.builder().results(ToolCallResults.inCallOrder(keys, results))
				.metadata(settled == null ? Map.of() : settled.getMetadata()).build());
		return updates;
	}

	private Map<String, Object> limitExceeded(long threadCount, long runCount, List<Object> denied) {
		if (exitBehavior == ExitBehavior.ERROR) {
			throw new ToolCallLimitExceededException((int) Math.min(threadCount, Integer.MAX_VALUE),
					(int) Math.min(runCount, Integer.MAX_VALUE), threadLimit, runLimit, toolName);
		}

		String message = buildLimitExceededMessage(threadCount, runCount, threadLimit, runLimit, toolName);
		List<Object> updates = new ArrayList<>(denied);
		AssistantMessage answer = new AssistantMessage(message);
		updates.add(answer);
		Map<String, Object> stateUpdates = new HashMap<>(Map.of("messages", updates, "jump_to", JumpTo.end));
		if (getAgent() != null && getAgent().getOutputKey() != null && !getAgent().getOutputKey().isEmpty()) {
			stateUpdates.put(getAgent().getOutputKey(), answer);
		}
		return stateUpdates;
	}

	private String buildLimitExceededMessage(long threadCount, long runCount,
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

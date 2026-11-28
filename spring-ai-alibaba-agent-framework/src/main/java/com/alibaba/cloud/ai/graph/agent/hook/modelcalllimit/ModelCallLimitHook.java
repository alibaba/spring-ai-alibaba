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
package com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Hook that tracks and limits model call counts.
 *
 * This hook monitors the number of model calls made during agent execution
 * and can terminate the agent when specified limits are reached. It supports
 * both thread-level and run-level call counting with configurable exit behaviors.
 * A checkpoint saver is needed to retain a thread count across separate invocations.
 */
@HookPositions({HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL})
public class ModelCallLimitHook extends ModelHook {

	private static final String THREAD_COUNT_KEY = "__model_call_limit_thread_count__";
	private static final String RUN_COUNT_KEY = "__model_call_limit_run_count__";

	private final Integer threadLimit;
	private final Integer runLimit;
	private final ExitBehavior exitBehavior;

	private ModelCallLimitHook(Builder builder) {
		this.threadLimit = builder.threadLimit;
		this.runLimit = builder.runLimit;
		this.exitBehavior = builder.exitBehavior;
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
		int runModelCallCount = runCount(config);
		int threadModelCallCount = threadLimit == null ? runModelCallCount : threadCount(state);

		// Check if limits are already exceeded (before making the call)
		boolean threadLimitExceeded = threadLimit != null && threadModelCallCount >= threadLimit;
		boolean runLimitExceeded = runLimit != null && runModelCallCount >= runLimit;

		if (threadLimitExceeded || runLimitExceeded) {
			if (exitBehavior == ExitBehavior.ERROR) {
				throw new ModelCallLimitExceededException(
						threadModelCallCount,
						runModelCallCount,
						threadLimit,
						runLimit
				);
			}
			else if (exitBehavior == ExitBehavior.END) {
				// Add message indicating limit was exceeded and jump to end
				String message = buildLimitExceededMessage(
						threadModelCallCount,
						runModelCallCount,
						threadLimit,
						runLimit
				);

				List<Message> messages = new ArrayList<>();
				messages.add(new AssistantMessage(message));

				Map<String, Object> updates = new HashMap<>();
				updates.put("messages", messages);
				updates.put("jump_to", JumpTo.end);
				return CompletableFuture.completedFuture(updates);
			}
		}

		return CompletableFuture.completedFuture(Map.of());
	}

	@Override
	public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
		config.context().put(RUN_COUNT_KEY, runCount(config) + 1);
		return CompletableFuture.completedFuture(
				threadLimit == null ? Map.of() : Map.of(threadCountKey(), threadCount(state) + 1));
	}

	private int runCount(RunnableConfig config) {
		return (int) config.context().getOrDefault(RUN_COUNT_KEY, 0);
	}

	private int threadCount(OverAllState state) {
		return state.<Number>value(threadCountKey()).map(Number::intValue).orElse(0);
	}

	private String threadCountKey() {
		return THREAD_COUNT_KEY + getAgentName() + "_" + getName();
	}

	@Override
	public Map<String, KeyStrategy> getKeyStrategys() {
		return threadLimit == null ? Map.of() : Map.of(threadCountKey(), new ReplaceStrategy());
	}

	private String buildLimitExceededMessage(int threadCount, int runCount, Integer threadLimit, Integer runLimit) {
		List<String> exceededLimits = new ArrayList<>();
		if (threadLimit != null && threadCount >= threadLimit) {
			exceededLimits.add(String.format("thread limit (%d/%d)", threadCount, threadLimit));
		}
		if (runLimit != null && runCount >= runLimit) {
			exceededLimits.add(String.format("run limit (%d/%d)", runCount, runLimit));
		}
		return "Model call limits exceeded: " + String.join(", ", exceededLimits);
	}

	@Override
	public String getName() {
		return "ModelCallLimit";
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
		private Integer threadLimit;
		private Integer runLimit;
		private ExitBehavior exitBehavior = ExitBehavior.END;

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

		public ModelCallLimitHook build() {
			if (threadLimit == null && runLimit == null) {
				throw new IllegalArgumentException("At least one limit must be specified (threadLimit or runLimit)");
			}
			return new ModelCallLimitHook(this);
		}
	}
}

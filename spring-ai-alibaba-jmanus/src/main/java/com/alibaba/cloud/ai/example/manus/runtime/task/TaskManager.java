/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.manus.runtime.task;

import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.llm.ILlmService;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A lightweight TaskManager responsible for:
 * - Creating/scheduling child tasks
 * - Aggregating child results
 * - Patching parent's ChatMemory with aggregated result before resuming parent
 */
public class TaskManager {

	private final ILlmService llmService;
	private final ManusProperties manusProperties;
    private final Executor executor; // may be null, then common pool is used
    private final Map<String, PlanTask> taskRegistry = new ConcurrentHashMap<>();

	public TaskManager(ILlmService llmService, ManusProperties manusProperties, Executor executor) {
		this.llmService = Objects.requireNonNull(llmService, "llmService must not be null");
		this.manusProperties = Objects.requireNonNull(manusProperties, "manusProperties must not be null");
		this.executor = executor;
	}

    public void registerParentTask(PlanTask task) {
        if (task != null && task.getContext() != null) {
            taskRegistry.put(task.getContext().getCurrentPlanId(), task);
        }
    }

    public PlanTask getRegisteredTask(String planId) {
        return taskRegistry.get(planId);
    }

	/**
	 * Schedule child tasks from planIds using a factory, wait for all to complete, and return their results map.
	 */
	public CompletableFuture<Map<String, String>> scheduleChildren(Collection<String> childPlanIds,
			Function<String, PlanTask> taskFactory) {
		List<CompletableFuture<Void>> runFutures = new ArrayList<>();
		Map<String, CompletableFuture<ExecutionContext>> resultFutures = new HashMap<>();

		for (String childId : childPlanIds) {
			PlanTask task = taskFactory.apply(childId);
			CompletableFuture<Void> runFuture = CompletableFuture.runAsync(task::start,
					executor == null ? CompletableFuture.delayedExecutor(0, java.util.concurrent.TimeUnit.MILLISECONDS)
							: executor);
			runFutures.add(runFuture);
			resultFutures.put(childId, task.getFuture().thenApply(ExecutionContext.class::cast).toCompletableFuture());
		}

		return CompletableFuture.allOf(runFutures.toArray(new CompletableFuture[0]))
				.thenCompose(v -> CompletableFuture.allOf(resultFutures.values().toArray(new CompletableFuture[0])))
				.thenApply(v -> {
					Map<String, String> results = new HashMap<>();
					for (Map.Entry<String, CompletableFuture<ExecutionContext>> e : resultFutures.entrySet()) {
						ExecutionContext ctx = e.getValue().join();
						String result = ctx.getResultSummary();
						results.put(e.getKey(), result == null ? "" : result);
					}
					return results;
				});
	}

	/**
	 * Patch parent's ChatMemory to reflect the aggregated result from child tasks.
	 * Strategy: replace the last ToolResponseMessage by appending a clarifying AssistantMessage,
	 * or rebuild the memory keeping all messages except the last ToolResponseMessage.
	 */
	public void patchParentMemoryWithAggregatedResult(String parentPlanId, String aggregatedResult) {
		if (aggregatedResult == null) {
			aggregatedResult = "";
		}
		ChatMemory chatMemory = llmService.getAgentMemory(manusProperties.getMaxMemory());
		List<Message> mem = new ArrayList<>(chatMemory.get(parentPlanId));
		if (mem.isEmpty()) {
			// Simply append an assistant message
			chatMemory.add(parentPlanId, new AssistantMessage(aggregatedResult));
			return;
		}

		int lastToolIdx = -1;
		for (int i = mem.size() - 1; i >= 0; i--) {
			if (mem.get(i) instanceof ToolResponseMessage) {
				lastToolIdx = i;
				break;
			}
		}

		if (lastToolIdx < 0) {
			// No tool response present; append an assistant message carrying the replacement
			chatMemory.add(parentPlanId, new AssistantMessage(aggregatedResult));
			return;
		}

		// Rebuild memory: keep everything before last tool response, append replacement as assistant message
		List<Message> rebuilt = new ArrayList<>(mem.subList(0, lastToolIdx));
		rebuilt.add(new AssistantMessage(aggregatedResult));
		chatMemory.clear(parentPlanId);
		for (Message m : rebuilt) {
			chatMemory.add(parentPlanId, m);
		}
	}

	/**
	 * High-level helper for DynamicAgent: detect/handle sub-plans for a parent planId.
	 * It is responsible for scheduling children externally and patching memory,
	 * and returns the aggregated result string to be used as toolcall replacement.
	 */
	public CompletionStage<String> handleSubPlansForParent(String parentPlanId, Collection<String> childPlanIds,
			Function<String, PlanTask> taskFactory) {
		return scheduleChildren(childPlanIds, taskFactory).thenApply(results -> {
			StringBuilder sb = new StringBuilder();
			results.forEach((k, v) -> sb.append("[").append(k).append("] ").append(v == null ? "" : v).append("\n"));
			String aggregated = sb.toString().trim();
			patchParentMemoryWithAggregatedResult(parentPlanId, aggregated);
			return aggregated;
		});
	}

    /**
     * Non-blocking orchestration: suspend registered parent, schedule children, patch memory, and resume.
     */
    public CompletionStage<Void> scheduleChildrenPatchAndResumeByPlanId(String parentPlanId,
            Collection<String> childPlanIds, Function<String, PlanTask> taskFactory) {
        PlanTask parentTask = getRegisteredTask(parentPlanId);
        CompletableFuture<Void> suspension = null;
        if (parentTask != null) {
            suspension = parentTask.suspendForChildren(childPlanIds);
        }
        CompletableFuture<Void> finalSuspension = suspension;
        return scheduleChildren(childPlanIds, taskFactory).thenAccept(results -> {
            StringBuilder sb = new StringBuilder();
            results.forEach((k, v) -> sb.append("[").append(k).append("] ").append(v == null ? "" : v).append("\n"));
            String aggregated = sb.toString().trim();
            patchParentMemoryWithAggregatedResult(parentPlanId, aggregated);
            if (parentTask != null) {
                parentTask.completeChildrenAndResume(results);
                if (finalSuspension != null && !finalSuspension.isDone()) {
                    finalSuspension.complete(null);
                }
            }
        });
    }
}



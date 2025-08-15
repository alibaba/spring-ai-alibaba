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
import com.alibaba.cloud.ai.example.manus.runtime.vo.PlanExecutionResult;
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
 * TaskManager is the central orchestrator for managing PlanTask lifecycle and execution.
 * 
 * Key Responsibilities:
 * 1. **Task Registration**: Maintains a registry of parent tasks for later reference
 * 2. **Child Task Scheduling**: Creates and schedules child PlanTask instances
 * 3. **Result Aggregation**: Collects results from multiple child tasks
 * 4. **Memory Management**: Patches parent's ChatMemory with aggregated child results
 * 5. **Task Coordination**: Handles parent-child task relationships and resumption
 * 
 * Execution Flow:
 * - Parent task registers itself with TaskManager
 * - Parent creates child tasks and submits them via scheduleChildren()
 * - TaskManager executes child tasks asynchronously
 * - Results are aggregated and patched to parent's memory
 * - Parent task resumes with updated context
 */
public class TaskManager {

	private final ILlmService llmService;
	private final ManusProperties manusProperties;
    private final Executor executor; // may be null, then common pool is used
    
    /**
     * Registry of parent tasks by planId.
     * Used to retrieve parent tasks when child tasks complete.
     */
    private final Map<String, PlanTask> taskRegistry = new ConcurrentHashMap<>();

	public TaskManager(ILlmService llmService, ManusProperties manusProperties, Executor executor) {
		this.llmService = Objects.requireNonNull(llmService, "llmService must not be null");
		this.manusProperties = Objects.requireNonNull(manusProperties, "manusProperties must not be null");
		this.executor = executor;
	}

    /**
     * Registers a parent task in the registry for later retrieval.
     * This is called by parent tasks when they need to manage child tasks.
     */
    public void registerParentTask(PlanTask task) {
        if (task != null && task.getContext() != null) {
            taskRegistry.put(task.getContext().getCurrentPlanId(), task);
        }
    }

    /**
     * Retrieves a registered parent task by planId.
     * Used when child tasks complete and need to notify their parent.
     */
    public PlanTask getRegisteredTask(String planId) {
        return taskRegistry.get(planId);
    }

	/**
	 * **CRITICAL METHOD: This is where PlanTask.start() is actually called!**
	 * 
	 * Schedules child tasks from planIds using a factory, executes them asynchronously,
	 * waits for all to complete, and returns their results as a Map of planId to PlanExecutionResult.
	 * 
	 * Execution Flow:
	 * 1. For each child planId, create a PlanTask using the provided factory
	 * 2. **EXECUTE: Call PlanTask.start() asynchronously via CompletableFuture.runAsync()**
	 * 3. Collect futures for both execution (runFutures) and results (resultFutures)
	 * 4. Wait for all executions to complete
	 * 5. Wait for all results to be available
	 * 6. Return a Map of planId to individual PlanExecutionResult objects
	 * 
	 * @param childPlanIds Collection of plan IDs to execute
	 * @param taskFactory Function to create PlanTask instances from planIds
	 * @return CompletableFuture that completes with Map<String, PlanExecutionResult> containing individual results for each child plan
	 */
	public CompletableFuture<Map<String, PlanExecutionResult>> scheduleChildren(Collection<String> childPlanIds,
			Function<String, PlanTask> taskFactory) {
		List<CompletableFuture<Void>> runFutures = new ArrayList<>();
		Map<String, CompletableFuture<PlanExecutionResult>> resultFutures = new HashMap<>();

		for (String childId : childPlanIds) {
			// Create PlanTask instance using the factory
			PlanTask task = taskFactory.apply(childId);
			
			// *** EXECUTION POINT: PlanTask.start() is called here! ***
			// This is the actual method invocation that starts the plan execution
			CompletableFuture<Void> runFuture = CompletableFuture.runAsync(task::start,
					executor == null ? CompletableFuture.delayedExecutor(0, java.util.concurrent.TimeUnit.MILLISECONDS)
							: executor);
			runFutures.add(runFuture);
			
			// Collect the result future from the task
			resultFutures.put(childId, task.getFuture().toCompletableFuture());
		}

		// Wait for all executions to complete, then wait for all results
		return CompletableFuture.allOf(runFutures.toArray(new CompletableFuture[0]))
				.thenCompose(executionComplete -> CompletableFuture.allOf(resultFutures.values().toArray(new CompletableFuture[0])))
				.thenApply(resultsComplete -> {
					// Create a map of planId -> PlanExecutionResult
					Map<String, PlanExecutionResult> results = new HashMap<>();
					
					// Collect individual results for each child plan
					for (Map.Entry<String, CompletableFuture<PlanExecutionResult>> e : resultFutures.entrySet()) {
						String planId = e.getKey();
						PlanExecutionResult childResult = e.getValue().join();
						results.put(planId, childResult);
					}
					
					return results;
				});
	}

	/**
	 * Patches parent's ChatMemory to reflect the aggregated result from child tasks.
	 * 
	 * Strategy: 
	 * 1. Replace the last ToolResponseMessage by appending a clarifying AssistantMessage, OR
	 * 2. Rebuild the memory keeping all messages except the last ToolResponseMessage
	 * 
	 * This ensures the parent task has context about what the child tasks accomplished.
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
	 * 
	 * This method orchestrates the complete child task lifecycle:
	 * 1. Schedule children using scheduleChildren() (which calls PlanTask.start())
	 * 2. Aggregate results from all children
	 * 3. Patch parent's memory with aggregated results
	 * 4. Return aggregated result string to be used as toolcall replacement
	 * 
	 * @param parentPlanId ID of the parent plan
	 * @param childPlanIds Collection of child plan IDs to execute
	 * @param taskFactory Function to create PlanTask instances
	 * @return CompletionStage that completes with aggregated result string
	 */
	public CompletionStage<String> handleSubPlansForParent(String parentPlanId, Collection<String> childPlanIds,
			Function<String, PlanTask> taskFactory) {
		return scheduleChildren(childPlanIds, taskFactory).thenApply(resultsMap -> {
			// Aggregate results from individual PlanExecutionResult objects
			StringBuilder aggregated = new StringBuilder();
			for (Map.Entry<String, PlanExecutionResult> entry : resultsMap.entrySet()) {
				String planId = entry.getKey();
				PlanExecutionResult result = entry.getValue();
				String resultString = result.getEffectiveResult();
				aggregated.append("[").append(planId).append("] ").append(resultString).append("\n");
			}
			String aggregatedResult = aggregated.toString().trim();
			
			patchParentMemoryWithAggregatedResult(parentPlanId, aggregatedResult);
			return aggregatedResult;
		});
	}

    /**
     * Non-blocking orchestration: suspend registered parent, schedule children, patch memory, and resume.
     * 
     * This is the most sophisticated orchestration method that:
     * 1. Suspends the parent task (if registered)
     * 2. Schedules and executes child tasks via scheduleChildren() (calls PlanTask.start())
     * 3. Patches parent's memory with child results
     * 4. Resumes the parent task with updated context
     * 
     * @param parentPlanId ID of the parent plan to orchestrate
     * @param childPlanIds Collection of child plan IDs to execute
     * @param taskFactory Function to create PlanTask instances
     * @return CompletionStage that completes when orchestration is done
     */
    public CompletionStage<Void> scheduleChildrenPatchAndResumeByPlanId(String parentPlanId,
            Collection<String> childPlanIds, Function<String, PlanTask> taskFactory) {
        PlanTask parentTask = getRegisteredTask(parentPlanId);
        CompletableFuture<Void> suspension = null;
        if (parentTask != null) {
            suspension = parentTask.suspendForChildren(childPlanIds);
        }
        CompletableFuture<Void> finalSuspension = suspension;
        
        // *** EXECUTION POINT: This calls scheduleChildren() which calls PlanTask.start() ***
        return scheduleChildren(childPlanIds, taskFactory).thenAccept(resultsMap -> {
            // Aggregate results from individual PlanExecutionResult objects
            StringBuilder aggregated = new StringBuilder();
            Map<String, String> resultsMapForParent = new HashMap<>();
            
            for (Map.Entry<String, PlanExecutionResult> entry : resultsMap.entrySet()) {
                String planId = entry.getKey();
                PlanExecutionResult result = entry.getValue();
                String resultString = result.getEffectiveResult();
                
                aggregated.append("[").append(planId).append("] ").append(resultString).append("\n");
                resultsMapForParent.put(planId, resultString);
            }
            String aggregatedResult = aggregated.toString().trim();
            
            patchParentMemoryWithAggregatedResult(parentPlanId, aggregatedResult);
            if (parentTask != null) {
                parentTask.completeChildrenAndResume(resultsMapForParent);
                if (finalSuspension != null && !finalSuspension.isDone()) {
                    finalSuspension.complete(null);
                }
            }
        });
    }
}



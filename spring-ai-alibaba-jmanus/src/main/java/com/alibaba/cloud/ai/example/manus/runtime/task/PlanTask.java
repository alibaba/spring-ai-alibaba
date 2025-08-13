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

import com.alibaba.cloud.ai.example.manus.planning.executor.PlanExecutorInterface;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;

import java.util.Objects;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class PlanTask {

	private final String id = UUID.randomUUID().toString();
	private final String parentPlanId; // null for root
	private final ExecutionContext context;
	private final PlanExecutorInterface executor;
	private volatile TaskState state = TaskState.READY;
	private final CompletableFuture<ExecutionContext> future = new CompletableFuture<>();
    private volatile int nextStepIndex = 0; // internal checkpoint placeholder

    // Children coordination placeholders (managed externally by a TaskManager)
    private volatile Set<String> waitingChildPlanIds = new HashSet<>();
    private volatile CompletableFuture<Void> childrenCompletion;
    private volatile String lastToolCallReplacementResult;

	// Replacement result is stored locally; external manager should patch ChatMemory

	public PlanTask(ExecutionContext context, String parentPlanId, PlanExecutorInterface executor) {
		this.context = Objects.requireNonNull(context, "context must not be null");
		this.parentPlanId = parentPlanId; // may be null
		this.executor = Objects.requireNonNull(executor, "executor must not be null");
	}

	public String getId() {
		return id;
	}

	public String getParentPlanId() {
		return parentPlanId;
	}

	public ExecutionContext getContext() {
		return context;
	}

	public TaskState getState() {
		return state;
	}

	public void start() {
		if (state != TaskState.READY && state != TaskState.SUSPENDED) {
			return;
		}
		state = TaskState.RUNNING;
		try {
			executor.executeAllSteps(context);
			state = TaskState.COMPLETED;
			future.complete(context);
		}
		catch (Exception e) {
			state = TaskState.FAILED;
			future.completeExceptionally(e);
		}
	}

    public void resume() {
        // Minimal placeholder: resume from internal checkpoint (nextStepIndex)
        start();
    }

	public void cancel() {
		if (!future.isDone()) {
			state = TaskState.FAILED;
			future.cancel(true);
		}
	}

	public CompletionStage<?> getFuture() {
		return future;
	}

    /**
     * Suspend current task for child plan execution. This only flips the state and prepares a barrier;
     * actual child task creation/management is handled by an external TaskManager.
     * @param childPlanIds the child plan ids to wait for
     * @return a future that should be completed by external manager when all children are done
     */
    public synchronized CompletableFuture<Void> suspendForChildren(Collection<String> childPlanIds) {
        if (state != TaskState.RUNNING) {
            return childrenCompletion == null ? new CompletableFuture<>() : childrenCompletion;
        }
        state = TaskState.WAITING_CHILDREN;
        waitingChildPlanIds.clear();
        if (childPlanIds != null) {
            waitingChildPlanIds.addAll(childPlanIds);
        }
        childrenCompletion = new CompletableFuture<>();
        return childrenCompletion;
    }

    /**
     * Notify that all children have completed and provide a consolidated result to replace the triggering toolcall result.
     * External TaskManager should call this, then PlanTask will store the replacement and resume.
     * @param childResults map childPlanId -> result string
     */
    public synchronized void completeChildrenAndResume(Map<String, String> childResults) {
        if (childResults != null && !childResults.isEmpty()) {
            // Minimal merge strategy: join results by newline; callers can use a richer format if needed
            StringBuilder sb = new StringBuilder();
            childResults.forEach((k, v) -> {
                sb.append("[").append(k).append("] ").append(v == null ? "" : v).append("\n");
            });
            lastToolCallReplacementResult = sb.toString().trim();
            // Do not write into ExecutionContext.toolsContext. External TaskManager must patch ChatMemory
        }

        waitingChildPlanIds.clear();
        if (childrenCompletion != null && !childrenCompletion.isDone()) {
            childrenCompletion.complete(null);
        }
        state = TaskState.SUSPENDED; // set to suspended before resuming, for observability
        resume();
    }

    public String getLastToolCallReplacementResult() {
        return lastToolCallReplacementResult;
    }
}



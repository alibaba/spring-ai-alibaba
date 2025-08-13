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
}



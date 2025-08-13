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

import com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanningCoordinator;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * Simple task wrapper to execute a plan asynchronously and return a CompletableFuture.
 */
public final class PlanExecutionTask {

	private PlanExecutionTask() {
	}

	/**
	 * Submit an asynchronous task to execute the given plan using the common pool.
	 * @param planningFlow planning coordinator
	 * @param context execution context
	 * @return CompletableFuture of the execution result context
	 */
	public static CompletableFuture<ExecutionContext> submit(PlanningCoordinator planningFlow,
			ExecutionContext context) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return planningFlow.executePlan(context);
			}
			catch (Exception e) {
				throw new CompletionException("Failed to execute plan: " + e.getMessage(), e);
			}
		});
	}

	/**
	 * Submit an asynchronous task to execute the given plan using the provided executor.
	 * @param planningFlow planning coordinator
	 * @param context execution context
	 * @param executor executor to run the task
	 * @return CompletableFuture of the execution result context
	 */
	public static CompletableFuture<ExecutionContext> submit(PlanningCoordinator planningFlow,
			ExecutionContext context, Executor executor) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return planningFlow.executePlan(context);
			}
			catch (Exception e) {
				throw new CompletionException("Failed to execute plan: " + e.getMessage(), e);
			}
		}, executor);
	}
}



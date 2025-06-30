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
package com.alibaba.cloud.ai.example.manus.planning.coordinator;

import com.alibaba.cloud.ai.example.manus.planning.creator.PlanCreator;
import com.alibaba.cloud.ai.example.manus.planning.executor.PlanExecutorInterface;
import com.alibaba.cloud.ai.example.manus.planning.finalizer.PlanFinalizer;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;

/**
 * The coordinator of the planning process, responsible for coordinating the creation,
 * execution and summary of the plan
 */
public class PlanningCoordinator {

	private final PlanCreator planCreator;

	private final PlanExecutorInterface planExecutor;

	private final PlanFinalizer planFinalizer;

	public PlanningCoordinator(PlanCreator planCreator, PlanExecutorInterface planExecutor,
			PlanFinalizer planFinalizer) {
		this.planCreator = planCreator;
		this.planExecutor = planExecutor;
		this.planFinalizer = planFinalizer;
	}

	/**
	 * Create a plan only, without executing it
	 * @param context execution context
	 * @return execution context
	 */
	public ExecutionContext createPlan(ExecutionContext context) {
		// Only execute the create plan step
		context.setUseMemory(false);
		planCreator.createPlan(context);
		return context;
	}

	/**
	 * Execute the complete plan process
	 * @param context execution context
	 * @return execution summary
	 */
	public ExecutionContext executePlan(ExecutionContext context) {
		context.setUseMemory(true);
		// 1. Create a plan
		planCreator.createPlan(context);

		// 2. Execute the plan
		planExecutor.executeAllSteps(context);

		// 3. Generate a summary
		planFinalizer.generateSummary(context);

		return context;
	}

	/**
	 * Execute an existing plan (skip the create plan step)
	 * @param context execution context containing the existing plan
	 * @return execution summary
	 */
	public ExecutionContext executeExistingPlan(ExecutionContext context) {
		// 1. Execute the plan
		planExecutor.executeAllSteps(context);

		// 2. Generate a summary
		planFinalizer.generateSummary(context);

		return context;
	}

}

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
import com.alibaba.cloud.ai.example.manus.planning.executor.PlanExecutor;
import com.alibaba.cloud.ai.example.manus.planning.finalizer.PlanFinalizer;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;

/**
 * 计划流程的总协调器 负责协调计划的创建、执行和总结三个主要步骤
 */
public class PlanningCoordinator {

	private final PlanCreator planCreator;

	private final PlanExecutor planExecutor;

	private final PlanFinalizer planFinalizer;

	public PlanningCoordinator(PlanCreator planCreator, PlanExecutor planExecutor, PlanFinalizer planFinalizer) {
		this.planCreator = planCreator;
		this.planExecutor = planExecutor;
		this.planFinalizer = planFinalizer;
	}

	/**
	 * 仅创建计划，不执行
	 * @param context 执行上下文
	 * @return 执行上下文
	 */
	public ExecutionContext createPlan(ExecutionContext context) {
		// 只执行创建计划步骤
		context.setUseMemory(false);
		planCreator.createPlan(context);
		return context;
	}

	/**
	 * 执行完整的计划流程
	 * @param context 执行上下文
	 * @return 执行总结
	 */
	public ExecutionContext executePlan(ExecutionContext context) {
		context.setUseMemory(true);
		// 1. 创建计划
		planCreator.createPlan(context);

		// 2. 执行计划
		planExecutor.executeAllSteps(context);

		// 3. 生成总结
		planFinalizer.generateSummary(context);

		return context;
	}

	/**
	 * 执行已有计划（跳过创建计划步骤）
	 * @param context 包含现有计划的执行上下文
	 * @return 执行总结
	 */
	public ExecutionContext executeExistingPlan(ExecutionContext context) {
		// 1. 执行计划
		planExecutor.executeAllSteps(context);

		// 2. 生成总结
		planFinalizer.generateSummary(context);

		return context;
	}

}

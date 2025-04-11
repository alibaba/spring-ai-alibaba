package com.alibaba.cloud.ai.example.manus.planning.coordinator;

import com.alibaba.cloud.ai.example.manus.planning.creator.PlanCreator;
import com.alibaba.cloud.ai.example.manus.planning.executor.PlanExecutor;
import com.alibaba.cloud.ai.example.manus.planning.finalizer.PlanFinalizer;
import com.alibaba.cloud.ai.example.manus.planning.model.ExecutionContext;

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
	 * 执行完整的计划流程
	 * @param userRequest 用户请求
	 * @return 执行总结
	 */
	public ExecutionContext executePlan(ExecutionContext context) {
		// 1. 创建计划
		planCreator.createPlan(context);

		// 2. 执行计划
		planExecutor.executeAllSteps(context);

		// 3. 生成总结
		planFinalizer.generateSummary(context);

		return context;
	}

}

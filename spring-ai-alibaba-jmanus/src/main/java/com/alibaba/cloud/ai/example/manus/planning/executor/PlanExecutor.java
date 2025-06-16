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
package com.alibaba.cloud.ai.example.manus.planning.executor;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.cloud.ai.example.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.service.AgentService;
import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionStep;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.PlanInterface;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;

import java.util.List;

/**
 * 负责执行计划的类（基础实现）
 */
public class PlanExecutor extends AbstractPlanExecutor {

	public PlanExecutor(List<DynamicAgentEntity> agents, PlanExecutionRecorder recorder, AgentService agentService,
			LlmService llmService) {
		super(agents, recorder, agentService, llmService);
	}

	/**
	 * 执行整个计划的所有步骤
	 * @param context 执行上下文，包含用户请求和执行的过程信息
	 */
	@Override
	public void executeAllSteps(ExecutionContext context) {
		BaseAgent executor = null;
		PlanInterface plan = context.getPlan();
		plan.updateStepIndices();
		try {
			recordPlanExecutionStart(context);
			List<ExecutionStep> steps = plan.getAllSteps();

			if (CollectionUtil.isNotEmpty(steps)) {
				for (ExecutionStep step : steps) {
					BaseAgent executorinStep = executeStep(step, context);
					if (executorinStep != null) {
						executor = executorinStep;
					}
				}
			}

			context.setSuccess(true);
		}
		finally {
			performCleanup(context, executor);
		}
	}

}

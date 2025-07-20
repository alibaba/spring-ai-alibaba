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

import com.alibaba.cloud.ai.example.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.service.AgentService;
import com.alibaba.cloud.ai.example.manus.llm.ILlmService;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionStep;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.PlanInterface;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;

import java.util.List;

/**
 * Basic implementation class responsible for executing plans
 */
public class PlanExecutor extends AbstractPlanExecutor {

	/**
	 * Constructor for PlanExecutor
	 * @param agents List of dynamic agent entities
	 * @param recorder Plan execution recorder
	 * @param agentService Agent service
	 * @param llmService LLM service
	 */
	public PlanExecutor(List<DynamicAgentEntity> agents, PlanExecutionRecorder recorder, AgentService agentService,
			ILlmService llmService, ManusProperties manusProperties) {
		super(agents, recorder, agentService, llmService, manusProperties);
	}

	/**
	 * Execute all steps of the entire plan
	 * @param context Execution context containing user request and execution process
	 * information
	 */
	@Override
	public void executeAllSteps(ExecutionContext context) {
		BaseAgent lastExecutor = null;
		PlanInterface plan = context.getPlan();
		plan.updateStepIndices();

		try {
			recorder.recordPlanExecutionStart(context);
			List<ExecutionStep> steps = plan.getAllSteps();

			if (steps != null && !steps.isEmpty()) {
				for (ExecutionStep step : steps) {
					BaseAgent stepExecutor = executeStep(step, context);
					if (stepExecutor != null) {
						lastExecutor = stepExecutor;
					}
				}
			}

			context.setSuccess(true);
		}
		finally {
			performCleanup(context, lastExecutor);
		}
	}

}

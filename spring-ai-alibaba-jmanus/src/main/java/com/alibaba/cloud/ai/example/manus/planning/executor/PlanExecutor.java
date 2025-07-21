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
import com.alibaba.cloud.ai.example.manus.planning.model.vo.PlanConfirmData;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.PlanInterface;
import com.alibaba.cloud.ai.example.manus.planning.service.PlanConfirmService;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Basic implementation class responsible for executing plans
 */
public class PlanExecutor extends AbstractPlanExecutor {

	private static final Logger logger = LoggerFactory.getLogger(PlanExecutor.class);

	/**
	 * Constructor for PlanExecutor
	 * @param agents List of dynamic agent entities
	 * @param recorder Plan execution recorder
	 * @param agentService Agent service
	 * @param llmService LLM service
	 */
	public PlanExecutor(List<DynamicAgentEntity> agents, PlanExecutionRecorder recorder, AgentService agentService,
			ILlmService llmService, ManusProperties manusProperties, PlanConfirmService planConfirmService) {
		super(agents, recorder, agentService, llmService, manusProperties, planConfirmService);
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
			// process handle plan confirm
			handlePlanConfirm(plan);

			// If the plan is accepted, continue executing the step, otherwise end the
			// current task
			if (PlanConfirmData.ConfirmState.ACCEPT.getState().equals(plan.getAccepted())) {
				List<ExecutionStep> steps = plan.getAllSteps();
				if (steps != null && !steps.isEmpty()) {
					for (ExecutionStep step : steps) {
						BaseAgent stepExecutor = executeStep(step, context);
						if (stepExecutor != null) {
							lastExecutor = stepExecutor;
						}
					}
				}
			}
			context.setSuccess(true);
		}
		finally {
			performCleanup(context, lastExecutor);
		}
	}

	/**
	 * Handle plan confirm
	 * @param currentPlan current plan
	 */
	private void handlePlanConfirm(PlanInterface currentPlan) {
		if (!manusProperties.getAutoAcceptPlan()) {
			// Do not automatically accept the plan, need to manually confirm whether to
			// use the currently generated plan
			PlanConfirmData planConfirmData = new PlanConfirmData(currentPlan.getCurrentPlanId(),
					PlanConfirmData.ConfirmState.AWAIT.getState(), null, 0);
			planConfirmService.storeConfirmData(currentPlan.getCurrentPlanId(), planConfirmData);

			String accepted = waitingForUserConfirmPlan(currentPlan.getCurrentPlanId());
			currentPlan.setAccepted(accepted);
		}
		else {
			currentPlan.setAccepted(PlanConfirmData.ConfirmState.ACCEPT.getState());
		}
	}

	/**
	 * Wait for user confirmation of the plan
	 * @param planId current plan id
	 */
	private String waitingForUserConfirmPlan(String planId) {
		logger.info("Waiting for user confirm plan, planId:{}...", planId);
		long startTime = System.currentTimeMillis();
		long confirmPlanTimeout = manusProperties.getConfirmPlanTimeout() * 1000L;
		PlanConfirmData planConfirmData = planConfirmService.getConfirmData(planId);
		while (planConfirmData == null || planConfirmData.getAccepted() == null
				|| PlanConfirmData.ConfirmState.AWAIT.getState().equals(planConfirmData.getAccepted())) {
			long currentTime = System.currentTimeMillis();
			if (currentTime - startTime > confirmPlanTimeout) {
				logger.warn("Timeout waiting for user confirm plan, planId:{}", planId);
				String state = PlanConfirmData.ConfirmState.ACCEPT.getState();
				String type = PlanConfirmData.ConfirmType.TIMEOUT.getType();
				planConfirmData = new PlanConfirmData(planId, state, type, currentTime);
				planConfirmService.storeConfirmData(planId, planConfirmData);
				return state;
			}

			try {
				TimeUnit.MILLISECONDS.sleep(500); // Check every 500ms
				planConfirmData = planConfirmService.getConfirmData(planId);
			}
			catch (InterruptedException e) {
				// System exception auto confirm plan
				logger.warn("Interrupted while waiting for user confirm plan, planId:{}", planId);
				Thread.currentThread().interrupt();
				String state = PlanConfirmData.ConfirmState.ACCEPT.getState();
				String type = PlanConfirmData.ConfirmType.TIMEOUT.getType();
				planConfirmData = new PlanConfirmData(planId, state, type, currentTime);
				planConfirmService.storeConfirmData(planId, planConfirmData);
				return state;
			}
		}

		return planConfirmData.getAccepted();
	}

}

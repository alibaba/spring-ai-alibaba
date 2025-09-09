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
package com.alibaba.cloud.ai.manus.runtime.executor;

import com.alibaba.cloud.ai.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.manus.config.ManusProperties;
import com.alibaba.cloud.ai.manus.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.manus.agent.service.AgentService;
import com.alibaba.cloud.ai.manus.llm.ILlmService;
import com.alibaba.cloud.ai.manus.recorder.service.PlanExecutionRecorder;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.ExecutionContext;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.ExecutionStep;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.PlanExecutionResult;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.PlanInterface;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.StepResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dynamic Agent Plan Executor - Specialized executor for DynamicAgentExecutionPlan
 * with user-selected tools support
 */
public class DynamicToolPlanExecutor extends AbstractPlanExecutor {

	private static final Logger log = LoggerFactory.getLogger(DynamicToolPlanExecutor.class);


	/**
	 * Constructor for DynamicAgentPlanExecutor
	 * @param agents List of dynamic agent entities
	 * @param recorder Plan execution recorder
	 * @param agentService Agent service
	 * @param llmService LLM service
	 * @param manusProperties Manus properties
	 * @param levelBasedExecutorPool Level-based executor pool for depth-based execution
	 */
	public DynamicToolPlanExecutor(List<DynamicAgentEntity> agents, PlanExecutionRecorder recorder, 
			AgentService agentService, ILlmService llmService, ManusProperties manusProperties, 
			LevelBasedExecutorPool levelBasedExecutorPool) {
		super(agents, recorder, agentService, llmService, manusProperties, levelBasedExecutorPool);
		
	}
	/**
	 * </pre>
	 * @param context Execution context containing user request and execution process
	 * information
	 * @return CompletableFuture containing PlanExecutionResult with all step results
	 */
	public CompletableFuture<PlanExecutionResult> executeAllStepsAsync(ExecutionContext context) {
		// Get the plan depth from context to determine which executor pool to use
		int planDepth = context.getPlanDepth();

		// Get the appropriate executor for this depth level
		ExecutorService executor = levelBasedExecutorPool.getExecutorForLevel(planDepth);

		return CompletableFuture.supplyAsync(() -> {
			PlanExecutionResult result = new PlanExecutionResult();
			BaseAgent lastExecutor = null;
			PlanInterface plan = context.getPlan();
			plan.setCurrentPlanId(context.getCurrentPlanId());
			plan.setRootPlanId(context.getRootPlanId());
			plan.updateStepIndices();

			try {
				List<ExecutionStep> steps = plan.getAllSteps();

				recorder.recordPlanExecutionStart(context.getCurrentPlanId(), context.getPlan().getTitle(),
						context.getUserRequest(), steps, context.getParentPlanId(), context.getRootPlanId(),
						context.getToolCallId());

				if (steps != null && !steps.isEmpty()) {
					for (ExecutionStep step : steps) {
						BaseAgent stepExecutor = executeStep(step, context);
						if (stepExecutor != null) {
							lastExecutor = stepExecutor;

							// Collect step result
							StepResult stepResult = new StepResult();
							stepResult.setStepIndex(step.getStepIndex());
							stepResult.setStepRequirement(step.getStepRequirement());
							stepResult.setResult(step.getResult());
							stepResult.setStatus(step.getStatus());
							stepResult.setAgentName(stepExecutor.getName());

							result.addStepResult(stepResult);
						}
					}
				}

				context.setSuccess(true);
				result.setSuccess(true);
				result.setFinalResult(context.getPlan().getResult());

			}
			catch (Exception e) {
				context.setSuccess(false);
				result.setSuccess(false);
				result.setErrorMessage(e.getMessage());
			}
			finally {
				performCleanup(context, lastExecutor);
			}

			return result;
		}, executor);
	}

}

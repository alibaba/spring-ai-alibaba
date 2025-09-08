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
import com.alibaba.cloud.ai.manus.runtime.entity.vo.DynamicAgentExecutionPlan;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dynamic Agent Plan Executor - Specialized executor for DynamicAgentExecutionPlan
 * with user-selected tools support
 */
public class DynamicAgentPlanExecutor extends AbstractPlanExecutor {

	private static final Logger log = LoggerFactory.getLogger(DynamicAgentPlanExecutor.class);

	private final LevelBasedExecutorPool levelBasedExecutorPool;

	/**
	 * Constructor for DynamicAgentPlanExecutor
	 * @param agents List of dynamic agent entities
	 * @param recorder Plan execution recorder
	 * @param agentService Agent service
	 * @param llmService LLM service
	 * @param manusProperties Manus properties
	 * @param levelBasedExecutorPool Level-based executor pool for depth-based execution
	 */
	public DynamicAgentPlanExecutor(List<DynamicAgentEntity> agents, PlanExecutionRecorder recorder, 
			AgentService agentService, ILlmService llmService, ManusProperties manusProperties, 
			LevelBasedExecutorPool levelBasedExecutorPool) {
		super(agents, recorder, agentService, llmService, manusProperties);
		this.levelBasedExecutorPool = levelBasedExecutorPool;
	}

	/**
	 * Execute all steps asynchronously with Dynamic Agent specific tool selection
	 * @param context Execution context containing user request and execution process information
	 * @return CompletableFuture containing PlanExecutionResult with all step results
	 */
	@Override
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

			// Validate that this is a DynamicAgentExecutionPlan
			if (!(plan instanceof DynamicAgentExecutionPlan)) {
				log.error("DynamicAgentPlanExecutor can only execute DynamicAgentExecutionPlan, but got: {}",
						plan.getClass().getSimpleName());
				result.setSuccess(false);
				result.setErrorMessage("Invalid plan type for DynamicAgentPlanExecutor");
				return result;
			}

			DynamicAgentExecutionPlan dynamicPlan = (DynamicAgentExecutionPlan) plan;

			try {
				List<ExecutionStep> steps = plan.getAllSteps();

				recorder.recordPlanExecutionStart(context.getCurrentPlanId(), context.getPlan().getTitle(),
						context.getUserRequest(), steps, context.getParentPlanId(), context.getRootPlanId(),
						context.getToolCallId());

				// Log selected tools for debugging
				if (dynamicPlan.getSelectedToolKeys() != null && !dynamicPlan.getSelectedToolKeys().isEmpty()) {
					log.info("Executing Dynamic Agent plan with selected tools: {}", 
							String.join(", ", dynamicPlan.getSelectedToolKeys()));
				}

				if (steps != null && !steps.isEmpty()) {
					for (ExecutionStep step : steps) {
						// Execute step with Dynamic Agent specific tool selection
						BaseAgent stepExecutor = executeStepWithDynamicTools(step, context, dynamicPlan);
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
				log.error("Error during Dynamic Agent plan execution", e);
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

	/**
	 * Execute a single step with Dynamic Agent specific tool selection
	 * @param step Execution step to execute
	 * @param context Execution context
	 * @param dynamicPlan Dynamic Agent execution plan containing selected tools
	 * @return BaseAgent executor for the step
	 */
	private BaseAgent executeStepWithDynamicTools(ExecutionStep step, ExecutionContext context, 
			DynamicAgentExecutionPlan dynamicPlan) {
		
		String stepType = getStepFromStepReq(step.getStepRequirement());
		int stepIndex = step.getStepIndex();
		String expectedReturnInfo = step.getTerminateColumns();

		String planStatus = context.getPlan().getPlanExecutionStateStringFormat(true);
		String stepText = step.getStepRequirement();

		Map<String, Object> initSettings = new HashMap<>();
		initSettings.put(PLAN_STATUS_KEY, planStatus);
		initSettings.put(CURRENT_STEP_INDEX_KEY, String.valueOf(stepIndex));
		initSettings.put(STEP_TEXT_KEY, stepText);
		initSettings.put(EXTRA_PARAMS_KEY, context.getPlan().getExecutionParams());

		// Add Dynamic Agent specific settings
		if (dynamicPlan.getSelectedToolKeys() != null && !dynamicPlan.getSelectedToolKeys().isEmpty()) {
			initSettings.put("selectedToolKeys", dynamicPlan.getSelectedToolKeys());
			log.debug("Setting selected tools for step {}: {}", stepIndex, dynamicPlan.getSelectedToolKeys());
		}

		for (DynamicAgentEntity agent : agents) {
			if (agent.getAgentName().equalsIgnoreCase(stepType)) {
				BaseAgent executor = agentService.createDynamicBaseAgent(agent.getAgentName(),
						context.getPlan().getCurrentPlanId(), context.getPlan().getRootPlanId(), initSettings,
						expectedReturnInfo, step);
				return executor;
			}
		}
		throw new IllegalArgumentException(
				"No Agent Executor found for step type, check your agents list : " + stepType);
	}

	/**
	 * Get the step type from step requirement
	 * @param stepRequirement Step requirement string
	 * @return Step type extracted from requirement
	 */
	protected String getStepFromStepReq(String stepRequirement) {
		// This method should extract the agent type from the step requirement
		// For now, return a default or extract from the requirement
		if (stepRequirement == null || stepRequirement.trim().isEmpty()) {
			return "DEFAULT_AGENT";
		}
		
		// Look for agent type in square brackets like [AgentType]
		if (stepRequirement.contains("[") && stepRequirement.contains("]")) {
			int start = stepRequirement.indexOf("[");
			int end = stepRequirement.indexOf("]", start);
			if (start != -1 && end != -1 && end > start) {
				return stepRequirement.substring(start + 1, end).trim();
			}
		}
		
		// Default fallback
		return "DEFAULT_AGENT";
	}

}

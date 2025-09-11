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

import com.alibaba.cloud.ai.manus.agent.AgentState;
import com.alibaba.cloud.ai.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.manus.config.ManusProperties;
import com.alibaba.cloud.ai.manus.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.manus.agent.service.AgentService;
import com.alibaba.cloud.ai.manus.llm.ILlmService;
import com.alibaba.cloud.ai.manus.model.entity.DynamicModelEntity;
import com.alibaba.cloud.ai.manus.recorder.service.PlanExecutionRecorder;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.ExecutionContext;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.ExecutionStep;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.PlanExecutionResult;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.PlanInterface;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.StepResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for plan executors. Contains common logic and basic functionality
 * for all executor types.
 */
public abstract class AbstractPlanExecutor implements PlanExecutorInterface {

	private static final Logger logger = LoggerFactory.getLogger(AbstractPlanExecutor.class);

	protected final PlanExecutionRecorder recorder;

	// Pattern to match square brackets at the beginning of a string, supports
	// Chinese and
	// other characters
	protected final Pattern pattern = Pattern.compile("^\\s*\\[([^\\]]+)\\]");

	protected final List<DynamicAgentEntity> agents;

	protected final LevelBasedExecutorPool levelBasedExecutorPool;

	protected final AgentService agentService;

	protected ILlmService llmService;

	protected final ManusProperties manusProperties;

	// Define static final strings for the keys used in executorParams
	public static final String PLAN_STATUS_KEY = "planStatus";

	public static final String CURRENT_STEP_INDEX_KEY = "currentStepIndex";

	public static final String STEP_TEXT_KEY = "stepText";

	public static final String EXTRA_PARAMS_KEY = "extraParams";

	public static final String EXECUTION_ENV_STRING_KEY = "current_step_env_data";

	public AbstractPlanExecutor(List<DynamicAgentEntity> agents, PlanExecutionRecorder recorder,
			AgentService agentService, ILlmService llmService, ManusProperties manusProperties,
			LevelBasedExecutorPool levelBasedExecutorPool) {
		this.agents = agents;
		this.recorder = recorder;
		this.agentService = agentService;
		this.llmService = llmService;
		this.manusProperties = manusProperties;
		this.levelBasedExecutorPool = levelBasedExecutorPool;
	}

	/**
	 * General logic for executing a single step.
	 * @param step The execution step
	 * @param context The execution context
	 * @return The step executor
	 */
	protected BaseAgent executeStep(ExecutionStep step, ExecutionContext context) {
		try {
			BaseAgent executor = getExecutorForStep(context, step);
			if (executor == null) {
				logger.error("No executor found for step type: {}", step.getStepInStr());
				step.setResult("No executor found for step type: " + step.getStepInStr());
				return null;
			}

			step.setAgent(executor);
			executor.setState(AgentState.IN_PROGRESS);

			recorder.recordStepStart(step, context.getCurrentPlanId());
			String stepResultStr = executor.run();
			step.setResult(stepResultStr);
			recorder.recordStepEnd(step, context.getCurrentPlanId());

			return executor;
		}
		catch (Exception e) {
			logger.error("Error executing step: {}", step.getStepRequirement(), e);
			step.setResult("Execution failed: " + e.getMessage());
		}
		finally {
			recorder.recordStepEnd(step, context.getCurrentPlanId());
		}
		return null;
	}

	/**
	 * Extract the step type from the step requirement string.
	 */
	protected String getStepFromStepReq(String stepRequirement) {
		Matcher matcher = pattern.matcher(stepRequirement);
		if (matcher.find()) {
			return matcher.group(1).trim().toUpperCase();
		}
		return "DEFAULT_AGENT";
	}

	/**
	 * Get the executor for the step.
	 */
	protected BaseAgent getExecutorForStep(ExecutionContext context, ExecutionStep step) {

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

		for (DynamicAgentEntity agent : agents) {
			if (agent.getAgentName().equalsIgnoreCase(stepType)) {
				// Get model entity from agent - it's already loaded via @ManyToOne
				DynamicModelEntity modelEntity = agent.getModel();

				BaseAgent executor = agentService.createDynamicBaseAgent(agent.getAgentName(),
						context.getPlan().getCurrentPlanId(), context.getPlan().getRootPlanId(), initSettings,
						expectedReturnInfo, step, modelEntity, agent.getAvailableToolKeys());
				return executor;
			}
		}
		throw new IllegalArgumentException(
				"No Agent Executor found for step type, check your agents list : " + stepType);
	}

	protected PlanExecutionRecorder getRecorder() {
		return recorder;
	}

	/**
	 * Execute all steps asynchronously and return a CompletableFuture with execution
	 * results. Uses level-based executor pools based on plan depth.
	 *
	 * Usage example: <pre>
	 * CompletableFuture<PlanExecutionResult> future = planExecutor.executeAllStepsAsync(context);
	 *
	 * future.whenComplete((result, throwable) -> {
	 *     if (throwable != null) {
	 *         // Handle execution error
	 *         System.err.println("Execution failed: " + throwable.getMessage());
	 *     } else {
	 *         // Handle successful completion
	 *         if (result.isSuccess()) {
	 *             String finalResult = result.getEffectiveResult();
	 *             System.out.println("Final result: " + finalResult);
	 *
	 *             // Access individual step results
	 *             for (StepResult step : result.getStepResults()) {
	 *                 System.out.println("Step " + step.getStepIndex() + ": " + step.getResult());
	 *             }
	 *         } else {
	 *             System.err.println("Execution failed: " + result.getErrorMessage());
	 *         }
	 *     }
	 * });
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

	/**
	 * Cleanup work after execution is completed.
	 */
	protected void performCleanup(ExecutionContext context, BaseAgent lastExecutor) {
		String planId = context.getCurrentPlanId();
		llmService.clearAgentMemory(planId);
		if (lastExecutor != null) {
			lastExecutor.clearUp(planId);
		}
	}

}

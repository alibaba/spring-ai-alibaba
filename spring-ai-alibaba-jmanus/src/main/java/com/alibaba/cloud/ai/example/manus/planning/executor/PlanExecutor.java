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

import com.alibaba.cloud.ai.example.manus.agent.AgentState;
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
import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

	/**
	 * Execute all steps asynchronously and return a CompletableFuture with execution results
	 * 
	 * Usage example:
	 * <pre>
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
	 * 
	 * @param context Execution context containing user request and execution process information
	 * @return CompletableFuture containing PlanExecutionResult with all step results
	 */
	public CompletableFuture<PlanExecutionResult> executeAllStepsAsync(ExecutionContext context) {
		return CompletableFuture.supplyAsync(() -> {
			PlanExecutionResult result = new PlanExecutionResult();
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
				result.setFinalResult(context.getResultSummary());
				
			} catch (Exception e) {
				context.setSuccess(false);
				result.setSuccess(false);
				result.setErrorMessage(e.getMessage());
			} finally {
				performCleanup(context, lastExecutor);
			}
			
			return result;
		});
	}

	/**
	 * Result class containing execution results for all steps
	 */
	public static class PlanExecutionResult {
		private boolean success;
		private String finalResult;
		private String errorMessage;
		private List<StepResult> stepResults = new ArrayList<>();
		
		// Getters and setters
		public boolean isSuccess() { return success; }
		public void setSuccess(boolean success) { this.success = success; }
		
		public String getFinalResult() { return finalResult; }
		public void setFinalResult(String finalResult) { this.finalResult = finalResult; }
		
		public String getErrorMessage() { return errorMessage; }
		public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
		
		public List<StepResult> getStepResults() { return stepResults; }
		public void setStepResults(List<StepResult> stepResults) { this.stepResults = stepResults; }
		
		public void addStepResult(StepResult stepResult) {
			this.stepResults.add(stepResult);
		}
		
		/**
		 * Get the final result from the last successful step
		 * @return Final result string or error message
		 */
		public String getEffectiveResult() {
			if (!success) {
				return errorMessage != null ? errorMessage : "Execution failed";
			}
			
			// Try to get result from last step's result
			for (int i = stepResults.size() - 1; i >= 0; i--) {
				StepResult step = stepResults.get(i);
				if (step.getResult() != null && !step.getResult().isEmpty()) {
					return step.getResult();
				}
			}
			
			return finalResult != null ? finalResult : "No result available";
		}
	}

	/**
	 * Result class for individual step execution
	 */
	public static class StepResult {
		private Integer stepIndex;
		private String stepRequirement;
		private String result;
		private AgentState status;
		private String agentName;
		
		// Getters and setters
		public Integer getStepIndex() { return stepIndex; }
		public void setStepIndex(Integer stepIndex) { this.stepIndex = stepIndex; }
		
		public String getStepRequirement() { return stepRequirement; }
		public void setStepRequirement(String stepRequirement) { this.stepRequirement = stepRequirement; }
		
		public String getResult() { return result; }
		public void setResult(String result) { this.result = result; }
		
		public AgentState getStatus() { return status; }
		public void setStatus(AgentState status) { this.status = status; }
		
		public String getAgentName() { return agentName; }
		public void setAgentName(String agentName) { this.agentName = agentName; }
	}

}

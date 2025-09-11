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
package com.alibaba.cloud.ai.manus.runtime.entity.vo;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.cloud.ai.manus.agent.AgentState;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Plan entity class for managing execution plan related information
 */
public class ExecutionPlan extends AbstractExecutionPlan {

	private List<ExecutionStep> steps;

	/**
	 * Plan type for Jackson polymorphic deserialization
	 */
	private String planType = "simple";

	/**
	 * Default constructor - required for Jackson deserialization
	 */
	public ExecutionPlan() {
		super();
		this.steps = new ArrayList<>();
	}

	public ExecutionPlan(String currentPlanId, String parentPlanId, String title) {
		super(currentPlanId, parentPlanId, title);
		this.steps = new ArrayList<>();
	}

	public String getPlanType() {
		return planType;
	}

	public void setPlanType(String planType) {
		this.planType = planType;
	}

	// ExecutionPlan specific methods

	public List<ExecutionStep> getSteps() {
		return steps;
	}

	public void setSteps(List<ExecutionStep> steps) {
		this.steps = steps;
	}

	@JsonIgnore
	public int getStepCount() {
		return steps.size();
	}

	// Implementation of AbstractExecutionPlan abstract methods

	@Override
	@JsonIgnore
	public List<ExecutionStep> getAllSteps() {
		return new ArrayList<>(steps);
	}

	@Override
	@JsonIgnore
	public int getTotalStepCount() {
		return getStepCount();
	}

	@Override
	public void addStep(ExecutionStep step) {
		this.steps.add(step);
	}

	@Override
	public void removeStep(ExecutionStep step) {
		this.steps.remove(step);
	}

	@Override
	@JsonIgnore
	public boolean isEmpty() {
		return steps.isEmpty();
	}

	@Override
	protected void clearSteps() {
		steps.clear();
	}

	// state.append("Global Goal (The global goal is just a directional guidance, you
	// don't need to complete the global goal in the current request, just focus on the
	// currently executing step): ")
	// .append("\n")
	// .append(title)
	// .append("\n");
	@Override
	@JsonIgnore
	public String getPlanExecutionStateStringFormat(boolean onlyCompletedAndFirstInProgress) {
		StringBuilder state = new StringBuilder();

		state.append(
				"- User Original Requirements (This requirement is the user's initial input, information can be referenced, but in the current interaction round only the current step requirements need to be completed!) :\n");
		state.append(title).append("\n");
		if (getUserRequest() != null && !getUserRequest().isEmpty()) {
			state.append("").append(getUserRequest()).append("\n\n");
		}
		state.append("\n- Execution Parameters: ").append("\n");
		if (executionParams != null && !executionParams.isEmpty()) {
			state.append(executionParams).append("\n\n");
		}
		else {
			state.append("No execution parameters provided.\n\n");
		}

		state.append("- Historical Executed Step Records:\n");
		state.append(getStepsExecutionStateStringFormat(onlyCompletedAndFirstInProgress));

		return state.toString();
	}

	/**
	 * Get step execution status in string format
	 * @param onlyCompletedAndFirstInProgress When true, only output all completed steps
	 * and the first step in progress
	 * @return Formatted step execution status string
	 */
	@JsonIgnore
	public String getStepsExecutionStateStringFormat(boolean onlyCompletedAndFirstInProgress) {
		StringBuilder state = new StringBuilder();
		boolean foundInProgress = false;

		for (int i = 0; i < steps.size(); i++) {
			ExecutionStep step = steps.get(i);

			// If onlyCompletedAndFirstInProgress is true, only show COMPLETED status
			// steps and the first IN_PROGRESS status step
			if (onlyCompletedAndFirstInProgress) {
				// If it's COMPLETED status, always show
				if (step.getStatus() == AgentState.COMPLETED) {
					// Do nothing, continue to show
				}
				// If it's IN_PROGRESS status and haven't found other IN_PROGRESS steps
				// yet
				else if (step.getStatus() == AgentState.IN_PROGRESS && !foundInProgress) {
					foundInProgress = true; // Mark that IN_PROGRESS step has been found
				}
				// All other cases (not COMPLETED and not the first IN_PROGRESS)
				else {
					continue; // Skip steps that don't meet the criteria
				}
			}

			String symbol = switch (step.getStatus()) {
				case COMPLETED -> "[completed]";
				case IN_PROGRESS -> "[in_progress]";
				case BLOCKED -> "[blocked]";
				case NOT_STARTED -> "[not_started]";
				default -> "[ ]";
			};

			state.append(i + 1)
				.append(".  **Step ")
				.append(i)
				.append(":**\n")
				.append("    *   **Status:** ")
				.append(symbol)
				.append("\n")
				.append("    *   **Action:** ")
				.append(step.getStepRequirement())
				.append("\n");

			String result = step.getResult();
			if (result != null && !result.isEmpty()) {
				state.append("    *   **Result:** ").append(result).append("\n\n");
			}

		}
		return state.toString();
	}

	/**
	 * Get all step execution status in string format (compatible with old version)
	 * @return Formatted step execution status string
	 */
	@JsonIgnore
	public String getStepsExecutionStateStringFormat() {
		return getStepsExecutionStateStringFormat(false);
	}

	@Override
	public String getResult() {
		// If there are steps, return the result of the last step
		if (steps != null && !steps.isEmpty()) {
			ExecutionStep lastStep = steps.get(steps.size() - 1);
			if (lastStep != null && lastStep.getResult() != null) {
				return lastStep.getResult();
			}
		}
		// Return null if no steps or no result available
		return null;
	}

}

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
package com.alibaba.cloud.ai.example.manus.planning.model.vo;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.cloud.ai.example.manus.agent.AgentState;

/**
 * Plan entity class for managing execution plan related information
 */
public class ExecutionPlan {

	private String planId;

	private String title;

	private String userRequest;

	private String planningThinking;

	// Use simple string to store execution parameters
	private String executionParams;

	private List<ExecutionStep> steps;

	public ExecutionPlan(String planId, String title) {
		this.planId = planId;
		this.title = title;
		this.steps = new ArrayList<>();
		this.executionParams = "";
	}

	public String getPlanId() {
		return planId;
	}

	public void setPlanId(String planId) {
		this.planId = planId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public List<ExecutionStep> getSteps() {
		return steps;
	}

	public void setSteps(List<ExecutionStep> steps) {
		this.steps = steps;
	}

	public void addStep(ExecutionStep step) {
		this.steps.add(step);
	}

	public void removeStep(ExecutionStep step) {
		this.steps.remove(step);
	}

	public int getStepCount() {
		return steps.size();
	}

	public String getPlanningThinking() {
		return planningThinking;
	}

	public void setPlanningThinking(String planningThinking) {
		this.planningThinking = planningThinking;
	}

	public String getExecutionParams() {
		return executionParams;
	}

	public void setExecutionParams(String executionParams) {
		this.executionParams = executionParams;
	}

	/**
	 * Get user request
	 * @return User request string
	 */
	public String getUserRequest() {
		return userRequest;
	}

	/**
	 * Set user request
	 * @param userRequest User request string
	 */
	public void setUserRequest(String userRequest) {
		this.userRequest = userRequest;
	}

	@Override
	public String toString() {
		return "ExecutionPlan{" + "planId='" + planId + '\'' + ", title='" + title + '\'' + ", stepsCount="
				+ (steps != null ? steps.size() : 0) + '}';
	}

	// state.append("Global Goal (The global goal is just a directional guidance, you don't need to complete the global goal in the current request, just focus on the currently executing step): ")
	// .append("\n")
	// .append(title)
	// .append("\n");
	public String getPlanExecutionStateStringFormat(boolean onlyCompletedAndFirstInProgress) {
		StringBuilder state = new StringBuilder();

		state.append("- User Original Requirements (This requirement is the user's initial input, information can be referenced, but in the current interaction round only the current step requirements need to be completed!) :\n");
		state.append(title).append("\n");
		if (userRequest != null && !userRequest.isEmpty()) {
			state.append("").append(userRequest).append("\n\n");
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
	 * @param onlyCompletedAndFirstInProgress When true, only output all completed steps and the first step in progress
	 * @return Formatted step execution status string
	 */
	public String getStepsExecutionStateStringFormat(boolean onlyCompletedAndFirstInProgress) {
		StringBuilder state = new StringBuilder();
		boolean foundInProgress = false;

		for (int i = 0; i < steps.size(); i++) {
			ExecutionStep step = steps.get(i);

			// If onlyCompletedAndFirstInProgress is true, only show COMPLETED status steps and the first IN_PROGRESS status step
			if (onlyCompletedAndFirstInProgress) {
				// If it's COMPLETED status, always show
				if (step.getStatus() == AgentState.COMPLETED) {
					// Do nothing, continue to show
				}
				// If it's IN_PROGRESS status and haven't found other IN_PROGRESS steps yet
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
	public String getStepsExecutionStateStringFormat() {
		return getStepsExecutionStateStringFormat(false);
	}

	/**
	 * Convert plan to JSON string
	 * @return JSON string representation of the plan
	 */
	public String toJson() {
		StringBuilder json = new StringBuilder();
		json.append("{\n");
		json.append("  \"planId\": \"").append(planId).append("\",\n");
		json.append("  \"title\": \"").append(title).append("\",\n");

		// Add steps array
		json.append("  \"steps\": [\n");
		for (int i = 0; i < steps.size(); i++) {
			json.append(steps.get(i).toJson());
			if (i < steps.size() - 1) {
				json.append(",");
			}
			json.append("\n");
		}
		json.append("  ]\n");

		json.append("}");
		return json.toString();
	}

	/**
	 * Parse JSON string and create ExecutionPlan object
	 * @param planJson JSON string
	 * @param newPlanId New plan ID (optional, will override planId in JSON if provided)
	 * @return Parsed ExecutionPlan object
	 * @throws Exception Throws exception if parsing fails
	 */
	public static ExecutionPlan fromJson(String planJson, String newPlanId) throws Exception {
		com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
		com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(planJson);

		// Get plan title
		String title = rootNode.has("title") ? rootNode.get("title").asText() : "Plan from Template";

		// Use new plan ID or get from JSON
		String planId = (newPlanId != null && !newPlanId.isEmpty()) ? newPlanId
				: (rootNode.has("planId") ? rootNode.get("planId").asText() : "unknown-plan");

		// Create new ExecutionPlan object
		ExecutionPlan plan = new ExecutionPlan(planId, title);

		// If there are plan steps, add them to the plan
		if (rootNode.has("steps") && rootNode.get("steps").isArray()) {
			com.fasterxml.jackson.databind.JsonNode stepsNode = rootNode.get("steps");
			int stepIndex = 0;
			for (com.fasterxml.jackson.databind.JsonNode stepNode : stepsNode) {
				if (stepNode.has("stepRequirement")) {
					// Call ExecutionStep's fromJson method to create step
					ExecutionStep step = ExecutionStep.fromJson(stepNode);
					Integer stepIndexValFromJson = step.getStepIndex();
					if (stepIndexValFromJson != null) {
						stepIndex = stepIndexValFromJson;
					}
					else {
						step.setStepIndex(stepIndex);
					}
					plan.addStep(step);
					stepIndex++;
				}
			}
		}

		return plan;
	}

}

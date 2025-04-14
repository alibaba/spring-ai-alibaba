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
package com.alibaba.cloud.ai.example.manus.planning.model;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.cloud.ai.example.manus.flow.PlanStepStatus;

/**
 * 计划实体类，用于管理执行计划的相关信息
 */
public class ExecutionPlan {

	private String planId;

	private String title;

	private String planningThinking;

	private List<ExecutionStep> steps;

	public ExecutionPlan(String planId, String title) {
		this.planId = planId;
		this.title = title;
		this.steps = new ArrayList<>();
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

	public String getPlanExecutionStateStringFormat() {
		StringBuilder state = new StringBuilder();
		state.append("Plan: ").append(title).append(" (ID: ").append(planId).append(")\n");
		state.append("=".repeat(state.length())).append("\n\n");

		long completed = steps.stream().filter(step -> step.getStatus().equals(PlanStepStatus.COMPLETED)).count();
		int total = steps.size();
		double progress = total > 0 ? (completed * 100.0 / total) : 0;

		state.append(String.format("Progress: %d/%d steps (%.1f%%)\n\n", completed, total, progress));

		state.append("Steps:\n");
		state.append(getStepsExecutionStateStringFormat());

		return state.toString();
	}

	public String getStepsExecutionStateStringFormat() {
		StringBuilder state = new StringBuilder();
		for (int i = 0; i < steps.size(); i++) {

			ExecutionStep step = steps.get(i);
			String symbol = switch (step.getStatus()) {
				case COMPLETED -> "[completed]";
				case IN_PROGRESS -> "[in_progress]";
				case BLOCKED -> "[blocked]";
				case NOT_STARTED -> "[not_started]";
				default -> "[ ]";
			};
			state.append("step ")
				.append(i)
				.append(": ")
				.append(symbol)
				.append(" ")
				.append(step.getStepRequirement())
				.append("\n");
			state.append(" - step execution result: ").append("\n").append(step.getResult()).append("\n");
		}
		return state.toString();
	}

}

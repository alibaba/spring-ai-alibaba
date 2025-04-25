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

/**
 * 计划执行的整体结果
 */
public class ExecutionResult {

	private String planId;

	private List<ExecutionStep> stepResults;

	private String executionDetails;

	private boolean success;

	// Getters and Setters
	public String getPlanId() {
		return planId;
	}

	public void setPlanId(String planId) {
		this.planId = planId;
	}

	public List<ExecutionStep> getStepResults() {
		return stepResults;
	}

	public void setStepResults(List<ExecutionStep> stepResults) {
		this.stepResults = stepResults;
	}

	public String getExecutionDetails() {
		return executionDetails;
	}

	public void setExecutionDetails(String executionDetails) {
		this.executionDetails = executionDetails;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public void addStepResult(ExecutionStep stepResult) {
		if (this.stepResults == null) {
			this.stepResults = new ArrayList<>();
		}
		this.stepResults.add(stepResult);
	}

}

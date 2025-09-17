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

/**
 * Result class containing execution results for all steps
 */
public class PlanExecutionResult {

	private boolean success;

	private String finalResult;

	private String errorMessage;

	private List<StepResult> stepResults = new ArrayList<>();

	// Getters and setters
	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getFinalResult() {
		return finalResult;
	}

	public void setFinalResult(String finalResult) {
		this.finalResult = finalResult;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public List<StepResult> getStepResults() {
		return stepResults;
	}

	public void setStepResults(List<StepResult> stepResults) {
		this.stepResults = stepResults;
	}

	public void addStepResult(StepResult stepResult) {
		this.stepResults.add(stepResult);
	}

}

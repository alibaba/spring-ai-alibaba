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

import com.alibaba.cloud.ai.example.manus.flow.PlanStepStatus;

/**
 * 单个步骤的执行结果
 */
public class ExecutionStep {

	private int stepIndex;

	private String stepRequirement;

	private String result;

	private PlanStepStatus status;

	public int getStepIndex() {
		return stepIndex;
	}

	public void setStepIndex(int stepIndex) {
		this.stepIndex = stepIndex;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public PlanStepStatus getStatus() {
		return status;
	}

	public void setStatus(PlanStepStatus status) {
		this.status = status;
	}

	public String getStepRequirement() {
		return stepRequirement;
	}

	public void setStepRequirement(String stepRequirement) {
		this.stepRequirement = stepRequirement;
	}

	public String getStepInStr() {
		StringBuilder sb = new StringBuilder();
		sb.append(stepIndex);
		sb.append(". ");
		sb.append("[").append(status).append("]");
		sb.append(" ");
		sb.append(stepRequirement);

		return sb.toString();
	}

}

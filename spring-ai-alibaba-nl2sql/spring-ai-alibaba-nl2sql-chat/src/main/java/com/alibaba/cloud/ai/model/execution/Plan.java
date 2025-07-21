/*
 * Copyright 2024-2025 the original author or authors.
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

package com.alibaba.cloud.ai.model.execution;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class Plan {

	@JsonProperty("thought_process")
	private String thoughtProcess;

	@JsonProperty("execution_plan")
	private List<ExecutionStep> executionPlan;

	public Plan() {
	}

	public Plan(String thoughtProcess, List<ExecutionStep> executionPlan) {
		this.thoughtProcess = thoughtProcess;
		this.executionPlan = executionPlan;
	}

	public String getThoughtProcess() {
		return thoughtProcess;
	}

	public void setThoughtProcess(String thoughtProcess) {
		this.thoughtProcess = thoughtProcess;
	}

	public List<ExecutionStep> getExecutionPlan() {
		return executionPlan;
	}

	public void setExecutionPlan(List<ExecutionStep> executionPlan) {
		this.executionPlan = executionPlan;
	}

	@Override
	public String toString() {
		return "Plan{" + "thoughtProcess='" + thoughtProcess + '\'' + ", executionPlan=" + executionPlan + '}';
	}

	public String toJsonStr() {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			return objectMapper.writeValueAsString(this);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to convert object to JSON string", e);
		}
	}

}

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

package com.alibaba.cloud.ai.manus.coordinator.service;

import java.util.Map;

/**
 * Coordinator Result Wrapper Class
 */
public class CoordinatorResult {

	private String sessionId;

	private Map<String, Object> inputParameters;

	private int parametersCount;

	private String executionStatus;

	private String executionResult;

	public CoordinatorResult() {
	}

	public CoordinatorResult(String sessionId, Map<String, Object> inputParameters, int parametersCount,
			String executionStatus, String executionResult) {
		this.sessionId = sessionId;
		this.inputParameters = inputParameters;
		this.parametersCount = parametersCount;
		this.executionStatus = executionStatus;
		this.executionResult = executionResult;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public Map<String, Object> getInputParameters() {
		return inputParameters;
	}

	public void setInputParameters(Map<String, Object> inputParameters) {
		this.inputParameters = inputParameters;
	}

	public int getParametersCount() {
		return parametersCount;
	}

	public void setParametersCount(int parametersCount) {
		this.parametersCount = parametersCount;
	}

	public String getExecutionStatus() {
		return executionStatus;
	}

	public void setExecutionStatus(String executionStatus) {
		this.executionStatus = executionStatus;
	}

	public String getExecutionResult() {
		return executionResult;
	}

	public void setExecutionResult(String executionResult) {
		this.executionResult = executionResult;
	}

}

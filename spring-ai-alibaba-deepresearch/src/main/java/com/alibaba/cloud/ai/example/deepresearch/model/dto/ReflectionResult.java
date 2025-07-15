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

package com.alibaba.cloud.ai.example.deepresearch.model.dto;

/**
 * Reflection result DTO for parsing reflection agent's JSON response
 *
 * @author sixiyida
 * @since 2025/7/10
 */
public class ReflectionResult {

	/**
	 * Whether the evaluation passed
	 */
	private boolean passed;

	/**
	 * Evaluation feedback
	 */
	private String feedback;

	/**
	 * Previous execution result, used to provide context during regeneration
	 */
	private String executionResult;

	public ReflectionResult() {
	}

	public ReflectionResult(boolean passed, String feedback) {
		this.passed = passed;
		this.feedback = feedback;
	}

	public ReflectionResult(boolean passed, String feedback, String executionResult) {
		this.passed = passed;
		this.feedback = feedback;
		this.executionResult = executionResult;
	}

	public boolean isPassed() {
		return passed;
	}

	public void setPassed(boolean passed) {
		this.passed = passed;
	}

	public String getFeedback() {
		return feedback;
	}

	public void setFeedback(String feedback) {
		this.feedback = feedback;
	}

	public String getExecutionResult() {
		return executionResult;
	}

	public void setExecutionResult(String executionResult) {
		this.executionResult = executionResult;
	}

	public boolean hasExecutionResult() {
		return executionResult != null && !executionResult.trim().isEmpty();
	}

	@Override
	public String toString() {
		return String.format("ReflectionResult{passed=%s, feedback='%s', executionResult='%s'}", passed, feedback,
				executionResult);
	}

}

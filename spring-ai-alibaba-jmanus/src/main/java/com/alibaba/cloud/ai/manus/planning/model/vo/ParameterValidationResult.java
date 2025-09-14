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

package com.alibaba.cloud.ai.manus.planning.model.vo;

import java.util.List;

/**
 * Parameter validation result class for storing parameter validation result information
 */
public class ParameterValidationResult {

	/**
	 * Whether validation passed
	 */
	private boolean valid;

	/**
	 * List of missing parameters
	 */
	private List<String> missingParameters;

	/**
	 * List of found parameters
	 */
	private List<String> foundParameters;

	/**
	 * Validation result message
	 */
	private String message;

	/**
	 * Default constructor
	 */
	public ParameterValidationResult() {
		this.valid = false;
		this.missingParameters = new java.util.ArrayList<>();
		this.foundParameters = new java.util.ArrayList<>();
		this.message = "";
	}

	/**
	 * Constructor with parameters
	 */
	public ParameterValidationResult(boolean valid, List<String> missingParameters, List<String> foundParameters,
			String message) {
		this.valid = valid;
		this.missingParameters = missingParameters != null ? missingParameters : new java.util.ArrayList<>();
		this.foundParameters = foundParameters != null ? foundParameters : new java.util.ArrayList<>();
		this.message = message != null ? message : "";
	}

	// Getters and Setters
	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public List<String> getMissingParameters() {
		return missingParameters;
	}

	public void setMissingParameters(List<String> missingParameters) {
		this.missingParameters = missingParameters;
	}

	public List<String> getFoundParameters() {
		return foundParameters;
	}

	public void setFoundParameters(List<String> foundParameters) {
		this.foundParameters = foundParameters;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Add missing parameter
	 */
	public void addMissingParameter(String parameter) {
		if (this.missingParameters == null) {
			this.missingParameters = new java.util.ArrayList<>();
		}
		this.missingParameters.add(parameter);
	}

	/**
	 * Add found parameter
	 */
	public void addFoundParameter(String parameter) {
		if (this.foundParameters == null) {
			this.foundParameters = new java.util.ArrayList<>();
		}
		this.foundParameters.add(parameter);
	}

	/**
	 * Check if there are critical missing parameters
	 */
	public boolean hasCriticalMissingParameters() {
		return missingParameters != null && !missingParameters.isEmpty();
	}

	/**
	 * Get count of missing parameters
	 */
	public int getMissingParameterCount() {
		return missingParameters != null ? missingParameters.size() : 0;
	}

	/**
	 * Get count of found parameters
	 */
	public int getFoundParameterCount() {
		return foundParameters != null ? foundParameters.size() : 0;
	}

	@Override
	public String toString() {
		return "ParameterValidationResult{" + "valid=" + valid + ", missingParameters=" + missingParameters
				+ ", foundParameters=" + foundParameters + ", message='" + message + '\'' + '}';
	}

}

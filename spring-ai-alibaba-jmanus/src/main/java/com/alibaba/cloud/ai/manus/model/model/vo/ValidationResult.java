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
package com.alibaba.cloud.ai.manus.model.model.vo;

import java.util.List;

/**
 * Validation result data transfer object
 */
public class ValidationResult {

	private boolean valid;

	private String message;

	private List<AvailableModel> availableModels;

	public ValidationResult() {
	}

	public ValidationResult(boolean valid, String message) {
		this.valid = valid;
		this.message = message;
	}

	public ValidationResult(boolean valid, String message, List<AvailableModel> availableModels) {
		this.valid = valid;
		this.message = message;
		this.availableModels = availableModels;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public List<AvailableModel> getAvailableModels() {
		return availableModels;
	}

	public void setAvailableModels(List<AvailableModel> availableModels) {
		this.availableModels = availableModels;
	}

}

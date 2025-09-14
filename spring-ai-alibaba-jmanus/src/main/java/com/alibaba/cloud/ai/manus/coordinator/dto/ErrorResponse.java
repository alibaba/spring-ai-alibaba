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
package com.alibaba.cloud.ai.manus.coordinator.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard error response DTO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

	private String errorCode;

	private String message;

	private String details;

	private long timestamp;

	public ErrorResponse() {
		this.timestamp = System.currentTimeMillis();
	}

	public ErrorResponse(String errorCode, String message) {
		this();
		this.errorCode = errorCode;
		this.message = message;
	}

	public ErrorResponse(String errorCode, String message, String details) {
		this(errorCode, message);
		this.details = details;
	}

	// Getters and setters
	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getDetails() {
		return details;
	}

	public void setDetails(String details) {
		this.details = details;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

}

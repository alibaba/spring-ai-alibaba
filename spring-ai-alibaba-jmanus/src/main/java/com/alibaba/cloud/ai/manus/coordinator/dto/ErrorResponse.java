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

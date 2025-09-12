package com.alibaba.cloud.ai.manus.coordinator.exception;

/**
 * Custom exception for coordinator tool operations
 */
public class CoordinatorToolException extends RuntimeException {

	private final String errorCode;

	public CoordinatorToolException(String message) {
		super(message);
		this.errorCode = "COORDINATOR_TOOL_ERROR";
	}

	public CoordinatorToolException(String errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public CoordinatorToolException(String message, Throwable cause) {
		super(message, cause);
		this.errorCode = "COORDINATOR_TOOL_ERROR";
	}

	public String getErrorCode() {
		return errorCode;
	}

}

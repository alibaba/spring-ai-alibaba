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

import java.time.LocalDateTime;

/**
 * Unified Result Wrapper Class
 *
 * @param <T> Data type
 */
public class Result<T> {

	private boolean success;

	private String message;

	private String errorCode;

	private T data;

	private LocalDateTime timestamp;

	public Result() {
		this.timestamp = LocalDateTime.now();
	}

	public Result(boolean success, String message, String errorCode, T data) {
		this.success = success;
		this.message = message;
		this.errorCode = errorCode;
		this.data = data;
		this.timestamp = LocalDateTime.now();
	}

	/**
	 * Create success result
	 */
	public static <T> Result<T> success(T data) {
		return new Result<>(true, "Operation successful", null, data);
	}

	/**
	 * Create success result (with message)
	 */
	public static <T> Result<T> success(T data, String message) {
		return new Result<>(true, message, null, data);
	}

	/**
	 * Create failure result
	 */
	public static <T> Result<T> failure(String errorCode, String message) {
		return new Result<>(false, message, errorCode, null);
	}

	/**
	 * Create failure result (with data)
	 */
	public static <T> Result<T> failure(String errorCode, String message, T data) {
		return new Result<>(false, message, errorCode, data);
	}

	// Getters and Setters
	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public String toString() {
		return "Result{" + "success=" + success + ", message='" + message + '\'' + ", errorCode='" + errorCode + '\''
				+ ", data=" + data + ", timestamp=" + timestamp + '}';
	}

}

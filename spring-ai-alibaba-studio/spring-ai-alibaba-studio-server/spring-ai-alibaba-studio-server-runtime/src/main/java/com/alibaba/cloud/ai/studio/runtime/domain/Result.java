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

package com.alibaba.cloud.ai.studio.runtime.domain;

import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * A generic result wrapper class for API responses. This class encapsulates the response
 * data, status, and error information.
 *
 * @since 1.0.0.3
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Result<T> implements Serializable {

	/** Unique identifier for the request */
	@JsonProperty("request_id")
	private String requestId;

	/** The actual response data */
	private T data;

	/** Flag indicating if the request was successful */
	@JsonIgnore
	private boolean success = true;

	/** Error code if the request failed */
	private String code;

	/** Error message if the request failed */
	private String message;

	/**
	 * Creates a successful result with data
	 * @param data The response data
	 * @return Result instance
	 */
	public static <T> Result<T> success(T data) {
		return success(null, data);
	}

	/**
	 * Creates a successful result with request ID and data
	 * @param requestId The request identifier
	 * @param data The response data
	 * @return Result instance
	 */
	public static <T> Result<T> success(String requestId, T data) {
		return Result.<T>builder().success(true).requestId(requestId).data(data).build();
	}

	/**
	 * Creates an error result with error code
	 * @param errorCode The error code
	 * @return Result instance
	 */
	public static <T> Result<T> error(ErrorCode errorCode) {
		return error(null, errorCode);
	}

	/**
	 * Creates an error result with request ID and error code
	 * @param requestId The request identifier
	 * @param errorCode The error code
	 * @return Result instance
	 */
	public static <T> Result<T> error(String requestId, ErrorCode errorCode) {
		return Result.<T>builder()
			.requestId(requestId)
			.success(false)
			.code(errorCode.getCode())
			.message(errorCode.toError().getMessage())
			.build();
	}

	/**
	 * Creates an error result with request ID and error details
	 * @param requestId The request identifier
	 * @param error The error details
	 * @return Result instance
	 */
	public static <T> Result<T> error(String requestId, Error error) {
		return Result.<T>builder()
			.requestId(requestId)
			.success(false)
			.code(error.getCode())
			.message(error.getMessage())
			.build();
	}

}

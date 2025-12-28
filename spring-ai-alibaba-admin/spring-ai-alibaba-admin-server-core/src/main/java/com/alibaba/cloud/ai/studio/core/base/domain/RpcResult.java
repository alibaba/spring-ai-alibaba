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

package com.alibaba.cloud.ai.studio.core.base.domain;

import lombok.Getter;
import lombok.Setter;

/**
 * A wrapper class for RPC (Remote Procedure Call) results. Provides standardized response
 * format with success status, error handling, and response data.
 *
 * @since 1.0.0.3
 */
@Getter
@Setter
public class RpcResult {

	/** Indicates if the RPC call was successful */
	private boolean success;

	/** HTTP status code or custom error code */
	private Integer code;

	/** Error message or description */
	private String message;

	/** Response data from the RPC call */
	private Object response;

	/** Indicates if the RPC call timed out */
	private boolean timeout = false;

	/** Original raw response from the RPC call */
	@Setter
	private String originResponse;

	public RpcResult() {
		this.code = 200;
	}

	public static RpcResult create() {
		return new RpcResult();
	}

	public static RpcResult success(Object response) {
		RpcResult result = create();
		result.setSuccess(true);
		result.setResponse(response);
		result.setCode(200);
		return result;
	}

	public static RpcResult error(int code, String message) {
		RpcResult result = create();
		result.setSuccess(false);
		result.setCode(code);
		result.setMessage(message);
		return result;
	}

}

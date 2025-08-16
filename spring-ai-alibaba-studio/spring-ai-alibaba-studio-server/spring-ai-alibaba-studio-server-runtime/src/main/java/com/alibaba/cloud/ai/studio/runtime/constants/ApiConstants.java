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

package com.alibaba.cloud.ai.studio.runtime.constants;

/**
 * Constants for the client module. Contains error codes and timeout configurations.
 *
 * @since 1.0.0.3
 */
public interface ApiConstants {

	/** Error code for invalid request */
	String INVALID_REQUEST_ERROR = "invalid_request_error";

	/** Error code for response errors */
	String RESPONSE_ERROR = "response_error";

	/** Error code for cancelled operations */
	String OPERATION_CANCEL = "operation_cancel";

	/** Default timeout for RPC requests in milliseconds */
	int RPC_REQUEST_TIMEOUT = 15000;

	/** Default timeout for RPC model requests in milliseconds */
	int RPC_MODEL_REQUEST_TIMEOUT = 180000;

	/** Prefix for token authentication */
	String TOKEN_PREFIX = "Bearer";

	/** Key for access token */
	String ACCESS_TOKEN = "access_token";

}

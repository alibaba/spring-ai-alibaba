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

/**
 * Tool Operation Error Code Enum
 */
public enum ToolErrorCode {

	TOOL_NOT_FOUND("TOOL_001", "Tool not found"), TOOL_ALREADY_EXISTS("TOOL_002", "Tool already exists"),
	NETWORK_ERROR("TOOL_003", "Network connection error"), PERMISSION_DENIED("TOOL_004", "Insufficient permissions"),
	INVALID_PARAMETER("TOOL_005", "Invalid parameter"), SERVER_ERROR("TOOL_006", "Internal server error"),
	TIMEOUT_ERROR("TOOL_007", "Operation timeout"), PUBLISH_FAILED("TOOL_008", "Publication failed"),
	UNPUBLISH_FAILED("TOOL_009", "Unpublishing failed"), LOAD_FAILED("TOOL_010", "Loading failed"),
	REFRESH_FAILED("TOOL_011", "Refresh failed");

	private final String code;

	private final String defaultMessage;

	ToolErrorCode(String code, String defaultMessage) {
		this.code = code;
		this.defaultMessage = defaultMessage;
	}

	public String getCode() {
		return code;
	}

	public String getDefaultMessage() {
		return defaultMessage;
	}

	@Override
	public String toString() {
		return code;
	}

}

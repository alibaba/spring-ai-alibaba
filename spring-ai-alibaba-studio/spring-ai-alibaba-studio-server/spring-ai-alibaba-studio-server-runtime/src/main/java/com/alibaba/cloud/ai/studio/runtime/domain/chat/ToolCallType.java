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

package com.alibaba.cloud.ai.studio.runtime.domain.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enum representing different types of tool calls in the system. Each type corresponds to
 * a specific interaction pattern between the client and tools.
 *
 * @since 1.0.0.3
 */

@Getter
@AllArgsConstructor
public enum ToolCallType {

	/** Standard function call type */
	@JsonProperty("function")
	FUNCTION("function"),

	/** Basic tool call type */
	@JsonProperty("tool_call")
	TOOL_CALL("tool_call"),

	/** Result of a tool call */
	@JsonProperty("tool_result")
	TOOL_RESULT("tool_result"),

	/** MCP tool call type */
	@JsonProperty("mcp_tool_call")
	MCP_TOOL_CALL("mcp_tool_call"),

	/** Result of an MCP tool call */
	@JsonProperty("mcp_tool_result")
	MCP_TOOL_RESULT("mcp_tool_result"),

	/** Component-specific tool call type */
	@JsonProperty("component_tool_call")
	COMPONENT_TOOL_CALL("component_tool_call"),

	/** Result of a component tool call */
	@JsonProperty("component_tool_result")
	COMPONENT_TOOL_RESULT("component_tool_result"),

	/** File search operation call type */
	@JsonProperty("file_search_call")
	FILE_SEARCH_CALL("file_search_call"),

	/** Result of a file search operation */
	@JsonProperty("file_search_result")
	FILE_SEARCH_RESULT("file_search_result"),;

	/** The string value representing this tool call type */
	private final String value;

}

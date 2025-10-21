/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.interceptor;

import org.springframework.ai.chat.messages.ToolResponseMessage;

/**
 * Response object for tool calls.
 */
public class ToolCallResponse {

	private final String result;
	private final String toolName;
	private final String toolCallId;

	public ToolCallResponse(String result, String toolName, String toolCallId) {
		this.result = result;
		this.toolName = toolName;
		this.toolCallId = toolCallId;
	}

	public String getResult() {
		return result;
	}

	public String getToolName() {
		return toolName;
	}

	public String getToolCallId() {
		return toolCallId;
	}

	public ToolResponseMessage.ToolResponse toToolResponse() {
		return new ToolResponseMessage.ToolResponse(toolCallId, toolName, result);
	}

	public static ToolCallResponse of(String toolCallId, String toolName, String result) {
		return new ToolCallResponse(result, toolName, toolCallId);
	}
}


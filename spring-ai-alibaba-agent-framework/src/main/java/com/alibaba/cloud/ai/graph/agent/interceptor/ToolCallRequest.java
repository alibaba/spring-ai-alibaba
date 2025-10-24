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

import org.springframework.ai.chat.messages.AssistantMessage;

/**
 * Request object for tool calls.
 */
public class ToolCallRequest {

	private final String toolName;
	private final String arguments;
	private final String toolCallId;

	public ToolCallRequest(String toolName, String arguments, String toolCallId) {
		this.toolName = toolName;
		this.arguments = arguments;
		this.toolCallId = toolCallId;
	}

	public static ToolCallRequest from(AssistantMessage.ToolCall toolCall) {
		return new ToolCallRequest(toolCall.name(), toolCall.arguments(), toolCall.id());
	}

	public String getToolName() {
		return toolName;
	}

	public String getArguments() {
		return arguments;
	}

	public String getToolCallId() {
		return toolCallId;
	}
}


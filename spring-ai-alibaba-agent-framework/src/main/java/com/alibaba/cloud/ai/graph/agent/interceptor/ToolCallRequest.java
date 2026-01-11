/*
 * Copyright 2024-2026 the original author or authors.
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

import java.util.Map;

/**
 * Request object for tool calls.
 */
public class ToolCallRequest {

	private final String toolName;
	private final String arguments;
	private final String toolCallId;
	private final Map<String, Object> context;

	public ToolCallRequest(String toolName, String arguments, String toolCallId, Map<String, Object> context) {
		this.toolName = toolName;
		this.arguments = arguments;
		this.toolCallId = toolCallId;
		this.context = context;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static Builder builder(ToolCallRequest request) {
		return new Builder()
				.toolName(request.toolName)
				.arguments(request.arguments)
				.toolCallId(request.toolCallId)
				.context(request.context);
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

	public Map<String, Object> getContext() {
		return context;
	}

	public static class Builder {
		private String toolName;
		private String arguments;
		private String toolCallId;
		private Map<String, Object> context;

		public Builder toolCall(AssistantMessage.ToolCall toolCall) {
			this.toolName = toolCall.name();
			this.arguments = toolCall.arguments();
			this.toolCallId = toolCall.id();
			return this;
		}

		public Builder toolName(String toolName) {
			this.toolName = toolName;
			return this;
		}

		public Builder arguments(String arguments) {
			this.arguments = arguments;
			return this;
		}

		public Builder toolCallId(String toolCallId) {
			this.toolCallId = toolCallId;
			return this;
		}

		public Builder context(Map<String, Object> context) {
			this.context = context;
			return this;
		}

		public ToolCallRequest build() {
			return new ToolCallRequest(toolName, arguments, toolCallId, context);
		}
	}
}


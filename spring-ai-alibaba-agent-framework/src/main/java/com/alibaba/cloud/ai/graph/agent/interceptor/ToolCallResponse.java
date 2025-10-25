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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Response object for tool calls.
 */
public class ToolCallResponse {

	private final String result;
	private final String toolName;
	private final String toolCallId;
	private final String status;
	private final Map<String, Object> metadata;

	public ToolCallResponse(String result, String toolName, String toolCallId) {
		this(result, toolName, toolCallId, null, null);
	}

	public ToolCallResponse(String result, String toolName, String toolCallId, String status, Map<String, Object> metadata) {
		this.result = result;
		this.toolName = toolName;
		this.toolCallId = toolCallId;
		this.status = status;
		this.metadata = metadata != null ? new HashMap<>(metadata) : Collections.emptyMap();
	}

	public static ToolCallResponse of(String toolCallId, String toolName, String result) {
		return new ToolCallResponse(result, toolName, toolCallId);
	}

	public static Builder builder() {
		return new Builder();
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

	public String getStatus() {
		return status;
	}

	public Map<String, Object> getMetadata() {
		return Collections.unmodifiableMap(metadata);
	}

	public ToolResponseMessage.ToolResponse toToolResponse() {
		return new ToolResponseMessage.ToolResponse(toolCallId, toolName, result);
	}

	public static class Builder {
		private String content;
		private String toolName;
		private String toolCallId;
		private String status;
		private Map<String, Object> metadata;

		public Builder content(String content) {
			this.content = content;
			return this;
		}

		public Builder toolName(String toolName) {
			this.toolName = toolName;
			return this;
		}

		public Builder toolCallId(String toolCallId) {
			this.toolCallId = toolCallId;
			return this;
		}

		public Builder status(String status) {
			this.status = status;
			return this;
		}

		public Builder metadata(Map<String, Object> metadata) {
			this.metadata = metadata;
			return this;
		}

		public ToolCallResponse build() {
			return new ToolCallResponse(content, toolName, toolCallId, status, metadata);
		}
	}
}


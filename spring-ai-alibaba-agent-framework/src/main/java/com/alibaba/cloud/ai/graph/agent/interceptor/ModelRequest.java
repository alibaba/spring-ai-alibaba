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

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Request object for model calls.
 * Contains all information needed to make a model invocation.
 */
public class ModelRequest {
	private final SystemMessage systemMessage;
	private final Map<String, Object> context;
	private final List<Message> messages;
	private final ToolCallingChatOptions options;
	// tools working for current request, leave it to empty if all default in options should be used.
	private final List<String> tools;
	// dynamic tool callbacks for current request.
	private final List<ToolCallback> dynamicToolCallbacks;

	// tool descriptions for tool selection, mapping tool name to description.
	private final Map<String, String> toolDescriptions;

	public ModelRequest(SystemMessage systemMessage, List<Message> messages, ToolCallingChatOptions options,
			List<String> tools, List<ToolCallback> dynamicToolCallbacks, Map<String, String> toolDescriptions,
			Map<String, Object> context) {
		this.systemMessage = systemMessage;
		this.messages = messages;
		this.options = options;
		this.tools = tools;
		this.dynamicToolCallbacks = dynamicToolCallbacks;
		this.toolDescriptions = toolDescriptions;
		this.context = context;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static Builder builder(ModelRequest request) {
		return new Builder()
				.systemMessage(request.systemMessage)
				.messages(request.messages)
				.options(request.options)
				.tools(request.tools)
				.dynamicToolCallbacks(request.dynamicToolCallbacks)
				.toolDescriptions(request.toolDescriptions)
				.context(request.context);
	}

	public List<Message> getMessages() {
		return messages;
	}

	public SystemMessage getSystemMessage() {
		return systemMessage;
	}

	public ToolCallingChatOptions getOptions() {
		return options;
	}

	public List<String> getTools() {
		return tools;
	}

	public List<ToolCallback> getDynamicToolCallbacks() {
		return dynamicToolCallbacks;
	}

	public Map<String, String> getToolDescriptions() {
		return toolDescriptions;
	}

	public Map<String, Object> getContext() {
		return context;
	}

	public static class Builder {
		private SystemMessage systemMessage;
		private List<Message> messages;
		private ToolCallingChatOptions options;
		private List<String> tools = List.of();
		private List<ToolCallback> dynamicToolCallbacks = List.of();

		private Map<String, String> toolDescriptions = new HashMap<>();

		private Map<String, Object> context = new HashMap<>();

		public Builder systemMessage(SystemMessage systemMessage) {
			this.systemMessage = systemMessage;
			return this;
		}

		public Builder messages(List<Message> messages) {
			this.messages = messages;
			return this;
		}

		public Builder options(ToolCallingChatOptions options) {
			this.options = options;
			return this;
		}

		public Builder tools(List<String> tools) {
			if (tools != null) {
				this.tools = new ArrayList<>(tools);
			}
			return this;
		}

		public Builder dynamicToolCallbacks(List<ToolCallback> dynamicToolCallbacks) {
			if (dynamicToolCallbacks != null) {
				this.dynamicToolCallbacks = new ArrayList<>(dynamicToolCallbacks);
			}
			return this;
		}

		public Builder toolDescriptions(Map<String, String> toolDescriptions) {
			if (toolDescriptions != null) {
				this.toolDescriptions = new HashMap<>(toolDescriptions);
			}
			return this;
		}

		public Builder context(Map<String, Object> context) {
			if (context != null) {
				this.context = new HashMap<>(context);
			}
			return this;
		}

		public ModelRequest build() {
			return new ModelRequest(systemMessage, messages, options, tools, dynamicToolCallbacks, toolDescriptions,
					context);
		}
	}
}


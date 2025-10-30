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

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Request object for model calls.
 * Contains all information needed to make a model invocation.
 */
public class ModelRequest {

	private final Map<String, Object> context;
	private final List<Message> messages;
	private final ChatOptions options;
	private final List<String> tools;

	public ModelRequest(List<Message> messages, ChatOptions options, List<String> tools, Map<String, Object> context) {
		this.messages = messages;
		this.options = options;
		this.tools = tools;
		this.context = context;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static Builder builder(ModelRequest request) {
		return new Builder()
				.messages(request.messages)
				.options(request.options)
				.tools(request.tools)
				.context(request.context);
	}

	public List<Message> getMessages() {
		return messages;
	}

	public ChatOptions getOptions() {
		return options;
	}

	public List<String> getTools() {
		return tools;
	}

	public Map<String, Object> getContext() {
		return context;
	}

	public static class Builder {
		private List<Message> messages;
		private ChatOptions options;
		private List<String> tools;
		private Map<String, Object> context;

		public Builder messages(List<Message> messages) {
			this.messages = messages;
			return this;
		}

		public Builder options(ChatOptions options) {
			this.options = options;
			return this;
		}

		public Builder tools(List<String> tools) {
			this.tools = tools;
			return this;
		}

		public Builder context(Map<String, Object> context) {
			this.context = new HashMap<>(context);
			return this;
		}

		public ModelRequest build() {
			return new ModelRequest(messages, options, tools, context);
		}
	}
}


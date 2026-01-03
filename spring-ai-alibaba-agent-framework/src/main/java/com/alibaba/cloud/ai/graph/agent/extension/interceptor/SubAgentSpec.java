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
package com.alibaba.cloud.ai.graph.agent.extension.interceptor;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

/**
 * Specification for creating a subagent.
 *
 * This class defines the configuration for a custom subagent that can be used
 * with the SubAgentInterceptor. Each subagent has its own name, description,
 * system prompt, and optionally custom tools and interceptors.
 */
public class SubAgentSpec {

	private final String name;
	private final String description;
	private final String systemPrompt;
	private final ChatModel model;
	private final List<ToolCallback> tools;
	private final List<ModelInterceptor> interceptors;
	private final boolean enableLoopingLog;

	private SubAgentSpec(Builder builder) {
		this.name = builder.name;
		this.description = builder.description;
		this.systemPrompt = builder.systemPrompt;
		this.model = builder.model;
		this.tools = builder.tools;
		this.interceptors = builder.interceptors;
		this.enableLoopingLog = builder.enableLoopingLog;
	}

	public static Builder builder() {
		return new Builder();
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getSystemPrompt() {
		return systemPrompt;
	}

	public ChatModel getModel() {
		return model;
	}

	public List<ToolCallback> getTools() {
		return tools;
	}

	public List<ModelInterceptor> getInterceptors() {
		return interceptors;
	}

	public boolean isEnableLoopingLog() {
		return enableLoopingLog;
	}

	public static class Builder {
		private String name;
		private String description;
		private String systemPrompt;
		private ChatModel model;
		private List<ToolCallback> tools;
		private List<ModelInterceptor> interceptors;
		private boolean enableLoopingLog;

		/**
		 * Set the name of the subagent (required).
		 */
		public Builder name(String name) {
			this.name = name;
			return this;
		}

		/**
		 * Set the description of the subagent (required).
		 * This is used by the main agent to decide whether to call the subagent.
		 */
		public Builder description(String description) {
			this.description = description;
			return this;
		}

		/**
		 * Set the system prompt for the subagent (required).
		 * This is used as the instruction when the subagent is invoked.
		 */
		public Builder systemPrompt(String systemPrompt) {
			this.systemPrompt = systemPrompt;
			return this;
		}

		/**
		 * Set a custom model for this subagent.
		 * If not set, the default model from SubAgentInterceptor will be used.
		 */
		public Builder model(ChatModel model) {
			this.model = model;
			return this;
		}

		/**
		 * Set custom tools for this subagent.
		 * If not set, the default tools from SubAgentInterceptor will be used.
		 */
		public Builder tools(List<ToolCallback> tools) {
			this.tools = tools;
			return this;
		}

		/**
		 * Set custom interceptors for this subagent.
		 * These will be applied after the default interceptors from SubAgentInterceptor.
		 */
		public Builder interceptors(List<ModelInterceptor> interceptors) {
			this.interceptors = interceptors;
			return this;
		}

		public Builder enableLoopingLog(boolean enableLoopingLog) {
			this.enableLoopingLog = enableLoopingLog;
			return this;
		}

		public SubAgentSpec build() {
			if (name == null || name.trim().isEmpty()) {
				throw new IllegalArgumentException("SubAgent name is required");
			}
			if (description == null || description.trim().isEmpty()) {
				throw new IllegalArgumentException("SubAgent description is required");
			}
			if (systemPrompt == null || systemPrompt.trim().isEmpty()) {
				throw new IllegalArgumentException("SubAgent system prompt is required");
			}
			return new SubAgentSpec(this);
		}
	}
}


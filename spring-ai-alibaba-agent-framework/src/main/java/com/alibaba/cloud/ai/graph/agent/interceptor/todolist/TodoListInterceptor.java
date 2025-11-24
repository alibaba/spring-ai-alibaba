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
package com.alibaba.cloud.ai.graph.agent.interceptor.todolist;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.agent.tools.WriteTodosTool;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.tool.ToolCallback;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;

import static com.alibaba.cloud.ai.graph.agent.tools.WriteTodosTool.DEFAULT_TOOL_DESCRIPTION;

/**
 * Model interceptor that provides todo list management capabilities to agents.
 *
 * This interceptor enhances the system prompt to guide agents on using todo lists
 * for complex multi-step operations. It helps agents:
 * - Track progress on complex tasks
 * - Organize work into manageable steps
 * - Provide users with visibility into task completion
 *
 * The interceptor automatically injects system prompts that guide the agent on when
 * and how to use the todo functionality effectively.
 *
 * Example:
 * TodoListInterceptor interceptor = TodoListInterceptor.builder()
 *     .systemPrompt("Custom guidance for using todos...")
 *     .build();
 */
public class TodoListInterceptor extends ModelInterceptor {

	private static final String DEFAULT_SYSTEM_PROMPT = """
			## `write_todos`
			
			You have access to the `write_todos` tool to help you manage and plan complex objectives.
			Use this tool for complex objectives to ensure that you are tracking each necessary step and giving the user visibility into your progress.
			This tool is very helpful for planning complex objectives, and for breaking down these larger complex objectives into smaller steps.
			
			It is critical that you mark todos as completed as soon as you are done with a step. Do not batch up multiple steps before marking them as completed.
			For simple objectives that only require a few steps, it is better to just complete the objective directly and NOT use this tool.
			Writing todos takes time and tokens, use it when it is helpful for managing complex many-step problems! But not for simple few-step requests.
			
			## Important To-Do List Usage Notes to Remember
			- The `write_todos` tool should never be called multiple times in parallel.
			- Don't be afraid to revise the To-Do list as you go. New information may reveal new tasks that need to be done, or old tasks that are irrelevant.
			""";

	private final List<ToolCallback> tools;
	private final String systemPrompt;
	private final String toolDescription;

	private TodoListInterceptor(Builder builder) {
		// Create the write_todos tool with the custom description
		this.tools = Collections.singletonList(
				WriteTodosTool.builder().
						withName("write_todos")
						.withDescription(builder.toolDescription)
						.build()
		);
		this.systemPrompt = builder.systemPrompt;
		this.toolDescription = builder.toolDescription;
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public List<ToolCallback> getTools() {
		return tools;
	}

	@Override
	public String getName() {
		return "TodoList";
	}

	@Override
	public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
		SystemMessage enhancedSystemMessage;

		if (request.getSystemMessage() == null) {
			enhancedSystemMessage = new SystemMessage(this.systemPrompt);
		} else {
			enhancedSystemMessage = new SystemMessage(request.getSystemMessage().getText() + "\n\n" + systemPrompt);
		}

		// Create enhanced request
		ModelRequest enhancedRequest = ModelRequest.builder(request)
				.systemMessage(enhancedSystemMessage)
				.build();

		// Call the handler with enhanced request
		return handler.call(enhancedRequest);
	}

	/**
	 * Todo item status.
	 */
	@JsonFormat(shape = JsonFormat.Shape.STRING)
	public enum TodoStatus {
		PENDING("pending"),
		IN_PROGRESS("in_progress"),
		COMPLETED("completed");

		private final String value;

		TodoStatus(String value) {
			this.value = value;
		}

		@JsonCreator
		public static TodoStatus fromValue(String value) {
			if (value == null) {
				throw new IllegalArgumentException("Status value cannot be null");
			}

			// First try to match against the lowercase values
			for (TodoStatus status : values()) {
				if (status.value.equals(value)) {
					return status;
				}
			}

			// Fallback: try to match against enum constant names (case-insensitive)
			try {
				return TodoStatus.valueOf(value.toUpperCase());
			}
			catch (IllegalArgumentException e) {
				// If that fails too, throw a helpful error
				throw new IllegalArgumentException(
						"Unknown status: " + value + ". Valid values are: pending, in_progress, completed");
			}
		}

		@JsonValue
		public String getValue() {
			return value;
		}
	}

	/**
	 * Represents a single todo item.
	 */
	public static class Todo {
		private String content;
		private TodoStatus status;

		public Todo() {
		}

		public Todo(String content, TodoStatus status) {
			this.content = content;
			this.status = status;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public TodoStatus getStatus() {
			return status;
		}

		public void setStatus(TodoStatus status) {
			this.status = status;
		}

		@Override
		public String toString() {
			return String.format("Todo{content='%s', status=%s}", content, status);
		}
	}

	public static class Builder {
		private String systemPrompt = DEFAULT_SYSTEM_PROMPT;
		private String toolDescription = DEFAULT_TOOL_DESCRIPTION;

		/**
		 * Set a custom system prompt for guiding todo usage.
		 */
		public Builder systemPrompt(String systemPrompt) {
			this.systemPrompt = systemPrompt;
			return this;
		}

		/**
		 * Set a custom tool description for the write_todos tool.
		 */
		public Builder toolDescription(String toolDescription) {
			this.toolDescription = toolDescription;
			return this;
		}

		public TodoListInterceptor build() {
			return new TodoListInterceptor(this);
		}
	}
}


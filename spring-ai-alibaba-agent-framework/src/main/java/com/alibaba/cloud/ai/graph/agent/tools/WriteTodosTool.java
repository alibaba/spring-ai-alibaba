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
package com.alibaba.cloud.ai.graph.agent.tools;

import com.alibaba.cloud.ai.graph.agent.interceptor.todolist.TodoListInterceptor.Todo;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Tool for writing and managing todos in the agent workflow.
 * This tool allows agents to create, update, and track task lists.
 *
 */
public class WriteTodosTool implements BiFunction<WriteTodosTool.Request, ToolContext, WriteTodosTool.Response> {

	public WriteTodosTool() {
	}

	@Override
	public Response apply(Request request, ToolContext toolContext) {
		// Extract state from ToolContext
		Map<String, Object> contextData = toolContext.getContext();
		Map<String, Object> extraState = (Map<String, Object>)contextData.get("extraState");

		// Update the state with todos
		extraState.put("todos", request.todos);

		// Return the tool response message
		return new Response("Updated todo list to " + request.todos);
	}


	@JsonClassDescription("Request to write or update todos")
	public record Request(
			@JsonProperty(required = true, value = "todos")
			@JsonPropertyDescription("List of todo items with content and status")
			List<Todo> todos
	) {}

	public record Response(String message) {}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String name = "write_todos";

		private String description = "Tool for writing and managing todos in the agent workflow. "
				+ "This tool allows agents to create, update, and track task lists.";

		public Builder() {
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder withDescription(String description) {
			this.description = description;
			return this;
		}

		public ToolCallback build() {
			return FunctionToolCallback.builder(name, new WriteTodosTool())
				.description(description)
				.inputType(Request.class)
				.build();
		}

	}
}


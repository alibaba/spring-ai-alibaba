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

import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_STATE_FOR_UPDATE_CONTEXT_KEY;

/**
 * Tool for writing and managing todos in the agent workflow.
 * This tool allows agents to create, update, and track task lists.
 *
 */
public class WriteTodosTool implements BiFunction<WriteTodosTool.Request, ToolContext, WriteTodosTool.Response> {
	public static final String DEFAULT_TOOL_DESCRIPTION = """
			Use this tool to create and manage a structured task list for your current work session. This helps you track progress, organize complex tasks, and demonstrate thoroughness to the user.
			
			Only use this tool if you think it will be helpful in staying organized. If the user's request is trivial and takes less than 3 steps, it is better to NOT use this tool and just do the task directly.
			
			## When to Use This Tool
			Use this tool in these scenarios:
			
			1. Complex multi-step tasks - When a task requires 3 or more distinct steps or actions
			2. Non-trivial and complex tasks - Tasks that require careful planning or multiple operations
			3. User explicitly requests todo list - When the user directly asks you to use the todo list
			4. User provides multiple tasks - When users provide a list of things to be done (numbered or comma-separated)
			5. The plan may need future revisions or updates based on results from the first few steps
			
			## How to Use This Tool
			1. When you start working on a task - Mark it as in_progress BEFORE beginning work.
			2. After completing a task - Mark it as completed and add any new follow-up tasks discovered during implementation.
			3. You can also update future tasks, such as deleting them if they are no longer necessary, or adding new tasks that are necessary. Don't change previously completed tasks.
			4. You can make several updates to the todo list at once. For example, when you complete a task, you can mark the next task you need to start as in_progress.
			
			## When NOT to Use This Tool
			It is important to skip using this tool when:
			1. There is only a single, straightforward task
			2. The task is trivial and tracking it provides no benefit
			3. The task can be completed in less than 3 trivial steps
			4. The task is purely conversational or informational
			
			## Task States and Management
			
			1. **Task States**: Use these states to track progress:
			   - pending: Task not yet started
			   - in_progress: Currently working on (you can have multiple tasks in_progress at a time if they are not related to each other and can be run in parallel)
			   - completed: Task finished successfully
			
			2. **Task Management**:
			   - Update task status in real-time as you work
			   - Mark tasks complete IMMEDIATELY after finishing (don't batch completions)
			   - Complete current tasks before starting new ones
			   - Remove tasks that are no longer relevant from the list entirely
			   - IMPORTANT: When you write this todo list, you should mark your first task (or tasks) as in_progress immediately!.
			   - IMPORTANT: Unless all tasks are completed, you should always have at least one task in_progress to show the user that you are working on something.
			
			3. **Task Completion Requirements**:
			   - ONLY mark a task as completed when you have FULLY accomplished it
			   - If you encounter errors, blockers, or cannot finish, keep the task as in_progress
			   - When blocked, create a new task describing what needs to be resolved
			   - Never mark a task as completed if:
			     - There are unresolved issues or errors
			     - Work is partial or incomplete
			     - You encountered blockers that prevent completion
			     - You couldn't find necessary resources or dependencies
			     - Quality standards haven't been met
			
			4. **Task Breakdown**:
			   - Create specific, actionable items
			   - Break complex tasks into smaller, manageable steps
			   - Use clear, descriptive task names
			
			Being proactive with task management demonstrates attentiveness and ensures you complete all requirements successfully
			Remember: If you only need to make a few tool calls to complete a task, and it is clear what you need to do, it is better to just do the task directly and NOT call this tool at all.
			""";

	public WriteTodosTool() {
	}

	@Override
	public Response apply(Request request, ToolContext toolContext) {
		try {
			// Extract state from ToolContext
			Map<String, Object> contextData = toolContext.getContext();
			if (contextData == null) {
				return new Response("Error: Tool context is not available");
			}

			Object extraStateObj = contextData.get(AGENT_STATE_FOR_UPDATE_CONTEXT_KEY);
			if (extraStateObj == null) {
				return new Response("Error: Extra state is not initialized");
			}

			if (!(extraStateObj instanceof Map)) {
				return new Response("Error: Extra state has invalid type");
			}

			@SuppressWarnings("unchecked")
		Map<String, Object> extraState = (Map<String, Object>) extraStateObj;

			// Update the state with todos
			extraState.put("todos", request.todos);

			// Return the tool response message
			return new Response("Updated todo list to " + request.todos);

		}
		catch (ClassCastException e) {
			return new Response("Error: Invalid state type - " + e.getMessage());
		}
		catch (Exception e) {
			return new Response("Error: Failed to update todos - " + e.getMessage());
		}
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

		private String description = DEFAULT_TOOL_DESCRIPTION;

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


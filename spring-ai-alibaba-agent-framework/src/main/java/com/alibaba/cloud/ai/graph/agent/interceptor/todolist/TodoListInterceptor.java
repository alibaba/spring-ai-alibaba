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

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;

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
 * <pre>
 * TodoListInterceptor interceptor = TodoListInterceptor.builder()
 *     .systemPrompt("Custom guidance for using todos...")
 *     .build();
 * </pre>
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

	private static final String DEFAULT_TOOL_DESCRIPTION = """
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
		// Enhance the system prompt with todo guidance
		List<Message> enhancedMessages = new ArrayList<>(request.getMessages());

		// Check if there's already a system message
		boolean hasSystemMessage = enhancedMessages.stream()
				.anyMatch(msg -> msg instanceof SystemMessage);

		if (hasSystemMessage) {
			// Append to existing system message
			for (int i = 0; i < enhancedMessages.size(); i++) {
				Message msg = enhancedMessages.get(i);
				if (msg instanceof SystemMessage systemMsg) {
					String enhancedContent = systemMsg.getText() + "\n\n" + systemPrompt;
					enhancedMessages.set(i, new SystemMessage(enhancedContent));
					break;
				}
			}
		}
		else {
			// Add new system message at the beginning
			enhancedMessages.add(0, new SystemMessage(systemPrompt));
		}

		// Create enhanced request
		ModelRequest enhancedRequest = ModelRequest.builder()
				.messages(enhancedMessages)
				.options(request.getOptions())
				.tools(request.getTools())
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


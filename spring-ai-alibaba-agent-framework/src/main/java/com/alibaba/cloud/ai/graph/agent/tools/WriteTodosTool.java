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
package com.alibaba.cloud.ai.graph.agent.tools;

import com.alibaba.cloud.ai.graph.agent.interceptor.todolist.TodoListInterceptor.Todo;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * <p>
 * Inspired by Claude Code's TodoWrite tool, it helps AI agents track progress,
 * organize complex tasks, and provide visibility into task execution.
 */
public class WriteTodosTool implements BiFunction<WriteTodosTool.Request, ToolContext, WriteTodosTool.Response> {

	private static final Logger logger = LoggerFactory.getLogger(WriteTodosTool.class);

	public static final String SUCCESS_MESSAGE = "Todos have been modified successfully. Ensure that you continue to use the todo list to track your progress. Please proceed with the current tasks if applicable.";

	/**
	 * Handler invoked when todos are updated. Allows external consumers to react to todo
	 * changes (e.g., persist, notify UI, log).
	 */
	@FunctionalInterface
	public interface TodoEventHandler {

		void handle(List<Todo> todos);

	}

	private final TodoEventHandler todoEventHandler;

	// @formatter:off
	public static final String DEFAULT_TOOL_DESCRIPTION = """
		Use this tool to create and manage a structured task list for your current coding session. This helps you track progress, organize complex tasks, and demonstrate thoroughness to the user.
		It also helps the user understand the progress of the task and overall progress of their requests.

		## When to Use This Tool
		Use this tool proactively in these scenarios:

		1. Complex multi-step tasks - When a task requires 3 or more distinct steps or actions
		2. Non-trivial and complex tasks - Tasks that require careful planning or multiple operations
		3. User explicitly requests todo list - When the user directly asks you to use the todo list
		4. User provides multiple tasks - When users provide a list of things to be done (numbered or comma-separated)
		5. After receiving new instructions - Immediately capture user requirements as todos
		6. When you start working on a task - Mark it as in_progress BEFORE beginning work. Ideally you should only have one todo as in_progress at a time
		7. After completing a task - Mark it as completed and add any new follow-up tasks discovered during implementation

		## When NOT to Use This Tool

		Skip using this tool when:
		1. There is only a single, straightforward task
		2. The task is trivial and tracking it provides no organizational benefit
		3. The task can be completed in less than 3 trivial steps
		4. The task is purely conversational or informational

		NOTE that you should not use this tool if there is only one trivial task to do. In this case you are better off just doing the task directly.

		## Validation Rules

		The tool validates the following:
		- Task content must not be empty or blank
		- All tasks must have valid status values (pending, in_progress, completed)

		## Examples of When to Use the Todo List

		<example>
		User: I want to add a dark mode toggle to the application settings. Make sure you run the tests and build when you're done!
		Assistant: I'll help add a dark mode toggle to your application settings. Let me create a todo list to track this implementation.
		*Creates todo list with the following items:*
		1. Creating dark mode toggle component in Settings page
		2. Adding dark mode state management (context/store)
		3. Implementing CSS-in-JS styles for dark theme
		4. Updating existing components to support theme switching
		5. Running tests and build process, addressing any failures or errors that occur
		*Begins working on the first task*

		<reasoning>
		The assistant used the todo list because:
		1. Adding dark mode is a multi-step feature requiring UI, state management, and styling changes
		2. The user explicitly requested tests and build be run afterward
		3. The assistant inferred that tests and build need to pass by adding "Ensure tests and build succeed" as the final task
		</reasoning>
		</example>

		<example>
		User: Help me rename the function getCwd to getCurrentWorkingDirectory across my project
		Assistant: Let me first search through your codebase to find all occurrences of 'getCwd'.
		*Uses grep or search tools to locate all instances of getCwd in the codebase*
		Assistant: I've found 15 instances of 'getCwd' across 8 different files. Let me create a todo list to track these changes.
		*Creates todo list with specific items for each file that needs updating*

		<reasoning>
		The assistant used the todo list because:
		1. First, the assistant searched to understand the scope of the task
		2. Upon finding multiple occurrences across different files, it determined this was a complex task with multiple steps
		3. The todo list helps ensure every instance is tracked and updated systematically
		4. This approach prevents missing any occurrences and maintains code consistency
		</reasoning>
		</example>

		<example>
		User: I need to implement these features for my e-commerce site: user registration, product catalog, shopping cart, and checkout flow.
		Assistant: I'll help implement these features. First, let's add all the features to the todo list.
		*Creates a todo list breaking down each feature into specific tasks based on the project architecture*
		Assistant: Let's start with user registration. This will involve creating database models, API endpoints, and frontend forms that integrate with your existing authentication system.

		<reasoning>
		The assistant used the todo list because:
		1. The user provided multiple complex features to implement in a comma separated list
		2. The todo list helps organize these large features into manageable tasks
		3. This approach allows for tracking progress across the entire implementation
		</reasoning>
		</example>

		## Examples of When NOT to Use the Todo List

		<example>
		User: How do I print 'Hello World' in Python?
		Assistant: In Python, you can print "Hello World" with this simple code:
		print("Hello World")
		This will output the text "Hello World" to the console when executed.

		<reasoning>
		The assistant did not use the todo list because this is a single, trivial task that can be completed in one step. There's no need to track multiple tasks or steps for such a straightforward request.
		</reasoning>
		</example>

		<example>
		User: What does the git status command do?
		Assistant: The git status command shows the current state of your working directory and staging area. It displays which changes have been staged, which haven't, and which files aren't being tracked by Git.

		<reasoning>
		The assistant did not use the todo list because this is an informational request with no actual coding task to complete. The user is simply asking for an explanation, not for the assistant to perform multiple steps or tasks.
		</reasoning>
		</example>

		<example>
		User: Can you add a comment to the calculateTotal function to explain what it does?
		Assistant: Sure, let me add a comment to the calculateTotal function to explain what it does.
		* Uses the Edit tool to add a comment to the calculateTotal function *

		<reasoning>
		The assistant did not use the todo list because this is a single, straightforward task confined to one location in the code. Adding a comment doesn't require tracking multiple steps or systematic organization.
		</reasoning>
		</example>

		## Task States and Management

		1. **Task States**: Use these states to track progress:
		- pending: Task not yet started
		- in_progress: Currently working on (limit to ONE task at a time for sequential work)
		- completed: Task finished successfully

		**IMPORTANT**: Task descriptions must have two forms:
		- content: The imperative form describing what needs to be done (e.g., "Run tests", "Build the project")
		- activeForm: The present continuous form shown during execution (e.g., "Running tests", "Building the project")

		2. **Task Management**:
		- Update task status in real-time as you work
		- Mark tasks complete IMMEDIATELY after finishing (don't batch completions)
		- Exactly ONE task should be in_progress at any time for sequential workflows
		- Complete current tasks before starting new ones
		- Remove tasks that are no longer relevant from the list entirely

		3. **Task Completion Requirements**:
		- ONLY mark a task as completed when you have FULLY accomplished it
		- If you encounter errors, blockers, or cannot finish, keep the task as in_progress
		- When blocked, create a new task describing what needs to be resolved
		- Never mark a task as completed if:
			- Tests are failing
			- Implementation is partial
			- You encountered unresolved errors
			- You couldn't find necessary files or dependencies

		4. **Task Breakdown**:
		- Create specific, actionable items
		- Break complex tasks into smaller, manageable steps
		- Use clear, descriptive task names
		- Always provide both forms:
			- content: "Fix authentication bug"
			- activeForm: "Fixing authentication bug"

		When in doubt, use this tool. Being proactive with task management demonstrates attentiveness and ensures you complete all requirements successfully.
		""";
	// @formatter:on

	public WriteTodosTool() {
		this(todos -> logger.debug("Updated Todos: {}", todos));
	}

	protected WriteTodosTool(TodoEventHandler todoEventHandler) {
		this.todoEventHandler = todoEventHandler;
	}

	@Override
	public Response apply(Request request, ToolContext toolContext) {
		try {
			// Validate todos
			Response validationError = validateTodos(request.todos);
			if (validationError != null) {
				return validationError;
			}

			// Extract state from ToolContext
			Map<String, Object> contextData = toolContext.getContext();
			if (contextData == null) {
				return Response.error("Tool context is not available");
			}

			Object extraStateObj = contextData.get(AGENT_STATE_FOR_UPDATE_CONTEXT_KEY);
			if (extraStateObj == null) {
				return Response.error("Extra state is not initialized");
			}

			if (!(extraStateObj instanceof Map)) {
				return Response.error("Extra state has invalid type");
			}

			@SuppressWarnings("unchecked")
			Map<String, Object> extraState = (Map<String, Object>) extraStateObj;

			// Update the state with todos
			extraState.put("todos", request.todos);

			// Notify external handler
			this.todoEventHandler.handle(request.todos);

			return Response.success(SUCCESS_MESSAGE, request.todos.size());
		}
		catch (ClassCastException e) {
			return Response.error("Invalid state type - " + e.getMessage());
		}
		catch (Exception e) {
			return Response.error("Failed to update todos - " + e.getMessage());
		}
	}

	private Response validateTodos(List<Todo> todos) {
		if (todos == null) {
			return Response.error("Todos cannot be null");
		}

		for (int i = 0; i < todos.size(); i++) {
			Todo item = todos.get(i);

			if (item == null) {
				return Response.error("Task at index " + i + " is null");
			}

			if (item.getContent() == null || item.getContent().isBlank()) {
				return Response.error("Task at index " + i
						+ " has empty or blank content. All tasks must have meaningful content.");
			}

			if (item.getStatus() == null) {
				return Response.error("Task at index " + i
						+ " has null status. Status must be one of: pending, in_progress, completed");
			}
		}

		return null;
	}

	@JsonClassDescription("Request to write or update todos")
	public record Request(
			@JsonProperty(required = true, value = "todos")
			@JsonPropertyDescription("List of todo items. Each item must have: content (imperative form, e.g. 'Run tests'), status (pending|in_progress|completed), and activeForm (present continuous, e.g. 'Running tests')")
			List<Todo> todos) {
	}

	/**
	 * Tool response with success status and message.
	 * Provides clear feedback for both successful updates and error cases.
	 */
	public record Response(
			@JsonProperty("success") boolean success,
			@JsonProperty("message") String message,
			@JsonProperty("todoCount") Integer todoCount) {

		public static Response success(String message, int todoCount) {
			return new Response(true, message, todoCount);
		}

		public static Response error(String message) {
			return new Response(false, "Error: " + message, null);
		}

		/**
		 * Returns the message suitable for display to the agent.
		 * For success: the guidance message; for error: the error details.
		 */
		public String getDisplayMessage() {
			return message;
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String name = "write_todos";

		private String description = DEFAULT_TOOL_DESCRIPTION;

		private TodoEventHandler todoEventHandler = todos -> logger.debug("Updated Todos: {}", todos);

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

		/**
		 * Set handler invoked when todos are updated. Use for persistence, UI
		 * notification, or custom logic.
		 */
		public Builder todoEventHandler(TodoEventHandler todoEventHandler) {
			this.todoEventHandler = todoEventHandler;
			return this;
		}

		public ToolCallback build() {
			return FunctionToolCallback.builder(name, new WriteTodosTool(this.todoEventHandler))
				.description(description)
				.inputType(Request.class)
				.build();
		}

	}

}

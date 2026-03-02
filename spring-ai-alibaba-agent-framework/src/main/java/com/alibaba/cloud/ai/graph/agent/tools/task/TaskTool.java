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
package com.alibaba.cloud.ai.graph.agent.tools.task;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * Tool that enables invoking sub-agents to handle complex, isolated tasks.
 * <p>
 * Inspired by spring-ai-agent-utils TaskTool, this allows the main agent to delegate
 * work to specialized sub-agents, each with their own context and capabilities.
 * Supports both synchronous and background execution.
 */
public class TaskTool implements BiFunction<TaskTool.Request, ToolContext, String> {

	private static final Logger logger = LoggerFactory.getLogger(TaskTool.class);

	// @formatter:off
	public static final String DEFAULT_DESCRIPTION = """
		Launch a specialized sub-agent to handle complex, multi-step tasks autonomously.

		Use this tool when:
		- The task requires exploring codebases, researching, or multi-step execution
		- You need to delegate to an agent with specific expertise (e.g., Explore, general-purpose)
		- The task would consume too much context if done directly

		Available sub-agent types and their capabilities:
		%s

		Parameters:
		- description: Short (3-5 word) summary of what the agent will do
		- prompt: The detailed task for the sub-agent to perform
		- subagent_type: The type of specialized agent to use (required)
		- run_in_background: Set to true to run asynchronously; use TaskOutput tool to retrieve results later

		When run_in_background=true, returns a task_id. Use the TaskOutput tool with that task_id to get results.
		""";
	// @formatter:on

	private final Map<String, ReactAgent> subAgents;

	private final TaskRepository taskRepository;

	public TaskTool(Map<String, ReactAgent> subAgents, TaskRepository taskRepository) {
		Assert.notEmpty(subAgents, "subAgents must not be empty");
		Assert.notNull(taskRepository, "taskRepository must not be null");
		this.subAgents = Map.copyOf(subAgents);
		this.taskRepository = taskRepository;
	}

	@Override
	public String apply(Request request, ToolContext toolContext) {
		String subagentType = request.subagentType();
		if (!StringUtils.hasText(subagentType)) {
			return "Error: subagent_type is required";
		}

		if (!subAgents.containsKey(subagentType)) {
			return "Error: Unknown subagent type: " + subagentType + ". Allowed types: " + subAgents.keySet();
		}

		ReactAgent subAgent = subAgents.get(subagentType);
		String prompt = request.prompt();
		if (!StringUtils.hasText(prompt)) {
			return "Error: prompt is required";
		}

		if (Boolean.TRUE.equals(request.runInBackground())) {
			String taskId = "task_" + UUID.randomUUID();
			this.taskRepository.putTask(taskId, () -> executeSubAgent(subAgent, prompt));
			return String.format(
					"task_id: %s%n%nBackground task started. Use TaskOutput tool with task_id='%s' to retrieve results.",
					taskId, taskId);
		}

		return executeSubAgent(subAgent, prompt);
	}

	private String executeSubAgent(ReactAgent subAgent, String prompt) {
		try {
			AssistantMessage result = subAgent.call(prompt);
			return result != null ? result.getText() : "Sub-agent returned empty response";
		}
		catch (Exception e) {
			logger.warn("Sub-agent execution failed: {}", e.getMessage());
			return "Error executing sub-agent: " + e.getMessage();
		}
	}

	/**
	 * Request structure for the Task tool.
	 */
	@JsonClassDescription("Request to launch a sub-agent task")
	public record Request(
			@JsonProperty(required = true)
			@JsonPropertyDescription("Short (3-5 word) description of what the agent will do")
			String description,

			@JsonProperty(required = true)
			@JsonPropertyDescription("The detailed task for the sub-agent to perform")
			String prompt,

			@JsonProperty(required = true, value = "subagent_type")
			@JsonPropertyDescription("The type of specialized agent to use")
			String subagentType,

			@JsonProperty(value = "run_in_background")
			@JsonPropertyDescription("Set to true to run in background; use TaskOutput to retrieve results")
			Boolean runInBackground) {
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private final Map<String, ReactAgent> subAgents = new HashMap<>();

		private TaskRepository taskRepository = new DefaultTaskRepository();

		private String name = "Task";

		private String description;

		public Builder subAgent(String type, ReactAgent agent) {
			Assert.hasText(type, "type must not be empty");
			Assert.notNull(agent, "agent must not be null");
			this.subAgents.put(type, agent);
			return this;
		}

		public Builder subAgents(Map<String, ReactAgent> agents) {
			if (!CollectionUtils.isEmpty(agents)) {
				this.subAgents.putAll(agents);
			}
			return this;
		}

		public Builder taskRepository(TaskRepository taskRepository) {
			Assert.notNull(taskRepository, "taskRepository must not be null");
			this.taskRepository = taskRepository;
			return this;
		}

		public Builder withName(String name) {
			this.name = name != null ? name : "Task";
			return this;
		}

		public Builder withDescription(String description) {
			this.description = description;
			return this;
		}

		public ToolCallback build() {
			Assert.notEmpty(this.subAgents, "At least one sub-agent must be configured");

			String desc = this.description;
			if (!StringUtils.hasText(desc)) {
				String agentList = this.subAgents.entrySet().stream()
						.map(e -> "- " + e.getKey() + ": " + e.getValue().description())
						.reduce("", (a, b) -> a + b + "\n");
				desc = DEFAULT_DESCRIPTION.formatted(agentList);
			}

			TaskTool taskTool = new TaskTool(this.subAgents, this.taskRepository);
			return FunctionToolCallback.builder(this.name, taskTool)
					.description(desc)
					.inputType(Request.class)
					.build();
		}
	}

}

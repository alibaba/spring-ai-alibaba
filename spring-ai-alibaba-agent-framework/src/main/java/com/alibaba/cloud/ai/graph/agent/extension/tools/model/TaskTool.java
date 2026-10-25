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
package com.alibaba.cloud.ai.graph.agent.extension.tools.model;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Tool that enables invoking subagents to handle complex, isolated tasks.
 *
 * This tool allows the main agent to delegate work to specialized subagents,
 * each with their own context and capabilities.
 */
public class TaskTool implements BiFunction<TaskTool.TaskRequest, ToolContext, String> {

	private final Map<String, ReactAgent> subAgents;

	public TaskTool(Map<String, ReactAgent> subAgents) {
		this.subAgents = subAgents;
	}

	@Override
	public String apply(TaskRequest request, ToolContext toolContext) {
		// Validate subagent type
		if (!subAgents.containsKey(request.subagentType)) {
			return "Error: invoked agent of type " + request.subagentType +
					", the only allowed types are " + subAgents.keySet();
		}

		// Get the subagent
		ReactAgent subAgent = subAgents.get(request.subagentType);

		try {
			// Invoke the subagent with the task description
			Optional<NodeOutput> output = subAgent.invokeAndGetOutput(request.description);

			// A subagent that carries HumanInTheLoopHook stops with InterruptionMetadata when one of
			// its tool calls needs approval. Report that instead of returning the paused model turn
			// as if it were the subagent's answer.
			if (output.isPresent() && output.get() instanceof InterruptionMetadata interruption) {
				return pendingApprovalMessage(request.subagentType, interruption);
			}

			// Return the subagent's response
			return lastAssistantMessage(subAgent, output).getText();
		}
		catch (Exception e) {
			return "Error executing subagent task: " + e.getMessage();
		}
	}

	private static String pendingApprovalMessage(String subagentType, InterruptionMetadata interruption) {
		String calls = interruption.toolFeedbacks()
			.stream()
			.map(tf -> tf.getName() + " " + tf.getArguments())
			.collect(Collectors.joining("; "));
		return "Subagent " + subagentType + " stopped before running tool calls that require human approval. "
				+ "They were NOT executed: " + calls + ". "
				+ "The task is not complete. If the action is still wanted, request it directly so that it goes "
				+ "through human approval.";
	}

	private static AssistantMessage lastAssistantMessage(ReactAgent subAgent, Optional<NodeOutput> output) {
		if (StringUtils.hasLength(subAgent.getOutputKey())) {
			return output.flatMap(o -> o.state().value(subAgent.getOutputKey()))
				.map(AssistantMessage.class::cast)
				.orElseThrow(() -> new IllegalStateException(
						"Output key " + subAgent.getOutputKey() + " not found in agent state"));
		}
		return output.flatMap(o -> o.state().value("messages"))
			.stream()
			.flatMap(messages -> ((List<?>) messages).stream())
			.filter(AssistantMessage.class::isInstance)
			.map(AssistantMessage.class::cast)
			.reduce((first, second) -> second)
			.orElseThrow(() -> new IllegalStateException("No AssistantMessage found in 'messages' state"));
	}

	/**
	 * Create a ToolCallback for the task tool.
	 */
	public static ToolCallback createTaskToolCallback(Map<String, ReactAgent> subAgents, String description) {
		return FunctionToolCallback.builder("task", new TaskTool(subAgents))
				.description(description)
				.inputType(TaskRequest.class)
				.build();
	}

	/**
	 * Request structure for the task tool.
	 */
	public static class TaskRequest {

		@JsonProperty(required = true)
		@JsonPropertyDescription("Detailed description of the task to be performed by the subagent")
		public String description;

		@JsonProperty(required = true, value = "subagent_type")
		@JsonPropertyDescription("The type of subagent to use for this task")
		public String subagentType;

		public TaskRequest() {
		}

		public TaskRequest(String description, String subagentType) {
			this.description = description;
			this.subagentType = subagentType;
		}
	}
}


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
package com.alibaba.cloud.ai.graph.agent.hook.toolselection;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.BeforeModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uses an LLM to select relevant tools before calling the main model.
 *
 * When an agent has many tools available, this hook filters them down
 * to only the most relevant ones for the user's query. This reduces token usage
 * and helps the main model focus on the right tools.
 *
 * Example:
 * <pre>
 * ToolSelectionHook selector = ToolSelectionHook.builder()
 *     .model(selectionModel)
 *     .maxTools(3)
 *     .build();
 * </pre>
 */
public class ToolSelectionHook extends BeforeModelHook {

	private static final Logger log = LoggerFactory.getLogger(ToolSelectionHook.class);

	private static final String DEFAULT_SYSTEM_PROMPT =
			"Your goal is to select the most relevant tools for answering the user's query.";

	private final ChatModel selectionModel;
	private final String systemPrompt;
	private final Integer maxTools;
	private final Set<String> alwaysInclude;

	private ToolSelectionHook(Builder builder) {
		this.selectionModel = builder.selectionModel;
		this.systemPrompt = builder.systemPrompt;
		this.maxTools = builder.maxTools;
		this.alwaysInclude = builder.alwaysInclude != null
				? new HashSet<>(builder.alwaysInclude)
				: new HashSet<>();
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
		// This hook would integrate with the tool selection pipeline
		// The actual implementation depends on how tools are managed in the state
		return CompletableFuture.completedFuture(Map.of());
	}

	/**
	 * Select tools from the available set based on the user's query.
	 *
	 * @param availableTools All available tools
	 * @param userQuery The user's query
	 * @return Selected subset of tools
	 */
	public <T> List<T> selectTools(List<T> availableTools, String userQuery,
			Function<T, String> nameExtractor, Function<T, String> descExtractor) {

		if (availableTools == null || availableTools.size() <= (maxTools != null ? maxTools : availableTools.size())) {
			return availableTools;
		}

		// Build selection prompt
		StringBuilder toolList = new StringBuilder();
		for (T tool : availableTools) {
			String name = nameExtractor.apply(tool);
			String description = descExtractor.apply(tool);
			toolList.append("- ").append(name).append(": ").append(description).append("\n");
		}

		String selectionPrompt = String.format(
				"%s\n\nAvailable tools:\n%s\n\nUser query: %s\n\nSelect the most relevant tools.",
				systemPrompt, toolList.toString(), userQuery
		);

		try {
			// Use the selection model to choose tools
			Prompt prompt = new Prompt(List.of(
					new SystemMessage(systemPrompt),
					new org.springframework.ai.chat.messages.UserMessage(
							"Available tools:\n" + toolList + "\n\nUser query: " + userQuery
					)
			));

			// In a real implementation, this would use structured output
			// to get the selected tool names
			var response = selectionModel.call(prompt);

			// Parse response and filter tools
			// This is simplified - actual implementation would use structured output
			Set<String> selectedNames = parseSelectedTools(response.getResult().getOutput().getText());

			// Always include specified tools
			selectedNames.addAll(alwaysInclude);

			// Filter and limit
			List<T> selected = availableTools.stream()
					.filter(tool -> selectedNames.contains(nameExtractor.apply(tool)))
					.limit(maxTools != null ? maxTools : availableTools.size())
					.collect(Collectors.toList());

			// Add always-include tools
			for (T tool : availableTools) {
				if (alwaysInclude.contains(nameExtractor.apply(tool)) && !selected.contains(tool)) {
					selected.add(tool);
				}
			}

			log.info("Selected {} tools from {} available", selected.size(), availableTools.size());
			return selected;

		}
		catch (Exception e) {
			log.warn("Tool selection failed, using all tools: {}", e.getMessage());
			return availableTools;
		}
	}

	private Set<String> parseSelectedTools(String response) {
		// Simple parsing - in reality would use structured output
		Set<String> tools = new HashSet<>();
		String[] lines = response.split("\n");
		for (String line : lines) {
			line = line.trim();
			if (line.startsWith("-") || line.startsWith("*")) {
				String toolName = line.substring(1).trim().split(":")[0].trim();
				tools.add(toolName);
			}
		}
		return tools;
	}

	@Override
	public String getName() {
		return "ToolSelection";
	}

	@Override
	public List<JumpTo> canJumpTo() {
		return List.of();
	}

	public static class Builder {
		private ChatModel selectionModel;
		private String systemPrompt = DEFAULT_SYSTEM_PROMPT;
		private Integer maxTools;
		private Set<String> alwaysInclude;

		public Builder model(ChatModel model) {
			this.selectionModel = model;
			return this;
		}

		public Builder systemPrompt(String systemPrompt) {
			this.systemPrompt = systemPrompt;
			return this;
		}

		public Builder maxTools(Integer maxTools) {
			this.maxTools = maxTools;
			return this;
		}

		public Builder alwaysInclude(Set<String> toolNames) {
			this.alwaysInclude = toolNames;
			return this;
		}

		public Builder alwaysInclude(String... toolNames) {
			this.alwaysInclude = new HashSet<>(Arrays.asList(toolNames));
			return this;
		}

		public ToolSelectionHook build() {
			return new ToolSelectionHook(this);
		}
	}
}


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
package com.alibaba.cloud.ai.graph.agent.interceptor.toolselection;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uses an LLM to select relevant tools before calling the main model.
 *
 * When an agent has many tools available, this interceptor filters them down
 * to only the most relevant ones for the user's query. This reduces token usage
 * and helps the main model focus on the right tools.
 *
 * Example:
 * ToolSelectionInterceptor interceptor = ToolSelectionInterceptor.builder()
 *     .selectionModel(gpt4oMini)
 *     .maxTools(3)
 *     .build();
 */
public class ToolSelectionInterceptor extends ModelInterceptor {

	private static final Logger log = LoggerFactory.getLogger(ToolSelectionInterceptor.class);

	private static final String DEFAULT_SYSTEM_PROMPT =
			"Your goal is to select the most relevant tools for answering the user's query.";

	private final ChatModel selectionModel;
	private final String systemPrompt;
	private final Integer maxTools;
	private final Set<String> alwaysInclude;
	private final ObjectMapper objectMapper;

	private ToolSelectionInterceptor(Builder builder) {
		this.selectionModel = builder.selectionModel;
		this.systemPrompt = builder.systemPrompt;
		this.maxTools = builder.maxTools;
		this.alwaysInclude = builder.alwaysInclude != null
				? new HashSet<>(builder.alwaysInclude)
				: new HashSet<>();
		this.objectMapper = new ObjectMapper();
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
		List<String> availableTools = request.getTools();

		// If no tools or already within limit, skip selection
		if (availableTools == null || availableTools.isEmpty() ||
				(maxTools != null && availableTools.size() <= maxTools)) {
			return handler.call(request);
		}

		// Find the last user message
		String lastUserQuery = findLastUserMessage(request.getMessages());
		if (lastUserQuery == null) {
			log.debug("No user message found, skipping tool selection");
			return handler.call(request);
		}

		// Perform tool selection
		Set<String> selectedToolNames = selectTools(availableTools, lastUserQuery);

		log.info("Selected {} tools from {} available: {}",
				selectedToolNames.size(), availableTools.size(), selectedToolNames);

		// Filter tools based on selection
		List<String> filteredTools = availableTools.stream()
				.filter(selectedToolNames::contains)
				.collect(Collectors.toList());

		// Create new request with filtered tools
		ModelRequest filteredRequest = ModelRequest.builder(request)
				.tools(filteredTools)
				.build();

		return handler.call(filteredRequest);
	}

	private String findLastUserMessage(List<Message> messages) {
		for (int i = messages.size() - 1; i >= 0; i--) {
			Message msg = messages.get(i);
			if (msg instanceof UserMessage) {
				return msg.getText();
			}
		}
		return null;
	}

	private Set<String> selectTools(List<String> toolNames, String userQuery) {
		try {
			// Build tool list for prompt
			StringBuilder toolList = new StringBuilder();
			for (String toolName : toolNames) {
				toolList.append("- ").append(toolName).append("\n");
			}

			String maxToolsInstruction = maxTools != null
					? "\nIMPORTANT: List the tool names in order of relevance. " +
					"Select at most " + maxTools + " tools."
					: "";

			// Create selection prompt
			List<Message> selectionMessages = List.of(
					new SystemMessage(systemPrompt + maxToolsInstruction),
					new UserMessage("Available tools:\n" + toolList +
							"\nUser query: " + userQuery +
							"\n\nRespond with a JSON object containing a 'tools' array with the selected tool names: {\"tools\": [\"tool1\", \"tool2\"]}")
			);

			Prompt prompt = new Prompt(selectionMessages);
			var response = selectionModel.call(prompt);
			String responseText = response.getResult().getOutput().getText();

			// Parse JSON response
			Set<String> selected = parseToolSelection(responseText);

			// Add always-include tools
			selected.addAll(alwaysInclude);

			// Limit to maxTools if specified
			if (maxTools != null && selected.size() > maxTools) {
				List<String> selectedList = new ArrayList<>(selected);
				selected = new HashSet<>(selectedList.subList(0, maxTools));
			}

			return selected;

		}
		catch (Exception e) {
			log.warn("Tool selection failed, using all tools: {}", e.getMessage());
			return new HashSet<>(toolNames);
		}
	}

	private Set<String> parseToolSelection(String responseText) {
		try {
			// Try to parse as JSON
			ToolSelectionResponse response = objectMapper.readValue(responseText, ToolSelectionResponse.class);
			return new HashSet<>(response.tools);
		}
		catch (Exception e) {
			// Fallback: extract tool names from text
			log.debug("Failed to parse JSON, using fallback extraction");
			return new HashSet<>();
		}
	}

	@Override
	public String getName() {
		return "ToolSelection";
	}

	private static class ToolSelectionResponse {
		@JsonProperty("tools")
		public List<String> tools;
	}

	public static class Builder {
		private ChatModel selectionModel;
		private String systemPrompt = DEFAULT_SYSTEM_PROMPT;
		private Integer maxTools;
		private Set<String> alwaysInclude;

		public Builder selectionModel(ChatModel selectionModel) {
			this.selectionModel = selectionModel;
			return this;
		}

		public Builder systemPrompt(String systemPrompt) {
			this.systemPrompt = systemPrompt;
			return this;
		}

		public Builder maxTools(int maxTools) {
			if (maxTools <= 0) {
				throw new IllegalArgumentException("maxTools must be > 0");
			}
			this.maxTools = maxTools;
			return this;
		}

		public Builder alwaysInclude(Set<String> alwaysInclude) {
			this.alwaysInclude = alwaysInclude;
			return this;
		}

		public Builder alwaysInclude(String... toolNames) {
			this.alwaysInclude = new HashSet<>(Arrays.asList(toolNames));
			return this;
		}

		public ToolSelectionInterceptor build() {
			if (selectionModel == null) {
				throw new IllegalStateException("selectionModel is required");
			}
			return new ToolSelectionInterceptor(this);
		}
	}
}

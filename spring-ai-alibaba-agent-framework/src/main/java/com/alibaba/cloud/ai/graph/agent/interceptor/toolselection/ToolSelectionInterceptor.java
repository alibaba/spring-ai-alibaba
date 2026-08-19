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
package com.alibaba.cloud.ai.graph.agent.interceptor.toolselection;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Selects relevant tools before calling the main model.
 *
 * When an agent has many tools available, this interceptor filters them down
 * to only the most relevant ones for the user's query. This reduces token usage
 * and helps the main model focus on the right tools. Selection can be performed
 * by an LLM or by a custom {@link ToolSelectionStrategy}.
 *
 * Example:
 * ToolSelectionInterceptor interceptor = ToolSelectionInterceptor.builder()
 *     .selectionModel(gpt4oMini)
 *     .maxTools(3)
 *     .build();
 */
public class ToolSelectionInterceptor extends ModelInterceptor {

	private static final Logger log = LoggerFactory.getLogger(ToolSelectionInterceptor.class);

	private final ToolSelectionStrategy selectionStrategy;

	private final Integer maxTools;

	private final Set<String> alwaysInclude;

	private ToolSelectionInterceptor(Builder builder) {
		this.selectionStrategy = builder.selectionStrategy != null
				? builder.selectionStrategy
				: new LlmToolSelectionStrategy(builder.selectionModel, builder.systemPrompt);
		this.maxTools = builder.maxTools;
		this.alwaysInclude = builder.alwaysInclude != null
				? new LinkedHashSet<>(builder.alwaysInclude)
				: new LinkedHashSet<>();
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
		List<String> staticTools = request.getTools() != null ? request.getTools() : List.of();
		List<ToolCallback> dynamicTools = request.getDynamicToolCallbacks() != null
				? request.getDynamicToolCallbacks()
				: List.of();
		List<String> availableTools = buildAvailableToolNames(staticTools, dynamicTools);

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

		Set<String> selectedToolNames;
		try {
			ToolSelectionRequest selectionRequest = new ToolSelectionRequest(lastUserQuery,
					buildToolMetadata(availableTools, request.getToolDescriptions(), dynamicTools),
					maxTools, request.getContext());
			List<String> selected = selectionStrategy.select(selectionRequest);
			selectedToolNames = normalizeSelection(availableTools, selected);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Tool selection interrupted", e);
		}
		catch (Exception e) {
			log.warn("Tool selection failed, using all tools: {}", e.getMessage());
			return handler.call(request);
		}

		log.info("Selected {} tools from {} available: {}",
				selectedToolNames.size(), availableTools.size(), selectedToolNames);

		// Filter tools based on selection
		List<String> filteredTools = staticTools.stream().filter(selectedToolNames::contains).toList();
		List<ToolCallback> filteredDynamicTools = dynamicTools.stream()
				.filter(tool -> selectedToolNames.contains(tool.getToolDefinition().name()))
				.toList();

		// Create new request with filtered tools
		ModelRequest.Builder filteredRequestBuilder = ModelRequest.builder(request)
				.tools(filteredTools)
				.dynamicToolCallbacks(filteredDynamicTools);
		if (filteredTools.isEmpty()) {
			ToolCallingChatOptions options = request.getOptions() != null
					? request.getOptions().copy()
					: ToolCallingChatOptions.builder().build();
			options.setToolCallbacks(List.of());
			filteredRequestBuilder.options(options);
		}
		ModelRequest filteredRequest = filteredRequestBuilder.build();

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

	private List<String> buildAvailableToolNames(List<String> staticTools, List<ToolCallback> dynamicTools) {
		LinkedHashSet<String> toolNames = new LinkedHashSet<>(staticTools);
		for (ToolCallback dynamicTool : dynamicTools) {
			toolNames.add(dynamicTool.getToolDefinition().name());
		}
		return List.copyOf(toolNames);
	}

	private List<ToolMetadata> buildToolMetadata(List<String> toolNames, Map<String, String> toolDescriptions,
			List<ToolCallback> dynamicTools) {
		Map<String, String> descriptions = new HashMap<>();
		if (toolDescriptions != null) {
			descriptions.putAll(toolDescriptions);
		}
		for (ToolCallback dynamicTool : dynamicTools) {
			String name = dynamicTool.getToolDefinition().name();
			descriptions.putIfAbsent(name, dynamicTool.getToolDefinition().description());
		}
		return toolNames.stream()
				.map(toolName -> new ToolMetadata(toolName, descriptions.get(toolName)))
				.toList();
	}

	private Set<String> normalizeSelection(List<String> availableTools, List<String> selectedTools) {
		Set<String> available = new HashSet<>(availableTools);
		LinkedHashSet<String> normalized = new LinkedHashSet<>();

		// Always-included tools take priority when maxTools is configured.
		for (String toolName : alwaysInclude) {
			if (available.contains(toolName)) {
				normalized.add(toolName);
			}
		}

		if (selectedTools != null) {
			for (String toolName : selectedTools) {
				if (available.contains(toolName)) {
					normalized.add(toolName);
				}
				if (maxTools != null && normalized.size() >= maxTools) {
					break;
				}
			}
		}

		if (maxTools != null && normalized.size() > maxTools) {
			return new LinkedHashSet<>(normalized.stream().limit(maxTools).toList());
		}

		return normalized;
	}

	@Override
	public String getName() {
		return "ToolSelection";
	}

	public static class Builder {
		private ChatModel selectionModel;

		private ToolSelectionStrategy selectionStrategy;

		private String systemPrompt = LlmToolSelectionStrategy.DEFAULT_SYSTEM_PROMPT;

		private Integer maxTools;

		private Set<String> alwaysInclude;

		public Builder selectionModel(ChatModel selectionModel) {
			this.selectionModel = selectionModel;
			return this;
		}

		/**
		 * Use a custom strategy instead of an LLM to select tools. A custom strategy
		 * can integrate lexical search, a vector store, or business routing rules.
		 * @param selectionStrategy tool selection strategy
		 * @return this builder
		 */
		public Builder selectionStrategy(ToolSelectionStrategy selectionStrategy) {
			this.selectionStrategy = selectionStrategy;
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
			this.alwaysInclude = new LinkedHashSet<>(Arrays.asList(toolNames));
			return this;
		}

		public ToolSelectionInterceptor build() {
			if (selectionModel != null && selectionStrategy != null) {
				throw new IllegalStateException("selectionModel and selectionStrategy are mutually exclusive");
			}
			if (selectionModel == null && selectionStrategy == null) {
				throw new IllegalStateException("selectionModel or selectionStrategy is required");
			}
			return new ToolSelectionInterceptor(this);
		}
	}
}

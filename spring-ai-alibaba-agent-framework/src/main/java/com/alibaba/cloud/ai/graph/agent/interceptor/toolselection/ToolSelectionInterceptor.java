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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.agent.tool.ContextSourceTool;
import com.alibaba.cloud.ai.graph.agent.tool.DeferredToolCatalog;
import com.alibaba.cloud.ai.graph.agent.tool.ToolCallbackUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.util.StringUtils;

/**
 * Ranks available tools with a separate model and applies optional count and
 * definition budgets before passing them to the main model.
 */
public class ToolSelectionInterceptor extends ModelInterceptor {

	private static final Logger log = LoggerFactory.getLogger(ToolSelectionInterceptor.class);

	private static final String DEFAULT_SYSTEM_PROMPT =
			"Your goal is to select the most relevant tools for answering the user's query.";

	private static final ObjectMapper JSON = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

	private final ChatModel selectionModel;

	private final String systemPrompt;

	private final Integer maxTools;

	private final Integer maxToolSchemaTokens;

	private final Set<String> alwaysInclude;

	private final int maxSelectionRetries;

	private final FailureBehavior failureBehavior;

	private ToolSelectionInterceptor(Builder builder) {
		this.selectionModel = builder.selectionModel;
		this.systemPrompt = builder.systemPrompt;
		this.maxTools = builder.maxTools;
		this.maxToolSchemaTokens = builder.maxToolSchemaTokens;
		this.alwaysInclude = Set.copyOf(builder.alwaysInclude);
		this.maxSelectionRetries = builder.maxSelectionRetries;
		this.failureBehavior = builder.failureBehavior;
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
		var available = new LinkedHashSet<>(request.getTools());
		var dynamic = request.getDynamicToolCallbacks();
		if (!request.hasToolSelection() && dynamic != null) {
			ToolCallbackUtils.deduplicateByName(dynamic).forEach(tool -> available.add(tool.getToolDefinition().name()));
		}
		if (available.isEmpty()) {
			return handler.call(request);
		}

		var options = request.getOptions();
		var callbacks = ToolCallbackUtils.deduplicateByName(options == null ? null : options.getToolCallbacks(), dynamic);
		Map<String, ToolDefinition> definitions = new LinkedHashMap<>();
		callbacks.forEach(tool -> definitions.put(tool.getToolDefinition().name(), tool.getToolDefinition()));
		var context = request.getContext();
		var discovery = new LinkedHashSet<String>();
		if (context != null && Boolean.TRUE.equals(context.get(DeferredToolCatalog.DISCOVERY_CONTEXT_KEY))) {
			discovery.addAll(DeferredToolCatalog.DISCOVERY_TOOLS);
		}
		callbacks.stream().filter(ContextSourceTool.class::isInstance)
			.map(tool -> tool.getToolDefinition().name()).forEach(discovery::add);
		var required = available.stream().filter(name -> this.alwaysInclude.contains(name) || discovery.contains(name))
			.toList();
		var candidates = available.stream().filter(name -> !required.contains(name)).toList();
		var descriptions = request.getToolDescriptions() == null ? Map.<String, String>of() : request.getToolDescriptions();
		Map<String, Long> costs = new LinkedHashMap<>();
		for (String name : available) {
			var definition = definitions.get(name);
			costs.put(name, definition == null
					? ToolCallbackUtils.estimateSchemaTokens(name, descriptions.get(name), null)
					: ToolCallbackUtils.estimateSchemaTokens(definition));
		}
		int optionalLimit = this.maxTools == null ? Integer.MAX_VALUE
				: Math.max(0, this.maxTools - (int) required.stream().filter(name -> !discovery.contains(name)).count());
		long remainingTokens = this.maxToolSchemaTokens == null ? Long.MAX_VALUE
				: this.maxToolSchemaTokens - required.stream().mapToLong(costs::get).sum();
		if (candidates.isEmpty() || ((this.maxTools != null || this.maxToolSchemaTokens != null)
				&& candidates.size() <= optionalLimit && candidates.stream().mapToLong(costs::get).sum() <= remainingTokens)) {
			return handler.call(request);
		}

		List<String> ranked = List.of();
		if (optionalLimit > 0 && remainingTokens > 0) {
			String userQuery = findLastUserMessage(request.getMessages());
			ranked = StringUtils.hasText(userQuery)
					? rankTools(candidates, userQuery, descriptions, definitions, optionalLimit) : candidates;
		}
		var selected = new ArrayList<>(required);
		for (String name : ranked) {
			if (optionalLimit == 0) {
				break;
			}
			long cost = costs.get(name);
			if (cost <= remainingTokens) {
				selected.add(name);
				optionalLimit--;
				remainingTokens -= cost;
			}
		}
		return handler.call(ModelRequest.builder(request).tools(selected).build());
	}

	private static String findLastUserMessage(List<Message> messages) {
		for (int i = messages.size() - 1; i >= 0; i--) {
			if (messages.get(i) instanceof UserMessage message) {
				return message.getText();
			}
		}
		return null;
	}

	private List<String> rankTools(List<String> candidates, String query, Map<String, String> descriptions,
			Map<String, ToolDefinition> definitions, int optionalLimit) {
		var toolList = new StringBuilder();
		for (String name : candidates) {
			toolList.append("- ").append(name);
			String description = descriptions.get(name);
			if (!StringUtils.hasText(description) && definitions.containsKey(name)) {
				description = definitions.get(name).description();
			}
			if (StringUtils.hasText(description)) {
				toolList.append(": ").append(description);
			}
			toolList.append('\n');
		}
		String limit = this.maxToolSchemaTokens == null && this.maxTools != null
				? " Select at most " + optionalLimit + " tools." : "";
		var prompt = new Prompt(List.of(new SystemMessage(this.systemPrompt + "\nRank tools by relevance." + limit),
				new UserMessage("Available tools:\n" + toolList + "\nUser query: " + query
						+ "\n\nRespond with a JSON object containing a 'tools' array of names, such as {\"tools\":[\"tool1\"]}.")));
		var eligible = new LinkedHashSet<>(candidates);
		for (int attempt = 0; ; attempt++) {
			try {
				var response = this.selectionModel.call(prompt);
				return parseRanking(response.getResult().getOutput().getText(), eligible);
			}
			catch (JsonProcessingException | RuntimeException error) {
				if (attempt >= this.maxSelectionRetries) {
					log.debug("Tool selection failed after {} attempts ({})", (long) attempt + 1,
							error.getClass().getSimpleName());
					return switch (this.failureBehavior) {
						case USE_AVAILABLE -> candidates;
						case REQUIRED_ONLY -> List.of();
						case ERROR -> throw new IllegalStateException("Could not select request tools", error);
					};
				}
			}
		}
	}

	private static List<String> parseRanking(String text, Set<String> candidates) throws JsonProcessingException {
		var tools = JSON.readTree(text).path("tools");
		if (!tools.isArray()) {
			throw new IllegalArgumentException("Tool selection must contain a tools array");
		}
		var ranking = new LinkedHashSet<String>();
		for (var tool : tools) {
			if (!tool.isTextual()) {
				throw new IllegalArgumentException("Tool selection names must be strings");
			}
			if (candidates.contains(tool.textValue())) {
				ranking.add(tool.textValue());
			}
		}
		return List.copyOf(ranking);
	}

	@Override
	public String getName() {
		return "ToolSelection";
	}

	public enum FailureBehavior {

		/** Use the available tools in registration order, subject to the configured budgets. */
		USE_AVAILABLE,

		REQUIRED_ONLY,

		ERROR

	}

	public static class Builder {

		private ChatModel selectionModel;

		private String systemPrompt = DEFAULT_SYSTEM_PROMPT;

		private Integer maxTools;

		private Integer maxToolSchemaTokens;

		private Set<String> alwaysInclude = Set.of();

		private int maxSelectionRetries = 1;

		private FailureBehavior failureBehavior = FailureBehavior.USE_AVAILABLE;

		public Builder selectionModel(ChatModel selectionModel) {
			this.selectionModel = selectionModel;
			return this;
		}

		public Builder systemPrompt(String systemPrompt) {
			this.systemPrompt = systemPrompt;
			return this;
		}

		/** Sets the tool limit. Required tools are retained, and discovery tools have separate slots. */
		public Builder maxTools(int maxTools) {
			if (maxTools <= 0) {
				throw new IllegalArgumentException("maxTools must be > 0");
			}
			this.maxTools = maxTools;
			return this;
		}

		/** Limits estimated definition tokens, with required tool definitions retained. */
		public Builder maxToolSchemaTokens(int maxToolSchemaTokens) {
			if (maxToolSchemaTokens <= 0) {
				throw new IllegalArgumentException("maxToolSchemaTokens must be > 0");
			}
			this.maxToolSchemaTokens = maxToolSchemaTokens;
			return this;
		}

		public Builder alwaysInclude(Set<String> alwaysInclude) {
			this.alwaysInclude = alwaysInclude == null ? Set.of() : Set.copyOf(alwaysInclude);
			return this;
		}

		public Builder alwaysInclude(String... toolNames) {
			return alwaysInclude(new LinkedHashSet<>(List.of(toolNames)));
		}

		public Builder maxSelectionRetries(int maxSelectionRetries) {
			if (maxSelectionRetries < 0) {
				throw new IllegalArgumentException("maxSelectionRetries must be non-negative");
			}
			this.maxSelectionRetries = maxSelectionRetries;
			return this;
		}

		public Builder onSelectionFailure(FailureBehavior failureBehavior) {
			this.failureBehavior = Objects.requireNonNull(failureBehavior, "failureBehavior must not be null");
			return this;
		}

		public ToolSelectionInterceptor build() {
			if (this.selectionModel == null) {
				throw new IllegalStateException("selectionModel is required");
			}
			return new ToolSelectionInterceptor(this);
		}

	}

}

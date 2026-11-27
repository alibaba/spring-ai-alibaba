/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent.tool;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import com.alibaba.cloud.ai.graph.GraphLifecycleListener;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.util.StringUtils;

/**
 * Registers searchable descriptors and loads reusable callbacks on demand.
 * Attach a catalog with {@code ReactAgent.builder().deferredTools(catalog)}. A successful
 * load exposes its tool definition to subsequent requests in the same execution.
 * Suppliers create callbacks that can be reused across executions.
 */
public final class DeferredToolCatalog {

	public static final String SEARCH_TOOL_NAME = "search_tools";

	public static final String LOAD_TOOL_NAME = "load_tools";

	public static final List<String> DISCOVERY_TOOLS = List.of(SEARCH_TOOL_NAME, LOAD_TOOL_NAME);

	public static final String DISCOVERY_CONTEXT_KEY = "_DEFERRED_TOOL_DISCOVERY_";

	private static final String STATE_KEY = "_DEFERRED_TOOLS_:";

	private static final String EXECUTION_ID = "executionId";

	private static final String NAMES = "names";

	private static final Logger logger = LoggerFactory.getLogger(DeferredToolCatalog.class);

	private final Map<String, Entry> entries;

	private final Map<String, ToolCallback> callbacks = new ConcurrentHashMap<>();

	private final int maxResults;

	private DeferredToolCatalog(Builder builder) {
		this.entries = Collections.unmodifiableMap(new LinkedHashMap<>(builder.entries));
		this.maxResults = builder.maxResults;
	}

	public static Builder builder() {
		return new Builder();
	}

	public Set<String> names() {
		return this.entries.keySet();
	}

	public static String stateKey(String agentName) {
		return STATE_KEY + agentName;
	}

	public static KeyStrategy activationStrategy() {
		return (previous, update) -> {
			if (previous instanceof Map<?, ?> old && update instanceof Map<?, ?> next
					&& next.get(EXECUTION_ID) instanceof String id && id.equals(old.get(EXECUTION_ID))) {
				var merged = new LinkedHashSet<>(readNames(old.get(NAMES)));
				merged.addAll(readNames(next.get(NAMES)));
				return Map.of(EXECUTION_ID, id, NAMES, List.copyOf(merged));
			}
			return update;
		};
	}

	public List<ToolCallback> activeCallbacks(OverAllState state, String agentName) {
		String executionId = state.value(GraphLifecycleListener.EXECUTION_ID_KEY, String.class).orElse(null);
		if (executionId == null || !(state.value(stateKey(agentName)).orElse(null) instanceof Map<?, ?> activation)
				|| !executionId.equals(activation.get(EXECUTION_ID))) {
			return List.of();
		}
		return readNames(activation.get(NAMES)).stream().map(this::resolve).filter(Objects::nonNull).toList();
	}

	/**
	 * Resolves a tool from this catalog. Graph callers use this only for a tool that
	 * was activated or already bound to the current request.
	 */
	public ToolCallback resolve(String name) {
		var entry = this.entries.get(name);
		if (entry == null) {
			return null;
		}
		try {
			return this.callbacks.computeIfAbsent(name, key -> {
				var callback = Objects.requireNonNull(entry.factory().get(), "Tool factory returned null");
				if (!name.equals(callback.getToolDefinition().name())) {
					throw new IllegalStateException("Tool definition does not match its catalog name");
				}
				return callback;
			});
		}
		catch (RuntimeException error) {
			logger.warn("Could not load deferred tool ({})", error.getClass().getSimpleName());
			return null;
		}
	}

	public List<ToolCallback> discoveryTools(String agentName) {
		Function<SearchRequest, List<Descriptor>> search = request -> search(request.query());
		BiFunction<LoadRequest, ToolContext, Map<String, Object>> load =
				(request, context) -> load(agentName, request, context);
		return List.of(FunctionToolCallback.builder(SEARCH_TOOL_NAME, search)
				.description("Find deferred tools by name or purpose. Call load_tools before using a result.")
				.inputType(SearchRequest.class).build(),
			FunctionToolCallback.builder(LOAD_TOOL_NAME, load)
				.description("Load named deferred tools for the next model request in this task.")
				.inputType(LoadRequest.class).build());
	}

	private List<Descriptor> search(String query) {
		String text = Objects.toString(query, "").strip().toLowerCase(Locale.ROOT);
		List<String> terms = text.isEmpty() ? List.of() : List.of(text.split("\\s+"));
		return this.entries.entrySet().stream()
			.filter(item -> {
				String candidate = (item.getKey() + " " + item.getValue().description()).toLowerCase(Locale.ROOT);
				return terms.stream().allMatch(candidate::contains);
			})
			.limit(this.maxResults)
			.map(item -> new Descriptor(item.getKey(), item.getValue().description()))
			.toList();
	}

	private Map<String, Object> load(String agentName, LoadRequest request, ToolContext context) {
		var execution = ToolContextHelper.getState(context)
			.flatMap(state -> state.value(GraphLifecycleListener.EXECUTION_ID_KEY, String.class));
		var updates = ToolContextHelper.getStateForUpdate(context);
		if (execution.isEmpty() || updates.isEmpty()) {
			return Map.of("error", "Tool loading requires an agent execution");
		}
		if (request.names() == null || request.names().isEmpty() || request.names().size() > this.maxResults
				|| request.names().stream().anyMatch(name -> !StringUtils.hasText(name))) {
			return Map.of("error", "Provide between 1 and " + this.maxResults + " non-blank tool names");
		}
		var requested = new LinkedHashSet<>(request.names());
		var loaded = requested.stream().filter(name -> resolve(name) != null).toList();
		if (!loaded.isEmpty()) {
			updates.get().put(stateKey(agentName), Map.of(EXECUTION_ID, execution.get(), NAMES, loaded));
		}
		requested.removeAll(loaded);
		return Map.of("loaded", loaded, "unavailable", List.copyOf(requested));
	}

	private static List<String> readNames(Object value) {
		return value instanceof List<?> names
				? names.stream().filter(String.class::isInstance).map(String.class::cast).toList() : List.of();
	}

	public record Descriptor(String name, String description) {
	}

	private record Entry(String description, Supplier<? extends ToolCallback> factory) {
	}

	private record SearchRequest(@JsonPropertyDescription("Search words for the tool name or description") String query) {
	}

	private record LoadRequest(
			@JsonProperty(required = true) @JsonPropertyDescription("Tool names from search_tools") List<String> names) {
	}

	public static final class Builder {

		private final Map<String, Entry> entries = new LinkedHashMap<>();

		private int maxResults = 8;

		public Builder tool(String name, String description, Supplier<? extends ToolCallback> factory) {
			if (!StringUtils.hasText(name) || DISCOVERY_TOOLS.contains(name) || this.entries.containsKey(name)) {
				throw new IllegalArgumentException("Deferred tool names must be unique and non-reserved");
			}
			if (!StringUtils.hasText(description)) {
				throw new IllegalArgumentException("Deferred tools require a description");
			}
			this.entries.put(name, new Entry(description, Objects.requireNonNull(factory, "factory must not be null")));
			return this;
		}

		/** Limits results per search and the number of names accepted per load. */
		public Builder maxResults(int maxResults) {
			if (maxResults < 1) {
				throw new IllegalArgumentException("maxResults must be positive");
			}
			this.maxResults = maxResults;
			return this;
		}

		public DeferredToolCatalog build() {
			return new DeferredToolCatalog(this);
		}

	}

}

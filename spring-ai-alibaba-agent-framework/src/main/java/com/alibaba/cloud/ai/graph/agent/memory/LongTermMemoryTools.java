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
package com.alibaba.cloud.ai.graph.agent.memory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.context.ContextPage;
import com.alibaba.cloud.ai.graph.agent.tool.ContextSourceTool;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper;
import com.alibaba.cloud.ai.graph.store.Store;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.alibaba.cloud.ai.graph.store.StoreSearchRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.util.JsonHelper;
import org.springframework.util.StringUtils;

/**
 * Registers explicit search and read tools for cross-session memory. The application
 * resolves a non-empty namespace from trusted runtime configuration on every tool
 * call. The model supplies a query, a key, or a page offset.
 */
public final class LongTermMemoryTools {

	public static final String SEARCH_MEMORY = "search_memory";

	public static final String READ_MEMORY = "read_memory";

	private static final JsonHelper JSON = new JsonHelper();

	private final Store store;

	private final Function<RunnableConfig, List<String>> namespaceResolver;

	private final int maxSearchResults;

	private final int pageCharacters;

	private LongTermMemoryTools(Builder builder) {
		this.store = builder.store;
		this.namespaceResolver = Objects.requireNonNull(builder.namespaceResolver, "A namespace resolver is required");
		this.maxSearchResults = builder.maxSearchResults;
		this.pageCharacters = builder.pageCharacters;
	}

	public static Builder builder() {
		return new Builder();
	}

	/** Registers these callbacks with the agent's tools. */
	public List<ToolCallback> tools() {
		return List.of(new ContextSourceTool(FunctionToolCallback.builder(SEARCH_MEMORY,
				(BiFunction<SearchRequest, ToolContext, Object>) this::search)
				.description("Find relevant long-term memory keys for this task. Use read_memory to retrieve a value.")
				.inputType(SearchRequest.class).build()),
			new ContextSourceTool(FunctionToolCallback.builder(READ_MEMORY,
					(BiFunction<ReadRequest, ToolContext, Object>) this::read)
				.description("Read a long-term memory value by key. Use nextOffset to continue reading the same key.")
				.inputType(ReadRequest.class).build()));
	}

	private Object search(SearchRequest request, ToolContext context) {
		var scoped = scope(context);
		if (scoped.isEmpty()) {
			return Map.of("error", "Long-term memory is unavailable");
		}
		var memory = scoped.get();
		var query = StoreSearchRequest.builder().namespace(memory.namespace()).exactNamespace(true)
			.query(request.query()).sortFields(List.of("key")).limit(this.maxSearchResults).build();
		return memory.store().searchItems(query).getItems().stream()
			.filter(item -> memory.namespace().equals(item.getNamespace()))
			.map(StoreItem::getKey).filter(LongTermMemoryTools::usableKey)
			.distinct().limit(this.maxSearchResults).toList();
	}

	private Object read(ReadRequest request, ToolContext context) {
		var scoped = scope(context);
		if (scoped.isEmpty()) {
			return Map.of("error", "Long-term memory is unavailable");
		}
		if (!usableKey(request.key())) {
			return Map.of("error", "Invalid memory key");
		}
		var memory = scoped.get();
		var item = memory.store().getItem(memory.namespace(), request.key())
			.filter(found -> memory.namespace().equals(found.getNamespace()) && request.key().equals(found.getKey()));
		if (item.isEmpty()) {
			return Map.of("error", "Memory item is unavailable");
		}
		try {
			return ContextPage.read(JSON.toJson(item.get().getValue()), request.offset(), this.pageCharacters);
		}
		catch (IllegalArgumentException error) {
			return Map.of("error", "Invalid memory read request");
		}
	}

	private Optional<Scope> scope(ToolContext context) {
		var config = ToolContextHelper.getConfig(context);
		if (config.isEmpty()) {
			return Optional.empty();
		}
		List<String> namespace = this.namespaceResolver.apply(config.get());
		if (namespace == null || namespace.isEmpty() || namespace.stream().anyMatch(part -> !StringUtils.hasText(part))) {
			return Optional.empty();
		}
		Store source = this.store != null ? this.store : config.get().store();
		if (source == null) {
			source = ToolContextHelper.getState(context).map(state -> state.getStore()).orElse(null);
		}
		return source == null ? Optional.empty() : Optional.of(new Scope(source, List.copyOf(namespace)));
	}

	private static boolean usableKey(String key) {
		return StringUtils.hasText(key) && key.length() <= 256 && !key.equals(".") && !key.equals("..")
				&& key.indexOf('/') == -1 && key.indexOf('\\') == -1
				&& key.chars().noneMatch(Character::isISOControl);
	}

	private record Scope(Store store, List<String> namespace) {
	}

	private record SearchRequest(@JsonPropertyDescription("Words describing the memory you need") String query) {
	}

	private record ReadRequest(@JsonProperty(required = true) String key,
			@JsonPropertyDescription("Use the nextOffset returned by a previous read") Integer offset) {
	}

	public static final class Builder {

		private Store store;

		private Function<RunnableConfig, List<String>> namespaceResolver;

		private int maxSearchResults = 8;

		private int pageCharacters = 4096;

		/** Uses a specific Store. The default is to use the execution's configured Store. */
		public Builder store(Store store) {
			this.store = store;
			return this;
		}

		/** Resolves the exact namespace from application-supplied execution metadata. */
		public Builder namespace(Function<RunnableConfig, List<String>> namespaceResolver) {
			this.namespaceResolver = namespaceResolver;
			return this;
		}

		public Builder maxSearchResults(int maxSearchResults) {
			if (maxSearchResults < 1) {
				throw new IllegalArgumentException("maxSearchResults must be positive");
			}
			this.maxSearchResults = maxSearchResults;
			return this;
		}

		public Builder pageCharacters(int pageCharacters) {
			if (pageCharacters < 2) {
				throw new IllegalArgumentException("pageCharacters must be at least two");
			}
			this.pageCharacters = pageCharacters;
			return this;
		}

		public LongTermMemoryTools build() {
			return new LongTermMemoryTools(this);
		}

	}

}

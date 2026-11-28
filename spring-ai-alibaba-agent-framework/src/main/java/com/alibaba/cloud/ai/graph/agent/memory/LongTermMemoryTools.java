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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.context.ContextPage;
import com.alibaba.cloud.ai.graph.agent.tool.ContextSourceTool;
import com.alibaba.cloud.ai.graph.store.Store;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.util.JsonHelper;

/**
 * Registers explicit search and read tools for cross-session memory. The application
 * resolves a non-empty namespace from trusted runtime configuration on every tool
 * call. The model supplies a query, a key, or a page offset.
 */
public final class LongTermMemoryTools {

	public static final String SEARCH_MEMORY = "search_memory";

	public static final String READ_MEMORY = "read_memory";

	private static final JsonHelper JSON = new JsonHelper();

	private static final Logger logger = LoggerFactory.getLogger(LongTermMemoryTools.class);

	private final Store store;

	private final Function<RunnableConfig, List<String>> namespaceResolver;

	private final int maxSearchResults;

	private final int pageCharacters;

	private final int maxQueryCharacters;

	private final int maxCallsPerToolPerTurn;

	private final ObservationRegistry observationRegistry;

	private LongTermMemoryTools(Builder builder) {
		this.store = builder.store;
		this.namespaceResolver = Objects.requireNonNull(builder.namespaceResolver, "A namespace resolver is required");
		this.maxSearchResults = builder.maxSearchResults;
		this.pageCharacters = builder.pageCharacters;
		this.maxQueryCharacters = builder.maxQueryCharacters;
		this.maxCallsPerToolPerTurn = builder.maxCallsPerToolPerTurn;
		this.observationRegistry = builder.observationRegistry;
	}

	public static Builder builder() {
		return new Builder();
	}

	/** Registers these callbacks with the agent's tools. */
	public List<ToolCallback> tools() {
		return List.of(new ContextSourceTool(FunctionToolCallback.builder(SEARCH_MEMORY,
				(BiFunction<SearchRequest, ToolContext, Object>) this::search)
				.description("Find relevant long-term memory keys for this task. Use read_memory to retrieve a value.")
				.inputType(SearchRequest.class).build(), this.maxCallsPerToolPerTurn),
			new ContextSourceTool(FunctionToolCallback.builder(READ_MEMORY,
					(BiFunction<ReadRequest, ToolContext, Object>) this::read)
				.description("Read a long-term memory value by key. Use nextOffset to continue reading the same key.")
					.inputType(ReadRequest.class).build(), this.maxCallsPerToolPerTurn));
	}

	private Object search(SearchRequest request, ToolContext context) {
		return observe("search", observation -> {
			var scoped = ScopedMemory.resolve(context, this.store, this.namespaceResolver);
			if (scoped.isEmpty()) {
				return error(observation, "scope_unavailable", "Long-term memory is unavailable");
			}
			if (request.query() != null && request.query().length() > this.maxQueryCharacters) {
				return error(observation, "invalid_request", "Memory search query is too long");
			}
			var keys = scoped.get().searchKeys(request.query(), this.maxSearchResults);
			observation.lowCardinalityKeyValue("outcome", keys.isEmpty() ? "miss" : "success");
			if (!observation.isNoop()) {
				observation.getContext().put("result.count", keys.size());
			}
			return keys;
		});
	}

	private Object read(ReadRequest request, ToolContext context) {
		return observe("read", observation -> {
			var scoped = ScopedMemory.resolve(context, this.store, this.namespaceResolver);
			if (scoped.isEmpty()) {
				return error(observation, "scope_unavailable", "Long-term memory is unavailable");
			}
			if (!ScopedMemory.usableKey(request.key())) {
				return error(observation, "invalid_request", "Invalid memory key");
			}
			if (request.offset() != null && request.offset() < 0) {
				return error(observation, "invalid_request", "Invalid memory read request");
			}
			var item = scoped.get().get(request.key());
			if (item.isEmpty()) {
				return error(observation, "miss", "Memory item is unavailable");
			}
			String content = JSON.toJson(item.get().getValue());
			ContextPage page;
			try {
				page = ContextPage.read(content, request.offset(), this.pageCharacters);
			}
			catch (IllegalArgumentException invalidOffset) {
				return error(observation, "invalid_request", "Invalid memory read request");
			}
			observation.lowCardinalityKeyValue("outcome", "success");
			observation.lowCardinalityKeyValue("paginated", Boolean.toString(page.nextOffset() != null));
			if (!observation.isNoop()) {
				observation.getContext().put("result.bytes", page.content().getBytes(StandardCharsets.UTF_8).length);
			}
			return page;
		});
	}

	private Object observe(String operation, Function<Observation, Object> action) {
		var observation = Observation.createNotStarted("saa.agent.memory", this.observationRegistry)
				.lowCardinalityKeyValue("operation", operation).start();
		try {
			return action.apply(observation);
		}
		catch (RuntimeException failure) {
			// Store errors may contain user data or connection details.
			logger.warn("Long-term memory {} failed ({})", operation, failure.getClass().getSimpleName());
			return error(observation, "backend_error", "Long-term memory is temporarily unavailable");
		}
		finally {
			observation.stop();
		}
	}

	private static Map<String, String> error(Observation observation, String outcome, String message) {
		observation.lowCardinalityKeyValue("outcome", outcome);
		return Map.of("error", message);
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

		private int maxQueryCharacters = 1024;

		private int maxCallsPerToolPerTurn = Integer.MAX_VALUE;

		private ObservationRegistry observationRegistry = ObservationRegistry.NOOP;

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

		/** Bounds the search query length in UTF-16 code units. Defaults to 1024. */
		public Builder maxQueryCharacters(int maxQueryCharacters) {
			if (maxQueryCharacters < 1) {
				throw new IllegalArgumentException("maxQueryCharacters must be positive");
			}
			this.maxQueryCharacters = maxQueryCharacters;
			return this;
		}

		/** Sets an additional per-turn limit for each of the two memory tools. */
		public Builder maxCallsPerToolPerTurn(int maxCallsPerToolPerTurn) {
			if (maxCallsPerToolPerTurn < 1) {
				throw new IllegalArgumentException("maxCallsPerToolPerTurn must be positive");
			}
			this.maxCallsPerToolPerTurn = maxCallsPerToolPerTurn;
			return this;
		}

		/** Observes recall without recording identities, queries, or memory content. */
		public Builder observationRegistry(ObservationRegistry observationRegistry) {
			this.observationRegistry = Objects.requireNonNull(observationRegistry, "observationRegistry");
			return this;
		}

		public LongTermMemoryTools build() {
			return new LongTermMemoryTools(this);
		}

	}

}

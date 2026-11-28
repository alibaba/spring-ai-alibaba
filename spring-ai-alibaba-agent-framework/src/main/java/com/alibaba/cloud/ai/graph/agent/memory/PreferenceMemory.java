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
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.sql.DataSource;

import com.alibaba.cloud.ai.graph.GraphLifecycleListener;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback.FeedbackResult;
import com.alibaba.cloud.ai.graph.agent.tool.ContextSourceTool;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.util.JsonHelper;
import org.springframework.util.StringUtils;

import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_TOOL_CALL_ID_CONTEXT_KEY;

/** Scoped, versioned application preferences backed by a transactional JDBC data source. */
public final class PreferenceMemory {

	public static final String READ_PREFERENCE = "read_preference";

	public static final String SAVE_PREFERENCE = "save_preference";

	private static final Logger logger = LoggerFactory.getLogger(PreferenceMemory.class);

	private static final JsonHelper JSON = new JsonHelper();

	private static final Set<String> FORMATS = Set.of("paragraphs", "bullets", "markdown");

	private final JdbcPreferenceRepository repository;

	private final Function<RunnableConfig, List<String>> namespaceResolver;

	private final Predicate<RunnableConfig> writeAuthorization;

	private final Function<RunnableConfig, ToolWritePermit> toolWritePermit;

	private final Set<String> languageTags;

	private PreferenceMemory(Builder builder) {
		this.repository = new JdbcPreferenceRepository(builder.dataSource);
		this.namespaceResolver = Objects.requireNonNull(builder.namespaceResolver, "A namespace resolver is required");
		this.writeAuthorization = builder.writeAuthorization;
		this.toolWritePermit = builder.toolWritePermit;
		this.languageTags = builder.languageTags;
	}

	public static Builder builder(DataSource dataSource) {
		return new Builder(dataSource);
	}

	/** Creates the preference and receipt tables if they do not already exist. */
	public void initializeSchema() {
		this.repository.initializeSchema();
	}

	public Snapshot read(RunnableConfig config, Key key) {
		return this.repository.read(namespace(config), Objects.requireNonNull(key, "key"));
	}

	/**
	 * Saves a validated value when the current version equals the observed version.
	 * Supply the same operation ID and arguments when retrying.
	 */
	public Receipt save(RunnableConfig config, Key key, String value, long expectedVersion, String operationId) {
		var namespace = writableNamespace(config);
		return this.repository.change(namespace, saveChange(key, value, expectedVersion, operationId), "application");
	}

	/** Removes the value while retaining a version that prevents stale writes. */
	public Receipt delete(RunnableConfig config, Key key, long expectedVersion, String operationId) {
		var namespace = writableNamespace(config);
		return this.repository.change(namespace, new Change(key, null, expectedVersion, operationId), "application");
	}

	/**
	 * Binds an application's approval of a save to the persisted request, execution
	 * and scope. Call this after authorizing the reviewer and recording their decision.
	 */
	public ToolWritePermit permitSave(RunnableConfig config, InterruptionMetadata request, ToolFeedback decision,
			String operationId) {
		var namespace = writableNamespace(config);
		String threadId = config.threadId().filter(StringUtils::hasText)
				.orElseThrow(() -> new IllegalArgumentException("An explicit thread ID is required"));
		var pending = request.toolFeedbacks().stream()
				.filter(call -> SAVE_PREFERENCE.equals(call.getName()) && call.getId().equals(decision.getId()))
				.findFirst().orElseThrow(() -> new IllegalArgumentException("No preference save matches this decision"));
		var result = decision.getResult();
		if (request.interruptionId().isEmpty() || !SAVE_PREFERENCE.equals(decision.getName())
				|| (result != FeedbackResult.APPROVED && result != FeedbackResult.EDITED)
				|| !pending.getAllowedDecisions().contains(result)) {
			throw new IllegalArgumentException("An approved preference save decision is required");
		}
		String arguments = result == FeedbackResult.EDITED ? decision.getArguments() : pending.getArguments();
		var input = JSON.fromJson(arguments, SaveRequest.class);
		var change = saveChange(input, operationId);
		String executionId = request.state().value(GraphLifecycleListener.EXECUTION_ID_KEY, String.class)
				.filter(StringUtils::hasText).orElseThrow(() -> new IllegalArgumentException("Missing execution ID"));
		return new ToolWritePermit(threadId, executionId, pending.getId(), namespace, change.key(), change.value(),
				change.expectedVersion(), change.operationId());
	}

	/** Publishes reading by default and adds the approval-gated save tool when configured. */
	public List<ToolCallback> tools() {
		var reader = new ContextSourceTool(FunctionToolCallback.builder(READ_PREFERENCE,
				(BiFunction<ReadRequest, ToolContext, Object>) this::readTool)
				.description("Read the current value and version of reply_language or reply_format. Version 0 means no prior change.")
				.inputType(ReadRequest.class).build());
		if (this.toolWritePermit == null) {
			return List.of(reader);
		}
		var writer = FunctionToolCallback.builder(SAVE_PREFERENCE,
				(BiFunction<SaveRequest, ToolContext, Object>) this::saveTool)
				.description("Propose saving an explicitly requested reply preference. Use read_preference for its version. "
						+ "Only SAVED confirms a committed change. A replayed receipt describes the original operation.")
				.inputType(SaveRequest.class).toolMetadata(ToolMetadata.builder().requiresConfirmation(true).build()).build();
		return List.of(reader, writer);
	}

	private Object readTool(ReadRequest input, ToolContext context) {
		return toolResult(() -> {
			var snapshot = read(toolConfig(context), Key.parse(input.key()));
			return snapshot.value().<Object>map(value -> Map.of("found", true, "version", snapshot.version(), "value", value))
					.orElseGet(() -> Map.of("found", false, "version", snapshot.version()));
		});
	}

	private Object saveTool(SaveRequest input, ToolContext context) {
		return toolResult(() -> {
			var config = toolConfig(context);
			var namespace = writableNamespace(config);
			var permit = this.toolWritePermit.apply(config);
			if (permit == null) {
				throw new SecurityException("A current preference save permit is required");
			}
			var change = saveChange(input, permit.operationId());
			if (!permit.matches(config, context, namespace, change)) {
				throw new SecurityException("The preference save does not match its permit");
			}
			return this.repository.change(namespace, change, "approved_tool");
		});
	}

	private static Object toolResult(Supplier<Object> action) {
		try {
			return action.get();
		}
		catch (SecurityException denied) {
			return Map.of("status", "DENIED");
		}
		catch (IllegalArgumentException invalid) {
			return Map.of("status", "INVALID_REQUEST");
		}
		catch (RuntimeException failure) {
			logger.warn("Preference memory operation failed ({})", failure.getClass().getSimpleName());
			return Map.of("status", "UNAVAILABLE");
		}
	}

	private static RunnableConfig toolConfig(ToolContext context) {
		return ToolContextHelper.getConfig(context)
				.orElseThrow(() -> new SecurityException("Preference memory requires trusted run configuration"));
	}

	private Change saveChange(SaveRequest input, String operationId) {
		if (input == null || input.expectedVersion() == null) {
			throw new IllegalArgumentException("An expected preference version is required");
		}
		return saveChange(Key.parse(input.key()), input.value(), input.expectedVersion(), operationId);
	}

	private Change saveChange(Key key, String value, long expectedVersion, String operationId) {
		Objects.requireNonNull(key, "key");
		var allowed = key == Key.REPLY_LANGUAGE ? this.languageTags : FORMATS;
		if (value == null || !allowed.contains(value)) {
			throw new IllegalArgumentException("Unsupported value for " + key.storeKey());
		}
		return new Change(key, value, expectedVersion, operationId);
	}

	private List<String> namespace(RunnableConfig config) {
		return ScopedMemory.namespace(config, this.namespaceResolver)
				.orElseThrow(() -> new SecurityException("Preference memory scope is unavailable"));
	}

	private List<String> writableNamespace(RunnableConfig config) {
		var namespace = namespace(config);
		if (!this.writeAuthorization.test(config)) {
			throw new SecurityException("Preference writes are not authorized");
		}
		return namespace;
	}

	public enum Key {

		REPLY_LANGUAGE("reply_language"), REPLY_FORMAT("reply_format");

		private final String name;

		Key(String name) {
			this.name = name;
		}

		public String storeKey() {
			return this.name;
		}

		private static Key parse(String key) {
			for (Key candidate : values()) {
				if (candidate.name.equals(key)) {
					return candidate;
				}
			}
			throw new IllegalArgumentException("Unknown preference key");
		}

	}

	public enum Status {

		SAVED, DELETED, VERSION_CONFLICT, OPERATION_CONFLICT

	}

	/** Version 0 represents an untouched preference. A deleted value retains its version. */
	public record Snapshot(long version, Optional<StoreItem> item) {

		public Optional<String> value() {
			return this.item.map(value -> (String) value.getValue().get("value"));
		}

	}

	/** Describes the outcome of an operation, independent of later changes to the preference. */
	public record Receipt(Status status, long version, boolean replayed) {
	}

	/** Trusted runtime authorization for a single, approved tool call. */
	public record ToolWritePermit(String threadId, String executionId, String callId, List<String> namespace,
			Key key, String value, long expectedVersion, String operationId) {

		public ToolWritePermit {
			namespace = List.copyOf(namespace);
		}

		private boolean matches(RunnableConfig config, ToolContext context, List<String> currentNamespace, Change change) {
			return StringUtils.hasText(this.threadId) && config.threadId().filter(this.threadId::equals).isPresent()
					&& StringUtils.hasText(this.executionId) && ToolContextHelper.getState(context)
							.flatMap(state -> state.value(GraphLifecycleListener.EXECUTION_ID_KEY, String.class))
							.filter(this.executionId::equals).isPresent()
					&& StringUtils.hasText(this.callId)
					&& ToolContextHelper.getMetadata(context, AGENT_TOOL_CALL_ID_CONTEXT_KEY, String.class)
							.filter(this.callId::equals).isPresent()
					&& this.namespace.equals(currentNamespace) && this.key == change.key()
					&& Objects.equals(this.value, change.value()) && this.expectedVersion == change.expectedVersion();
		}

	}

	record Change(Key key, String value, long expectedVersion, String operationId) {

		Change {
			Objects.requireNonNull(key, "key");
			if (expectedVersion < 0 || expectedVersion == Long.MAX_VALUE) {
				throw new IllegalArgumentException("Invalid expected preference version");
			}
			if (!StringUtils.hasText(operationId) || operationId.length() > 128
					|| operationId.chars().anyMatch(Character::isISOControl)) {
				throw new IllegalArgumentException("A stable operation ID of at most 128 characters is required");
			}
		}

	}

	private record ReadRequest(@JsonProperty(required = true)
			@JsonPropertyDescription("reply_language or reply_format") String key) {
	}

	private record SaveRequest(@JsonProperty(required = true)
			@JsonPropertyDescription("reply_language or reply_format") String key,
			@JsonProperty(required = true)
			@JsonPropertyDescription("An allowed language tag, or paragraphs, bullets or markdown") String value,
			@JsonProperty(required = true)
			@JsonPropertyDescription("Current version from read_preference") Long expectedVersion) {
	}

	public static final class Builder {

		private final DataSource dataSource;

		private Function<RunnableConfig, List<String>> namespaceResolver;

		private Predicate<RunnableConfig> writeAuthorization = config -> false;

		private Function<RunnableConfig, ToolWritePermit> toolWritePermit;

		private Set<String> languageTags = Set.of("zh-CN", "en-US");

		private Builder(DataSource dataSource) {
			this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
		}

		/** Resolves the exact namespace from application-authenticated runtime data. */
		public Builder namespace(Function<RunnableConfig, List<String>> namespaceResolver) {
			this.namespaceResolver = Objects.requireNonNull(namespaceResolver, "namespaceResolver");
			return this;
		}

		/** Authorizes each application or tool write, including retries. */
		public Builder authorizeWrites(Predicate<RunnableConfig> writeAuthorization) {
			this.writeAuthorization = Objects.requireNonNull(writeAuthorization, "writeAuthorization");
			return this;
		}

		/** Enables the save tool and resolves the application's current approval permit. */
		public Builder toolWritePermit(Function<RunnableConfig, ToolWritePermit> toolWritePermit) {
			this.toolWritePermit = Objects.requireNonNull(toolWritePermit, "toolWritePermit");
			return this;
		}

		/** Sets the application allowlist of accepted language tags. */
		public Builder languageTags(Set<String> languageTags) {
			var tags = Set.copyOf(languageTags);
			if (tags.isEmpty() || tags.stream().anyMatch(tag -> tag.length() > 64
					|| !tag.matches("[A-Za-z]{2,8}(-[A-Za-z0-9]{1,8})*"))) {
				throw new IllegalArgumentException("At least one valid language tag is required");
			}
			this.languageTags = tags;
			return this;
		}

		public PreferenceMemory build() {
			return new PreferenceMemory(this);
		}

	}

}

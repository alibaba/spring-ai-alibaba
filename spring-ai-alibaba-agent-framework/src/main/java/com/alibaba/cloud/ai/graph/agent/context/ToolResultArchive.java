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
package com.alibaba.cloud.ai.graph.agent.context;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.tool.ContextSourceTool;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper;
import com.alibaba.cloud.ai.graph.store.Store;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.part.TextPart;
import org.springframework.ai.chat.messages.part.ToolResultPart;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.util.StringUtils;

/**
 * Stores oversized text results as individually readable pages. A reference is resolved
 * within the owner, thread and agent of the current invocation. The host must authorize
 * the supplied thread ID and configure an owner resolver when sharing a store across
 * owners. Reuse a durable store and the same scope when resuming a checkpoint.
 */
public final class ToolResultArchive {

	public static final String READ_TOOL_NAME = "read_archived_tool_result";

	private static final String REFERENCE_ATTRIBUTE = "spring.ai.alibaba.toolResultArchive";

	private static final String MANIFEST_SUFFIX = "-manifest";

	private static final String LEGACY_MANIFEST_SUFFIX = ":manifest";

	private static final String PAGE_COUNT = "pages";

	private static final String TEXT = "text";

	private static final Pattern REFERENCE_PATTERN = Pattern.compile("[a-f0-9]{64}");

	private static final Logger logger = LoggerFactory.getLogger(ToolResultArchive.class);

	private final Store store;

	private final Function<RunnableConfig, String> ownerScope;

	private final int thresholdChars;

	private final int pageSizeChars;

	private ToolResultArchive(Builder builder) {
		this.store = builder.store;
		this.ownerScope = builder.ownerScope;
		this.thresholdChars = builder.thresholdChars;
		this.pageSizeChars = builder.pageSizeChars;
	}

	public static Builder builder(Store store) {
		return new Builder(store);
	}

	public static boolean isArchived(ToolResultPart result) {
		return result.attributes().containsKey(REFERENCE_ATTRIBUTE);
	}

	/**
	 * Archives a text result if it exceeds the configured threshold. Returns the original
	 * result when the scope is missing, the content is ineligible or storage fails.
	 */
	public ToolResultPart archive(RunnableConfig config, String agentName, String callKey, ToolResultPart result) {
		if (result.isError() || result.payload() != null || isArchived(result) || READ_TOOL_NAME.equals(result.name())
				|| result.content().size() != 1 || !(result.content().get(0) instanceof TextPart text)) {
			return result;
		}
		String content = text.text();
		if (content.length() <= this.thresholdChars) {
			return result;
		}
		int chars = content.codePointCount(0, content.length());
		if (chars <= this.thresholdChars) {
			return result;
		}
		try {
			var namespace = namespace(config, agentName);
			if (namespace.isEmpty()) {
				return result;
			}
			// Include the page size so a configuration change cannot overwrite pages
			// referenced by an older checkpoint.
			String reference = digest(namespace.get().get(1), callKey, Objects.toString(result.name(), ""),
					Integer.toString(this.pageSizeChars), content);
			int pages = (int) ((chars + (long) this.pageSizeChars - 1) / this.pageSizeChars);
			if (this.store.getItem(namespace.get(), reference + MANIFEST_SUFFIX).isEmpty()
					&& getLegacyItem(namespace.get(), reference + LEGACY_MANIFEST_SUFFIX).isEmpty()) {
				writePages(namespace.get(), reference, content, chars, pages);
			}
			var attributes = new HashMap<>(result.attributes());
			attributes.put(REFERENCE_ATTRIBUTE, reference);
			int previewEnd = content.offsetByCodePoints(0, Math.min(256, this.pageSizeChars));
			String preview = """
					Tool result archived. Use %s with reference "%s" and page 1 to read the full result (%d pages).
					Preview:
					%s
					""".formatted(READ_TOOL_NAME, reference, pages, content.substring(0, previewEnd));
			return new ToolResultPart(result.id(), result.name(), List.of(TextPart.of(preview)), false, null, attributes);
		}
		catch (RuntimeException error) {
			logger.warn("Could not archive tool result ({})", error.getClass().getSimpleName());
			return result;
		}
	}

	private void writePages(List<String> namespace, String reference, String content, int chars, int pages) {
		int offset = 0;
		int remaining = chars;
		for (int index = 0; index < pages; index++) {
			int size = Math.min(remaining, this.pageSizeChars);
			int end = content.offsetByCodePoints(offset, size);
			this.store.putItem(StoreItem.of(namespace, pageKey(reference, index + 1),
					Map.of(TEXT, content.substring(offset, end))));
			offset = end;
			remaining -= size;
		}
		// The manifest is written last, so readers only see a completed archive.
		this.store.putItem(StoreItem.of(namespace, reference + MANIFEST_SUFFIX, Map.of(PAGE_COUNT, pages)));
	}

	/**
	 * Reads one page in the current scope. The page number starts at one. An empty
	 * result covers unavailable references and out-of-range pages.
	 */
	public Optional<Page> readPage(RunnableConfig config, String agentName, String reference, int page) {
		if (reference == null || !REFERENCE_PATTERN.matcher(reference).matches() || page < 1) {
			return Optional.empty();
		}
		var namespace = namespace(config, agentName);
		if (namespace.isEmpty()) {
			return Optional.empty();
		}
		var manifest = this.store.getItem(namespace.get(), reference + MANIFEST_SUFFIX);
		boolean legacy = false;
		if (manifest.isEmpty()) {
			manifest = getLegacyItem(namespace.get(), reference + LEGACY_MANIFEST_SUFFIX);
			legacy = manifest.isPresent();
		}
		if (manifest.isEmpty() || !(manifest.get().getValue().get(PAGE_COUNT) instanceof Number count)
				|| page > count.intValue()) {
			return Optional.empty();
		}
		Optional<StoreItem> storedPage = legacy
				? getLegacyItem(namespace.get(), legacyPageKey(reference, page))
				: this.store.getItem(namespace.get(), pageKey(reference, page));
		return storedPage
			.map(item -> item.getValue().get(TEXT))
			.filter(String.class::isInstance)
			.map(String.class::cast)
			.map(text -> new Page(reference, page, text, page < count.intValue() ? page + 1 : null));
	}

	public ToolCallback createReadTool(String agentName) {
		BiFunction<ReadRequest, ToolContext, Object> read = (request, context) -> {
			try {
				int page = request.page() == null ? 1 : request.page();
				var result = ToolContextHelper.getConfig(context)
					.flatMap(config -> readPage(config, agentName, request.reference(), page));
				if (result.isPresent()) {
					return result.get();
				}
			}
			catch (RuntimeException error) {
				logger.warn("Could not read archived tool result ({})", error.getClass().getSimpleName());
			}
			return Map.of("error", "The tool result page is unavailable in this conversation.");
		};
		return new ContextSourceTool(FunctionToolCallback.builder(READ_TOOL_NAME, read)
			.description("Read one page of a tool result archived in this conversation. Follow nextPage until it is null.")
			.inputType(ReadRequest.class)
			.build());
	}

	private Optional<List<String>> namespace(RunnableConfig config, String agentName) {
		return config.threadId().filter(StringUtils::hasText).flatMap(thread -> {
			String owner = this.ownerScope.apply(config);
			return StringUtils.hasText(owner) && StringUtils.hasText(agentName)
					? Optional.of(List.of("tool-result-archive", digest(owner, thread, agentName))) : Optional.empty();
		});
	}

	private Optional<StoreItem> getLegacyItem(List<String> namespace, String key) {
		try {
			return this.store.getItem(namespace, key);
		}
		catch (RuntimeException ex) {
			for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
				if (cause instanceof IllegalArgumentException) {
					return Optional.empty();
				}
			}
			throw ex;
		}
	}

	private static String pageKey(String reference, int page) {
		return reference + "-page-" + page;
	}

	private static String legacyPageKey(String reference, int page) {
		return reference + ":page:" + page;
	}

	private static String digest(String... parts) {
		try {
			var digest = MessageDigest.getInstance("SHA-256");
			for (String part : parts) {
				byte[] bytes = part.getBytes(StandardCharsets.UTF_8);
				digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
				digest.update(bytes);
			}
			return HexFormat.of().formatHex(digest.digest());
		}
		catch (NoSuchAlgorithmException error) {
			throw new IllegalStateException("SHA-256 is unavailable", error);
		}
	}

	public record Page(String reference, int page, String text, Integer nextPage) {
	}

	private record ReadRequest(
			@JsonProperty(required = true) @JsonPropertyDescription("Reference returned with the archived result") String reference,
			@JsonPropertyDescription("One-based page number, defaults to 1") Integer page) {
	}

	public static final class Builder {

		private final Store store;

		private Function<RunnableConfig, String> ownerScope = config -> "default";

		private int thresholdChars = 32_000;

		private int pageSizeChars = 4_000;

		private Builder(Store store) {
			this.store = Objects.requireNonNull(store, "store must not be null");
		}

		/** Resolves the owner from trusted invocation data. A blank owner disables access. */
		public Builder ownerScope(Function<RunnableConfig, String> resolver) {
			this.ownerScope = Objects.requireNonNull(resolver, "owner scope resolver must not be null");
			return this;
		}

		/** Archives results that are longer than this many Unicode code points. */
		public Builder thresholdChars(int threshold) {
			if (threshold < 1) {
				throw new IllegalArgumentException("thresholdChars must be positive");
			}
			this.thresholdChars = threshold;
			return this;
		}

		/** Sets the maximum number of Unicode code points in each stored page. */
		public Builder pageSizeChars(int size) {
			if (size < 1) {
				throw new IllegalArgumentException("pageSizeChars must be positive");
			}
			this.pageSizeChars = size;
			return this;
		}

		public ToolResultArchive build() {
			return new ToolResultArchive(this);
		}

	}

}

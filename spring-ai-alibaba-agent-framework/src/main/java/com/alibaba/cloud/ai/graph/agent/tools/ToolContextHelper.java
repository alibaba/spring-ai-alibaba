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
package com.alibaba.cloud.ai.graph.agent.tools;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.lang.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * Utility class for accessing common metadata from ToolContext.
 *
 * <p>This class provides type-safe, user-friendly methods to access frequently used
 * context data. For custom metadata or advanced use cases, you can still use
 * {@link ToolContextConstants} keys directly with {@link ToolContext#getContext()}.</p>
 *
 * <p><strong>Two ways to access ToolContext data:</strong></p>
 *
 * <p>1. <strong>Helper way</strong> (recommended for standard data):</p>
 * <pre>
 * // Get RunnableConfig - type-safe, no casting needed
 * Optional&lt;RunnableConfig&gt; config = ToolContextHelper.getConfig(toolContext);
 *
 * // Get OverAllState
 * Optional&lt;OverAllState&gt; state = ToolContextHelper.getState(toolContext);
 *
 * // Get custom metadata with type safety
 * Optional&lt;String&gt; userId = ToolContextHelper.getMetadata(toolContext, "user_id", String.class);
 * </pre>
 *
 * <p>2. <strong>Direct key access</strong> (for custom data or batch operations):</p>
 * <pre>
 * // Access using ToolContextConstants
 * Map&lt;String, Object&gt; context = toolContext.getContext();
 * RunnableConfig config = (RunnableConfig) context.get(ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY);
 *
 * // Or access your custom keys
 * Object myData = context.get("my_custom_key");
 * </pre>
 *
 * @see ToolContext
 * @see ToolContextConstants
 */
public final class ToolContextHelper {

	private ToolContextHelper() {
		// Utility class, prevent instantiation
	}

	/**
	 * Get the RunnableConfig from ToolContext.
	 *
	 * <p>This is a convenience method equivalent to:
	 * {@code toolContext.getContext().get(ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY)}</p>
	 *
	 * @param toolContext the tool context
	 * @return Optional containing RunnableConfig if present
	 */
	public static Optional<RunnableConfig> getConfig(@Nullable ToolContext toolContext) {
		if (toolContext == null) {
			return Optional.empty();
		}
		return getMetadata(toolContext, ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY, RunnableConfig.class);
	}

	/**
	 * Get the agent state (OverAllState) from ToolContext.
	 *
	 * <p>This is a convenience method equivalent to:
	 * {@code toolContext.getContext().get(ToolContextConstants.AGENT_STATE_CONTEXT_KEY)}</p>
	 *
	 * @param toolContext the tool context
	 * @return Optional containing OverAllState if present
	 */
	public static Optional<OverAllState> getState(@Nullable ToolContext toolContext) {
		if (toolContext == null) {
			return Optional.empty();
		}
		return getMetadata(toolContext, ToolContextConstants.AGENT_STATE_CONTEXT_KEY, OverAllState.class);
	}

	/**
	 * Get the state map for update from ToolContext.
	 * This is used when the tool needs to modify state.
	 *
	 * <p>This is a convenience method equivalent to:
	 * {@code toolContext.getContext().get(ToolContextConstants.AGENT_STATE_FOR_UPDATE_CONTEXT_KEY)}</p>
	 *
	 * @param toolContext the tool context
	 * @return Optional containing state map if present
	 */
	public static Optional<Map<String, Object>> getStateForUpdate(@Nullable ToolContext toolContext) {
		if (toolContext == null) {
			return Optional.empty();
		}
		return getMetadata(toolContext, ToolContextConstants.AGENT_STATE_FOR_UPDATE_CONTEXT_KEY, Map.class)
				.map(map -> (Map<String, Object>) map);
	}

	/**
	 * Get metadata value by key with type safety.
	 *
	 * <p>Works with both framework keys and your custom keys:</p>
	 * <pre>
	 * // Framework key via constant
	 * Optional&lt;RunnableConfig&gt; config = ToolContextHelper.getMetadata(
	 *     toolContext, ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY, RunnableConfig.class);
	 *
	 * // Your custom key
	 * Optional&lt;String&gt; myData = ToolContextHelper.getMetadata(toolContext, "my_key", String.class);
	 * </pre>
	 *
	 * @param toolContext the tool context
	 * @param key the metadata key (can be from {@link ToolContextConstants} or custom)
	 * @param type the expected type class
	 * @param <T> the type of the metadata value
	 * @return Optional containing typed value if present and type matches
	 */
	public static <T> Optional<T> getMetadata(@Nullable ToolContext toolContext, String key, Class<T> type) {
		if (toolContext == null || key == null) {
			return Optional.empty();
		}

		Map<String, Object> context = toolContext.getContext();
		if (context == null) {
			return Optional.empty();
		}

		Object value = context.get(key);
		if (value == null) {
			return Optional.empty();
		}

		if (type.isInstance(value)) {
			return Optional.of(type.cast(value));
		}

		return Optional.empty();
	}

	/**
	 * Get metadata value with default fallback.
	 *
	 * @param toolContext the tool context
	 * @param key the metadata key
	 * @param type the expected type class
	 * @param defaultValue the default value if key not found or type mismatch
	 * @param <T> the type of the metadata value
	 * @return the value if present and type matches, otherwise defaultValue
	 */
	public static <T> T getMetadataOrDefault(@Nullable ToolContext toolContext, String key, Class<T> type,
			T defaultValue) {
		return getMetadata(toolContext, key, type).orElse(defaultValue);
	}

	/**
	 * Check if ToolContext contains a specific key.
	 *
	 * @param toolContext the tool context
	 * @param key the key to check
	 * @return true if the key exists
	 */
	public static boolean hasKey(@Nullable ToolContext toolContext, String key) {
		if (toolContext == null || key == null) {
			return false;
		}

		Map<String, Object> context = toolContext.getContext();
		if (context == null) {
			return false;
		}

		return context.containsKey(key);
	}

	/**
	 * Get all context data as a Map.
	 * Returns empty map if context is null.
	 *
	 * @param toolContext the tool context
	 * @return unmodifiable view of context data, or empty map
	 */
	public static Map<String, Object> getAllContext(@Nullable ToolContext toolContext) {
		if (toolContext == null) {
			return Map.of();
		}

		Map<String, Object> context = toolContext.getContext();
		if (context == null) {
			return Map.of();
		}

		return Map.copyOf(context);
	}

}

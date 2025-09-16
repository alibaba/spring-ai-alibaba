/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.manus.context;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;

public class JManusExecutionContext {

	private static final Logger logger = LoggerFactory.getLogger(JManusExecutionContext.class);

	/**
	 * Special object to represent null values in ConcurrentHashMap which doesn't allow
	 * null values.
	 */
	private static final Object NULL_VALUE = new Object();

	/**
	 * The unique identifier of the execution plan this context belongs to.
	 */
	private final String planId;

	/**
	 * Timestamp when this context was created.
	 */
	private final LocalDateTime createdAt;

	/**
	 * Thread-safe storage for context data using ContextKey for type safety.
	 */
	private final Map<ContextKey<?>, Object> data;

	/**
	 * Additional metadata for debugging and monitoring.
	 */
	private final Map<String, Object> metadata;

	/**
	 * Reference to the JManus execution context for integration with existing plan
	 * execution system. This allows access to plan execution history and state
	 * information.
	 */
	private ExecutionContext jmanusExecutionContext;

	/**
	 * Creates a new JManusExecutionContext for the specified plan.
	 * @param planId The unique identifier of the execution plan
	 * @throws IllegalArgumentException if planId is null or empty
	 */
	public JManusExecutionContext(String planId) {
		if (planId == null || planId.trim().isEmpty()) {
			throw new IllegalArgumentException("Plan ID cannot be null or empty");
		}

		this.planId = planId;
		this.createdAt = LocalDateTime.now();
		this.data = new ConcurrentHashMap<>();
		this.metadata = new ConcurrentHashMap<>();

		logger.debug("Created new JManusExecutionContext for plan: {}", planId);
	}

	/**
	 * Creates a new JManusExecutionContext with integration to existing JManus execution
	 * context.
	 * @param planId The unique identifier of the execution plan
	 * @param jmanusExecutionContext The existing JManus execution context for integration
	 * @throws IllegalArgumentException if planId is null or empty
	 */
	public JManusExecutionContext(String planId, ExecutionContext jmanusExecutionContext) {
		this(planId);
		this.jmanusExecutionContext = jmanusExecutionContext;
		logger.debug("Created new JManusExecutionContext for plan: {} with JManus integration", planId);
	}

	/**
	 * Stores a value in the context using a type-safe key.
	 * @param <T> The type of the value
	 * @param key The context key
	 * @param value The value to store (can be null)
	 * @return The previous value associated with the key, or null if none
	 * @throws IllegalArgumentException if key is null or value type is incompatible
	 */
	public <T> T put(ContextKey<T> key, T value) {
		Objects.requireNonNull(key, "Context key cannot be null");

		if (value != null && !key.isCompatibleType(value)) {
			throw new IllegalArgumentException(
					String.format("Value type %s is not compatible with key type %s for key '%s'",
							value.getClass().getSimpleName(), key.getType().getSimpleName(), key.getName()));
		}

		// Use NULL_VALUE to represent null in ConcurrentHashMap
		Object storeValue = (value == null) ? NULL_VALUE : value;
		Object previous = data.put(key, storeValue);

		logger.trace("Stored value for key '{}' in context for plan: {}", key.getName(), planId);

		// Convert back from NULL_VALUE to null for return value
		return (previous == NULL_VALUE) ? null : key.cast(previous);
	}

	/**
	 * Retrieves a value from the context using a type-safe key.
	 * @param <T> The type of the value
	 * @param key The context key
	 * @return An Optional containing the value if present, or empty if not found
	 * @throws IllegalArgumentException if key is null
	 */
	public <T> Optional<T> get(ContextKey<T> key) {
		Objects.requireNonNull(key, "Context key cannot be null");

		Object value = data.get(key);
		if (value == null) {
			return Optional.empty();
		}

		// Convert NULL_VALUE back to null
		if (value == NULL_VALUE) {
			// We need to return Optional containing null, but Optional.of(null) throws
			// NPE
			// So we use Optional.ofNullable which handles null correctly
			return Optional.ofNullable(null);
		}

		return Optional.of(key.cast(value));
	}

	/**
	 * Retrieves a value from the context, returning a default value if not found.
	 * @param <T> The type of the value
	 * @param key The context key
	 * @param defaultValue The default value to return if key is not found
	 * @return The value associated with the key, or the default value if not found
	 * @throws IllegalArgumentException if key is null
	 */
	public <T> T getOrDefault(ContextKey<T> key, T defaultValue) {
		return get(key).orElse(defaultValue);
	}

	/**
	 * Removes a value from the context.
	 * @param <T> The type of the value
	 * @param key The context key
	 * @return The previous value associated with the key, or null if none
	 * @throws IllegalArgumentException if key is null
	 */
	public <T> T remove(ContextKey<T> key) {
		Objects.requireNonNull(key, "Context key cannot be null");

		Object previous = data.remove(key);

		if (previous != null) {
			logger.trace("Removed value for key '{}' from context for plan: {}", key.getName(), planId);
		}

		return key.cast(previous);
	}

	/**
	 * Checks if the context contains a value for the specified key.
	 * @param key The context key
	 * @return true if the context contains a value for the key, false otherwise
	 * @throws IllegalArgumentException if key is null
	 */
	public boolean containsKey(ContextKey<?> key) {
		Objects.requireNonNull(key, "Context key cannot be null");
		return data.containsKey(key);
	}

	/**
	 * Returns the number of key-value pairs in the context.
	 * @return The size of the context
	 */
	public int size() {
		return data.size();
	}

	/**
	 * Checks if the context is empty.
	 * @return true if the context contains no key-value pairs, false otherwise
	 */
	public boolean isEmpty() {
		return data.isEmpty();
	}

	/**
	 * Clears all data from the context.
	 */
	public void clear() {
		int previousSize = data.size();
		data.clear();

		if (previousSize > 0) {
			logger.debug("Cleared all data from context for plan: {} ({} entries removed)", planId, previousSize);
		}
	}

	/**
	 * Returns an immutable set of all keys in the context.
	 * @return An immutable set of context keys
	 */
	public Set<ContextKey<?>> keySet() {
		return Collections.unmodifiableSet(data.keySet());
	}

	/**
	 * Stores metadata in the context. Metadata is used for debugging and monitoring
	 * purposes and does not provide type safety guarantees.
	 * @param key The metadata key
	 * @param value The metadata value
	 * @return The previous value associated with the key, or null if none
	 * @throws IllegalArgumentException if key is null or empty
	 */
	public Object putMetadata(String key, Object value) {
		if (key == null || key.trim().isEmpty()) {
			throw new IllegalArgumentException("Metadata key cannot be null or empty");
		}

		// Use NULL_VALUE to represent null in ConcurrentHashMap
		Object storeValue = (value == null) ? NULL_VALUE : value;
		Object previous = metadata.put(key, storeValue);

		// Convert back from NULL_VALUE to null for return value
		return (previous == NULL_VALUE) ? null : previous;
	}

	/**
	 * Retrieves metadata from the context.
	 * @param key The metadata key
	 * @return The metadata value, or null if not found
	 * @throws IllegalArgumentException if key is null or empty
	 */
	public Object getMetadata(String key) {
		if (key == null || key.trim().isEmpty()) {
			throw new IllegalArgumentException("Metadata key cannot be null or empty");
		}

		Object value = metadata.get(key);
		// Convert NULL_VALUE back to null
		return (value == NULL_VALUE) ? null : value;
	}

	/**
	 * Returns an immutable copy of all metadata.
	 * @return An immutable map of all metadata
	 */
	public Map<String, Object> getAllMetadata() {
		Map<String, Object> result = new HashMap<>();
		for (Map.Entry<String, Object> entry : metadata.entrySet()) {
			Object value = entry.getValue();
			// Convert NULL_VALUE back to null
			result.put(entry.getKey(), (value == NULL_VALUE) ? null : value);
		}
		return Collections.unmodifiableMap(result);
	}

	/**
	 * Creates an immutable snapshot of the current context state. This is useful for
	 * debugging, rollback operations, or creating checkpoints.
	 * @return An immutable snapshot of the context
	 */
	public ContextSnapshot createSnapshot() {
		return new ContextSnapshot(planId, createdAt, new HashMap<>(data), new HashMap<>(metadata));
	}

	/**
	 * Generates a formatted string representation of the context state. This method
	 * integrates with the existing plan execution state visualization.
	 * @param includeMetadata Whether to include metadata in the output
	 * @return A formatted string representation of the context state
	 */
	public String getStateString(boolean includeMetadata) {
		StringBuilder sb = new StringBuilder();

		sb.append("=== JManus Execution Context State ===\n");
		sb.append("Plan ID: ").append(planId).append("\n");
		sb.append("Created At: ").append(createdAt).append("\n");
		sb.append("Data Entries: ").append(data.size()).append("\n");

		if (!data.isEmpty()) {
			sb.append("\n--- Context Data ---\n");
			data.forEach((key, value) -> {
				sb.append("  ").append(key.getName()).append(" (").append(key.getType().getSimpleName()).append("): ");

				if (value == null) {
					sb.append("null");
				}
				else {
					sb.append(value.toString());
				}
				sb.append("\n");
			});
		}

		if (includeMetadata && !metadata.isEmpty()) {
			sb.append("\n--- Metadata ---\n");
			metadata.forEach((key, value) -> sb.append("  ")
				.append(key)
				.append(": ")
				.append(value != null ? value.toString() : "null")
				.append("\n"));
		}

		return sb.toString();
	}

	/**
	 * Gets the plan ID this context belongs to.
	 * @return The plan ID
	 */
	public String getPlanId() {
		return planId;
	}

	/**
	 * Sets the JManus execution context for integration with existing plan execution
	 * system.
	 * @param jmanusExecutionContext The JManus execution context
	 */
	public void setJmanusExecutionContext(ExecutionContext jmanusExecutionContext) {
		this.jmanusExecutionContext = jmanusExecutionContext;
	}

	/**
	 * Gets the JManus execution context for accessing plan execution information.
	 * @return The JManus execution context, or null if not set
	 */
	public ExecutionContext getJmanusExecutionContext() {
		return jmanusExecutionContext;
	}

	/**
	 * Gets the execution history snapshot from the integrated JManus plan system. This
	 * provides a formatted string representation of all completed and current steps,
	 * which is useful for LLM context, debugging, and execution summaries.
	 * @param onlyCompletedAndFirstInProgress If true, only shows completed steps and
	 * first in-progress step
	 * @return Formatted execution history string, or empty string if no JManus context is
	 * available
	 */
	public String getExecutionHistorySnapshot(boolean onlyCompletedAndFirstInProgress) {
		if (jmanusExecutionContext != null && jmanusExecutionContext.getPlan() != null) {
			try {
				return jmanusExecutionContext.getPlan()
					.getPlanExecutionStateStringFormat(onlyCompletedAndFirstInProgress);
			}
			catch (Exception e) {
				logger.warn("Failed to get execution history snapshot for plan: {} - {}", planId, e.getMessage());
				return "";
			}
		}
		return "";
	}

	/**
	 * Gets a combined view of both the structured context data and the execution history.
	 * This method provides the best of both worlds: structured data access for
	 * step-to-step communication, and formatted execution history for comprehensive
	 * context understanding.
	 * @return A formatted string containing both context data summary and execution
	 * history
	 */
	public String getCombinedContextView() {
		StringBuilder combined = new StringBuilder();

		// Add structured data summary
		combined.append("=== Structured Context Data ===\n");
		combined.append(getStateString(false));

		// Add execution history if available
		String history = getExecutionHistorySnapshot(false);
		if (!history.isEmpty()) {
			combined.append("\n\n=== Execution History ===\n");
			combined.append(history);
		}

		return combined.toString();
	}

	/**
	 * Gets the timestamp when this context was created.
	 * @return The creation timestamp
	 */
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	@Override
	public String toString() {
		return String.format("JManusExecutionContext{planId='%s', size=%d, createdAt=%s}", planId, data.size(),
				createdAt);
	}

	/**
	 * Immutable snapshot of a context state at a specific point in time. Useful for
	 * debugging, auditing, and rollback operations.
	 */
	public static class ContextSnapshot {

		private final String planId;

		private final LocalDateTime createdAt;

		private final LocalDateTime snapshotTime;

		private final Map<ContextKey<?>, Object> data;

		private final Map<String, Object> metadata;

		private ContextSnapshot(String planId, LocalDateTime createdAt, Map<ContextKey<?>, Object> data,
				Map<String, Object> metadata) {
			this.planId = planId;
			this.createdAt = createdAt;
			this.snapshotTime = LocalDateTime.now();
			this.data = Collections.unmodifiableMap(data);
			this.metadata = Collections.unmodifiableMap(metadata);
		}

		public String getPlanId() {
			return planId;
		}

		public LocalDateTime getCreatedAt() {
			return createdAt;
		}

		public LocalDateTime getSnapshotTime() {
			return snapshotTime;
		}

		public Map<ContextKey<?>, Object> getData() {
			return data;
		}

		public Map<String, Object> getMetadata() {
			return metadata;
		}

		@Override
		public String toString() {
			return String.format("ContextSnapshot{planId='%s', dataSize=%d, snapshotTime=%s}", planId, data.size(),
					snapshotTime);
		}

	}

}

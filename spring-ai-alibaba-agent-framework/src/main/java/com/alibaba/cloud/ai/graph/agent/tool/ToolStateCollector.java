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
package com.alibaba.cloud.ai.graph.agent.tool;

import com.alibaba.cloud.ai.graph.KeyStrategy;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Collects state updates from parallel tool executions with isolation. Each tool gets its
 * own update map to avoid concurrent write conflicts. Merges are performed in original
 * tool order for deterministic results.
 *
 * <p>
 * <b>Thread Safety:</b> This class is thread-safe. Each tool receives an isolated
 * ConcurrentHashMap for its updates, allowing safe concurrent writes even if the tool has
 * internal async operations.
 * </p>
 *
 * <p>
 * <b>Usage Contract:</b> {@link #mergeAll()} must only be called once, after all tools
 * have completed. Calling it multiple times will throw an exception.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>{@code
 * ToolStateCollector collector = new ToolStateCollector(3, state.keyStrategies());
 *
 * // Each tool gets its own update map
 * Map<String, Object> tool0Updates = collector.createToolUpdateMap(0);
 * Map<String, Object> tool1Updates = collector.createToolUpdateMap(1);
 * Map<String, Object> tool2Updates = collector.createToolUpdateMap(2);
 *
 * // Tools can safely write to their maps concurrently
 * tool0Updates.put("result", "value0");
 * tool1Updates.put("result", "value1");
 *
 * // After all tools complete, merge in index order
 * Map<String, Object> merged = collector.mergeAll();
 * }</pre>
 *
 * @author disaster
 * @since 1.0.0
 * @see KeyStrategy
 */
public class ToolStateCollector {

	private final ConcurrentMap<Integer, Map<String, Object>> toolUpdatesByIndex = new ConcurrentHashMap<>();

	private final int totalTools;

	private final Map<String, KeyStrategy> keyStrategies;

	private final AtomicBoolean merged = new AtomicBoolean(false);

	/**
	 * Creates a new state collector.
	 * @param totalTools the total number of tools to collect state from
	 * @param keyStrategies the key strategies for merging (nullable)
	 */
	public ToolStateCollector(int totalTools, Map<String, KeyStrategy> keyStrategies) {
		this.totalTools = totalTools;
		this.keyStrategies = keyStrategies != null ? keyStrategies : Collections.emptyMap();
	}

	/**
	 * Creates an isolated update map for a tool at the given index. The returned map is a
	 * ConcurrentHashMap, allowing safe concurrent writes even if the tool implementation
	 * has internal async operations.
	 * @param index the tool index (0-based, in original toolCalls order)
	 * @return a new ConcurrentHashMap for the tool to write updates to
	 * @throws IllegalStateException if mergeAll() has already been called
	 */
	public Map<String, Object> createToolUpdateMap(int index) {
		if (merged.get()) {
			throw new IllegalStateException("Cannot create update map after mergeAll() has been called");
		}
		// Using ConcurrentHashMap to support tools with internal async operations
		Map<String, Object> updateMap = new ConcurrentHashMap<>();
		toolUpdatesByIndex.put(index, updateMap);
		return updateMap;
	}

	/**
	 * Discards updates for a tool at the given index.
	 *
	 * <p>
	 * This is useful when a tool execution times out and we want to avoid merging partial
	 * updates that may still be written after the timeout.
	 * </p>
	 * @param index the tool index (0-based, in original toolCalls order)
	 */
	public void discardToolUpdateMap(int index) {
		toolUpdatesByIndex.remove(index);
	}

	/**
	 * Merges all tool updates in original index order (0, 1, 2, ...). This ensures
	 * deterministic results regardless of completion order.
	 *
	 * <p>
	 * <b>Note:</b> This method can only be called once. Subsequent calls will throw an
	 * {@link IllegalStateException}. This ensures that merging happens only after all
	 * tools have completed their execution.
	 * </p>
	 * @return the merged state updates
	 * @throws IllegalStateException if called more than once
	 */
	public Map<String, Object> mergeAll() {
		if (!merged.compareAndSet(false, true)) {
			throw new IllegalStateException("mergeAll() can only be called once");
		}

		Map<String, Object> result = new ConcurrentHashMap<>();

		for (int i = 0; i < totalTools; i++) {
			Map<String, Object> toolUpdate = toolUpdatesByIndex.get(i);
			if (toolUpdate == null || toolUpdate.isEmpty()) {
				continue;
			}

			for (Map.Entry<String, Object> entry : toolUpdate.entrySet()) {
				String key = entry.getKey();
				Object newValue = entry.getValue();
				Object existingValue = result.get(key);

				if (existingValue == null) {
					result.put(key, newValue);
				}
				else {
					KeyStrategy strategy = keyStrategies.getOrDefault(key, KeyStrategy.REPLACE);
					result.put(key, strategy.apply(existingValue, newValue));
				}
			}
		}

		return result;
	}

	/**
	 * Gets the number of tools that have submitted updates.
	 * @return the count of tools with updates
	 */
	public int getCompletedCount() {
		return toolUpdatesByIndex.size();
	}

	/**
	 * Checks if mergeAll() has been called.
	 * @return true if merged
	 */
	public boolean isMerged() {
		return merged.get();
	}

}

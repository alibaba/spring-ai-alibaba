/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.graph.store;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an item stored in the Store with hierarchical namespace support.
 * <p>
 * Each StoreItem contains a namespace path, key, value, and timestamps for creation and
 * last update. The namespace provides hierarchical organization of data, while the key
 * serves as the unique identifier within that namespace.
 * </p>
 *
 * <h2>Usage Examples</h2> <pre>{@code
 * // Create a simple item
 * StoreItem item = StoreItem.of(
 *     List.of("users", "user123"),
 *     "profile",
 *     Map.of("name", "John Doe", "email", "john@example.com")
 * );
 *
 * // Create item with nested namespace
 * StoreItem preferences = StoreItem.of(
 *     List.of("users", "user123", "settings", "ui"),
 *     "theme",
 *     Map.of("mode", "dark", "fontSize", 14)
 * );
 * }</pre>
 *
 * @author Spring AI Alibaba
 * @since 1.0.0.3
 */
public class StoreItem implements Serializable {

	/**
	 * The hierarchical namespace path for organizing items. For example: ["users",
	 * "user123", "preferences"]
	 */
	private List<String> namespace;

	/**
	 * The unique key within the namespace.
	 */
	private String key;

	/**
	 * The item value as a structured Map.
	 */
	private Map<String, Object> value;

	/**
	 * Timestamp when the item was created (milliseconds since epoch).
	 */
	private long createdAt;

	/**
	 * Timestamp when the item was last updated (milliseconds since epoch).
	 */
	private long updatedAt;

	/**
	 * Default constructor for serialization frameworks.
	 */
	public StoreItem() {
	}

	/**
	 * Constructs a StoreItem with the specified namespace, key, and value. Timestamps are
	 * set to the current time.
	 * @param namespace the hierarchical namespace path
	 * @param key the item key
	 * @param value the item value
	 */
	public StoreItem(List<String> namespace, String key, Map<String, Object> value) {
		this.namespace = namespace;
		this.key = key;
		this.value = value;
		long now = System.currentTimeMillis();
		this.createdAt = now;
		this.updatedAt = now;
	}

	/**
	 * Constructs a StoreItem with full parameters including timestamps.
	 * @param namespace the hierarchical namespace path
	 * @param key the item key
	 * @param value the item value
	 * @param createdAt creation timestamp (milliseconds since epoch)
	 * @param updatedAt last update timestamp (milliseconds since epoch)
	 */
	public StoreItem(List<String> namespace, String key, Map<String, Object> value, long createdAt, long updatedAt) {
		this.namespace = namespace;
		this.key = key;
		this.value = value;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	/**
	 * Static factory method to create a StoreItem.
	 * @param namespace the hierarchical namespace path
	 * @param key the item key
	 * @param value the item value
	 * @return a new StoreItem instance
	 */
	public static StoreItem of(List<String> namespace, String key, Map<String, Object> value) {
		return new StoreItem(namespace, key, value);
	}

	/**
	 * Updates the item's value and sets updatedAt to current time.
	 * @param newValue the new value
	 */
	public void updateValue(Map<String, Object> newValue) {
		this.value = newValue;
		this.updatedAt = System.currentTimeMillis();
	}

	// Getters and Setters

	public List<String> getNamespace() {
		return namespace;
	}

	public void setNamespace(List<String> namespace) {
		this.namespace = namespace;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Map<String, Object> getValue() {
		return value;
	}

	public void setValue(Map<String, Object> value) {
		this.value = value;
		this.updatedAt = System.currentTimeMillis();
	}

	public long getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(long createdAt) {
		this.createdAt = createdAt;
	}

	public long getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(long updatedAt) {
		this.updatedAt = updatedAt;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		StoreItem storeItem = (StoreItem) o;
		return Objects.equals(namespace, storeItem.namespace) && Objects.equals(key, storeItem.key);
	}

	@Override
	public int hashCode() {
		return Objects.hash(namespace, key);
	}

	@Override
	public String toString() {
		return "StoreItem{" + "namespace=" + namespace + ", key='" + key + '\'' + ", value=" + value + ", createdAt="
				+ createdAt + ", updatedAt=" + updatedAt + '}';
	}

}

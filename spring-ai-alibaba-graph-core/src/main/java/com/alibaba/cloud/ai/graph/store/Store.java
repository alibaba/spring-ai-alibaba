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

import java.util.List;
import java.util.Optional;

/**
 * Interface for long-term memory storage in multi-agent systems.
 * <p>
 * Store provides persistent, cross-session memory management capabilities, supporting
 * hierarchical namespaces and structured data storage. This is different from
 * CheckpointSaver which focuses on short-term graph state persistence.
 * </p>
 *
 * <h2>Key Features</h2>
 * <ul>
 * <li><strong>Hierarchical Namespaces:</strong> Organize data using nested
 * namespaces</li>
 * <li><strong>Structured Data:</strong> Store complex Map-based data structures</li>
 * <li><strong>Search and Filter:</strong> Query data by namespace, key patterns, and
 * content</li>
 * <li><strong>Pagination:</strong> Support for large result sets with offset/limit</li>
 * <li><strong>Cross-Session:</strong> Data persists across different execution
 * sessions</li>
 * </ul>
 *
 * <h2>Usage Example</h2> <pre>{@code
 * // Store user preferences
 * StoreItem preferences = StoreItem.of(
 *     List.of("users", "user123", "preferences"),
 *     "ui_settings",
 *     Map.of("theme", "dark", "language", "en-US")
 * );
 * store.putItem(preferences);
 *
 * // Retrieve data
 * Optional<StoreItem> item = store.getItem(
 *     List.of("users", "user123", "preferences"),
 *     "ui_settings"
 * );
 *
 * // Search for items
 * StoreSearchRequest searchRequest = StoreSearchRequest.builder()
 *     .namespace("users")
 *     .query("preferences")
 *     .limit(10)
 *     .build();
 * StoreSearchResult result = store.searchItems(searchRequest);
 * }</pre>
 *
 * @author Spring AI Alibaba
 * @since 1.0.0.3
 */
public interface Store {

	/**
	 * Store an item in the specified namespace with the given key. If an item with the
	 * same namespace and key already exists, it will be updated.
	 * @param item the item to store
	 * @throws IllegalArgumentException if item is null or invalid
	 */
	void putItem(StoreItem item);

	/**
	 * Retrieve an item from the specified namespace with the given key.
	 * @param namespace the hierarchical namespace path
	 * @param key the item key
	 * @return Optional containing the item if found, empty otherwise
	 * @throws IllegalArgumentException if namespace or key is null/invalid
	 */
	Optional<StoreItem> getItem(List<String> namespace, String key);

	/**
	 * Delete an item from the specified namespace with the given key.
	 * @param namespace the hierarchical namespace path
	 * @param key the item key
	 * @return true if the item was deleted, false if it didn't exist
	 * @throws IllegalArgumentException if namespace or key is null/invalid
	 */
	boolean deleteItem(List<String> namespace, String key);

	/**
	 * Search for items based on the provided search criteria.
	 * @param searchRequest the search parameters
	 * @return search results with matching items
	 * @throws IllegalArgumentException if searchRequest is null
	 */
	StoreSearchResult searchItems(StoreSearchRequest searchRequest);

	/**
	 * List available namespaces based on the provided criteria.
	 * @param namespaceRequest the namespace listing parameters
	 * @return list of namespace paths
	 * @throws IllegalArgumentException if namespaceRequest is null
	 */
	List<String> listNamespaces(NamespaceListRequest namespaceRequest);

	/**
	 * Clear all items from the store.
	 * <p>
	 * <strong>WARNING:</strong> This operation is irreversible and will remove all stored
	 * data.
	 * </p>
	 */
	void clear();

	/**
	 * Get the total number of items in the store.
	 * @return the number of items stored
	 */
	long size();

	/**
	 * Check if the store is empty.
	 * @return true if the store contains no items, false otherwise
	 */
	boolean isEmpty();

}

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
package com.alibaba.cloud.ai.graph.store.stores;

import com.alibaba.cloud.ai.graph.store.NamespaceListRequest;
import com.alibaba.cloud.ai.graph.store.Store;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.alibaba.cloud.ai.graph.store.StoreSearchRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Abstract base class for Store implementations providing common validation and utility
 * methods.
 * <p>
 * This class offers a foundation for implementing the Store interface with consistent
 * validation behavior and common helper methods.
 * </p>
 *
 * @author Spring AI Alibaba
 * @since 1.0.0.3
 */
public abstract class BaseStore implements Store {

	/**
	 * Validates the putItem parameters.
	 * @param item the item to validate
	 */
	protected void validatePutItem(StoreItem item) {
		if (item == null) {
			throw new IllegalArgumentException("item cannot be null");
		}
		if (item.getNamespace() == null) {
			throw new IllegalArgumentException("namespace cannot be null");
		}
		if (item.getKey() == null || item.getKey().trim().isEmpty()) {
			throw new IllegalArgumentException("key cannot be null or empty");
		}
	}

	/**
	 * Validates the getItem parameters.
	 * @param namespace namespace
	 * @param key key
	 */
	protected void validateGetItem(List<String> namespace, String key) {
		if (namespace == null) {
			throw new IllegalArgumentException("namespace cannot be null");
		}
		if (key == null) {
			throw new IllegalArgumentException("key cannot be null");
		}
		if (key.trim().isEmpty()) {
			throw new IllegalArgumentException("key cannot be empty");
		}
	}

	/**
	 * Validates the deleteItem parameters.
	 * @param namespace namespace
	 * @param key key
	 */
	protected void validateDeleteItem(List<String> namespace, String key) {
		if (namespace == null) {
			throw new IllegalArgumentException("namespace cannot be null");
		}
		if (key == null) {
			throw new IllegalArgumentException("key cannot be null");
		}
		if (key.trim().isEmpty()) {
			throw new IllegalArgumentException("key cannot be empty");
		}
	}

	/**
	 * Validates the searchItems parameters.
	 * @param searchRequest search request
	 */
	protected void validateSearchItems(StoreSearchRequest searchRequest) {
		if (searchRequest == null) {
			throw new IllegalArgumentException("searchRequest cannot be null");
		}
	}

	/**
	 * Validates the listNamespaces parameters.
	 * @param namespaceRequest namespace request
	 */
	protected void validateListNamespaces(NamespaceListRequest namespaceRequest) {
		if (namespaceRequest == null) {
			throw new IllegalArgumentException("namespaceRequest cannot be null");
		}
	}

	/**
	 * Create store key from namespace and key Uses safe encoding to avoid conflicts from
	 * special characters.
	 * @param namespace namespace
	 * @param key key
	 * @return store key
	 */
	protected String createStoreKey(List<String> namespace, String key) {
		try {
			Map<String, Object> keyData = new HashMap<>();
			keyData.put("namespace", namespace);
			keyData.put("key", key);
			return Base64.getEncoder().encodeToString(new ObjectMapper().writeValueAsBytes(keyData));
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to create store key", e);
		}
	}

	/**
	 * Parse store key to namespace and key.
	 * @param storeKey store key
	 * @return array containing [namespace, key]
	 */
	@SuppressWarnings("unchecked")
	protected Object[] parseStoreKey(String storeKey) {
		try {
			byte[] decoded = Base64.getDecoder().decode(storeKey);
			Map<String, Object> keyData = new ObjectMapper().readValue(decoded, Map.class);
			List<String> namespace = (List<String>) keyData.get("namespace");
			String key = (String) keyData.get("key");
			return new Object[] { namespace, key };
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to parse store key: " + storeKey, e);
		}
	}

	/**
	 * Check if namespace starts with given prefix.
	 * @param namespace namespace to check
	 * @param prefix prefix to match
	 * @return true if starts with prefix
	 */
	protected boolean startsWithPrefix(List<String> namespace, List<String> prefix) {
		if (prefix.isEmpty()) {
			return true;
		}
		if (prefix.size() > namespace.size()) {
			return false;
		}
		for (int i = 0; i < prefix.size(); i++) {
			if (!Objects.equals(namespace.get(i), prefix.get(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Preserve the original {@code createdAt} when a {@code putItem} overwrites an
	 * existing item with the same namespace and key.
	 * <p>
	 * {@link Store#putItem(StoreItem)} defines put-on-existing-key as an update, and the
	 * creation time should stay stable across updates (only {@code updatedAt} changes),
	 * consistent with {@code StoreItem#updateValue}. Overwrite-based implementations
	 * (memory/file system/etc.) should call this to carry the existing item's
	 * {@code createdAt} onto the incoming item before writing.
	 * <p>
	 * This performs a read-modify-write and must be invoked <strong>inside</strong> the
	 * same write-locked critical section that performs the actual write, so the lookup
	 * and the subsequent store are atomic. It calls {@link #getItem(List, String)}, which
	 * acquires the read lock; a {@link java.util.concurrent.locks.ReentrantReadWriteLock}
	 * allows a thread holding the write lock to also acquire the read lock (lock
	 * downgrade), so this is safe to call while the write lock is held.
	 * <p>
	 * Preservation is best-effort: if the existing record cannot be read (e.g. a prior
	 * write left a truncated/corrupt payload), the lookup failure is ignored and the
	 * incoming {@code createdAt} is kept as-is, so a retried {@code putItem} can still
	 * overwrite and repair the damaged record rather than being blocked by it.
	 * @param item the incoming item about to be stored (its {@code createdAt} may be
	 * overwritten with the existing item's value)
	 */
	protected void preserveCreatedAt(StoreItem item) {
		try {
			getItem(item.getNamespace(), item.getKey())
				.ifPresent(existing -> item.setCreatedAt(existing.getCreatedAt()));
		}
		catch (RuntimeException e) {
			// Best-effort: an unreadable existing record has no preservable timestamp.
			// Keep the incoming createdAt so the write below can replace the record.
		}
	}

	/**
	 * Check if item matches the search filter.
	 * @param item item to check
	 * @param searchRequest search parameters
	 * @return true if matches
	 */
	protected boolean matchesSearchCriteria(StoreItem item, StoreSearchRequest searchRequest) {
		// Namespace filter
		List<String> namespaceFilter = searchRequest.getNamespace();
		if (!namespaceFilter.isEmpty() && !startsWithPrefix(item.getNamespace(), namespaceFilter)) {
			return false;
		}

		// Text query filter
		String query = searchRequest.getQuery();
		if (query != null && !query.trim().isEmpty()) {
			String lowerQuery = query.toLowerCase();
			// Search in key
			if (!item.getKey().toLowerCase().contains(lowerQuery)) {
				// Search in value
				String valueStr = item.getValue().toString().toLowerCase();
				if (!valueStr.contains(lowerQuery)) {
					return false;
				}
			}
		}

		// Custom filters
		Map<String, Object> filters = searchRequest.getFilter();
		if (!filters.isEmpty()) {
			Map<String, Object> itemValue = item.getValue();
			for (Map.Entry<String, Object> filter : filters.entrySet()) {
				Object itemVal = itemValue.get(filter.getKey());
				if (!Objects.equals(itemVal, filter.getValue())) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Create comparator for sorting items.
	 * @param searchRequest search parameters
	 * @return comparator
	 */
	protected Comparator<StoreItem> createComparator(StoreSearchRequest searchRequest) {
		List<String> sortFields = searchRequest.getSortFields();
		boolean ascending = searchRequest.isAscending();

		return (item1, item2) -> {
			for (String field : sortFields) {
				int comparison = compareByField(item1, item2, field);
				if (comparison != 0) {
					return ascending ? comparison : -comparison;
				}
			}
			return 0;
		};
	}

	/**
	 * Compare two items by field.
	 * @param item1 first item
	 * @param item2 second item
	 * @param field field name
	 * @return comparison result
	 */
	@SuppressWarnings("unchecked")
	private int compareByField(StoreItem item1, StoreItem item2, String field) {
		switch (field) {
			case "createdAt":
				return Long.compare(item1.getCreatedAt(), item2.getCreatedAt());
			case "updatedAt":
				return Long.compare(item1.getUpdatedAt(), item2.getUpdatedAt());
			case "key":
				return item1.getKey().compareTo(item2.getKey());
			case "namespace":
				return String.join("/", item1.getNamespace()).compareTo(String.join("/", item2.getNamespace()));
			default:
				// Try to compare by value field
				Object val1 = item1.getValue().get(field);
				Object val2 = item2.getValue().get(field);
				if (val1 == null && val2 == null) {
					return 0;
				}
				if (val1 == null) {
					return -1;
				}
				if (val2 == null) {
					return 1;
				}
				if (val1 instanceof Comparable && val2 instanceof Comparable) {
					return ((Comparable<Object>) val1).compareTo(val2);
				}
				return val1.toString().compareTo(val2.toString());
		}
	}

}

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
package com.alibaba.cloud.ai.graph.store.stores;

import com.alibaba.cloud.ai.graph.store.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * In-memory implementation of the Store interface.
 * <p>
 * This implementation stores all data in memory using a ConcurrentHashMap. It's suitable
 * for testing, development, and lightweight applications where persistence is not
 * required.
 * </p>
 * <p>
 * <strong>Note:</strong> All data is lost when the application restarts.
 * </p>
 *
 * @author Spring AI Alibaba
 * @since 1.0.0.3
 */
public class MemoryStore extends BaseStore {

	/**
	 * Thread-safe storage for store items. Key format: "namespace1/namespace2/key"
	 */
	private final Map<String, StoreItem> storage = new ConcurrentHashMap<>();

	/**
	 * Read-write lock for thread safety during search operations.
	 */
	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	@Override
	public void putItem(StoreItem item) {
		validatePutItem(item);

		lock.writeLock().lock();
		try {
			String storeKey = createStoreKey(item.getNamespace(), item.getKey());
			storage.put(storeKey, item);
		}
		finally {
			lock.writeLock().unlock();
		}

	}

	@Override
	public Optional<StoreItem> getItem(List<String> namespace, String key) {
		validateGetItem(namespace, key);

		lock.readLock().lock();
		try {
			String storeKey = createStoreKey(namespace, key);
			return Optional.ofNullable(storage.get(storeKey));
		}
		finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public boolean deleteItem(List<String> namespace, String key) {
		validateDeleteItem(namespace, key);

		lock.writeLock().lock();
		try {
			String storeKey = createStoreKey(namespace, key);
			return storage.remove(storeKey) != null;
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public StoreSearchResult searchItems(StoreSearchRequest searchRequest) {
		validateSearchItems(searchRequest);

		lock.readLock().lock();
		try {
			List<StoreItem> allItems = new ArrayList<>(storage.values());

			// Apply filters
			List<StoreItem> filteredItems = allItems.stream()
				.filter(item -> matchesSearchCriteria(item, searchRequest))
				.collect(Collectors.toList());

			// Sort items
			if (!searchRequest.getSortFields().isEmpty()) {
				filteredItems.sort(createComparator(searchRequest));
			}

			long totalCount = filteredItems.size();

			// Apply pagination
			int offset = searchRequest.getOffset();
			int limit = searchRequest.getLimit();

			if (offset >= filteredItems.size()) {
				return StoreSearchResult.of(Collections.emptyList(), totalCount, offset, limit);
			}

			int endIndex = Math.min(offset + limit, filteredItems.size());
			List<StoreItem> resultItems = filteredItems.subList(offset, endIndex);

			return StoreSearchResult.of(resultItems, totalCount, offset, limit);
		}
		finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public List<String> listNamespaces(NamespaceListRequest namespaceRequest) {
		validateListNamespaces(namespaceRequest);

		lock.readLock().lock();
		try {
			Set<String> namespaceSet = new HashSet<>();
			List<String> prefixFilter = namespaceRequest.getNamespace();

			for (StoreItem item : storage.values()) {
				List<String> itemNamespace = item.getNamespace();

				// Check if namespace starts with prefix filter
				if (!prefixFilter.isEmpty() && !startsWithPrefix(itemNamespace, prefixFilter)) {
					continue;
				}

				// Generate all possible namespace paths up to maxDepth
				int maxDepth = namespaceRequest.getMaxDepth();
				int depth = (maxDepth == -1) ? itemNamespace.size() : Math.min(maxDepth, itemNamespace.size());

				for (int i = 1; i <= depth; i++) {
					String namespacePath = String.join("/", itemNamespace.subList(0, i));
					namespaceSet.add(namespacePath);
				}
			}

			List<String> namespaces = new ArrayList<>(namespaceSet);
			Collections.sort(namespaces);

			// Apply pagination
			int offset = namespaceRequest.getOffset();
			int limit = namespaceRequest.getLimit();

			if (offset >= namespaces.size()) {
				return Collections.emptyList();
			}

			int endIndex = Math.min(offset + limit, namespaces.size());
			return namespaces.subList(offset, endIndex);
		}
		finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public void clear() {
		storage.clear();
	}

	@Override
	public long size() {
		return storage.size();
	}

	@Override
	public boolean isEmpty() {
		return storage.isEmpty();
	}

}

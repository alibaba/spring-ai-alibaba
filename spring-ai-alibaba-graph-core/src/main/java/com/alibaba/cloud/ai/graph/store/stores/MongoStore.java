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
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * MongoDB-like implementation of the Store interface using in-memory storage.
 * <p>
 * This implementation simulates MongoDB behavior using a ConcurrentHashMap for
 * environments where actual MongoDB dependencies are not available. For production use
 * with actual MongoDB, replace this with a proper MongoDB client implementation.
 * </p>
 *
 * @author Spring AI Alibaba
 * @since 1.0.0.3
 */
public class MongoStore extends BaseStore {

	private final Map<String, Map<String, Object>> mongoLikeCollection;

	private final ObjectMapper objectMapper;

	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	private final String collectionName;

	/**
	 * Constructor with default collection name.
	 */
	public MongoStore() {
		this("store");
	}

	/**
	 * Constructor with custom collection name.
	 * @param collectionName collection name
	 */
	public MongoStore(String collectionName) {
		this.mongoLikeCollection = new HashMap<>();
		this.collectionName = collectionName;
		this.objectMapper = new ObjectMapper();
		this.objectMapper.findAndRegisterModules();
	}

	@Override
	public void putItem(StoreItem item) {
		validatePutItem(item);

		lock.writeLock().lock();
		try {
			String documentId = createDocumentId(item.getNamespace(), item.getKey());

			Map<String, Object> doc = new HashMap<>();
			doc.put("_id", documentId);
			doc.put("namespace", item.getNamespace());
			doc.put("key", item.getKey());
			doc.put("value", item.getValue());
			doc.put("createdAt", item.getCreatedAt());
			doc.put("updatedAt", item.getUpdatedAt());

			mongoLikeCollection.put(documentId, doc);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to store item in MongoDB-like storage", e);
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
			String documentId = createDocumentId(namespace, key);
			Map<String, Object> doc = mongoLikeCollection.get(documentId);

			if (doc == null) {
				return Optional.empty();
			}

			return Optional.of(documentToStoreItem(doc));
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to retrieve item from MongoDB-like storage", e);
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
			String documentId = createDocumentId(namespace, key);
			return mongoLikeCollection.remove(documentId) != null;
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to delete item from MongoDB-like storage", e);
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
			List<StoreItem> allItems = getAllItems();

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

			List<StoreItem> allItems = getAllItems();

			for (StoreItem item : allItems) {
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
		lock.writeLock().lock();
		try {
			mongoLikeCollection.clear();
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public long size() {
		lock.readLock().lock();
		try {
			return mongoLikeCollection.size();
		}
		finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	/**
	 * Create document ID from namespace and key.
	 * @param namespace namespace
	 * @param key key
	 * @return document ID
	 */
	private String createDocumentId(List<String> namespace, String key) {
		return createStoreKey(namespace, key);
	}

	/**
	 * Get all items from MongoDB-like collection.
	 * @return list of all items
	 */
	private List<StoreItem> getAllItems() {
		List<StoreItem> items = new ArrayList<>();

		for (Map<String, Object> doc : mongoLikeCollection.values()) {
			try {
				items.add(documentToStoreItem(doc));
			}
			catch (Exception e) {
				// Skip invalid documents
			}
		}

		return items;
	}

	/**
	 * Convert document to StoreItem.
	 * @param doc MongoDB-like document
	 * @return StoreItem
	 */
	@SuppressWarnings("unchecked")
	private StoreItem documentToStoreItem(Map<String, Object> doc) {
		List<String> namespace = (List<String>) doc.get("namespace");
		String key = (String) doc.get("key");
		Map<String, Object> value = (Map<String, Object>) doc.get("value");
		long createdAt = ((Number) doc.get("createdAt")).longValue();
		long updatedAt = ((Number) doc.get("updatedAt")).longValue();

		return new StoreItem(namespace, key, value, createdAt, updatedAt);
	}

}

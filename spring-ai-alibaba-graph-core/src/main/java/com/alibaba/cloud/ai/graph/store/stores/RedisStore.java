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
import com.alibaba.cloud.ai.graph.store.constant.StoreConstant;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Redis-like implementation of the Store interface using in-memory storage.
 * <p>
 * This implementation simulates Redis behavior using a ConcurrentHashMap for environments
 * where actual Redis dependencies are not available. For production use with actual
 * Redis, replace this with a proper Redis client implementation.
 * </p>
 *
 * @author Spring AI Alibaba
 * @since 1.0.0.3
 */
public class RedisStore extends BaseStore {

	private final Map<String, String> redisLikeStorage;

	private final ObjectMapper objectMapper;

	private final String keyPrefix;

	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	/**
	 * Constructor with default key prefix.
	 */
	public RedisStore() {
		this(StoreConstant.REDIS_KEY_PREFIX);
	}

	/**
	 * Constructor with custom key prefix.
	 * @param keyPrefix Redis key prefix
	 */
	public RedisStore(String keyPrefix) {
		this.redisLikeStorage = new HashMap<>();
		this.keyPrefix = keyPrefix;
		this.objectMapper = new ObjectMapper();
		this.objectMapper.findAndRegisterModules();
	}

	@Override
	public void putItem(StoreItem item) {
		validatePutItem(item);

		lock.writeLock().lock();
		try {
			String redisKey = createRedisKey(item.getNamespace(), item.getKey());
			String itemJson = objectMapper.writeValueAsString(item);
			redisLikeStorage.put(redisKey, itemJson);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to store item in Redis-like storage", e);
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
			String redisKey = createRedisKey(namespace, key);
			String value = redisLikeStorage.get(redisKey);

			if (value == null) {
				return Optional.empty();
			}

			StoreItem item = objectMapper.readValue(value, StoreItem.class);
			return Optional.of(item);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to retrieve item from Redis-like storage", e);
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
			String redisKey = createRedisKey(namespace, key);
			return redisLikeStorage.remove(redisKey) != null;
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
			Set<String> keysToRemove = redisLikeStorage.keySet()
				.stream()
				.filter(key -> key.startsWith(keyPrefix))
				.collect(Collectors.toSet());
			keysToRemove.forEach(redisLikeStorage::remove);
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public long size() {
		lock.readLock().lock();
		try {
			return redisLikeStorage.keySet().stream().filter(key -> key.startsWith(keyPrefix)).count();
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
	 * Create Redis key from namespace and key.
	 * @param namespace namespace
	 * @param key key
	 * @return Redis key
	 */
	private String createRedisKey(List<String> namespace, String key) {
		String storeKey = createStoreKey(namespace, key);
		return keyPrefix + storeKey;
	}

	/**
	 * Get all items from Redis-like storage.
	 * @return list of all items
	 */
	private List<StoreItem> getAllItems() {
		List<StoreItem> items = new ArrayList<>();

		for (Map.Entry<String, String> entry : redisLikeStorage.entrySet()) {
			if (entry.getKey().startsWith(keyPrefix)) {
				try {
					StoreItem item = objectMapper.readValue(entry.getValue(), StoreItem.class);
					items.add(item);
				}
				catch (Exception e) {
					// Skip invalid items
				}
			}
		}

		return items;
	}

}

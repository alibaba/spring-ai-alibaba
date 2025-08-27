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

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * File system-based implementation of the Store interface.
 * <p>
 * This implementation stores items as JSON files in a directory structure that mirrors
 * the hierarchical namespace organization. It's suitable for single-node deployments
 * where local file system persistence is sufficient.
 * </p>
 *
 * @author Spring AI Alibaba
 * @since 1.0.0.3
 */
public class FileSystemStore extends BaseStore {

	private final Path rootPath;

	private final ObjectMapper objectMapper;

	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	/**
	 * Constructor with root directory path.
	 * @param rootDirectory the root directory for storage
	 */
	public FileSystemStore(String rootDirectory) {
		this(Paths.get(rootDirectory));
	}

	/**
	 * Constructor with root path.
	 * @param rootPath the root path for storage
	 */
	public FileSystemStore(Path rootPath) {
		this.rootPath = rootPath;
		this.objectMapper = new ObjectMapper();
		this.objectMapper.findAndRegisterModules();
		initializeRootDirectory();
	}

	@Override
	public void putItem(StoreItem item) {
		validatePutItem(item);

		lock.writeLock().lock();
		try {
			Path itemPath = createItemPath(item.getNamespace(), item.getKey());
			ensureDirectoryExists(itemPath.getParent());

			String itemJson = objectMapper.writeValueAsString(item);
			Files.write(itemPath, itemJson.getBytes());
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to store item to file system", e);
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
			Path itemPath = createItemPath(namespace, key);
			if (!Files.exists(itemPath)) {
				return Optional.empty();
			}

			String itemJson = Files.readString(itemPath);
			StoreItem item = objectMapper.readValue(itemJson, StoreItem.class);
			return Optional.of(item);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to retrieve item from file system", e);
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
			Path itemPath = createItemPath(namespace, key);
			if (Files.exists(itemPath)) {
				Files.delete(itemPath);
				// Clean up empty directories
				cleanupEmptyDirectories(itemPath.getParent());
				return true;
			}
			return false;
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to delete item from file system", e);
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
			scanDirectoriesForNamespaces(rootPath, List.of(), namespaceSet, namespaceRequest);

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
			if (Files.exists(rootPath)) {
				deleteDirectoryRecursively(rootPath);
			}
			initializeRootDirectory();
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to clear file system store", e);
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public long size() {
		return getAllItems().size();
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	/**
	 * Initialize root directory.
	 */
	private void initializeRootDirectory() {
		try {
			Files.createDirectories(rootPath);
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to create root directory: " + rootPath, e);
		}
	}

	/**
	 * Create item path from namespace and key.
	 * @param namespace namespace
	 * @param key key
	 * @return item path
	 */
	private Path createItemPath(List<String> namespace, String key) {
		Path path = rootPath;
		for (String ns : namespace) {
			path = path.resolve(ns);
		}
		return path.resolve(key + ".json");
	}

	/**
	 * Ensure directory exists.
	 * @param directory directory to create
	 */
	private void ensureDirectoryExists(Path directory) throws IOException {
		if (!Files.exists(directory)) {
			Files.createDirectories(directory);
		}
	}

	/**
	 * Get all items from file system.
	 * @return list of all items
	 */
	private List<StoreItem> getAllItems() {
		List<StoreItem> items = new ArrayList<>();
		if (!Files.exists(rootPath)) {
			return items;
		}

		try {
			Files.walk(rootPath).filter(path -> path.toString().endsWith(".json")).forEach(path -> {
				try {
					String itemJson = Files.readString(path);
					StoreItem item = objectMapper.readValue(itemJson, StoreItem.class);
					items.add(item);
				}
				catch (Exception e) {
					// Skip invalid files
				}
			});
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to scan file system for items", e);
		}

		return items;
	}

	/**
	 * Scan directories for namespaces.
	 * @param path current path
	 * @param currentNamespace current namespace path
	 * @param namespaceSet set to collect namespaces
	 * @param request namespace request
	 */
	private void scanDirectoriesForNamespaces(Path path, List<String> currentNamespace, Set<String> namespaceSet,
			NamespaceListRequest request) {
		try {
			if (!Files.exists(path) || !Files.isDirectory(path)) {
				return;
			}

			Files.list(path).forEach(subPath -> {
				if (Files.isDirectory(subPath)) {
					List<String> newNamespace = new ArrayList<>(currentNamespace);
					newNamespace.add(subPath.getFileName().toString());

					// Check constraints
					if (matchesNamespacePrefix(newNamespace, request.getNamespace())
							&& matchesMaxDepth(newNamespace, request.getMaxDepth())) {
						String namespacePath = String.join("/", newNamespace);
						namespaceSet.add(namespacePath);
					}

					// Recurse into subdirectory
					scanDirectoriesForNamespaces(subPath, newNamespace, namespaceSet, request);
				}
			});
		}
		catch (IOException e) {
			// Skip directories that can't be read
		}
	}

	/**
	 * Check if namespace matches prefix filter.
	 * @param namespace namespace to check
	 * @param prefixFilter prefix filter
	 * @return true if matches
	 */
	private boolean matchesNamespacePrefix(List<String> namespace, List<String> prefixFilter) {
		if (prefixFilter.isEmpty()) {
			return true;
		}
		return startsWithPrefix(namespace, prefixFilter);
	}

	/**
	 * Check if namespace matches max depth constraint.
	 * @param namespace namespace to check
	 * @param maxDepth max depth
	 * @return true if matches
	 */
	private boolean matchesMaxDepth(List<String> namespace, int maxDepth) {
		return maxDepth == -1 || namespace.size() <= maxDepth;
	}

	/**
	 * Clean up empty directories.
	 * @param directory directory to clean
	 */
	private void cleanupEmptyDirectories(Path directory) {
		try {
			while (directory != null && !directory.equals(rootPath)) {
				if (Files.exists(directory) && isDirectoryEmpty(directory)) {
					Files.delete(directory);
					directory = directory.getParent();
				}
				else {
					break;
				}
			}
		}
		catch (IOException e) {
			// Ignore cleanup failures
		}
	}

	/**
	 * Check if directory is empty.
	 * @param directory directory to check
	 * @return true if empty
	 */
	private boolean isDirectoryEmpty(Path directory) throws IOException {
		try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
			return !dirStream.iterator().hasNext();
		}
	}

	/**
	 * Delete directory recursively.
	 * @param directory directory to delete
	 */
	private void deleteDirectoryRecursively(Path directory) throws IOException {
		Files.walk(directory).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(file -> {
			if (!file.delete()) {
				file.deleteOnExit();
			}
		});
	}

}

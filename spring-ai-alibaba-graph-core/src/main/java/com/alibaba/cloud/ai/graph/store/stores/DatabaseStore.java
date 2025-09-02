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

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Relational database-based implementation of the Store interface.
 * <p>
 * This implementation uses JDBC for database storage, supporting MySQL, PostgreSQL, H2,
 * and other JDBC-compatible databases. It provides ACID compliance and enterprise-grade
 * reliability.
 * </p>
 *
 * @author Spring AI Alibaba
 * @since 1.0.0.3
 */
public class DatabaseStore extends BaseStore {

	private final DataSource dataSource;

	private final ObjectMapper objectMapper;

	private final String tableName;

	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	/**
	 * Constructor with default table name.
	 * @param dataSource database data source
	 */
	public DatabaseStore(DataSource dataSource) {
		this(dataSource, "spring_ai_store");
	}

	/**
	 * Constructor with custom table name.
	 * @param dataSource database data source
	 * @param tableName table name
	 */
	public DatabaseStore(DataSource dataSource, String tableName) {
		this.dataSource = dataSource;
		this.tableName = tableName;
		this.objectMapper = new ObjectMapper();
		this.objectMapper.findAndRegisterModules();
		initializeTable();
	}

	@Override
	public void putItem(StoreItem item) {
		validatePutItem(item);

		lock.writeLock().lock();
		try {
			String itemId = createItemId(item.getNamespace(), item.getKey());
			String namespaceJson = objectMapper.writeValueAsString(item.getNamespace());
			String valueJson = objectMapper.writeValueAsString(item.getValue());

			// Use MERGE for H2 compatibility instead of ON DUPLICATE KEY UPDATE
			String sql = "MERGE INTO " + tableName + " (id, namespace, key_name, value_json, created_at, updated_at) "
					+ "KEY(id) VALUES (?, ?, ?, ?, ?, ?)";

			try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

				stmt.setString(1, itemId);
				stmt.setString(2, namespaceJson);
				stmt.setString(3, item.getKey());
				stmt.setString(4, valueJson);
				stmt.setTimestamp(5, new Timestamp(item.getCreatedAt()));
				stmt.setTimestamp(6, new Timestamp(item.getUpdatedAt()));

				stmt.executeUpdate();
			}
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to store item in database", e);
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
			String itemId = createItemId(namespace, key);
			String sql = "SELECT namespace, key_name, value_json, created_at, updated_at FROM " + tableName
					+ " WHERE id = ?";

			try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

				stmt.setString(1, itemId);
				ResultSet rs = stmt.executeQuery();

				if (rs.next()) {
					return Optional.of(resultSetToStoreItem(rs));
				}

				return Optional.empty();
			}
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to retrieve item from database", e);
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
			String itemId = createItemId(namespace, key);
			String sql = "DELETE FROM " + tableName + " WHERE id = ?";

			try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

				stmt.setString(1, itemId);
				return stmt.executeUpdate() > 0;
			}
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to delete item from database", e);
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
		String sql = "DELETE FROM " + tableName;

		try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
			stmt.executeUpdate(sql);
		}
		catch (SQLException e) {
			throw new RuntimeException("Failed to clear database store", e);
		}
	}

	@Override
	public long size() {
		String sql = "SELECT COUNT(*) FROM " + tableName;

		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(sql)) {

			rs.next();
			return rs.getLong(1);
		}
		catch (SQLException e) {
			throw new RuntimeException("Failed to get store size from database", e);
		}
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	/**
	 * Initialize database table.
	 */
	private void initializeTable() {
		// Create table with database-agnostic SQL
		String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" + "id VARCHAR(1000) PRIMARY KEY, "
				+ "namespace TEXT, " + "key_name VARCHAR(500), " + "value_json TEXT, " + "created_at TIMESTAMP, "
				+ "updated_at TIMESTAMP" + ")";

		try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
			stmt.executeUpdate(sql);
		}
		catch (SQLException e) {
			throw new RuntimeException("Failed to initialize table", e);
		}
	}

	/**
	 * Create item ID from namespace and key.
	 * @param namespace namespace
	 * @param key key
	 * @return item ID
	 */
	private String createItemId(List<String> namespace, String key) {
		return createStoreKey(namespace, key);
	}

	/**
	 * Get all items from database.
	 * @return list of all items
	 */
	private List<StoreItem> getAllItems() {
		List<StoreItem> items = new ArrayList<>();
		String sql = "SELECT namespace, key_name, value_json, created_at, updated_at FROM " + tableName;

		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(sql)) {

			while (rs.next()) {
				try {
					items.add(resultSetToStoreItem(rs));
				}
				catch (Exception e) {
					// Skip invalid items
				}
			}
		}
		catch (SQLException e) {
			throw new RuntimeException("Failed to retrieve all items from database", e);
		}

		return items;
	}

	/**
	 * Convert ResultSet to StoreItem.
	 * @param rs result set
	 * @return StoreItem
	 * @throws Exception if conversion fails
	 */
	@SuppressWarnings("unchecked")
	private StoreItem resultSetToStoreItem(ResultSet rs) throws Exception {
		String namespaceJson = rs.getString("namespace");
		String key = rs.getString("key_name");
		String valueJson = rs.getString("value_json");
		Timestamp createdAt = rs.getTimestamp("created_at");
		Timestamp updatedAt = rs.getTimestamp("updated_at");

		List<String> namespace = objectMapper.readValue(namespaceJson, List.class);
		Map<String, Object> value = objectMapper.readValue(valueJson, Map.class);

		return new StoreItem(namespace, key, value, createdAt.getTime(), updatedAt.getTime());
	}

}

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

import com.alibaba.cloud.ai.graph.store.NamespaceListRequest;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.alibaba.cloud.ai.graph.store.StoreSearchRequest;
import com.alibaba.cloud.ai.graph.store.StoreSearchResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for DatabaseStore implementation using H2 memory database.
 *
 * @author Spring AI Alibaba
 */
class DatabaseStoreTest {

	private DatabaseStore databaseStore;

	@BeforeEach
	void setUp() {
		// Create H2 in-memory database with unique URL for test isolation
		String dbUrl = "jdbc:h2:mem:testdb" + System.nanoTime() + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(dbUrl);
		config.setUsername("sa");
		config.setPassword("");
		config.setDriverClassName("org.h2.Driver");

		DataSource dataSource = new HikariDataSource(config);
		databaseStore = new DatabaseStore(dataSource, "test_store");
	}

	@Test
	void testPutAndGetItem() {
		// Given
		List<String> namespace = List.of("users", "user123");
		String key = "preferences";
		Map<String, Object> value = Map.of("theme", "dark", "language", "en-US");
		StoreItem item = StoreItem.of(namespace, key, value);

		// When
		databaseStore.putItem(item);
		Optional<StoreItem> retrieved = databaseStore.getItem(namespace, key);

		// Then
		assertThat(retrieved).isPresent();
		assertThat(retrieved.get().getNamespace()).isEqualTo(namespace);
		assertThat(retrieved.get().getKey()).isEqualTo(key);
		assertThat(retrieved.get().getValue()).isEqualTo(value);
	}

	@Test
	void testDeleteItem() {
		// Given
		List<String> namespace = List.of("test", "namespace");
		String key = "test_key";
		Map<String, Object> value = Map.of("data", "test_value");
		StoreItem item = StoreItem.of(namespace, key, value);

		databaseStore.putItem(item);
		assertThat(databaseStore.getItem(namespace, key)).isPresent();

		// When
		boolean deleted = databaseStore.deleteItem(namespace, key);

		// Then
		assertThat(deleted).isTrue();
		assertThat(databaseStore.getItem(namespace, key)).isEmpty();
		assertThat(databaseStore.deleteItem(namespace, key)).isFalse(); // Already deleted
	}

	@Test
	void testSearchItems() {
		// Given
		setupTestData();

		// When - search all items
		StoreSearchRequest request = StoreSearchRequest.builder().build();
		StoreSearchResult result = databaseStore.searchItems(request);

		// Then
		assertThat(result.getItems()).hasSize(3);
		assertThat(result.getTotalCount()).isEqualTo(3);
	}

	@Test
	void testListNamespaces() {
		// Given
		setupTestData();

		// When
		NamespaceListRequest request = NamespaceListRequest.builder().build();
		List<String> namespaces = databaseStore.listNamespaces(request);

		// Then
		assertThat(namespaces).hasSize(6);
		assertThat(namespaces).containsExactlyInAnyOrder("users", "users/admin", "users/user1",
				"users/user1/preferences", "users/user2", "users/user2/preferences");
	}

	@Test
	void testValidationErrors() {
		// Test null item
		assertThrows(IllegalArgumentException.class, () -> databaseStore.putItem(null));

		// Test null namespace
		assertThrows(IllegalArgumentException.class, () -> databaseStore.getItem(null, "key"));

		// Test null key
		assertThrows(IllegalArgumentException.class, () -> databaseStore.getItem(List.of("namespace"), null));

		// Test empty key
		assertThrows(IllegalArgumentException.class, () -> databaseStore.getItem(List.of("namespace"), ""));

		// Test null search request
		assertThrows(IllegalArgumentException.class, () -> databaseStore.searchItems(null));

		// Test null namespace request
		assertThrows(IllegalArgumentException.class, () -> databaseStore.listNamespaces(null));
	}

	@Test
	void testSizeAndClear() {
		// Given
		assertThat(databaseStore.isEmpty()).isTrue();
		assertThat(databaseStore.size()).isEqualTo(0);

		setupTestData();

		// When
		assertThat(databaseStore.isEmpty()).isFalse();
		assertThat(databaseStore.size()).isEqualTo(3);

		databaseStore.clear();

		// Then
		assertThat(databaseStore.isEmpty()).isTrue();
		assertThat(databaseStore.size()).isEqualTo(0);
	}

	@Test
	void testUpdateExistingItem() {
		// Given
		List<String> namespace = List.of("test");
		String key = "updatable_item";
		Map<String, Object> originalValue = Map.of("version", 1);
		StoreItem originalItem = StoreItem.of(namespace, key, originalValue);

		databaseStore.putItem(originalItem);

		// When - update the same item
		Map<String, Object> updatedValue = Map.of("version", 2, "updated", true);
		StoreItem updatedItem = StoreItem.of(namespace, key, updatedValue);
		databaseStore.putItem(updatedItem);

		// Then
		Optional<StoreItem> retrieved = databaseStore.getItem(namespace, key);
		assertThat(retrieved).isPresent();
		assertThat(retrieved.get().getValue()).isEqualTo(updatedValue);
		assertThat(databaseStore.size()).isEqualTo(1); // Should still be 1 item
	}

	private void setupTestData() {
		// User admin data
		databaseStore.putItem(
				StoreItem.of(List.of("users", "admin"), "profile", Map.of("name", "Administrator", "role", "admin")));

		// User1 preferences
		databaseStore.putItem(StoreItem.of(List.of("users", "user1", "preferences"), "ui_settings",
				Map.of("theme", "dark", "language", "en-US")));

		// User2 preferences
		databaseStore.putItem(StoreItem.of(List.of("users", "user2", "preferences"), "ui_settings",
				Map.of("theme", "light", "language", "zh-CN")));
	}

}

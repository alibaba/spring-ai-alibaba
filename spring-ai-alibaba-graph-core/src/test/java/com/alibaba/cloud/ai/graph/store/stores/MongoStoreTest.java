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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for MongoStore implementation.
 *
 * @author Spring AI Alibaba
 */
class MongoStoreTest {

	private MongoStore mongoStore;

	@BeforeEach
	void setUp() {
		mongoStore = new MongoStore();
	}

	@Test
	void testPutAndGetItem() {
		// Given
		List<String> namespace = List.of("users", "user123");
		String key = "preferences";
		Map<String, Object> value = Map.of("theme", "dark", "language", "en-US");
		StoreItem item = StoreItem.of(namespace, key, value);

		// When
		mongoStore.putItem(item);
		Optional<StoreItem> retrieved = mongoStore.getItem(namespace, key);

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

		mongoStore.putItem(item);
		assertThat(mongoStore.getItem(namespace, key)).isPresent();

		// When
		boolean deleted = mongoStore.deleteItem(namespace, key);

		// Then
		assertThat(deleted).isTrue();
		assertThat(mongoStore.getItem(namespace, key)).isEmpty();
		assertThat(mongoStore.deleteItem(namespace, key)).isFalse(); // Already deleted
	}

	@Test
	void testSearchItems() {
		// Given
		setupTestData();

		// When - search all items
		StoreSearchRequest request = StoreSearchRequest.builder().build();
		StoreSearchResult result = mongoStore.searchItems(request);

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
		List<String> namespaces = mongoStore.listNamespaces(request);

		// Then
		assertThat(namespaces).hasSize(6);
		assertThat(namespaces).containsExactlyInAnyOrder("users", "users/admin", "users/user1",
				"users/user1/preferences", "users/user2", "users/user2/preferences");
	}

	@Test
	void testValidationErrors() {
		// Test null item
		assertThrows(IllegalArgumentException.class, () -> mongoStore.putItem(null));

		// Test null namespace
		assertThrows(IllegalArgumentException.class, () -> mongoStore.getItem(null, "key"));

		// Test null key
		assertThrows(IllegalArgumentException.class, () -> mongoStore.getItem(List.of("namespace"), null));

		// Test empty key
		assertThrows(IllegalArgumentException.class, () -> mongoStore.getItem(List.of("namespace"), ""));

		// Test null search request
		assertThrows(IllegalArgumentException.class, () -> mongoStore.searchItems(null));

		// Test null namespace request
		assertThrows(IllegalArgumentException.class, () -> mongoStore.listNamespaces(null));
	}

	@Test
	void testSizeAndClear() {
		// Given
		assertThat(mongoStore.isEmpty()).isTrue();
		assertThat(mongoStore.size()).isEqualTo(0);

		setupTestData();

		// When
		assertThat(mongoStore.isEmpty()).isFalse();
		assertThat(mongoStore.size()).isEqualTo(3);

		mongoStore.clear();

		// Then
		assertThat(mongoStore.isEmpty()).isTrue();
		assertThat(mongoStore.size()).isEqualTo(0);
	}

	@Test
	void testSearchByNamespace() {
		// Given
		setupTestData();

		// When
		StoreSearchRequest request = StoreSearchRequest.builder().namespace(List.of("users", "user1")).build();
		StoreSearchResult result = mongoStore.searchItems(request);

		// Then
		assertThat(result.getItems()).hasSize(1);
		List<String> namespace = result.getItems().get(0).getNamespace();
		assertThat(namespace).hasSize(3);
		assertThat(namespace.get(0)).isEqualTo("users");
		assertThat(namespace.get(1)).isEqualTo("user1");
		assertThat(namespace.get(2)).isEqualTo("preferences");
	}

	@Test
	void testSearchByQuery() {
		// Given
		setupTestData();

		// When
		StoreSearchRequest request = StoreSearchRequest.builder().query("Administrator").build();
		StoreSearchResult result = mongoStore.searchItems(request);

		// Then
		assertThat(result.getItems()).hasSize(1);
		assertThat(result.getItems().get(0).getValue().get("name")).isEqualTo("Administrator");
	}

	@Test
	void testSearchWithFilters() {
		// Given
		setupTestData();

		// When
		StoreSearchRequest request = StoreSearchRequest.builder().filter(Map.of("theme", "dark")).build();
		StoreSearchResult result = mongoStore.searchItems(request);

		// Then
		assertThat(result.getItems()).hasSize(1);
		assertThat(result.getItems().get(0).getValue().get("theme")).isEqualTo("dark");
	}

	@Test
	void testPagination() {
		// Given
		setupTestData();

		// When
		StoreSearchRequest request = StoreSearchRequest.builder().offset(1).limit(1).build();
		StoreSearchResult result = mongoStore.searchItems(request);

		// Then
		assertThat(result.getItems()).hasSize(1);
		assertThat(result.getOffset()).isEqualTo(1);
		assertThat(result.getLimit()).isEqualTo(1);
		assertThat(result.getTotalCount()).isEqualTo(3);
	}

	@Test
	void testUpdateExistingItem() {
		// Given
		List<String> namespace = List.of("test");
		String key = "updatable_item";
		Map<String, Object> originalValue = Map.of("version", 1);
		StoreItem originalItem = StoreItem.of(namespace, key, originalValue);

		mongoStore.putItem(originalItem);

		// When - update the same item
		Map<String, Object> updatedValue = Map.of("version", 2, "updated", true);
		StoreItem updatedItem = StoreItem.of(namespace, key, updatedValue);
		mongoStore.putItem(updatedItem);

		// Then
		Optional<StoreItem> retrieved = mongoStore.getItem(namespace, key);
		assertThat(retrieved).isPresent();
		assertThat(retrieved.get().getValue()).isEqualTo(updatedValue);
		assertThat(mongoStore.size()).isEqualTo(1); // Should still be 1 item
	}

	private void setupTestData() {
		// User admin data
		mongoStore.putItem(
				StoreItem.of(List.of("users", "admin"), "profile", Map.of("name", "Administrator", "role", "admin")));

		// User1 preferences
		mongoStore.putItem(StoreItem.of(List.of("users", "user1", "preferences"), "ui_settings",
				Map.of("theme", "dark", "language", "en-US")));

		// User2 preferences
		mongoStore.putItem(StoreItem.of(List.of("users", "user2", "preferences"), "ui_settings",
				Map.of("theme", "light", "language", "zh-CN")));
	}

}

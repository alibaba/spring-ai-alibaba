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

import com.alibaba.cloud.ai.graph.store.stores.DatabaseStore;
import com.alibaba.cloud.ai.graph.store.stores.FileSystemStore;
import com.alibaba.cloud.ai.graph.store.stores.MemoryStore;
import com.alibaba.cloud.ai.graph.store.stores.MongoStore;
import com.alibaba.cloud.ai.graph.store.stores.RedisStore;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for all Store implementations.
 *
 * @author Spring AI Alibaba
 */
public class StoreIntegrationTest {

	@TempDir
	Path tempDir;

	@Test
	void testAllStoreImplementations() {
		// Create instances of all Store implementations
		List<Store> stores = Arrays.asList(new MemoryStore(), new FileSystemStore(tempDir), createDatabaseStore(),
				new RedisStore(), new MongoStore());

		for (Store store : stores) {
			testStoreBasicOperations(store);
			store.clear(); // Clean up after each test
		}
	}

	@Test
	void testStoreSearchAndNamespaceOperations() {
		// Test with MemoryStore as representative
		Store store = new MemoryStore();

		// Setup test data with hierarchical namespaces
		setupHierarchicalTestData(store);

		// Test search operations
		testSearchOperations(store);

		// Test namespace listing
		testNamespaceOperations(store);

		store.clear();
	}

	@Test
	void testStoreConsistencyAcrossImplementations() {
		// Create instances of all Store implementations
		List<Store> stores = Arrays.asList(new MemoryStore(), new FileSystemStore(tempDir.resolve("consistency")),
				createDatabaseStore(), new RedisStore(), new MongoStore());

		// Test data
		List<String> namespace = List.of("test", "consistency");
		String key = "data";
		Map<String, Object> value = Map.of("message", "hello world", "count", 42);
		StoreItem item = StoreItem.of(namespace, key, value);

		// Test consistency across all implementations
		for (Store store : stores) {
			// Put item
			store.putItem(item);

			// Get item and verify
			Optional<StoreItem> retrieved = store.getItem(namespace, key);
			assertThat(retrieved).isPresent();
			assertThat(retrieved.get().getNamespace()).isEqualTo(namespace);
			assertThat(retrieved.get().getKey()).isEqualTo(key);
			assertThat(retrieved.get().getValue()).isEqualTo(value);

			// Test size
			assertThat(store.size()).isEqualTo(1);
			assertThat(store.isEmpty()).isFalse();

			// Clean up
			store.clear();
			assertThat(store.isEmpty()).isTrue();
		}
	}

	private void testStoreBasicOperations(Store store) {
		// Test data
		List<String> namespace = List.of("users", "user123");
		String key = "preferences";
		Map<String, Object> value = Map.of("theme", "dark", "language", "en-US");
		StoreItem item = StoreItem.of(namespace, key, value);

		// Test put and get
		store.putItem(item);
		Optional<StoreItem> retrieved = store.getItem(namespace, key);
		assertThat(retrieved).isPresent();
		assertThat(retrieved.get().getNamespace()).isEqualTo(namespace);
		assertThat(retrieved.get().getKey()).isEqualTo(key);
		assertThat(retrieved.get().getValue()).isEqualTo(value);

		// Test size
		assertThat(store.size()).isEqualTo(1);
		assertThat(store.isEmpty()).isFalse();

		// Test delete
		boolean deleted = store.deleteItem(namespace, key);
		assertThat(deleted).isTrue();
		assertThat(store.getItem(namespace, key)).isEmpty();
		assertThat(store.isEmpty()).isTrue();
	}

	private void setupHierarchicalTestData(Store store) {
		// Create hierarchical data structure
		store.putItem(StoreItem.of(List.of("company", "engineering"), "team_size", Map.of("count", 50)));
		store.putItem(StoreItem.of(List.of("company", "engineering", "backend"), "languages",
				Map.of("primary", "Java", "secondary", "Go")));
		store.putItem(StoreItem.of(List.of("company", "engineering", "frontend"), "frameworks",
				Map.of("primary", "React", "secondary", "Vue")));
		store.putItem(StoreItem.of(List.of("company", "marketing"), "budget", Map.of("annual", 1000000)));
		store.putItem(StoreItem.of(List.of("company", "hr"), "policies", Map.of("remote_work", true)));
	}

	private void testSearchOperations(Store store) {
		// Test search all
		StoreSearchRequest allRequest = StoreSearchRequest.builder().build();
		StoreSearchResult allResult = store.searchItems(allRequest);
		assertThat(allResult.getItems()).hasSize(5);

		// Test search by namespace prefix
		StoreSearchRequest engineeringRequest = StoreSearchRequest.builder()
			.namespace(List.of("company", "engineering"))
			.build();
		StoreSearchResult engineeringResult = store.searchItems(engineeringRequest);
		assertThat(engineeringResult.getItems()).hasSize(3); // engineering + backend +
		// frontend

		// Test search by query
		StoreSearchRequest queryRequest = StoreSearchRequest.builder().query("Java").build();
		StoreSearchResult queryResult = store.searchItems(queryRequest);
		assertThat(queryResult.getItems()).hasSize(1);

		// Test search with filter
		StoreSearchRequest filterRequest = StoreSearchRequest.builder().filter(Map.of("remote_work", true)).build();
		StoreSearchResult filterResult = store.searchItems(filterRequest);
		assertThat(filterResult.getItems()).hasSize(1);

		// Test pagination
		StoreSearchRequest paginatedRequest = StoreSearchRequest.builder().offset(1).limit(2).build();
		StoreSearchResult paginatedResult = store.searchItems(paginatedRequest);
		assertThat(paginatedResult.getItems()).hasSize(2);
		assertThat(paginatedResult.getTotalCount()).isEqualTo(5);
		assertThat(paginatedResult.getOffset()).isEqualTo(1);
		assertThat(paginatedResult.getLimit()).isEqualTo(2);
	}

	private void testNamespaceOperations(Store store) {
		// Test list all namespaces
		NamespaceListRequest allNamespaces = NamespaceListRequest.builder().build();
		List<String> namespaces = store.listNamespaces(allNamespaces);
		assertThat(namespaces).contains("company", "company/engineering", "company/engineering/backend",
				"company/engineering/frontend", "company/marketing", "company/hr");

		// Test list with prefix filter
		NamespaceListRequest engineeringNamespaces = NamespaceListRequest.builder()
			.namespace(List.of("company", "engineering"))
			.build();
		List<String> engineeringNs = store.listNamespaces(engineeringNamespaces);
		assertThat(engineeringNs).contains("company/engineering", "company/engineering/backend",
				"company/engineering/frontend");
		assertThat(engineeringNs).doesNotContain("company/marketing", "company/hr");

		// Test list with max depth
		NamespaceListRequest limitedDepth = NamespaceListRequest.builder().maxDepth(2).build();
		List<String> limitedNs = store.listNamespaces(limitedDepth);
		// Should not contain 3-level namespaces
		assertThat(limitedNs).doesNotContain("company/engineering/backend", "company/engineering/frontend");
	}

	private DatabaseStore createDatabaseStore() {
		// Create H2 in-memory database
		String dbUrl = "jdbc:h2:mem:integration_test_" + System.nanoTime()
				+ ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(dbUrl);
		config.setUsername("sa");
		config.setPassword("");
		config.setDriverClassName("org.h2.Driver");

		return new DatabaseStore(new HikariDataSource(config), "integration_store");
	}

}

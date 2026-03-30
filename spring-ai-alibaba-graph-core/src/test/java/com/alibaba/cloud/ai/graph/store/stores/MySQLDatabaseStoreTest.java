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

import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for DatabaseStore with MySQL to verify TEXT column PRIMARY KEY fix.
 *
 * @author Spring AI Alibaba
 */
@Testcontainers
class MySQLDatabaseStoreTest {

	@Container
	private static final MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0")
		.withDatabaseName("testdb")
		.withUsername("test")
		.withPassword("test");

	private static DatabaseStore databaseStore;

	@BeforeAll
	static void setUp() {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(mysqlContainer.getJdbcUrl());
		config.setUsername(mysqlContainer.getUsername());
		config.setPassword(mysqlContainer.getPassword());
		config.setDriverClassName("com.mysql.cj.jdbc.Driver");

		DataSource dataSource = new HikariDataSource(config);
		// This should succeed without the "BLOB/TEXT column 'id' used in key
		// specification without a key length" error
		databaseStore = new DatabaseStore(dataSource, "test_store");
	}

	@AfterAll
	static void tearDown() {
		if (databaseStore != null) {
			databaseStore.clear();
		}
	}

	@Test
	void testMySQLPutAndGetItem() {
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
	void testMySQLTableCreationSucceeds() {
		// This test verifies that the table was created successfully
		// If the VARCHAR fix wasn't applied, the constructor would have thrown an
		// exception
		assertThat(databaseStore).isNotNull();
		assertThat(databaseStore.isEmpty()).isTrue();
	}

	@Test
	void testMySQLUpdateExistingItem() {
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

}

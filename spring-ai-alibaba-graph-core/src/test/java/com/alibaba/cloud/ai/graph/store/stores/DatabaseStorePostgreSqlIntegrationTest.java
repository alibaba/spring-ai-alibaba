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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real PostgreSQL integration tests for DatabaseStore SQL compatibility.
 */
@Testcontainers
class DatabaseStorePostgreSqlIntegrationTest {

	@Container
	private static final PostgreSQLContainer<?> POSTGRESQL = new PostgreSQLContainer<>("postgres:17-alpine")
		.withDatabaseName("testdb")
		.withUsername("postgres")
		.withPassword("postgres");

	private static HikariDataSource dataSource;

	private static DatabaseStore databaseStore;

	@BeforeAll
	static void setUp() {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(POSTGRESQL.getJdbcUrl());
		config.setUsername(POSTGRESQL.getUsername());
		config.setPassword(POSTGRESQL.getPassword());
		config.setDriverClassName("org.postgresql.Driver");
		config.setMaximumPoolSize(3);

		dataSource = new HikariDataSource(config);
		databaseStore = new DatabaseStore(dataSource, "pg17_store");
	}

	@AfterAll
	static void tearDown() {
		if (databaseStore != null) {
			databaseStore.clear();
		}
		if (dataSource != null) {
			dataSource.close();
		}
	}

	@Test
	/**
	 * Verifies put/get flow works on PostgreSQL 17 with dialect-aware upsert SQL.
	 */
	void shouldInsertAndReadItemOnPostgresql17() {
		List<String> namespace = List.of("user_profiles");
		String key = "user_001";
		Map<String, Object> value = Map.of("name", "Wang", "age", 28);

		databaseStore.putItem(StoreItem.of(namespace, key, value));
		Optional<StoreItem> result = databaseStore.getItem(namespace, key);

		assertThat(result).isPresent();
		assertThat(result.get().getValue()).isEqualTo(value);
	}

	@Test
	/**
	 * Verifies upsert update path works on PostgreSQL 17.
	 */
	void shouldUpsertUpdateItemOnPostgresql17() {
		List<String> namespace = List.of("user_profiles");
		String key = "user_002";

		databaseStore.putItem(StoreItem.of(namespace, key, Map.of("version", 1)));
		databaseStore.putItem(StoreItem.of(namespace, key, Map.of("version", 2, "updated", true)));

		Optional<StoreItem> result = databaseStore.getItem(namespace, key);
		assertThat(result).isPresent();
		assertThat(result.get().getValue()).isEqualTo(Map.of("version", 2, "updated", true));
	}

}

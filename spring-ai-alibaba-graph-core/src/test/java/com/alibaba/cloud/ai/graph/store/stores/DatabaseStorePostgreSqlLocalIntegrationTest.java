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
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Local PostgreSQL integration tests for DatabaseStore.
 * <p>
 * Enable this test by passing:
 * -Dpg.local.test.enabled=true
 * and optional connection parameters:
 * -Dpg.local.host=localhost
 * -Dpg.local.port=5432
 * -Dpg.local.database=testdb
 * -Dpg.local.user=postgres
 * -Dpg.local.password=postgres
 * </p>
 */
@EnabledIfSystemProperty(named = "pg.local.test.enabled", matches = "true")
class DatabaseStorePostgreSqlLocalIntegrationTest {

	private static HikariDataSource dataSource;

	private static DatabaseStore databaseStore;

	@BeforeAll
	static void setUp() {
		String host = System.getProperty("pg.local.host", "localhost");
		String port = System.getProperty("pg.local.port", "5432");
		String database = System.getProperty("pg.local.database", "testdb");
		String username = System.getProperty("pg.local.user", "postgres");
		String password = System.getProperty("pg.local.password", "123456");
		ensureDatabaseExists(host, port, database, username, password);
		String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + database;

		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(jdbcUrl);
		config.setUsername(username);
		config.setPassword(password);
		config.setDriverClassName("org.postgresql.Driver");
		config.setMaximumPoolSize(3);

		dataSource = new HikariDataSource(config);
		databaseStore = new DatabaseStore(dataSource, "pg_local_store");
	}

	/**
	 * Ensures the target PostgreSQL database exists before opening the main test pool.
	 */
	private static void ensureDatabaseExists(String host, String port, String database, String username,
			String password) {
		String adminJdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/postgres";
		try (Connection conn = DriverManager.getConnection(adminJdbcUrl, username, password)) {
			try (PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
				stmt.setString(1, database);
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						return;
					}
				}
			}
			try (Statement createStmt = conn.createStatement()) {
				createStmt.execute("CREATE DATABASE \"" + database.replace("\"", "\"\"") + "\"");
			}
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to ensure local PostgreSQL database exists: " + database, e);
		}
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
	 * Verifies insert and read operations on a local PostgreSQL instance.
	 */
	void shouldInsertAndReadItemOnLocalPostgresql() {
		List<String> namespace = List.of("user_profiles");
		String key = "local_user_001";
		Map<String, Object> value = Map.of("name", "LocalUser", "age", 30);

		databaseStore.putItem(StoreItem.of(namespace, key, value));
		Optional<StoreItem> result = databaseStore.getItem(namespace, key);

		assertThat(result).isPresent();
		assertThat(result.get().getValue()).isEqualTo(value);
	}

	@Test
	/**
	 * Verifies upsert update behavior on a local PostgreSQL instance.
	 */
	void shouldUpsertUpdateOnLocalPostgresql() {
		List<String> namespace = List.of("user_profiles");
		String key = "local_user_002";

		databaseStore.putItem(StoreItem.of(namespace, key, Map.of("version", 1)));
		databaseStore.putItem(StoreItem.of(namespace, key, Map.of("version", 2, "updated", true)));

		Optional<StoreItem> result = databaseStore.getItem(namespace, key);
		assertThat(result).isPresent();
		assertThat(result.get().getValue()).isEqualTo(Map.of("version", 2, "updated", true));
	}

}

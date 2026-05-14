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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfDockerAvailable
@Testcontainers
class DatabaseStoreMySqlIntegrationTest {

	private static final String MYSQL_IMAGE_NAME = "mysql:8.0";

	private static MySQLContainer<?> mysqlContainer;

	private static DataSource dataSource;

	@BeforeAll
	static void setUp() {
		MySQLContainer<?> container = new MySQLContainer<>(MYSQL_IMAGE_NAME)
			.withDatabaseName("testdb")
			.withUsername("testuser")
			.withPassword("testpwd");
		container.start();
		mysqlContainer = container;

		MysqlDataSource mysqlDataSource = new MysqlDataSource();
		mysqlDataSource.setURL(mysqlContainer.getJdbcUrl());
		mysqlDataSource.setUser(mysqlContainer.getUsername());
		mysqlDataSource.setPassword(mysqlContainer.getPassword());
		dataSource = mysqlDataSource;
	}

	@AfterAll
	static void tearDown() {
		if (mysqlContainer != null) {
			mysqlContainer.close();
		}
	}

	@Test
	void shouldInitializeAndUpsertItemsOnMySql() throws Exception {
		DatabaseStore store = new DatabaseStore(dataSource, "spring_ai_store_mysql_test");

		List<String> namespace = List.of("users", "alice", "preferences");
		String key = "ui_settings";
		store.putItem(StoreItem.of(namespace, key, Map.of("theme", "dark", "language", "zh-CN")));

		Optional<StoreItem> initialItem = store.getItem(namespace, key);
		assertThat(initialItem).isPresent();
		assertThat(initialItem.get().getValue()).containsEntry("theme", "dark");

		store.putItem(StoreItem.of(namespace, key, Map.of("theme", "light", "language", "zh-CN", "fontSize", 14)));

		Optional<StoreItem> updatedItem = store.getItem(namespace, key);
		assertThat(updatedItem).isPresent();
		assertThat(updatedItem.get().getValue()).containsEntry("theme", "light");
		assertThat(updatedItem.get().getValue()).containsEntry("fontSize", 14);
		assertThat(store.size()).isEqualTo(1);
	}

	@Test
	void shouldCreateFixedLengthPrimaryKeyOnMySql() throws Exception {
		new DatabaseStore(dataSource, "spring_ai_store_mysql_schema_test");

		try (Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(
						"SHOW COLUMNS FROM spring_ai_store_mysql_schema_test LIKE 'id'")) {
			assertThat(resultSet.next()).isTrue();
			assertThat(resultSet.getString("Type")).isEqualToIgnoringCase("char(64)");
			assertThat(resultSet.getString("Key")).isEqualToIgnoringCase("PRI");
		}
	}

}

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

import com.alibaba.cloud.ai.graph.store.NamespaceListRequest;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.alibaba.cloud.ai.graph.store.StoreSearchRequest;
import com.alibaba.cloud.ai.graph.store.StoreSearchResult;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Dialect-focused tests for DatabaseStore using mocked JDBC objects.
 * This avoids local database dependencies in CI and validates SQL routing logic.
 */
class DatabaseStoreAllDialectTest {

	@Test
	/**
	 * Verifies that MySQL dialect uses ON DUPLICATE KEY UPDATE for upsert.
	 */
	void shouldUseMysqlUpsertSql() throws Exception {
		TestFixture fixture = fixture("MySQL");
		StoreItem item = StoreItem.of(List.of("users", "u1"), "k1", Map.of("name", "Alice"));

		fixture.store.putItem(item);

		verify(fixture.connection).prepareStatement(contains("ON DUPLICATE KEY UPDATE"));
	}

	@Test
	/**
	 * Verifies that PostgreSQL dialect uses ON CONFLICT for upsert.
	 */
	void shouldUsePostgresqlUpsertSql() throws Exception {
		TestFixture fixture = fixture("PostgreSQL");
		StoreItem item = StoreItem.of(List.of("users", "u1"), "k1", Map.of("name", "Alice"));

		fixture.store.putItem(item);

		verify(fixture.connection).prepareStatement(contains("ON CONFLICT (id) DO UPDATE"));
	}

	@Test
	/**
	 * Verifies that Oracle dialect uses MERGE INTO ... USING DUAL for upsert.
	 */
	void shouldUseOracleUpsertSql() throws Exception {
		TestFixture fixture = fixture("Oracle");
		StoreItem item = StoreItem.of(List.of("users", "u1"), "k1", Map.of("name", "Alice"));

		fixture.store.putItem(item);

		verify(fixture.connection).prepareStatement(contains("MERGE INTO"));
		verify(fixture.connection).prepareStatement(contains("USING DUAL"));
	}

	@Test
	/**
	 * Verifies that H2 dialect uses MERGE ... KEY(id) for upsert.
	 */
	void shouldUseH2MergeSql() throws Exception {
		TestFixture fixture = fixture("H2");
		StoreItem item = StoreItem.of(List.of("users", "u1"), "k1", Map.of("name", "Alice"));

		fixture.store.putItem(item);

		verify(fixture.connection).prepareStatement(contains("MERGE INTO"));
		verify(fixture.connection).prepareStatement(contains("KEY(id) VALUES"));
	}

	@Test
	/**
	 * Verifies that MySQL DDL is used during table initialization.
	 */
	void shouldUseMysqlInitializeTableSql() throws Exception {
		TestFixture fixture = fixture("MySQL");

		verify(fixture.statement).executeUpdate(contains("CREATE TABLE IF NOT EXISTS mock_store"));
		verify(fixture.statement).executeUpdate(contains("id VARCHAR(500) PRIMARY KEY"));
	}

	@Test
	/**
	 * Verifies blank table names fall back to the default table name.
	 */
	void shouldFallbackToDefaultTableNameWhenTableNameIsBlank() throws Exception {
		TestFixture fixture = fixture("MySQL", "   ");
		StoreItem item = StoreItem.of(List.of("users", "u1"), "k1", Map.of("name", "Alice"));

		fixture.store.putItem(item);

		verify(fixture.statement).executeUpdate(contains("CREATE TABLE IF NOT EXISTS spring_ai_store"));
		verify(fixture.connection).prepareStatement(contains("INSERT INTO spring_ai_store"));
	}

	@Test
	/**
	 * Verifies that PostgreSQL DDL is used during table initialization.
	 */
	void shouldUsePostgresqlInitializeTableSql() throws Exception {
		TestFixture fixture = fixture("PostgreSQL");

		verify(fixture.statement).executeUpdate(contains("CREATE TABLE IF NOT EXISTS mock_store"));
		verify(fixture.statement).executeUpdate(contains("id TEXT PRIMARY KEY"));
	}

	@Test
	/**
	 * Verifies that Oracle DDL is used during table initialization.
	 */
	void shouldUseOracleInitializeTableSql() throws Exception {
		TestFixture fixture = fixture("Oracle");

		verify(fixture.statement).executeUpdate(contains("CREATE TABLE mock_store"));
		verify(fixture.statement).executeUpdate(contains("id VARCHAR2(500) PRIMARY KEY"));
	}

	@Test
	/**
	 * Verifies getItem issues a SELECT and deserializes one record correctly.
	 */
	void shouldUseGetItemSqlAndDeserializeResult() throws Exception {
		TestFixture fixture = fixture("MySQL");
		when(fixture.preparedStatement.executeQuery()).thenReturn(fixture.resultSet);
		when(fixture.resultSet.next()).thenReturn(true);
		when(fixture.resultSet.getString("namespace")).thenReturn("[\"users\",\"u1\"]");
		when(fixture.resultSet.getString("key_name")).thenReturn("k1");
		when(fixture.resultSet.getString("value_json")).thenReturn("{\"name\":\"Alice\"}");
		when(fixture.resultSet.getTimestamp("created_at")).thenReturn(new java.sql.Timestamp(1000L));
		when(fixture.resultSet.getTimestamp("updated_at")).thenReturn(new java.sql.Timestamp(2000L));

		Optional<StoreItem> item = fixture.store.getItem(List.of("users", "u1"), "k1");

		assertThat(item).isPresent();
		assertThat(item.get().getKey()).isEqualTo("k1");
		verify(fixture.connection).prepareStatement(contains("WHERE id = ?"));
	}

	@Test
	/**
	 * Verifies deleteItem issues the expected DELETE SQL.
	 */
	void shouldUseDeleteItemSql() throws Exception {
		TestFixture fixture = fixture("MySQL");
		when(fixture.preparedStatement.executeUpdate()).thenReturn(1);

		boolean deleted = fixture.store.deleteItem(List.of("users", "u1"), "k1");

		assertThat(deleted).isTrue();
		verify(fixture.connection).prepareStatement(contains("DELETE FROM mock_store WHERE id = ?"));
	}

	@Test
	/**
	 * Verifies clear and size issue DELETE and COUNT SQL respectively.
	 */
	void shouldUseClearAndSizeSql() throws Exception {
		TestFixture fixture = fixture("MySQL");
		when(fixture.statement.executeQuery(contains("COUNT(*)"))).thenReturn(fixture.resultSet);
		when(fixture.resultSet.next()).thenReturn(true);
		when(fixture.resultSet.getLong(1)).thenReturn(3L);

		fixture.store.clear();
		long size = fixture.store.size();

		assertThat(size).isEqualTo(3L);
		verify(fixture.statement).executeUpdate("DELETE FROM mock_store");
		verify(fixture.statement).executeQuery("SELECT COUNT(*) FROM mock_store");
	}

	@Test
	/**
	 * Verifies searchItems and listNamespaces both use the full table SELECT query.
	 */
	void shouldUseSelectAllSqlForSearchAndNamespaces() throws Exception {
		TestFixture fixture = fixture("MySQL");
		when(fixture.statement.executeQuery(contains("SELECT namespace, key_name, value_json, created_at, updated_at FROM mock_store")))
			.thenReturn(fixture.resultSet);
		when(fixture.resultSet.next()).thenReturn(true, true, false, true, true, false);
		when(fixture.resultSet.getString("namespace")).thenReturn("[\"users\",\"u1\"]", "[\"products\",\"p1\"]",
				"[\"users\",\"u1\"]", "[\"products\",\"p1\"]");
		when(fixture.resultSet.getString("key_name")).thenReturn("k1", "k2", "k1", "k2");
		when(fixture.resultSet.getString("value_json")).thenReturn("{\"name\":\"Alice\"}", "{\"name\":\"Book\"}",
				"{\"name\":\"Alice\"}", "{\"name\":\"Book\"}");
		when(fixture.resultSet.getTimestamp("created_at")).thenReturn(new java.sql.Timestamp(1000L),
				new java.sql.Timestamp(1000L), new java.sql.Timestamp(1000L), new java.sql.Timestamp(1000L));
		when(fixture.resultSet.getTimestamp("updated_at")).thenReturn(new java.sql.Timestamp(2000L),
				new java.sql.Timestamp(2000L), new java.sql.Timestamp(2000L), new java.sql.Timestamp(2000L));

		StoreSearchResult result = fixture.store.searchItems(StoreSearchRequest.builder().offset(0).limit(10).build());
		List<String> namespaces = fixture.store.listNamespaces(NamespaceListRequest.builder().build());

		assertThat(result.getItems()).hasSize(2);
		assertThat(namespaces).contains("users", "products");
		verify(fixture.statement, times(2))
			.executeQuery("SELECT namespace, key_name, value_json, created_at, updated_at FROM mock_store");
	}

	@Test
	/**
	 * Verifies unsupported dialects are rejected during initialization.
	 */
	void shouldThrowForUnsupportedDialect() throws Exception {
		assertThatThrownBy(() -> fixture("SQLite"))
			.isInstanceOf(UnsupportedOperationException.class)
			.hasMessageContaining("Unsupported database dialect");
	}

	@Test
	/**
	 * Verifies input validation for putItem.
	 */
	void shouldValidateInputForPutItem() throws Exception {
		TestFixture fixture = fixture("MySQL");

		assertThatThrownBy(() -> fixture.store.putItem(null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	/**
	 * Verifies MySQL JDBC URL generation.
	 */
	void shouldBuildMysqlJdbcUrl() {
		String jdbcUrl = DatabaseStore.buildJdbcUrl("mysql", "localhost", 3306, "ai_db");
		assertThat(jdbcUrl).contains("jdbc:mysql://localhost:3306/ai_db");
		assertThat(jdbcUrl).contains("createDatabaseIfNotExist=true");
	}

	@Test
	/**
	 * Verifies PostgreSQL JDBC URL generation.
	 */
	void shouldBuildPostgresqlJdbcUrl() {
		String jdbcUrl = DatabaseStore.buildJdbcUrl("postgresql", "localhost", 5432, "ai_db");
		assertThat(jdbcUrl).isEqualTo("jdbc:postgresql://localhost:5432/ai_db");
	}

	@Test
	/**
	 * Verifies Oracle JDBC URL generation.
	 */
	void shouldBuildOracleJdbcUrl() {
		String jdbcUrl = DatabaseStore.buildJdbcUrl("oracle", "localhost", 1521, "FREE");
		assertThat(jdbcUrl).isEqualTo("jdbc:oracle:thin:@localhost:1521:FREE");
	}

	@Test
	/**
	 * Verifies H2 JDBC URL generation.
	 */
	void shouldBuildH2JdbcUrl() {
		String jdbcUrl = DatabaseStore.buildJdbcUrl("h2", "ignored", 1, "testdb");
		assertThat(jdbcUrl).isEqualTo("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
	}

	@Test
	/**
	 * Verifies JDBC URL builder validates required parameters.
	 */
	void shouldValidateBuildJdbcUrlParameters() {
		assertThatThrownBy(() -> DatabaseStore.buildJdbcUrl("", "localhost", 3306, "ai_db"))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> DatabaseStore.buildJdbcUrl("mysql", "", 3306, "ai_db"))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> DatabaseStore.buildJdbcUrl("mysql", "localhost", 0, "ai_db"))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> DatabaseStore.buildJdbcUrl("mysql", "localhost", 3306, ""))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> DatabaseStore.buildJdbcUrl("sqlite", "localhost", 3306, "ai_db"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	private TestFixture fixture(String databaseProductName) throws Exception {
		return fixture(databaseProductName, "mock_store");
	}

	private TestFixture fixture(String databaseProductName, String tableName) throws Exception {
		DataSource dataSource = mock(DataSource.class);
		Connection connection = mock(Connection.class);
		DatabaseMetaData metaData = mock(DatabaseMetaData.class);
		Statement statement = mock(Statement.class);
		PreparedStatement preparedStatement = mock(PreparedStatement.class);
		ResultSet resultSet = mock(ResultSet.class);

		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.getMetaData()).thenReturn(metaData);
		when(metaData.getDatabaseProductName()).thenReturn(databaseProductName);
		when(connection.createStatement()).thenReturn(statement);
		when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
		when(statement.executeUpdate(anyString())).thenReturn(1);
		when(statement.executeQuery(anyString())).thenReturn(resultSet);
		when(preparedStatement.executeUpdate()).thenReturn(1);
		when(preparedStatement.executeQuery()).thenReturn(resultSet);

		DatabaseStore store = new DatabaseStore(dataSource, tableName);
		assertThat(store).isNotNull();
		return new TestFixture(store, connection, statement, preparedStatement, resultSet);
	}

	private record TestFixture(DatabaseStore store, Connection connection, Statement statement,
			PreparedStatement preparedStatement, ResultSet resultSet) {
	}

}

/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent.memory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLTransactionRollbackException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import com.alibaba.cloud.ai.graph.agent.memory.PreferenceMemory.Change;
import com.alibaba.cloud.ai.graph.agent.memory.PreferenceMemory.Key;
import com.alibaba.cloud.ai.graph.agent.memory.PreferenceMemory.Receipt;
import com.alibaba.cloud.ai.graph.agent.memory.PreferenceMemory.Snapshot;
import com.alibaba.cloud.ai.graph.agent.memory.PreferenceMemory.Status;
import com.alibaba.cloud.ai.graph.store.StoreItem;

import org.springframework.ai.util.JsonHelper;

final class JdbcPreferenceRepository {

	private static final JsonHelper JSON = new JsonHelper();

	private static final int SCHEMA_VERSION = 1;

	private static final int MAX_ATTEMPTS = 6;

	private final DataSource dataSource;

	JdbcPreferenceRepository(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	void initializeSchema() {
		try (var connection = this.dataSource.getConnection()) {
			connection.setAutoCommit(true);
			try (var statement = connection.createStatement()) {
				statement.executeUpdate("""
						CREATE TABLE IF NOT EXISTS saa_agent_preferences (
						    scope_id VARCHAR(64) NOT NULL,
						    preference_key VARCHAR(32) NOT NULL,
						    preference_value VARCHAR(64),
						    revision BIGINT NOT NULL,
						    created_at BIGINT NOT NULL,
						    updated_at BIGINT NOT NULL,
						    schema_version INTEGER NOT NULL,
						    write_source VARCHAR(24) NOT NULL,
						    PRIMARY KEY (scope_id, preference_key)
						)
						""");
				statement.executeUpdate("""
						CREATE TABLE IF NOT EXISTS saa_agent_preference_receipts (
						    scope_id VARCHAR(64) NOT NULL,
						    operation_id VARCHAR(128) NOT NULL,
						    request_hash VARCHAR(64) NOT NULL,
						    result_status VARCHAR(24) NOT NULL,
						    result_revision BIGINT NOT NULL,
						    PRIMARY KEY (scope_id, operation_id)
						)
						""");
			}
		}
		catch (SQLException failure) {
			throw new IllegalStateException("Could not initialize preference memory", failure);
		}
	}

	Snapshot read(List<String> namespace, Key key) {
		try (var connection = this.dataSource.getConnection()) {
			var row = selectPreference(connection, hash(namespace), key);
			if (row == null) {
				return new Snapshot(0, Optional.empty());
			}
			if (row.value() == null) {
				return new Snapshot(row.revision(), Optional.empty());
			}
			var item = new StoreItem(namespace, key.storeKey(), Map.of("value", row.value(), "version", row.revision(),
					"schemaVersion", row.schemaVersion(), "source", row.source()), row.createdAt(), row.updatedAt());
			return new Snapshot(row.revision(), Optional.of(item));
		}
		catch (SQLException failure) {
			throw new IllegalStateException("Could not read preference memory", failure);
		}
	}

	Receipt change(List<String> namespace, Change change, String source) {
		String scope = hash(namespace);
		String requestHash = hash(List.of(change.key().storeKey(), change.expectedVersion(),
				change.value() == null ? "" : change.value()));
		SQLException retryFailure = null;
		for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
			try (var connection = this.dataSource.getConnection()) {
				boolean previousAutoCommit = connection.getAutoCommit();
				int previousIsolation = connection.getTransactionIsolation();
				boolean committed = false;
				connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
				connection.setAutoCommit(false);
				try {
					Receipt result = change(connection, scope, change, requestHash, source);
					if (result != null) {
						connection.commit();
						committed = true;
						return result;
					}
					connection.rollback();
				}
				catch (SQLException failure) {
					try {
						connection.rollback();
					}
					catch (SQLException rollbackFailure) {
						failure.addSuppressed(rollbackFailure);
						throw failure;
					}
					if (!retryable(failure)) {
						throw failure;
					}
					retryFailure = failure;
				}
				finally {
					restoreConnectionState(connection, previousAutoCommit, previousIsolation, committed || retryFailure != null);
				}
			}
			catch (SQLException failure) {
				throw new IllegalStateException("Could not commit preference memory", failure);
			}
		}
		throw new IllegalStateException("Preference memory could not settle a concurrent operation", retryFailure);
	}

	private Receipt change(Connection connection, String scope, Change change, String requestHash, String source)
			throws SQLException {
		var prior = selectReceipt(connection, scope, change.operationId(), requestHash);
		if (prior != null) {
			return prior;
		}
		var previous = selectPreference(connection, scope, change.key());
		long currentVersion = previous == null ? 0 : previous.revision();
		Receipt receipt;
		if (currentVersion != change.expectedVersion()) {
			receipt = new Receipt(Status.VERSION_CONFLICT, currentVersion, false);
		}
		else {
			long newVersion = currentVersion + 1;
			if (!putPreference(connection, scope, change, newVersion, source)) {
				return null;
			}
			receipt = new Receipt(change.value() == null ? Status.DELETED : Status.SAVED, newVersion, false);
		}
		recordReceipt(connection, scope, change.operationId(), requestHash, receipt);
		return receipt;
	}

	private static PreferenceRow selectPreference(Connection connection, String scope, Key key) throws SQLException {
		try (var statement = connection.prepareStatement("""
				SELECT preference_value, revision, created_at, updated_at, schema_version, write_source
				FROM saa_agent_preferences WHERE scope_id = ? AND preference_key = ?
				""")) {
			setKey(statement, scope, key);
			try (var rows = statement.executeQuery()) {
				return rows.next() ? new PreferenceRow(rows.getString(1), rows.getLong(2), rows.getLong(3),
						rows.getLong(4), rows.getInt(5), rows.getString(6)) : null;
			}
		}
	}

	private static boolean putPreference(Connection connection, String scope, Change change, long revision, String source)
			throws SQLException {
		long timestamp = System.currentTimeMillis();
		if (change.expectedVersion() == 0) {
			try (var statement = connection.prepareStatement("""
					INSERT INTO saa_agent_preferences
					    (scope_id, preference_key, preference_value, revision, created_at, updated_at, schema_version, write_source)
					VALUES (?, ?, ?, ?, ?, ?, ?, ?)
					""")) {
				setKey(statement, scope, change.key());
				statement.setString(3, change.value());
				statement.setLong(4, revision);
				statement.setLong(5, timestamp);
				statement.setLong(6, timestamp);
				statement.setInt(7, SCHEMA_VERSION);
				statement.setString(8, source);
				return statement.executeUpdate() == 1;
			}
		}
		try (var statement = connection.prepareStatement("""
				UPDATE saa_agent_preferences
				SET preference_value = ?, revision = ?, updated_at = ?, schema_version = ?, write_source = ?
				WHERE scope_id = ? AND preference_key = ? AND revision = ?
				""")) {
			statement.setString(1, change.value());
			statement.setLong(2, revision);
			statement.setLong(3, timestamp);
			statement.setInt(4, SCHEMA_VERSION);
			statement.setString(5, source);
			statement.setString(6, scope);
			statement.setString(7, change.key().storeKey());
			statement.setLong(8, change.expectedVersion());
			return statement.executeUpdate() == 1;
		}
	}

	private static Receipt selectReceipt(Connection connection, String scope, String operationId, String requestHash)
			throws SQLException {
		try (var statement = connection.prepareStatement("""
				SELECT request_hash, result_status, result_revision
				FROM saa_agent_preference_receipts WHERE scope_id = ? AND operation_id = ?
				""")) {
			statement.setString(1, scope);
			statement.setString(2, operationId);
			try (var rows = statement.executeQuery()) {
				if (!rows.next()) {
					return null;
				}
				if (!requestHash.equals(rows.getString(1))) {
					return new Receipt(Status.OPERATION_CONFLICT, rows.getLong(3), false);
				}
				return new Receipt(Status.valueOf(rows.getString(2)), rows.getLong(3), true);
			}
		}
	}

	private static void recordReceipt(Connection connection, String scope, String operationId, String requestHash,
			Receipt receipt) throws SQLException {
		try (var statement = connection.prepareStatement("""
				INSERT INTO saa_agent_preference_receipts
				    (scope_id, operation_id, request_hash, result_status, result_revision)
				VALUES (?, ?, ?, ?, ?)
				""")) {
			statement.setString(1, scope);
			statement.setString(2, operationId);
			statement.setString(3, requestHash);
			statement.setString(4, receipt.status().name());
			statement.setLong(5, receipt.version());
			statement.executeUpdate();
		}
	}

	private static void setKey(PreparedStatement statement, String scope, Key key) throws SQLException {
		statement.setString(1, scope);
		statement.setString(2, key.storeKey());
	}

	private static void restoreConnectionState(Connection connection, boolean autoCommit, int isolation,
			boolean suppressErrors) throws SQLException {
		try {
			if (!connection.isClosed()) {
				connection.setAutoCommit(autoCommit);
				connection.setTransactionIsolation(isolation);
			}
		}
		catch (SQLException failure) {
			if (!suppressErrors) {
				throw failure;
			}
		}
	}

	private static boolean retryable(SQLException failure) {
		if (failure instanceof SQLTransactionRollbackException) {
			return true;
		}
		String state = failure.getSQLState();
		// Uniqueness races with the first write or a duplicate operation across PostgreSQL/H2 (23505)
		// and MySQL/MariaDB/Oracle/ANSI SQL (23000), plus serialization/deadlock states (40001/40P01).
		return "23505".equals(state) || "23000".equals(state)
				|| (failure instanceof SQLIntegrityConstraintViolationException && state == null)
				|| "40001".equals(state) || "40P01".equals(state);
	}

	private static String hash(List<?> values) {
		try {
			var digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(JSON.toJson(values).getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException failure) {
			throw new IllegalStateException("SHA-256 is unavailable", failure);
		}
	}

	private record PreferenceRow(String value, long revision, long createdAt, long updatedAt, int schemaVersion,
			String source) {
	}

}

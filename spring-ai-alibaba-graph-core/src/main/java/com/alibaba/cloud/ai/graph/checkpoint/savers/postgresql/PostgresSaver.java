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
package com.alibaba.cloud.ai.graph.checkpoint.savers.postgresql;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Base64;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.sql.DataSource;

import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class PostgresSaver extends MemorySaver {
	private static final Logger log = LoggerFactory.getLogger(PostgresSaver.class);
	/**
	 * Datasource used to create the store
	 */
	protected final DataSource datasource;

	private final StateSerializer stateSerializer;

	protected PostgresSaver(Builder builder) throws SQLException {
		this.datasource = builder.datasource;
		this.stateSerializer = builder.stateSerializer;
		initTable(builder.dropTablesFirst, builder.createTables);
	}

	public static Builder builder() {
		return new Builder();
	}

	private void rollback(Connection conn, Checkpoint checkpoint, String threadId) {
		if (conn == null) return;

		requireNonNull(checkpoint, "checkpoint cannot be null");

		try {
			conn.rollback();
			log.warn("Transaction rolled back for checkpoint {}", checkpoint.getId());
		}
		catch (SQLException exRollback) {
			log.error("Failed to rollback transaction for checkpoint id {} in thread {}",
					checkpoint.getId(),
					threadId,
					exRollback);
		}
	}

	private String encodeState(Map<String, Object> data) throws IOException {
		var binaryData = stateSerializer.dataToBytes(data);
		var base64Data = Base64.getEncoder().encodeToString(binaryData);
		return format("""
				{"binaryPayload": "%s"}
				""", base64Data);
	}

	private Map<String, Object> decodeState(byte[] binaryPayload, String contentType) throws IOException, ClassNotFoundException {
		if (!Objects.equals(contentType, stateSerializer.contentType())) {
			throw new IllegalStateException(
					format("Content Type used for store state '%s' is different from one '%s' used for deserialize it",
							contentType,
							stateSerializer.contentType()));
		}

		byte[] bytes = Base64.getDecoder().decode(binaryPayload);
		return stateSerializer.dataFromBytes(bytes);
	}

	protected void initTable(boolean dropTablesFirst, boolean createTables) throws SQLException {
		var sqlDropTables = """
				DROP TABLE IF EXISTS GraphCheckpoint CASCADE;
				DROP TABLE IF EXISTS GraphThread CASCADE;
				""";

		var sqlCreateTables = """
				CREATE TABLE IF NOT EXISTS GraphThread (
				     thread_id UUID PRIMARY KEY,
				     thread_name VARCHAR(255),
				     is_released BOOLEAN DEFAULT FALSE NOT NULL
				 );
				
				 CREATE TABLE IF NOT EXISTS GraphCheckpoint (
				     checkpoint_id UUID PRIMARY KEY,
				     parent_checkpoint_id UUID,
				     thread_id UUID NOT NULL,
				     node_id VARCHAR(255),
				     next_node_id VARCHAR(255),
				     state_data JSONB NOT NULL,
				     state_content_type VARCHAR(100) NOT NULL, -- New field for content type
				     saved_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
				
				     CONSTRAINT fk_thread
				         FOREIGN KEY(thread_id)
				         REFERENCES GraphThread(thread_id)
				         ON DELETE CASCADE
				 );
				
				 CREATE INDEX idx_lg4jcheckpoint_thread_id ON GraphCheckpoint(thread_id);
				 CREATE INDEX idx_lg4jcheckpoint_thread_id_saved_at_desc ON GraphCheckpoint(thread_id, saved_at DESC);
				 CREATE UNIQUE INDEX idx_unique_lg4jthread_thread_name_unreleased  ON GraphThread(thread_name) WHERE is_released = FALSE;
				""";


		String sqlCommand = null;
		try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
			if (dropTablesFirst) {
				log.trace("Executing drop tables:\n---\n{}---", sqlDropTables);
				sqlCommand = sqlDropTables;
				statement.executeUpdate(sqlCommand);
			}
			if (createTables) {
				log.trace("Executing create tables:\n---\n{}---", sqlCreateTables);
				sqlCommand = sqlCreateTables;
				statement.executeUpdate(sqlCommand);
			}
		}
		catch (SQLException ex) {
			log.error("error executing command\n{}\n", sqlCommand, ex);
			throw ex;
		}
	}

	@Override
	protected LinkedList<Checkpoint> loadedCheckpoints(RunnableConfig config, LinkedList<Checkpoint> checkpoints) throws Exception {

		if (!checkpoints.isEmpty()) return checkpoints;

		var threadId = config.threadId().orElse(THREAD_ID_DEFAULT);

		var sqlCheckThread = """
				SELECT COUNT(*)
				FROM GraphThread
				WHERE thread_name = ? AND is_released = FALSE
				""";
		var sqlQueryCheckpoints = """
				WITH matched_thread AS (
				    SELECT thread_id
				    FROM GraphThread
				    WHERE thread_name = ? AND is_released = FALSE
				)
				SELECT  c.checkpoint_id,
				        c.node_id,
				        c.next_node_id,
				        c.state_data->>'binaryPayload' AS base64_data,
				        c.state_content_type,
				        c.parent_checkpoint_id
				FROM matched_thread t
				JOIN GraphCheckpoint c ON c.thread_id = t.thread_id
				ORDER BY c.saved_at DESC
				""";
		try (Connection conn = getConnection()) {

			try (PreparedStatement ps = conn.prepareStatement(sqlCheckThread)) {
				ps.setString(1, threadId);
				var resultSet = ps.executeQuery();
				resultSet.next();
				var count = resultSet.getInt(1);

				if (count == 0) {
					return checkpoints;
				}
				if (count > 1) {
					throw new IllegalStateException(format("there are more than one Thread '%s' open (not released yet)", threadId));
				}
			}

			log.trace("Executing select checkpoints:\n---\n{}---", sqlQueryCheckpoints);
			try (PreparedStatement ps = conn.prepareStatement(sqlQueryCheckpoints)) {
				ps.setString(1, threadId);
				var rs = ps.executeQuery();
				while (rs.next()) {
					var checkpoint = Checkpoint.builder()
							.id(rs.getString(1))
							.nodeId(rs.getString(2))
							.nextNodeId(rs.getString(3))
							.state(decodeState(rs.getBytes(4), rs.getString(5)))
							.build();
					checkpoints.add(checkpoint);
				}
			}

		}

		return checkpoints;
	}

	private void insertCheckpoint(Connection conn, RunnableConfig config, LinkedList<Checkpoint> checkpoints, Checkpoint checkpoint) throws Exception {
		var threadId = config.threadId().orElse(THREAD_ID_DEFAULT);

		var upsertThreadSql = """
				WITH inserted AS (
				    INSERT INTO GraphThread (thread_id, thread_name, is_released)
				    VALUES (?, ?, FALSE)
				    ON CONFLICT (thread_name)
				    WHERE is_released = FALSE
				    DO NOTHING
				    RETURNING thread_id
				)
				SELECT thread_id FROM inserted
				UNION ALL
				SELECT thread_id FROM GraphThread
				WHERE thread_name = ? AND is_released = FALSE
				LIMIT 1;
				""";

		var insertCheckpointSql = """
				INSERT INTO GraphCheckpoint(
				checkpoint_id,
				parent_checkpoint_id,
				thread_id,
				node_id,
				next_node_id,
				state_data,
				state_content_type)
				VALUES (?, ?, ?, ?, ?, ?::jsonb, ?)
				""";
		UUID threadUUID = null;

		// 1. Upsert thread information
		try (PreparedStatement ps = conn.prepareStatement(upsertThreadSql)) {
			var field = 0;
			ps.setObject(++field, UUID.randomUUID(), Types.OTHER);
			ps.setString(++field, threadId);
			ps.setString(++field, threadId);

			log.trace("Executing upsert thread:\n---\n{}---", upsertThreadSql);

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					threadUUID = rs.getObject("thread_id", UUID.class);
				}
			}
		}


		// 2. Insert checkpoint data
		try (PreparedStatement ps = conn.prepareStatement(insertCheckpointSql)) {
			var field = 0;
			// checkpoint_id
			ps.setObject(++field,
					UUID.fromString(checkpoint.getId()),
					Types.OTHER);
			// parent_checkpoint_id
			ps.setNull(++field, Types.OTHER);
			// thread_id
			ps.setObject(++field,
					requireNonNull(threadUUID, "threadUUID cannot be null"),
					Types.OTHER);
			// node_id
			ps.setString(++field, checkpoint.getNodeId());
			// next_node_id
			ps.setString(++field, checkpoint.getNextNodeId());
			// state_data
			ps.setString(++field, encodeState(checkpoint.getState()));
			// state_content_type
			ps.setString(++field, stateSerializer.contentType());

			// DB schema has DEFAULT CURRENT_TIMESTAMP for saved_at.
			// If checkpoint provides a specific time, use it. Otherwise, use current time from Java.
			// To use DB default, one would typically omit the column or pass NULL if the column definition allows it to trigger default.
			// OffsetDateTime savedAt = checkpoint.getSavedAt().orElse(OffsetDateTime.now());
			// psCheckpoint.setObject(8, savedAt);
			log.trace("Executing insert checkpoint:\n---\n{}---", insertCheckpointSql);
			ps.executeUpdate();
		}

	}

	@Override
	protected void insertedCheckpoint(RunnableConfig config, LinkedList<Checkpoint> checkpoints, Checkpoint checkpoint) throws Exception {
		var threadId = config.threadId().orElse(THREAD_ID_DEFAULT);

		Connection conn = null;
		try (Connection ignored = conn = getConnection()) {
			conn.setAutoCommit(false); // Start transaction

			insertCheckpoint(conn, config, checkpoints, checkpoint);

			conn.commit();
			log.debug("Checkpoint {} for thread {} inserted successfully.", checkpoint.getId(), threadId);

		}
		catch (SQLException | IOException e) { // IOException from convertStateToJson
			log.error("Error inserting checkpoint with id {} in thread {}", checkpoint.getId(), threadId, e);
			rollback(conn, checkpoint, threadId);
			throw e;
		}

	}

	@Override
	protected void updatedCheckpoint(RunnableConfig config,
			LinkedList<Checkpoint> checkpoints,
			Checkpoint checkpoint) throws Exception {

		final var threadId = config.threadId().orElse(THREAD_ID_DEFAULT);

		var deletePreviousCheckpointSql = """
				DELETE FROM GraphCheckpoint
				WHERE checkpoint_id = ?;
				""";

		Connection conn = null;

		try (Connection ignored = conn = getConnection()) {
			conn.setAutoCommit(false); // Start transaction

			if (config.checkPointId().isPresent()) {

				try (PreparedStatement ps = conn.prepareStatement(deletePreviousCheckpointSql)) {
					var field = 0;
					ps.setObject(++field,
							UUID.fromString(config.checkPointId().get()),
							Types.OTHER); // nullable
					log.trace("Executing deleting previous checkpoint with id {} in thread {}:\n---\n{}---",
							config.checkPointId().get(),
							threadId,
							deletePreviousCheckpointSql);
					ps.executeUpdate();
				}
			}

			insertCheckpoint(conn, config, checkpoints, checkpoint);

			conn.commit();

			log.debug("Checkpoint with id {} for thread {} inserted successfully.",
					checkpoint.getId(),
					threadId);

		}
		catch (SQLException | IOException e) { // IOException from convertStateToJson
			log.error("Error inserting checkpoint with id {} in thread {}",
					checkpoint.getId(),
					threadId,
					e);
			rollback(conn, checkpoint, threadId);
			throw e;
		}
	}

	@Override
	protected void releasedCheckpoints(RunnableConfig config, LinkedList<Checkpoint> checkpoints, Tag releaseTag) throws Exception {
		var threadId = config.threadId().orElse(THREAD_ID_DEFAULT);

		var selectThreadSql = """
				SELECT thread_id FROM GraphThread
				WHERE thread_name = ? AND is_released = FALSE
				""";
		var releaseThreadSql = """
				UPDATE GraphThread
				SET
				    is_released = TRUE
				WHERE thread_id = ?;
				""";
		try (Connection conn = getConnection()) {

			UUID threadUUID = null;
			try (PreparedStatement ps = conn.prepareStatement(selectThreadSql)) {
				var field = 0;
				ps.setString(++field, threadId);

				try (ResultSet rs = ps.executeQuery()) {
					var rows = 0;
					while (rs.next()) {
						threadUUID = rs.getObject("thread_id", UUID.class);
						++rows;
					}
					if (rows == 0) {
						throw new IllegalStateException(format("active Thread '%s' not found", threadId));
					}
					if (rows > 1) {
						throw new IllegalStateException(format("duplicate active Thread '%s' found", threadId));
					}
				}
			}

			log.trace("Executing release Thread:\n---\n{}---", releaseThreadSql);
			try (PreparedStatement ps = conn.prepareStatement(releaseThreadSql)) {
				var field = 0;
				ps.setObject(++field,
						Objects.requireNonNull(threadUUID, "threadUUID cannot be null"),
						Types.OTHER); // nullable
				ps.executeUpdate();

			}
		}

	}

	/**
	 * Datasource connection
	 * Creates the vector extension and add the vector type if it does not exist.
	 * Could be overridden in case extension creation and adding type is done at datasource initialization step.
	 *
	 * @return Datasource connection
	 * @throws SQLException exception
	 */
	protected Connection getConnection() throws SQLException {
		return datasource.getConnection();
	}

	public static class Builder extends MemorySaver.Builder {
		public StateSerializer stateSerializer;
		private String host;
		private Integer port;
		private String user;
		private String password;
		private String database;
		private boolean createTables;
		private boolean dropTablesFirst;
		private DataSource datasource;

		public Builder stateSerializer(StateSerializer stateSerializer) {
			this.stateSerializer = stateSerializer;
			return this;
		}

		public Builder host(String host) {
			this.host = host;
			return this;
		}

		public Builder port(Integer port) {
			this.port = port;
			return this;
		}

		public Builder user(String user) {
			this.user = user;
			return this;
		}

		public Builder password(String password) {
			this.password = password;
			return this;
		}

		public Builder database(String database) {
			this.database = database;
			return this;
		}

		public Builder createTables(boolean createTables) {
			this.createTables = createTables;
			return this;
		}

		public Builder dropTablesFirst(boolean dropTablesFirst) {
			this.dropTablesFirst = dropTablesFirst;
			return this;
		}

		private String requireNotBlank(String value, String name) {
			if (requireNonNull(value, format("'%s' cannot be null", name)).isBlank()) {
				throw new IllegalArgumentException(format("'%s' cannot be blank", name));
			}
			return value;
		}

		public PostgresSaver build() {
			if (stateSerializer == null) {
				log.info("No StateSerializer for saver provided, using default SpringAiJacksonStateSerializer, please make sure saver uses the same serializer of the graph.");
				this.stateSerializer = StateGraph.DEFAULT_JACKSON_SERIALIZER;
			}

			if (port <= 0) {
				throw new IllegalArgumentException("port must be greater than 0");
			}
			var ds = new PGSimpleDataSource();
			ds.setDatabaseName(requireNotBlank(database, "database"));
			ds.setUser(requireNotBlank(user, "user"));
			ds.setPassword(requireNonNull(password, "password cannot be null"));
			ds.setPortNumbers(new int[] {port});
			ds.setServerNames(new String[] {requireNotBlank(host, "host")});

			datasource = ds;
			createTables = createTables || dropTablesFirst;

			try {
				return new PostgresSaver(this);
			}
			catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}
}


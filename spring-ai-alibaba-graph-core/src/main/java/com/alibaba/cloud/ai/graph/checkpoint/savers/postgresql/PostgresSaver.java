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

import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.savers.jdbc.AbstractJdbcCheckpointSaver;
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
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * <p>
 * PostgresSaver stores Graph state in a PostgreSQL database and keeps only a bounded
 * latest-checkpoint cache in memory.
 * </p>
 * <p>
 * Two tables are used to store the workflow state:
 *
 * <pre>
 *     CREATE TABLE GraphThread (
 *          thread_id UUID PRIMARY KEY,
 *          thread_name VARCHAR(255),
 *          is_released BOOLEAN DEFAULT FALSE NOT NULL
 *     )
 *     CREATE UNIQUE INDEX idx_unique_lg4jthread_thread_name_unreleased
 *          ON GraphThread(thread_name) WHERE is_released = FALSE
 *
 *     CREATE TABLE GraphCheckpoint (
 *          checkpoint_id UUID PRIMARY KEY,
 *          parent_checkpoint_id UUID,
 *          thread_id UUID NOT NULL,
 *          node_id VARCHAR(255),
 *          next_node_id VARCHAR(255),
 *          state_data JSONB NOT NULL,
 *          state_content_type VARCHAR(100) NOT NULL,
 *          saved_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
 *
 *          CONSTRAINT fk_thread
 *              FOREIGN KEY(thread_id)
 *              REFERENCES GraphThread(thread_id)
 *              ON DELETE CASCADE
 *     )
 * </pre>
 * </p>
 * <p>
 * A builder can be used to create an instance of PostgresSaver. The builder
 * allows to configure the following options:
 * - DataSource: indicates which data source should be used to connect
 * to the database
 * - CreateOption : indicates whether the tables should be created or
 * existing tables should be used.
 * - MaxCachedThreads: indicates how many latest checkpoints are kept in memory.
 * </p>
 * <p>
 * Ex:
 *
 * <pre>
 * var saver = PostgresSaver.builder()
 *         .stateSerializer(STATE_SERIALIZER)
 *         .createOption(CreateOption.CREATE_OR_REPLACE)
 *         .datasource(DATA_SOURCE)
 *         .build();
 * </pre>
 * </p>
 */
public class PostgresSaver extends AbstractJdbcCheckpointSaver {

	private static final Logger log = LoggerFactory.getLogger(PostgresSaver.class);

	// DDL statements
	private static final String DROP_TABLES = """
			DROP TABLE IF EXISTS GraphCheckpoint CASCADE;
			DROP TABLE IF EXISTS GraphThread CASCADE;
			""";

	private static final String CREATE_TABLES = """
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
			     state_content_type VARCHAR(100) NOT NULL,
			     saved_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

			     CONSTRAINT fk_thread
			         FOREIGN KEY(thread_id)
			         REFERENCES GraphThread(thread_id)
			         ON DELETE CASCADE
			 );
			""";

	private static final String CREATE_INDEXES = """
			CREATE INDEX IF NOT EXISTS idx_lg4jcheckpoint_thread_id ON GraphCheckpoint(thread_id);
			CREATE INDEX IF NOT EXISTS idx_lg4jcheckpoint_thread_id_saved_at_desc ON GraphCheckpoint(thread_id, saved_at DESC);
			CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_lg4jthread_thread_name_unreleased ON GraphThread(thread_name) WHERE is_released = FALSE;
			""";

	// DML statements
	private static final String UPSERT_THREAD = """
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

	private static final String INSERT_CHECKPOINT = """
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

	private static final String UPDATE_CHECKPOINT = """
			UPDATE GraphCheckpoint c
			SET
			  checkpoint_id = ?,
			  node_id = ?,
			  next_node_id = ?,
			  state_data = ?::jsonb,
			  state_content_type = ?
			FROM GraphThread t
			WHERE c.thread_id = t.thread_id
			  AND t.thread_name = ? AND t.is_released = FALSE
			  AND c.checkpoint_id = ?
			""";

	private static final String SELECT_CHECKPOINTS = """
			SELECT
			  c.checkpoint_id,
			  c.node_id,
			  c.next_node_id,
			  c.state_data->>'binaryPayload' AS base64_data,
			  c.state_content_type
			FROM GraphCheckpoint c
			  JOIN GraphThread t ON c.thread_id = t.thread_id
			WHERE t.thread_name = ? AND t.is_released = FALSE
			ORDER BY c.saved_at DESC
			""";

	private static final String SELECT_LATEST_CHECKPOINT = """
			SELECT
			  c.checkpoint_id,
			  c.node_id,
			  c.next_node_id,
			  c.state_data->>'binaryPayload' AS base64_data,
			  c.state_content_type
			FROM GraphCheckpoint c
			  JOIN GraphThread t ON c.thread_id = t.thread_id
			WHERE t.thread_name = ? AND t.is_released = FALSE
			ORDER BY c.saved_at DESC
			LIMIT 1
			""";

	private static final String SELECT_CHECKPOINT_BY_ID = """
			SELECT
			  c.checkpoint_id,
			  c.node_id,
			  c.next_node_id,
			  c.state_data->>'binaryPayload' AS base64_data,
			  c.state_content_type
			FROM GraphCheckpoint c
			  JOIN GraphThread t ON c.thread_id = t.thread_id
			WHERE t.thread_name = ? AND t.is_released = FALSE
			  AND c.checkpoint_id = ?
			""";

	private static final String RELEASE_THREAD = """
			UPDATE GraphThread
			SET is_released = TRUE
			WHERE thread_name = ? AND is_released = FALSE
			""";

	/**
	 * Datasource used to create the store
	 */
	protected final DataSource datasource;

	private final StateSerializer stateSerializer;

	private final CreateOption createOption;

	/**
	 * Private constructor used by the builder to create a new instance of
	 * PostgresSaver.
	 *
	 * @param builder the builder
	 */
	private PostgresSaver(Builder builder) throws SQLException {
		super(builder.maxCachedThreads);
		this.datasource = builder.datasource;
		this.stateSerializer = builder.stateSerializer;
		this.createOption = builder.createOption;
		initTable(createOption);
	}

	/**
	 * Creates an instance of a builder that allows to configure and create a new
	 * instance of PostgresSaver.
	 *
	 * @return a new instance of the builder.
	 */
	public static Builder builder() {
		return new Builder();
	}

	private void rollback(Connection conn, Checkpoint checkpoint, String threadId) {
		if (conn == null) {
			return;
		}

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

	private void rollback(Connection conn, String threadId) {
		if (conn == null) {
			return;
		}

		try {
			conn.rollback();
			log.warn("Transaction rolled back for thread {}", threadId);
		}
		catch (SQLException exRollback) {
			log.error("Failed to rollback transaction for thread {}", threadId, exRollback);
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

	protected void initTable(CreateOption createOption) throws SQLException {
		String sqlCommand = null;
		try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
			if (createOption == CreateOption.CREATE_OR_REPLACE) {
				log.trace("Executing drop tables:\n---\n{}---", DROP_TABLES);
				sqlCommand = DROP_TABLES;
				statement.executeUpdate(sqlCommand);
			}
			if (createOption == CreateOption.CREATE_OR_REPLACE ||
				createOption == CreateOption.CREATE_IF_NOT_EXISTS) {
				log.trace("Executing create tables:\n---\n{}---", CREATE_TABLES);
				sqlCommand = CREATE_TABLES;
				statement.executeUpdate(sqlCommand);

				log.trace("Executing create indexes:\n---\n{}---", CREATE_INDEXES);
				sqlCommand = CREATE_INDEXES;
				statement.executeUpdate(sqlCommand);
			}
		}
		catch (SQLException ex) {
			log.error("error executing command\n{}\n", sqlCommand, ex);
			throw ex;
		}
	}

	private Checkpoint readCheckpoint(ResultSet resultSet)
			throws SQLException, IOException, ClassNotFoundException {
		return Checkpoint.builder()
				.id(resultSet.getString(1))
				.nodeId(resultSet.getString(2))
				.nextNodeId(resultSet.getString(3))
				.state(decodeState(resultSet.getBytes(4), resultSet.getString(5)))
				.build();
	}

	@Override
	protected LinkedList<Checkpoint> selectCheckpoints(String threadId) throws Exception {
		LinkedList<Checkpoint> checkpoints = new LinkedList<>();
		try (Connection conn = getConnection();
				PreparedStatement ps = conn.prepareStatement(SELECT_CHECKPOINTS)) {

			log.trace("Executing select checkpoints:\n---\n{}---", SELECT_CHECKPOINTS);
			ps.setString(1, threadId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					checkpoints.add(readCheckpoint(rs));
				}
			}
		}
		catch (SQLException | IOException | ClassNotFoundException ex) {
			throw new Exception("Unable to load checkpoints", ex);
		}
		return checkpoints;
	}

	@Override
	protected Optional<Checkpoint> selectLatestCheckpoint(String threadId) throws Exception {
		try (Connection conn = getConnection();
				PreparedStatement ps = conn.prepareStatement(SELECT_LATEST_CHECKPOINT)) {

			log.trace("Executing select latest checkpoint:\n---\n{}---", SELECT_LATEST_CHECKPOINT);
			ps.setString(1, threadId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return Optional.of(readCheckpoint(rs));
				}
				return Optional.empty();
			}
		}
		catch (SQLException | IOException | ClassNotFoundException ex) {
			throw new Exception("Unable to load latest checkpoint", ex);
		}
	}

	@Override
	protected Optional<Checkpoint> selectCheckpointById(String threadId, String checkpointId) throws Exception {
		try (Connection conn = getConnection();
				PreparedStatement ps = conn.prepareStatement(SELECT_CHECKPOINT_BY_ID)) {

			log.trace("Executing select checkpoint by id:\n---\n{}---", SELECT_CHECKPOINT_BY_ID);
			ps.setString(1, threadId);
			ps.setObject(2, UUID.fromString(checkpointId), Types.OTHER);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return Optional.of(readCheckpoint(rs));
				}
				return Optional.empty();
			}
		}
		catch (SQLException | IOException | ClassNotFoundException ex) {
			throw new Exception("Unable to load checkpoint", ex);
		}
	}

	@Override
	protected void insertCheckpoint(String threadId, Checkpoint checkpoint) throws Exception {
		Connection conn = null;
		try (Connection ignored = conn = getConnection()) {
			conn.setAutoCommit(false);
			insertCheckpoint(conn, threadId, checkpoint);
			conn.commit();
			log.debug("Checkpoint {} for thread {} inserted successfully.", checkpoint.getId(), threadId);
		}
		catch (SQLException | IOException ex) {
			log.error("Error inserting checkpoint with id {} in thread {}", checkpoint.getId(), threadId, ex);
			rollback(conn, checkpoint, threadId);
			throw new Exception("Unable to insert checkpoint", ex);
		}
	}

	private void insertCheckpoint(Connection conn, String threadId, Checkpoint checkpoint) throws Exception {
		UUID threadUUID = null;

		// 1. Upsert thread information
		try (PreparedStatement ps = conn.prepareStatement(UPSERT_THREAD)) {
			var field = 0;
			ps.setObject(++field, UUID.randomUUID(), Types.OTHER);
			ps.setString(++field, threadId);
			ps.setString(++field, threadId);

			log.trace("Executing upsert thread:\n---\n{}---", UPSERT_THREAD);

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					threadUUID = rs.getObject("thread_id", UUID.class);
				}
			}
		}


		// 2. Insert checkpoint data
		try (PreparedStatement ps = conn.prepareStatement(INSERT_CHECKPOINT)) {
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
			log.trace("Executing insert checkpoint:\n---\n{}---", INSERT_CHECKPOINT);
			ps.executeUpdate();
		}

	}

	@Override
	protected void updateCheckpoint(String threadId, String checkpointId, Checkpoint checkpoint) throws Exception {
		Connection conn = null;
		try (Connection ignored = conn = getConnection()) {
			conn.setAutoCommit(false);
			try (PreparedStatement ps = conn.prepareStatement(UPDATE_CHECKPOINT)) {
				var field = 0;
				ps.setObject(++field, UUID.fromString(checkpoint.getId()), Types.OTHER);
				ps.setString(++field, checkpoint.getNodeId());
				ps.setString(++field, checkpoint.getNextNodeId());
				ps.setString(++field, encodeState(checkpoint.getState()));
				ps.setString(++field, stateSerializer.contentType());
				ps.setString(++field, threadId);
				ps.setObject(++field, UUID.fromString(checkpointId), Types.OTHER);
				log.trace("Executing update checkpoint:\n---\n{}---", UPDATE_CHECKPOINT);
				int rowsAffected = ps.executeUpdate();
				if (rowsAffected == 0) {
					conn.rollback();
					throw new NoSuchElementException(format("Checkpoint with id %s not found!", checkpointId));
				}
			}
			conn.commit();
			log.debug("Checkpoint with id {} for thread {} updated successfully.",
					checkpoint.getId(),
					threadId);
		}
		catch (SQLException | IOException ex) {
			log.error("Error updating checkpoint with id {} in thread {}", checkpoint.getId(), threadId, ex);
			rollback(conn, checkpoint, threadId);
			throw new Exception("Unable to update checkpoint", ex);
		}
	}

	@Override
	protected void releaseThread(String threadId) throws Exception {
		Connection conn = null;
		try (Connection ignored = conn = getConnection()) {
			conn.setAutoCommit(false);
			log.trace("Executing release Thread:\n---\n{}---", RELEASE_THREAD);
			try (PreparedStatement ps = conn.prepareStatement(RELEASE_THREAD)) {
				ps.setString(1, threadId);
				int rowsAffected = ps.executeUpdate();
				if (rowsAffected == 0) {
					conn.rollback();
					throw new IllegalStateException(format("Thread '%s' not found or already released", threadId));
				}
			}
			conn.commit();
			log.debug("Thread {} released successfully.", threadId);
		}
		catch (SQLException ex) {
			log.error("Error releasing thread {}", threadId, ex);
			rollback(conn, threadId);
			throw new Exception("Unable to release checkpoint", ex);
		}
	}

	/**
	 * Datasource connection
	 *
	 * @return Datasource connection
	 * @throws SQLException exception
	 */
	protected Connection getConnection() throws SQLException {
		return datasource.getConnection();
	}

	/**
	 * A builder for PostgresSaver.
	 */
	public static class Builder {
		public StateSerializer stateSerializer;
		private String host;
		private Integer port;
		private String user;
		private String password;
		private String database;
		private DataSource datasource;

		// New CreateOption field with default value
		private CreateOption createOption = CreateOption.CREATE_IF_NOT_EXISTS;

		private int maxCachedThreads = 1024;

		// Legacy fields for backward compatibility
		private boolean createTables;
		private boolean dropTablesFirst;

		/**
		 * Sets the maximum number of latest checkpoints retained in memory.
		 * @param maxCachedThreads max cached threads, or 0 to disable the cache
		 * @return this builder
		 */
		public Builder maxCachedThreads(int maxCachedThreads) {
			if (maxCachedThreads < 0) {
				throw new IllegalArgumentException("maxCachedThreads must be greater than or equal to 0");
			}
			this.maxCachedThreads = maxCachedThreads;
			return this;
		}

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

		/**
		 * Sets the create option for table initialization.
		 * @param createOption the create option (CREATE_NONE, CREATE_IF_NOT_EXISTS, CREATE_OR_REPLACE)
		 * @return this builder
		 */
		public Builder createOption(CreateOption createOption) {
			this.createOption = createOption;
			return this;
		}

		/**
		 * @deprecated Use {@link #createOption(CreateOption)} instead.
		 * Sets whether to create tables on initialization.
		 * @param createTables true to create tables
		 * @return this builder
		 */
		@Deprecated
		public Builder createTables(boolean createTables) {
			this.createTables = createTables;
			// Convert to CreateOption for backward compatibility
			if (!createTables) {
				this.createOption = CreateOption.CREATE_NONE;
			}
			return this;
		}

		/**
		 * Sets the DataSource directly. This is useful for testing or when you want to
		 * provide a pre-configured DataSource instead of building one from host/port/user/password.
		 * @param datasource the DataSource to use
		 * @return this builder
		 */
		public Builder datasource(DataSource datasource) {
			this.datasource = datasource;
			return this;
		}

		/**
		 * @deprecated Use {@link #createOption(CreateOption)} instead.
		 * Sets whether to drop tables before creating them.
		 * @param dropTablesFirst true to drop tables first
		 * @return this builder
		 */
		@Deprecated
		public Builder dropTablesFirst(boolean dropTablesFirst) {
			this.dropTablesFirst = dropTablesFirst;
			// Convert to CreateOption for backward compatibility
			if (dropTablesFirst && this.createOption != CreateOption.CREATE_NONE) {
				this.createOption = CreateOption.CREATE_OR_REPLACE;
			}
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

			// If datasource is already set (e.g., for testing), use it directly
			if (datasource == null) {
				if (port == null || port <= 0) {
					throw new IllegalArgumentException("port must be greater than 0");
				}
				var ds = new PGSimpleDataSource();
				ds.setDatabaseName(requireNotBlank(database, "database"));
				ds.setUser(requireNotBlank(user, "user"));
				ds.setPassword(requireNonNull(password, "password cannot be null"));
				ds.setPortNumbers(new int[] {port});
				ds.setServerNames(new String[] {requireNotBlank(host, "host")});
				datasource = ds;
			}

			try {
				return new PostgresSaver(this);
			}
			catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}
}

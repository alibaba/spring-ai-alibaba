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
package com.alibaba.cloud.ai.graph.checkpoint.savers.mysql;

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
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * <p>
 * MysqlSaver stores Graph state in a MySQL database and keeps only a bounded
 * latest-checkpoint cache in memory.
 * </p>
 * <p>
 * Two tables are used to store the workflow state:
 *
 * <pre>
 *     CREATE TABLE GRAPH_THREAD (
 *          thread_id VARCHAR(36) PRIMARY KEY,
 *          thread_name VARCHAR(255),
 *          is_released BOOLEAN DEFAULT FALSE NOT NULL
 *     )
 *     CREATE UNIQUE INDEX IDX_GRAPH_THREAD_NAME_RELEASED
 *          ON GRAPH_THREAD(thread_name, is_released)
 *
 *     CREATE TABLE GRAPH_CHECKPOINT (
 *          checkpoint_seq BIGINT NOT NULL AUTO_INCREMENT UNIQUE,
 *          checkpoint_id VARCHAR(36) PRIMARY KEY,
 *          thread_id VARCHAR(36) NOT NULL,
 *          node_id VARCHAR(255),
 *          next_node_id VARCHAR(255),
 *          state_data JSON NOT NULL,
 *          saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 *
 *          CONSTRAINT GRAPH_FK_THREAD
 *              FOREIGN KEY(thread_id)
 *              REFERENCES GRAPH_THREAD(thread_id)
 *              ON DELETE CASCADE
 *     )
 * </pre>
 * </p>
 * <p>
 * A builder can be used to create an instance of MysqlSaver. The builder
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
 * var saver = MysqlSaver.builder()
 *         .stateSerializer(STATE_SERIALIZER)
 *         .createOption(CreateOption.CREATE_OR_REPLACE)
 *         .dataSource(DATA_SOURCE)
 *         .build();
 * </pre>
 * </p>
 */
public class MysqlSaver extends AbstractJdbcCheckpointSaver {

	private static final Logger log = LoggerFactory.getLogger(MysqlSaver.class);

	// DDL statements
	private static final String CREATE_THREAD_TABLE = """
			CREATE TABLE IF NOT EXISTS GRAPH_THREAD (
			   thread_id VARCHAR(36) PRIMARY KEY,
			   thread_name VARCHAR(255),
			   is_released BOOLEAN DEFAULT FALSE NOT NULL
			)""";

	private static final String INDEX_THREAD_TABLE = """
			CREATE UNIQUE INDEX IDX_GRAPH_THREAD_NAME_RELEASED
			  ON GRAPH_THREAD(thread_name, is_released)
			""";

	private static final String CREATE_CHECKPOINT_TABLE = """
			CREATE TABLE IF NOT EXISTS GRAPH_CHECKPOINT (
			   checkpoint_seq BIGINT NOT NULL AUTO_INCREMENT UNIQUE,
			   checkpoint_id VARCHAR(36) PRIMARY KEY,
			   thread_id VARCHAR(36) NOT NULL,
			   node_id VARCHAR(255),
			   next_node_id VARCHAR(255),
			   state_data JSON NOT NULL,
			   saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
			
			   CONSTRAINT GRAPH_FK_THREAD
			       FOREIGN KEY(thread_id)
			       REFERENCES GRAPH_THREAD(thread_id)
			       ON DELETE CASCADE
			)""";

	private static final String DROP_CHECKPOINT_TABLE = "DROP TABLE IF EXISTS GRAPH_CHECKPOINT";
	private static final String DROP_THREAD_TABLE = "DROP TABLE IF EXISTS GRAPH_THREAD";

	private static final String INDEX_CHECKPOINT_THREAD_SEQUENCE = """
			CREATE INDEX IDX_GRAPH_CHECKPOINT_THREAD_SEQUENCE
			  ON GRAPH_CHECKPOINT(thread_id, checkpoint_seq)
			""";

	private static final String ADD_CHECKPOINT_SEQUENCE_COLUMN = """
			ALTER TABLE GRAPH_CHECKPOINT
			ADD COLUMN checkpoint_seq BIGINT NOT NULL AUTO_INCREMENT UNIQUE
			""";

	private static final String HAS_CHECKPOINT_SEQUENCE_COLUMN = """
			SHOW COLUMNS FROM GRAPH_CHECKPOINT LIKE 'checkpoint_seq'
			""";

	// DML statements
	private static final String UPSERT_THREAD = """
			INSERT INTO GRAPH_THREAD (thread_id, thread_name, is_released)
			VALUES (?, ?, FALSE)
			ON DUPLICATE KEY UPDATE thread_id = thread_id
			""";

	private static final String INSERT_CHECKPOINT = """
			INSERT INTO GRAPH_CHECKPOINT(checkpoint_id, thread_id, node_id, next_node_id, state_data)
			SELECT ?, thread_id, ?, ?, ?
			FROM GRAPH_THREAD
			WHERE thread_name = ? AND is_released = FALSE
			""";

	private static final String UPDATE_CHECKPOINT = """
			UPDATE GRAPH_CHECKPOINT c
			INNER JOIN GRAPH_THREAD t ON c.thread_id = t.thread_id
			SET
			  c.checkpoint_id = ?,
			  c.node_id = ?,
			  c.next_node_id = ?,
			  c.state_data = ?
			WHERE t.thread_name = ? AND t.is_released != TRUE
			  AND c.checkpoint_id = ?
			""";

	private static final String SELECT_CHECKPOINTS = """
			SELECT
			  c.checkpoint_id,
			  c.node_id,
			  c.next_node_id,
			  JSON_UNQUOTE(JSON_EXTRACT(c.state_data, '$.binaryPayload')) AS base64_data
			FROM GRAPH_CHECKPOINT c
			  INNER JOIN GRAPH_THREAD t ON c.thread_id = t.thread_id
			WHERE t.thread_name = ? AND t.is_released != TRUE
			ORDER BY c.checkpoint_seq DESC
			""";

	private static final String RELEASE_THREAD = """
			UPDATE GRAPH_THREAD SET is_released = TRUE WHERE thread_name = ? AND is_released = FALSE
			""";

	private static final String DELETE_CHECKPOINTS = """
			DELETE c FROM GRAPH_CHECKPOINT c
			INNER JOIN GRAPH_THREAD t ON c.thread_id = t.thread_id
			WHERE t.thread_name = ? AND t.is_released != TRUE
			  AND c.checkpoint_id IN (%s)
			""";

	private static final String SELECT_LATEST_CHECKPOINT = """
			SELECT
			  c.checkpoint_id,
			  c.node_id,
			  c.next_node_id,
			  JSON_UNQUOTE(JSON_EXTRACT(c.state_data, '$.binaryPayload')) AS base64_data
			FROM GRAPH_CHECKPOINT c
			  INNER JOIN GRAPH_THREAD t ON c.thread_id = t.thread_id
			WHERE t.thread_name = ? AND t.is_released != TRUE
			ORDER BY c.checkpoint_seq DESC
			LIMIT 1
			""";

	private static final String SELECT_CHECKPOINT_BY_ID = """
			SELECT
			  c.checkpoint_id,
			  c.node_id,
			  c.next_node_id,
			  JSON_UNQUOTE(JSON_EXTRACT(c.state_data, '$.binaryPayload')) AS base64_data
			FROM GRAPH_CHECKPOINT c
			  INNER JOIN GRAPH_THREAD t ON c.thread_id = t.thread_id
			WHERE t.thread_name = ? AND t.is_released != TRUE
			  AND c.checkpoint_id = ?
			""";

	// Configuration
	private final DataSource dataSource;
	private final CreateOption createOption;
	private final StateSerializer stateSerializer;

	/**
	 * Private constructor used by the builder to create a new instance of
	 * MysqlSaver.
	 *
	 * @param builder the builder
	 */
	private MysqlSaver(Builder builder) {
		super(builder.maxCachedThreads);
		this.dataSource = builder.dataSource;
		this.createOption = builder.createOption;
		this.stateSerializer = builder.stateSerializer;
		initTables();
	}

	/**
	 * Creates an instance of a builder that allows to configure and create a new
	 * instance of MysqlSaver.
	 *
	 * @return a new instance of the builder.
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Rolls back a transaction and logs the error.
	 *
	 * @param conn      the database connection
	 * @param checkpoint the checkpoint being processed
	 * @param threadId  the thread ID
	 */
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

	/**
	 * Rolls back a transaction and logs the error (for operations without checkpoint).
	 *
	 * @param conn     the database connection
	 * @param threadId the thread ID
	 */
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

	private Map<String, Object> decodeState(byte[] binaryPayload) throws IOException, ClassNotFoundException {
		byte[] bytes = Base64.getDecoder().decode(binaryPayload);
		return stateSerializer.dataFromBytes(bytes);
	}

	/**
	 * Initializes the database according the create options.
	 */
	protected void initTables() {
		try (Connection connection = dataSource.getConnection();
			 Statement statement = connection.createStatement()) {
			if (createOption == CreateOption.CREATE_OR_REPLACE) {
				// Drop tables (indexes are automatically dropped with tables in MySQL)
				statement.addBatch(DROP_CHECKPOINT_TABLE);
				statement.addBatch(DROP_THREAD_TABLE);
				statement.executeBatch();
			}
			if (createOption == CreateOption.CREATE_OR_REPLACE ||
					createOption == CreateOption.CREATE_IF_NOT_EXISTS) {
				statement.execute(CREATE_THREAD_TABLE);
				statement.execute(CREATE_CHECKPOINT_TABLE);
				ensureCheckpointSequenceColumn(connection);
				createIndexIfAbsent(statement, INDEX_THREAD_TABLE);
				createIndexIfAbsent(statement, INDEX_CHECKPOINT_THREAD_SEQUENCE);
			}
		}
		catch (SQLException sqlException) {
			throw new RuntimeException("Unable to create tables", sqlException);
		}
	}

	private void createIndexIfAbsent(Statement statement, String indexSql) throws SQLException {
		try {
			statement.execute(indexSql);
		}
		catch (SQLException e) {
			// Ignore "Duplicate key name" error (error code 1061)
			if (e.getErrorCode() != 1061) {
				throw e;
			}
		}
	}

	private void ensureCheckpointSequenceColumn(Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement();
			 ResultSet resultSet = statement.executeQuery(HAS_CHECKPOINT_SEQUENCE_COLUMN)) {
			if (resultSet.next()) {
				return;
			}
		}

		try (Statement statement = connection.createStatement()) {
			statement.execute(ADD_CHECKPOINT_SEQUENCE_COLUMN);
		}
	}

	private Checkpoint readCheckpoint(ResultSet resultSet)
			throws SQLException, IOException, ClassNotFoundException {
		return Checkpoint.builder()
				.id(resultSet.getString(1))
				.nodeId(resultSet.getString(2))
				.nextNodeId(resultSet.getString(3))
				.state(decodeState(resultSet.getBytes(4)))
				.build();
	}

	/**
	 * Loads full checkpoint history on demand without retaining it in cache.
	 */
	@Override
	protected LinkedList<Checkpoint> selectCheckpoints(String threadName) throws Exception {
		LinkedList<Checkpoint> checkpoints = new LinkedList<>();
		try (Connection connection = dataSource.getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(SELECT_CHECKPOINTS)) {

			preparedStatement.setString(1, threadName);
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					checkpoints.add(readCheckpoint(resultSet));
				}
			}
		}
		catch (SQLException | IOException | ClassNotFoundException ex) {
			throw new Exception("Unable to load checkpoints", ex);
		}
		return checkpoints;
	}

	@Override
	protected Optional<Checkpoint> selectLatestCheckpoint(String threadName) throws Exception {
		try (Connection connection = dataSource.getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(SELECT_LATEST_CHECKPOINT)) {

			preparedStatement.setString(1, threadName);
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					return Optional.of(readCheckpoint(resultSet));
				}
				return Optional.empty();
			}
		}
		catch (SQLException | IOException | ClassNotFoundException ex) {
			throw new Exception("Unable to load latest checkpoint", ex);
		}
	}

	@Override
	protected Optional<Checkpoint> selectCheckpointById(String threadName, String checkpointId) throws Exception {
		try (Connection connection = dataSource.getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(SELECT_CHECKPOINT_BY_ID)) {

			preparedStatement.setString(1, threadName);
			preparedStatement.setString(2, checkpointId);
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					return Optional.of(readCheckpoint(resultSet));
				}
				return Optional.empty();
			}
		}
		catch (SQLException | IOException | ClassNotFoundException ex) {
			throw new Exception("Unable to load checkpoint", ex);
		}
	}

	@Override
	protected void insertCheckpoint(String threadName, Checkpoint checkpoint) throws Exception {
		Connection conn = null;
		try (Connection ignored = conn = dataSource.getConnection()) {
			conn.setAutoCommit(false);

			try (PreparedStatement upsertStatement = conn.prepareStatement(UPSERT_THREAD);
					PreparedStatement insertCheckpointStatement = conn.prepareStatement(INSERT_CHECKPOINT)) {

				upsertStatement.setString(1, UUID.randomUUID().toString());
				upsertStatement.setString(2, threadName);
				upsertStatement.execute();

				insertCheckpointStatement.setString(1, checkpoint.getId());
				insertCheckpointStatement.setString(2, checkpoint.getNodeId());
				insertCheckpointStatement.setString(3, checkpoint.getNextNodeId());
				insertCheckpointStatement.setString(4, encodeState(checkpoint.getState()));
				insertCheckpointStatement.setString(5, threadName);
				insertCheckpointStatement.execute();
			}

			conn.commit();
			log.debug("Checkpoint {} for thread {} inserted successfully.", checkpoint.getId(), threadName);
		}
		catch (SQLException | IOException ex) {
			log.error("Error inserting checkpoint with id {} in thread {}", checkpoint.getId(), threadName, ex);
			rollback(conn, checkpoint, threadName);
			throw new Exception("Unable to insert checkpoint", ex);
		}
	}

	@Override
	protected void updateCheckpoint(String threadName, String checkpointId, Checkpoint checkpoint) throws Exception {
		Connection conn = null;
		try (Connection ignored = conn = dataSource.getConnection()) {
			conn.setAutoCommit(false);

			try (PreparedStatement preparedStatement = conn.prepareStatement(UPDATE_CHECKPOINT)) {
				preparedStatement.setString(1, checkpoint.getId());
				preparedStatement.setString(2, checkpoint.getNodeId());
				preparedStatement.setString(3, checkpoint.getNextNodeId());
				preparedStatement.setString(4, encodeState(checkpoint.getState()));
				preparedStatement.setString(5, threadName);
				preparedStatement.setString(6, checkpointId);
				int rowsAffected = preparedStatement.executeUpdate();
				if (rowsAffected == 0) {
					conn.rollback();
					throw new NoSuchElementException(format("Checkpoint with id %s not found!", checkpointId));
				}
			}

			conn.commit();
			log.debug("Checkpoint with id {} for thread {} updated successfully.", checkpoint.getId(), threadName);
		}
		catch (SQLException | IOException ex) {
			log.error("Error updating checkpoint with id {} in thread {}", checkpoint.getId(), threadName, ex);
			rollback(conn, checkpoint, threadName);
			throw new Exception("Unable to update checkpoint", ex);
		}
	}

	@Override
	protected void deleteCheckpoints(String threadName, Collection<String> checkpointIds) throws Exception {
		if (checkpointIds.isEmpty()) {
			return;
		}
		try (Connection connection = dataSource.getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(
						DELETE_CHECKPOINTS.formatted(String.join(", ", Collections.nCopies(checkpointIds.size(), "?"))))) {
			preparedStatement.setString(1, threadName);
			int index = 2;
			for (String checkpointId : checkpointIds) {
				preparedStatement.setString(index++, checkpointId);
			}
			preparedStatement.executeUpdate();
		}
		catch (SQLException ex) {
			throw new Exception("Unable to delete retained checkpoints", ex);
		}
	}

	@Override
	protected void releaseThread(String threadName) throws Exception {
		Connection conn = null;
		try (Connection ignored = conn = dataSource.getConnection()) {
			conn.setAutoCommit(false);

			try (PreparedStatement preparedStatement = conn.prepareStatement(RELEASE_THREAD)) {
				preparedStatement.setString(1, threadName);
				int rowsAffected = preparedStatement.executeUpdate();
				if (rowsAffected == 0) {
					conn.rollback();
					throw new IllegalStateException(format("Thread '%s' not found or already released", threadName));
				}
			}

			conn.commit();
			log.debug("Thread {} released successfully.", threadName);
		}
		catch (SQLException ex) {
			log.error("Error releasing thread {}", threadName, ex);
			rollback(conn, threadName);
			throw new Exception("Unable to release checkpoint", ex);
		}
	}

	/**
	 * A builder for MysqlSaver.
	 */
	public static class Builder {
		private DataSource dataSource;
		private CreateOption createOption = CreateOption.CREATE_IF_NOT_EXISTS;
		private StateSerializer stateSerializer;
		private int maxCachedThreads = 1024;

		/**
		 * Sets the maximum number of latest checkpoints retained in memory.
		 *
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

		/**
		 * Sets the state serializer
		 *
		 * @param stateSerializer the state serializer
		 * @return this builder
		 */
		public Builder stateSerializer(StateSerializer stateSerializer) {
			this.stateSerializer = stateSerializer;
			return this;
		}

		/**
		 * Sets the datasource
		 *
		 * @param dataSource the datasource
		 * @return this builder
		 */
		public Builder dataSource(DataSource dataSource) {
			this.dataSource = dataSource;
			return this;
		}

		/**
		 * Sets the create options (default {@link CreateOption#CREATE_IF_NOT_EXISTS}.
		 *
		 * @param createOption the create options
		 * @return this builder
		 */
		public Builder createOption(CreateOption createOption) {
			this.createOption = createOption;
			return this;
		}

		/**
		 * Creates a new instance of MysqlSaver
		 *
		 * @return the new instance of MysqlSaver.
		 */
		public MysqlSaver build() {
			if (stateSerializer == null) {
				this.stateSerializer = StateGraph.DEFAULT_JACKSON_SERIALIZER;
			}
			return new MysqlSaver(this);
		}
	}
}

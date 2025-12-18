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
package com.alibaba.cloud.ai.graph.checkpoint.savers.oracle;

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
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.sql.DataSource;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import oracle.jdbc.OracleStatement;
import oracle.jdbc.OracleType;
import oracle.jdbc.OracleTypes;
import oracle.jdbc.provider.oson.OsonFactory;
import oracle.sql.json.OracleJsonDatum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;


/**
 * <p>
 * OracleSaver is an extension of MemorySaver that enables persistent,
 * reliable storage of workflow state in an Oracle database.
 * </p>
 * <p>
 * Two tables are used to store the workflow state:
 *
 * <pre>
 *     CREATE TABLE GRAPH_THREAD (
 *          thread_id VARCHAR2(36) PRIMARY KEY,
 *          thread_name VARCHAR(255),
 *          is_released BOOLEAN DEFAULT FALSE NOT NULL
 *     )
 *     CREATE INDEX IDX_GRAPH_THREAD_NAME_RELEASED
 *          ON GRAPH_THREAD(thread_name, is_released)
 *
 *     CREATE TABLE GRAPH_CHECKPOINT (
 *          checkpoint_id VARCHAR2(36) PRIMARY KEY,
 *          thread_id VARCHAR2(36) NOT NULL,
 *          node_id VARCHAR(255),
 *          next_node_id VARCHAR(255),
 *          state_data JSON NOT NULL,
 *          state_content_type VARCHAR(100) NOT NULL,
 *          saved_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
 *
 *          CONSTRAINT GRAPH_FK_THREAD
 *              FOREIGN KEY(thread_id)
 *              REFERENCES GRAPH_THREAD(thread_id)
 *              ON DELETE CASCADE
 *     )
 * </pre>
 * </p>
 * <p>
 * A builder can be use to create an instance or OracleSaver. The builder
 * allows to configure the following options:
 * - DataSource: indicates which data source should be used to connect
 * to the database
 * - CreateOption : indicates whether the tables should be created or
 * existing tables should be used.
 * - StateSerializer: the serializer used to serialize/deserialize state data
 * </p>
 * <p>
 * Ex:
 *
 * <pre>
 * var saver = OracleSaver.builder()
 *         .createOption(CreateOption.CREATE_OR_REPLACE)
 *         .dataSource(DATA_SOURCE)
 *         .stateSerializer(STATE_SERIALIZER)
 *         .build();
 * </pre>
 * </p>
 */
public class OracleSaver extends MemorySaver {

	private static final Logger log = LoggerFactory.getLogger(OracleSaver.class);

	// DDL statements
	private static final String CREATE_THREAD_TABLE = """
			CREATE TABLE IF NOT EXISTS GRAPH_THREAD (
			   thread_id VARCHAR2(36) PRIMARY KEY,
			   thread_name VARCHAR(255),
			   is_released BOOLEAN DEFAULT FALSE NOT NULL
			)""";

	private static final String INDEX_THREAD_TABLE = """
			CREATE INDEX IF NOT EXISTS IDX_GRAPH_THREAD_NAME_RELEASED
			  ON GRAPH_THREAD(thread_name, is_released)
			""";

	private static final String CREATE_CHECKPOINT_TABLE = """
			CREATE TABLE IF NOT EXISTS GRAPH_CHECKPOINT (
			   checkpoint_id VARCHAR2(36) PRIMARY KEY,
			   thread_id VARCHAR2(36) NOT NULL,
			   node_id VARCHAR(255),
			   next_node_id VARCHAR(255),
			   state_data JSON NOT NULL,
			   state_content_type VARCHAR(100) NOT NULL,
			   saved_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
			
			   CONSTRAINT GRAPH_FK_THREAD
			       FOREIGN KEY(thread_id)
			       REFERENCES GRAPH_THREAD(thread_id)
			       ON DELETE CASCADE
			)""";
	private static final String DROP_THREAD_INDEX = "DROP INDEX IF EXISTS IDX_GRAPH_THREAD_NAME_RELEASED";
	private static final String DROP_THREAD_TABLE = "DROP TABLE IF EXISTS GRAPH_THREAD CASCADE CONSTRAINTS";
	private static final String DROP_CHECKPOINT_TABLE = "DROP TABLE IF EXISTS GRAPH_CHECKPOINT CASCADE CONSTRAINTS";

	// DML statements
	private static final String UPSERT_THREAD = """
			MERGE INTO GRAPH_THREAD existing
			USING (SELECT ? AS THREAD_ID, ? AS THREAD_NAME, FALSE AS IS_RELEASED FROM DUAL) new
			ON (existing.THREAD_NAME = new.THREAD_NAME AND existing.IS_RELEASED = new.IS_RELEASED)
			WHEN NOT MATCHED THEN INSERT (THREAD_ID, THREAD_NAME, IS_RELEASED)
			VALUES (new.THREAD_ID, new.THREAD_NAME, new.IS_RELEASED)
			""";

	private static final String INSERT_CHECKPOINT = """
			INSERT INTO GRAPH_CHECKPOINT(checkpoint_id, thread_id, node_id, next_node_id, state_data, state_content_type)
			SELECT ?, thread_id, ?, ?, ?, ?
			FROM GRAPH_THREAD
			WHERE THREAD_NAME = ? AND IS_RELEASED = FALSE
			""";

	private static final String UPDATE_CHECKPOINT = """
			UPDATE GRAPH_CHECKPOINT
			SET
			  checkpoint_id = ?,
			  node_id = ?,
			  next_node_id = ?,
			  state_data = ?,
			  state_content_type = ?
			WHERE checkpoint_id = ?
			""";

	private static final String SELECT_CHECKPOINTS = """
			SELECT
			  c.checkpoint_id,
			  c.node_id,
			  c.next_node_id,
			  c.state_data,
			  c.state_content_type
			FROM GRAPH_CHECKPOINT c
			  INNER JOIN GRAPH_THREAD t ON c.thread_id = t.thread_id
			WHERE t.thread_name = ? AND t.is_released != TRUE
			ORDER BY c.saved_at DESC
			""";

	private static final String DELETE_CHECKPOINTS = """
			    DELETE FROM GRAPH_CHECKPOINT WHERE checkpoint_id = ?
			""";

	private static final String RELEASE_THREAD = """
			UPDATE GRAPH_THREAD SET is_released = TRUE WHERE thread_name = ? AND is_released = FALSE
			""";

	// Configuration
	private final DataSource dataSource;
	private final CreateOption createOption;
	private final StateSerializer stateSerializer;

	/**
	 * Private constructor used by the builder to create a new instance of
	 * OracleSaver.
	 *
	 * @param dataSource      the data source
	 * @param createOption    the create options
	 * @param stateSerializer the state serializer
	 */
	private OracleSaver(DataSource dataSource, CreateOption createOption, StateSerializer stateSerializer) {
		this.dataSource = dataSource;
		this.createOption = createOption;
		this.stateSerializer = Objects.requireNonNull(stateSerializer, "stateSerializer cannot be null");
		initTables();
	}

	/**
	 * Creates an instance of a builder that allows to configure and create a new
	 * instance of OracleSaver.
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

		Objects.requireNonNull(checkpoint, "checkpoint cannot be null");

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

	/**
	 * Encodes state data to JSON string format for storage.
	 *
	 * @param data the state data to encode
	 * @return JSON string containing Base64-encoded binary payload
	 * @throws IOException if serialization fails
	 */
	private String encodeState(Map<String, Object> data) throws IOException {
		var binaryData = stateSerializer.dataToBytes(data);
		var base64Data = Base64.getEncoder().encodeToString(binaryData);
		return format("""
				{"binaryPayload": "%s"}
				""", base64Data);
	}

	/**
	 * Decodes state data from JSON string format.
	 *
	 * @param binaryPayload the Base64-encoded binary payload
	 * @param contentType   the content type of the stored state
	 * @return the decoded state data
	 * @throws IOException            if deserialization fails
	 * @throws ClassNotFoundException if class not found during deserialization
	 */
	private Map<String, Object> decodeState(byte[] binaryPayload, String contentType)
			throws IOException, ClassNotFoundException {
		if (!Objects.equals(contentType, stateSerializer.contentType())) {
			throw new IllegalStateException(
					format("Content Type used for store state '%s' is different from one '%s' used for deserialize it",
							contentType,
							stateSerializer.contentType()));
		}

		byte[] bytes = Base64.getDecoder().decode(binaryPayload);
		return stateSerializer.dataFromBytes(bytes);
	}

	/**
	 * If the list of checkpoints is empty, loads the checkpoints from the database.
	 *
	 * @param config      the configuration
	 * @param checkpoints the list of checkpoints
	 * @return a list of checkpoints
	 * @throws Exception if an error occurs while the checkpoints are being
	 *                   loaded from the database.
	 */
	@Override
	protected LinkedList<Checkpoint> loadedCheckpoints(RunnableConfig config, LinkedList<Checkpoint> checkpoints)
			throws Exception {
		if (!checkpoints.isEmpty()) {
			return checkpoints;
		}

		final String threadName = config.threadId().orElse(THREAD_ID_DEFAULT);
		JsonFactory osonFactory = new OsonFactory();
		ObjectMapper objectMapper = new ObjectMapper(osonFactory);

		try (Connection connection = dataSource.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement(SELECT_CHECKPOINTS)) {

			// Calls to defineColumnType reduce the number of network requests. When Oracle
			// JDBC knows that it is
			// fetching VECTOR, CLOB, and/or JSON columns, the first request it sends to the
			// database can include a LOB
			// prefetch size (VECTOR and JSON are value-based-lobs). If defineColumnType is
			// not called, then JDBC needs
			// to send an additional request with the LOB prefetch size, after the first
			// request has the database
			// respond with the column data types. To request all data, the prefetch size is
			// Integer.MAX_VALUE.
			OracleStatement oracleStatement = preparedStatement.unwrap(OracleStatement.class);
			oracleStatement.defineColumnType(1, OracleTypes.VARCHAR); // checkpoint_id
			oracleStatement.defineColumnType(2, OracleTypes.VARCHAR); // node_id
			oracleStatement.defineColumnType(3, OracleTypes.VARCHAR); // next_node_id
			oracleStatement.defineColumnType(4, OracleTypes.JSON, Integer.MAX_VALUE); // state_data
			oracleStatement.defineColumnType(5, OracleTypes.VARCHAR); // state_content_type
			oracleStatement.setLobPrefetchSize(Integer.MAX_VALUE); // Workaround for Oracle JDBC bug 37030121

			preparedStatement.setString(1, threadName);
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					byte[] osonBytes = resultSet.getObject(4, OracleJsonDatum.class).shareBytes();
					String contentType = resultSet.getString(5);

					// Parse JSON to extract binaryPayload
					Map<String, Object> jsonMap = objectMapper.readValue(osonBytes, Map.class);
					String base64Data = (String) jsonMap.get("binaryPayload");
					// Convert base64 string to bytes (don't decode yet, decodeState will do that)
					byte[] binaryPayload = base64Data.getBytes(StandardCharsets.UTF_8);

					Checkpoint checkpoint = Checkpoint.builder()
							.id(resultSet.getString(1))
							.nodeId(resultSet.getString(2))
							.nextNodeId(resultSet.getString(3))
							.state(decodeState(binaryPayload, contentType))
							.build();
					checkpoints.add(checkpoint);
				}
			}
		}
		catch (SQLException sqlException) {
			throw new Exception("Unable to load checkpoints", sqlException);
		}
		catch (ClassNotFoundException e) {
			throw new Exception("Unable to deserialize checkpoint state", e);
		}
		return checkpoints;
	}

	/**
	 * Inserts a checkpoint to the database
	 *
	 * @param config      the configuration
	 * @param checkpoints the list of checkpoints
	 * @param checkpoint  the checkpoint to insert
	 * @throws Exception if an error occurs while inserting the checkpoint in the
	 *                   database.
	 */
	@Override
	protected void insertedCheckpoint(RunnableConfig config, LinkedList<Checkpoint> checkpoints, Checkpoint checkpoint)
			throws Exception {

		final String threadName = config.threadId().orElse(THREAD_ID_DEFAULT);
		Connection conn = null;

		try (Connection ignored = conn = dataSource.getConnection()) {
			conn.setAutoCommit(false); // Start transaction

			try (PreparedStatement upsertStatement = conn.prepareStatement(UPSERT_THREAD);
				 PreparedStatement insertCheckpointStatement = conn.prepareStatement(INSERT_CHECKPOINT)) {

				upsertStatement.setString(1, UUID.randomUUID().toString());
				upsertStatement.setString(2, threadName);
				upsertStatement.execute();

				String encodedState = encodeState(checkpoint.getState());
				insertCheckpointStatement.setString(1, checkpoint.getId());
				insertCheckpointStatement.setString(2, checkpoint.getNodeId());
				insertCheckpointStatement.setString(3, checkpoint.getNextNodeId());
				insertCheckpointStatement.setObject(4, encodedState, OracleType.JSON);
				insertCheckpointStatement.setString(5, stateSerializer.contentType());
				insertCheckpointStatement.setString(6, threadName);

				insertCheckpointStatement.execute();
			}

			conn.commit();
			log.debug("Checkpoint {} for thread {} inserted successfully.", checkpoint.getId(), threadName);
		}
		catch (SQLException | IOException e) {
			log.error("Error inserting checkpoint with id {} in thread {}", checkpoint.getId(), threadName, e);
			rollback(conn, checkpoint, threadName);
			throw new Exception("Unable to insert checkpoint", e);
		}

	}

	/**
	 * Marks the checkpoints as released
	 *
	 * @param config      the configuration
	 * @param checkpoints the checkpoints
	 * @param releaseTag  the release tab
	 * @throws Exception if an error occurs while marking the checkpoints as
	 *                   released
	 */
	@Override
	protected void releasedCheckpoints(RunnableConfig config, LinkedList<Checkpoint> checkpoints, Tag releaseTag)
			throws Exception {
		final String threadName = config.threadId().orElse(THREAD_ID_DEFAULT);
		Connection conn = null;

		try (Connection ignored = conn = dataSource.getConnection()) {
			conn.setAutoCommit(false); // Start transaction

			try (PreparedStatement preparedStatement = conn.prepareStatement(RELEASE_THREAD)) {
				preparedStatement.setString(1, threadName);
				int rowsAffected = preparedStatement.executeUpdate();

				if (rowsAffected == 0) {
					conn.rollback();
					throw new IllegalStateException(
							format("Thread '%s' not found or already released", threadName));
				}
			}

			conn.commit();
			log.debug("Thread {} released successfully.", threadName);
		}
		catch (SQLException e) {
			log.error("Error releasing thread {}", threadName, e);
			rollback(conn, threadName);
			throw new Exception("Unable to release checkpoint", e);
		}
	}

	/**
	 * If the checkpoint exists, updates the checkpoint, otherwise it inserts it.
	 *
	 * @param config      the configuration
	 * @param checkpoints the list of checkpoints
	 * @param checkpoint  the checkpoint
	 * @throws Exception if an error occurs while inserting or updating the
	 *                   checkpoint.
	 */
	@Override
	protected void updatedCheckpoint(RunnableConfig config, LinkedList<Checkpoint> checkpoints, Checkpoint checkpoint)
			throws Exception {
		final String threadName = config.threadId().orElse(THREAD_ID_DEFAULT);
		Connection conn = null;

		try (Connection ignored = conn = dataSource.getConnection()) {
			conn.setAutoCommit(false); // Start transaction

			if (config.checkPointId().isPresent()) {
				// Update existing checkpoint
				try (PreparedStatement preparedStatement = conn.prepareStatement(UPDATE_CHECKPOINT)) {
					String encodedState = encodeState(checkpoint.getState());
					preparedStatement.setString(1, checkpoint.getId());
					preparedStatement.setString(2, checkpoint.getNodeId());
					preparedStatement.setString(3, checkpoint.getNextNodeId());
					preparedStatement.setObject(4, encodedState, OracleType.JSON);
					preparedStatement.setString(5, stateSerializer.contentType());
					preparedStatement.setString(6, config.checkPointId().get());
					preparedStatement.execute();
				}
			}
			else {
				// Insert new checkpoint (within same transaction)
				try (PreparedStatement upsertStatement = conn.prepareStatement(UPSERT_THREAD);
					 PreparedStatement insertCheckpointStatement = conn.prepareStatement(INSERT_CHECKPOINT)) {

					upsertStatement.setString(1, UUID.randomUUID().toString());
					upsertStatement.setString(2, threadName);
					upsertStatement.execute();

					String encodedState = encodeState(checkpoint.getState());
					insertCheckpointStatement.setString(1, checkpoint.getId());
					insertCheckpointStatement.setString(2, checkpoint.getNodeId());
					insertCheckpointStatement.setString(3, checkpoint.getNextNodeId());
					insertCheckpointStatement.setObject(4, encodedState, OracleType.JSON);
					insertCheckpointStatement.setString(5, stateSerializer.contentType());
					insertCheckpointStatement.setString(6, threadName);

					insertCheckpointStatement.execute();
				}
			}

			conn.commit();
			log.debug("Checkpoint with id {} for thread {} updated successfully.", checkpoint.getId(), threadName);
		}
		catch (SQLException | IOException e) {
			log.error("Error updating checkpoint with id {} in thread {}", checkpoint.getId(), threadName, e);
			rollback(conn, checkpoint, threadName);
			throw new Exception("Unable to update checkpoint", e);
		}
	}

	/**
	 * Initializes the database according the create options.
	 */
	protected void initTables() {
		try (Connection connection = dataSource.getConnection();
			 Statement statement = connection.createStatement()) {
			if (createOption == CreateOption.CREATE_OR_REPLACE) {
				statement.addBatch(DROP_THREAD_INDEX);
				statement.addBatch(DROP_CHECKPOINT_TABLE);
				statement.addBatch(DROP_THREAD_TABLE);
			}
			if (createOption == CreateOption.CREATE_OR_REPLACE ||
					createOption == CreateOption.CREATE_IF_NOT_EXISTS) {
				statement.addBatch(CREATE_THREAD_TABLE);
				statement.addBatch(INDEX_THREAD_TABLE);
				statement.addBatch(CREATE_CHECKPOINT_TABLE);
				statement.executeBatch();
			}
		}
		catch (SQLException sqlException) {
			throw new RuntimeException("Unable to create tables", sqlException);
		}
	}

	/**
	 * A builder for OracleSaver.
	 */
	public static class Builder extends MemorySaver.Builder {
		private DataSource dataSource;
		private CreateOption createOption = CreateOption.CREATE_IF_NOT_EXISTS;
		private StateSerializer stateSerializer;

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
		 * Creates a new instance of OracleSaver
		 *
		 * @return the new instance of OracleSaver.
		 */
		public OracleSaver build() {
			Objects.requireNonNull(dataSource, "dataSource cannot be null");
            if (stateSerializer == null) {
                this.stateSerializer = StateGraph.DEFAULT_JACKSON_SERIALIZER;
            }
            return new OracleSaver(dataSource, createOption, stateSerializer);
		}
	}
}

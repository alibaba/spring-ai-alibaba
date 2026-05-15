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
package com.alibaba.cloud.ai.graph.checkpoint.savers.oracle;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

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
 * OracleSaver stores Graph state in an Oracle database and keeps only a bounded
 * latest-checkpoint cache in memory.
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
 * - MaxCachedThreads: indicates how many latest checkpoints are kept in memory.
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
public class OracleSaver implements BaseCheckpointSaver {

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
			UPDATE GRAPH_CHECKPOINT c
			SET
			  checkpoint_id = ?,
			  node_id = ?,
			  next_node_id = ?,
			  state_data = ?,
			  state_content_type = ?
			WHERE c.checkpoint_id = ?
			  AND EXISTS (
			    SELECT 1 FROM GRAPH_THREAD t
			    WHERE t.thread_id = c.thread_id
			      AND t.thread_name = ?
			      AND t.is_released != TRUE
			  )
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

	private static final String SELECT_LATEST_CHECKPOINT = """
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
			FETCH FIRST 1 ROW ONLY
			""";

	private static final String SELECT_CHECKPOINT_BY_ID = """
			SELECT
			  c.checkpoint_id,
			  c.node_id,
			  c.next_node_id,
			  c.state_data,
			  c.state_content_type
			FROM GRAPH_CHECKPOINT c
			  INNER JOIN GRAPH_THREAD t ON c.thread_id = t.thread_id
			WHERE t.thread_name = ? AND t.is_released != TRUE
			  AND c.checkpoint_id = ?
			""";

	private static final String RELEASE_THREAD = """
			UPDATE GRAPH_THREAD SET is_released = TRUE WHERE thread_name = ? AND is_released = FALSE
			""";

	// Configuration
	private final DataSource dataSource;
	private final CreateOption createOption;
	private final StateSerializer stateSerializer;
	private final Map<String, Checkpoint> latestCheckpointCache;
	private final ReentrantLock lock = new ReentrantLock();
	private final int maxCachedThreads;

	/**
	 * Private constructor used by the builder to create a new instance of
	 * OracleSaver.
	 *
	 * @param builder the builder
	 */
	private OracleSaver(Builder builder) {
		this.dataSource = builder.dataSource;
		this.createOption = builder.createOption;
		this.stateSerializer = Objects.requireNonNull(builder.stateSerializer, "stateSerializer cannot be null");
		this.maxCachedThreads = builder.maxCachedThreads;
		this.latestCheckpointCache = createLatestCheckpointCache(builder.maxCachedThreads);
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

	private ObjectMapper osonObjectMapper() {
		JsonFactory osonFactory = new OsonFactory();
		return new ObjectMapper(osonFactory);
	}

	private void defineCheckpointColumns(PreparedStatement preparedStatement) throws SQLException {
		// Defining JSON columns up front avoids an additional Oracle JDBC metadata round trip.
		OracleStatement oracleStatement = preparedStatement.unwrap(OracleStatement.class);
		oracleStatement.defineColumnType(1, OracleTypes.VARCHAR);
		oracleStatement.defineColumnType(2, OracleTypes.VARCHAR);
		oracleStatement.defineColumnType(3, OracleTypes.VARCHAR);
		oracleStatement.defineColumnType(4, OracleTypes.JSON, Integer.MAX_VALUE);
		oracleStatement.defineColumnType(5, OracleTypes.VARCHAR);
		oracleStatement.setLobPrefetchSize(Integer.MAX_VALUE);
	}

	private Checkpoint readCheckpoint(ResultSet resultSet, ObjectMapper objectMapper)
			throws SQLException, IOException, ClassNotFoundException {
		byte[] osonBytes = resultSet.getObject(4, OracleJsonDatum.class).shareBytes();
		Map<String, Object> jsonMap = objectMapper.readValue(osonBytes, Map.class);
		String base64Data = (String) jsonMap.get("binaryPayload");
		byte[] binaryPayload = base64Data.getBytes(StandardCharsets.UTF_8);

		return Checkpoint.builder()
				.id(resultSet.getString(1))
				.nodeId(resultSet.getString(2))
				.nextNodeId(resultSet.getString(3))
				.state(decodeState(binaryPayload, resultSet.getString(5)))
				.build();
	}

	/**
	 * Loads full checkpoint history on demand without retaining it in cache.
	 */
	private LinkedList<Checkpoint> selectCheckpoints(String threadName) throws Exception {
		LinkedList<Checkpoint> checkpoints = new LinkedList<>();
		ObjectMapper objectMapper = osonObjectMapper();
		try (Connection connection = dataSource.getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(SELECT_CHECKPOINTS)) {

			defineCheckpointColumns(preparedStatement);
			preparedStatement.setString(1, threadName);
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					checkpoints.add(readCheckpoint(resultSet, objectMapper));
				}
			}
		}
		catch (SQLException | IOException | ClassNotFoundException ex) {
			throw new Exception("Unable to load checkpoints", ex);
		}
		return checkpoints;
	}

	private Optional<Checkpoint> selectLatestCheckpoint(String threadName) throws Exception {
		ObjectMapper objectMapper = osonObjectMapper();
		try (Connection connection = dataSource.getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(SELECT_LATEST_CHECKPOINT)) {

			defineCheckpointColumns(preparedStatement);
			preparedStatement.setString(1, threadName);
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					return Optional.of(readCheckpoint(resultSet, objectMapper));
				}
				return Optional.empty();
			}
		}
		catch (SQLException | IOException | ClassNotFoundException ex) {
			throw new Exception("Unable to load latest checkpoint", ex);
		}
	}

	private Optional<Checkpoint> selectCheckpointById(String threadName, String checkpointId) throws Exception {
		ObjectMapper objectMapper = osonObjectMapper();
		try (Connection connection = dataSource.getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(SELECT_CHECKPOINT_BY_ID)) {

			defineCheckpointColumns(preparedStatement);
			preparedStatement.setString(1, threadName);
			preparedStatement.setString(2, checkpointId);
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					return Optional.of(readCheckpoint(resultSet, objectMapper));
				}
				return Optional.empty();
			}
		}
		catch (SQLException | IOException | ClassNotFoundException ex) {
			throw new Exception("Unable to load checkpoint", ex);
		}
	}

	private void insertCheckpoint(String threadName, Checkpoint checkpoint) throws Exception {
		Connection conn = null;

		try (Connection ignored = conn = dataSource.getConnection()) {
			conn.setAutoCommit(false);

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
		catch (SQLException | IOException ex) {
			log.error("Error inserting checkpoint with id {} in thread {}", checkpoint.getId(), threadName, ex);
			rollback(conn, checkpoint, threadName);
			throw new Exception("Unable to insert checkpoint", ex);
		}
	}

	private void updateCheckpoint(String threadName, String checkpointId, Checkpoint checkpoint) throws Exception {
		Connection conn = null;

		try (Connection ignored = conn = dataSource.getConnection()) {
			conn.setAutoCommit(false);

			try (PreparedStatement preparedStatement = conn.prepareStatement(UPDATE_CHECKPOINT)) {
				String encodedState = encodeState(checkpoint.getState());
				preparedStatement.setString(1, checkpoint.getId());
				preparedStatement.setString(2, checkpoint.getNodeId());
				preparedStatement.setString(3, checkpoint.getNextNodeId());
				preparedStatement.setObject(4, encodedState, OracleType.JSON);
				preparedStatement.setString(5, stateSerializer.contentType());
				preparedStatement.setString(6, checkpointId);
				preparedStatement.setString(7, threadName);
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

	private void releaseThread(String threadName) throws Exception {
		Connection conn = null;

		try (Connection ignored = conn = dataSource.getConnection()) {
			conn.setAutoCommit(false);

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
		catch (SQLException ex) {
			log.error("Error releasing thread {}", threadName, ex);
			rollback(conn, threadName);
			throw new Exception("Unable to release checkpoint", ex);
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
	 * Lists active checkpoints for the configured thread.
	 */
	@Override
	public Collection<Checkpoint> list(RunnableConfig config) {
		lock.lock();
		try {
			String threadName = config.threadId().orElse(THREAD_ID_DEFAULT);
			LinkedList<Checkpoint> checkpoints = selectCheckpoints(threadName);
			if (!checkpoints.isEmpty()) {
				cacheLatest(threadName, checkpoints.peek());
			}
			return Collections.unmodifiableCollection(checkpoints);
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Gets a checkpoint for the configured thread.
	 */
	@Override
	public Optional<Checkpoint> get(RunnableConfig config) {
		lock.lock();
		try {
			String threadName = config.threadId().orElse(THREAD_ID_DEFAULT);
			if (config.checkPointId().isPresent()) {
				return selectCheckpointById(threadName, config.checkPointId().get());
			}

			Optional<Checkpoint> cached = getCachedLatest(threadName);
			if (cached.isPresent()) {
				return cached;
			}

			Optional<Checkpoint> latest = selectLatestCheckpoint(threadName);
			latest.ifPresent(checkpoint -> cacheLatest(threadName, checkpoint));
			return latest;
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Inserts or updates a checkpoint.
	 */
	@Override
	public RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception {
		lock.lock();
		try {
			String threadName = config.threadId().orElse(THREAD_ID_DEFAULT);
			if (config.checkPointId().isPresent()) {
				String checkpointId = config.checkPointId().get();
				updateCheckpoint(threadName, checkpointId, checkpoint);
				getCachedLatest(threadName)
						.filter(latest -> latest.getId().equals(checkpointId))
						.ifPresent(latest -> cacheLatest(threadName, checkpoint));
				return config;
			}

			insertCheckpoint(threadName, checkpoint);
			cacheLatest(threadName, checkpoint);
			return RunnableConfig.builder(config)
					.checkPointId(checkpoint.getId())
					.build();
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Releases the active thread and returns the released checkpoints.
	 */
	@Override
	public Tag release(RunnableConfig config) throws Exception {
		lock.lock();
		try {
			String threadName = config.threadId().orElse(THREAD_ID_DEFAULT);
			LinkedList<Checkpoint> checkpoints = selectCheckpoints(threadName);
			releaseThread(threadName);
			removeCachedLatest(threadName);
			return new Tag(threadName, checkpoints);
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Creates a bounded LRU cache for latest checkpoints.
	 */
	private static Map<String, Checkpoint> createLatestCheckpointCache(int maxCachedThreads) {
		if (maxCachedThreads == 0) {
			return Collections.emptyMap();
		}
		return new LinkedHashMap<>(16, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<String, Checkpoint> eldest) {
				return size() > maxCachedThreads;
			}
		};
	}

	private Optional<Checkpoint> getCachedLatest(String threadName) {
		if (maxCachedThreads == 0) {
			return Optional.empty();
		}
		return Optional.ofNullable(latestCheckpointCache.get(threadName));
	}

	private void cacheLatest(String threadName, Checkpoint checkpoint) {
		if (maxCachedThreads > 0) {
			latestCheckpointCache.put(threadName, checkpoint);
		}
	}

	private void removeCachedLatest(String threadName) {
		if (maxCachedThreads > 0) {
			latestCheckpointCache.remove(threadName);
		}
	}

	/**
	 * A builder for OracleSaver.
	 */
	public static class Builder {
		private DataSource dataSource;
		private CreateOption createOption = CreateOption.CREATE_IF_NOT_EXISTS;
		private StateSerializer stateSerializer;
		private int maxCachedThreads = 1024;

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
		 * Creates a new instance of OracleSaver
		 *
		 * @return the new instance of OracleSaver.
		 */
		public OracleSaver build() {
			Objects.requireNonNull(dataSource, "dataSource cannot be null");
			if (stateSerializer == null) {
				this.stateSerializer = StateGraph.DEFAULT_JACKSON_SERIALIZER;
			}
			return new OracleSaver(this);
		}
	}
}

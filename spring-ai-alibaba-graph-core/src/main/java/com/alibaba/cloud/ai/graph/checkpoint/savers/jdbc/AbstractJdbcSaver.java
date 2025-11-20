/*
 * Copyright 2025-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.checkpoint.savers.jdbc;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

import static java.lang.String.format;

/**
 * Abstract base class for JDBC-based checkpoint savers.
 * such as: PGSQL, MySQL, H2 and etc.
 *
 * @author yuluo-yx
 * @since 1.1.0.0-M4
 */
public abstract class AbstractJdbcSaver implements BaseCheckpointSaver {

	private static final Logger logger = LoggerFactory.getLogger(AbstractJdbcSaver.class);

	protected final DataSource dataSource;

	protected final ObjectMapper objectMapper;

	protected final String tableName;

	/**
	 * Constructs an AbstractJdbcSaver with default table name.
	 * 
	 * @param dataSource the JDBC DataSource to use for database connections
	 */
	protected AbstractJdbcSaver(DataSource dataSource) {
		this(dataSource, new ObjectMapper(), "checkpoint_store");
	}

	/**
	 * Constructs an AbstractJdbcSaver with custom ObjectMapper.
	 * 
	 * @param dataSource   the JDBC DataSource to use for database connections
	 * @param objectMapper the ObjectMapper for JSON serialization
	 */
	protected AbstractJdbcSaver(DataSource dataSource, ObjectMapper objectMapper) {
		this(dataSource, objectMapper, "checkpoint_store");
	}

	/**
	 * Constructs an AbstractJdbcSaver with custom table name.
	 * 
	 * @param dataSource   the JDBC DataSource to use for database connections
	 * @param objectMapper the ObjectMapper for JSON serialization
	 * @param tableName    the name of the database table to store checkpoints
	 */
	protected AbstractJdbcSaver(DataSource dataSource, ObjectMapper objectMapper, String tableName) {
		this.dataSource = Objects.requireNonNull(dataSource, "dataSource cannot be null");
		this.objectMapper = configureObjectMapper(objectMapper);
		this.tableName = Objects.requireNonNull(tableName, "tableName cannot be null");
		initializeTable();
	}

	private static ObjectMapper configureObjectMapper(ObjectMapper objectMapper) {
		// Reuse BaseCheckpointSaver's configuration to ensure consistent Message
		// serialization
		return BaseCheckpointSaver.configureObjectMapper(objectMapper);
	}

	/**
	 * Returns the SQL statement to create the checkpoint table.
	 * Subclasses should provide database-specific DDL.
	 * 
	 * @return the CREATE TABLE SQL statement
	 */
	protected abstract String getCreateTableSql();

	/**
	 * Returns the SQL statement to select checkpoint data by thread ID.
	 * 
	 * @return the SELECT SQL statement
	 */
	protected String getSelectSql() {
		return "SELECT checkpoint_data FROM %s WHERE thread_id = ?".formatted(tableName);
	}

	/**
	 * Returns the SQL statement to insert a new checkpoint record.
	 * 
	 * @return the INSERT SQL statement
	 */
	protected abstract String getInsertSql();

	/**
	 * Returns the SQL statement to update an existing checkpoint record.
	 * 
	 * @return the UPDATE SQL statement
	 */
	protected String getUpdateSql() {
		return """
				UPDATE %s
				SET checkpoint_data = ?, updated_at = CURRENT_TIMESTAMP
				WHERE thread_id = ?
				""".formatted(tableName);
	}

	/**
	 * Returns the SQL statement to delete checkpoint data by thread ID.
	 * 
	 * @return the DELETE SQL statement
	 */
	protected String getDeleteSql() {
		return "DELETE FROM %s WHERE thread_id = ?".formatted(tableName);
	}

	/**
	 * Initializes the database table if it doesn't exist.
	 */
	protected void initializeTable() {
		try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
			stmt.execute(getCreateTableSql());
			logger.debug("Checkpoint table '{}' initialized successfully", tableName);
		} catch (SQLException e) {
			throw new RuntimeException("Failed to initialize checkpoint table: " + tableName, e);
		}
	}

	@Override
	public Collection<Checkpoint> list(RunnableConfig config) {
		Optional<String> threadIdOpt = config.threadId();
		if (threadIdOpt.isEmpty()) {
			throw new IllegalArgumentException("threadId is not allowed to be null");
		}

		String threadId = threadIdOpt.get();

		try (Connection conn = dataSource.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(getSelectSql())) {

			pstmt.setString(1, threadId);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					String checkpointJson = rs.getString("checkpoint_data");
					List<Checkpoint> checkpoints = objectMapper.readValue(checkpointJson, new TypeReference<>() {
					});
					return checkpoints;
				}
				return Collections.emptyList();
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to list checkpoints for threadId: " + threadId, e);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to deserialize checkpoint data", e);
		}
	}

	@Override
	public Optional<Checkpoint> get(RunnableConfig config) {
		Optional<String> threadIdOpt = config.threadId();
		if (threadIdOpt.isEmpty()) {
			throw new IllegalArgumentException("threadId is not allowed to be null");
		}

		String threadId = threadIdOpt.get();

		try (Connection conn = dataSource.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(getSelectSql())) {

			pstmt.setString(1, threadId);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					String checkpointJson = rs.getString("checkpoint_data");
					List<Checkpoint> checkpoints = objectMapper.readValue(checkpointJson, new TypeReference<>() {
					});

					// If specific checkpoint ID is requested
					if (config.checkPointId().isPresent()) {
						return config.checkPointId()
								.flatMap(id -> checkpoints.stream()
										.filter(checkpoint -> checkpoint.getId().equals(id))
										.findFirst());
					}

					// Return the latest (first in list)
					return getLast(getLinkedList(checkpoints), config);
				}
				return Optional.empty();
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to get checkpoint for threadId: " + threadId, e);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to deserialize checkpoint data", e);
		}
	}

	@Override
	public RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception {
		Optional<String> threadIdOpt = config.threadId();
		if (threadIdOpt.isEmpty()) {
			throw new IllegalArgumentException("threadId is not allowed to be null");
		}

		String threadId = threadIdOpt.get();

		try (Connection conn = dataSource.getConnection()) {
			conn.setAutoCommit(false);
			try {
				// Read existing checkpoints
				LinkedList<Checkpoint> checkpoints;
				boolean recordExists = false;
				try (PreparedStatement pstmt = conn.prepareStatement(getSelectSql())) {
					pstmt.setString(1, threadId);
					try (ResultSet rs = pstmt.executeQuery()) {
						if (rs.next()) {
							recordExists = true;
							String checkpointJson = rs.getString("checkpoint_data");
							List<Checkpoint> existingList = objectMapper.readValue(checkpointJson,
									new TypeReference<>() {
									});
							checkpoints = getLinkedList(existingList);
						} else {
							checkpoints = new LinkedList<>();
						}
					}
				}

				// Update or insert checkpoint
				if (config.checkPointId().isPresent()) {
					// Replace existing checkpoint
					String checkPointId = config.checkPointId().get();
					int index = IntStream.range(0, checkpoints.size())
							.filter(i -> checkpoints.get(i).getId().equals(checkPointId))
							.findFirst()
							.orElseThrow(() -> new NoSuchElementException(
									format("Checkpoint with id %s not found!", checkPointId)));
					checkpoints.set(index, checkpoint);
				} else {
					// Add new checkpoint to the front
					checkpoints.push(checkpoint);
				}

				// Save back to database using UPDATE or INSERT
				String checkpointJson = objectMapper.writeValueAsString(checkpoints);
				if (recordExists) {
					// Update existing record
					try (PreparedStatement pstmt = conn.prepareStatement(getUpdateSql())) {
						pstmt.setString(1, checkpointJson);
						pstmt.setString(2, threadId);
						pstmt.executeUpdate();
					}
				} else {
					// Insert new record
					try (PreparedStatement pstmt = conn.prepareStatement(getInsertSql())) {
						setInsertParameters(pstmt, threadId, checkpointJson);
						pstmt.executeUpdate();
					}
				}

				conn.commit();

				if (config.checkPointId().isPresent()) {
					return RunnableConfig.builder(config).checkPointId(checkpoint.getId()).build();
				}
				return RunnableConfig.builder(config).checkPointId(checkpoint.getId()).build();
			} catch (Exception e) {
				conn.rollback();
				throw e;
			} finally {
				conn.setAutoCommit(true);
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to put checkpoint for threadId: " + threadId, e);
		}
	}

	/**
	 * Sets the parameters for the INSERT statement.
	 * Subclasses can override this if they need different parameter ordering.
	 * 
	 * @param pstmt          the PreparedStatement
	 * @param threadId       the thread ID
	 * @param checkpointJson the serialized checkpoint data
	 * @throws SQLException if a database access error occurs
	 */
	protected void setInsertParameters(PreparedStatement pstmt, String threadId, String checkpointJson)
			throws SQLException {
		pstmt.setString(1, threadId);
		pstmt.setString(2, checkpointJson);
	}

	@Override
	public boolean clear(RunnableConfig config) {
		Optional<String> threadIdOpt = config.threadId();
		if (threadIdOpt.isEmpty()) {
			throw new IllegalArgumentException("threadId is not allowed to be null");
		}

		String threadId = threadIdOpt.get();

		try (Connection conn = dataSource.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(getDeleteSql())) {

			pstmt.setString(1, threadId);
			int rowsAffected = pstmt.executeUpdate();
			return rowsAffected > 0;
		} catch (SQLException e) {
			throw new RuntimeException("Failed to clear checkpoints for threadId: " + threadId, e);
		}
	}

}

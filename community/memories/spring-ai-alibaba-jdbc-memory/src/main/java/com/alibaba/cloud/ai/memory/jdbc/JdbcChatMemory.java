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
package com.alibaba.cloud.ai.memory.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * @author future0923
 */
public abstract class JdbcChatMemory implements ChatMemory, AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(JdbcChatMemory.class);

	private static final String DEFAULT_TABLE_NAME = "chat_memory";

	private final Connection connection;

	private final String tableName;

	private Integer lastN;

	protected JdbcChatMemory(String username, String password, String jdbcUrl) {
		this(username, password, jdbcUrl, DEFAULT_TABLE_NAME, Integer.MAX_VALUE);
	}

	protected JdbcChatMemory(String username, String password, String jdbcUrl, String tableName, Integer lastN) {
		this.lastN = lastN;
		this.tableName = tableName;
		try {
			this.connection = DriverManager.getConnection(jdbcUrl, username, password);
			checkAndCreateTable();
		}
		catch (SQLException e) {
			throw new RuntimeException("Error connecting to the database", e);
		}
	}

	protected JdbcChatMemory(Connection connection) {
		this(connection, DEFAULT_TABLE_NAME);
	}

	protected JdbcChatMemory(Connection connection, String tableName) {
		this.connection = connection;
		this.tableName = tableName;
		try {
			checkAndCreateTable();
		}
		catch (SQLException e) {
			throw new RuntimeException("Error checking the database table", e);
		}
	}

	protected abstract String jdbcType();

	protected abstract String hasTableSql(String tableName);

	protected abstract String createTableSql(String tableName);

	/**
	 * Generate paginated query SQL based on the database type. Default implementation
	 * uses LIMIT clause. Subclasses can override this method if their database uses
	 * different pagination syntax.
	 * @param tableName The name of the table
	 * @param lastN Number of records to return, if greater than 0
	 * @return SQL query string with pagination
	 */
	protected String generatePaginatedQuerySql(String tableName, int lastN) {
		StringBuilder sqlBuilder = new StringBuilder("SELECT messages,type FROM ").append(tableName)
			.append(" WHERE conversation_id = ?");

		if (lastN > 0) {
			sqlBuilder.append(" LIMIT ?");
		}

		return sqlBuilder.toString();
	}

	private void checkAndCreateTable() throws SQLException {
		String checkTableQuery = hasTableSql(tableName);
		try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(checkTableQuery)) {
			if (rs.next()) {
				logger.info("Table {} exists.", tableName);
			}
			else {
				logger.info("Table {} does not exist. Creating table...", tableName);
				createTable();
			}
		}
	}

	private void createTable() {
		try (Statement stmt = connection.createStatement()) {
			stmt.execute(createTableSql(tableName));
			logger.info("Table {} created successfully.", tableName);
		}
		catch (Exception e) {
			throw new RuntimeException("Error creating table " + tableName + " ", e);
		}
	}

	@Override
	public void add(String conversationId, List<Message> messages) {
		try {
			for (Message message : messages) {
				String sql = "INSERT INTO " + tableName + " (messages, conversation_id, type) VALUES (?, ?, ?)";
				try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {
					stmt.setString(1, message.getText());
					stmt.setString(2, conversationId);
					stmt.setString(3, message.getMessageType().name());
					stmt.executeUpdate();
				}
			}
		}
		catch (Exception e) {
			logger.error("Error adding messages to {} chat memory", jdbcType(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<Message> get(String conversationId, int lastN) {
		return this.selectMessageById(conversationId, lastN);
	}

	@Override
	public List<Message> get(String conversationId) {
		return this.selectMessageById(conversationId, lastN);
	}

	@Override
	public void clear(String conversationId) {
		String sql = "DELETE FROM " + tableName + " WHERE conversation_id = ?";
		try (PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setString(1, conversationId);
			stmt.executeUpdate();
		}
		catch (Exception e) {
			logger.error("Error clearing messages from {} chat memory", jdbcType(), e);
			throw new RuntimeException("Error executing delete ", e);
		}

	}

	@Override
	public void close() throws Exception {
		if (connection != null) {
			connection.close();
		}
	}

	public void clearOverLimit(String conversationId, int maxLimit, int deleteSize) {
		try {
			List<Message> all = this.selectMessageById(conversationId);
			if (all.size() >= maxLimit) {
				// Delete oldest messages first
				String sql = "DELETE FROM " + tableName + " WHERE conversation_id = ? ORDER BY ROWID LIMIT ?";
				try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {
					stmt.setString(1, conversationId);
					stmt.setInt(2, deleteSize);
					stmt.executeUpdate();
				}
				catch (SQLException e) {
					// If the database doesn't support ORDER BY in DELETE, fallback to
					// alternative approach
					all = all.stream().skip(Math.max(0, deleteSize)).toList();
					// Clear all messages and reinsert the remaining ones
					clear(conversationId);
					for (Message message : all) {
						add(conversationId, List.of(message));
					}
				}
			}
		}
		catch (Exception e) {
			logger.error("Error clearing messages from {} chat memory", jdbcType(), e);
			throw new RuntimeException(e);
		}
	}

	public List<Message> selectMessageById(String conversationId, int lastN) {
		List<Message> totalMessage = new ArrayList<>();
		String sql = generatePaginatedQuerySql(tableName, lastN);
		try (PreparedStatement stmt = connection.prepareStatement(sql)) {
			// Set conversation ID parameters.
			stmt.setString(1, conversationId);
			// If there is a limit, set the limit parameter.
			if (lastN > 0) {
				stmt.setInt(2, lastN);
			}
			ResultSet resultSet = stmt.executeQuery();
			while (resultSet.next()) {
				var content = resultSet.getString("messages");
				var type = MessageType.valueOf(resultSet.getString("type"));

				var message = switch (type) {
					case USER -> new UserMessage(content);
					case ASSISTANT -> new AssistantMessage(content);
					case SYSTEM -> new SystemMessage(content);
					default -> null;
				};
				totalMessage.add(message);

			}
		}
		catch (SQLException e) {
			logger.error("select message by {} error，sql:{}", jdbcType(), sql, e);
			throw new RuntimeException(e);
		}
		return totalMessage;
	}

	public List<Message> selectMessageById(String conversationId) {
		return this.selectMessageById(conversationId, 0);
	}

}

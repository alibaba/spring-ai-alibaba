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

import com.alibaba.cloud.ai.memory.jdbc.serializer.MessageDeserializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

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

	private final ObjectMapper objectMapper = new ObjectMapper();

	protected abstract String jdbcType();

	protected abstract String hasTableSql(String tableName);

	protected abstract String createTableSql(String tableName);

	protected JdbcChatMemory(String username, String password, String jdbcUrl) {
		this(username, password, jdbcUrl, DEFAULT_TABLE_NAME);
	}

	protected JdbcChatMemory(String username, String password, String jdbcUrl, String tableName) {
		// Configure ObjectMapper to support interface deserialization
		SimpleModule module = new SimpleModule();
		module.addDeserializer(Message.class, new MessageDeserializer());
		this.objectMapper.registerModule(module);
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
		// Configure ObjectMapper to support interface deserialization
		SimpleModule module = new SimpleModule();
		module.addDeserializer(Message.class, new MessageDeserializer());
		this.objectMapper.registerModule(module);
		this.connection = connection;
		this.tableName = tableName;
		try {
			checkAndCreateTable();
		}
		catch (SQLException e) {
			throw new RuntimeException("Error checking the database table", e);
		}
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
			List<Message> all = this.selectMessageById(conversationId);
			all.addAll(messages);
			this.updateMessageById(conversationId, this.objectMapper.writeValueAsString(all));
		}
		catch (Exception e) {
			logger.error("Error adding messages to {} chat memory", jdbcType(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<Message> get(String conversationId, int lastN) {
		List<Message> all;
		try {
			all = this.selectMessageById(conversationId);
		}
		catch (Exception e) {
			logger.error("Error getting messages from {} chat memory", jdbcType(), e);
			throw new RuntimeException(e);
		}
		return all != null ? all.stream().skip(Math.max(0, all.size() - lastN)).toList() : List.of();
	}

	@Override
	public void clear(String conversationId) {
		StringBuilder sql = new StringBuilder("DELETE FROM " + tableName + " WHERE conversation_id = '");
		sql.append(conversationId);
		sql.append("'");
		try (Statement stmt = connection.createStatement()) {
			stmt.executeUpdate(sql.toString());
		}
		catch (Exception e) {
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
				all = all.stream().skip(Math.max(0, deleteSize)).toList();
				this.updateMessageById(conversationId, this.objectMapper.writeValueAsString(all));
			}
		}
		catch (Exception e) {
			logger.error("Error clearing messages from {} chat memory", jdbcType(), e);
			throw new RuntimeException(e);
		}
	}

	public List<Message> selectMessageById(String conversationId) {
		List<Message> totalMessage = new ArrayList<>();
		StringBuilder sql = new StringBuilder("SELECT messages FROM " + tableName + " WHERE conversation_id = '");
		sql.append(conversationId);
		sql.append("'");
		try (Statement stmt = connection.createStatement()) {
			ResultSet resultSet = stmt.executeQuery(sql.toString());
			String oldMessage;
			while (resultSet.next()) {
				oldMessage = resultSet.getString("messages");
				if (oldMessage != null && !oldMessage.isEmpty()) {
					List<Message> all = this.objectMapper.readValue(oldMessage, new TypeReference<>() {
					});
					totalMessage.addAll(all);
				}
			}
		}
		catch (SQLException | JsonProcessingException e) {
			logger.error("select message by {} error，sql:{}", jdbcType(), sql, e);
			throw new RuntimeException(e);
		}
		return totalMessage;
	}

	public void updateMessageById(String conversationId, String messages) {
		// Remove newlines and escape single quotes
		messages = messages.replaceAll("[\\r\\n]", "").replace("'", "''");
		String sql;
		if (this.selectMessageById(conversationId).isEmpty()) {
			sql = "INSERT INTO chat_memory (messages, conversation_id) VALUES (?, ?)";
		}
		else {
			sql = "UPDATE chat_memory SET messages = ? WHERE conversation_id = ?";
		}
		try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {
			stmt.setString(1, messages);
			stmt.setString(2, conversationId);
			stmt.executeUpdate();
		}
		catch (SQLException e) {
			logger.error("update message by {} error，sql:{}", sql, jdbcType(), e);
			throw new RuntimeException(e);
		}
	}

}

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

package com.alibaba.cloud.ai.memory.sqlite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.cloud.ai.memory.sqlite.serializer.MessageDeserializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

/**
 * SQLite database implementation of {@link ChatMemory}.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

public class SQliteChatMemory implements ChatMemory, AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(SQliteChatMemory.class);

	private static final String DEFAULT_DATABASE = "chat_memory.db";

	private static final String DEFAULT_TABLE_NAME = "chat_memory";

	private final Connection connection;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public SQliteChatMemory() {
		this(DEFAULT_DATABASE);
	}

	public SQliteChatMemory(String databaseName) {

		try {
			this.connection = DriverManager.getConnection("jdbc:sqlite:" + databaseName);
			checkAndCreateTable();
		}
		catch (SQLException e) {
			throw new RuntimeException("Error connecting to the database", e);
		}

		SimpleModule module = new SimpleModule();
		module.addDeserializer(Message.class, new MessageDeserializer());
		this.objectMapper.registerModule(module);
	}

	private void checkAndCreateTable() throws SQLException {

		String createTableSQL = "CREATE TABLE IF NOT EXISTS " + DEFAULT_TABLE_NAME + " ("
				+ "id INTEGER PRIMARY KEY AUTOINCREMENT," + "conversation_id TEXT UNIQUE," + "messages TEXT" + ")";

		try (Statement stmt = connection.createStatement()) {
			stmt.execute(createTableSQL);
			logger.info("Table " + DEFAULT_TABLE_NAME + " checked/created successfully.");
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

			logger.error("Error adding messages to SQLite chat memory", e);
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
			logger.error("Error getting messages from SQLite chat memory", e);
			throw new RuntimeException(e);
		}

		return all != null ? all.stream().skip(Math.max(0, all.size() - lastN)).toList() : List.of();
	}

	@Override
	public void clear(String conversationId) {

		String sql = "DELETE FROM " + DEFAULT_TABLE_NAME + " WHERE conversation_id = ?";

		try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
			pstmt.setString(1, conversationId);
			pstmt.executeUpdate();
		}
		catch (Exception e) {

			throw new RuntimeException("Error executing delete", e);
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
				List<Message> messagesToKeep = all.stream().skip(Math.max(0, deleteSize)).toList();

				this.updateMessageById(conversationId, this.objectMapper.writeValueAsString(messagesToKeep));
				logger.info("Cleared messages for conversationId: " + conversationId + ", retained: "
						+ messagesToKeep.size());
			}
		}
		catch (Exception e) {

			logger.error("Error clearing messages from SQLite chat memory", e);
			throw new RuntimeException(e);
		}
	}

	public List<Message> selectMessageById(String conversationId) {

		List<Message> totalMessage = new ArrayList<>();
		String sql = "SELECT messages FROM " + DEFAULT_TABLE_NAME + " WHERE conversation_id = ?";

		try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

			pstmt.setString(1, conversationId);
			ResultSet resultSet = pstmt.executeQuery();
			String oldMessage;
			while (resultSet.next()) {

				oldMessage = resultSet.getString("messages");

				if (oldMessage != null && !oldMessage.isEmpty()) {
					List<Message> data = this.objectMapper.readValue(oldMessage, new TypeReference<>() {
					});
					System.out.println("data: " + data);
					totalMessage.addAll(data);
				}
			}
		}
		catch (SQLException | JsonProcessingException e) {

			logger.error("Select message by SQLite error, sql:{}", sql, e);
			throw new RuntimeException(e);
		}

		return totalMessage;
	}

	public void updateMessageById(String conversationId, String messages) {

		String sql;

		if (this.selectMessageById(conversationId).isEmpty()) {
			sql = "INSERT INTO " + DEFAULT_TABLE_NAME + " (conversation_id, messages) VALUES (?, ?)";
		}
		else {
			sql = "UPDATE " + DEFAULT_TABLE_NAME + " SET messages = ? WHERE conversation_id = ?";
		}

		try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

			if (this.selectMessageById(conversationId).isEmpty()) {
				pstmt.setString(1, conversationId);
				pstmt.setString(2, messages);
			}
			else {
				pstmt.setString(1, messages);
				pstmt.setString(2, conversationId);
			}
			pstmt.executeUpdate();
		}
		catch (SQLException e) {

			logger.error("Update message by SQLite error, sql:{}", sql, e);
			throw new RuntimeException(e);
		}

	}

}

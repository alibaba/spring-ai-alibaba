package com.alibaba.cloud.ai.memory.store;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;

import com.alibaba.cloud.ai.memory.MemoryMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

/**
 * @author yuluo
 * @author wblu214
 * @author <a href="mailto:2897718178@qq.com">wblu214</a>
 */

public class MySQLChatMemory implements ChatMemory, AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(MySQLChatMemory.class);

	private static final String DEFAULT_DATABASE = "spring_ai_alibaba_mysql";

	private static final String DEFAULT_TABLE_NAME = "chat_memory";

	private static final String DEFAULT_URL = "127.0.0.1:3306";

	private static final String DEFAULT_USERNAME = "root";

	private static final String DEFAULT_PASSWORD = "root";

	private final Connection connection;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public MySQLChatMemory() {

		this(DEFAULT_USERNAME, DEFAULT_PASSWORD, DEFAULT_URL);
	}

	public MySQLChatMemory(String username, String password, String url) {

		try {
			this.connection = DriverManager.getConnection(
					String.format("jdbc:mysql://%s/%s?serverTimezone=UTC", url, DEFAULT_DATABASE), username, password);
			checkAndCreateTable();
		}
		catch (SQLException e) {
			throw new RuntimeException("Error connecting to the database", e);
		}
	}

	public MySQLChatMemory(Connection connection) {

		this.connection = connection;

		try {
			checkAndCreateTable();
		}
		catch (SQLException e) {
			throw new RuntimeException("Error checking the database table", e);
		}
	}

	private void checkAndCreateTable() throws SQLException {

		String checkTableQuery = String.format("SHOW TABLES LIKE '%s'", DEFAULT_TABLE_NAME);
		try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(checkTableQuery)) {

			if (rs.next()) {
				logger.info("Table " + DEFAULT_TABLE_NAME + " exists.");
			}
			else {
				logger.info("Table " + DEFAULT_TABLE_NAME + " does not exist. Creating table...");
				createTable();
			}
		}
	}

	private void createTable() {
		try (Statement stmt = connection.createStatement()) {
			stmt.execute("USE spring_ai_alibaba_mysql");
			stmt.execute(
					"CREATE TABLE chat_memory( id BIGINT AUTO_INCREMENT PRIMARY KEY,conversation_id  VARBINARY(256)  NULL,messages VARBINARY(6144) NULL,UNIQUE (conversation_id));");
			logger.info("Table " + DEFAULT_TABLE_NAME + " created successfully.");
		}
		catch (Exception e) {
			throw new RuntimeException("Error creating table " + DEFAULT_TABLE_NAME + " ", e);
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
			logger.error("Error adding messages to MySQL chat memory", e);
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
			logger.error("Error getting messages from MySQL chat memory", e);
			throw new RuntimeException(e);
		}
		return all != null ? all.stream().skip(Math.max(0, all.size() - lastN)).toList() : List.of();
	}

	@Override
	public void clear(String conversationId) {
		StringBuilder sql = new StringBuilder("DELETE FROM " + DEFAULT_TABLE_NAME + " WHERE conversation_id = '");
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
			logger.error("Error clearing messages from MySQL chat memory", e);
			throw new RuntimeException(e);
		}
	}

	public List<Message> selectMessageById(String conversationId) {
		List<Message> totalMessage = new ArrayList<>();
		StringBuilder sql = new StringBuilder(
				"SELECT messages FROM " + DEFAULT_TABLE_NAME + " WHERE conversation_id = '");
		sql.append(conversationId);
		sql.append("'");
		try (Statement stmt = connection.createStatement()) {
			ResultSet resultSet = stmt.executeQuery(sql.toString());
			String oldMessage;
			while (resultSet.next()) {
				oldMessage = resultSet.getString("messages");
				if (oldMessage != null && !oldMessage.isEmpty()) {
					List<MemoryMessage> all = this.objectMapper.readValue(oldMessage, new TypeReference<>() {
					});
					totalMessage.addAll(all);
				}
			}
		}
		catch (SQLException | JsonProcessingException e) {
			logger.error("select message by mysql error，sql:{}", sql, e);
			throw new RuntimeException(e);
		}
		return totalMessage;
	}

	public void updateMessageById(String conversationId, String messages) {
		StringBuilder sql;
		if (this.selectMessageById(conversationId).isEmpty()) {
			sql = new StringBuilder("INSERT INTO " + DEFAULT_TABLE_NAME + " (conversation_id, messages) VALUES ('");
			sql.append(conversationId);
			sql.append("', '");
			sql.append(messages);
			sql.append("')");
		}
		else {
			sql = new StringBuilder("UPDATE " + DEFAULT_TABLE_NAME + " SET messages = '");
			sql.append(messages);
			sql.append("' WHERE conversation_id = '");
			sql.append(conversationId);
			sql.append("'");
		}
		try (Statement stmt = connection.createStatement()) {
			stmt.executeUpdate(sql.toString());
		}
		catch (SQLException e) {
			logger.error("update message by mysql error，sql:{}", sql, e);
			throw new RuntimeException(e);
		}
	}

}
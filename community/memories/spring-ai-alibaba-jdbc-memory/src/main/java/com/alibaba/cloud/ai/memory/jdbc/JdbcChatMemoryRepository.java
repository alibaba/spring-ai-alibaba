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

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.*;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public abstract class JdbcChatMemoryRepository implements ChatMemoryRepository {

	public static final String TABLE_NAME = "ai_chat_memory";

	private static final String QUERY_GET_IDS = """
			SELECT DISTINCT conversation_id FROM ai_chat_memory
			""";

	private static final String QUERY_ADD = """
			INSERT INTO ai_chat_memory (conversation_id, content, type, "timestamp") VALUES (?, ?, ?, ?)
			""";

	private static final String QUERY_GET = """
			SELECT content, type FROM ai_chat_memory WHERE conversation_id = ? ORDER BY "timestamp"
			""";

	private static final String QUERY_CLEAR = "DELETE FROM ai_chat_memory WHERE conversation_id = ?";

	private final JdbcTemplate jdbcTemplate;

	public JdbcChatMemoryRepository(JdbcTemplate jdbcTemplate) {
		Assert.notNull(jdbcTemplate, "jdbcTemplate cannot be null");
		this.jdbcTemplate = jdbcTemplate;
		checkAndCreateTable();
	}

	private void checkAndCreateTable() {
		if (!jdbcTemplate.query(hasTableSql(TABLE_NAME), ResultSet::next)) {
			jdbcTemplate.execute(createTableSql(TABLE_NAME));
		}
	}

	@Override
	public List<String> findConversationIds() {
		List<String> conversationIds = this.jdbcTemplate.query(QUERY_GET_IDS, rs -> {
			var ids = new ArrayList<String>();
			while (rs.next()) {
				ids.add(rs.getString(1));
			}
			return ids;
		});
		return conversationIds != null ? conversationIds : List.of();
	}

	@Override
	public List<Message> findByConversationId(String conversationId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		return this.jdbcTemplate.query(getGetSql(), new JdbcChatMemoryRepository.MessageRowMapper(), conversationId);
	}

	@Override
	public void saveAll(String conversationId, List<Message> messages) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		Assert.notNull(messages, "messages cannot be null");
		Assert.noNullElements(messages, "messages cannot contain null elements");
		this.deleteByConversationId(conversationId);
		this.jdbcTemplate.batchUpdate(getAddSql(),
				new JdbcChatMemoryRepository.AddBatchPreparedStatement(conversationId, messages));
	}

	@Override
	public void deleteByConversationId(String conversationId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		this.jdbcTemplate.update(QUERY_CLEAR, conversationId);
	}

	/**
	 * Get the SQL statement used to add records
	 * @return SQL
	 */
	protected String getAddSql() {
		return QUERY_ADD;
	}

	/**
	 * Get the SQL statement used for querying records
	 * @return SQL
	 */
	protected String getGetSql() {
		return QUERY_GET;
	}

	private record AddBatchPreparedStatement(String conversationId, List<Message> messages,
			AtomicLong instantSeq) implements BatchPreparedStatementSetter {

		private AddBatchPreparedStatement(String conversationId, List<Message> messages) {
			this(conversationId, messages, new AtomicLong(Instant.now().toEpochMilli()));
		}

		@Override
		public void setValues(PreparedStatement ps, int i) throws SQLException {
			var message = this.messages.get(i);

			ps.setString(1, this.conversationId);
			ps.setString(2, message.getText());
			ps.setString(3, message.getMessageType().name());
			ps.setTimestamp(4, new Timestamp(instantSeq.getAndIncrement()));
		}

		@Override
		public int getBatchSize() {
			return this.messages.size();
		}
	}

	private static class MessageRowMapper implements RowMapper<Message> {

		@Override
		@Nullable
		public Message mapRow(ResultSet rs, int i) throws SQLException {
			var content = rs.getString(1);
			var type = MessageType.valueOf(rs.getString(2));

			return switch (type) {
				case USER -> new UserMessage(content);
				case ASSISTANT -> new AssistantMessage(content);
				case SYSTEM -> new SystemMessage(content);
				// The content is always stored empty for ToolResponseMessages.
				// If we want to capture the actual content, we need to extend
				// AddBatchPreparedStatement to support it.
				case TOOL -> new ToolResponseMessage(List.of());
			};
		}

	}

	protected abstract String hasTableSql(String tableName);

	protected abstract String createTableSql(String tableName);

}

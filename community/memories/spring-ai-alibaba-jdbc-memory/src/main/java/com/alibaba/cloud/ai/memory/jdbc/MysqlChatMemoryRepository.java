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

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * MySQL implementation of chat memory repository
 */
public class MysqlChatMemoryRepository extends JdbcChatMemoryRepository {

	// MySQL specific query statements
	private static final String MYSQL_QUERY_ADD = "INSERT INTO ai_chat_memory (conversation_id, content, type, timestamp) VALUES (?, ?, ?, ?)";

	private static final String MYSQL_QUERY_GET = "SELECT content, type FROM ai_chat_memory WHERE conversation_id = ? ORDER BY timestamp";

	private MysqlChatMemoryRepository(JdbcTemplate jdbcTemplate) {
		super(jdbcTemplate);
	}

	public static MysqlBuilder mysqlBuilder() {
		return new MysqlBuilder();
	}

	public static class MysqlBuilder {

		private JdbcTemplate jdbcTemplate;

		public MysqlBuilder jdbcTemplate(JdbcTemplate jdbcTemplate) {
			this.jdbcTemplate = jdbcTemplate;
			return this;
		}

		public MysqlChatMemoryRepository build() {
			return new MysqlChatMemoryRepository(this.jdbcTemplate);
		}

	}

	@Override
	protected String hasTableSql(String tableName) {
		return String.format(
				"SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = '%s'",
				tableName);
	}

	@Override
	protected String createTableSql(String tableName) {
		return String.format(
				"CREATE TABLE %s (id BIGINT AUTO_INCREMENT PRIMARY KEY, "
						+ "conversation_id VARCHAR(256) NOT NULL, content LONGTEXT NOT NULL, "
						+ "type VARCHAR(100) NOT NULL, timestamp TIMESTAMP NOT NULL, "
						+ "CONSTRAINT chk_message_type CHECK (type IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL')))",
				tableName);
	}

	@Override
	protected String getAddSql() {
		return MYSQL_QUERY_ADD;
	}

	@Override
	protected String getGetSql() {
		return MYSQL_QUERY_GET;
	}

}

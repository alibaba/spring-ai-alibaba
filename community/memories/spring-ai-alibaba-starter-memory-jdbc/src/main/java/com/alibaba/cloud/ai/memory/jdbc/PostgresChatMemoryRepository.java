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

public class PostgresChatMemoryRepository extends JdbcChatMemoryRepository {

	private static final String POSTGRES_QUERY_ADD = "INSERT INTO ai_chat_memory (conversation_id, content, type, timestamp) VALUES (?, ?, ?, ?)";

	private static final String POSTGRES_QUERY_GET = "SELECT content, type FROM ai_chat_memory WHERE conversation_id = ? ORDER BY timestamp";

	private PostgresChatMemoryRepository(JdbcTemplate jdbcTemplate) {
		super(jdbcTemplate);
	}

	public static PostgresBuilder postgresBuilder() {
		return new PostgresBuilder();
	}

	public static class PostgresBuilder {

		private JdbcTemplate jdbcTemplate;

		public PostgresBuilder jdbcTemplate(JdbcTemplate jdbcTemplate) {
			this.jdbcTemplate = jdbcTemplate;
			return this;
		}

		public PostgresChatMemoryRepository build() {
			return new PostgresChatMemoryRepository(this.jdbcTemplate);
		}

	}

	@Override
	protected String hasTableSql(String tableName) {
		return String.format("SELECT table_name FROM information_schema.tables WHERE table_name = '%s'",
				tableName.toLowerCase());
	}

	@Override
	protected String createTableSql(String tableName) {
		return String.format(
				"CREATE TABLE %s (id BIGSERIAL PRIMARY KEY, "
						+ "conversation_id VARCHAR(256) NOT NULL, content TEXT NOT NULL, "
						+ "type VARCHAR(100) NOT NULL, timestamp TIMESTAMP NOT NULL, "
						+ "CONSTRAINT chk_message_type CHECK (type IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL')))",
				tableName);
	}

	@Override
	protected String getAddSql() {
		return POSTGRES_QUERY_ADD;
	}

	@Override
	protected String getGetSql() {
		return POSTGRES_QUERY_GET;
	}

}

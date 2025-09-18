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
package com.alibaba.cloud.ai.manus.memory.repository;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * auth: dahua
 */
public class H2ChatMemoryRepository extends JdbcChatMemoryRepository {

	// H2 specific query statements
	private static final String H2_QUERY_ADD = "INSERT INTO ai_chat_memory (conversation_id, content, type, timestamp) VALUES (?, ?, ?, ?)";

	private static final String H2_QUERY_GET = "SELECT content, type FROM ai_chat_memory WHERE conversation_id = ? ORDER BY timestamp";

	private H2ChatMemoryRepository(JdbcTemplate jdbcTemplate) {
		super(jdbcTemplate);
	}

	public static H2Builder h2Builder() {
		return new H2Builder();
	}

	public static class H2Builder {

		private JdbcTemplate jdbcTemplate;

		public H2Builder jdbcTemplate(JdbcTemplate jdbcTemplate) {
			this.jdbcTemplate = jdbcTemplate;
			return this;
		}

		public H2ChatMemoryRepository build() {
			return new H2ChatMemoryRepository(this.jdbcTemplate);
		}

	}

	@Override
	protected String hasTableSql(String tableName) {
		return String.format("SELECT table_name FROM information_schema.tables WHERE table_name = '%s'", tableName);
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
		return H2_QUERY_ADD;
	}

	@Override
	protected String getGetSql() {
		return H2_QUERY_GET;
	}

}

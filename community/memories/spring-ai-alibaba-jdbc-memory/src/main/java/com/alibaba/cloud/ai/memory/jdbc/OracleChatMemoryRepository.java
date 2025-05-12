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
 * Oracle implementation of the chat memory repository
 */
public class OracleChatMemoryRepository extends JdbcChatMemoryRepository {

	// Oracle specific query statement, not wrapping the 'timestamp' keyword with double
	// quotes
	private static final String ORACLE_QUERY_ADD = "INSERT INTO ai_chat_memory (conversation_id, content, type, timestamp) VALUES (?, ?, ?, ?)";

	private static final String ORACLE_QUERY_GET = "SELECT content, type FROM ai_chat_memory WHERE conversation_id = ? ORDER BY timestamp";

	private OracleChatMemoryRepository(JdbcTemplate jdbcTemplate) {
		super(jdbcTemplate);
	}

	public static OracleBuilder oracleBuilder() {
		return new OracleBuilder();
	}

	public static class OracleBuilder {

		private JdbcTemplate jdbcTemplate;

		public OracleBuilder jdbcTemplate(JdbcTemplate jdbcTemplate) {
			this.jdbcTemplate = jdbcTemplate;
			return this;
		}

		public OracleChatMemoryRepository build() {
			return new OracleChatMemoryRepository(this.jdbcTemplate);
		}

	}

	@Override
	protected String hasTableSql(String tableName) {
		return String.format("SELECT table_name FROM all_tables WHERE table_name = UPPER('%s')", tableName);
	}

	@Override
	protected String createTableSql(String tableName) {
		return String.format(
				"CREATE TABLE %s (id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
						+ "conversation_id VARCHAR2(256) NOT NULL, content CLOB NOT NULL, "
						+ "type VARCHAR2(100) NOT NULL, timestamp TIMESTAMP NOT NULL, "
						+ "CONSTRAINT chk_message_type CHECK (type IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL')))",
				tableName);
	}

	@Override
	protected String getAddSql() {
		return ORACLE_QUERY_ADD;
	}

	@Override
	protected String getGetSql() {
		return ORACLE_QUERY_GET;
	}

}

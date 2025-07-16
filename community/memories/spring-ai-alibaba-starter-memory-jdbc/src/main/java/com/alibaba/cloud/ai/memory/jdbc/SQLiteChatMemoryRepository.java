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

public class SQLiteChatMemoryRepository extends JdbcChatMemoryRepository {

	private SQLiteChatMemoryRepository(JdbcTemplate jdbcTemplate) {
		super(jdbcTemplate);
	}

	public static SQLiteBuilder sqliteBuilder() {
		return new SQLiteBuilder();
	}

	public static class SQLiteBuilder {

		private JdbcTemplate jdbcTemplate;

		public SQLiteBuilder jdbcTemplate(JdbcTemplate jdbcTemplate) {
			this.jdbcTemplate = jdbcTemplate;
			return this;
		}

		public SQLiteChatMemoryRepository build() {
			return new SQLiteChatMemoryRepository(this.jdbcTemplate);
		}

	}

	@Override
	protected String hasTableSql(String tableName) {
		return String.format("SELECT name FROM sqlite_master WHERE type = 'table' AND name LIKE '%s'", tableName);
	}

	@Override
	protected String createTableSql(String tableName) {
		return String.format("CREATE TABLE IF NOT EXISTS %s ( conversation_id TEXT NOT NULL,"
				+ "    content TEXT NOT NULL, type TEXT NOT NULL, timestamp REAL NOT NULL,"
				+ "    CHECK (type IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL')));", tableName);
	}

}

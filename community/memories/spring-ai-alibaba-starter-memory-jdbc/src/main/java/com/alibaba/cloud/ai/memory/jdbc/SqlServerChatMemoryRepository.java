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

public class SqlServerChatMemoryRepository extends JdbcChatMemoryRepository {

	private SqlServerChatMemoryRepository(JdbcTemplate jdbcTemplate) {
		super(jdbcTemplate);
	}

	public static SqlServerBuilder sqlServerBuilder() {
		return new SqlServerBuilder();
	}

	public static class SqlServerBuilder {

		private JdbcTemplate jdbcTemplate;

		public SqlServerBuilder jdbcTemplate(JdbcTemplate jdbcTemplate) {
			this.jdbcTemplate = jdbcTemplate;
			return this;
		}

		public SqlServerChatMemoryRepository build() {
			return new SqlServerChatMemoryRepository(this.jdbcTemplate);
		}

	}

	@Override
	protected String hasTableSql(String tableName) {
		return String.format("SELECT name FROM sys.tables WHERE name LIKE '%s';", tableName);
	}

	@Override
	protected String createTableSql(String tableName) {
		return String.format("CREATE TABLE %s ( conversation_id NVARCHAR(256) NOT NULL,"
				+ "    content NVARCHAR(MAX) NOT NULL, type VARCHAR(100) NOT NULL, timestamp DATETIME2 NOT NULL,"
				+ "    CHECK (type IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL')));", tableName);
	}

}

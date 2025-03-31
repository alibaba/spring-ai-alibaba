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

import java.sql.Connection;

/**
 * @author future0923
 */
public class PostgresChatMemory extends JdbcChatMemory {

	private static final String JDBC_TYPE = "postgresql";

	public PostgresChatMemory(String username, String password, String url) {
		super(username, password, url);
	}

	public PostgresChatMemory(String username, String password, String jdbcUrl, String tableName) {
		super(username, password, jdbcUrl, tableName);
	}

	public PostgresChatMemory(Connection connection) {
		super(connection);
	}

	public PostgresChatMemory(Connection connection, String tableName) {
		super(connection, tableName);
	}

	@Override
	protected String jdbcType() {
		return JDBC_TYPE;
	}

	@Override
	protected String hasTableSql(String tableName) {
		return String.format(
				"SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE' AND table_name LIKE '%s'",
				tableName);
	}

	@Override
	protected String createTableSql(String tableName) {
		return String.format(
				"CREATE TABLE %s ( id BIGSERIAL PRIMARY KEY, conversation_id VARCHAR(256), messages TEXT, UNIQUE (conversation_id));",
				tableName);
	}

}

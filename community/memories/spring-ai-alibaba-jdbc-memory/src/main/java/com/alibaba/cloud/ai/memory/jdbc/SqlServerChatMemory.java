package com.alibaba.cloud.ai.memory.jdbc;

import java.sql.Connection;

/**
 * @author future0923
 */
public class SqlServerChatMemory extends JdbcChatMemory {

	private static final String JDBC_TYPE = "sqlserver";

	public SqlServerChatMemory(String username, String password, String url) {
		super(username, password, url);
	}

	public SqlServerChatMemory(String username, String password, String jdbcUrl, String tableName) {
		super(username, password, jdbcUrl, tableName);
	}

	public SqlServerChatMemory(Connection connection) {
		super(connection);
	}

	public SqlServerChatMemory(Connection connection, String tableName) {
		super(connection, tableName);
	}

	@Override
	protected String jdbcType() {
		return JDBC_TYPE;
	}

	@Override
	protected String hasTableSql(String tableName) {
		return String.format("SELECT name FROM sys.tables WHERE name LIKE '%s';", tableName);
	}

	@Override
	protected String createTableSql(String tableName) {
		return String.format(
				"CREATE TABLE %s ( id BIGINT IDENTITY(1,1) PRIMARY KEY, conversation_id NVARCHAR(256), messages NVARCHAR(MAX), CONSTRAINT uq_conversation_id UNIQUE (conversation_id));",
				tableName);
	}

}

package com.alibaba.cloud.ai.memory.jdbc;

import java.sql.Connection;

/**
 * @author future0923
 */
public class MysqlChatMemory extends JdbcChatMemory {

	private static final String JDBC_TYPE = "mysql";

	public MysqlChatMemory(String username, String password, String jdbcUrl) {
		super(username, password, jdbcUrl);
	}

	public MysqlChatMemory(String username, String password, String jdbcUrl, String tableName) {
		super(username, password, jdbcUrl, tableName);
	}

	public MysqlChatMemory(Connection connection) {
		super(connection);
	}

	public MysqlChatMemory(Connection connection, String tableName) {
		super(connection, tableName);
	}

	@Override
	protected String jdbcType() {
		return JDBC_TYPE;
	}

	@Override
	protected String hasTableSql(String tableName) {
		return String.format("SHOW TABLES LIKE '%s'", tableName);
	}

	@Override
	protected String createTableSql(String tableName) {
		return String.format(
				"CREATE TABLE %s( id BIGINT AUTO_INCREMENT PRIMARY KEY,conversation_id  VARCHAR(256)  NULL,messages TEXT NULL,UNIQUE (conversation_id)) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;",
				tableName);
	}

}

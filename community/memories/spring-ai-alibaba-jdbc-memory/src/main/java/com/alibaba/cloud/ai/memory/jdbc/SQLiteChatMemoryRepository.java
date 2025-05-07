package com.alibaba.cloud.ai.memory.jdbc;

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

import java.util.List;

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

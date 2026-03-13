/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.multiagents.workflow.sqlagent.tools;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SQL tools for the SQL agent: list tables, get schema, run query.
 */
public final class SqlTools {

	private final JdbcTemplate jdbcTemplate;

	public SqlTools(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Tool(name = "sql_db_list_tables", description = "Input is an empty string, output is a comma-separated list of tables in the database.")
	public String listTables(@ToolParam(description = "Empty string") String ignored) {
		List<String> tables = jdbcTemplate.queryForList(
				"SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE'",
				String.class);
		return String.join(", ", tables);
	}

	@Tool(name = "sql_db_schema", description = "Input is a comma-separated list of tables, output is the schema and sample rows for those tables.")
	public String getSchema(@ToolParam(description = "Comma-separated table names") String tableNames) {
		String[] tables = tableNames.split(",");
		StringBuilder sb = new StringBuilder();
		for (String table : tables) {
			String t = table.trim();
			if (t.isEmpty())
				continue;
			try {
				List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM " + t + " LIMIT 3");
				sb.append("CREATE TABLE \"").append(t).append("\" (");
				List<Map<String, Object>> columns = jdbcTemplate
						.queryForList("SELECT COLUMN_NAME, TYPE_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ?",
								t.toUpperCase());
				sb.append(columns.stream()
						.map(c -> "\"" + c.get("COLUMN_NAME") + "\" " + c.get("TYPE_NAME"))
						.collect(Collectors.joining(", ")));
				sb.append(")\n\n");
				if (!rows.isEmpty()) {
					sb.append("Sample rows:\n");
					rows.forEach(r -> sb.append(r.toString()).append("\n"));
				}
				sb.append("\n");
			}
			catch (Exception e) {
				sb.append("Error for table ").append(t).append(": ").append(e.getMessage()).append("\n");
			}
		}
		return sb.toString();
	}

	@Tool(name = "sql_db_query", description = "Execute a SQL query. Input is a detailed and correct SQL query. Output is the result.")
	public String runQuery(@ToolParam(description = "SQL query to execute") String query) {
		if (query.toUpperCase().contains("INSERT") || query.toUpperCase().contains("UPDATE")
				|| query.toUpperCase().contains("DELETE") || query.toUpperCase().contains("DROP")) {
			return "Error: Only SELECT queries are allowed.";
		}
		try {
			List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);
			return rows.toString();
		}
		catch (Exception e) {
			return "Error: " + e.getMessage();
		}
	}

	public List<ToolCallback> allTools() {
		return Arrays.asList(ToolCallbacks.from(this));
	}

	public ToolCallback getSchemaTool() {
		return findByName("sql_db_schema");
	}

	public ToolCallback runQueryTool() {
		return findByName("sql_db_query");
	}

	public ToolCallback listTablesTool() {
		return findByName("sql_db_list_tables");
	}

	private ToolCallback findByName(String name) {
		return allTools().stream()
				.filter(t -> name.equals(t.getToolDefinition().name()))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unknown tool: " + name));
	}

	/**
	 * Resolver for ToolNode that resolves tools by name from this SqlTools instance.
	 */
	public ToolCallbackResolver resolver() {
		return name -> findByName(name);
	}

}

/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.example.manus.tool.database.action;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.cloud.ai.example.manus.tool.database.DatabaseRequest;
import com.alibaba.cloud.ai.example.manus.tool.database.DataSourceService;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecuteSqlAction extends AbstractDatabaseAction {

	private static final Logger log = LoggerFactory.getLogger(ExecuteSqlAction.class);

	@Override
	public ToolExecuteResult execute(DatabaseRequest request, DataSourceService dataSourceService) {
		String query = request.getQuery();
		String datasourceName = request.getDatasourceName();

		if (query == null || query.trim().isEmpty()) {
			log.warn("ExecuteSqlAction failed: missing query statement, datasourceName={}", datasourceName);
			return new ToolExecuteResult(
					"Datasource: " + (datasourceName != null ? datasourceName : "default") + "\n错误: 缺少查询语句");
		}
		String[] statements = query.split(";");
		List<String> results = new ArrayList<>();
		try (Connection conn = datasourceName != null && !datasourceName.trim().isEmpty()
				? dataSourceService.getConnection(datasourceName) : dataSourceService.getConnection();
				Statement stmt = conn.createStatement()) {
			for (String sql : statements) {
				sql = sql.trim();
				if (sql.isEmpty())
					continue;
				boolean hasResultSet = stmt.execute(sql);
				if (hasResultSet) {
					try (ResultSet rs = stmt.getResultSet()) {
						results.add(formatResultSet(rs));
					}
				}
				else {
					int updateCount = stmt.getUpdateCount();
					results.add("执行成功。影响行数: " + updateCount);
				}
			}
			log.info("ExecuteSqlAction completed successfully, datasourceName={}, statements={}", datasourceName,
					statements.length);
			String resultContent = "Datasource: " + (datasourceName != null ? datasourceName : "default") + "\n"
					+ String.join("\n---\n", results);
			return new ToolExecuteResult(resultContent);
		}
		catch (SQLException e) {
			log.error("ExecuteSqlAction failed with SQLException, datasourceName={}, error={}", datasourceName,
					e.getMessage(), e);
			return new ToolExecuteResult("Datasource: " + (datasourceName != null ? datasourceName : "default")
					+ "\nSQL执行失败: " + e.getMessage());
		}
	}

	private String formatResultSet(ResultSet rs) throws SQLException {
		StringBuilder sb = new StringBuilder();
		int columnCount = rs.getMetaData().getColumnCount();
		// 列名
		for (int i = 1; i <= columnCount; i++) {
			sb.append(rs.getMetaData().getColumnName(i));
			if (i < columnCount)
				sb.append(",");
		}
		sb.append("\n");
		// 数据
		while (rs.next()) {
			for (int i = 1; i <= columnCount; i++) {
				Object val = rs.getObject(i);
				sb.append(val == null ? "NULL" : val.toString());
				if (i < columnCount)
					sb.append(",");
			}
			sb.append("\n");
		}
		return sb.toString().trim();
	}

}

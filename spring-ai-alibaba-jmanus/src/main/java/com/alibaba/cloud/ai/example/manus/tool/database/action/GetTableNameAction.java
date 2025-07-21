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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import com.alibaba.cloud.ai.example.manus.tool.database.DatabaseRequest;
import com.alibaba.cloud.ai.example.manus.tool.database.DataSourceService;
import com.alibaba.cloud.ai.example.manus.tool.database.meta.TableMeta;
import com.alibaba.cloud.ai.example.manus.tool.database.meta.ColumnMeta;
import com.alibaba.cloud.ai.example.manus.tool.database.sql.DatabaseSqlGenerator;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetTableNameAction extends AbstractDatabaseAction {

	private static final Logger log = LoggerFactory.getLogger(GetTableNameAction.class);

	@Override
	public ToolExecuteResult execute(DatabaseRequest request, DataSourceService dataSourceService) {
		String text = request.getText();
		String datasourceName = request.getDatasourceName();

		if (text == null || text.trim().isEmpty()) {
			log.warn("GetTableNameAction failed: missing text parameter, datasourceName={}", datasourceName);
			return new ToolExecuteResult(
					"Datasource: " + (datasourceName != null ? datasourceName : "default") + "\n缺少查询语句");
		}
		// 获取数据库类型
		String databaseType = datasourceName != null && !datasourceName.trim().isEmpty()
				? dataSourceService.getDataSourceType(datasourceName) : dataSourceService.getDataSourceType();

		// 根据数据库类型生成SQL
		String sql = DatabaseSqlGenerator.generateTableInfoSql(databaseType, true, text);

		try (Connection conn = datasourceName != null && !datasourceName.trim().isEmpty()
				? dataSourceService.getConnection(datasourceName) : dataSourceService.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, "%" + text + "%");
			try (ResultSet rs = ps.executeQuery()) {
				List<TableMeta> tableMetaList = new java.util.ArrayList<>();
				while (rs.next()) {
					TableMeta meta = new TableMeta();
					meta.setTableName(rs.getString("TABLE_NAME"));
					meta.setTableComment(rs.getString("TABLE_COMMENT"));
					meta.setColumns(new java.util.ArrayList<>());
					meta.setIndexes(new java.util.ArrayList<>());
					tableMetaList.add(meta);
				}

				// 获取每个表的列信息
				for (TableMeta tableMeta : tableMetaList) {
					String columnSql = DatabaseSqlGenerator.generateColumnInfoSql(databaseType, "?");
					try (PreparedStatement columnPs = conn.prepareStatement(columnSql)) {
						columnPs.setString(1, tableMeta.getTableName());
						try (ResultSet columnRs = columnPs.executeQuery()) {
							while (columnRs.next()) {
								ColumnMeta columnMeta = new ColumnMeta();
								columnMeta.setColumnName(columnRs.getString("COLUMN_NAME"));
								columnMeta.setColumnType(columnRs.getString("COLUMN_TYPE"));
								columnMeta.setColumnLength(columnRs.getObject("CHARACTER_MAXIMUM_LENGTH") == null ? null
										: columnRs.getObject("CHARACTER_MAXIMUM_LENGTH").toString());
								columnMeta.setColumnComment(columnRs.getString("COLUMN_COMMENT"));
								columnMeta.setDefaultValue(columnRs.getString("COLUMN_DEFAULT"));
								columnMeta.setNotNull("NO".equals(columnRs.getString("IS_NULLABLE")));
								columnMeta.setIndexes(new java.util.ArrayList<>());
								tableMeta.getColumns().add(columnMeta);
							}
						}
					}
				}

				ObjectMapper objectMapper = new ObjectMapper();
				String json = objectMapper.writeValueAsString(tableMetaList);
				log.info("GetTableNameAction completed successfully, datasourceName={}, found {} tables",
						datasourceName, tableMetaList.size());
				String resultContent = "Datasource: " + (datasourceName != null ? datasourceName : "default") + "\n"
						+ json;
				return new ToolExecuteResult(resultContent);
			}
		}
		catch (Exception e) {
			log.error("GetTableNameAction failed with exception, datasourceName={}, error={}", datasourceName,
					e.getMessage(), e);
			return new ToolExecuteResult("Datasource: " + (datasourceName != null ? datasourceName : "default")
					+ "\n执行查询时出错: " + e.getMessage());
		}
	}

}

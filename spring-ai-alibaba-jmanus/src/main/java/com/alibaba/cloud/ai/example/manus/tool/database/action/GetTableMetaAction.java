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

import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.tool.database.DatabaseRequest;
import com.alibaba.cloud.ai.example.manus.tool.database.DataSourceService;
import com.alibaba.cloud.ai.example.manus.tool.database.meta.TableMeta;
import com.alibaba.cloud.ai.example.manus.tool.database.meta.ColumnMeta;
import com.alibaba.cloud.ai.example.manus.tool.database.meta.IndexMeta;
import com.alibaba.cloud.ai.example.manus.tool.database.sql.DatabaseSqlGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class GetTableMetaAction extends AbstractDatabaseAction {

	private static final Logger log = LoggerFactory.getLogger(GetTableMetaAction.class);

	@Override
	public ToolExecuteResult execute(DatabaseRequest request, DataSourceService dataSourceService) {
		String text = request.getText();
		String datasourceName = request.getDatasourceName();

		List<TableMeta> tableMetaList = new ArrayList<>();
		Map<String, TableMeta> tableMetaMap = new LinkedHashMap<>();
		// 1. 获取表信息
		String databaseType = datasourceName != null && !datasourceName.trim().isEmpty()
				? dataSourceService.getDataSourceType(datasourceName) : dataSourceService.getDataSourceType();

		boolean fuzzy = text != null && !text.trim().isEmpty();
		String tableSql = DatabaseSqlGenerator.generateTableInfoSql(databaseType, fuzzy, text);

		try (Connection conn = datasourceName != null && !datasourceName.trim().isEmpty()
				? dataSourceService.getConnection(datasourceName) : dataSourceService.getConnection();
				PreparedStatement ps = conn.prepareStatement(tableSql)) {
			if (fuzzy) {
				ps.setString(1, "%" + text + "%");
			}
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					String tableName = rs.getString("TABLE_NAME");
					String tableComment = rs.getString("TABLE_COMMENT");
					TableMeta tableMeta = new TableMeta();
					tableMeta.setTableName(tableName);
					tableMeta.setTableComment(tableComment);
					tableMeta.setColumns(new ArrayList<>());
					tableMeta.setIndexes(new ArrayList<>());
					tableMetaMap.put(tableName, tableMeta);
				}
			}
		}
		catch (SQLException e) {
			log.error("GetTableMetaAction failed to fetch table info, datasourceName={}, error={}", datasourceName,
					e.getMessage(), e);
			return new ToolExecuteResult("Datasource: " + (datasourceName != null ? datasourceName : "default")
					+ "\n获取表信息出错: " + e.getMessage());
		}
		if (tableMetaMap.isEmpty()) {
			log.warn("GetTableMetaAction found no tables, datasourceName={}, fuzzy={}", datasourceName, fuzzy);
			return new ToolExecuteResult(
					"Datasource: " + (datasourceName != null ? datasourceName : "default") + "\n未找到符合条件的表");
		}
		// 2. 获取字段信息
		StringBuilder inClause = new StringBuilder();
		for (int i = 0; i < tableMetaMap.size(); i++) {
			inClause.append("?");
			if (i < tableMetaMap.size() - 1)
				inClause.append(",");
		}
		String columnSql = DatabaseSqlGenerator.generateColumnInfoSql(databaseType, inClause.toString());
		try (Connection conn = datasourceName != null && !datasourceName.trim().isEmpty()
				? dataSourceService.getConnection(datasourceName) : dataSourceService.getConnection();
				PreparedStatement ps = conn.prepareStatement(columnSql)) {
			int idx = 1;
			for (String tableName : tableMetaMap.keySet()) {
				ps.setString(idx++, tableName);
			}
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					String tableName = rs.getString("TABLE_NAME");
					TableMeta tableMeta = tableMetaMap.get(tableName);
					if (tableMeta == null)
						continue;
					ColumnMeta columnMeta = new ColumnMeta();
					columnMeta.setColumnName(rs.getString("COLUMN_NAME"));
					columnMeta.setColumnType(rs.getString("COLUMN_TYPE"));
					columnMeta.setColumnLength(rs.getObject("CHARACTER_MAXIMUM_LENGTH") == null ? null
							: rs.getObject("CHARACTER_MAXIMUM_LENGTH").toString());
					columnMeta.setColumnComment(rs.getString("COLUMN_COMMENT"));
					columnMeta.setDefaultValue(rs.getString("COLUMN_DEFAULT"));
					columnMeta.setNotNull("NO".equals(rs.getString("IS_NULLABLE")));
					columnMeta.setIndexes(new ArrayList<>());
					tableMeta.getColumns().add(columnMeta);
				}
			}
		}
		catch (SQLException e) {
			log.error("GetTableMetaAction failed to fetch column info, datasourceName={}, error={}", datasourceName,
					e.getMessage(), e);
			return new ToolExecuteResult("Datasource: " + (datasourceName != null ? datasourceName : "default")
					+ "\n获取字段信息出错: " + e.getMessage());
		}
		// 3. 获取索引信息
		String indexSql = DatabaseSqlGenerator.generateIndexInfoSql(databaseType, inClause.toString());
		try (Connection conn = datasourceName != null && !datasourceName.trim().isEmpty()
				? dataSourceService.getConnection(datasourceName) : dataSourceService.getConnection();
				PreparedStatement ps = conn.prepareStatement(indexSql)) {
			int idx = 1;
			for (String tableName : tableMetaMap.keySet()) {
				ps.setString(idx++, tableName);
			}
			try (ResultSet rs = ps.executeQuery()) {
				// Map<tableName, Map<indexName, IndexMeta>>
				Map<String, Map<String, IndexMeta>> tableIndexMap = new HashMap<>();
				while (rs.next()) {
					String tableName = rs.getString("TABLE_NAME");
					String indexName = rs.getString("INDEX_NAME");
					String columnName = rs.getString("COLUMN_NAME");
					String indexType = rs.getString("INDEX_TYPE");
					TableMeta tableMeta = tableMetaMap.get(tableName);
					if (tableMeta == null)
						continue;
					// Find the column
					ColumnMeta refColumn = null;
					for (ColumnMeta col : tableMeta.getColumns()) {
						if (col.getColumnName().equals(columnName)) {
							refColumn = col;
							break;
						}
					}
					// Group index by tableName+indexName
					tableIndexMap.putIfAbsent(tableName, new HashMap<>());
					Map<String, IndexMeta> indexMap = tableIndexMap.get(tableName);
					IndexMeta indexMeta = indexMap.get(indexName);
					if (indexMeta == null) {
						indexMeta = new IndexMeta();
						indexMeta.setIndexName(indexName);
						indexMeta.setIndexType(indexType);
						indexMeta.setRefColumnNames(new ArrayList<>());
						indexMap.put(indexName, indexMeta);
						tableMeta.getIndexes().add(indexMeta);
					}
					indexMeta.getRefColumnNames().add(columnName);
					if (refColumn != null) {
						refColumn.getIndexes().add(indexMeta);
					}
				}
			}
		}
		catch (SQLException e) {
			log.error("GetTableMetaAction failed to fetch index info, datasourceName={}, error={}", datasourceName,
					e.getMessage(), e);
			return new ToolExecuteResult("Datasource: " + (datasourceName != null ? datasourceName : "default")
					+ "\n获取索引信息出错: " + e.getMessage());
		}
		tableMetaList.addAll(tableMetaMap.values());
		// 4. 返回结构化对象
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			String json = objectMapper.writeValueAsString(tableMetaList);
			log.info("GetTableMetaAction completed successfully, datasourceName={}, found {} tables", datasourceName,
					tableMetaList.size());
			String resultContent = "Datasource: " + (datasourceName != null ? datasourceName : "default") + "\n" + json;
			return new ToolExecuteResult(resultContent);
		}
		catch (JsonProcessingException e) {
			log.error("GetTableMetaAction failed to serialize result, datasourceName={}, error={}", datasourceName,
					e.getMessage(), e);
			return new ToolExecuteResult("Datasource: " + (datasourceName != null ? datasourceName : "default")
					+ "\n结果序列化出错: " + e.getMessage());
		}
	}

}

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

package com.alibaba.cloud.ai.manus.tool.database.action;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.alibaba.cloud.ai.manus.tool.database.DatabaseRequest;
import com.alibaba.cloud.ai.manus.tool.database.DataSourceService;
import com.alibaba.cloud.ai.manus.tool.database.sql.DatabaseSqlGenerator;
import com.alibaba.cloud.ai.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.manus.tool.database.meta.IndexMeta;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetTableIndexAction extends AbstractDatabaseAction {

	private final ObjectMapper objectMapper;

	private static final Logger log = LoggerFactory.getLogger(GetTableIndexAction.class);

	public GetTableIndexAction(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public ToolExecuteResult execute(DatabaseRequest request, DataSourceService dataSourceService) {
		String text = request.getText();
		String datasourceName = request.getDatasourceName();

		if (text == null || text.trim().isEmpty()) {
			log.warn("GetTableIndexAction failed: missing text parameter, datasourceName={}", datasourceName);
			return new ToolExecuteResult("Datasource: " + (datasourceName != null ? datasourceName : "default")
					+ "\nMissing query statement");
		}
		String[] tableNames = text.split(",");
		StringBuilder inClause = new StringBuilder();
		for (int i = 0; i < tableNames.length; i++) {
			inClause.append("?");
			if (i < tableNames.length - 1)
				inClause.append(",");
		}
		// Get database type
		String databaseType = datasourceName != null && !datasourceName.trim().isEmpty()
				? dataSourceService.getDataSourceType(datasourceName) : dataSourceService.getDataSourceType();

		// Generate SQL based on database type
		String sql = DatabaseSqlGenerator.generateIndexInfoSql(databaseType, inClause.toString());

		try (Connection conn = datasourceName != null && !datasourceName.trim().isEmpty()
				? dataSourceService.getConnection(datasourceName) : dataSourceService.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {
			for (int i = 0; i < tableNames.length; i++) {
				ps.setString(i + 1, tableNames[i].trim());
			}
			try (ResultSet rs = ps.executeQuery()) {
				// Map<tableName, Map<indexName, IndexMeta>>
				java.util.Map<String, java.util.Map<String, IndexMeta>> tableIndexMap = new java.util.HashMap<>();
				while (rs.next()) {
					String tableName = rs.getString("TABLE_NAME");
					String indexName = rs.getString("INDEX_NAME");
					String columnName = rs.getString("COLUMN_NAME");
					String indexType = rs.getString("INDEX_TYPE");
					tableIndexMap.putIfAbsent(tableName, new java.util.HashMap<>());
					java.util.Map<String, IndexMeta> indexMap = tableIndexMap.get(tableName);
					IndexMeta indexMeta = indexMap.get(indexName);
					if (indexMeta == null) {
						indexMeta = new IndexMeta();
						indexMeta.setIndexName(indexName);
						indexMeta.setIndexType(indexType);
						indexMeta.setRefColumnNames(new java.util.ArrayList<>());
						indexMap.put(indexName, indexMeta);
					}
					indexMeta.getRefColumnNames().add(columnName);
				}
				// Merge all indexes from all tables into one list
				java.util.List<IndexMeta> allIndexes = new java.util.ArrayList<>();
				for (java.util.Map<String, IndexMeta> indexMap : tableIndexMap.values()) {
					allIndexes.addAll(indexMap.values());
				}
				String json = objectMapper.writeValueAsString(allIndexes);
				log.info("GetTableIndexAction completed successfully, datasourceName={}, found {} indexes",
						datasourceName, allIndexes.size());
				String resultContent = "Datasource: " + (datasourceName != null ? datasourceName : "default") + "\n"
						+ json;
				return new ToolExecuteResult(resultContent);
			}
		}
		catch (Exception e) {
			log.error("GetTableIndexAction failed with exception, datasourceName={}, error={}", datasourceName,
					e.getMessage(), e);
			return new ToolExecuteResult("Datasource: " + (datasourceName != null ? datasourceName : "default")
					+ "\nError executing query: " + e.getMessage());
		}
	}

}

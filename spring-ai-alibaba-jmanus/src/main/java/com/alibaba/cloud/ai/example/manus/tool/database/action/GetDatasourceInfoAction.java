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
import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.tool.database.DataSourceService;
import com.alibaba.cloud.ai.example.manus.tool.database.DatabaseRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GetDatasourceInfoAction extends AbstractDatabaseAction {

	private static final Logger log = LoggerFactory.getLogger(GetDatasourceInfoAction.class);

	@Override
	public ToolExecuteResult execute(DatabaseRequest request, DataSourceService dataSourceService) {
		String datasourceName = request.getDatasourceName(); // Use datasourceName field

		try {
			Map<String, Object> result = new HashMap<>();

			if (datasourceName == null || datasourceName.trim().isEmpty()) {
				// If no specific datasource name provided, return all available
				// datasources
				result.put("availableDatasources", dataSourceService.getDataSourceNames());
				result.put("datasourceTypes", dataSourceService.getDataSourceTypeMap());
				result.put("totalCount", dataSourceService.getDataSourceCount());
				result.put("message", "Available datasources listed");
				log.info("GetDatasourceInfoAction listed all datasources, count={}",
						dataSourceService.getDataSourceCount());
			}
			else {
				// Get specific datasource info
				if (!dataSourceService.hasDataSource(datasourceName)) {
					log.warn("GetDatasourceInfoAction failed: datasource not found, datasourceName={}", datasourceName);
					return new ToolExecuteResult("Datasource: " + (datasourceName != null ? datasourceName : "default")
							+ "\nDatasource '" + datasourceName + "' not found");
				}

				try (Connection conn = dataSourceService.getConnection(datasourceName)) {
					DatabaseMetaData metaData = conn.getMetaData();

					Map<String, Object> datasourceInfo = new HashMap<>();
					datasourceInfo.put("datasourceName", datasourceName);
					datasourceInfo.put("type", dataSourceService.getDataSourceType(datasourceName));
					datasourceInfo.put("databaseProductName", metaData.getDatabaseProductName());
					datasourceInfo.put("databaseProductVersion", metaData.getDatabaseProductVersion());
					datasourceInfo.put("driverName", metaData.getDriverName());
					datasourceInfo.put("driverVersion", metaData.getDriverVersion());
					datasourceInfo.put("url", metaData.getURL());
					datasourceInfo.put("userName", metaData.getUserName());
					datasourceInfo.put("catalogTerm", metaData.getCatalogTerm());
					datasourceInfo.put("schemaTerm", metaData.getSchemaTerm());

					result.put("datasourceInfo", datasourceInfo);
					result.put("message", "Datasource information retrieved successfully");
					log.info("GetDatasourceInfoAction retrieved specific datasource info, datasourceName={}",
							datasourceName);
				}
			}

			ObjectMapper objectMapper = new ObjectMapper();
			String json = objectMapper.writeValueAsString(result);
			String resultContent = "Datasource: " + (datasourceName != null ? datasourceName : "default") + "\n" + json;
			return new ToolExecuteResult(resultContent);

		}
		catch (Exception e) {
			log.error("GetDatasourceInfoAction failed with exception, datasourceName={}, error={}", datasourceName,
					e.getMessage(), e);
			return new ToolExecuteResult("Datasource: " + (datasourceName != null ? datasourceName : "default")
					+ "\n获取数据源信息时出错: " + e.getMessage());
		}
	}

}

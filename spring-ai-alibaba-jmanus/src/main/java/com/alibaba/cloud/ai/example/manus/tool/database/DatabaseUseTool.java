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
package com.alibaba.cloud.ai.example.manus.tool.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.api.OpenAiApi;

import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.tool.AbstractBaseTool;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.tool.database.action.ExecuteSqlAction;
import com.alibaba.cloud.ai.example.manus.tool.database.action.GetTableNameAction;
import com.alibaba.cloud.ai.example.manus.tool.database.action.GetTableIndexAction;
import com.alibaba.cloud.ai.example.manus.tool.database.action.GetTableMetaAction;
import com.alibaba.cloud.ai.example.manus.tool.database.action.GetDatasourceInfoAction;

import java.util.Map;

public class DatabaseUseTool extends AbstractBaseTool<DatabaseRequest> {

	private static final Logger log = LoggerFactory.getLogger(DatabaseUseTool.class);

	private final ManusProperties manusProperties;

	private final DataSourceService dataSourceService;

	public DatabaseUseTool(ManusProperties manusProperties, DataSourceService dataSourceService) {
		this.manusProperties = manusProperties;
		this.dataSourceService = dataSourceService;
	}

	public DataSourceService getDataSourceService() {
		return dataSourceService;
	}

	private final String PARAMETERS = """
			{
			    "oneOf": [
			        {
			            "type": "object",
			            "properties": {
			                "action": { "type": "string", "const": "execute_sql" },
			                "query": { "type": "string", "description": "要执行的SQL语句" },
			                "datasourceName": { "type": "string", "description": "数据源名称，可选" }
			            },
			            "required": ["action", "query"],
			            "additionalProperties": false
			        },
			        {
			            "type": "object",
			            "properties": {
			                "action": { "type": "string", "const": "get_table_name" },
			                "text": { "type": "string", "description": "要搜索的表中文名、表描述，仅支持单个查询" },
			                "datasourceName": { "type": "string", "description": "数据源名称，可选" }
			            },
			            "required": ["action", "text"],
			            "additionalProperties": false
			        },
			        {
			            "type": "object",
			            "properties": {
			                "action": { "type": "string", "const": "get_table_index" },
			                "text": { "type": "string", "description": "要搜索的表名" },
			                "datasourceName": { "type": "string", "description": "数据源名称，可选" }
			            },
			            "required": ["action", "text"],
			            "additionalProperties": false
			        },
			        {
			            "type": "object",
			            "properties": {
			                "action": { "type": "string", "const": "get_table_meta" },
			                "text": { "type": "string", "description": "模糊搜索表描述，留空则获取所有表" },
			                "datasourceName": { "type": "string", "description": "数据源名称，可选" }
			            },
			            "required": ["action"],
			            "additionalProperties": false
			        },
			        {
			            "type": "object",
			            "properties": {
			                "action": { "type": "string", "const": "get_datasource_info" },
			                "datasourceName": { "type": "string", "description": "数据源名称，留空则获取所有可用数据源" }
			            },
			            "required": ["action"],
			            "additionalProperties": false
			        }
			    ]
			}
			""";

	private final String name = "database_use";

	private final String description = """
			与数据库交互，执行SQL、表结构、索引、健康状态等操作。支持的操作包括：
			- 'execute_sql'：执行SQL语句
			- 'get_table_name'：根据表注释查找表名
			- 'get_table_index'：获取表索引信息
			- 'get_table_meta'：获取表结构、字段、索引的完整元信息
			- 'get_datasource_info'：获取数据源信息
			""";

	public OpenAiApi.FunctionTool getToolDefinition() {
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(description, name, PARAMETERS);
		OpenAiApi.FunctionTool functionTool = new OpenAiApi.FunctionTool(function);
		return functionTool;
	}

	@Override
	public String getServiceGroup() {
		return "default-service-group";
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getParameters() {
		return PARAMETERS;
	}

	@Override
	public Class<DatabaseRequest> getInputType() {
		return DatabaseRequest.class;
	}

	@Override
	public ToolExecuteResult run(DatabaseRequest request) {
		String action = request.getAction();
		log.info("DatabaseUseTool request: action={}", action);
		try {
			if (action == null) {
				return new ToolExecuteResult("Action parameter is required");
			}
			switch (action) {
				case "execute_sql":
					return new ExecuteSqlAction().execute(request, dataSourceService);
				case "get_table_name":
					return new GetTableNameAction().execute(request, dataSourceService);
				case "get_table_index":
					return new GetTableIndexAction().execute(request, dataSourceService);
				case "get_table_meta": {
					// 先用text查，如果没查到再查全部
					GetTableMetaAction metaAction = new GetTableMetaAction();
					ToolExecuteResult result = metaAction.execute(request, dataSourceService);
					if (result == null || result.getOutput() == null || result.getOutput().trim().isEmpty()
							|| result.getOutput().equals("[]") || result.getOutput().contains("未找到符合条件的表")) {
						DatabaseRequest allReq = new DatabaseRequest();
						allReq.setAction("get_table_meta");
						allReq.setText(null);
						result = metaAction.execute(allReq, dataSourceService);
					}
					return result;
				}
				case "get_datasource_info":
					return new GetDatasourceInfoAction().execute(request, dataSourceService);
				default:
					return new ToolExecuteResult("Unknown action: " + action);
			}
		}
		catch (Exception e) {
			log.error("Database action '" + action + "' failed", e);
			return new ToolExecuteResult("Database action '" + action + "' failed: " + e.getMessage());
		}
	}

	@Override
	public void cleanup(String planId) {
		if (planId != null) {
			log.info("Cleaning up database resources for plan: {}", planId);
			try {
				// 关闭所有数据源的连接
				dataSourceService.closeAllConnections();
				log.info("Successfully cleaned up database connections for plan: {}", planId);
			}
			catch (Exception e) {
				log.error("Failed to cleanup database resources for plan: {}", planId, e);
			}
		}
	}

	@Override
	public String getCurrentToolStateString() {
		try {
			// 获取所有数据源信息
			Map<String, String> datasourceInfo = dataSourceService.getAllDatasourceInfo();

			// 构建数据源状态信息
			StringBuilder stateBuilder = new StringBuilder();
			stateBuilder.append("\n=== Database Tool Current State ===\n");

			if (datasourceInfo.isEmpty()) {
				stateBuilder.append("No datasources configured or available.\n");
			}
			else {
				stateBuilder.append("Available datasources:\n");
				for (Map.Entry<String, String> entry : datasourceInfo.entrySet()) {
					String datasourceName = entry.getKey();
					String datasourceType = entry.getValue();
					stateBuilder.append(String.format("  - %s (%s)\n", datasourceName, datasourceType));
				}

				// 获取默认数据源信息
				try {
					String defaultType = dataSourceService.getDataSourceType();
					stateBuilder.append(String.format("\nDefault datasource type: %s\n", defaultType));
				}
				catch (Exception e) {
					stateBuilder.append("\nDefault datasource: Not available\n");
				}

				// 测试连接状态
				stateBuilder.append("\nConnection status:\n");
				for (String datasourceName : datasourceInfo.keySet()) {
					try {
						dataSourceService.getConnection(datasourceName);
						stateBuilder.append(String.format("  - %s: Connected ✓\n", datasourceName));
					}
					catch (Exception e) {
						stateBuilder.append(
								String.format("  - %s: Connection failed ✗ (%s)\n", datasourceName, e.getMessage()));
					}
				}
			}

			stateBuilder.append("\n=== End Database Tool State ===\n");
			return stateBuilder.toString();

		}
		catch (Exception e) {
			log.error("Failed to get database tool state", e);
			return String.format("Database tool state error: %s", e.getMessage());
		}
	}

	public static DatabaseUseTool getInstance(DataSourceService dataSourceService) {
		return new DatabaseUseTool(null, dataSourceService);
	}

}

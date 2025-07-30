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
package com.alibaba.cloud.ai.example.manus.tool.tableProcessor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.example.manus.tool.AbstractBaseTool;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.api.OpenAiApi;

public class TableProcessorTool extends AbstractBaseTool<TableProcessorTool.TableInput> {

	private static final Logger log = LoggerFactory.getLogger(TableProcessorTool.class);

	/**
	 * Internal input class for defining input parameters of table processing tool
	 */
	public static class TableInput {

		private String action;

		@com.fasterxml.jackson.annotation.JsonProperty("file_path")
		private String filePath;

		@com.fasterxml.jackson.annotation.JsonProperty("table_name")
		private String tableName;

		private List<String> headers;

		private List<String> data;

		private List<String> keywords;

		@com.fasterxml.jackson.annotation.JsonProperty("row_indices")
		private List<Integer> rowIndices;

		public TableInput() {
		}

		public String getAction() {
			return action;
		}

		public void setAction(String action) {
			this.action = action;
		}

		public String getFilePath() {
			return filePath;
		}

		public void setFilePath(String filePath) {
			this.filePath = filePath;
		}

		public String getTableName() {
			return tableName;
		}

		public void setTableName(String tableName) {
			this.tableName = tableName;
		}

		public List<String> getHeaders() {
			return headers;
		}

		public void setHeaders(List<String> headers) {
			this.headers = headers;
		}

		public List<String> getData() {
			return data;
		}

		public void setData(List<String> data) {
			this.data = data;
		}

		public List<String> getKeywords() {
			return keywords;
		}

		public void setKeywords(List<String> keywords) {
			this.keywords = keywords;
		}

		public List<Integer> getRowIndices() {
			return rowIndices;
		}

		public void setRowIndices(List<Integer> rowIndices) {
			this.rowIndices = rowIndices;
		}
	}

	private final TableProcessingService tableProcessingService;

	private final ObjectMapper objectMapper;

	public TableProcessorTool(TableProcessingService tableProcessingService, ObjectMapper objectMapper) {
		this.tableProcessingService = tableProcessingService;
		this.objectMapper = objectMapper;
	}

	private final String PARAMETERS = """
			{
			  "oneOf": [
				{
				  "type": "object",
				  "properties": {
					"action": {
					  "type": "string",
					  "const": "create_table"
					},
					"file_path": {
					  "type": "string",
					  "description": "要创建的表格文件路径（相对路径）"
					},
					"sheet_name": {
					  "type": "string",
					  "description": "工作表名称"
					},
					"headers": {
					  "type": "array",
					  "items": {
						"type": "string"
					  },
					  "description": "表头列表（不包括ID列，ID列会自动添加）"
					}
				  },
				  "required": ["action", "file_path", "headers"],
				  "additionalProperties": false
				},
				{
				  "type": "object",
				  "properties": {
					"action": {
					  "type": "string",
					  "const": "get_structure"
					},
					"file_path": {
					  "type": "string",
					  "description": "表格文件路径"
					}
				  },
				  "required": ["action", "file_path"],
				  "additionalProperties": false
				},
				{
				  "type": "object",
				  "properties": {
					"action": {
					  "type": "string",
					  "const": "write_data"
					},
					"file_path": {
					  "type": "string",
					  "description": "表格文件路径"
					},
					"data": {
					  "type": "array",
					  "items": {
						"type": "string"
					  },
					  "description": "要写入的数据列表，必须与表头数量一致"
					}
				  },
				  "required": ["action", "file_path", "data"],
				  "additionalProperties": false
				},
				{
				  "type": "object",
				  "properties": {
					"action": {
					  "type": "string",
					  "const": "write_multiple_rows"
					},
					"file_path": {
					  "type": "string",
					  "description": "表格文件路径"
					},
					"data": {
					  "type": "array",
					  "items": {
						"type": "array",
						"items": {
						  "type": "string"
						}
					  },
					  "description": "要写入的多行数据列表，每行数据必须与表头数量一致"
					}
				  },
				  "required": ["action", "file_path", "data"],
				  "additionalProperties": false
				},
				{
				  "type": "object",
				  "properties": {
					"action": {
					  "type": "string",
					  "const": "search_rows"
					},
					"file_path": {
					  "type": "string",
					  "description": "表格文件路径"
					},
					"keywords": {
					  "type": "array",
					  "items": {
						"type": "string"
					  },
					  "description": "搜索关键词列表"
					}
				  },
				  "required": ["action", "file_path", "keywords"],
				  "additionalProperties": false
				},
				{
				  "type": "object",
				  "properties": {
					"action": {
					  "type": "string",
					  "const": "delete_rows"
					},
					"file_path": {
					  "type": "string",
					  "description": "要删除行的表格文件路径"
					},
					"row_indices": {
					  "type": "array",
					  "items": {
						"type": "integer"
					  },
					  "description": "要删除的行索引列表（从0开始）"
					}
				  },
				  "required": ["action", "file_path", "row_indices"],
				  "additionalProperties": false
				}
			  ]
			}
			""";

	private static final String TOOL_NAME = "table_processor";

	private final String TOOL_DESCRIPTION = """
			表格处理工具，用于创建和操作表格文件（支持Excel和CSV格式）。
			支持的操作：
			- create_table: 创建新表格，接受文件路径、工作表名和表头列表作为参数，自动添加ID列为第一列
			- get_structure: 获取表格结构（表头信息）
			- write_data: 将单行数据写入表格，要求数据列数与表头一致
			- write_multiple_rows: 将多行数据写入表格，要求每行数据列数与表头一致
			- search_rows: 根据关键词组在表格中查找匹配行
			- delete_rows: 根据行索引列表删除指定行

			注意事项：
			1. 文件路径应使用相对路径，使用绝对路径会报错
			2. 所有内容都以字符串形式处理
			3. ID列会自动作为第一列添加到表格中
			4. 写入数据时，数据列数必须与表头列数一致，否则会返回错误提示
			""";

	public OpenAiApi.FunctionTool getToolDefinition() {
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(TOOL_DESCRIPTION, TOOL_NAME,
				PARAMETERS);
		OpenAiApi.FunctionTool functionTool = new OpenAiApi.FunctionTool(function);
		return functionTool;
	}

	public ToolExecuteResult run(String toolInput) {
		log.info("TableProcessorTool toolInput:{}", toolInput);
		try {
			Map<String, Object> toolInputMap = objectMapper.readValue(toolInput,
					new TypeReference<Map<String, Object>>() {
					});
			String planId = this.currentPlanId;

			String action = (String) toolInputMap.get("action");
			String filePath = (String) toolInputMap.get("file_path");

			// Basic parameter validation
			if (action == null) {
				return new ToolExecuteResult("错误：action参数是必需的");
			}
			if (filePath == null) {
				return new ToolExecuteResult("错误：file_path参数是必需的");
			}

			return switch (action) {
				case "create_table" -> {
					List<String> headers = (List<String>) toolInputMap.get("headers");
					String tableName = (String) toolInputMap.get("table_name");

					if (headers == null) {
						yield new ToolExecuteResult("错误：create_table操作需要headers参数");
					}

					yield createTable(planId, filePath, tableName, headers);
				}
				case "get_structure" -> getTableStructure(planId, filePath);
				case "write_data" -> {
					List<String> data = (List<String>) toolInputMap.get("data");

					if (data == null) {
						yield new ToolExecuteResult("错误：write_data操作需要data参数");
					}

					yield writeDataToTable(planId, filePath, data);
				}
				case "write_multiple_rows" -> {
					List<List<String>> data = (List<List<String>>) toolInputMap.get("data");

					if (data == null) {
						yield new ToolExecuteResult("错误：write_multiple_rows操作需要data参数");
					}

					yield writeMultipleRowsToTable(planId, filePath, data);
				}
				case "search_rows" -> {
					List<String> keywords = (List<String>) toolInputMap.get("keywords");

					if (keywords == null) {
						yield new ToolExecuteResult("错误：search_rows操作需要keywords参数");
					}

					yield searchRows(planId, filePath, keywords);
				}
				case "delete_rows" -> {
					List<Integer> rowIndices = (List<Integer>) toolInputMap.get("row_indices");

					if (rowIndices == null) {
						yield new ToolExecuteResult("错误：delete_rows操作需要row_indices参数");
					}

					yield deleteRowsByList(planId, filePath, rowIndices);
				}
				default -> {
					tableProcessingService.updateFileState(planId, filePath, "Error: Unknown action");
					yield new ToolExecuteResult(
							"未知操作: " + action + "。支持的操作: create_table, get_structure, write_data, search_rows, delete_rows");
				}
			};
		}
		catch (Exception e) {
			String planId = this.currentPlanId;
			tableProcessingService.updateFileState(planId, tableProcessingService.getCurrentFilePath(planId),
					"Error: " + e.getMessage());
			return new ToolExecuteResult("工具执行失败: " + e.getMessage());
		}
	}

	/**
	 * 执行表格处理操作，接受强类型输入对象
	 */
	@Override
	public ToolExecuteResult run(TableInput input) {
		log.info("TableProcessorTool input: action={}, filePath={}", input.getAction(), input.getFilePath());
		try {
			String planId = this.currentPlanId;
			String action = input.getAction();
			String filePath = input.getFilePath();

			// Basic parameter validation
			if (action == null) {
				return new ToolExecuteResult("错误：action参数是必需的");
			}
			if (filePath == null) {
				return new ToolExecuteResult("错误：file_path参数是必需的");
			}

			return switch (action) {
				case "create_table" -> {
					List<String> headers = input.getHeaders();
					String tableName = input.getTableName();

					if (headers == null) {
						yield new ToolExecuteResult("错误：create_table操作需要headers参数");
					}

					yield createTable(planId, filePath, tableName, headers);
				}
				case "get_structure" -> getTableStructure(planId, filePath);
				case "write_data" -> {
					List<String> data = input.getData();

					if (data == null) {
						yield new ToolExecuteResult("错误：write_data操作需要data参数");
					}

					yield writeDataToTable(planId, filePath, data);
				}
				case "search_rows" -> {
					List<String> keywords = input.getKeywords();

					if (keywords == null) {
						yield new ToolExecuteResult("错误：search_rows操作需要keywords参数");
					}

					yield searchRows(planId, filePath, keywords);
				}
				case "delete_rows" -> {
					List<Integer> rowIndices = input.getRowIndices();

					if (rowIndices == null) {
						yield new ToolExecuteResult("错误：delete_rows操作需要row_indices参数");
					}

					yield deleteRowsByList(planId, filePath, rowIndices);
				}
				default -> {
					tableProcessingService.updateFileState(planId, filePath, "Error: Unknown action");
					yield new ToolExecuteResult(
							"未知操作: " + action + "。支持的操作: create_table, get_structure, write_data, search_rows, delete_rows");
				}
			};
		}
		catch (Exception e) {
			String planId = this.currentPlanId;
			tableProcessingService.updateFileState(planId, tableProcessingService.getCurrentFilePath(planId),
					"Error: " + e.getMessage());
			return new ToolExecuteResult("工具执行失败: " + e.getMessage());
		}
	}

	private ToolExecuteResult createTable(String planId, String filePath, String tableName, List<String> headers) {
		try {
			// Check file type
			if (!tableProcessingService.isSupportedFileType(filePath)) {
				tableProcessingService.updateFileState(planId, filePath, "Error: Unsupported file type");
				return new ToolExecuteResult("不支持的文件类型。仅支持Excel(.xlsx,.xls)和CSV(.csv)文件。");
			}

			tableProcessingService.createTable(planId, filePath, tableName, headers);
			return new ToolExecuteResult("表格创建成功: " + filePath);
		}
		catch (IOException e) {
			tableProcessingService.updateFileState(planId, filePath, "Error: " + e.getMessage());
			return new ToolExecuteResult("创建表格失败: " + e.getMessage());
		}
	}

	private ToolExecuteResult getTableStructure(String planId, String filePath) {
		try {
			List<String> headers = tableProcessingService.getTableStructure(planId, filePath);
			tableProcessingService.updateFileState(planId, filePath, "Success: Retrieved table structure");
			return new ToolExecuteResult("表头信息: " + headers.toString());
		}
		catch (IOException e) {
			tableProcessingService.updateFileState(planId, filePath, "Error: " + e.getMessage());
			return new ToolExecuteResult("获取表结构失败: " + e.getMessage());
		}
	}

	private ToolExecuteResult writeDataToTable(String planId, String filePath, List<String> data) {
		try {
			tableProcessingService.writeDataToTable(planId, filePath, data);
			return new ToolExecuteResult("数据写入成功");
		}
		catch (IOException e) {
			tableProcessingService.updateFileState(planId, filePath, "Error: " + e.getMessage());
			return new ToolExecuteResult("写入数据失败: " + e.getMessage());
		}
	}

	private ToolExecuteResult writeMultipleRowsToTable(String planId, String filePath, List<List<String>> data) {
		try {
			tableProcessingService.writeMultipleRowsToTable(planId, filePath, data);
			return new ToolExecuteResult("多行数据写入成功");
		}
		catch (IOException e) {
			tableProcessingService.updateFileState(planId, filePath, "Error: " + e.getMessage());
			return new ToolExecuteResult("写入多行数据失败: " + e.getMessage());
		}
	}

	private ToolExecuteResult searchRows(String planId, String filePath, List<String> keywords) {
		try {
			List<List<String>> matchingRows = tableProcessingService.searchRows(planId, filePath, keywords);
			tableProcessingService.updateFileState(planId, filePath, "Success: Rows searched");
			if (matchingRows.isEmpty()) {
				return new ToolExecuteResult("未找到匹配的行");
			}
			else {
				StringBuilder result = new StringBuilder("找到匹配的行:\n");
				for (int i = 0; i < matchingRows.size(); i++) {
					result.append(String.format("第%d行: %s\n", i + 1, matchingRows.get(i).toString()));
				}
				return new ToolExecuteResult(result.toString());
			}
		}
		catch (IOException e) {
			tableProcessingService.updateFileState(planId, filePath, "Error: " + e.getMessage());
			return new ToolExecuteResult("搜索行失败: " + e.getMessage());
		}
	}

	private ToolExecuteResult deleteRowsByList(String planId, String filePath, List<Integer> rowIndices) {
		try {
			tableProcessingService.deleteRowsByList(planId, filePath, rowIndices);
			return new ToolExecuteResult("删除成功，已删除" + rowIndices.size() + "行");
		}
		catch (IOException e) {
			tableProcessingService.updateFileState(planId, filePath, "Error: " + e.getMessage());
			return new ToolExecuteResult("删除行失败: " + e.getMessage());
		}
	}

	@Override
	public String getCurrentToolStateString() {
		String planId = this.currentPlanId;
		try {
			Path workingDir = tableProcessingService.getAbsolutePath(planId, "");
			return String.format(
					"""
							Current Table Processing State:
							- working Directory:
							%s

							- Operations are automatically handled (no manual file opening/closing required)
							- All file operations (open, save) are performed automatically
							- Supported file types: xlsx, xls, csv

							- Last Operation Result:
							%s
							""",
					workingDir.toString(), tableProcessingService.getLastOperationResult(planId).isEmpty()
							? "No operation performed yet" : tableProcessingService.getLastOperationResult(planId));
		}
		catch (Exception e) {
			return String.format("""
					Current Table Processing State:
					- Error getting working directory: %s

					- Last Operation Result:
					%s
					""", e.getMessage(), tableProcessingService.getLastOperationResult(planId).isEmpty()
					? "No operation performed yet" : tableProcessingService.getLastOperationResult(planId));
		}
	}

	@Override
	public String getName() {
		return TOOL_NAME;
	}

	@Override
	public String getDescription() {
		return TOOL_DESCRIPTION;
	}

	@Override
	public String getParameters() {
		return PARAMETERS;
	}

	@Override
	public Class<TableInput> getInputType() {
		return TableInput.class;
	}

	@Override
	public void cleanup(String planId) {
		if (planId != null) {
			log.info("Cleaning up table processing resources for plan: {}", planId);
			tableProcessingService.cleanupPlanDirectory(planId);
		}
	}

	@Override
	public String getServiceGroup() {
		return "default-service-group";
	}
}
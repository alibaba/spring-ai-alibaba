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
package com.alibaba.cloud.ai.manus.tool.tableProcessor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.alibaba.cloud.ai.manus.tool.AbstractBaseTool;
import com.alibaba.cloud.ai.manus.tool.code.ToolExecuteResult;

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

		@com.fasterxml.jackson.annotation.JsonProperty("multiple_rows_data")
		private List<List<String>> multipleRowsData;

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

		public List<List<String>> getMultipleRowsData() {
			return multipleRowsData;
		}

		public void setMultipleRowsData(List<List<String>> multipleRowsData) {
			this.multipleRowsData = multipleRowsData;
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

	public TableProcessorTool(TableProcessingService tableProcessingService) {
		this.tableProcessingService = tableProcessingService;
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
					  "description": "Path to the table file to be created (relative path)"
					},
					"sheet_name": {
					  "type": "string",
					  "description": "Worksheet name"
					},
					"headers": {
					  "type": "array",
					  "items": {
						"type": "string"
					  },
					  "description": "List of headers (excluding ID column, which will be added automatically)"
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
					  "description": "Path to the table file"
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
					  "const": "write_multiple_rows"
					},
					"file_path": {
					  "type": "string",
					  "description": "Path to the table file"
					},
					"multiple_rows_data": {
					  "type": "array",
					  "items": {
						"type": "array",
						"items": {
						  "type": "string"
						}
					  },
					  "description": "List of multiple rows data to write, each row data must match the number of headers"
					}
				  },
				  "required": ["action", "file_path", "multiple_rows_data"],
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
					  "description": "Path to the table file"
					},
					"keywords": {
					  "type": "array",
					  "items": {
						"type": "string"
					  },
					  "description": "List of search keywords"
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
					  "description": "Path to the table file from which rows will be deleted"
					},
					"row_indices": {
					  "type": "array",
					  "items": {
						"type": "integer"
					  },
					  "description": "List of row indices to delete (starting from 0)"
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
			Table processing tool for creating and operating table files (supporting Excel and CSV formats).
			Supported operations:
			- create_table: Create a new table, accepting file path, worksheet name, and headers list as parameters, automatically adding ID column as the first column
			- get_structure: Get table structure (header information)
			- write_multiple_rows: Write multiple rows of data to the table, requiring the number of data columns to match the headers
			- search_rows: Search for matching rows in the table based on keyword groups
			- delete_rows: Delete specified rows based on row index list

			Important notes:
			1. File paths should use relative paths, using absolute paths will cause errors
			2. All content is processed as strings
			3. ID column will be automatically added as the first column in the table
			4. When writing data, the number of data columns must match the number of header columns, otherwise an error message will be returned
			""";

	public OpenAiApi.FunctionTool getToolDefinition() {
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(TOOL_DESCRIPTION, TOOL_NAME,
				PARAMETERS);
		OpenAiApi.FunctionTool functionTool = new OpenAiApi.FunctionTool(function);
		return functionTool;
	}

	/**
	 * Execute table processing operations with strongly typed input object
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
				return new ToolExecuteResult("Error: action parameter is required");
			}
			if (filePath == null) {
				return new ToolExecuteResult("Error: file_path parameter is required");
			}

			return switch (action) {
				case "create_table" -> {
					List<String> headers = input.getHeaders();
					String tableName = input.getTableName();

					if (headers == null) {
						yield new ToolExecuteResult("Error: headers parameter is required for create_table operation");
					}

					yield createTable(planId, filePath, tableName, headers);
				}
				case "get_structure" -> getTableStructure(planId, filePath);
				case "write_multiple_rows" -> {
					// Get data as 2D array
					List<List<String>> multipleRows = input.getMultipleRowsData();
					if (multipleRows == null) {
						yield new ToolExecuteResult(
								"Error: multiple_rows_data parameter (2D array format) is required for write_multiple_rows operation");
					}

					yield writeMultipleRowsToTable(planId, filePath, multipleRows);
				}
				case "search_rows" -> {
					List<String> keywords = input.getKeywords();

					if (keywords == null) {
						yield new ToolExecuteResult("Error: keywords parameter is required for search_rows operation");
					}

					yield searchRows(planId, filePath, keywords);
				}
				case "delete_rows" -> {
					List<Integer> rowIndices = input.getRowIndices();

					if (rowIndices == null) {
						yield new ToolExecuteResult(
								"Error: row_indices parameter is required for delete_rows operation");
					}

					yield deleteRowsByList(planId, filePath, rowIndices);
				}
				default -> {
					tableProcessingService.updateFileState(planId, filePath, "Error: Unknown action");
					yield new ToolExecuteResult("Unknown operation: " + action
							+ ". Supported operations: create_table, get_structure, write_multiple_rows, search_rows, delete_rows");
				}
			};
		}
		catch (Exception e) {
			String planId = this.currentPlanId;
			tableProcessingService.updateFileState(planId, tableProcessingService.getCurrentFilePath(planId),
					"Error: " + e.getMessage());
			return new ToolExecuteResult("Tool execution failed: " + e.getMessage());
		}
	}

	private ToolExecuteResult createTable(String planId, String filePath, String tableName, List<String> headers) {
		try {
			// Check file type
			if (!tableProcessingService.isSupportedFileType(filePath)) {
				tableProcessingService.updateFileState(planId, filePath, "Error: Unsupported file type");
				return new ToolExecuteResult(
						"Unsupported file type. Only Excel (.xlsx, .xls) and CSV (.csv) files are supported.");
			}

			tableProcessingService.createTable(planId, filePath, tableName, headers);
			return new ToolExecuteResult("Table created successfully: " + filePath);
		}
		catch (IOException e) {
			tableProcessingService.updateFileState(planId, filePath, "Error: " + e.getMessage());
			return new ToolExecuteResult("Failed to create table: " + e.getMessage());
		}
	}

	private ToolExecuteResult getTableStructure(String planId, String filePath) {
		try {
			List<String> headers = tableProcessingService.getTableStructure(planId, filePath);
			tableProcessingService.updateFileState(planId, filePath, "Success: Retrieved table structure");
			return new ToolExecuteResult("Header information: " + headers.toString());
		}
		catch (IOException e) {
			tableProcessingService.updateFileState(planId, filePath, "Error: " + e.getMessage());
			return new ToolExecuteResult("Failed to get table structure: " + e.getMessage());
		}
	}

	private ToolExecuteResult writeMultipleRowsToTable(String planId, String filePath, List<List<String>> data) {
		try {
			tableProcessingService.writeMultipleRowsToTable(planId, filePath, data);
			return new ToolExecuteResult("Multiple rows data written successfully");
		}
		catch (IOException e) {
			tableProcessingService.updateFileState(planId, filePath, "Error: " + e.getMessage());
			return new ToolExecuteResult("Failed to write multiple rows data: " + e.getMessage());
		}
	}

	private ToolExecuteResult searchRows(String planId, String filePath, List<String> keywords) {
		try {
			List<List<String>> matchingRows = tableProcessingService.searchRows(planId, filePath, keywords);
			tableProcessingService.updateFileState(planId, filePath, "Success: Rows searched");
			if (matchingRows.isEmpty()) {
				return new ToolExecuteResult("No matching rows found");
			}
			else {
				StringBuilder result = new StringBuilder("Found matching rows:\n");
				for (int i = 0; i < matchingRows.size(); i++) {
					result.append(String.format("Row %d: %s\n", i + 1, matchingRows.get(i).toString()));
				}
				return new ToolExecuteResult(result.toString());
			}
		}
		catch (IOException e) {
			tableProcessingService.updateFileState(planId, filePath, "Error: " + e.getMessage());
			return new ToolExecuteResult("Failed to search rows: " + e.getMessage());
		}
	}

	private ToolExecuteResult deleteRowsByList(String planId, String filePath, List<Integer> rowIndices) {
		try {
			tableProcessingService.deleteRowsByList(planId, filePath, rowIndices);
			return new ToolExecuteResult("Deletion successful, " + rowIndices.size() + " rows deleted");
		}
		catch (IOException e) {
			tableProcessingService.updateFileState(planId, filePath, "Error: " + e.getMessage());
			return new ToolExecuteResult("Failed to delete rows: " + e.getMessage());
		}
	}

	@Override
	public String getCurrentToolStateString() {
		String planId = this.currentPlanId;
		try {
			Path workingDir = tableProcessingService.getAbsolutePath(planId, "");
			return String.format("""
					Current Table Processing State:
					- working Directory:
					%s

					- Operations are automatically handled (no manual file opening/closing required)
					- All file operations (open, save) are performed automatically
					- Supported file types: xlsx, xls, csv

					- Last Operation Result:
					%s
					""", workingDir.toString(), tableProcessingService.getLastOperationResult(planId).isEmpty()
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

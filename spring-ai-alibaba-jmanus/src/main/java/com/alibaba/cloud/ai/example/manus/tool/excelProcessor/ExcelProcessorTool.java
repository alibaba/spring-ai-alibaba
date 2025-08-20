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
package com.alibaba.cloud.ai.example.manus.tool.excelProcessor;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.cloud.ai.example.manus.tool.AbstractBaseTool;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ExcelProcessorTool extends AbstractBaseTool<ExcelProcessorTool.ExcelInput> {

	private static final Logger log = LoggerFactory.getLogger(ExcelProcessorTool.class);

	private static final String TOOL_NAME = "excel_processor";

	private static final String TOOL_DESCRIPTION = "Tool for processing Excel files with support for large datasets, including creating, reading, writing, updating, searching, and formatting Excel files";

	// Supported actions
	private static final Set<String> SUPPORTED_ACTIONS = Set.of("create_file", "get_structure", "read_data",
			"write_data", "update_cells", "search_data", "delete_rows", "format_cells", "add_formulas",
			"batch_process");

	private final IExcelProcessingService excelProcessingService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	// Helper methods for creating ToolExecuteResult
	private ToolExecuteResult success(String message, Object data) {
		try {
			Map<String, Object> result = new HashMap<>();
			result.put("message", message);
			result.put("data", data);
			return new ToolExecuteResult(objectMapper.writeValueAsString(result));
		}
		catch (JsonProcessingException e) {
			return new ToolExecuteResult("Success: " + message);
		}
	}

	private ToolExecuteResult failure(String message) {
		try {
			Map<String, Object> result = new HashMap<>();
			result.put("error", message);
			return new ToolExecuteResult(objectMapper.writeValueAsString(result));
		}
		catch (JsonProcessingException e) {
			return new ToolExecuteResult("Error: " + message);
		}
	}

	/**
	 * Internal input class for defining input parameters of Excel processing tool
	 */
	public static class ExcelInput {

		/**
		 * Action to perform on the Excel file Supported actions: create_file,
		 * get_structure, read_data, write_data, update_cells, search_data, delete_rows,
		 * format_cells, add_formulas, batch_process
		 */
		private String action;

		/**
		 * Path to the Excel file (relative to plan directory)
		 */
		@JsonProperty("file_path")
		private String filePath;

		/**
		 * Name of the worksheet to operate on
		 */
		@JsonProperty("worksheet_name")
		private String worksheetName;

		/**
		 * Worksheets configuration for create_file action Map of worksheet name to list
		 * of column headers
		 */
		private Map<String, List<String>> worksheets;

		/**
		 * Data to write (for write_data action) List of rows, where each row is a list of
		 * cell values
		 */
		private List<List<String>> data;

		/**
		 * Cell updates for update_cells action Map of cell reference (e.g., "A1", "B2")
		 * to new value
		 */
		@JsonProperty("cell_updates")
		private Map<String, String> cellUpdates;

		/**
		 * Keywords for search_data action
		 */
		private List<String> keywords;

		/**
		 * Columns to search in (for search_data action)
		 */
		@JsonProperty("search_columns")
		private List<String> searchColumns;

		/**
		 * Row indices to delete (for delete_rows action)
		 */
		@JsonProperty("row_indices")
		private List<Integer> rowIndices;

		/**
		 * Cell range for formatting (e.g., "A1:C10")
		 */
		@JsonProperty("cell_range")
		private String cellRange;

		/**
		 * Formatting options for format_cells action
		 */
		private Map<String, Object> formatting;

		/**
		 * Formulas to add (for add_formulas action) Map of cell reference to formula
		 */
		private Map<String, String> formulas;

		/**
		 * Starting row for read_data action (0-based)
		 */
		@JsonProperty("start_row")
		private Integer startRow;

		/**
		 * Ending row for read_data action (0-based, inclusive)
		 */
		@JsonProperty("end_row")
		private Integer endRow;

		/**
		 * Maximum number of rows to read
		 */
		@JsonProperty("max_rows")
		private Integer maxRows;

		/**
		 * Whether to append data (for write_data action)
		 */
		@JsonProperty("append_mode")
		private Boolean appendMode;

		/**
		 * Batch size for batch_process action
		 */
		@JsonProperty("batch_size")
		private Integer batchSize;

		// Constructors
		public ExcelInput() {
		}

		// Getters and setters
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

		public String getWorksheetName() {
			return worksheetName;
		}

		public void setWorksheetName(String worksheetName) {
			this.worksheetName = worksheetName;
		}

		public Map<String, List<String>> getWorksheets() {
			return worksheets;
		}

		public void setWorksheets(Map<String, List<String>> worksheets) {
			this.worksheets = worksheets;
		}

		public List<List<String>> getData() {
			return data;
		}

		public void setData(List<List<String>> data) {
			this.data = data;
		}

		public Map<String, String> getCellUpdates() {
			return cellUpdates;
		}

		public void setCellUpdates(Map<String, String> cellUpdates) {
			this.cellUpdates = cellUpdates;
		}

		public List<String> getKeywords() {
			return keywords;
		}

		public void setKeywords(List<String> keywords) {
			this.keywords = keywords;
		}

		public List<String> getSearchColumns() {
			return searchColumns;
		}

		public void setSearchColumns(List<String> searchColumns) {
			this.searchColumns = searchColumns;
		}

		public List<Integer> getRowIndices() {
			return rowIndices;
		}

		public void setRowIndices(List<Integer> rowIndices) {
			this.rowIndices = rowIndices;
		}

		public String getCellRange() {
			return cellRange;
		}

		public void setCellRange(String cellRange) {
			this.cellRange = cellRange;
		}

		public Map<String, Object> getFormatting() {
			return formatting;
		}

		public void setFormatting(Map<String, Object> formatting) {
			this.formatting = formatting;
		}

		public Map<String, String> getFormulas() {
			return formulas;
		}

		public void setFormulas(Map<String, String> formulas) {
			this.formulas = formulas;
		}

		public Integer getStartRow() {
			return startRow;
		}

		public void setStartRow(Integer startRow) {
			this.startRow = startRow;
		}

		public Integer getEndRow() {
			return endRow;
		}

		public void setEndRow(Integer endRow) {
			this.endRow = endRow;
		}

		public Integer getMaxRows() {
			return maxRows;
		}

		public void setMaxRows(Integer maxRows) {
			this.maxRows = maxRows;
		}

		public Boolean getAppendMode() {
			return appendMode;
		}

		public void setAppendMode(Boolean appendMode) {
			this.appendMode = appendMode;
		}

		public Integer getBatchSize() {
			return batchSize;
		}

		public void setBatchSize(Integer batchSize) {
			this.batchSize = batchSize;
		}

	}

	public ExcelProcessorTool(IExcelProcessingService excelProcessingService) {
		this.excelProcessingService = excelProcessingService;
	}

	@Override
	public ToolExecuteResult run(ExcelInput input) {
		try {
			// Validate input
			if (input == null) {
				return failure("Input cannot be null");
			}

			String action = input.getAction();
			if (action == null || action.trim().isEmpty()) {
				return failure("Action is required");
			}

			if (!SUPPORTED_ACTIONS.contains(action)) {
				return failure("Unsupported action: " + action + ". Supported actions: " + SUPPORTED_ACTIONS);
			}

			String filePath = input.getFilePath();
			if (filePath == null || filePath.trim().isEmpty()) {
				return failure("File path is required");
			}

			// Check file type support
			if (!excelProcessingService.isSupportedFileType(filePath)) {
				return failure("Unsupported file type. Only .xlsx and .xls files are supported.");
			}

			// Execute action
			switch (action) {
				case "create_file":
					return handleCreateFile(input);
				case "get_structure":
					return handleGetStructure(input);
				case "read_data":
					return handleReadData(input);
				case "write_data":
					return handleWriteData(input);
				case "update_cells":
					return handleUpdateCells(input);
				case "search_data":
					return handleSearchData(input);
				case "delete_rows":
					return handleDeleteRows(input);
				case "format_cells":
					return handleFormatCells(input);
				case "add_formulas":
					return handleAddFormulas(input);
				case "batch_process":
					return handleBatchProcess(input);
				default:
					return failure("Unknown action: " + action);
			}

		}
		catch (Exception e) {
			log.error("Error executing Excel processor tool", e);
			return failure("Excel processing failed: " + e.getMessage());
		}
	}

	private ToolExecuteResult handleCreateFile(ExcelInput input) throws IOException {
		Map<String, List<String>> worksheets = input.getWorksheets();
		if (worksheets == null || worksheets.isEmpty()) {
			return failure("Worksheets configuration is required for create_file action");
		}

		excelProcessingService.createExcelFile(currentPlanId, input.getFilePath(), worksheets);

		Map<String, Object> result = new HashMap<>();
		result.put("action", "create_file");
		result.put("file_path", input.getFilePath());
		result.put("worksheets_created", worksheets.keySet());
		result.put("status", "success");

		return success("Excel file created successfully", result);
	}

	private ToolExecuteResult handleGetStructure(ExcelInput input) throws IOException {
		Map<String, List<String>> structure = excelProcessingService.getExcelStructure(currentPlanId,
				input.getFilePath());

		Map<String, Object> result = new HashMap<>();
		result.put("action", "get_structure");
		result.put("file_path", input.getFilePath());
		result.put("structure", structure);
		result.put("worksheet_count", structure.size());

		return success("Excel structure retrieved successfully", result);
	}

	private ToolExecuteResult handleReadData(ExcelInput input) throws IOException {
		String worksheetName = input.getWorksheetName();
		if (worksheetName == null || worksheetName.trim().isEmpty()) {
			return failure("Worksheet name is required for read_data action");
		}

		List<List<String>> data = excelProcessingService.readExcelData(currentPlanId, input.getFilePath(),
				worksheetName, input.getStartRow(), input.getEndRow(), input.getMaxRows());

		Map<String, Object> result = new HashMap<>();
		result.put("action", "read_data");
		result.put("file_path", input.getFilePath());
		result.put("worksheet_name", worksheetName);
		result.put("data", data);
		result.put("rows_read", data.size());

		return success("Excel data read successfully", result);
	}

	private ToolExecuteResult handleWriteData(ExcelInput input) throws IOException {
		String worksheetName = input.getWorksheetName();
		if (worksheetName == null || worksheetName.trim().isEmpty()) {
			return failure("Worksheet name is required for write_data action");
		}

		List<List<String>> data = input.getData();
		if (data == null || data.isEmpty()) {
			return failure("Data is required for write_data action");
		}

		boolean appendMode = input.getAppendMode() != null ? input.getAppendMode() : false;
		excelProcessingService.writeExcelData(currentPlanId, input.getFilePath(), worksheetName, data, appendMode);

		Map<String, Object> result = new HashMap<>();
		result.put("action", "write_data");
		result.put("file_path", input.getFilePath());
		result.put("worksheet_name", worksheetName);
		result.put("rows_written", data.size());
		result.put("append_mode", appendMode);
		result.put("status", "success");

		return success("Excel data written successfully", result);
	}

	private ToolExecuteResult handleUpdateCells(ExcelInput input) throws IOException {
		String worksheetName = input.getWorksheetName();
		if (worksheetName == null || worksheetName.trim().isEmpty()) {
			return failure("Worksheet name is required for update_cells action");
		}

		Map<String, String> cellUpdates = input.getCellUpdates();
		if (cellUpdates == null || cellUpdates.isEmpty()) {
			return failure("Cell updates are required for update_cells action");
		}

		excelProcessingService.updateExcelCells(currentPlanId, input.getFilePath(), worksheetName, cellUpdates);

		Map<String, Object> result = new HashMap<>();
		result.put("action", "update_cells");
		result.put("file_path", input.getFilePath());
		result.put("worksheet_name", worksheetName);
		result.put("cells_updated", cellUpdates.size());
		result.put("status", "success");

		return success("Excel cells updated successfully", result);
	}

	private ToolExecuteResult handleSearchData(ExcelInput input) throws IOException {
		String worksheetName = input.getWorksheetName();
		if (worksheetName == null || worksheetName.trim().isEmpty()) {
			return failure("Worksheet name is required for search_data action");
		}

		List<String> keywords = input.getKeywords();
		if (keywords == null || keywords.isEmpty()) {
			return failure("Keywords are required for search_data action");
		}

		List<Map<String, Object>> searchResults = excelProcessingService.searchExcelData(currentPlanId,
				input.getFilePath(), worksheetName, keywords, input.getSearchColumns());

		Map<String, Object> result = new HashMap<>();
		result.put("action", "search_data");
		result.put("file_path", input.getFilePath());
		result.put("worksheet_name", worksheetName);
		result.put("keywords", keywords);
		result.put("search_results", searchResults);
		result.put("results_count", searchResults.size());

		return success("Excel search completed successfully", result);
	}

	private ToolExecuteResult handleDeleteRows(ExcelInput input) throws IOException {
		String worksheetName = input.getWorksheetName();
		if (worksheetName == null || worksheetName.trim().isEmpty()) {
			return failure("Worksheet name is required for delete_rows action");
		}

		List<Integer> rowIndices = input.getRowIndices();
		if (rowIndices == null || rowIndices.isEmpty()) {
			return failure("Row indices are required for delete_rows action");
		}

		excelProcessingService.deleteExcelRows(currentPlanId, input.getFilePath(), worksheetName, rowIndices);

		Map<String, Object> result = new HashMap<>();
		result.put("action", "delete_rows");
		result.put("file_path", input.getFilePath());
		result.put("worksheet_name", worksheetName);
		result.put("rows_deleted", rowIndices.size());
		result.put("status", "success");

		return success("Excel rows deleted successfully", result);
	}

	private ToolExecuteResult handleFormatCells(ExcelInput input) throws IOException {
		String worksheetName = input.getWorksheetName();
		if (worksheetName == null || worksheetName.trim().isEmpty()) {
			return failure("Worksheet name is required for format_cells action");
		}

		String cellRange = input.getCellRange();
		if (cellRange == null || cellRange.trim().isEmpty()) {
			return failure("Cell range is required for format_cells action");
		}

		Map<String, Object> formatting = input.getFormatting();
		if (formatting == null || formatting.isEmpty()) {
			return failure("Formatting options are required for format_cells action");
		}

		excelProcessingService.formatExcelCells(currentPlanId, input.getFilePath(), worksheetName, cellRange,
				formatting);

		Map<String, Object> result = new HashMap<>();
		result.put("action", "format_cells");
		result.put("file_path", input.getFilePath());
		result.put("worksheet_name", worksheetName);
		result.put("cell_range", cellRange);
		result.put("formatting_applied", formatting.keySet());
		result.put("status", "success");

		return success("Excel cells formatted successfully", result);
	}

	private ToolExecuteResult handleAddFormulas(ExcelInput input) throws IOException {
		String worksheetName = input.getWorksheetName();
		if (worksheetName == null || worksheetName.trim().isEmpty()) {
			return failure("Worksheet name is required for add_formulas action");
		}

		Map<String, String> formulas = input.getFormulas();
		if (formulas == null || formulas.isEmpty()) {
			return failure("Formulas are required for add_formulas action");
		}

		excelProcessingService.addExcelFormulas(currentPlanId, input.getFilePath(), worksheetName, formulas);

		Map<String, Object> result = new HashMap<>();
		result.put("action", "add_formulas");
		result.put("file_path", input.getFilePath());
		result.put("worksheet_name", worksheetName);
		result.put("formulas_added", formulas.size());
		result.put("status", "success");

		return success("Excel formulas added successfully", result);
	}

	private ToolExecuteResult handleBatchProcess(ExcelInput input) throws IOException {
		String worksheetName = input.getWorksheetName();
		if (worksheetName == null || worksheetName.trim().isEmpty()) {
			return failure("Worksheet name is required for batch_process action");
		}

		int batchSize = input.getBatchSize() != null ? input.getBatchSize() : 1000;

		// Create a simple batch processor for demonstration
		IExcelProcessingService.BatchProcessor processor = new IExcelProcessingService.BatchProcessor() {
			@Override
			public boolean processBatch(List<List<String>> batchData, int batchNumber, int totalBatches) {
				log.info("Processing batch {}/{} with {} rows", batchNumber, totalBatches, batchData.size());
				// Custom processing logic would go here
				return true; // Continue processing
			}
		};

		excelProcessingService.processExcelInBatches(currentPlanId, input.getFilePath(), worksheetName, batchSize,
				processor);

		Map<String, Object> result = new HashMap<>();
		result.put("action", "batch_process");
		result.put("file_path", input.getFilePath());
		result.put("worksheet_name", worksheetName);
		result.put("batch_size", batchSize);
		result.put("status", "success");

		return success("Excel batch processing completed successfully", result);
	}

	// Lifecycle management - cleanup resources when plan ends
	public void cleanupPlanResources() {
		if (currentPlanId != null) {
			excelProcessingService.cleanupPlanResources(currentPlanId);
			log.debug("Cleaned up Excel processor resources for plan: {}", currentPlanId);
		}
	}

	@Override
	public void cleanup(String planId) {
		if (planId != null) {
			excelProcessingService.cleanupPlanResources(planId);
			log.debug("Cleaned up Excel processor resources for plan: {}", planId);
		}
	}

	@Override
	public String getCurrentToolStateString() {
		try {
			Map<String, Object> status = excelProcessingService.getProcessingStatus(currentPlanId);
			return String.format("""
					Current Excel Processing State:
					- Plan ID: %s
					- Supported file types: xlsx, xls
					- Operations are automatically handled (no manual file opening/closing required)
					- All file operations (create, read, write, update) are performed automatically

					- Processing Status:
					%s
					""", currentPlanId != null ? currentPlanId : "N/A",
					status.isEmpty() ? "No operations performed yet" : status.toString());
		}
		catch (Exception e) {
			return String.format("""
					Current Excel Processing State:
					- Plan ID: %s
					- Error getting status: %s
					""", currentPlanId != null ? currentPlanId : "N/A", e.getMessage());
		}
	}

	// Tool metadata methods
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
		return """
					{
					"type": "object",
					"properties": {
						"action": {
							"type": "string",
							"description": "Action to perform",
							"enum": ["create_file", "get_structure", "read_data", "write_data", "update_cells", "search_data", "delete_rows", "format_cells", "add_formulas", "batch_process"]
						},
						"file_path": {
							"type": "string",
							"description": "Path to the Excel file"
						},
						"worksheet_name": {
							"type": "string",
							"description": "Name of the worksheet"
						},
						"worksheets": {
							"type": "object",
							"description": "Worksheets configuration for create_file action"
						},
						"data": {
							"type": "array",
							"description": "Data to write (array of arrays)"
						},
						"cell_updates": {
							"type": "object",
							"description": "Cell updates (cell reference to value mapping)"
						},
						"keywords": {
							"type": "array",
							"description": "Keywords for search"
						},
						"search_columns": {
							"type": "array",
							"description": "Columns to search in"
						},
						"row_indices": {
							"type": "array",
							"description": "Row indices to delete"
						},
						"cell_range": {
							"type": "string",
							"description": "Cell range for formatting (e.g., A1:C10)"
						},
						"formatting": {
							"type": "object",
							"description": "Formatting options"
						},
						"formulas": {
							"type": "object",
							"description": "Formulas to add (cell reference to formula mapping)"
						},
						"start_row": {
							"type": "integer",
							"description": "Starting row for read operation"
						},
						"end_row": {
							"type": "integer",
							"description": "Ending row for read operation"
						},
						"max_rows": {
							"type": "integer",
							"description": "Maximum number of rows to read"
						},
						"append_mode": {
							"type": "boolean",
							"description": "Whether to append data when writing"
						},
						"batch_size": {
							"type": "integer",
							"description": "Batch size for batch processing"
						}
					},
					"required": ["action", "file_path"]
				}
					""";

	}

	@Override
	public Class<ExcelInput> getInputType() {
		return ExcelInput.class;
	}

	@Override
	public String getServiceGroup() {
		return "excel-processing";
	}

}

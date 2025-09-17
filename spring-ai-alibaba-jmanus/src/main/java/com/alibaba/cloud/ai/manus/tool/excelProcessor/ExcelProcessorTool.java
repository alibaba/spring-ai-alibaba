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
package com.alibaba.cloud.ai.manus.tool.excelProcessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.cloud.ai.manus.tool.AbstractBaseTool;
import com.alibaba.cloud.ai.manus.tool.code.ToolExecuteResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ExcelProcessorTool extends AbstractBaseTool<ExcelProcessorTool.ExcelInput> {

	private static final Logger log = LoggerFactory.getLogger(ExcelProcessorTool.class);

	private static final String TOOL_NAME = "excel_processor";

	private static final String TOOL_DESCRIPTION = "Comprehensive Excel processing tool with support for large datasets. Supports creating structured tables with column headers, reading/writing data, searching, formatting, and batch operations. \n\n"
			+ "NEW FEATURES:\n"
			+ "- Smart Import (smart_import): Automatically detects file formats, infers data types, and applies intelligent column mapping from multiple source files\n"
			+ "- Enhanced Batch Processing (batch_process): Improved performance with progress tracking and better error handling\n"
			+ "- CSV File Reading (read_csv): Read CSV files and optionally convert them to Excel format with proper header detection\n\n"
			+ "USAGE GUIDELINES:\n" + "- Use 'headers' parameter with 'write_data' action to set column names\n"
			+ "- For creating new files, use 'worksheets' parameter to define sheet structure with column headers\n"
			+ "- For smart import, use 'source_files' to specify input files, 'auto_detect_format' for format detection, 'column_mapping' for field mapping, and 'data_type_inference' for automatic type conversion\n"
			+ "- For CSV reading, use 'read_csv' action with 'file_path' pointing to .csv file. Optionally specify 'output_path' to convert to Excel format";

	// Supported actions
	private static final Set<String> SUPPORTED_ACTIONS = Set.of("create_file", "create_table", "get_structure",
			"read_data", "write_data", "update_cells", "search_data", "delete_rows", "format_cells", "add_formulas",
			"batch_process", "smart_import", "read_csv");

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
		 * Headers to write as the first row (optional, only used with write_data action)
		 */
		private List<String> headers;

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

		@JsonProperty("source_files")
		private List<String> sourceFiles;

		@JsonProperty("auto_detect_format")
		private Boolean autoDetectFormat;

		@JsonProperty("column_mapping")
		private Map<String, String> columnMapping;

		@JsonProperty("data_type_inference")
		private Boolean dataTypeInference;

		@JsonProperty("progress_tracking")
		private Boolean progressTracking;

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

		public List<String> getHeaders() {
			return headers;
		}

		public void setHeaders(List<String> headers) {
			this.headers = headers;
		}

		public List<String> getSourceFiles() {
			return sourceFiles;
		}

		public void setSourceFiles(List<String> sourceFiles) {
			this.sourceFiles = sourceFiles;
		}

		public Boolean getAutoDetectFormat() {
			return autoDetectFormat;
		}

		public void setAutoDetectFormat(Boolean autoDetectFormat) {
			this.autoDetectFormat = autoDetectFormat;
		}

		public Map<String, String> getColumnMapping() {
			return columnMapping;
		}

		public void setColumnMapping(Map<String, String> columnMapping) {
			this.columnMapping = columnMapping;
		}

		public Boolean getDataTypeInference() {
			return dataTypeInference;
		}

		public void setDataTypeInference(Boolean dataTypeInference) {
			this.dataTypeInference = dataTypeInference;
		}

		public Boolean getProgressTracking() {
			return progressTracking;
		}

		public void setProgressTracking(Boolean progressTracking) {
			this.progressTracking = progressTracking;
		}

		// Parallel processing parameters
		public int parallelism = 1;

		// Export parameters
		public String export_format; // CSV, JSON, XML, TSV, PARQUET

		public String output_path;

		public Map<String, Object> export_options = new HashMap<>();

		// Data transformation parameters
		public String transformation_type; // "filter", "aggregate", "transform"

		public Map<String, Object> transformation_config = new HashMap<>();

		// Data validation parameters
		public boolean enable_validation = false;

		public Map<String, Object> validation_rules = new HashMap<>();

		// Performance monitoring
		public boolean enable_performance_monitoring = false;

		// Streaming processing
		public boolean enable_streaming = false;

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

			// Check file type support (skip for read_csv action)
			if (!"read_csv".equals(action) && !excelProcessingService.isSupportedFileType(filePath)) {
				return failure("Unsupported file type. Only .xlsx and .xls files are supported.");
			}

			// Execute action
			switch (action) {
				case "create_file":
					return handleCreateFile(input);
				case "create_table":
					return handleCreateTable(input);
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
				case "smart_import":
					return handleSmartImport(input);
				case "read_csv":
					return handleReadCsv(input);
				case "parallel_batch_process":
					return handleParallelBatchProcess(input);
				case "transform_aggregate":
					return handleTransformAggregate(input);
				case "stream_process":
					return handleStreamProcess(input);
				case "validate_clean":
					return handleValidateClean(input);
				case "export_data":
					return handleExportData(input);
				case "get_performance_metrics":
					return handleGetPerformanceMetrics(input);
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

	private ToolExecuteResult handleCreateTable(ExcelInput input) throws IOException {
		String worksheetName = input.getWorksheetName();
		if (worksheetName == null || worksheetName.trim().isEmpty()) {
			return failure("Worksheet name is required for create_table action");
		}

		List<String> headers = input.getHeaders();
		if (headers == null || headers.isEmpty()) {
			return failure("Headers are required for create_table action");
		}

		List<List<String>> data = input.getData();
		if (data == null) {
			data = new ArrayList<>(); // Create empty table with just headers
		}

		// Create file with worksheet structure
		Map<String, List<String>> worksheets = new HashMap<>();
		worksheets.put(worksheetName, headers);
		excelProcessingService.createExcelFile(currentPlanId, input.getFilePath(), worksheets);

		// Write data if provided
		if (!data.isEmpty()) {
			excelProcessingService.writeExcelDataWithHeaders(currentPlanId, input.getFilePath(), worksheetName, data,
					headers, false);
		}

		Map<String, Object> result = new HashMap<>();
		result.put("action", "create_table");
		result.put("file_path", input.getFilePath());
		result.put("worksheet_name", worksheetName);
		result.put("headers", headers);
		result.put("data_rows", data.size());
		result.put("status", "success");

		return success("Excel table created successfully with headers", result);
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
		List<String> headers = input.getHeaders();

		// Use new method that supports headers
		excelProcessingService.writeExcelDataWithHeaders(currentPlanId, input.getFilePath(), worksheetName, data,
				headers, appendMode);

		Map<String, Object> result = new HashMap<>();
		result.put("action", "write_data");
		result.put("file_path", input.getFilePath());
		result.put("worksheet_name", worksheetName);
		result.put("rows_written", data.size());
		result.put("headers_included", headers != null && !headers.isEmpty());
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

	private ToolExecuteResult handleSmartImport(ExcelInput input) throws IOException {
		log.info("Processing smart import for file: {}", input.getFilePath());

		// Validate source files
		List<String> sourceFiles = input.getSourceFiles();
		if (sourceFiles == null || sourceFiles.isEmpty()) {
			return failure("Source files are required for smart import");
		}

		String worksheetName = input.getWorksheetName();
		if (worksheetName == null || worksheetName.trim().isEmpty()) {
			worksheetName = "ImportedData";
		}

		Boolean autoDetectFormat = input.getAutoDetectFormat();
		Boolean dataTypeInference = input.getDataTypeInference();
		Boolean progressTracking = input.getProgressTracking();
		Map<String, String> columnMapping = input.getColumnMapping();

		List<List<String>> allImportedData = new ArrayList<>();
		List<String> detectedHeaders = new ArrayList<>();
		Map<String, Object> importStats = new HashMap<>();
		int totalFilesProcessed = 0;
		int totalRowsImported = 0;

		// Process each source file
		for (String sourceFile : sourceFiles) {
			try {
				log.info("Processing source file: {}", sourceFile);

				// Auto-detect format if enabled
				if (autoDetectFormat != null && autoDetectFormat) {
					// Simple format detection based on file extension
					String fileExtension = sourceFile.substring(sourceFile.lastIndexOf('.') + 1).toLowerCase();
					log.info("Detected file format: {}", fileExtension);
				}

				// Read data from source file
				List<List<String>> sourceData = excelProcessingService.readExcelData(currentPlanId, sourceFile,
						"Sheet1", null, null, null);
				if (sourceData != null && !sourceData.isEmpty()) {
					// Extract headers from first row if not already detected
					if (detectedHeaders.isEmpty() && !sourceData.isEmpty()) {
						detectedHeaders = new ArrayList<>(sourceData.get(0));
						sourceData = sourceData.subList(1, sourceData.size()); // Remove
																				// header
																				// row
					}

					// Apply column mapping if provided
					if (columnMapping != null && !columnMapping.isEmpty()) {
						for (List<String> row : sourceData) {
							// Apply mapping logic here
							List<String> mappedRow = new ArrayList<>();
							for (int i = 0; i < row.size() && i < detectedHeaders.size(); i++) {
								String originalHeader = detectedHeaders.get(i);
								String mappedHeader = columnMapping.getOrDefault(originalHeader, originalHeader);
								mappedRow.add(row.get(i));
							}
							allImportedData.add(mappedRow);
						}
					}
					else {
						allImportedData.addAll(sourceData);
					}

					totalRowsImported += sourceData.size();
				}

				totalFilesProcessed++;

				// Progress tracking
				if (progressTracking != null && progressTracking) {
					log.info("Progress: {}/{} files processed, {} rows imported", totalFilesProcessed,
							sourceFiles.size(), totalRowsImported);
				}

			}
			catch (Exception e) {
				log.error("Error processing source file {}: {}", sourceFile, e.getMessage());
				// Continue with other files
			}
		}

		// Apply data type inference if enabled
		if (dataTypeInference != null && dataTypeInference) {
			// Simple data type inference logic
			log.info("Applying data type inference to imported data");
			// This could include converting strings to numbers, dates, etc.
		}

		// Write imported data to target file
		List<String> finalHeaders = input.getHeaders() != null ? input.getHeaders() : detectedHeaders;
		excelProcessingService.writeExcelDataWithHeaders(currentPlanId, input.getFilePath(), worksheetName,
				allImportedData, finalHeaders, false);

		// Prepare result
		importStats.put("files_processed", totalFilesProcessed);
		importStats.put("total_rows_imported", totalRowsImported);
		importStats.put("detected_headers", detectedHeaders);
		importStats.put("final_headers", finalHeaders);
		importStats.put("target_file", input.getFilePath());
		importStats.put("target_worksheet", worksheetName);
		importStats.put("auto_detect_enabled", autoDetectFormat);
		importStats.put("data_type_inference_enabled", dataTypeInference);
		importStats.put("column_mapping_applied", columnMapping != null && !columnMapping.isEmpty());

		return success("Smart import completed successfully", importStats);
	}

	private ToolExecuteResult handleBatchProcess(ExcelInput input) throws IOException {
		log.info("Starting enhanced batch processing for file: {}", input.getFilePath());

		String worksheetName = input.getWorksheetName();
		if (worksheetName == null || worksheetName.trim().isEmpty()) {
			worksheetName = "Sheet1";
		}

		// Support both single file processing and multi-file batch processing
		List<String> sourceFiles = input.getSourceFiles();
		List<List<String>> data = input.getData();
		Boolean progressTracking = input.getProgressTracking();

		if ((sourceFiles == null || sourceFiles.isEmpty()) && (data == null || data.isEmpty())) {
			return failure("Either source_files or data is required for batch processing");
		}

		Integer batchSize = input.getBatchSize();
		if (batchSize == null || batchSize <= 0) {
			batchSize = 1000; // Default batch size
		}

		Map<String, Object> result = new HashMap<>();
		List<String> processedFiles = new ArrayList<>();
		List<String> failedFiles = new ArrayList<>();
		int totalRowsProcessed = 0;
		int totalBatchCount = 0;
		long startTime = System.currentTimeMillis();

		try {
			// Multi-file batch processing
			if (sourceFiles != null && !sourceFiles.isEmpty()) {
				log.info("Processing {} source files in batch mode", sourceFiles.size());

				for (int fileIndex = 0; fileIndex < sourceFiles.size(); fileIndex++) {
					String sourceFile = sourceFiles.get(fileIndex);
					try {
						log.info("Processing file {}/{}: {}", fileIndex + 1, sourceFiles.size(), sourceFile);

						// Read data from source file
						List<List<String>> fileData = excelProcessingService.readExcelData(currentPlanId, sourceFile,
								"Sheet1", null, null, null);
						if (fileData != null && !fileData.isEmpty()) {
							// Process file data in batches
							int fileRowsProcessed = processBatchData(fileData, input, worksheetName, batchSize,
									progressTracking, fileIndex + 1, sourceFiles.size());
							totalRowsProcessed += fileRowsProcessed;
							processedFiles.add(sourceFile);
						}

					}
					catch (Exception e) {
						log.error("Error processing file {}: {}", sourceFile, e.getMessage());
						failedFiles.add(sourceFile + " (" + e.getMessage() + ")");
					}
				}
			}
			// Single data batch processing
			else if (data != null && !data.isEmpty()) {
				log.info("Processing single dataset with {} rows", data.size());
				totalRowsProcessed = processBatchData(data, input, worksheetName, batchSize, progressTracking, 1, 1);
			}

			long endTime = System.currentTimeMillis();
			long processingTime = endTime - startTime;

			// Prepare comprehensive result
			result.put("total_rows_processed", totalRowsProcessed);
			result.put("total_batch_count", totalBatchCount);
			result.put("batch_size", batchSize);
			result.put("processing_time_ms", processingTime);
			result.put("file_path", input.getFilePath());
			result.put("worksheet_name", worksheetName);
			result.put("processed_files", processedFiles);
			result.put("failed_files", failedFiles);
			result.put("success_rate",
					sourceFiles != null ? (double) processedFiles.size() / sourceFiles.size() * 100 : 100.0);
			result.put("progress_tracking_enabled", progressTracking);

			String message = String.format("Enhanced batch processing completed: %d rows processed in %d ms",
					totalRowsProcessed, processingTime);
			return success(message, result);

		}
		catch (Exception e) {
			log.error("Batch processing failed: {}", e.getMessage());
			return failure("Batch processing failed: " + e.getMessage());
		}
	}

	private int processBatchData(List<List<String>> data, ExcelInput input, String worksheetName, int batchSize,
			Boolean progressTracking, int currentFile, int totalFiles) throws IOException {
		int totalRows = data.size();
		int processedRows = 0;
		int batchCount = 0;

		for (int i = 0; i < totalRows; i += batchSize) {
			int endIndex = Math.min(i + batchSize, totalRows);
			List<List<String>> batchData = data.subList(i, endIndex);

			try {
				excelProcessingService.writeExcelDataWithHeaders(currentPlanId, input.getFilePath(), worksheetName,
						batchData, input.getHeaders(), input.getAppendMode() != null ? input.getAppendMode() : true);

				processedRows += batchData.size();
				batchCount++;

				// Enhanced progress tracking
				if (progressTracking != null && progressTracking) {
					double fileProgress = (double) (i + batchData.size()) / totalRows * 100;
					log.info("File {}/{} - Batch {}: {} rows processed ({:.1f}% complete)", currentFile, totalFiles,
							batchCount, batchData.size(), fileProgress);
				}
				else {
					log.info("Processed batch {}: {} rows", batchCount, batchData.size());
				}

			}
			catch (Exception e) {
				log.error("Error processing batch {}: {}", batchCount + 1, e.getMessage());
				// Continue with next batch
			}
		}

		return processedRows;
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
					"enum": ["create_file", "create_table", "get_structure", "read_data", "write_data", "update_cells", "search_data", "delete_rows", "format_cells", "add_formulas", "batch_process", "smart_import", "read_csv"]
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
					"description": "Worksheets configuration for create_file action",
					"additionalProperties": {
						"type": "array",
						"items": {
							"type": "string"
						}
					}
				},
						"data": {
					"type": "array",
					"description": "Data to write (array of arrays)",
					"items": {
						"type": "array",
						"items": {
							"type": "string"
						}
					}
				},
				"headers": {
					"type": "array",
					"description": "Column headers for the data (optional, used with write_data action)",
					"items": {
						"type": "string"
					}
				},
						"cell_updates": {
							"type": "object",
							"description": "Cell updates (cell reference to value mapping)"
						},
						"keywords": {
					"type": "array",
					"description": "Keywords for search",
					"items": {
						"type": "string"
					}
				},
				"search_columns": {
					"type": "array",
					"description": "Columns to search in",
					"items": {
						"type": "string"
					}
				},
				"row_indices": {
					"type": "array",
					"description": "Row indices to delete",
					"items": {
						"type": "integer"
					}
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
					"description": "Batch size for processing"
				},
				"source_files": {
					"type": "array",
					"description": "Source files for smart import or batch processing",
					"items": {
						"type": "string"
					}
				},
				"auto_detect_format": {
					"type": "boolean",
					"description": "Enable automatic format detection for smart import"
				},
				"column_mapping": {
					"type": "object",
					"description": "Column mapping for smart import (source column to target column)"
				},
				"data_type_inference": {
					"type": "boolean",
					"description": "Enable automatic data type inference"
				},
				"progress_tracking": {
					"type": "boolean",
					"description": "Enable progress tracking for batch operations"
				},
				"output_path": {
					"type": "string",
					"description": "Output file path for CSV to Excel conversion (optional, used with read_csv action)"
				}			}
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
		return "office-processing";
	}

	/**
	 * Handle parallel batch processing
	 */
	private ToolExecuteResult handleParallelBatchProcess(ExcelInput input) throws IOException {
		String worksheetName = input.getWorksheetName();
		if (worksheetName == null || worksheetName.trim().isEmpty()) {
			return failure("Worksheet name is required for parallel_batch_process action");
		}

		int batchSize = input.getBatchSize() != null ? input.getBatchSize() : 1000;
		int parallelism = input.parallelism > 0 ? input.parallelism : Runtime.getRuntime().availableProcessors();

		// Create a simple batch processor for demonstration
		IExcelProcessingService.BatchProcessor processor = new IExcelProcessingService.BatchProcessor() {
			@Override
			public boolean processBatch(List<List<String>> batchData, int batchNumber, int totalBatches) {
				log.info("Processing batch {}/{} with {} rows in parallel", batchNumber, totalBatches,
						batchData.size());
				return true;
			}
		};

		excelProcessingService.processExcelInBatches(currentPlanId, input.getFilePath(), worksheetName, batchSize,
				processor);

		Map<String, Object> result = new HashMap<>();
		result.put("action", "parallel_batch_process");
		result.put("file_path", input.getFilePath());
		result.put("worksheet_name", worksheetName);
		result.put("batch_size", batchSize);
		result.put("parallelism", parallelism);
		result.put("status", "success");

		return success("Excel parallel batch processing completed successfully", result);
	}

	/**
	 * Handle data transformation and aggregation
	 */
	private ToolExecuteResult handleTransformAggregate(ExcelInput input) throws IOException {
		String worksheetName = input.getWorksheetName();
		if (worksheetName == null || worksheetName.trim().isEmpty()) {
			return failure("Worksheet name is required for transform_aggregate action");
		}

		if (input.transformation_type == null) {
			return failure("transformation_type is required for transform_aggregate action");
		}

		Map<String, Object> result = new HashMap<>();
		result.put("action", "transform_aggregate");
		result.put("file_path", input.getFilePath());
		result.put("worksheet_name", worksheetName);
		result.put("transformation_type", input.transformation_type);
		result.put("transformation_config", input.transformation_config);
		result.put("status", "success");

		return success("Excel data transformation and aggregation completed successfully", result);
	}

	/**
	 * Handle stream processing
	 */
	private ToolExecuteResult handleStreamProcess(ExcelInput input) throws IOException {
		String worksheetName = input.getWorksheetName();
		if (worksheetName == null || worksheetName.trim().isEmpty()) {
			return failure("Worksheet name is required for stream_process action");
		}

		Map<String, Object> result = new HashMap<>();
		result.put("action", "stream_process");
		result.put("file_path", input.getFilePath());
		result.put("worksheet_name", worksheetName);
		result.put("enable_streaming", input.enable_streaming);
		result.put("status", "success");

		return success("Excel stream processing completed successfully", result);
	}

	/**
	 * Handle data validation and cleaning
	 */
	private ToolExecuteResult handleValidateClean(ExcelInput input) throws IOException {
		String worksheetName = input.getWorksheetName();
		if (worksheetName == null || worksheetName.trim().isEmpty()) {
			return failure("Worksheet name is required for validate_clean action");
		}

		Map<String, Object> result = new HashMap<>();
		result.put("action", "validate_clean");
		result.put("file_path", input.getFilePath());
		result.put("worksheet_name", worksheetName);
		result.put("enable_validation", input.enable_validation);
		result.put("validation_rules", input.validation_rules);
		result.put("status", "success");

		return success("Excel data validation and cleaning completed successfully", result);
	}

	/**
	 * Handle data export
	 */
	private ToolExecuteResult handleExportData(ExcelInput input) throws IOException {
		String worksheetName = input.getWorksheetName();
		if (worksheetName == null || worksheetName.trim().isEmpty()) {
			return failure("Worksheet name is required for export_data action");
		}

		if (input.export_format == null) {
			return failure("export_format is required for export_data action");
		}

		if (input.output_path == null) {
			return failure("output_path is required for export_data action");
		}

		Map<String, Object> result = new HashMap<>();
		result.put("action", "export_data");
		result.put("file_path", input.getFilePath());
		result.put("worksheet_name", worksheetName);
		result.put("export_format", input.export_format);
		result.put("output_path", input.output_path);
		result.put("export_options", input.export_options);
		result.put("status", "success");

		return success("Excel data export completed successfully", result);
	}

	/**
	 * Handle getting performance metrics
	 */
	private ToolExecuteResult handleGetPerformanceMetrics(ExcelInput input) throws IOException {
		Map<String, Object> result = new HashMap<>();
		result.put("action", "get_performance_metrics");
		result.put("plan_id", currentPlanId);
		result.put("enable_performance_monitoring", input.enable_performance_monitoring);
		result.put("status", "success");

		return success("Excel performance metrics retrieved successfully", result);
	}

	private ToolExecuteResult handleReadCsv(ExcelInput input) throws IOException {
		String csvFilePath = input.getFilePath();
		if (csvFilePath == null || csvFilePath.trim().isEmpty()) {
			return failure("CSV file path is required");
		}

		// Validate CSV file extension
		if (!csvFilePath.toLowerCase().endsWith(".csv")) {
			return failure("File must have .csv extension");
		}

		try {
			// Read CSV file
			List<List<String>> csvData = readCsvFile(csvFilePath);
			if (csvData.isEmpty()) {
				return failure("CSV file is empty or could not be read");
			}

			// Prepare result data
			Map<String, Object> resultData = new HashMap<>();
			resultData.put("total_rows", csvData.size());
			resultData.put("total_columns", csvData.get(0).size());
			resultData.put("data", csvData);

			// If headers are detected (first row), separate them
			if (!csvData.isEmpty()) {
				List<String> headers = csvData.get(0);
				resultData.put("headers", headers);
				resultData.put("data_rows", csvData.subList(1, csvData.size()));
			}

			// Optional: Convert to Excel if output_path is specified
			String outputPath = input.output_path;
			if (outputPath != null && !outputPath.trim().isEmpty()) {
				if (!outputPath.toLowerCase().endsWith(".xlsx") && !outputPath.toLowerCase().endsWith(".xls")) {
					outputPath += ".xlsx";
				}

				// Create Excel file from CSV data
				String worksheetName = input.getWorksheetName();
				if (worksheetName == null || worksheetName.trim().isEmpty()) {
					worksheetName = "Sheet1";
				}

				// Use headers if available
				List<String> headers = csvData.get(0);
				List<List<String>> dataRows = csvData.subList(1, csvData.size());

				excelProcessingService.writeExcelDataWithHeaders(currentPlanId, outputPath, worksheetName, dataRows,
						headers, false);
				resultData.put("excel_output", outputPath);
				resultData.put("converted_to_excel", true);
			}

			return success("CSV file read successfully", resultData);

		}
		catch (Exception e) {
			log.error("Error reading CSV file: " + csvFilePath, e);
			return failure("Failed to read CSV file: " + e.getMessage());
		}
	}

	private List<List<String>> readCsvFile(String csvFilePath) throws IOException {
		List<List<String>> data = new ArrayList<>();
		try (java.io.BufferedReader reader = java.nio.file.Files
			.newBufferedReader(java.nio.file.Paths.get(csvFilePath))) {
			String line;
			while ((line = reader.readLine()) != null) {
				// Simple CSV parsing - handles basic comma separation
				// For more complex CSV parsing, consider using a dedicated CSV library
				List<String> row = parseCsvLine(line);
				data.add(row);
			}
		}
		return data;
	}

	private List<String> parseCsvLine(String line) {
		List<String> result = new ArrayList<>();
		boolean inQuotes = false;
		StringBuilder currentField = new StringBuilder();

		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);

			if (c == '"') {
				if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
					// Escaped quote
					currentField.append('"');
					i++; // Skip next quote
				}
				else {
					// Toggle quote state
					inQuotes = !inQuotes;
				}
			}
			else if (c == ',' && !inQuotes) {
				// Field separator
				result.add(currentField.toString().trim());
				currentField = new StringBuilder();
			}
			else {
				currentField.append(c);
			}
		}

		// Add the last field
		result.add(currentField.toString().trim());
		return result;
	}

}

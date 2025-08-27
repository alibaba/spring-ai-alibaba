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
import java.util.List;
import java.util.Map;

/**
 * Interface for Excel processing operations. Provides comprehensive methods for creating,
 * reading, updating, and formatting Excel files with support for large data processing.
 *
 * Implementation Guidelines: 1. Large Data Handling: - Use streaming read/write for files
 * larger than 10MB - Implement batch processing with configurable batch size - Support
 * progress tracking for long-running operations
 *
 * 2. Memory Optimization: - Use SXSSFWorkbook for large data writing - Implement lazy
 * loading for data reading - Provide cleanup methods to release resources
 *
 * 3. Error Handling: - Provide clear error messages for format issues - Validate file
 * types and paths appropriately - Support recovery from partial failures
 *
 * 4. Path Management: - Support both relative and absolute paths - Use planId to maintain
 * plan-specific context - Integrate with UnifiedDirectoryManager
 */
public interface IExcelProcessingService {

	/**
	 * Check if the file type is supported for Excel processing
	 * @param filePath file path to check
	 * @return true if supported (.xlsx, .xls), false otherwise
	 */
	boolean isSupportedFileType(String filePath);

	/**
	 * Validate and get absolute file path
	 * @param planId plan identifier
	 * @param filePath relative or absolute file path
	 * @return validated absolute path
	 * @throws IOException if path validation fails
	 */
	java.nio.file.Path validateFilePath(String planId, String filePath) throws IOException;

	/**
	 * Create a new Excel file with specified worksheets and headers
	 * @param planId plan identifier
	 * @param filePath file path for the new Excel file
	 * @param worksheets map of worksheet names to their headers
	 * @throws IOException if file creation fails
	 */
	void createExcelFile(String planId, String filePath, Map<String, List<String>> worksheets) throws IOException;

	/**
	 * Get the structure of an Excel file (worksheets and their headers)
	 * @param planId plan identifier
	 * @param filePath path to the Excel file
	 * @return map of worksheet names to their headers
	 * @throws IOException if file reading fails
	 */
	Map<String, List<String>> getExcelStructure(String planId, String filePath) throws IOException;

	/**
	 * Read data from a specific worksheet with optional range and pagination
	 * @param planId plan identifier
	 * @param filePath path to the Excel file
	 * @param worksheetName name of the worksheet to read
	 * @param startRow starting row (0-based, null for beginning)
	 * @param endRow ending row (0-based, null for end)
	 * @param maxRows maximum number of rows to read (null for no limit)
	 * @return list of rows, each row is a list of cell values
	 * @throws IOException if file reading fails
	 */
	List<List<String>> readExcelData(String planId, String filePath, String worksheetName, Integer startRow,
			Integer endRow, Integer maxRows) throws IOException;

	/**
	 * Write data to a specific worksheet (append or overwrite mode)
	 * @param planId plan identifier
	 * @param filePath path to the Excel file
	 * @param worksheetName name of the worksheet
	 * @param data list of rows to write
	 * @param appendMode true to append, false to overwrite
	 * @throws IOException if file writing fails
	 */
	void writeExcelData(String planId, String filePath, String worksheetName, List<List<String>> data,
			boolean appendMode) throws IOException;

	/**
	 * Write data to a specific worksheet with optional headers (append or overwrite mode)
	 * @param planId plan identifier
	 * @param filePath path to the Excel file
	 * @param worksheetName name of the worksheet
	 * @param data list of rows to write
	 * @param headers optional headers to write as first row (null to skip)
	 * @param appendMode true to append, false to overwrite
	 * @throws IOException if file writing fails
	 */
	void writeExcelDataWithHeaders(String planId, String filePath, String worksheetName, List<List<String>> data,
			List<String> headers, boolean appendMode) throws IOException;

	/**
	 * Update specific cells in a worksheet
	 * @param planId plan identifier
	 * @param filePath path to the Excel file
	 * @param worksheetName name of the worksheet
	 * @param updates map of cell coordinates (e.g., "A1") to new values
	 * @throws IOException if file updating fails
	 */
	void updateExcelCells(String planId, String filePath, String worksheetName, Map<String, String> updates)
			throws IOException;

	/**
	 * Search for rows containing specific keywords in a worksheet
	 * @param planId plan identifier
	 * @param filePath path to the Excel file
	 * @param worksheetName name of the worksheet
	 * @param keywords list of keywords to search for
	 * @param searchColumns specific columns to search (null for all columns)
	 * @return list of matching rows with their row numbers
	 * @throws IOException if file reading fails
	 */
	List<Map<String, Object>> searchExcelData(String planId, String filePath, String worksheetName,
			List<String> keywords, List<String> searchColumns) throws IOException;

	/**
	 * Delete rows from a worksheet by row indices
	 * @param planId plan identifier
	 * @param filePath path to the Excel file
	 * @param worksheetName name of the worksheet
	 * @param rowIndices list of row indices to delete (0-based)
	 * @throws IOException if file updating fails
	 */
	void deleteExcelRows(String planId, String filePath, String worksheetName, List<Integer> rowIndices)
			throws IOException;

	/**
	 * Format cells in a worksheet (font, color, borders, etc.)
	 * @param planId plan identifier
	 * @param filePath path to the Excel file
	 * @param worksheetName name of the worksheet
	 * @param cellRange cell range (e.g., "A1:C10")
	 * @param formatting formatting options as key-value pairs
	 * @throws IOException if file updating fails
	 */
	void formatExcelCells(String planId, String filePath, String worksheetName, String cellRange,
			Map<String, Object> formatting) throws IOException;

	/**
	 * Add formulas to specific cells
	 * @param planId plan identifier
	 * @param filePath path to the Excel file
	 * @param worksheetName name of the worksheet
	 * @param formulas map of cell coordinates to formula strings
	 * @throws IOException if file updating fails
	 */
	void addExcelFormulas(String planId, String filePath, String worksheetName, Map<String, String> formulas)
			throws IOException;

	/**
	 * Process large Excel files in batches with progress callback
	 * @param planId plan identifier
	 * @param filePath path to the Excel file
	 * @param worksheetName name of the worksheet
	 * @param batchSize number of rows to process in each batch
	 * @param processor callback function to process each batch
	 * @throws IOException if file processing fails
	 */
	void processExcelInBatches(String planId, String filePath, String worksheetName, int batchSize,
			BatchProcessor processor) throws IOException;

	/**
	 * Get current processing status for long-running operations
	 * @param planId plan identifier
	 * @return status information including progress percentage
	 */
	Map<String, Object> getProcessingStatus(String planId);

	/**
	 * Clean up resources for a specific plan
	 * @param planId plan identifier
	 */
	void cleanupPlanResources(String planId);

	/**
	 * Process Excel data in parallel batches for better performance
	 * @param planId plan identifier
	 * @param filePath path to the Excel file
	 * @param worksheetName name of the worksheet
	 * @param batchSize number of rows to process in each batch
	 * @param parallelism number of parallel threads to use
	 * @param processor callback function to process each batch
	 * @throws IOException if file processing fails
	 */
	void processExcelInParallelBatches(String planId, String filePath, String worksheetName, int batchSize,
			int parallelism, BatchProcessor processor) throws IOException;

	/**
	 * Transform and aggregate Excel data with custom functions
	 * @param planId plan identifier
	 * @param filePath path to the Excel file
	 * @param worksheetName name of the worksheet
	 * @param transformer data transformation function
	 * @param aggregator data aggregation function
	 * @return aggregated result
	 * @throws IOException if file processing fails
	 */
	<T, R> R transformAndAggregateExcelData(String planId, String filePath, String worksheetName,
			DataTransformer<T> transformer, DataAggregator<T, R> aggregator) throws IOException;

	/**
	 * Stream process large Excel files with minimal memory usage
	 * @param planId plan identifier
	 * @param filePath path to the Excel file
	 * @param worksheetName name of the worksheet
	 * @param streamProcessor stream processing function
	 * @throws IOException if file processing fails
	 */
	void streamProcessExcelData(String planId, String filePath, String worksheetName, StreamProcessor streamProcessor)
			throws IOException;

	/**
	 * Validate and clean Excel data
	 * @param planId plan identifier
	 * @param filePath path to the Excel file
	 * @param worksheetName name of the worksheet
	 * @param validator data validation function
	 * @param cleaner data cleaning function
	 * @return validation and cleaning report
	 * @throws IOException if file processing fails
	 */
	Map<String, Object> validateAndCleanExcelData(String planId, String filePath, String worksheetName,
			DataValidator validator, DataCleaner cleaner) throws IOException;

	/**
	 * Export Excel data to different formats
	 * @param planId plan identifier
	 * @param filePath path to the Excel file
	 * @param worksheetName name of the worksheet
	 * @param outputPath output file path
	 * @param format export format (CSV, JSON, XML)
	 * @param options export options
	 * @throws IOException if export fails
	 */
	void exportExcelData(String planId, String filePath, String worksheetName, String outputPath, ExportFormat format,
			Map<String, Object> options) throws IOException;

	/**
	 * Get detailed performance metrics for processing operations
	 * @param planId plan identifier
	 * @return performance metrics including memory usage, processing time, etc.
	 */
	Map<String, Object> getPerformanceMetrics(String planId);

	/**
	 * Functional interface for batch processing
	 */
	@FunctionalInterface
	interface BatchProcessor {

		/**
		 * Process a batch of data
		 * @param batchData list of rows in the current batch
		 * @param batchNumber current batch number (starting from 1)
		 * @param totalBatches total number of batches
		 * @return true to continue processing, false to stop
		 */
		boolean processBatch(List<List<String>> batchData, int batchNumber, int totalBatches);

	}

	/**
	 * Functional interface for data transformation
	 */
	@FunctionalInterface
	interface DataTransformer<T> {

		/**
		 * Transform a row of data
		 * @param rowData input row data
		 * @param rowIndex row index (0-based)
		 * @return transformed data
		 */
		T transform(List<String> rowData, int rowIndex);

	}

	/**
	 * Functional interface for data aggregation
	 */
	@FunctionalInterface
	interface DataAggregator<T, R> {

		/**
		 * Aggregate transformed data
		 * @param transformedData list of transformed data
		 * @return aggregated result
		 */
		R aggregate(List<T> transformedData);

	}

	/**
	 * Functional interface for stream processing
	 */
	@FunctionalInterface
	interface StreamProcessor {

		/**
		 * Process a single row in streaming mode
		 * @param rowData row data
		 * @param rowIndex row index (0-based)
		 * @return true to continue processing, false to stop
		 */
		boolean processRow(List<String> rowData, int rowIndex);

	}

	/**
	 * Functional interface for data validation
	 */
	@FunctionalInterface
	interface DataValidator {

		/**
		 * Validate a row of data
		 * @param rowData row data to validate
		 * @param rowIndex row index (0-based)
		 * @return validation result with errors if any
		 */
		ValidationResult validate(List<String> rowData, int rowIndex);

	}

	/**
	 * Functional interface for data cleaning
	 */
	@FunctionalInterface
	interface DataCleaner {

		/**
		 * Clean a row of data
		 * @param rowData row data to clean
		 * @param rowIndex row index (0-based)
		 * @return cleaned row data
		 */
		List<String> clean(List<String> rowData, int rowIndex);

	}

	/**
	 * Validation result class
	 */
	class ValidationResult {

		private final boolean valid;

		private final List<String> errors;

		public ValidationResult(boolean valid, List<String> errors) {
			this.valid = valid;
			this.errors = errors != null ? errors : new ArrayList<>();
		}

		public boolean isValid() {
			return valid;
		}

		public List<String> getErrors() {
			return errors;
		}

	}

	/**
	 * Export format enumeration
	 */
	enum ExportFormat {

		CSV, JSON, XML, TSV, PARQUET

	}

}

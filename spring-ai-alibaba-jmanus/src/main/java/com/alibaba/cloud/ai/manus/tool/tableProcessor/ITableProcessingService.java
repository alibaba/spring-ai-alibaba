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
import java.util.List;

/**
 * Interface for table processing operations. Provides methods for creating, reading,
 * updating, and searching table data.
 *
 * Implementation Guidelines: 1. ID Column Handling: - The ID column is a system-reserved
 * field and should be automatically added as the first column - When returning table
 * structure, exclude the ID column from the result - When writing data, handle ID-based
 * updates or insertions intelligently: * If data includes a valid ID that exists, update
 * the corresponding row * If data includes an ID that doesn't exist, insert as a new row
 * * If no ID is specified, generate a new ID automatically - When reading data, exclude
 * the ID column from the returned results 2. Error Handling: - Provide clear error
 * messages for data size mismatches - Validate file types and paths appropriately 3. Path
 * Management: - Support both relative and absolute paths - Use planId to maintain
 * plan-specific context
 */
public interface ITableProcessingService {

	/**
	 * Check if the file type is supported
	 *
	 * Implementation Logic: - Supported file extensions: .xlsx, .xls, .csv -
	 * Case-insensitive file extension matching
	 * @param filePath file path
	 * @return true if supported, false otherwise
	 */
	boolean isSupportedFileType(String filePath);

	/**
	 * Validate and get absolute file path
	 *
	 * Implementation Logic: - Validate that filePath is not null or empty - Resolve
	 * relative paths against the plan directory - Handle absolute paths directly - Update
	 * current file path tracking if needed
	 * @param planId plan ID
	 * @param filePath file path (relative or absolute)
	 * @return absolute Path object
	 * @throws IOException if path validation fails
	 */
	java.nio.file.Path validateFilePath(String planId, String filePath) throws IOException;

	/**
	 * Get absolute path for a given relative path
	 *
	 * Implementation Logic: - Resolve the relative filePath against the plan directory -
	 * Return the absolute path without updating current file tracking
	 * @param planId plan ID
	 * @param filePath file path
	 * @return absolute Path
	 * @throws IOException if path resolution fails
	 */
	java.nio.file.Path getAbsolutePath(String planId, String filePath) throws IOException;

	/**
	 * Update file state for a plan
	 *
	 * Implementation Logic: - Store file state information per planId and filePath - Use
	 * thread-safe data structures for concurrent access - Log state updates for debugging
	 * purposes
	 * @param planId plan ID
	 * @param filePath file path
	 * @param state state message
	 */
	void updateFileState(String planId, String filePath, String state);

	/**
	 * Get last operation result for a plan
	 *
	 * Implementation Logic: - Return the state of the current file for the given plan -
	 * If no current file, return the first available state - Return empty string if no
	 * states are available
	 * @param planId plan ID
	 * @return last operation result
	 */
	String getLastOperationResult(String planId);

	/**
	 * Get current file path for a plan
	 *
	 * Implementation Logic: - Return the last file path that was accessed for this plan -
	 * Return empty string if no file path is tracked for this plan
	 * @param planId plan ID
	 * @return current file path
	 */
	String getCurrentFilePath(String planId);

	/**
	 * Create a new table with headers
	 *
	 * Implementation Logic: - Validate that filePath is a relative path (not absolute) -
	 * Check that headers do not contain "ID" (case-insensitive) as it's a reserved column
	 * name - Create parent directories if they don't exist - Automatically add "ID" as
	 * the first column in the table - Write headers to the file using appropriate format
	 * based on file extension - Update file state to "Success: Table created with
	 * headers"
	 * @param planId plan ID
	 * @param filePath relative file path (absolute path will cause an error)
	 * @param sheetName sheet name
	 * @param headers list of headers (ID column will be added as the first column)
	 * @throws IOException if file operation fails
	 */
	void createTable(String planId, String filePath, String sheetName, List<String> headers) throws IOException;

	/**
	 * Get table structure (headers)
	 *
	 * Implementation Logic: - Validate file path and check file existence - Read the
	 * header row from the file - Exclude the "ID" column from the returned headers -
	 * Return headers in the order they appear in the file (excluding ID)
	 * @param planId plan ID
	 * @param filePath file path (relative or absolute)
	 * @return list of headers
	 * @throws IOException if file operation fails
	 */
	List<String> getTableStructure(String planId, String filePath) throws IOException;

	/**
	 * Write data to table, ensuring data matches header size
	 *
	 * Implementation Logic: - Validate file path and check file existence - Get table
	 * structure to validate data size - Handle ID-based operations: * If data starts with
	 * a valid ID that exists in the table, update that row * If data starts with an ID
	 * that doesn't exist, insert as new row with that ID * If no ID is specified or ID is
	 * invalid, generate a new ID and insert - Validate that data size matches expected
	 * columns (excluding ID column) - Write updated data back to file - Update file state
	 * to "Success: Data written to table"
	 * @param planId plan ID
	 * @param filePath file path (relative or absolute)
	 * @param data data to write (must match header size)
	 * @throws IOException if file operation fails or data size mismatch
	 */
	void writeDataToTable(String planId, String filePath, List<String> data) throws IOException;

	/**
	 * Write multiple rows of data to table, ensuring data matches header size
	 *
	 * Implementation Logic: - Validate file type and check file existence - Get table
	 * structure to validate data size for all rows - For each row: * Handle ID-based
	 * operations: - If row starts with a valid ID that exists, update that row - If row
	 * starts with an ID that doesn't exist, insert as new row - If no ID is specified or
	 * ID is invalid, generate a new ID * Validate that row data size matches expected
	 * columns (excluding ID column) - Write all updated data back to file - Update file
	 * state to "Success: Multiple rows written to table"
	 * @param planId plan ID
	 * @param filePath file path (relative or absolute)
	 * @param data list of data rows to write (each row must match header size)
	 * @throws IOException if file operation fails or data size mismatch
	 */
	void writeMultipleRowsToTable(String planId, String filePath, List<List<String>> data) throws IOException;

	/**
	 * Search for rows matching keywords
	 *
	 * Implementation Logic: - Validate file path and check file existence - Read all data
	 * from the file - Search for rows that contain any of the specified keywords in any
	 * cell - Exclude header row from search results - Return matching rows (without ID
	 * column)
	 * @param planId plan ID
	 * @param filePath file path (relative or absolute)
	 * @param keywords list of keywords to search for
	 * @return list of matching rows
	 * @throws IOException if file operation fails
	 */
	List<List<String>> searchRows(String planId, String filePath, List<String> keywords) throws IOException;

	/**
	 * Delete rows by list of row indices
	 *
	 * Implementation Logic: - Validate file path and check file existence - Read all data
	 * from the file - Validate that all row indices are within valid range - Sort indices
	 * in descending order to avoid index shifting issues during deletion - Remove
	 * specified rows (adjusting for header row) - Write remaining data back to file -
	 * Update file state to "Success: Rows deleted"
	 * @param planId plan ID
	 * @param filePath file path (relative or absolute)
	 * @param rowIndices list of row indices to delete (0-based, excluding header)
	 * @throws IOException if file operation fails
	 */
	void deleteRowsByList(String planId, String filePath, List<Integer> rowIndices) throws IOException;

	/**
	 * Clean up plan directory resources
	 *
	 * Implementation Logic: - Remove all state tracking for the specified planId - Clean
	 * up any temporary resources associated with the plan - Log cleanup operation for
	 * debugging purposes
	 * @param planId plan ID
	 */
	void cleanupPlanDirectory(String planId);

}

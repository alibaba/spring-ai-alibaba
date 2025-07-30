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
import java.util.List;

/**
 * Interface for table processing operations.
 * Provides methods for creating, reading, updating, and searching table data.
 */
public interface ITableProcessingService {

	/**
	 * Check if the file type is supported
	 * 
	 * @param filePath file path
	 * @return true if supported, false otherwise
	 */
	boolean isSupportedFileType(String filePath);

	/**
	 * Validate and get absolute file path
	 * 
	 * @param planId   plan ID
	 * @param filePath file path (relative or absolute)
	 * @return absolute Path object
	 * @throws IOException if path validation fails
	 */
	java.nio.file.Path validateFilePath(String planId, String filePath) throws IOException;

	/**
	 * Get absolute path for a given relative path
	 * 
	 * @param planId   plan ID
	 * @param filePath file path
	 * @return absolute Path
	 * @throws IOException if path resolution fails
	 */
	java.nio.file.Path getAbsolutePath(String planId, String filePath) throws IOException;

	/**
	 * Update file state for a plan
	 * 
	 * @param planId   plan ID
	 * @param filePath file path
	 * @param state    state message
	 */
	void updateFileState(String planId, String filePath, String state);

	/**
	 * Get last operation result for a plan
	 * 
	 * @param planId plan ID
	 * @return last operation result
	 */
	String getLastOperationResult(String planId);

	/**
	 * Get current file path for a plan
	 * 
	 * @param planId plan ID
	 * @return current file path
	 */
	String getCurrentFilePath(String planId);

	/**
	 * Create a new table with headers
	 * 
	 * @param planId    plan ID
	 * @param filePath  relative file path (absolute path will cause an error)
	 * @param sheetName sheet name
	 * @param headers   list of headers (ID column will be added as the first column)
	 * @throws IOException if file operation fails
	 */
	void createTable(String planId, String filePath, String sheetName, List<String> headers) throws IOException;

	/**
	 * Get table structure (headers)
	 * 
	 * @param planId   plan ID
	 * @param filePath file path (relative or absolute)
	 * @return list of headers
	 * @throws IOException if file operation fails
	 */
	List<String> getTableStructure(String planId, String filePath) throws IOException;

	/**
	 * Write data to table, ensuring data matches header size
	 * 
	 * @param planId   plan ID
	 * @param filePath file path (relative or absolute)
	 * @param data     data to write (must match header size)
	 * @throws IOException if file operation fails or data size mismatch
	 */
	void writeDataToTable(String planId, String filePath, List<String> data) throws IOException;

	/**
	 * Search for rows matching keywords
	 * 
	 * @param planId   plan ID
	 * @param filePath file path (relative or absolute)
	 * @param keywords list of keywords to search for
	 * @return list of matching rows
	 * @throws IOException if file operation fails
	 */
	List<List<String>> searchRows(String planId, String filePath, List<String> keywords) throws IOException;

	/**
	 * Delete rows by list of row indices
	 * 
	 * @param planId     plan ID
	 * @param filePath   file path (relative or absolute)
	 * @param rowIndices list of row indices to delete (0-based, excluding header)
	 * @throws IOException if file operation fails
	 */
	void deleteRowsByList(String planId, String filePath, List<Integer> rowIndices) throws IOException;

	/**
	 * Clean up plan directory resources
	 * 
	 * @param planId plan ID
	 */
	void cleanupPlanDirectory(String planId);

}
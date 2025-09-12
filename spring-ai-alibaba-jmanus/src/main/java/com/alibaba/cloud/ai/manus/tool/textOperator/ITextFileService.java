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
package com.alibaba.cloud.ai.manus.tool.textOperator;

import java.io.IOException;

import com.alibaba.cloud.ai.manus.config.ManusProperties;
import com.alibaba.cloud.ai.manus.tool.innerStorage.SmartContentSavingService;

/**
 * Text file service interface providing file operation management functions
 */
public interface ITextFileService {

	/**
	 * Get internal storage service
	 * @return Internal storage service
	 */
	SmartContentSavingService getInnerStorageService();

	/**
	 * Get file status for specified plan
	 * @param planId Plan ID
	 * @return File status
	 */
	Object getFileState(String planId);

	/**
	 * Close files for specified plan
	 * @param planId Plan ID
	 */
	void closeFileForPlan(String planId);

	/**
	 * Check if file type is supported
	 * @param filePath File path
	 * @return Whether supported
	 */
	boolean isSupportedFileType(String filePath);

	/**
	 * Get file extension
	 * @param filePath File path
	 * @return File extension
	 */
	String getFileExtension(String filePath);

	/**
	 * Validate and get absolute path
	 * @param workingDirectoryPath Working directory path
	 * @param filePath File path
	 * @throws IOException IO exception
	 */
	void validateAndGetAbsolutePath(String workingDirectoryPath, String filePath) throws IOException;

	/**
	 * Update file status
	 * @param planId Plan ID
	 * @param filePath File path
	 * @param operationResult Operation result
	 */
	void updateFileState(String planId, String filePath, String operationResult);

	/**
	 * Get current file path
	 * @param planId Plan ID
	 * @return Current file path
	 */
	String getCurrentFilePath(String planId);

	/**
	 * Get Manus properties
	 * @return Manus properties
	 */
	ManusProperties getManusProperties();

	/**
	 * Get last operation result
	 * @param planId Plan ID
	 * @return Last operation result
	 */
	String getLastOperationResult(String planId);

	/**
	 * Clean up resources
	 */
	void cleanup();

}

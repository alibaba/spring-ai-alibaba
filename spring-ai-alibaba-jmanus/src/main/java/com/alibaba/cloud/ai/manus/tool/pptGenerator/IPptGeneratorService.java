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
package com.alibaba.cloud.ai.manus.tool.pptGenerator;

import java.io.IOException;

import com.alibaba.cloud.ai.manus.config.ManusProperties;
import com.alibaba.cloud.ai.manus.tool.textOperator.FileState;

/**
 * PPT generator service interface, providing PPT file operation management functions
 */
public interface IPptGeneratorService {

	/**
	 * Get the file state
	 * @param planId Plan ID
	 * @return File state
	 */
	FileState getFileState(String planId);

	/**
	 * Update the file state
	 * @param planId Plan ID
	 * @param filePath File path
	 * @param operationResult Operation result
	 */
	void updateFileState(String planId, String filePath, String operationResult);

	/**
	 * Get the current file path
	 * @param planId Plan ID
	 * @return Current file path
	 */
	String getCurrentFilePath(String planId);

	/**
	 * Get the last operation result
	 * @param planId Plan ID
	 * @return Last operation result
	 */
	String getLastOperationResult(String planId);

	/**
	 * Validate PPT file path
	 * @param planId Plan ID
	 * @param filePath File path
	 * @return Validated absolute path
	 * @throws IOException IO exception
	 */
	String validatePptFilePath(String planId, String filePath) throws IOException;

	/**
	 * Check if the file type is supported
	 * @param filePath File path
	 * @return True if supported, false otherwise
	 */
	boolean isSupportedPptFileType(String filePath);

	/**
	 * Get the file extension
	 * @param filePath File path
	 * @return File extension
	 */
	String getFileExtension(String filePath);

	/**
	 * Clean up plan resources
	 * @param planId Plan ID
	 */
	void cleanupForPlan(String planId);

	/**
	 * Get the Manus configuration properties
	 * @return Manus configuration properties
	 */
	ManusProperties getManusProperties();

	/**
	 * Get template list
	 * @return Template list in JSON format
	 * @throws IOException IO exception
	 */
	String getTemplateList() throws IOException;

	/**
	 * Get template content
	 * @param path Template path
	 * @return Template content in JSON format
	 * @throws IOException IO exception
	 */
	String getTemplate(String path) throws IOException;

}

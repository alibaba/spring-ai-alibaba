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
package com.alibaba.cloud.ai.manus.tool.filesystem;

import java.io.IOException;
import java.nio.file.Path;

import com.alibaba.cloud.ai.manus.config.ManusProperties;

/**
 * Unified directory manager interface providing centralized directory management for all
 * tools' filesystem operations
 */
public interface IUnifiedDirectoryManager {

	/**
	 * Get working directory path
	 * @return Working directory path string
	 */
	String getWorkingDirectoryPath();

	/**
	 * Get working directory
	 * @return Working directory Path object
	 */
	Path getWorkingDirectory();

	/**
	 * Get root plan directory
	 * @param rootPlanId Root plan ID
	 * @return Root plan directory Path
	 */
	Path getRootPlanDirectory(String rootPlanId);

	/**
	 * Get subtask directory
	 * @param rootPlanId Root plan ID
	 * @param subTaskId Subtask ID
	 * @return Subtask directory Path
	 */
	Path getSubTaskDirectory(String rootPlanId, String subTaskId);

	/**
	 * Get specified directory
	 * @param targetPath Target path
	 * @return Specified directory Path
	 * @throws IOException IO exception
	 * @throws SecurityException Security exception
	 */
	Path getSpecifiedDirectory(String targetPath) throws IOException, SecurityException;

	/**
	 * Ensure directory exists
	 * @param directory Directory
	 * @throws IOException IO exception
	 */
	void ensureDirectoryExists(Path directory) throws IOException;

	/**
	 * Check if path is allowed
	 * @param targetPath Target path
	 * @return Whether allowed
	 */
	boolean isPathAllowed(Path targetPath);

	/**
	 * Get internal storage root directory
	 * @return Internal storage root directory Path
	 */
	Path getInnerStorageRoot();

	/**
	 * Get relative path from working directory
	 * @param absolutePath Absolute path
	 * @return Relative path string
	 */
	String getRelativePathFromWorkingDirectory(Path absolutePath);

	/**
	 * Get Manus properties
	 * @return Manus properties
	 */
	ManusProperties getManusProperties();

	/**
	 * Clean up subtask directory
	 * @param rootPlanId Root plan ID
	 * @param subTaskId Subtask ID
	 * @throws IOException IO exception
	 */
	void cleanupSubTaskDirectory(String rootPlanId, String subTaskId) throws IOException;

	/**
	 * Clean up root plan directory
	 * @param rootPlanId Root plan ID
	 * @throws IOException IO exception
	 */
	void cleanupRootPlanDirectory(String rootPlanId) throws IOException;

}

/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.extension.file;

import java.util.List;

/**
 * Backend interface for file storage operations.
 *
 * Protocol for pluggable memory backends (single, unified).
 * Backends can store files in different locations (state, filesystem, database, etc.)
 * and provide a uniform interface for file operations.
 *
 * NOTE: This interface supports different backend implementations:
 * - StateBackend: Stores files in agent state (ephemeral)
 * - StoreBackend: Persistent storage using Store
 * - CompositeBackend: Routes operations to different backends by path
 * - FilesystemBackend: Direct filesystem operations
 */
public interface FilesystemBackend {
	/**
	 * Read file content with line numbers and pagination.
	 *
	 * @param filePath Absolute or relative file path
	 * @param offset Line offset to start reading from (0-indexed)
	 * @param limit Maximum number of lines to read
	 * @return Formatted file content with line numbers, or error message
	 */
	String read(String filePath, int offset, int limit);

	/**
	 * Write content to a new file.
	 *
	 * @param filePath Absolute or relative file path
	 * @param content Content to write
	 * @return WriteResult with path or error. External storage sets filesUpdate=null.
	 */
	WriteResult write(String filePath, String content);

	/**
	 * Edit file by replacing string occurrences.
	 *
	 * @param filePath Absolute or relative file path
	 * @param oldString String to replace
	 * @param newString Replacement string
	 * @param replaceAll Whether to replace all occurrences
	 * @return EditResult with occurrences count or error. External storage sets filesUpdate=null.
	 */
	EditResult edit(String filePath, String oldString, String newString, boolean replaceAll);

	/**
	 * List files and directories in the specified directory (non-recursive).
	 *
	 * @param path Absolute directory path to list files from
	 * @return List of FileInfo objects for files and directories directly in the directory.
	 *         Directories have a trailing / in their path and isDir=true.
	 */
	List<FileInfo> lsInfo(String path);

	/**
	 * Find files matching glob pattern.
	 *
	 * @param pattern Glob pattern (e.g., "*.py", "**​/*.ts")
	 * @param path Base path to search from
	 * @return List of FileInfo objects for matching files
	 */
	List<FileInfo> globInfo(String pattern, String path);

	/**
	 * Search for pattern in files using regex.
	 *
	 * @param pattern Regex pattern to search for
	 * @param path Base path to search from (null for current directory)
	 * @param glob Glob pattern to filter files (null for all files)
	 * @return List of GrepMatch objects or error message
	 */
	Object grepRaw(String pattern, String path, String glob);
}


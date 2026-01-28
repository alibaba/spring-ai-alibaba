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
package com.alibaba.cloud.ai.graph.agent.extension.tools.filesystem;

import com.alibaba.cloud.ai.graph.agent.extension.file.FileInfo;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.nio.file.*;
import java.util.*;

/**
 * File system tools using Spring AI Tool annotations.
 * 
 * This class provides file system operations as Spring AI tools:
 * - read_file: Read file content with pagination
 * - write_file: Write content to a file
 * - edit_file: Edit file by replacing string occurrences
 * - list_files: List files and directories
 * 
 * This implementation is based on LocalFilesystemBackend but uses
 * Spring AI Tool annotations for better integration with the framework.
 */
public class FileSystemTools {

	private static final int DEFAULT_MAX_FILE_SIZE_MB = 10;

	private final Path cwd;
	private final boolean virtualMode;
	private final long maxFileSizeBytes;

	/**
	 * Default constructor using current working directory.
	 */
	public FileSystemTools() {
		this(null, false, DEFAULT_MAX_FILE_SIZE_MB);
	}

	/**
	 * Constructor with custom root directory.
	 * 
	 * @param rootDir Optional root directory for file operations
	 * @param virtualMode When true, treat incoming paths as virtual absolute paths under cwd
	 * @param maxFileSizeMb Maximum file size in MB for reading operations
	 */
	public FileSystemTools(String rootDir, boolean virtualMode, int maxFileSizeMb) {
		this.cwd = rootDir != null ? Paths.get(rootDir).toAbsolutePath().normalize() : Paths.get("").toAbsolutePath();
		this.virtualMode = virtualMode;
		this.maxFileSizeBytes = maxFileSizeMb * 1024L * 1024L;
	}

	/**
	 * Resolve a file path with security checks.
	 */
	private Path resolvePath(String key) throws IllegalArgumentException {
		if (virtualMode) {
			String vpath = key.startsWith("/") ? key : "/" + key;
			if (vpath.contains("..") || vpath.startsWith("~")) {
				throw new IllegalArgumentException("Path traversal not allowed");
			}
			Path full = cwd.resolve(vpath.substring(1)).normalize();
			if (!full.startsWith(cwd)) {
				throw new IllegalArgumentException("Path:" + full + " outside root directory: " + cwd);
			}
			return full;
		}

		Path path = Paths.get(key);
		if (path.isAbsolute()) {
			return path;
		}
		return cwd.resolve(path).normalize();
	}

	// @formatter:off
	@Tool(name = "read_file", description = """
		Reads a file from the filesystem. You can access any file directly by using this tool.
		Assume this tool is able to read all files on the machine. If the User provides a path to a file assume that path is valid. It is okay to read a file that does not exist; an error will be returned.

		Usage:
		- The file_path parameter must be an absolute path, not a relative path
		- By default, it reads up to 500 lines starting from the beginning of the file
		- **IMPORTANT for large files and codebase exploration**: Use pagination with offset and limit parameters to avoid context overflow
		  - First scan: read_file(path, limit=100) to see file structure
		  - Read more sections: read_file(path, offset=100, limit=200) for next 200 lines
		  - Only omit limit (read full file) when necessary for editing
		- Specify offset and limit: read_file(path, offset=0, limit=100) reads first 100 lines
		- Any lines longer than 10000 characters will be truncated
		- Results are returned using cat -n format, with line numbers starting at 1
		- You have the capability to call multiple tools in a single response. It is always better to speculatively read multiple files as a batch that are potentially useful.
		- If you read a file that exists but has empty contents you will receive a system reminder warning in place of file contents.
		- You should almost ALWAYS use the list_files tool before using this tool to verify the file path.
		""")
	public String readFile(
		@ToolParam(description = "The absolute path of the file to read") String filePath,
		@ToolParam(description = "Line offset to start reading from (default: 0)", required = false) Integer offset,
		@ToolParam(description = "Maximum number of lines to read (default: 500)", required = false) Integer limit,
		ToolContext toolContext) { // @formatter:on

		try {
			Path resolvedPath = resolvePath(filePath);
			return ReadFileTool.readFileContent(resolvedPath, offset, limit, true);
		}
		catch (IllegalArgumentException e) {
			return "Error: " + e.getMessage();
		}
		catch (Exception e) {
			return "Error reading file '" + filePath + "': " + e.getMessage();
		}
	}

	// @formatter:off
	@Tool(name = "write_file", description = """
		Writes content to a new file in the filesystem.

		Usage:
		- The file_path parameter must be an absolute path, not a relative path
		- This tool will create a new file. If the file already exists, an error will be returned.
		- To modify an existing file, use the edit_file tool instead.
		- Parent directories will be created automatically if they don't exist.
		""")
	public String writeFile(
		@ToolParam(description = "The absolute path of the file to write") String filePath,
		@ToolParam(description = "The content to write to the file") String content,
		ToolContext toolContext) { // @formatter:on

		try {
			Path resolvedPath = resolvePath(filePath);
			return WriteFileTool.writeFileContent(resolvedPath, content);
		}
		catch (IllegalArgumentException e) {
			return "Error: " + e.getMessage();
		}
		catch (Exception e) {
			return "Error writing file '" + filePath + "': " + e.getMessage();
		}
	}

	// @formatter:off
	@Tool(name = "edit_file", description = """
		Edits a file by replacing string occurrences.

		Usage:
		- The file_path parameter must be an absolute path, not a relative path
		- You must use the read_file tool at least once before editing to read the file's contents
		- When editing text, ensure you preserve the exact indentation (tabs/spaces) as it appears in the file
		- The edit will FAIL if old_string is not unique in the file. Either provide a larger string with more surrounding context to make it unique or use replace_all=true to change every instance
		- Use replace_all=true for replacing and renaming strings across the file
		""")
	public String editFile(
		@ToolParam(description = "The absolute path of the file to modify") String filePath,
		@ToolParam(description = "The text to replace") String oldString,
		@ToolParam(description = "The text to replace it with (must be different from old_string)") String newString,
		@ToolParam(description = "Replace all occurrences of old_string (default false)", required = false) Boolean replaceAll,
		ToolContext toolContext) { // @formatter:on

		try {
			Path resolvedPath = resolvePath(filePath);
			boolean replaceAllFlag = Boolean.TRUE.equals(replaceAll);
			return EditFileTool.editFileContent(resolvedPath, oldString, newString, replaceAllFlag);
		}
		catch (IllegalArgumentException e) {
			return "Error: " + e.getMessage();
		}
		catch (Exception e) {
			return "Error editing file '" + filePath + "': " + e.getMessage();
		}
	}

	// @formatter:off
	@Tool(name = "list_files", description = """
		Lists all files and directories in the specified directory.

		Usage:
		- The path parameter must be an absolute path, not a relative path
		- The list_files tool will return a list of all files and directories in the specified directory (non-recursive)
		- Directories are indicated with a trailing / in their path
		- This is very useful for exploring the file system and finding the right file to read or edit
		- You should almost ALWAYS use this tool before using the read_file or edit_file tools
		""")
	public String listFiles(
		@ToolParam(description = "The directory path to list files from") String path,
		ToolContext toolContext) { // @formatter:on

		try {
			Path dirPath = resolvePath(path);
			if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
				return "Error: Directory not found: " + path;
			}

			List<FileInfo> results = ListFilesTool.listFilesContent(dirPath, virtualMode ? cwd : null, virtualMode);

			// Format output
			StringBuilder result = new StringBuilder();
			for (FileInfo info : results) {
				result.append(info.getPath());
				if (info.getSize() != null) {
					result.append(" (").append(info.getSize()).append(" bytes)");
				}
				if (info.getModifiedAt() != null) {
					result.append(" [").append(info.getModifiedAt()).append("]");
				}
				result.append("\n");
			}

			return !result.isEmpty() ? result.toString().trim() : "Directory is empty";
		}
		catch (IllegalArgumentException e) {
			return "Error: " + e.getMessage();
		}
		catch (Exception e) {
			return "Error listing directory '" + path + "': " + e.getMessage();
		}
	}

	// Helper methods

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private String rootDir;
		private boolean virtualMode = false;
		private int maxFileSizeMb = DEFAULT_MAX_FILE_SIZE_MB;

		public Builder rootDir(String rootDir) {
			this.rootDir = rootDir;
			return this;
		}

		public Builder virtualMode(boolean virtualMode) {
			this.virtualMode = virtualMode;
			return this;
		}

		public Builder maxFileSizeMb(int maxFileSizeMb) {
			this.maxFileSizeMb = maxFileSizeMb;
			return this;
		}

		public FileSystemTools build() {
			return new FileSystemTools(rootDir, virtualMode, maxFileSizeMb);
		}
	}
}

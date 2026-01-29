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

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.function.BiFunction;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Tool for reading file contents with pagination support.
 */
public class ReadFileTool implements BiFunction<ReadFileTool.ReadFileRequest, ToolContext, String> {

	private static final String EMPTY_CONTENT_WARNING = "System reminder: File exists but has empty contents";
	private static final int MAX_LINE_LENGTH = 10000;
	private static final int LINE_NUMBER_WIDTH = 6;

	public static final String DESCRIPTION = """
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
- Any lines longer than 2000 characters will be truncated
- Results are returned using cat -n format, with line numbers starting at 1
- You have the capability to call multiple tools in a single response. It is always better to speculatively read multiple files as a batch that are potentially useful.
- If you read a file that exists but has empty contents you will receive a system reminder warning in place of file contents.
			- You should almost ALWAYS use the list_files tool before using this tool to verify the file path.
			""";

	public ReadFileTool() {
	}

	@Override
	public String apply(ReadFileRequest request, ToolContext toolContext) {
		try {
			Path path = Paths.get(request.filePath);
			return readFileContent(path, request.offset, request.limit, true);
		}
		catch (Exception e) {
			return "Error reading file: " + e.getMessage();
		}
	}

	/**
	 * Core logic for reading file content with pagination.
	 * This method can be reused by other classes like FileSystemTools.
	 *
	 * @param filePath The path to the file to read
	 * @param offset Line offset to start reading from (null means 0)
	 * @param limit Maximum number of lines to read (null means 500)
	 * @param checkEmpty Whether to check for empty content and return warning
	 * @return Formatted file content with line numbers, or error message
	 */
	public static String readFileContent(Path filePath, Integer offset, Integer limit, boolean checkEmpty) {
		try {
			if (!Files.exists(filePath) || !Files.isRegularFile(filePath, LinkOption.NOFOLLOW_LINKS)) {
				return "Error: File '" + filePath + "' not found";
			}

			String content = Files.readString(filePath);

			if (checkEmpty) {
				String emptyMsg = checkEmptyContent(content);
				if (emptyMsg != null) {
					return emptyMsg;
				}
			}

			String[] lines = content.split("\n", -1);
			// Remove trailing empty line if present
			if (lines.length > 0 && lines[lines.length - 1].isEmpty()) {
				lines = Arrays.copyOf(lines, lines.length - 1);
			}

			int startIdx = offset != null ? offset : 0;
			int endIdx = Math.min(startIdx + (limit != null ? limit : 500), lines.length);

			if (startIdx >= lines.length) {
				return "Error: Line offset " + startIdx + " exceeds file length (" + lines.length + " lines)";
			}

			String[] selectedLines = Arrays.copyOfRange(lines, startIdx, endIdx);
			return formatContentWithLineNumbers(selectedLines, startIdx + 1);
		}
		catch (IOException e) {
			return "Error reading file '" + filePath + "': " + e.getMessage();
		}
	}

	/**
	 * Check if content is empty and return warning message if so.
	 */
	private static String checkEmptyContent(String content) {
		if (content == null || content.trim().isEmpty()) {
			return EMPTY_CONTENT_WARNING;
		}
		return null;
	}

	/**
	 * Format lines with line numbers (cat -n format).
	 * Handles long lines by splitting them into chunks.
	 */
	private static String formatContentWithLineNumbers(String[] lines, int startLine) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			int lineNum = i + startLine;

			if (line.length() <= MAX_LINE_LENGTH) {
				result.append(String.format("%" + LINE_NUMBER_WIDTH + "d\t%s\n", lineNum, line));
			}
			else {
				// Split long line into chunks with continuation markers
				int numChunks = (line.length() + MAX_LINE_LENGTH - 1) / MAX_LINE_LENGTH;
				for (int chunkIdx = 0; chunkIdx < numChunks; chunkIdx++) {
					int start = chunkIdx * MAX_LINE_LENGTH;
					int end = Math.min(start + MAX_LINE_LENGTH, line.length());
					String chunk = line.substring(start, end);
					if (chunkIdx == 0) {
						result.append(String.format("%" + LINE_NUMBER_WIDTH + "d\t%s\n", lineNum, chunk));
					}
					else {
						String continuationMarker = lineNum + "." + chunkIdx;
						result.append(String.format("%" + LINE_NUMBER_WIDTH + "s\t%s\n", continuationMarker, chunk));
					}
				}
			}
		}
		// Remove trailing newline
		if (!result.isEmpty() && result.charAt(result.length() - 1) == '\n') {
			result.setLength(result.length() - 1);
		}
		return result.toString();
	}

	public static ToolCallback createReadFileToolCallback(String description) {
		return FunctionToolCallback.builder("read_file", new ReadFileTool())
				.description(description)
				.inputType(ReadFileRequest.class)
				.build();
	}

	/**
	 * Request structure for reading a file.
	 */
	public static class ReadFileRequest {

		@JsonProperty(required = true, value = "file_path")
		@JsonPropertyDescription("The absolute path of the file to read")
		public String filePath;

		@JsonProperty(value = "offset")
		@JsonPropertyDescription("Line offset to start reading from (default: 0)")
		public Integer offset;

		@JsonProperty(value = "limit")
		@JsonPropertyDescription("Maximum number of lines to read (default: 500)")
		public Integer limit;

		public ReadFileRequest() {
		}

		public ReadFileRequest(String filePath, Integer offset, Integer limit) {
			this.filePath = filePath;
			this.offset = offset;
			this.limit = limit;
		}
	}
}

/*
 * Copyright 2024-2025 the original author or authors.
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.BiFunction;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Tool for reading file contents with pagination support.
 */
public class ReadFileTool implements BiFunction<ReadFileTool.ReadFileRequest, ToolContext, String> {

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
			List<String> allLines = Files.readAllLines(path);

			// Apply pagination
			int start = request.offset != null ? request.offset : 0;
			int limit = request.limit != null ? request.limit : 500;
			int end = Math.min(start + limit, allLines.size());

			if (start >= allLines.size()) {
				return "Error: Offset " + start + " is beyond file length " + allLines.size();
			}

			List<String> lines = allLines.subList(start, end);

			// Add line numbers (cat -n format)
			StringBuilder result = new StringBuilder();
			for (int i = 0; i < lines.size(); i++) {
				result.append(String.format("%6d\t%s\n", start + i + 1, lines.get(i)));
			}

			return result.toString();
		}
		catch (IOException e) {
			return "Error reading file: " + e.getMessage();
		}
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

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
import java.nio.file.StandardOpenOption;
import java.util.function.BiFunction;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Tool for editing files using string replacement.
 */
public class EditFileTool implements BiFunction<EditFileTool.EditFileRequest, ToolContext, String> {

	public static final String DESCRIPTION = """
			Performs exact string replacements in files.
			
			Usage:
			- You must use your `read_file` tool at least once before editing.
			- When editing text from read_file output, preserve exact indentation
			- ALWAYS prefer editing existing files. NEVER write new files unless explicitly required.
			- The edit will FAIL if `old_string` is not unique in the file.
			- After editing, verify the changes by using the read_file tool.
			""";

	public EditFileTool() {
	}

	@Override
	public String apply(EditFileRequest request, ToolContext toolContext) {
		try {
			Path path = Paths.get(request.filePath);
			return editFileContent(path, request.oldString, request.newString, request.replaceAll);
		}
		catch (Exception e) {
			return "Error editing file: " + e.getMessage();
		}
	}

	/**
	 * Core logic for editing file content by replacing string occurrences.
	 * This method can be reused by other classes like FileSystemTools.
	 *
	 * @param filePath The path to the file to edit
	 * @param oldString The text to replace
	 * @param newString The text to replace it with
	 * @param replaceAll Whether to replace all occurrences (true) or only if unique (false)
	 * @return Success message with count of replacements, or error message
	 */
	public static String editFileContent(Path filePath, String oldString, String newString, boolean replaceAll) {
		try {
			if (!Files.exists(filePath) || !Files.isRegularFile(filePath, LinkOption.NOFOLLOW_LINKS)) {
				return "Error: File '" + filePath + "' not found";
			}

			String content = Files.readString(filePath);

			// Count occurrences
			int occurrences = countOccurrences(content, oldString);

			if (occurrences == 0) {
				return "Error: String not found in file: '" + oldString + "'";
			}

			// Check for uniqueness if not replace_all
			if (!replaceAll && occurrences > 1) {
				return "Error: String '" + oldString + "' appears " + occurrences +
					" times in file. Use replaceAll=true to replace all instances, or provide a more specific string with surrounding context.";
			}

			// Perform replacement
			String newContent;
			if (replaceAll) {
				newContent = content.replace(oldString, newString);
			}
			else {
				// Replace only the first occurrence using literal string matching
				// Note: replaceFirst() treats the first argument as a regex, which can cause issues
				// with special characters. We use indexOf() + substring() for literal matching.
				int replaceIndex = content.indexOf(oldString);
				if (replaceIndex != -1) {
					newContent = content.substring(0, replaceIndex) + newString
						+ content.substring(replaceIndex + oldString.length());
				}
				else {
					// Should not reach here as we already checked for existence
					newContent = content;
				}
			}

			// Write the modified content back
			Files.writeString(filePath, newContent,
				StandardOpenOption.TRUNCATE_EXISTING,
				StandardOpenOption.WRITE);

			return String.format("Successfully edited file: %s (replaced %d occurrence(s))", filePath, occurrences);
		}
		catch (IOException e) {
			return "Error editing file '" + filePath + "': " + e.getMessage();
		}
	}

	/**
	 * Count occurrences of a substring in content.
	 */
	private static int countOccurrences(String content, String search) {
		int count = 0;
		int index = 0;
		while ((index = content.indexOf(search, index)) != -1) {
			count++;
			index += search.length();
		}
		return count;
	}

	public static ToolCallback createEditFileToolCallback(String description) {
		return FunctionToolCallback.builder("edit_file", new EditFileTool())
				.description(description)
				.inputType(EditFileRequest.class)
				.build();
	}

	/**
	 * Request structure for editing a file.
	 */
	public static class EditFileRequest {

		@JsonProperty(required = true, value = "file_path")
		@JsonPropertyDescription("The absolute path of the file to edit")
		public String filePath;

		@JsonProperty(required = true, value = "old_string")
		@JsonPropertyDescription("The exact string to find and replace")
		public String oldString;

		@JsonProperty(required = true, value = "new_string")
		@JsonPropertyDescription("The new string to replace with")
		public String newString;

		@JsonProperty(value = "replace_all")
		@JsonPropertyDescription("If true, replace all occurrences; if false, only replace if unique (default: false)")
		public boolean replaceAll = false;

		public EditFileRequest() {
		}

		public EditFileRequest(String filePath, String oldString, String newString, boolean replaceAll) {
			this.filePath = filePath;
			this.oldString = oldString;
			this.newString = newString;
			this.replaceAll = replaceAll;
		}
	}
}

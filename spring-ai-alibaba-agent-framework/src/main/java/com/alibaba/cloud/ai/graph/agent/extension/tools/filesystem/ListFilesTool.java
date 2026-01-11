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
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Tool for listing files in a directory.
 */
public class ListFilesTool implements BiFunction<String, ToolContext, String> {

	public static final String DESCRIPTION = """
			Lists all files in the filesystem, filtering by directory.
			
			Usage:
			- The path parameter must be an absolute path, not a relative path
			- The list_files tool will return a list of all files in the specified directory.
			- This is very useful for exploring the file system and finding the right file to read or edit.
			- You should almost ALWAYS use this tool before using the Read or Edit tools.
			""";

	public ListFilesTool() {
	}

	@Override
	public String apply(
			@ToolParam(description = "The directory path to list files from") String path,
			ToolContext toolContext) {
		// Parse path from arguments
		File dir = new File(path);
		if (!dir.exists() || !dir.isDirectory()) {
			return "Error: Directory not found: " + path;
		}

		File[] files = dir.listFiles();
		if (files == null) {
			return "Error: Cannot read directory: " + path;
		}

		List<String> filePaths = new ArrayList<>();
		for (File file : files) {
			filePaths.add(file.getAbsolutePath());
		}

		return String.join("\n", filePaths);
	}

	public static ToolCallback createListFilesToolCallback(String description) {
		return FunctionToolCallback.builder("ls", new ListFilesTool())
				.description(description)
				.inputType(String.class)
				.build();
	}
}


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

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Tool for finding files matching a glob pattern.
 */
public class GlobTool implements BiFunction<String, ToolContext, String> {

	public static final String DESCRIPTION = """
			Find files matching a glob pattern.
			
			Usage:
			- Supports standard glob patterns: `*` (any characters), `**` (any directories), `?` (single character)
			- Returns a list of absolute file paths that match the pattern
			
			Examples:
			- `**/*.java` - Find all Java files
			- `*.txt` - Find all text files in root
			- `/src/**/*.xml` - Find all XML files under /src
			""";

	public GlobTool() {
	}

	@Override
	public String apply(
			@ToolParam(description = "The glob pattern to match files") String pattern,
			ToolContext toolContext) {
		try {
			Path basePathObj = Paths.get(System.getProperty("user.dir"));
			PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);

			List<String> matchedFiles = new ArrayList<>();

			Files.walk(basePathObj)
					.filter(Files::isRegularFile)
					.filter(path -> {
						Path relativePath = basePathObj.relativize(path);
						return matcher.matches(relativePath) || matcher.matches(path);
					})
					.forEach(path -> matchedFiles.add(path.toString()));

			if (matchedFiles.isEmpty()) {
				return "No files found matching pattern: " + pattern;
			}

			return String.join("\n", matchedFiles);
		}
		catch (IOException e) {
			return "Error searching for files: " + e.getMessage();
		}
	}

	public static ToolCallback createGlobToolCallback(String description) {
		return FunctionToolCallback.builder("glob", new GlobTool())
				.description(description)
				.inputType(String.class)
				.build();
	}
}

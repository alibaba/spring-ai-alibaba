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
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Tool for searching text patterns in files.
 */
public class GrepTool implements BiFunction<GrepTool.GrepRequest, ToolContext, String> {

	public static final String DESCRIPTION = """
			Search for a pattern in files.
			
			Usage:
			- The pattern parameter is the text to search for (literal string, not regex)
			- The path parameter filters which directory to search in
			- The glob parameter accepts a glob pattern to filter which files to search
			
			Examples:
			- Search all files: `grep(pattern="TODO")`
			- The search is case-sensitive by default.
			""";

	public GrepTool() {
	}

	@Override
	public String apply(GrepRequest request, ToolContext toolContext) {
		try {
			Path searchPath = request.path != null ?
					Paths.get(request.path) :
					Paths.get(System.getProperty("user.dir"));

			List<String> results = new ArrayList<>();
			PathMatcher globMatcher = request.glob != null ?
					FileSystems.getDefault().getPathMatcher("glob:" + request.glob) : null;

			Files.walk(searchPath)
					.filter(Files::isRegularFile)
					.filter(path -> globMatcher == null || globMatcher.matches(path.getFileName()))
					.forEach(path -> {
						try {
							List<String> lines = Files.readAllLines(path);
							for (int i = 0; i < lines.size(); i++) {
								if (lines.get(i).contains(request.pattern)) {
									String result = switch (request.outputMode) {
										case "files_with_matches" -> path.toString();
										case "content" -> path + ":" + (i + 1) + ": " + lines.get(i);
										case "count" -> path + ": matched";
										default -> path.toString();
									};
									results.add(result);
									if ("files_with_matches".equals(request.outputMode)) {
										break; // Only need file name once
									}
								}
							}
						}
						catch (IOException e) {
							// Skip files that can't be read
						}
					});

			if (results.isEmpty()) {
				return "No matches found for pattern: " + request.pattern;
			}

			return String.join("\n", results);
		}
		catch (IOException e) {
			return "Error searching files: " + e.getMessage();
		}
	}

	public static ToolCallback createGrepToolCallback(String description) {
		return FunctionToolCallback.builder("grep", new GrepTool())
				.description(description)
				.inputType(GrepRequest.class)
				.build();
	}

	/**
	 * Request structure for grep search.
	 */
	public static class GrepRequest {

		@JsonProperty(required = true)
		@JsonPropertyDescription("The text pattern to search for")
		public String pattern;

		@JsonProperty(value = "path")
		@JsonPropertyDescription("The directory path to search in (default: base path)")
		public String path;

		@JsonProperty(value = "glob")
		@JsonPropertyDescription("File pattern to filter which files to search (e.g., '*.java')")
		public String glob;

		@JsonProperty(value = "output_mode")
		@JsonPropertyDescription("Output format: 'files_with_matches', 'content', or 'count' (default: 'files_with_matches')")
		public String outputMode = "files_with_matches";

		public GrepRequest() {
		}

		public GrepRequest(String pattern, String path, String glob, String outputMode) {
			this.pattern = pattern;
			this.path = path;
			this.glob = glob;
			this.outputMode = outputMode;
		}
	}
}

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
package com.alibaba.cloud.ai.graph.agent.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * Glob search tool for fast file pattern matching.
 * Supports glob patterns like **&#47;*.js or src/**&#47;*.ts.
 * Returns matching file paths sorted by modification time.
 */
public class GlobSearchTool implements BiFunction<GlobSearchTool.Request, ToolContext, String> {

	private static final int DEFAULT_MAX_RESULTS = 2000;

	private static final Set<String> FORBIDDEN_BROAD_PATTERNS = Set.of(
			"**/*", "**/**", "*", "**"
	);

	private final Path rootPath;

	private final int maxResults;

	public GlobSearchTool(String rootPath) {
		this(rootPath, DEFAULT_MAX_RESULTS);
	}

	public GlobSearchTool(String rootPath, int maxResults) {
		this.rootPath = Paths.get(rootPath).toAbsolutePath().normalize();
		this.maxResults = maxResults > 0 ? maxResults : DEFAULT_MAX_RESULTS;
	}

	public record Request(
			@JsonProperty(required = true)
			@JsonPropertyDescription("The glob pattern to match files against")
			String pattern,

			@JsonProperty(defaultValue = "/")
			@JsonPropertyDescription("The directory to search in. If not specified, searches from root.")
			String path
	) {
		public Request {
			if (path == null || path.isEmpty()) {
				path = "/";
			}
		}
	}

	@Override
	public String apply(Request request, ToolContext toolContext) {
		try {
			String pattern = request.pattern() != null ? request.pattern().trim() : "";
			if (pattern.isEmpty()) {
				return "Error: Pattern is required";
			}
			if (FORBIDDEN_BROAD_PATTERNS.contains(pattern)) {
				return "Error: Pattern '" + pattern + "' matches all files and is not allowed (would return too many results). "
						+ "Use a more specific pattern, e.g. **/*.java, src/**/*.ts, or **/pom.xml.";
			}

			Path basePath = validateAndResolvePath(request.path());

			if (!Files.exists(basePath) || !Files.isDirectory(basePath)) {
				return "No files found";
			}

			// Use PathMatcher for glob pattern matching
			PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
			List<FileInfo> matchingFiles = new ArrayList<>(maxResults + 1);
			final boolean[] truncated = { false };

			Files.walkFileTree(basePath, new SimpleFileVisitor<>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					if (matchingFiles.size() >= maxResults) {
						truncated[0] = true;
						return FileVisitResult.TERMINATE;
					}
					Path relativePath = basePath.relativize(file);
					if (matcher.matches(relativePath)) {
						try {
							String virtualPath = "/" + rootPath.relativize(file).toString().replace("\\", "/");
							Instant modifiedTime = attrs.lastModifiedTime().toInstant();
							matchingFiles.add(new FileInfo(virtualPath, modifiedTime));
						} catch (Exception e) {
							// Skip files that can't be processed
						}
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) {
					// Skip files that can't be accessed
					return FileVisitResult.CONTINUE;
				}
			});

			if (matchingFiles.isEmpty()) {
				return "No files found";
			}

			// Sort by modification time (most recent first)
			matchingFiles.sort(Comparator.comparing(FileInfo::modifiedTime).reversed());

			String result = matchingFiles.stream()
					.map(FileInfo::path)
					.collect(Collectors.joining("\n"));
			if (truncated[0]) {
				result += "\n(Truncated at " + maxResults + " results; use a more specific pattern to reduce matches.)";
			}
			return result;

		} catch (PatternSyntaxException e) {
			return "Error: Invalid glob pattern syntax - " + e.getDescription();
		} catch (InvalidPathException e) {
			return "Error: Invalid path format - " + e.getReason();
		} catch (IOException e) {
			return "Error: I/O error - " + e.getMessage();
		} catch (Exception e) {
			return "Error: Unexpected error occurred - " + e.getMessage();
		}
	}

	private Path validateAndResolvePath(String path) throws IOException {
		// Normalize path
		if (!path.startsWith("/")) {
			path = "/" + path;
		}

		// Check for path traversal
		if (path.contains("..") || path.contains("~")) {
			throw new IOException("Path traversal not allowed");
		}

		// Convert virtual path to filesystem path
		String relative = path.substring(1); // Remove leading /
		Path fullPath = rootPath.resolve(relative).normalize();

		// Ensure path is within root
		if (!fullPath.startsWith(rootPath)) {
			throw new IOException("Path outside root directory: " + path);
		}

		return fullPath;
	}

	private record FileInfo(String path, Instant modifiedTime) {}

	public static Builder builder(String rootPath) {
		return new Builder(rootPath);
	}

	public static class Builder {

		private final String rootPath;

		private String name = "glob_search";

		private String description = "Fast file pattern matching tool that works with any codebase size. "
				+ "Supports glob patterns like **/*.js or src/**/*.ts. "
				+ "Returns matching file paths sorted by modification time (max " + DEFAULT_MAX_RESULTS + " results). "
				+ "Use a specific pattern; **/* is not allowed. Use this tool when you need to find files by name patterns.";

		private int maxResults = DEFAULT_MAX_RESULTS;

		public Builder(String rootPath) {
			this.rootPath = rootPath;
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder withDescription(String description) {
			this.description = description;
			return this;
		}

		/**
		 * Maximum number of file paths to return (default {@value #DEFAULT_MAX_RESULTS}).
		 * Prevents huge results that can break JSON serialization or memory.
		 */
		public Builder maxResults(int maxResults) {
			this.maxResults = maxResults > 0 ? maxResults : DEFAULT_MAX_RESULTS;
			return this;
		}

		public ToolCallback build() {
			return FunctionToolCallback.builder(name, new GlobSearchTool(rootPath, maxResults))
				.description(description)
				.inputType(Request.class)
				.build();
		}

	}

}

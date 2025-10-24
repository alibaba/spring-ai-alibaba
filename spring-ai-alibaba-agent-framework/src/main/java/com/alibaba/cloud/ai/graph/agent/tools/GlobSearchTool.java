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
import java.util.stream.Collectors;

/**
 * Glob search tool for fast file pattern matching.
 * Supports glob patterns like **&#47;*.js or src/**&#47;*.ts.
 * Returns matching file paths sorted by modification time.
 */
public class GlobSearchTool implements BiFunction<GlobSearchTool.Request, ToolContext, String> {

	private final Path rootPath;

	public GlobSearchTool(String rootPath) {
		this.rootPath = Paths.get(rootPath).toAbsolutePath().normalize();
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
			Path basePath = validateAndResolvePath(request.path());

			if (!Files.exists(basePath) || !Files.isDirectory(basePath)) {
				return "No files found";
			}

			// Use PathMatcher for glob pattern matching
			PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + request.pattern());
			List<FileInfo> matchingFiles = new ArrayList<>();

			Files.walkFileTree(basePath, new SimpleFileVisitor<>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
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

			return matchingFiles.stream()
					.map(FileInfo::path)
					.collect(Collectors.joining("\n"));

		} catch (Exception e) {
			return "No files found";
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
				+ "Returns matching file paths sorted by modification time. "
				+ "Use this tool when you need to find files by name patterns.";

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

		public ToolCallback build() {
			return FunctionToolCallback.builder(name, new GlobSearchTool(rootPath))
				.description(description)
				.inputType(Request.class)
				.build();
		}

	}

}

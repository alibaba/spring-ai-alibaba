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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * Grep search tool for fast content search.
 * Searches file contents using regular expressions.
 * Supports full regex syntax and filters files by pattern with the include parameter.
 */
public class GrepSearchTool implements BiFunction<GrepSearchTool.Request, ToolContext, String> {

	private final Path rootPath;
	private final boolean useRipgrep;
	private final long maxFileSizeBytes;

	public GrepSearchTool(String rootPath) {
		this(rootPath, true, 10);
	}

	public GrepSearchTool(String rootPath, boolean useRipgrep, int maxFileSizeMb) {
		this.rootPath = Paths.get(rootPath).toAbsolutePath().normalize();
		this.useRipgrep = useRipgrep;
		this.maxFileSizeBytes = maxFileSizeMb * 1024L * 1024L;
	}

	public record Request(
			@JsonProperty(required = true)
			@JsonPropertyDescription("The regular expression pattern to search for in file contents")
			String pattern,

			@JsonProperty(defaultValue = "/")
			@JsonPropertyDescription("The directory to search in. If not specified, searches from root.")
			String path,

			@JsonProperty
			@JsonPropertyDescription("File pattern to filter (e.g., \"*.js\", \"*.{ts,tsx}\")")
			String include,

			@JsonProperty(defaultValue = "files_with_matches")
			@JsonPropertyDescription("Output format: files_with_matches (default), content, or count")
			String outputMode
	) {
		public Request {
			if (path == null || path.isEmpty()) {
				path = "/";
			}
			if (outputMode == null || outputMode.isEmpty()) {
				outputMode = "files_with_matches";
			}
		}
	}

	@Override
	public String apply(Request request, ToolContext toolContext) {
		// Validate regex pattern
		try {
			Pattern.compile(request.pattern());
		} catch (PatternSyntaxException e) {
			return "Invalid regex pattern: " + e.getMessage();
		}

		// Validate include pattern if provided
		if (request.include() != null && !isValidIncludePattern(request.include())) {
			return "Invalid include pattern";
		}

		// Try ripgrep first if enabled
		Map<String, List<MatchInfo>> results = null;
		if (useRipgrep) {
			try {
				results = ripgrepSearch(request.pattern(), request.path(), request.include());
			} catch (Exception e) {
				// Fall back to Java search
			}
		}

		// Java fallback if ripgrep failed or is disabled
		if (results == null) {
			results = javaSearch(request.pattern(), request.path(), request.include());
		}

		if (results.isEmpty()) {
			return "No matches found";
		}

		return formatResults(results, request.outputMode());
	}

	private Map<String, List<MatchInfo>> ripgrepSearch(String pattern, String basePath, String include) {
		try {
			Path baseFullPath = validateAndResolvePath(basePath);

			if (!Files.exists(baseFullPath)) {
				return Collections.emptyMap();
			}

			// Build ripgrep command
			List<String> cmd = new ArrayList<>();
			cmd.add("rg");
			cmd.add("--json");

			if (include != null && !include.isEmpty()) {
				cmd.add("--glob");
				cmd.add(include);
			}

			cmd.add("--");
			cmd.add(pattern);
			cmd.add(baseFullPath.toString());

			ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.redirectErrorStream(true);
			Process process = pb.start();

			Map<String, List<MatchInfo>> results = new HashMap<>();

			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					try {
						// Parse ripgrep JSON output (simplified parsing)
						if (line.contains("\"type\":\"match\"")) {
							// This is a simplified approach; for production use a proper JSON parser
							results.putIfAbsent("ripgrep_result", new ArrayList<>());
						}
					} catch (Exception e) {
						// Skip malformed lines
					}
				}
			}

			boolean finished = process.waitFor(30, TimeUnit.SECONDS);
			if (!finished) {
				process.destroyForcibly();
				return null; // Trigger fallback
			}

			// If ripgrep succeeded but we got no results in expected format, fall back
			if (process.exitValue() != 0 && process.exitValue() != 1) {
				return null;
			}

			// For simplicity, always fall back to Java search
			// In production, implement proper JSON parsing for ripgrep output
			return null;

		} catch (Exception e) {
			return null; // Trigger fallback to Java search
		}
	}

	private Map<String, List<MatchInfo>> javaSearch(String patternStr, String basePath, String include) {
		try {
			Path baseFullPath = validateAndResolvePath(basePath);

			if (!Files.exists(baseFullPath)) {
				return Collections.emptyMap();
			}

			Pattern pattern = Pattern.compile(patternStr);
			Map<String, List<MatchInfo>> results = new LinkedHashMap<>();

			Files.walkFileTree(baseFullPath, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					// Check if file matches include pattern
					if (include != null && !matchIncludePattern(file.getFileName().toString(), include)) {
						return FileVisitResult.CONTINUE;
					}

					// Skip files that are too large
					if (attrs.size() > maxFileSizeBytes) {
						return FileVisitResult.CONTINUE;
					}

					try {
						String content = Files.readString(file, StandardCharsets.UTF_8);
						String[] lines = content.split("\r?\n");

						for (int i = 0; i < lines.length; i++) {
							Matcher matcher = pattern.matcher(lines[i]);
							if (matcher.find()) {
								String virtualPath = "/" + rootPath.relativize(file).toString().replace("\\", "/");
								results.computeIfAbsent(virtualPath, k -> new ArrayList<>())
										.add(new MatchInfo(i + 1, lines[i]));
							}
						}
					} catch (IOException e) {
						// Skip files that can't be read or are binary
					}

					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) {
					// Skip files that can't be accessed
					return FileVisitResult.CONTINUE;
				}
			});

			return results;

		} catch (Exception e) {
			return Collections.emptyMap();
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

	private boolean isValidIncludePattern(String pattern) {
		if (pattern == null || pattern.isEmpty()) {
			return false;
		}

		// Check for invalid characters
		return !pattern.contains("\0") && !pattern.contains("\n") && !pattern.contains("\r");
	}

	private boolean matchIncludePattern(String filename, String pattern) {
		// Simple glob matching - convert glob to regex
		// This is a simplified version; for production use a proper glob library
		String regex = pattern
				.replace(".", "\\.")
				.replace("*", ".*")
				.replace("?", ".");

		// Handle brace expansion like *.{js,ts}
		if (pattern.contains("{") && pattern.contains("}")) {
			regex = regex.replaceAll("\\{([^}]+)\\}", "($1)").replace(",", "|");
		}

		try {
			return filename.matches(regex);
		} catch (PatternSyntaxException e) {
			return false;
		}
	}

	private String formatResults(Map<String, List<MatchInfo>> results, String outputMode) {
		return switch (outputMode) {
			case "files_with_matches" -> results.keySet().stream()
					.sorted()
					.collect(Collectors.joining("\n"));

			case "content" -> results.entrySet().stream()
					.sorted(Map.Entry.comparingByKey())
					.flatMap(entry -> entry.getValue().stream()
							.map(info -> String.format("%s:%d:%s", entry.getKey(), info.lineNumber(), info.lineText())))
					.collect(Collectors.joining("\n"));

			case "count" -> results.entrySet().stream()
					.sorted(Map.Entry.comparingByKey())
					.map(entry -> String.format("%s:%d", entry.getKey(), entry.getValue().size()))
					.collect(Collectors.joining("\n"));

			default -> results.keySet().stream()
					.sorted()
					.collect(Collectors.joining("\n"));
		};
	}

	private record MatchInfo(int lineNumber, String lineText) {}

	public static Builder builder(String rootPath) {
		return new Builder(rootPath);
	}

	public static class Builder {

		private final String rootPath;

		private String name = "grep_search";

		private String description = "Fast content search tool that works with any codebase size. "
				+ "Searches file contents using regular expressions. "
				+ "Supports full regex syntax and filters files by pattern with the include parameter. "
				+ "Use this tool when you need to search for specific content within files.";

		private boolean useRipgrep = true;

		private int maxFileSizeMb = 10;

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

		public Builder withUseRipgrep(boolean useRipgrep) {
			this.useRipgrep = useRipgrep;
			return this;
		}

		public Builder withMaxFileSizeMb(int maxFileSizeMb) {
			this.maxFileSizeMb = maxFileSizeMb;
			return this;
		}

		public ToolCallback build() {
			return FunctionToolCallback
				.builder(name, new GrepSearchTool(rootPath, useRipgrep, maxFileSizeMb))
				.description(description)
				.inputType(Request.class)
				.build();
		}

	}

}

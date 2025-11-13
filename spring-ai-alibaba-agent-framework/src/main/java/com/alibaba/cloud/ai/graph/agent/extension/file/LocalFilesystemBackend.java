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
package com.alibaba.cloud.ai.graph.agent.extension.file;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Backend that reads and writes files directly from the filesystem.
 *
 * Files are accessed using their actual filesystem paths. Relative paths are
 * resolved relative to the current working directory. Content is read/written
 * as plain text, and metadata (timestamps) are derived from filesystem stats.
 *
 * Security and search upgrades:
 * - Secure path resolution with root containment when in virtual_mode (sandboxed to cwd)
 * - Prevent symlink-following on file I/O
 * - Ripgrep-powered grep with JSON parsing, plus Java fallback with regex
 *   and optional glob include filtering, while preserving virtual path behavior
 */
public class LocalFilesystemBackend implements FilesystemBackend {
	private static final String EMPTY_CONTENT_WARNING = "System reminder: File exists but has empty contents";
	private static final int MAX_LINE_LENGTH = 10000;
	private static final int LINE_NUMBER_WIDTH = 6;

	private final Path cwd;
	private final boolean virtualMode;
	private final long maxFileSizeBytes;

	/**
	 * Initialize filesystem backend.
	 *
	 * @param rootDir Optional root directory for file operations. If provided,
	 *                all file paths will be resolved relative to this directory.
	 *                If not provided, uses the current working directory.
	 * @param virtualMode When true, treat incoming paths as virtual absolute paths under
	 *                    cwd, disallow traversal (.., ~) and ensure resolved path stays within root.
	 * @param maxFileSizeMb Maximum file size in MB for reading operations
	 */
	public LocalFilesystemBackend(String rootDir, boolean virtualMode, int maxFileSizeMb) {
		this.cwd = rootDir != null ? Paths.get(rootDir).toAbsolutePath().normalize() : Paths.get("").toAbsolutePath();
		this.virtualMode = virtualMode;
		this.maxFileSizeBytes = maxFileSizeMb * 1024L * 1024L;
	}

	public LocalFilesystemBackend(String rootDir) {
		this(rootDir, false, 10);
	}

	/**
	 * Resolve a file path with security checks.
	 *
	 * When virtualMode=True, treat incoming paths as virtual absolute paths under
	 * cwd, disallow traversal (.., ~) and ensure resolved path stays within root.
	 * When virtualMode=False, preserve legacy behavior: absolute paths are allowed
	 * as-is; relative paths resolve under cwd.
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

	@Override
	public List<FileInfo> lsInfo(String path) {
		try {
			Path dirPath = resolvePath(path);
			if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
				return Collections.emptyList();
			}

			List<FileInfo> results = new ArrayList<>();
			String cwdStr = cwd.toString();
			if (!cwdStr.endsWith("/")) {
				cwdStr += "/";
			}

			try (Stream<Path> paths = Files.list(dirPath)) {
				for (Path childPath : paths.collect(Collectors.toList())) {
					try {
						boolean isFile = Files.isRegularFile(childPath, LinkOption.NOFOLLOW_LINKS);
						boolean isDir = Files.isDirectory(childPath, LinkOption.NOFOLLOW_LINKS);

						String absPath = childPath.toString();

						if (!virtualMode) {
							// Non-virtual mode: use absolute paths
							if (isFile) {
								try {
									BasicFileAttributes attrs = Files.readAttributes(childPath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
									results.add(new FileInfo(
										absPath,
										false,
										attrs.size(),
										formatTimestamp(attrs.lastModifiedTime().toInstant())
									));
								} catch (IOException e) {
									results.add(new FileInfo(absPath, false, null, null));
								}
							} else if (isDir) {
								try {
									BasicFileAttributes attrs = Files.readAttributes(childPath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
									results.add(new FileInfo(
										absPath + "/",
										true,
										0L,
										formatTimestamp(attrs.lastModifiedTime().toInstant())
									));
								} catch (IOException e) {
									results.add(new FileInfo(absPath + "/", true, null, null));
								}
							}
						} else {
							// Virtual mode: strip cwd prefix
							String relativePath;
							if (absPath.startsWith(cwdStr)) {
								relativePath = absPath.substring(cwdStr.length());
							} else if (absPath.startsWith(cwd.toString())) {
								relativePath = absPath.substring(cwd.toString().length());
								if (relativePath.startsWith("/")) {
									relativePath = relativePath.substring(1);
								}
							} else {
								relativePath = absPath;
							}

							String virtPath = "/" + relativePath;

							if (isFile) {
								try {
									BasicFileAttributes attrs = Files.readAttributes(childPath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
									results.add(new FileInfo(
										virtPath,
										false,
										attrs.size(),
										formatTimestamp(attrs.lastModifiedTime().toInstant())
									));
								} catch (IOException e) {
									results.add(new FileInfo(virtPath, false, null, null));
								}
							} else if (isDir) {
								try {
									BasicFileAttributes attrs = Files.readAttributes(childPath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
									results.add(new FileInfo(
										virtPath + "/",
										true,
										0L,
										formatTimestamp(attrs.lastModifiedTime().toInstant())
									));
								} catch (IOException e) {
									results.add(new FileInfo(virtPath + "/", true, null, null));
								}
							}
						}
					} catch (Exception ignored) {
						// Skip files that can't be accessed
					}
				}
			}

			// Keep deterministic order by path
			results.sort(Comparator.comparing(FileInfo::getPath));
			return results;
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	@Override
	public String read(String filePath, int offset, int limit) {
		try {
			Path resolvedPath = resolvePath(filePath);

			if (!Files.exists(resolvedPath) || !Files.isRegularFile(resolvedPath, LinkOption.NOFOLLOW_LINKS)) {
				return "Error: File '" + filePath + "' not found";
			}

			String content = new String(Files.readAllBytes(resolvedPath), StandardCharsets.UTF_8);

			String emptyMsg = checkEmptyContent(content);
			if (emptyMsg != null) {
				return emptyMsg;
			}

			String[] lines = content.split("\n", -1);
			// Remove trailing empty line if present
			if (lines.length > 0 && lines[lines.length - 1].isEmpty()) {
				lines = Arrays.copyOf(lines, lines.length - 1);
			}

			int startIdx = offset;
			int endIdx = Math.min(startIdx + limit, lines.length);

			if (startIdx >= lines.length) {
				return "Error: Line offset " + offset + " exceeds file length (" + lines.length + " lines)";
			}

			String[] selectedLines = Arrays.copyOfRange(lines, startIdx, endIdx);
			return formatContentWithLineNumbers(selectedLines, startIdx + 1);
		} catch (IllegalArgumentException e) {
			return "Error: " + e.getMessage();
		} catch (IOException e) {
			return "Error reading file '" + filePath + "': " + e.getMessage();
		}
	}

	@Override
	public WriteResult write(String filePath, String content) {
		try {
			Path resolvedPath = resolvePath(filePath);

			if (Files.exists(resolvedPath)) {
				return new WriteResult(null,
					"Cannot write to " + filePath + " because it already exists. Read and then make an edit, or write to a new path.",
					null);
			}

			// Create parent directories if needed
			Path parent = resolvedPath.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}

			Files.write(resolvedPath, content.getBytes(StandardCharsets.UTF_8),
				StandardOpenOption.CREATE_NEW,
				StandardOpenOption.WRITE);

			return new WriteResult(filePath, null, null);
		} catch (IllegalArgumentException e) {
			return new WriteResult(null, "Error: " + e.getMessage(), null);
		} catch (IOException e) {
			return new WriteResult(null, "Error writing file '" + filePath + "': " + e.getMessage(), null);
		}
	}

	@Override
	public EditResult edit(String filePath, String oldString, String newString, boolean replaceAll) {
		try {
			Path resolvedPath = resolvePath(filePath);

			if (!Files.exists(resolvedPath) || !Files.isRegularFile(resolvedPath, LinkOption.NOFOLLOW_LINKS)) {
				return new EditResult(null, 0, "Error: File '" + filePath + "' not found", null);
			}

			String content = new String(Files.readAllBytes(resolvedPath), StandardCharsets.UTF_8);

			// Perform string replacement
			int occurrences = countOccurrences(content, oldString);

			if (occurrences == 0) {
				return new EditResult(null, 0, "Error: String not found in file: '" + oldString + "'", null);
			}

			if (occurrences > 1 && !replaceAll) {
				return new EditResult(null, 0,
					"Error: String '" + oldString + "' appears " + occurrences +
					" times in file. Use replaceAll=true to replace all instances, or provide a more specific string with surrounding context.",
					null);
			}

			String newContent = content.replace(oldString, newString);

			Files.write(resolvedPath, newContent.getBytes(StandardCharsets.UTF_8),
				StandardOpenOption.TRUNCATE_EXISTING,
				StandardOpenOption.WRITE);

			return new EditResult(filePath, occurrences, null, null);
		} catch (IllegalArgumentException e) {
			return new EditResult(null, 0, "Error: " + e.getMessage(), null);
		} catch (IOException e) {
			return new EditResult(null, 0, "Error editing file '" + filePath + "': " + e.getMessage(), null);
		}
	}

	@Override
	public List<FileInfo> globInfo(String pattern, String path) {
		try {
			// Remove leading slash from pattern
			if (pattern.startsWith("/")) {
				pattern = pattern.substring(1);
			}

			Path searchPath = "/".equals(path) ? cwd : resolvePath(path);
			if (!Files.exists(searchPath) || !Files.isDirectory(searchPath)) {
				return Collections.emptyList();
			}

			List<FileInfo> results = new ArrayList<>();
			String globPattern = pattern;

			// Use PathMatcher for glob pattern matching
			PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + globPattern);
			final String cwdStr = cwd.toString() + (cwd.toString().endsWith("/") ? "" : "/");

			// Use recursive globbing to match files in subdirectories
			Files.walkFileTree(searchPath, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					try {
						if (!attrs.isRegularFile()) {
							return FileVisitResult.CONTINUE;
						}

						Path relativePath = searchPath.relativize(file);
						if (matcher.matches(relativePath)) {
							String absPath = file.toString();

							if (!virtualMode) {
								try {
									results.add(new FileInfo(
										absPath,
										false,
										attrs.size(),
										formatTimestamp(attrs.lastModifiedTime().toInstant())
									));
								} catch (Exception e) {
									results.add(new FileInfo(absPath, false, null, null));
								}
							} else {
								String relPath;
								if (absPath.startsWith(cwdStr)) {
									relPath = absPath.substring(cwdStr.length());
								} else if (absPath.startsWith(cwd.toString())) {
									relPath = absPath.substring(cwd.toString().length());
									if (relPath.startsWith("/")) {
										relPath = relPath.substring(1);
									}
								} else {
									relPath = absPath;
								}
								String virt = "/" + relPath;
								try {
									results.add(new FileInfo(
										virt,
										false,
										attrs.size(),
										formatTimestamp(attrs.lastModifiedTime().toInstant())
									));
								} catch (Exception e) {
									results.add(new FileInfo(virt, false, null, null));
								}
							}
						}
					} catch (Exception ignored) {
						// Skip files that can't be accessed
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) {
					return FileVisitResult.CONTINUE;
				}
			});

			results.sort(Comparator.comparing(FileInfo::getPath));
			return results;
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	@Override
	public Object grepRaw(String pattern, String path, String glob) {
		// Validate regex
		try {
			Pattern.compile(pattern);
		} catch (PatternSyntaxException e) {
			return "Invalid regex pattern: " + e.getMessage();
		}

		// Resolve base path
		Path baseFull;
		try {
			baseFull = resolvePath(path != null ? path : ".");
		} catch (IllegalArgumentException e) {
			return Collections.emptyList();
		}

		if (!Files.exists(baseFull)) {
			return Collections.emptyList();
		}

		// Try ripgrep first
		Map<String, List<LineMatch>> results = ripgrepSearch(pattern, baseFull, glob);
		if (results == null) {
			results = javaSearch(pattern, baseFull, glob);
		}

		List<GrepMatch> matches = new ArrayList<>();
		for (Map.Entry<String, List<LineMatch>> entry : results.entrySet()) {
			for (LineMatch lm : entry.getValue()) {
				matches.add(new GrepMatch(entry.getKey(), lm.lineNum, lm.lineText));
			}
		}
		return matches;
	}

	// Helper methods

	private Map<String, List<LineMatch>> ripgrepSearch(String pattern, Path baseFull, String includeGlob) {
		List<String> cmd = new ArrayList<>();
		cmd.add("rg");
		cmd.add("--json");
		if (includeGlob != null) {
			cmd.add("--glob");
			cmd.add(includeGlob);
		}
		cmd.add("--");
		cmd.add(pattern);
		cmd.add(baseFull.toString());

		try {
			ProcessBuilder pb = new ProcessBuilder(cmd);
			Process process = pb.start();

			Map<String, List<LineMatch>> results = new HashMap<>();
			ObjectMapper mapper = new ObjectMapper();

			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					try {
						JsonNode data = mapper.readTree(line);
						if (!"match".equals(data.path("type").asText())) {
							continue;
						}
						JsonNode pdata = data.path("data");
						String ftext = pdata.path("path").path("text").asText(null);
						if (ftext == null) {
							continue;
						}
						Path p = Paths.get(ftext);
						String virt;
						if (virtualMode) {
							try {
								Path resolved = p.toAbsolutePath().normalize();
								Path relative = cwd.relativize(resolved);
								virt = "/" + relative.toString().replace("\\", "/");
							} catch (Exception e) {
								continue;
							}
						} else {
							virt = p.toString();
						}
						Integer ln = pdata.path("line_number").asInt(0);
						if (ln == 0) {
							continue;
						}
						String lt = pdata.path("lines").path("text").asText("");
						if (lt.endsWith("\n")) {
							lt = lt.substring(0, lt.length() - 1);
						}
						results.computeIfAbsent(virt, k -> new ArrayList<>())
							.add(new LineMatch(ln, lt));
					} catch (Exception ignored) {
						// Skip malformed JSON lines
					}
				}
			}

			process.waitFor();
			return results;
		} catch (Exception e) {
			// Ripgrep not available or failed, return null to fallback to Java search
			return null;
		}
	}

	private Map<String, List<LineMatch>> javaSearch(String pattern, Path baseFull, String includeGlob) {
		Pattern regex;
		try {
			regex = Pattern.compile(pattern);
		} catch (PatternSyntaxException e) {
			return Collections.emptyMap();
		}

		Map<String, List<LineMatch>> results = new HashMap<>();
		Path root = Files.isDirectory(baseFull) ? baseFull : baseFull.getParent();

		try {
			Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path fp, BasicFileAttributes attrs) {
					if (!attrs.isRegularFile()) {
						return FileVisitResult.CONTINUE;
					}
					if (includeGlob != null) {
						PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + includeGlob);
						if (!matcher.matches(fp.getFileName())) {
							return FileVisitResult.CONTINUE;
						}
					}
					try {
						if (attrs.size() > maxFileSizeBytes) {
							return FileVisitResult.CONTINUE;
						}
						String content = new String(Files.readAllBytes(fp), StandardCharsets.UTF_8);
						String[] lines = content.split("\n", -1);
						for (int lineNum = 1; lineNum <= lines.length; lineNum++) {
							String line = lines[lineNum - 1];
							Matcher m = regex.matcher(line);
							if (m.find()) {
								String virtPath;
								if (virtualMode) {
									try {
										Path resolved = fp.toAbsolutePath().normalize();
										Path relative = cwd.relativize(resolved);
										virtPath = "/" + relative.toString().replace("\\", "/");
									} catch (Exception e) {
										continue;
									}
								} else {
									virtPath = fp.toString();
								}
								results.computeIfAbsent(virtPath, k -> new ArrayList<>())
									.add(new LineMatch(lineNum, line));
							}
						}
					} catch (Exception ignored) {
						// Skip files that can't be read
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) {
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException ignored) {
			// Return what we have so far
		}

		return results;
	}

	private String formatContentWithLineNumbers(String[] lines, int startLine) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			int lineNum = i + startLine;

			if (line.length() <= MAX_LINE_LENGTH) {
				result.append(String.format("%" + LINE_NUMBER_WIDTH + "d\t%s\n", lineNum, line));
			} else {
				// Split long line into chunks with continuation markers
				int numChunks = (line.length() + MAX_LINE_LENGTH - 1) / MAX_LINE_LENGTH;
				for (int chunkIdx = 0; chunkIdx < numChunks; chunkIdx++) {
					int start = chunkIdx * MAX_LINE_LENGTH;
					int end = Math.min(start + MAX_LINE_LENGTH, line.length());
					String chunk = line.substring(start, end);
					if (chunkIdx == 0) {
						result.append(String.format("%" + LINE_NUMBER_WIDTH + "d\t%s\n", lineNum, chunk));
					} else {
						String continuationMarker = lineNum + "." + chunkIdx;
						result.append(String.format("%" + LINE_NUMBER_WIDTH + "s\t%s\n", continuationMarker, chunk));
					}
				}
			}
		}
		// Remove trailing newline
		if (result.length() > 0 && result.charAt(result.length() - 1) == '\n') {
			result.setLength(result.length() - 1);
		}
		return result.toString();
	}

	private String checkEmptyContent(String content) {
		if (content == null || content.trim().isEmpty()) {
			return EMPTY_CONTENT_WARNING;
		}
		return null;
	}

	private int countOccurrences(String content, String search) {
		int count = 0;
		int index = 0;
		while ((index = content.indexOf(search, index)) != -1) {
			count++;
			index += search.length();
		}
		return count;
	}

	private String formatTimestamp(Instant instant) {
		return instant.atOffset(ZoneOffset.UTC)
			.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}

	private static class LineMatch {
		final int lineNum;
		final String lineText;

		LineMatch(int lineNum, String lineText) {
			this.lineNum = lineNum;
			this.lineText = lineText;
		}
	}
}


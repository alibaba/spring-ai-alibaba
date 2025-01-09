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
package com.alibaba.cloud.ai.reader.gptrepo;

import org.jetbrains.annotations.NotNull;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

/**
 * GptRepoDocumentReader - Used to read Git repository content and convert it to Document
 * format
 *
 * @author brianxiadong
 */
public class GptRepoDocumentReader implements DocumentReader {

	private static final String DEFAULT_ENCODING = "UTF-8";

	private static final String IGNORE_FILE = ".gptignore";

	private static final String SECTION_SEPARATOR = "----";

	private static final String END_MARKER = "--END--";

	// Default leading text constant
	private static final String DEFAULT_CONCATENATED_PREAMBLE = "The following text is a Git repository with code. "
			+ "The structure of the text are sections that begin with ----, "
			+ "followed by a single line containing the file path and file "
			+ "name, followed by a variable amount of lines containing the "
			+ "file contents. The text representing the Git repository ends "
			+ "when the symbols --END-- are encountered. Any further text beyond "
			+ "--END-- are meant to be interpreted as instructions using the "
			+ "aforementioned Git repository as context.\n";

	private static final String DEFAULT_SINGLE_FILE_PREAMBLE = "The following text is a file in a Git repository. "
			+ "The structure of the text are sections that begin with ----, "
			+ "followed by a single line containing the file path and file "
			+ "name, followed by a variable amount of lines containing the "
			+ "file contents. The text representing the file ends "
			+ "when the symbols --END-- are encountered. Any further text beyond "
			+ "--END-- are meant to be interpreted as instructions using the " + "aforementioned file as context.\n";

	// Whether to merge all file contents into one document
	private final boolean concatenate;

	// Repository path
	private final Path repoPath;

	// File extension filter
	private final List<String> extensions;

	// File encoding
	private final String encoding;

	// Custom leading text
	private final String preambleStr;

	/**
	 * Constructor
	 * @param repoPath Repository path
	 * @param concatenate Whether to merge all file contents
	 * @param extensions List of file extensions to process
	 * @param encoding File encoding
	 * @param preambleStr Custom leading text, use default if null
	 */
	public GptRepoDocumentReader(String repoPath, boolean concatenate, List<String> extensions, String encoding,
			String preambleStr) {
		this.repoPath = Paths.get(repoPath);
		this.concatenate = concatenate;
		this.extensions = extensions;
		this.encoding = encoding != null ? encoding : DEFAULT_ENCODING;
		this.preambleStr = preambleStr;
	}

	/**
	 * Constructor - Using default parameters
	 * @param repoPath Repository path
	 */
	public GptRepoDocumentReader(String repoPath) {
		this(repoPath, false, null, DEFAULT_ENCODING, null);
	}

	/**
	 * Constructor - Without custom leading text
	 * @param repoPath Repository path
	 * @param concatenate Whether to merge all file contents
	 * @param extensions List of file extensions to process
	 * @param encoding File encoding
	 */
	public GptRepoDocumentReader(String repoPath, boolean concatenate, List<String> extensions, String encoding) {
		this(repoPath, concatenate, extensions, encoding, null);
	}

	@Override
	public List<Document> get() {
		try {
			// Read .gptignore file
			List<String> ignorePatterns = readIgnorePatterns();

			// Process repository files
			List<String> processedTexts = processRepository(ignorePatterns);

			// Convert to Document list
			return processedTexts.stream().map(text -> {
				String finalText = getPreambleText() + text + "\n" + END_MARKER + "\n";
				Map<String, Object> metadata = new HashMap<>();
				metadata.put("source", repoPath.toString());

				// Extract file path from text content
				String filePath = extractFilePath(text);
				if (filePath != null) {
					Path path = Paths.get(filePath);
					metadata.put("file_path", filePath);
					metadata.put("file_name", path.getFileName().toString());
					metadata.put("directory", path.getParent() != null ? path.getParent().toString() : "");
				}

				return new Document(finalText, metadata);
			}).collect(Collectors.toList());
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to process repository: " + repoPath, e);
		}
	}

	/**
	 * Extract file path from formatted text content
	 * @param text Formatted text content
	 * @return File path, or null if not found
	 */
	private String extractFilePath(String text) {
		String[] lines = text.split("\n");
		if (lines.length >= 2 && lines[0].equals(SECTION_SEPARATOR)) {
			return lines[1].trim();
		}
		return null;
	}

	/**
	 * Get document leading text
	 */
	private String getPreambleText() {
		if (preambleStr != null) {
			return preambleStr;
		}
		return concatenate ? DEFAULT_CONCATENATED_PREAMBLE : DEFAULT_SINGLE_FILE_PREAMBLE;
	}

	/**
	 * Read .gptignore file content
	 */
	private List<String> readIgnorePatterns() throws IOException {
		Path ignorePath = repoPath.resolve(IGNORE_FILE);
		if (Files.exists(ignorePath)) {
			return Files.readAllLines(ignorePath, Charset.forName(encoding));
		}
		return Collections.emptyList();
	}

	/**
	 * Process repository files
	 */
	private List<String> processRepository(List<String> ignorePatterns) throws IOException {
		List<String> results = new ArrayList<>();
		StringBuilder concatenatedContent = new StringBuilder();

		Files.walkFileTree(repoPath, new SimpleFileVisitor<>() {
			@NotNull
			@Override
			public FileVisitResult visitFile(Path file, @NotNull BasicFileAttributes attrs) throws IOException {
				String relativePath = repoPath.relativize(file).toString();

				// Check if file should be ignored
				if (shouldIgnore(relativePath, ignorePatterns)) {
					return FileVisitResult.CONTINUE;
				}

				// Check file extension
				if (extensions != null && !extensions.isEmpty()) {
					String ext = com.google.common.io.Files.getFileExtension(file.toString());
					if (!extensions.contains(ext)) {
						return FileVisitResult.CONTINUE;
					}
				}

				// Read file content
				String content = Files.readString(file, Charset.forName(encoding));
				String formattedContent = formatFileContent(relativePath, content);

				if (concatenate) {
					concatenatedContent.append(formattedContent);
				}
				else {
					results.add(formattedContent);
				}

				return FileVisitResult.CONTINUE;
			}
		});

		if (concatenate && !concatenatedContent.isEmpty()) {
			results.add(concatenatedContent.toString());
		}

		return results;
	}

	/**
	 * Format file content
	 */
	private String formatFileContent(String relativePath, String content) {
		return String.format("%s\n%s\n%s\n", SECTION_SEPARATOR, relativePath, content);
	}

	/**
	 * Check if file should be ignored
	 */
	private boolean shouldIgnore(String path, List<String> ignorePatterns) {
		return ignorePatterns.stream().anyMatch(pattern -> {
			PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
			return matcher.matches(Paths.get(path));
		});
	}

}

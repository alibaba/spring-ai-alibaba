/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.example.deepresearch.util.export;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * File operation utility class providing basic file read/write functionality
 *
 * @author sixiyida
 * @since 2025/6/20
 */
public final class FileOperationUtil {

	private static final Logger logger = LoggerFactory.getLogger(FileOperationUtil.class);

	private static final Pattern INVALID_FILENAME_CHARS = Pattern.compile("[^a-zA-Z0-9\\-_\\s]");

	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	private static final String DEFAULT_FILENAME = "report";

	/**
	 * Reads content from a file
	 * @param filePath File path
	 * @return File content
	 * @throws RuntimeException If file reading fails
	 */
	public static String readFromFile(String filePath) {
		try {
			return Files.readString(Paths.get(filePath));
		}
		catch (IOException e) {
			logger.error("Failed to read file: {}", filePath, e);
			throw new RuntimeException("Failed to read file: " + filePath, e);
		}
	}

	/**
	 * Saves content to a file
	 * @param content Content
	 * @param filePath File path
	 * @return Saved file path
	 * @throws RuntimeException If file saving fails
	 */
	public static String saveContentToFile(String content, String filePath) {
		try {
			// Ensure parent directory exists
			Path path = Paths.get(filePath);
			createParentDirectories(path);

			// Write content
			Files.writeString(path, content);
			logger.info("Content saved to file: {}", filePath);
			return filePath;
		}
		catch (IOException e) {
			logger.error("Failed to save content to file: {}", filePath, e);
			throw new RuntimeException("Failed to save content to file: " + filePath, e);
		}
	}

	/**
	 * Creates parent directories (if they do not exist)
	 * @param path File path
	 * @throws IOException If directory creation fails
	 */
	private static void createParentDirectories(Path path) throws IOException {
		Path parent = path.getParent();
		if (parent != null && !Files.exists(parent)) {
			Files.createDirectories(parent);
		}
	}

	/**
	 * Creates a directory (if it does not exist)
	 * @param directoryPath Directory path
	 * @throws IllegalArgumentException If the directory path is null
	 * @throws RuntimeException If directory creation fails
	 */
	public static void createDirectoryIfNotExists(String directoryPath) {
		if (directoryPath == null) {
			logger.error("Directory path is null");
			throw new IllegalArgumentException("Directory path cannot be null");
		}

		try {
			Path path = Paths.get(directoryPath);
			if (!Files.exists(path)) {
				Files.createDirectories(path);
				logger.info("Created directory: {}", directoryPath);
			}
		}
		catch (IOException e) {
			logger.error("Failed to create directory: {}", directoryPath, e);
			throw new RuntimeException("Failed to create directory: " + directoryPath, e);
		}
	}

	/**
	 * Generates a filename using the thread ID as the filename
	 * @param threadId Thread ID
	 * @param extension File extension
	 * @return Generated filename
	 */
	public static String generateFilename(String threadId, String extension) {
		// Clean thread ID by removing characters unsuitable for filenames
		String cleanThreadId = INVALID_FILENAME_CHARS.matcher(threadId).replaceAll("").trim();

		cleanThreadId = WHITESPACE.matcher(cleanThreadId).replaceAll("_");

		// If the thread ID is empty after cleaning, use "report" as the default name
		if (cleanThreadId.isEmpty()) {
			cleanThreadId = DEFAULT_FILENAME;
		}

		return cleanThreadId + "." + extension;
	}

	/**
	 * Generates a filename using a custom title as the filename
	 * @param title Title
	 * @param extension File extension
	 * @return Generated filename
	 */
	public static String generateFilenameFromTitle(String title, String extension) {
		if (title == null || title.trim().isEmpty()) {
			return DEFAULT_FILENAME + "." + extension;
		}

		String cleanTitle = title.replaceAll("[\\\\/:*?\"<>|]", "").trim();

		cleanTitle = cleanTitle.replaceAll("\\s+", "_");

		if (cleanTitle.isEmpty()) {
			cleanTitle = DEFAULT_FILENAME;
		}

		if (cleanTitle.length() > 50) {
			cleanTitle = cleanTitle.substring(0, 50);
		}

		return cleanTitle + "." + extension;
	}

	/**
	 * Retrieves the ResponseEntity for file download
	 * @param filePath File path
	 * @param mediaType Media type
	 * @return ResponseEntity containing the file
	 * @throws IOException If the file does not exist or is unreadable
	 * @throws RuntimeException If the file cannot be read
	 */
	public static ResponseEntity<Resource> getFileDownload(String filePath, MediaType mediaType) throws IOException {
		Path path = Paths.get(filePath);
		Resource resource = new UrlResource(path.toUri());

		if (resource.exists() && resource.isReadable()) {
			return ResponseEntity.ok()
				.contentType(mediaType)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						"attachment; filename=\"" + path.getFileName().toString() + "\"")
				.body(resource);
		}
		else {
			throw new RuntimeException("Could not read file: " + filePath);
		}
	}

	/**
	 * Retrieves the ResponseEntity for file download using a custom display filename
	 * @param filePath File path
	 * @param mediaType Media type
	 * @param displayFilename Display filename
	 * @return ResponseEntity containing the file
	 * @throws IOException If the file does not exist or is unreadable
	 * @throws RuntimeException If the file cannot be read
	 */
	public static ResponseEntity<Resource> getFileDownload(String filePath, MediaType mediaType, String displayFilename)
			throws IOException {
		Path path = Paths.get(filePath);
		Resource resource = new UrlResource(path.toUri());

		if (resource.exists() && resource.isReadable()) {
			// Get file extension
			String originalFilename = path.getFileName().toString();
			String extension = originalFilename.contains(".")
					? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";

			// Ensure display filename has correct extension
			if (displayFilename == null || displayFilename.trim().isEmpty()) {
				displayFilename = originalFilename;
			}
			else if (!displayFilename.contains(".") && !extension.isEmpty()) {
				displayFilename = displayFilename + extension;
			}

			String encodedFilename;
			try {
				encodedFilename = java.net.URLEncoder.encode(displayFilename, StandardCharsets.UTF_8);
				encodedFilename = encodedFilename.replace("+", "%20");
			}
			catch (Exception e) {
				logger.warn("Failed to encode filename: {}, using fallback", displayFilename, e);
				encodedFilename = displayFilename.replaceAll("[^\\p{ASCII}]", "_");
			}

			return ResponseEntity.ok()
				.contentType(mediaType)
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
				.body(resource);
		}
		else {
			throw new RuntimeException("Could not read file: " + filePath);
		}
	}

	/**
	 * Checks if a file exists
	 * @param filePath File path
	 * @return Whether the file exists
	 */
	public static boolean fileExists(String filePath) {
		return filePath != null && Files.exists(Paths.get(filePath));
	}

}

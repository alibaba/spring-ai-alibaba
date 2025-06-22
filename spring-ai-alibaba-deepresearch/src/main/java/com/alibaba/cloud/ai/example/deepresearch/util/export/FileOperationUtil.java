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
 * 文件操作工具类，提供基础的文件读写功能
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
	 * 从文件读取内容
	 * @param filePath 文件路径
	 * @return 文件内容
	 * @throws RuntimeException 如果文件读取失败
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
	 * 将内容保存到文件
	 * @param content 内容
	 * @param filePath 文件路径
	 * @return 保存的文件路径
	 * @throws RuntimeException 如果文件保存失败
	 */
	public static String saveContentToFile(String content, String filePath) {
		try {
			// 确保父目录存在
			Path path = Paths.get(filePath);
			createParentDirectories(path);

			// 写入内容
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
	 * 创建父目录（如果不存在）
	 * @param path 文件路径
	 * @throws IOException 如果目录创建失败
	 */
	private static void createParentDirectories(Path path) throws IOException {
		Path parent = path.getParent();
		if (parent != null && !Files.exists(parent)) {
			Files.createDirectories(parent);
		}
	}

	/**
	 * 创建目录（如果不存在）
	 * @param directoryPath 目录路径
	 * @throws IllegalArgumentException 如果目录路径为null
	 * @throws RuntimeException 如果目录创建失败
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
	 * 生成文件名，使用线程ID作为文件名
	 * @param threadId 线程ID
	 * @param extension 扩展名
	 * @return 生成的文件名
	 */
	public static String generateFilename(String threadId, String extension) {
		// 清理线程ID，移除不适合作为文件名的字符
		String cleanThreadId = INVALID_FILENAME_CHARS.matcher(threadId).replaceAll("").trim();

		cleanThreadId = WHITESPACE.matcher(cleanThreadId).replaceAll("_");

		// 如果清理后线程ID为空，使用"report"作为默认名称
		if (cleanThreadId.isEmpty()) {
			cleanThreadId = DEFAULT_FILENAME;
		}

		return cleanThreadId + "." + extension;
	}

	/**
	 * 生成文件名，使用自定义标题作为文件名
	 * @param title 标题
	 * @param extension 扩展名
	 * @return 生成的文件名
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
	 * 获取文件下载的ResponseEntity
	 * @param filePath 文件路径
	 * @param mediaType 媒体类型
	 * @return 包含文件的ResponseEntity
	 * @throws IOException 如果文件不存在或不可读
	 * @throws RuntimeException 如果文件无法读取
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
	 * 获取文件下载的ResponseEntity，使用自定义的显示文件名
	 * @param filePath 文件路径
	 * @param mediaType 媒体类型
	 * @param displayFilename 显示的文件名
	 * @return 包含文件的ResponseEntity
	 * @throws IOException 如果文件不存在或不可读
	 * @throws RuntimeException 如果文件无法读取
	 */
	public static ResponseEntity<Resource> getFileDownload(String filePath, MediaType mediaType, String displayFilename)
			throws IOException {
		Path path = Paths.get(filePath);
		Resource resource = new UrlResource(path.toUri());

		if (resource.exists() && resource.isReadable()) {
			// 获取文件扩展名
			String originalFilename = path.getFileName().toString();
			String extension = originalFilename.contains(".")
					? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";

			// 确保显示文件名有正确的扩展名
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
	 * 检查文件是否存在
	 * @param filePath 文件路径
	 * @return 文件是否存在
	 */
	public static boolean fileExists(String filePath) {
		return filePath != null && Files.exists(Paths.get(filePath));
	}

}

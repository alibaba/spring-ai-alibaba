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

package com.alibaba.cloud.ai.example.deepresearch.service;

import com.alibaba.cloud.ai.example.deepresearch.util.export.FileOperationUtil;
import com.alibaba.cloud.ai.example.deepresearch.util.export.FormatConversionUtil;
import com.openhtmltopdf.slf4j.Slf4jLogger;
import com.openhtmltopdf.util.XRLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Report Export Service
 * Responsible for converting reports to different formats and saving them locally
 * Supports Markdown and PDF formats, and provides file download functionality
 *
 * @author sixiyida
 * @since 2025/6/20
 */
@Service
public class ExportService {

	private static final Logger logger = LoggerFactory.getLogger(ExportService.class);

	private static final Set<String> SUPPORTED_FORMATS = Set.of("markdown", "md", "pdf");

	private static final Pattern TITLE_PATTERN = Pattern.compile("^#\\s+(.+)$", Pattern.MULTILINE);

	@Autowired
	private ReportService reportService;

	private final String basePath;

	/**
	 * Constructor, initializes the export path
	 * @param basePath Base export path
	 */
	public ExportService(String basePath) {
		this.basePath = basePath;
		XRLog.setLoggerImpl(new Slf4jLogger());
		FileOperationUtil.createDirectoryIfNotExists(this.basePath);
		logger.info("ExportService initialized with base path: {}", this.basePath);
	}

	/**
	 * Saves content to a file
	 * @param content Content to save
	 * @param filePath File path
	 * @return Saved file path
	 */
	public String saveContentToFile(String content, String filePath) {
		return FileOperationUtil.saveContentToFile(content, filePath);
	}

	/**
	 * Saves report content in Markdown format
	 * @param threadId Thread ID
	 * @return Saved file path, returns null if the report does not exist
	 */
	public String saveAsMarkdown(String threadId) {
		String content = reportService.getReport(threadId);
		if (content == null) {
			logger.warn("No report content found for thread: {}", threadId);
			return null;
		}

		String filePath = getReportFilePath(threadId, "md");
		return saveContentToFile(content, filePath);
	}

	/**
	 * Converts report content to PDF and saves it
	 * @param threadId Thread ID
	 * @return Saved PDF file path, returns null if the report does not exist
	 */
	public String saveAsPdf(String threadId) {
		String content = reportService.getReport(threadId);
		if (content == null) {
			logger.warn("No report content found for thread: {}", threadId);
			return null;
		}

		String pdfFilePath = getReportFilePath(threadId, "pdf");
		FormatConversionUtil.convertMarkdownToPdfFile(content, pdfFilePath);
		return pdfFilePath;
	}

	/**
	 * Retrieves the file path of the report in the specified format
	 * @param threadId Thread ID
	 * @param format File format
	 * @return File path, returns null if the format is not supported
	 */
	public String getReportFilePath(String threadId, String format) {
		if (!isSupportedFormat(format)) {
			return null;
		}

		String extension = "markdown".equals(format) || "md".equals(format) ? "md" : format;
		String filename = FileOperationUtil.generateFilename(threadId, extension);
		return basePath + File.separator + filename;
	}

	/**
	 * Checks if the report file in the specified format exists
	 * @param threadId Thread ID
	 * @param format File format
	 * @return Whether the file exists
	 */
	public boolean reportFileExists(String threadId, String format) {
		String filePath = getReportFilePath(threadId, format);
		return FileOperationUtil.fileExists(filePath);
	}

	/**
	 * Extracts the title from Markdown content
	 * @param content Markdown content
	 * @return Extracted title, returns null if not found
	 */
	public String extractTitleFromContent(String content) {
		if (content == null || content.isEmpty()) {
			return null;
		}

		Matcher matcher = TITLE_PATTERN.matcher(content);
		if (matcher.find()) {
			return matcher.group(1).trim();
		}

		return null;
	}

	/**
	 * Directly downloads the report file
	 * @param threadId Thread ID
	 * @param format File format
	 * @return File download response
	 * @throws IOException If the file does not exist or is unreadable
	 * @throws RuntimeException If the report does not exist or the format is not supported
	 */
	public ResponseEntity<Resource> downloadReport(String threadId, String format) throws IOException {
		// Check if the exported report corresponding to the thread ID exists
		if (!existsReportByThreadId(threadId)) {
			throw new RuntimeException("Report not found for thread: " + threadId);
		}

		// Get the file path
		String filePath = getReportFilePath(threadId, format);
		if (filePath == null) {
			throw new RuntimeException("Unsupported format: " + format);
		}

		// Get report content to extract title
		String content = reportService.getReport(threadId);
		String title = extractTitleFromContent(content);

		logger.debug("Extracted title: {}", title);

		// If the file does not exist, generate it
		if (!FileOperationUtil.fileExists(filePath)) {
			logger.info("File does not exist, generating: {}", filePath);

			// Export file according to the requested format
			filePath = "markdown".equals(format) || "md".equals(format) ? saveAsMarkdown(threadId)
					: saveAsPdf(threadId);

			// Uniformly use 'markdown' as the key
			if ("md".equals(format)) {
				format = "markdown";
			}

			if (filePath == null) {
				throw new RuntimeException("Failed to export report to format: " + format);
			}
		}
		else {
			logger.info("File already exists, using: {}", filePath);
		}

		// Prepare file download display name
		String extension = "markdown".equals(format) || "md".equals(format) ? "md" : format;
		String displayFilename = null;

		if (title != null && !title.isEmpty()) {
			displayFilename = FileOperationUtil.generateFilenameFromTitle(title, extension);
			logger.info("Using title for download filename: {}", displayFilename);
		}

		// Return file download response
		MediaType mediaType = getMediaTypeForFormat(format);
		return displayFilename != null ? FileOperationUtil.getFileDownload(filePath, mediaType, displayFilename)
				: FileOperationUtil.getFileDownload(filePath, mediaType);
	}

	/**
	 * Retrieves the media type corresponding to the format
	 * @param format File format
	 * @return Corresponding media type
	 */
	private MediaType getMediaTypeForFormat(String format) {
		return switch (format) {
			case "markdown", "md" -> MediaType.TEXT_MARKDOWN;
			case "pdf" -> MediaType.APPLICATION_PDF;
			default -> MediaType.APPLICATION_OCTET_STREAM;
		};
	}

	/**
	 * Retrieves the ResponseEntity for file download
	 * @param filePath File path
	 * @param mediaType Media type
	 * @return ResponseEntity containing the file
	 * @throws IOException If the file does not exist or is unreadable
	 */
	public ResponseEntity<Resource> getFileDownload(String filePath, MediaType mediaType) throws IOException {
		return FileOperationUtil.getFileDownload(filePath, mediaType);
	}

	/**
	 * Checks if the report corresponding to the thread ID exists
	 * @param threadId Thread ID
	 * @return Whether the report exists
	 */
	public boolean existsReportByThreadId(String threadId) {
		return reportService.existsReport(threadId);
	}

	/**
	 * Checks if the format is supported
	 * @param format Export format
	 * @return Whether the format is supported
	 */
	public boolean isSupportedFormat(String format) {
		return SUPPORTED_FORMATS.contains(format);
	}

}

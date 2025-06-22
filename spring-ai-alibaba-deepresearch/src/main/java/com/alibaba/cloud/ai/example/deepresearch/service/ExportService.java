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
 * 报告导出服务，负责将报告转换为不同格式并保存到本地 支持Markdown和PDF格式，并提供文件下载功能
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
	 * 构造函数，初始化导出路径
	 * @param basePath 导出基础路径
	 */
	public ExportService(String basePath) {
		this.basePath = basePath;
		XRLog.setLoggerImpl(new Slf4jLogger());
		FileOperationUtil.createDirectoryIfNotExists(this.basePath);
		logger.info("ExportService initialized with base path: {}", this.basePath);
	}

	/**
	 * 将内容保存到文件
	 * @param content 内容
	 * @param filePath 文件路径
	 * @return 保存的文件路径
	 */
	public String saveContentToFile(String content, String filePath) {
		return FileOperationUtil.saveContentToFile(content, filePath);
	}

	/**
	 * 将报告内容保存为Markdown格式
	 * @param threadId 线程ID
	 * @return 保存的文件路径，如果报告不存在则返回null
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
	 * 将报告内容转换为PDF并保存
	 * @param threadId 线程ID
	 * @return 保存的PDF文件路径，如果报告不存在则返回null
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
	 * 获取指定格式报告的文件路径
	 * @param threadId 线程ID
	 * @param format 文件格式
	 * @return 文件路径，如果格式不支持则返回null
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
	 * 检查指定格式的报告文件是否存在
	 * @param threadId 线程ID
	 * @param format 文件格式
	 * @return 文件是否存在
	 */
	public boolean reportFileExists(String threadId, String format) {
		String filePath = getReportFilePath(threadId, format);
		return FileOperationUtil.fileExists(filePath);
	}

	/**
	 * 从Markdown内容中提取标题
	 * @param content Markdown内容
	 * @return 提取的标题，如果未找到则返回null
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
	 * 直接下载报告文件
	 * @param threadId 线程ID
	 * @param format 文件格式
	 * @return 文件下载响应
	 * @throws IOException 如果文件不存在或不可读
	 * @throws RuntimeException 如果报告不存在或格式不支持
	 */
	public ResponseEntity<Resource> downloadReport(String threadId, String format) throws IOException {
		// 检查线程ID对应导出的报告是否存在
		if (!existsReportByThreadId(threadId)) {
			throw new RuntimeException("Report not found for thread: " + threadId);
		}

		// 获取文件路径
		String filePath = getReportFilePath(threadId, format);
		if (filePath == null) {
			throw new RuntimeException("Unsupported format: " + format);
		}

		// 获取报告内容以提取标题
		String content = reportService.getReport(threadId);
		String title = extractTitleFromContent(content);

		logger.debug("Extracted title: {}", title);

		// 如果文件不存在，则生成文件
		if (!FileOperationUtil.fileExists(filePath)) {
			logger.info("File does not exist, generating: {}", filePath);

			// 根据请求的格式导出文件
			filePath = "markdown".equals(format) || "md".equals(format) ? saveAsMarkdown(threadId)
					: saveAsPdf(threadId);

			// 统一使用markdown作为键
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

		// 准备文件下载显示名称
		String extension = "markdown".equals(format) || "md".equals(format) ? "md" : format;
		String displayFilename = null;

		if (title != null && !title.isEmpty()) {
			displayFilename = FileOperationUtil.generateFilenameFromTitle(title, extension);
			logger.info("Using title for download filename: {}", displayFilename);
		}

		// 返回文件下载响应
		MediaType mediaType = getMediaTypeForFormat(format);
		return displayFilename != null ? FileOperationUtil.getFileDownload(filePath, mediaType, displayFilename)
				: FileOperationUtil.getFileDownload(filePath, mediaType);
	}

	/**
	 * 获取格式对应的媒体类型
	 * @param format 文件格式
	 * @return 对应的媒体类型
	 */
	private MediaType getMediaTypeForFormat(String format) {
		return switch (format) {
			case "markdown", "md" -> MediaType.TEXT_MARKDOWN;
			case "pdf" -> MediaType.APPLICATION_PDF;
			default -> MediaType.APPLICATION_OCTET_STREAM;
		};
	}

	/**
	 * 获取文件下载的ResponseEntity
	 * @param filePath 文件路径
	 * @param mediaType 媒体类型
	 * @return 包含文件的ResponseEntity
	 * @throws IOException 如果文件不存在或不可读
	 */
	public ResponseEntity<Resource> getFileDownload(String filePath, MediaType mediaType) throws IOException {
		return FileOperationUtil.getFileDownload(filePath, mediaType);
	}

	/**
	 * 检查线程ID对应的报告是否存在
	 * @param threadId 线程ID
	 * @return 是否存在
	 */
	public boolean existsReportByThreadId(String threadId) {
		return reportService.existsReport(threadId);
	}

	/**
	 * 检查格式是否支持
	 * @param format 导出格式
	 * @return 是否支持
	 */
	public boolean isSupportedFormat(String format) {
		return SUPPORTED_FORMATS.contains(format);
	}

}

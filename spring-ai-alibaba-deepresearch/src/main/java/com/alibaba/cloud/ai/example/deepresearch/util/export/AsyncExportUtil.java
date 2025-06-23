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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * 异步导出工具类，提供异步PDF转换功能
 *
 * @author sixiyida
 * @since 2025/6/20
 */
public class AsyncExportUtil {

	private static final Logger logger = LoggerFactory.getLogger(AsyncExportUtil.class);

	/**
	 * 异步将内容转换为PDF
	 * @param content 报告内容
	 * @param title 报告标题
	 * @param basePath 基础路径
	 * @return CompletableFuture包含PDF文件路径
	 */
	public static CompletableFuture<String> saveAsPdfAsync(String content, String title, String basePath,
			ThreadPoolTaskExecutor executor) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				String filename = FileOperationUtil.generateFilename(title, "pdf");
				String pdfFilePath = basePath + File.separator + filename;

				// 直接将Markdown转换为PDF
				byte[] pdfBytes = FormatConversionUtil.convertMarkdownToPdfBytes(content);

				// 保存到文件
				try (java.io.FileOutputStream fos = new java.io.FileOutputStream(pdfFilePath)) {
					fos.write(pdfBytes);
				}

				logger.info("Async PDF conversion completed: {}", pdfFilePath);
				return pdfFilePath;
			}
			catch (Exception e) {
				logger.error("Failed to convert to PDF asynchronously", e);
				throw new RuntimeException("Failed to convert to PDF asynchronously", e);
			}
		}, executor);
	}

}

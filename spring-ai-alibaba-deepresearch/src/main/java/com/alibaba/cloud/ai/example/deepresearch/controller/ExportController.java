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

package com.alibaba.cloud.ai.example.deepresearch.controller;

import com.alibaba.cloud.ai.example.deepresearch.model.req.ExportRequest;
import com.alibaba.cloud.ai.example.deepresearch.model.response.ExportResponse;
import com.alibaba.cloud.ai.example.deepresearch.service.ExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 报告导出控制器，提供报告导出和下载的API 支持用户选择下载格式，一键导出并下载
 *
 * @author sixiyida
 * @since 2025/6/20
 */
@RestController
@RequestMapping("/api/reports")
public class ExportController {

	private static final Logger logger = LoggerFactory.getLogger(ExportController.class);

	@Autowired
	private ExportService exportService;

	/**
	 * 导出报告，返回导出文件的元数据信息
	 * @param request 包含线程ID和导出格式的请求
	 * @return 导出文件的元数据信息
	 */
	@PostMapping("/export")
	public ResponseEntity<ExportResponse> exportReport(@RequestBody ExportRequest request) {
		if (request == null) {
			return ResponseEntity.badRequest().body(ExportResponse.error("Report request cannot be null"));
		}

		try {
			String threadId = request.threadId();
			String format = request.format().toLowerCase();

			logger.info("Exporting report for threadId: {}, format: {}", threadId, format);

			// 检查线程ID对应的报告是否存在
			if (!exportService.existsReportByThreadId(threadId)) {
				return ResponseEntity.badRequest()
					.body(ExportResponse.error("Report not found for thread: " + threadId));
			}

			// 检查请求的格式是否支持
			if (!exportService.isSupportedFormat(format)) {
				return ResponseEntity.badRequest()
					.body(ExportResponse
						.error("Unsupported format: " + format + ", only markdown and pdf are supported"));
			}

			// 实际保存文件
			String filePath = exportReport(threadId, format);
			if (filePath == null) {
				return ResponseEntity.badRequest()
					.body(ExportResponse.error("Failed to export report to format: " + format));
			}

			// 构建下载URL
			String downloadUrl = "/api/reports/download/" + threadId + "?format=" + format;

			// 构建成功响应
			ExportResponse response = ExportResponse.success(threadId, format, filePath, downloadUrl);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Failed to export report", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ExportResponse.error(e.getMessage()));
		}
	}

	/**
	 * 下载指定格式的报告文件
	 * @param threadId 线程ID
	 * @param format 文件格式
	 * @return 文件下载响应
	 */
	@GetMapping("/download/{threadId}")
	public ResponseEntity<?> downloadReport(@PathVariable String threadId,
			@RequestParam(required = false, defaultValue = "markdown") String format) {

		format = format.toLowerCase();

		try {
			// 检查格式是否支持
			if (!exportService.isSupportedFormat(format)) {
				return ResponseEntity.badRequest()
					.body(ExportResponse
						.error("Unsupported format: " + format + ", only markdown and pdf are supported"));
			}

			// 统一使用markdown作为键
			if ("md".equals(format)) {
				format = "markdown";
			}

			return exportService.downloadReport(threadId, format);
		}
		catch (Exception e) {
			logger.error("Failed to download report", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ExportResponse.error(e.getMessage()));
		}
	}

	/**
	 * 根据线程ID和格式导出报告
	 * @param threadId 线程ID
	 * @param format 导出格式
	 * @return 导出文件的路径
	 */
	private String exportReport(String threadId, String format) {
		if ("markdown".equals(format) || "md".equals(format)) {
			String filePath = exportService.saveAsMarkdown(threadId);
			return filePath;
		}
		else if ("pdf".equals(format)) {
			return exportService.saveAsPdf(threadId);
		}
		return null;
	}

}

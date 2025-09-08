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

import com.alibaba.cloud.ai.example.deepresearch.model.dto.ExportData;
import com.alibaba.cloud.ai.example.deepresearch.model.req.ExportRequest;
import com.alibaba.cloud.ai.example.deepresearch.model.response.ReportResponse;
import com.alibaba.cloud.ai.example.deepresearch.service.ExportService;
import com.alibaba.cloud.ai.example.deepresearch.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Optional;

/**
 * Controller of Report query
 *
 * @author huangzhen
 * @since 2025/6/18
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

	private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

	@Autowired
	private ExportService exportService;

	@Autowired
	private ReportService reportService;

	@Autowired
	private ChatClient interactionAgent;

	/**
	 * Retrieves report by thread ID
	 * @param threadId 线程ID
	 * @return report content
	 */
	@GetMapping("/{threadId}")
	public ResponseEntity<ReportResponse<String>> getReport(@PathVariable String threadId) {
		try {
			logger.info("Querying report for thread ID: {}", threadId);
			return Optional.ofNullable(reportService.getReport(threadId))
				.map(report -> ResponseEntity
					.ok(ReportResponse.success(threadId, "Report retrieved successfully", report)))
				.orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(ReportResponse.notfound(threadId, "Report not found")));
		}
		catch (Exception e) {
			logger.error("Failed to get report for thread ID: {}", threadId, e);
			return ResponseEntity.internalServerError()
				.body(ReportResponse.error(threadId, "Failed to get report: " + e.getMessage()));
		}
	}

	/**
	 * Checks if report exists
	 * @param threadId 线程ID
	 * @return Existence status
	 */
	@GetMapping("/{threadId}/exists")
	public ResponseEntity<ReportResponse<Boolean>> existsReport(@PathVariable String threadId) {
		try {
			logger.info("Checking if report exists for thread ID: {}", threadId);
			boolean exists = reportService.existsReport(threadId);

			return ResponseEntity.ok(ReportResponse.success(threadId, "whether the report exists", exists));
		}
		catch (Exception e) {
			logger.error("Failed to check if report exists for thread ID: {}", threadId, e);
			return ResponseEntity.internalServerError()
				.body(ReportResponse.error(threadId, "Check failed: " + e.getMessage()));
		}
	}

	/**
	 * Deletes report
	 * @param threadId 线程ID
	 * @return Deletion result
	 */
	@DeleteMapping("/{threadId}")
	public ResponseEntity<ReportResponse> deleteReport(@PathVariable String threadId) {
		try {
			logger.info("Deleting report for thread ID: {}", threadId);

			if (!reportService.existsReport(threadId)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(ReportResponse.notfound(threadId, "Report not found"));
			}

			reportService.deleteReport(threadId);
			return ResponseEntity.ok(ReportResponse.success(threadId, "Report deleted successfully", null));
		}
		catch (Exception e) {
			logger.error("Failed to delete report for thread ID: {}", threadId, e);
			return ResponseEntity.internalServerError()
				.body(ReportResponse.error(threadId, "Failed to delete report: " + e.getMessage()));
		}
	}

	/**
	 * Exports report and returns metadata information of the exported file
	 * @param request Request containing thread ID and export format
	 * @return Metadata information of the exported file
	 */
	@PostMapping("/export")
	public ResponseEntity<ReportResponse> exportReport(@RequestBody ExportRequest request) {
		if (request == null) {
			return ResponseEntity.badRequest().body(ReportResponse.error(null, "Report request cannot be null"));
		}

		try {
			String threadId = request.threadId();
			String format = request.format().toLowerCase();

			logger.info("Exporting report for threadId: {}, format: {}", threadId, format);

			// Check if the report corresponding to the thread ID exists
			if (!exportService.existsReportByThreadId(threadId)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(ReportResponse.error(threadId, "Report not found for thread: " + threadId));
			}

			// Check if the requested format is supported
			if (!exportService.isSupportedFormat(format)) {
				return ResponseEntity.badRequest()
					.body(ReportResponse.error(threadId,
							"Unsupported format: " + format + ", only markdown and pdf are supported"));
			}

			// Save file
			String filePath = exportReport(threadId, format);
			if (filePath == null) {
				return ResponseEntity.badRequest()
					.body(ReportResponse.error(threadId, "Failed to export report to format: " + format));
			}

			// Construct download URL
			String downloadUrl = "/api/reports/download/" + threadId + "?format=" + format;

			// Construct success response
			ReportResponse<ExportData> response = ReportResponse.success(threadId, "Report exported successfully",
					ExportData.success(format, filePath, downloadUrl));
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Failed to export report", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ReportResponse.error(null, e.getMessage()));
		}
	}

	/**
	 * Downloads report file in specified format
	 * @param threadId 线程ID
	 * @param format 文件格式
	 * @return File download response
	 */
	@GetMapping("/download/{threadId}")
	public ResponseEntity<?> downloadReport(@PathVariable String threadId,
			@RequestParam(required = false, defaultValue = "markdown") String format) {

		format = format.toLowerCase();

		try {
			// Check if the format is supported
			if (!exportService.isSupportedFormat(format)) {
				return ResponseEntity.badRequest()
					.body(ReportResponse.error(threadId,
							"Unsupported format: " + format + ", only markdown and pdf are supported"));
			}

			// Consistently use Markdown as the key format
			if ("md".equals(format)) {
				format = "markdown";
			}

			return exportService.downloadReport(threadId, format);
		}
		catch (Exception e) {
			logger.error("Failed to download report", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ReportResponse.error(threadId, e.getMessage()));
		}
	}

	/**
	 * Construct interactive HTML report
	 * @param threadId
	 * @return
	 */
	@GetMapping(value = "/build-html", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ChatResponse> buildInteractiveHtml(String threadId) {
		if (threadId == null || threadId.isEmpty()) {
			logger.error("threadId is null or empty");
			return Flux.error(new IllegalArgumentException("threadId cannot be null or empty"));
		}
		String reportInfo = reportService.getReport(threadId);
		if (reportInfo == null) {
			logger.error("Report with threadId {} not found", threadId);
			return Flux.error(new IllegalArgumentException("Report not found"));
		}
		else {
			logger.debug("Found report for threadId: {} ,Report info: {}", threadId, reportInfo);
		}
		logger.info("Building interactive HTML report");
		// Use ChatClient to construct HTML report
		return interactionAgent.prompt(reportInfo).stream().chatResponse();
	}

	/**
	 * Exports report by thread ID and format
	 * @param threadId 线程ID
	 * @param format 导出格式
	 * @return Path to the exported file
	 */
	private String exportReport(String threadId, String format) {
		if ("markdown".equals(format) || "md".equals(format)) {
			return exportService.saveAsMarkdown(threadId);
		}
		else if ("pdf".equals(format)) {
			return exportService.saveAsPdf(threadId);
		}
		return null;
	}

}

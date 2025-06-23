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

import com.alibaba.cloud.ai.example.deepresearch.model.response.BaseResponse;
import com.alibaba.cloud.ai.example.deepresearch.model.response.ExistsResponse;
import com.alibaba.cloud.ai.example.deepresearch.model.response.ReportResponse;
import com.alibaba.cloud.ai.example.deepresearch.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 报告查询控制器
 *
 * @author huangzhen
 * @since 2025/6/18
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

	private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

	private final ReportService reportService;

	public ReportController(ReportService reportService) {
		this.reportService = reportService;
	}

	/**
	 * 根据线程ID获取报告
	 * @param threadId 线程ID
	 * @return 报告内容
	 */
	@GetMapping("/{threadId}")
	public ResponseEntity<ReportResponse> getReport(@PathVariable String threadId) {
		try {
			logger.info("Querying report for thread ID: {}", threadId);
			String report = reportService.getReport(threadId);

			if (report != null) {
				return ResponseEntity.ok(ReportResponse.success(threadId, report));
			}
			else {
				return ResponseEntity.notFound().build();
			}
		}
		catch (Exception e) {
			logger.error("Failed to get report for thread ID: {}", threadId, e);
			return ResponseEntity.internalServerError()
				.body(ReportResponse.error(threadId, "Failed to get report: " + e.getMessage()));
		}
	}

	/**
	 * 检查报告是否存在
	 * @param threadId 线程ID
	 * @return 是否存在
	 */
	@GetMapping("/{threadId}/exists")
	public ResponseEntity<ExistsResponse> existsReport(@PathVariable String threadId) {
		try {
			logger.info("Checking if report exists for thread ID: {}", threadId);
			boolean exists = reportService.existsReport(threadId);

			return ResponseEntity.ok(ExistsResponse.success(threadId, exists));
		}
		catch (Exception e) {
			logger.error("Failed to check if report exists for thread ID: {}", threadId, e);
			return ResponseEntity.internalServerError()
				.body(ExistsResponse.error(threadId, "Check failed: " + e.getMessage()));
		}
	}

	/**
	 * 删除报告
	 * @param threadId 线程ID
	 * @return 删除结果
	 */
	@DeleteMapping("/{threadId}")
	public ResponseEntity<BaseResponse> deleteReport(@PathVariable String threadId) {
		try {
			logger.info("Deleting report for thread ID: {}", threadId);

			if (!reportService.existsReport(threadId)) {
				return ResponseEntity.notFound().build();
			}

			reportService.deleteReport(threadId);
			return ResponseEntity.ok(BaseResponse.success(threadId, "Report deleted successfully"));
		}
		catch (Exception e) {
			logger.error("Failed to delete report for thread ID: {}", threadId, e);
			return ResponseEntity.internalServerError()
				.body(BaseResponse.error(threadId, "Failed to delete report: " + e.getMessage()));
		}
	}

}

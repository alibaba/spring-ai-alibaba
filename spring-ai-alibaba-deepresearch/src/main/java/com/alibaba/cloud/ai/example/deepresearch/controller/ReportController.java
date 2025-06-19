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

import com.alibaba.cloud.ai.example.deepresearch.service.ReportRedisService;
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

	private final ReportRedisService reportRedisService;

	public ReportController(ReportRedisService reportRedisService) {
		this.reportRedisService = reportRedisService;
	}

	/**
	 * 根据线程ID获取报告
	 * @param threadId 线程ID
	 * @return 报告内容
	 */
	@GetMapping("/{threadId}")
	public ResponseEntity<ReportResponse> getReport(@PathVariable String threadId) {
		try {
			logger.info("查询报告，线程ID: {}", threadId);
			String report = reportRedisService.getReport(threadId);

			if (report != null) {
				ReportResponse response = new ReportResponse();
				response.setThreadId(threadId);
				response.setReport(report);
				response.setStatus("success");
				response.setMessage("报告获取成功");
				return ResponseEntity.ok(response);
			}
			else {
				ReportResponse response = new ReportResponse();
				response.setThreadId(threadId);
				response.setStatus("not_found");
				response.setMessage("未找到指定线程ID的报告");
				return ResponseEntity.notFound().build();
			}
		}
		catch (Exception e) {
			logger.error("获取报告失败，线程ID: {}", threadId, e);
			ReportResponse response = new ReportResponse();
			response.setThreadId(threadId);
			response.setStatus("error");
			response.setMessage("获取报告失败: " + e.getMessage());
			return ResponseEntity.internalServerError().body(response);
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
			logger.info("检查报告是否存在，线程ID: {}", threadId);
			boolean exists = reportRedisService.existsReport(threadId);

			ExistsResponse response = new ExistsResponse();
			response.setThreadId(threadId);
			response.setExists(exists);
			response.setStatus("success");
			response.setMessage(exists ? "报告存在" : "报告不存在");

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("检查报告是否存在失败，线程ID: {}", threadId, e);
			ExistsResponse response = new ExistsResponse();
			response.setThreadId(threadId);
			response.setExists(false);
			response.setStatus("error");
			response.setMessage("检查失败: " + e.getMessage());
			return ResponseEntity.internalServerError().body(response);
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
			logger.info("删除报告，线程ID: {}", threadId);

			if (!reportRedisService.existsReport(threadId)) {
				BaseResponse response = new BaseResponse();
				response.setThreadId(threadId);
				response.setStatus("not_found");
				response.setMessage("未找到指定线程ID的报告");
				return ResponseEntity.notFound().build();
			}

			reportRedisService.deleteReport(threadId);

			BaseResponse response = new BaseResponse();
			response.setThreadId(threadId);
			response.setStatus("success");
			response.setMessage("报告删除成功");

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("删除报告失败，线程ID: {}", threadId, e);
			BaseResponse response = new BaseResponse();
			response.setThreadId(threadId);
			response.setStatus("error");
			response.setMessage("删除报告失败: " + e.getMessage());
			return ResponseEntity.internalServerError().body(response);
		}
	}

	/**
	 * 基础响应类
	 */
	public static class BaseResponse {

		private String threadId;

		private String status;

		private String message;

		public String getThreadId() {
			return threadId;
		}

		public void setThreadId(String threadId) {
			this.threadId = threadId;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

	}

	/**
	 * 报告响应类
	 */
	public static class ReportResponse extends BaseResponse {

		private String report;

		public String getReport() {
			return report;
		}

		public void setReport(String report) {
			this.report = report;
		}

	}

	/**
	 * 存在性检查响应类
	 */
	public static class ExistsResponse extends BaseResponse {

		private boolean exists;

		public boolean isExists() {
			return exists;
		}

		public void setExists(boolean exists) {
			this.exists = exists;
		}

	}

}

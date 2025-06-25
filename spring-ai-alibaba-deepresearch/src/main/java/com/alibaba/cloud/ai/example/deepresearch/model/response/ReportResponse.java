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

package com.alibaba.cloud.ai.example.deepresearch.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 报告响应类
 *
 * @author huangzhen
 * @since 2025/6/20
 */
public record ReportResponse(
		/**
		 * 线程ID，用于标识当前对话的唯一性
		 */
		@JsonProperty("thread_id") String threadId,

		/**
		 * 状态
		 */
		@JsonProperty("status") String status,

		/**
		 * 消息
		 */
		@JsonProperty("message") String message,

		/**
		 * 报告内容
		 */
		@JsonProperty("report") String report) {

	public static ReportResponse success(String threadId, String report) {
		return new ReportResponse(threadId, "success", "Report retrieved successfully", report);
	}

	public static ReportResponse error(String threadId, String message) {
		return new ReportResponse(threadId, "error", message, null);
	}
}

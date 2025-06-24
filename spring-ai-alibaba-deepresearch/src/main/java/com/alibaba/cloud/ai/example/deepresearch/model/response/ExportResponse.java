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
 * 报告导出响应
 *
 * @author sixiyida
 * @since 2025/6/20
 */
public record ExportResponse(
		/**
		 * 操作是否成功
		 */
		@JsonProperty("success") boolean success,

		/**
		 * 线程ID，用于标识当前对话的唯一性
		 */
		@JsonProperty("thread_id") String threadId,

		/**
		 * 导出格式
		 */
		@JsonProperty("format") String format,

		/**
		 * 导出文件路径
		 */
		@JsonProperty("file_path") String filePath,

		/**
		 * 下载URL
		 */
		@JsonProperty("download_url") String downloadUrl,

		/**
		 * 错误信息，仅当success为false时有值
		 */
		@JsonProperty("error") String error) {

	public static ExportResponse success(String threadId, String format, String filePath, String downloadUrl) {
		return new ExportResponse(true, threadId, format, filePath, downloadUrl, null);
	}

	public static ExportResponse error(String errorMessage) {
		return new ExportResponse(false, null, null, null, null, errorMessage);
	}
}

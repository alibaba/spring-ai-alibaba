package com.alibaba.cloud.ai.example.deepresearch.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ExportData(
		/**
		 * 操作是否成功
		 */
		@JsonProperty("success") boolean success,

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
	public static ExportData success(String format, String filePath, String downloadUrl) {
		return new ExportData(true, format, filePath, downloadUrl, null);
	}

	public static ExportData error(String errorMessage) {
		return new ExportData(false, null, null, null, errorMessage);
	}
}

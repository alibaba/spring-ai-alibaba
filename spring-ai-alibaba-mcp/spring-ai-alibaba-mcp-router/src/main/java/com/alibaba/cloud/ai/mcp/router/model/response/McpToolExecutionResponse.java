/*
 * Copyright 2025-2026 the original author or authors.
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

package com.alibaba.cloud.ai.mcp.router.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * MCP 工具执行响应结果 用于封装 MCP 工具执行的结果，包含执行状态、结果数据和执行元信息
 *
 * @author spring-ai-alibaba
 * @since 2025.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpToolExecutionResponse {

	/**
	 * 执行是否成功
	 */
	@JsonProperty("success")
	private boolean success;

	/**
	 * 错误消息（当 success 为 false 时）
	 */
	@JsonProperty("error_message")
	private String errorMessage;

	/**
	 * 服务名称
	 */
	@JsonProperty("service_name")
	private String serviceName;

	/**
	 * 工具名称
	 */
	@JsonProperty("tool_name")
	private String toolName;

	/**
	 * 执行参数
	 */
	@JsonProperty("execution_parameters")
	private Map<String, Object> executionParameters;

	/**
	 * 执行结果
	 */
	@JsonProperty("result")
	private McpToolExecutionResult result;

	/**
	 * 执行元信息
	 */
	@JsonProperty("execution_meta")
	private McpExecutionMeta executionMeta;

	public McpToolExecutionResponse() {
	}

	/**
	 * 创建成功响应
	 */
	public static McpToolExecutionResponse success(String serviceName, String toolName, Map<String, Object> parameters,
			McpToolExecutionResult result, McpExecutionMeta meta) {
		McpToolExecutionResponse response = new McpToolExecutionResponse();
		response.success = true;
		response.serviceName = serviceName;
		response.toolName = toolName;
		response.executionParameters = parameters;
		response.result = result;
		response.executionMeta = meta;
		return response;
	}

	/**
	 * 创建失败响应
	 */
	public static McpToolExecutionResponse error(String serviceName, String toolName, Map<String, Object> parameters,
			String errorMessage) {
		McpToolExecutionResponse response = new McpToolExecutionResponse();
		response.success = false;
		response.serviceName = serviceName;
		response.toolName = toolName;
		response.executionParameters = parameters;
		response.errorMessage = errorMessage;
		return response;
	}

	/**
	 * MCP 工具执行结果
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class McpToolExecutionResult {

		/**
		 * 结果数据类型（text, json, binary 等）
		 */
		@JsonProperty("content_type")
		private String contentType;

		/**
		 * 结果内容
		 */
		@JsonProperty("content")
		private Object content;

		/**
		 * 原始响应（调试用）
		 */
		@JsonProperty("raw_response")
		private String rawResponse;

		/**
		 * 结果大小（字节）
		 */
		@JsonProperty("content_size")
		private Long contentSize;

		public McpToolExecutionResult() {
		}

		public McpToolExecutionResult(String contentType, Object content, String rawResponse) {
			this.contentType = contentType;
			this.content = content;
			this.rawResponse = rawResponse;
			this.contentSize = rawResponse != null ? (long) rawResponse.length() : null;
		}

		/**
		 * 创建文本结果
		 */
		public static McpToolExecutionResult text(String content) {
			return new McpToolExecutionResult("text", content, content);
		}

		/**
		 * 创建JSON结果
		 */
		public static McpToolExecutionResult json(Object content, String rawResponse) {
			return new McpToolExecutionResult("json", content, rawResponse);
		}

		// Getters and Setters
		public String getContentType() {
			return contentType;
		}

		public void setContentType(String contentType) {
			this.contentType = contentType;
		}

		public Object getContent() {
			return content;
		}

		public void setContent(Object content) {
			this.content = content;
		}

		public String getRawResponse() {
			return rawResponse;
		}

		public void setRawResponse(String rawResponse) {
			this.rawResponse = rawResponse;
			this.contentSize = rawResponse != null ? (long) rawResponse.length() : null;
		}

		public Long getContentSize() {
			return contentSize;
		}

		public void setContentSize(Long contentSize) {
			this.contentSize = contentSize;
		}

	}

	/**
	 * MCP 执行元信息
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class McpExecutionMeta {

		/**
		 * 执行开始时间戳
		 */
		@JsonProperty("execution_start")
		private Long executionStart;

		/**
		 * 执行结束时间戳
		 */
		@JsonProperty("execution_end")
		private Long executionEnd;

		/**
		 * 执行耗时（毫秒）
		 */
		@JsonProperty("execution_duration_ms")
		private Long executionDurationMs;

		/**
		 * 使用的协议
		 */
		@JsonProperty("protocol_used")
		private String protocolUsed;

		/**
		 * 连接URL
		 */
		@JsonProperty("connection_url")
		private String connectionUrl;

		/**
		 * 重试次数
		 */
		@JsonProperty("retry_count")
		private Integer retryCount;

		public McpExecutionMeta() {
		}

		public McpExecutionMeta(Long executionStart, Long executionEnd, String protocolUsed, String connectionUrl) {
			this.executionStart = executionStart;
			this.executionEnd = executionEnd;
			this.executionDurationMs = executionEnd != null && executionStart != null ? executionEnd - executionStart
					: null;
			this.protocolUsed = protocolUsed;
			this.connectionUrl = connectionUrl;
			this.retryCount = 0;
		}

		/**
		 * 创建执行开始的元信息
		 */
		public static McpExecutionMeta start(String protocolUsed, String connectionUrl) {
			return new McpExecutionMeta(System.currentTimeMillis(), null, protocolUsed, connectionUrl);
		}

		/**
		 * 完成执行
		 */
		public void complete() {
			this.executionEnd = System.currentTimeMillis();
			if (this.executionStart != null) {
				this.executionDurationMs = this.executionEnd - this.executionStart;
			}
		}

		// Getters and Setters
		public Long getExecutionStart() {
			return executionStart;
		}

		public void setExecutionStart(Long executionStart) {
			this.executionStart = executionStart;
		}

		public Long getExecutionEnd() {
			return executionEnd;
		}

		public void setExecutionEnd(Long executionEnd) {
			this.executionEnd = executionEnd;
		}

		public Long getExecutionDurationMs() {
			return executionDurationMs;
		}

		public void setExecutionDurationMs(Long executionDurationMs) {
			this.executionDurationMs = executionDurationMs;
		}

		public String getProtocolUsed() {
			return protocolUsed;
		}

		public void setProtocolUsed(String protocolUsed) {
			this.protocolUsed = protocolUsed;
		}

		public String getConnectionUrl() {
			return connectionUrl;
		}

		public void setConnectionUrl(String connectionUrl) {
			this.connectionUrl = connectionUrl;
		}

		public Integer getRetryCount() {
			return retryCount;
		}

		public void setRetryCount(Integer retryCount) {
			this.retryCount = retryCount;
		}

	}

	// Getters and Setters
	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getToolName() {
		return toolName;
	}

	public void setToolName(String toolName) {
		this.toolName = toolName;
	}

	public Map<String, Object> getExecutionParameters() {
		return executionParameters;
	}

	public void setExecutionParameters(Map<String, Object> executionParameters) {
		this.executionParameters = executionParameters;
	}

	public McpToolExecutionResult getResult() {
		return result;
	}

	public void setResult(McpToolExecutionResult result) {
		this.result = result;
	}

	public McpExecutionMeta getExecutionMeta() {
		return executionMeta;
	}

	public void setExecutionMeta(McpExecutionMeta executionMeta) {
		this.executionMeta = executionMeta;
	}

	@Override
	public String toString() {
		return "McpToolExecutionResponse{" + "success=" + success + ", errorMessage='" + errorMessage + '\''
				+ ", serviceName='" + serviceName + '\'' + ", toolName='" + toolName + '\'' + ", executionParameters="
				+ executionParameters + ", result=" + result + ", executionMeta=" + executionMeta + '}';
	}

}

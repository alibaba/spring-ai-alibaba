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

import java.util.List;
import java.util.Map;

/**
 * MCP 调试响应结果 用于封装 MCP 服务调试信息，包含连接状态、诊断信息和问题排查建议
 *
 * @author spring-ai-alibaba
 * @since 2025.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpDebugResponse {

	/**
	 * 调试是否成功
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
	 * 调试时间戳
	 */
	@JsonProperty("debug_timestamp")
	private Long debugTimestamp;

	/**
	 * 服务状态检查
	 */
	@JsonProperty("service_status")
	private McpServiceStatus serviceStatus;

	/**
	 * 连接诊断信息
	 */
	@JsonProperty("connection_diagnosis")
	private McpConnectionDiagnosis connectionDiagnosis;

	/**
	 * 问题排查建议
	 */
	@JsonProperty("troubleshooting_suggestions")
	private List<String> troubleshootingSuggestions;

	/**
	 * 系统环境信息
	 */
	@JsonProperty("system_info")
	private Map<String, Object> systemInfo;

	public McpDebugResponse() {
		this.debugTimestamp = System.currentTimeMillis();
	}

	/**
	 * 创建成功响应
	 */
	public static McpDebugResponse success(String serviceName, McpServiceStatus serviceStatus,
			McpConnectionDiagnosis connectionDiagnosis, List<String> suggestions) {
		McpDebugResponse response = new McpDebugResponse();
		response.success = true;
		response.serviceName = serviceName;
		response.serviceStatus = serviceStatus;
		response.connectionDiagnosis = connectionDiagnosis;
		response.troubleshootingSuggestions = suggestions;
		return response;
	}

	/**
	 * 创建失败响应
	 */
	public static McpDebugResponse error(String serviceName, String errorMessage) {
		McpDebugResponse response = new McpDebugResponse();
		response.success = false;
		response.serviceName = serviceName;
		response.errorMessage = errorMessage;
		return response;
	}

	/**
	 * MCP 服务状态
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class McpServiceStatus {

		/**
		 * 服务是否在向量存储中找到
		 */
		@JsonProperty("found_in_vector_store")
		private boolean foundInVectorStore;

		/**
		 * 服务是否在 Nacos 中找到
		 */
		@JsonProperty("found_in_nacos")
		private boolean foundInNacos;

		/**
		 * 远程配置是否有效
		 */
		@JsonProperty("remote_config_valid")
		private boolean remoteConfigValid;

		/**
		 * 服务引用是否有效
		 */
		@JsonProperty("service_ref_valid")
		private boolean serviceRefValid;

		/**
		 * 端点是否可用
		 */
		@JsonProperty("endpoint_available")
		private boolean endpointAvailable;

		/**
		 * 连接是否已缓存
		 */
		@JsonProperty("connection_cached")
		private boolean connectionCached;

		/**
		 * 服务基本信息
		 */
		@JsonProperty("service_info")
		private Map<String, String> serviceInfo;

		public McpServiceStatus() {
		}

		// Getters and Setters
		public boolean isFoundInVectorStore() {
			return foundInVectorStore;
		}

		public void setFoundInVectorStore(boolean foundInVectorStore) {
			this.foundInVectorStore = foundInVectorStore;
		}

		public boolean isFoundInNacos() {
			return foundInNacos;
		}

		public void setFoundInNacos(boolean foundInNacos) {
			this.foundInNacos = foundInNacos;
		}

		public boolean isRemoteConfigValid() {
			return remoteConfigValid;
		}

		public void setRemoteConfigValid(boolean remoteConfigValid) {
			this.remoteConfigValid = remoteConfigValid;
		}

		public boolean isServiceRefValid() {
			return serviceRefValid;
		}

		public void setServiceRefValid(boolean serviceRefValid) {
			this.serviceRefValid = serviceRefValid;
		}

		public boolean isEndpointAvailable() {
			return endpointAvailable;
		}

		public void setEndpointAvailable(boolean endpointAvailable) {
			this.endpointAvailable = endpointAvailable;
		}

		public boolean isConnectionCached() {
			return connectionCached;
		}

		public void setConnectionCached(boolean connectionCached) {
			this.connectionCached = connectionCached;
		}

		public Map<String, String> getServiceInfo() {
			return serviceInfo;
		}

		public void setServiceInfo(Map<String, String> serviceInfo) {
			this.serviceInfo = serviceInfo;
		}

	}

	/**
	 * MCP 连接诊断
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class McpConnectionDiagnosis {

		/**
		 * 完整连接 URL
		 */
		@JsonProperty("full_url")
		private String fullUrl;

		/**
		 * 基础 URL 是否可达
		 */
		@JsonProperty("base_url_reachable")
		private boolean baseUrlReachable;

		/**
		 * 端点是否可达
		 */
		@JsonProperty("endpoint_reachable")
		private boolean endpointReachable;

		/**
		 * HTTP 状态码
		 */
		@JsonProperty("http_status_code")
		private Integer httpStatusCode;

		/**
		 * 响应时间（毫秒）
		 */
		@JsonProperty("response_time_ms")
		private Long responseTimeMs;

		/**
		 * 诊断详情
		 */
		@JsonProperty("diagnosis_details")
		private List<String> diagnosisDetails;

		/**
		 * 网络错误信息
		 */
		@JsonProperty("network_error")
		private String networkError;

		public McpConnectionDiagnosis() {
		}

		// Getters and Setters
		public String getFullUrl() {
			return fullUrl;
		}

		public void setFullUrl(String fullUrl) {
			this.fullUrl = fullUrl;
		}

		public boolean isBaseUrlReachable() {
			return baseUrlReachable;
		}

		public void setBaseUrlReachable(boolean baseUrlReachable) {
			this.baseUrlReachable = baseUrlReachable;
		}

		public boolean isEndpointReachable() {
			return endpointReachable;
		}

		public void setEndpointReachable(boolean endpointReachable) {
			this.endpointReachable = endpointReachable;
		}

		public Integer getHttpStatusCode() {
			return httpStatusCode;
		}

		public void setHttpStatusCode(Integer httpStatusCode) {
			this.httpStatusCode = httpStatusCode;
		}

		public Long getResponseTimeMs() {
			return responseTimeMs;
		}

		public void setResponseTimeMs(Long responseTimeMs) {
			this.responseTimeMs = responseTimeMs;
		}

		public List<String> getDiagnosisDetails() {
			return diagnosisDetails;
		}

		public void setDiagnosisDetails(List<String> diagnosisDetails) {
			this.diagnosisDetails = diagnosisDetails;
		}

		public String getNetworkError() {
			return networkError;
		}

		public void setNetworkError(String networkError) {
			this.networkError = networkError;
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

	public Long getDebugTimestamp() {
		return debugTimestamp;
	}

	public void setDebugTimestamp(Long debugTimestamp) {
		this.debugTimestamp = debugTimestamp;
	}

	public McpServiceStatus getServiceStatus() {
		return serviceStatus;
	}

	public void setServiceStatus(McpServiceStatus serviceStatus) {
		this.serviceStatus = serviceStatus;
	}

	public McpConnectionDiagnosis getConnectionDiagnosis() {
		return connectionDiagnosis;
	}

	public void setConnectionDiagnosis(McpConnectionDiagnosis connectionDiagnosis) {
		this.connectionDiagnosis = connectionDiagnosis;
	}

	public List<String> getTroubleshootingSuggestions() {
		return troubleshootingSuggestions;
	}

	public void setTroubleshootingSuggestions(List<String> troubleshootingSuggestions) {
		this.troubleshootingSuggestions = troubleshootingSuggestions;
	}

	public Map<String, Object> getSystemInfo() {
		return systemInfo;
	}

	public void setSystemInfo(Map<String, Object> systemInfo) {
		this.systemInfo = systemInfo;
	}

	@Override
	public String toString() {
		return "McpDebugResponse{" + "success=" + success + ", errorMessage='" + errorMessage + '\'' + ", serviceName='"
				+ serviceName + '\'' + ", debugTimestamp=" + debugTimestamp + ", serviceStatus=" + serviceStatus
				+ ", connectionDiagnosis=" + connectionDiagnosis + ", troubleshootingSuggestions="
				+ troubleshootingSuggestions + ", systemInfo=" + systemInfo + '}';
	}

}

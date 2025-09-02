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
 * MCP Server 添加响应结果 用于封装添加 MCP Server 的结果，包含服务信息、连接状态和可用工具列表
 *
 * @author spring-ai-alibaba
 * @since 2025.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpServerAddResponse {

	/**
	 * 操作是否成功
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
	 * 服务信息
	 */
	@JsonProperty("service_info")
	private McpServerServiceInfo serviceInfo;

	/**
	 * 连接状态
	 */
	@JsonProperty("connection_status")
	private McpConnectionStatus connectionStatus;

	/**
	 * 可用工具列表
	 */
	@JsonProperty("available_tools")
	private List<McpToolInfo> availableTools;

	/**
	 * 使用指南
	 */
	@JsonProperty("usage_guide")
	private McpUsageGuide usageGuide;

	public McpServerAddResponse() {
	}

	/**
	 * 创建成功响应
	 */
	public static McpServerAddResponse success(String serviceName, McpServerServiceInfo serviceInfo,
			McpConnectionStatus connectionStatus, List<McpToolInfo> tools, McpUsageGuide usageGuide) {
		McpServerAddResponse response = new McpServerAddResponse();
		response.success = true;
		response.serviceName = serviceName;
		response.serviceInfo = serviceInfo;
		response.connectionStatus = connectionStatus;
		response.availableTools = tools;
		response.usageGuide = usageGuide;
		return response;
	}

	/**
	 * 创建失败响应
	 */
	public static McpServerAddResponse error(String serviceName, String errorMessage) {
		McpServerAddResponse response = new McpServerAddResponse();
		response.success = false;
		response.serviceName = serviceName;
		response.errorMessage = errorMessage;
		return response;
	}

	/**
	 * MCP 服务信息
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class McpServerServiceInfo {

		/**
		 * 服务名称
		 */
		@JsonProperty("name")
		private String name;

		/**
		 * 服务描述
		 */
		@JsonProperty("description")
		private String description;

		/**
		 * 协议类型（如 mcp-sse, http, https）
		 */
		@JsonProperty("protocol")
		private String protocol;

		/**
		 * 服务版本
		 */
		@JsonProperty("version")
		private String version;

		/**
		 * 服务端点
		 */
		@JsonProperty("endpoint")
		private String endpoint;

		/**
		 * 服务标签
		 */
		@JsonProperty("tags")
		private List<String> tags;

		public McpServerServiceInfo() {
		}

		public McpServerServiceInfo(String name, String description, String protocol, String version, String endpoint,
				List<String> tags) {
			this.name = name;
			this.description = description;
			this.protocol = protocol;
			this.version = version;
			this.endpoint = endpoint;
			this.tags = tags;
		}

		// Getters and Setters
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getProtocol() {
			return protocol;
		}

		public void setProtocol(String protocol) {
			this.protocol = protocol;
		}

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}

		public String getEndpoint() {
			return endpoint;
		}

		public void setEndpoint(String endpoint) {
			this.endpoint = endpoint;
		}

		public List<String> getTags() {
			return tags;
		}

		public void setTags(List<String> tags) {
			this.tags = tags;
		}

	}

	/**
	 * MCP 连接状态
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class McpConnectionStatus {

		/**
		 * 是否已连接
		 */
		@JsonProperty("connected")
		private boolean connected;

		/**
		 * 连接URL
		 */
		@JsonProperty("connection_url")
		private String connectionUrl;

		/**
		 * 连接时间戳
		 */
		@JsonProperty("connected_at")
		private Long connectedAt;

		/**
		 * 连接消息或错误信息
		 */
		@JsonProperty("message")
		private String message;

		public McpConnectionStatus() {
		}

		public McpConnectionStatus(boolean connected, String connectionUrl, String message) {
			this.connected = connected;
			this.connectionUrl = connectionUrl;
			this.message = message;
			this.connectedAt = connected ? System.currentTimeMillis() : null;
		}

		// Getters and Setters
		public boolean isConnected() {
			return connected;
		}

		public void setConnected(boolean connected) {
			this.connected = connected;
		}

		public String getConnectionUrl() {
			return connectionUrl;
		}

		public void setConnectionUrl(String connectionUrl) {
			this.connectionUrl = connectionUrl;
		}

		public Long getConnectedAt() {
			return connectedAt;
		}

		public void setConnectedAt(Long connectedAt) {
			this.connectedAt = connectedAt;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

	}

	/**
	 * MCP 工具信息
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class McpToolInfo {

		/**
		 * 工具名称
		 */
		@JsonProperty("name")
		private String name;

		/**
		 * 工具描述
		 */
		@JsonProperty("description")
		private String description;

		/**
		 * 工具参数定义
		 */
		@JsonProperty("parameters")
		private Map<String, McpToolParameter> parameters;

		/**
		 * 是否启用
		 */
		@JsonProperty("enabled")
		private boolean enabled;

		public McpToolInfo() {
		}

		public McpToolInfo(String name, String description, Map<String, McpToolParameter> parameters, boolean enabled) {
			this.name = name;
			this.description = description;
			this.parameters = parameters;
			this.enabled = enabled;
		}

		// Getters and Setters
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public Map<String, McpToolParameter> getParameters() {
			return parameters;
		}

		public void setParameters(Map<String, McpToolParameter> parameters) {
			this.parameters = parameters;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

	/**
	 * MCP 工具参数
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class McpToolParameter {

		/**
		 * 参数类型
		 */
		@JsonProperty("type")
		private String type;

		/**
		 * 参数描述
		 */
		@JsonProperty("description")
		private String description;

		/**
		 * 是否必需
		 */
		@JsonProperty("required")
		private boolean required;

		/**
		 * 默认值
		 */
		@JsonProperty("default_value")
		private Object defaultValue;

		public McpToolParameter() {
		}

		public McpToolParameter(String type, String description, boolean required, Object defaultValue) {
			this.type = type;
			this.description = description;
			this.required = required;
			this.defaultValue = defaultValue;
		}

		// Getters and Setters
		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public boolean isRequired() {
			return required;
		}

		public void setRequired(boolean required) {
			this.required = required;
		}

		public Object getDefaultValue() {
			return defaultValue;
		}

		public void setDefaultValue(Object defaultValue) {
			this.defaultValue = defaultValue;
		}

	}

	/**
	 * MCP 使用指南
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class McpUsageGuide {

		/**
		 * 如何调用工具的说明
		 */
		@JsonProperty("how_to_use_tools")
		private String howToUseTools;

		/**
		 * 示例调用
		 */
		@JsonProperty("example_call")
		private String exampleCall;

		/**
		 * 可用操作列表
		 */
		@JsonProperty("available_operations")
		private List<String> availableOperations;

		public McpUsageGuide() {
		}

		public McpUsageGuide(String howToUseTools, String exampleCall, List<String> availableOperations) {
			this.howToUseTools = howToUseTools;
			this.exampleCall = exampleCall;
			this.availableOperations = availableOperations;
		}

		// Getters and Setters
		public String getHowToUseTools() {
			return howToUseTools;
		}

		public void setHowToUseTools(String howToUseTools) {
			this.howToUseTools = howToUseTools;
		}

		public String getExampleCall() {
			return exampleCall;
		}

		public void setExampleCall(String exampleCall) {
			this.exampleCall = exampleCall;
		}

		public List<String> getAvailableOperations() {
			return availableOperations;
		}

		public void setAvailableOperations(List<String> availableOperations) {
			this.availableOperations = availableOperations;
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

	public McpServerServiceInfo getServiceInfo() {
		return serviceInfo;
	}

	public void setServiceInfo(McpServerServiceInfo serviceInfo) {
		this.serviceInfo = serviceInfo;
	}

	public McpConnectionStatus getConnectionStatus() {
		return connectionStatus;
	}

	public void setConnectionStatus(McpConnectionStatus connectionStatus) {
		this.connectionStatus = connectionStatus;
	}

	public List<McpToolInfo> getAvailableTools() {
		return availableTools;
	}

	public void setAvailableTools(List<McpToolInfo> availableTools) {
		this.availableTools = availableTools;
	}

	public McpUsageGuide getUsageGuide() {
		return usageGuide;
	}

	public void setUsageGuide(McpUsageGuide usageGuide) {
		this.usageGuide = usageGuide;
	}

	@Override
	public String toString() {
		return "McpServerAddResponse{" + "success=" + success + ", errorMessage='" + errorMessage + '\''
				+ ", serviceName='" + serviceName + '\'' + ", serviceInfo=" + serviceInfo + ", connectionStatus="
				+ connectionStatus + ", availableTools=" + availableTools + ", usageGuide=" + usageGuide + '}';
	}

}

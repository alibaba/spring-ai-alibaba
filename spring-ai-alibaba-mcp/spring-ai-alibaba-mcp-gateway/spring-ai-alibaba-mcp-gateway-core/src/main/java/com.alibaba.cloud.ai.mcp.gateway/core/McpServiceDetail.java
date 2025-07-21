/*
 * Copyright 2024-2025 the original author or authors.
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

package com.alibaba.cloud.ai.mcp.gateway.core;

import java.util.List;
import java.util.Map;

/**
 * MCP 服务详情信息
 */
public class McpServiceDetail {

	private String name;

	private String protocol;

	private McpServiceRef serviceRef;

	private McpToolSpecification toolSpec;

	private McpVersionDetail versionDetail;

	public McpServiceDetail() {
	}

	public McpServiceDetail(String name, String protocol, McpServiceRef serviceRef, McpToolSpecification toolSpec,
			McpVersionDetail versionDetail) {
		this.name = name;
		this.protocol = protocol;
		this.serviceRef = serviceRef;
		this.toolSpec = toolSpec;
		this.versionDetail = versionDetail;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public McpServiceRef getServiceRef() {
		return serviceRef;
	}

	public void setServiceRef(McpServiceRef serviceRef) {
		this.serviceRef = serviceRef;
	}

	public McpToolSpecification getToolSpec() {
		return toolSpec;
	}

	public void setToolSpec(McpToolSpecification toolSpec) {
		this.toolSpec = toolSpec;
	}

	public McpVersionDetail getVersionDetail() {
		return versionDetail;
	}

	public void setVersionDetail(McpVersionDetail versionDetail) {
		this.versionDetail = versionDetail;
	}

	/**
	 * MCP 服务引用信息
	 */
	public static class McpServiceRef {

		private String serviceName;

		private String namespaceId;

		private String groupName;

		public McpServiceRef() {
		}

		public McpServiceRef(String serviceName, String namespaceId, String groupName) {
			this.serviceName = serviceName;
			this.namespaceId = namespaceId;
			this.groupName = groupName;
		}

		public String getServiceName() {
			return serviceName;
		}

		public void setServiceName(String serviceName) {
			this.serviceName = serviceName;
		}

		public String getNamespaceId() {
			return namespaceId;
		}

		public void setNamespaceId(String namespaceId) {
			this.namespaceId = namespaceId;
		}

		public String getGroupName() {
			return groupName;
		}

		public void setGroupName(String groupName) {
			this.groupName = groupName;
		}

	}

	/**
	 * MCP 工具规格信息
	 */
	public static class McpToolSpecification {

		private List<McpTool> tools;

		private Map<String, McpToolMeta> toolsMeta;

		public McpToolSpecification() {
		}

		public McpToolSpecification(List<McpTool> tools, Map<String, McpToolMeta> toolsMeta) {
			this.tools = tools;
			this.toolsMeta = toolsMeta;
		}

		public List<McpTool> getTools() {
			return tools;
		}

		public void setTools(List<McpTool> tools) {
			this.tools = tools;
		}

		public Map<String, McpToolMeta> getToolsMeta() {
			return toolsMeta;
		}

		public void setToolsMeta(Map<String, McpToolMeta> toolsMeta) {
			this.toolsMeta = toolsMeta;
		}

	}

	/**
	 * MCP 工具信息
	 */
	public static class McpTool {

		private String name;

		private String description;

		private Map<String, Object> inputSchema;

		public McpTool() {
		}

		public McpTool(String name, String description, Map<String, Object> inputSchema) {
			this.name = name;
			this.description = description;
			this.inputSchema = inputSchema;
		}

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

		public Map<String, Object> getInputSchema() {
			return inputSchema;
		}

		public void setInputSchema(Map<String, Object> inputSchema) {
			this.inputSchema = inputSchema;
		}

	}

	/**
	 * MCP 工具元数据信息
	 */
	public static class McpToolMeta {

		private boolean enabled;

		private Map<String, Object> templates;

		public McpToolMeta() {
		}

		public McpToolMeta(boolean enabled, Map<String, Object> templates) {
			this.enabled = enabled;
			this.templates = templates;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public Map<String, Object> getTemplates() {
			return templates;
		}

		public void setTemplates(Map<String, Object> templates) {
			this.templates = templates;
		}

	}

	/**
	 * MCP 版本详情信息
	 */
	public static class McpVersionDetail {

		private String version;

		public McpVersionDetail() {
		}

		public McpVersionDetail(String version) {
			this.version = version;
		}

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}

	}

}

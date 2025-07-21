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

/**
 * MCP Gateway 工具定义抽象类
 */
public abstract class McpGatewayToolDefinition {

	private String name;

	private String description;

	private String version;

	private String protocol;

	private McpServiceDetail.McpServiceRef serviceRef;

	private Boolean enabled;

	private Object inputSchema;

	private McpServiceDetail.McpToolMeta toolMeta;

	public McpGatewayToolDefinition() {
	}

	public McpGatewayToolDefinition(String name, String description, Object inputSchema, String version,
			String protocol, McpServiceDetail.McpServiceRef serviceRef, McpServiceDetail.McpToolMeta toolMeta,
			Boolean enabled) {
		this.name = name;
		this.description = description;
		this.inputSchema = inputSchema;
		this.version = version;
		this.protocol = protocol;
		this.serviceRef = serviceRef;
		this.toolMeta = toolMeta;
		this.enabled = enabled;
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

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public McpServiceDetail.McpServiceRef getServiceRef() {
		return serviceRef;
	}

	public void setServiceRef(McpServiceDetail.McpServiceRef serviceRef) {
		this.serviceRef = serviceRef;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public Object getInputSchema() {
		return inputSchema;
	}

	public void setInputSchema(Object inputSchema) {
		this.inputSchema = inputSchema;
	}

	public McpServiceDetail.McpToolMeta getToolMeta() {
		return toolMeta;
	}

	public void setToolMeta(McpServiceDetail.McpToolMeta toolMeta) {
		this.toolMeta = toolMeta;
	}

	/**
	 * 获取输入模式字符串
	 * @return 输入模式字符串
	 */
	public abstract String getInputSchemaString();

	/**
	 * 获取远程服务配置
	 * @return 远程服务配置
	 */
	public abstract Object getRemoteServerConfig();

}

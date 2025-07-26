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
package com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo;

import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.po.McpConfigStatus;
import java.util.Map;
import java.util.List;

/**
 * MCP configuration request value object, used to receive configuration information
 * passed from the front end
 */
public class McpConfigRequestVO {

	/**
	 * Connection type: STUDIO, STREAMING, SSE
	 */
	private String connectionType;

	/**
	 * JSON string of MCP server configuration
	 */
	private String configJson;

	// 新增字段化属性，用于直接接收前端表单数据
	private Long id; // 用于区分新增和更新操作

	private String command;

	private String url;

	private List<String> args;

	private Map<String, String> env;

	private String mcpServerName;

	private McpConfigStatus status = McpConfigStatus.ENABLE; // 默认为启用状态

	public String getConnectionType() {
		return connectionType;
	}

	public void setConnectionType(String connectionType) {
		this.connectionType = connectionType;
	}

	public String getConfigJson() {
		return configJson;
	}

	public void setConfigJson(String configJson) {
		this.configJson = configJson;
	}

	// 新增字段的getter和setter
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public List<String> getArgs() {
		return args;
	}

	public void setArgs(List<String> args) {
		this.args = args;
	}

	public Map<String, String> getEnv() {
		return env;
	}

	public void setEnv(Map<String, String> env) {
		this.env = env;
	}

	public String getMcpServerName() {
		return mcpServerName;
	}

	public void setMcpServerName(String mcpServerName) {
		this.mcpServerName = mcpServerName;
	}

	public McpConfigStatus getStatus() {
		return status;
	}

	public void setStatus(McpConfigStatus status) {
		this.status = status;
	}

	/**
	 * 将字段化数据转换为JSON格式
	 */
	public String buildConfigJson() {
		// 构建单个服务器配置
		StringBuilder serverConfigBuilder = new StringBuilder();
		serverConfigBuilder.append("{");

		boolean hasContent = false;

		if (command != null && !command.trim().isEmpty()) {
			if (hasContent)
				serverConfigBuilder.append(",");
			serverConfigBuilder.append("\"command\":\"").append(command).append("\"");
			hasContent = true;
		}

		if (url != null && !url.trim().isEmpty()) {
			if (hasContent)
				serverConfigBuilder.append(",");
			serverConfigBuilder.append("\"url\":\"").append(url).append("\"");
			hasContent = true;
		}

		if (args != null && !args.isEmpty()) {
			if (hasContent)
				serverConfigBuilder.append(",");
			serverConfigBuilder.append("\"args\":[");
			for (int i = 0; i < args.size(); i++) {
				if (i > 0)
					serverConfigBuilder.append(",");
				serverConfigBuilder.append("\"").append(args.get(i)).append("\"");
			}
			serverConfigBuilder.append("]");
			hasContent = true;
		}

		if (env != null && !env.isEmpty()) {
			if (hasContent)
				serverConfigBuilder.append(",");
			serverConfigBuilder.append("\"env\":{");
			boolean first = true;
			for (Map.Entry<String, String> entry : env.entrySet()) {
				if (!first)
					serverConfigBuilder.append(",");
				serverConfigBuilder.append("\"")
					.append(entry.getKey())
					.append("\":\"")
					.append(entry.getValue())
					.append("\"");
				first = false;
			}
			serverConfigBuilder.append("}");
			hasContent = true;
		}

		// 添加status字段（总是包含）
		if (hasContent)
			serverConfigBuilder.append(",");
		serverConfigBuilder.append("\"status\":\"").append(status.name()).append("\"");

		serverConfigBuilder.append("}");

		// 构建完整的McpServersConfig格式
		String serverName = mcpServerName != null ? mcpServerName : "default";
		StringBuilder fullConfigBuilder = new StringBuilder();
		fullConfigBuilder.append("{\"mcpServers\":{\"");
		fullConfigBuilder.append(serverName);
		fullConfigBuilder.append("\":");
		fullConfigBuilder.append(serverConfigBuilder.toString());
		fullConfigBuilder.append("}}");

		return fullConfigBuilder.toString();
	}

	/**
	 * 检查是否使用字段化数据
	 */
	public boolean isFieldBased() {
		return (command != null && !command.trim().isEmpty()) || (url != null && !url.trim().isEmpty())
				|| (args != null && !args.isEmpty()) || (env != null && !env.isEmpty());
	}

}

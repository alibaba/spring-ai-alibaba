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

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 单个MCP服务器操作请求VO（表单方式） 用于新增和更新单个MCP服务器配置
 */
public class McpServerFormRequestVO {

	/**
	 * 服务器ID，用于区分新增/更新操作 null表示新增，非null表示更新
	 */
	private Long id;

	/**
	 * MCP服务器名称
	 */
	@JsonProperty("mcpServerName")
	private String mcpServerName;

	/**
	 * 连接类型：STUDIO, SSE, STREAMING
	 */
	@JsonProperty("connectionType")
	private String connectionType;

	/**
	 * 命令（STUDIO类型必需）
	 */
	private String command;

	/**
	 * URL（SSE/STREAMING类型必需）
	 */
	private String url;

	/**
	 * 参数列表（STUDIO类型可选）
	 */
	private List<String> args;

	/**
	 * 环境变量（STUDIO类型可选）
	 */
	private Map<String, String> env;

	/**
	 * 状态：ENABLE, DISABLE
	 */
	private String status;

	// Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getMcpServerName() {
		return mcpServerName;
	}

	public void setMcpServerName(String mcpServerName) {
		this.mcpServerName = mcpServerName;
	}

	public String getConnectionType() {
		return connectionType;
	}

	public void setConnectionType(String connectionType) {
		this.connectionType = connectionType;
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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * 判断是否为更新操作
	 * @return true表示更新，false表示新增
	 */
	public boolean isUpdate() {
		return id != null;
	}

	/**
	 * 验证请求数据是否有效
	 * @return true表示有效，false表示无效
	 */
	public boolean isValid() {
		// 基本字段验证
		if (mcpServerName == null || mcpServerName.trim().isEmpty()) {
			return false;
		}
		if (connectionType == null || connectionType.trim().isEmpty()) {
			return false;
		}

		// 根据连接类型验证必需字段
		switch (connectionType.toUpperCase()) {
			case "STUDIO":
				return command != null && !command.trim().isEmpty();
			case "SSE":
			case "STREAMING":
				return url != null && !url.trim().isEmpty();
			default:
				return false;
		}

	}

	/**
	 * 构建单个服务器的JSON配置
	 * @return JSON字符串
	 */
	public String buildConfigJson() {
		StringBuilder jsonBuilder = new StringBuilder();
		jsonBuilder.append("{");

		// 添加command（如果存在）
		if (command != null && !command.trim().isEmpty()) {
			jsonBuilder.append("\"command\":\"").append(command).append("\"");
		}

		// 添加url（如果存在）
		if (url != null && !url.trim().isEmpty()) {
			if (jsonBuilder.length() > 1) {
				jsonBuilder.append(",");
			}
			jsonBuilder.append("\"url\":\"").append(url).append("\"");
		}

		// 添加args（如果存在）
		if (args != null && !args.isEmpty()) {
			if (jsonBuilder.length() > 1) {
				jsonBuilder.append(",");
			}
			jsonBuilder.append("\"args\":[");
			for (int i = 0; i < args.size(); i++) {
				if (i > 0) {
					jsonBuilder.append(",");
				}
				jsonBuilder.append("\"").append(args.get(i)).append("\"");
			}
			jsonBuilder.append("]");
		}

		// 添加env（如果存在）
		if (env != null && !env.isEmpty()) {
			if (jsonBuilder.length() > 1) {
				jsonBuilder.append(",");
			}
			jsonBuilder.append("\"env\":{");
			boolean first = true;
			for (Map.Entry<String, String> entry : env.entrySet()) {
				if (!first) {
					jsonBuilder.append(",");
				}
				jsonBuilder.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
				first = false;
			}
			jsonBuilder.append("}");
		}

		jsonBuilder.append("}");
		return jsonBuilder.toString();
	}

	/**
	 * 构建完整的MCP配置JSON（包含mcpServers包装）
	 * @return 完整的JSON字符串
	 */
	public String buildFullConfigJson() {
		StringBuilder fullJsonBuilder = new StringBuilder();
		fullJsonBuilder.append("{\n  \"mcpServers\": {\n");
		fullJsonBuilder.append("    \"").append(mcpServerName).append("\": ");
		fullJsonBuilder.append(buildConfigJson());
		fullJsonBuilder.append("\n  }\n}");
		return fullJsonBuilder.toString();
	}

}

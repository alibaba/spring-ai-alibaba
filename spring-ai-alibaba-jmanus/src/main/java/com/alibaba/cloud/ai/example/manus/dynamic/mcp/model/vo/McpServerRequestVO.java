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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 单个MCP服务器操作请求VO（表单方式） 用于新增和更新单个MCP服务器配置
 */
public class McpServerRequestVO {

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
		return validateWithDetails().isEmpty();
	}

	/**
	 * 验证请求数据并返回详细错误信息
	 * @return 错误信息列表，空列表表示验证通过
	 */
	public List<String> validateWithDetails() {
		List<String> errors = new ArrayList<>();

		// 基本字段验证
		if (mcpServerName == null || mcpServerName.trim().isEmpty()) {
			errors.add("MCP名称不能为空");
		}

		if (connectionType == null || connectionType.trim().isEmpty()) {
			errors.add("连接类型不能为空");
		}

		// 根据连接类型验证必需字段
		if (connectionType != null) {
			String connectionTypeUpper = connectionType.toUpperCase();
			switch (connectionTypeUpper) {
				case "STUDIO":
					if (command == null || command.trim().isEmpty()) {
						errors.add("STUDIO类型必须提供命令(command)");
					}
					break;
				case "SSE":
					if (url == null || url.trim().isEmpty()) {
						errors.add("SSE类型必须提供URL");
					}
					else if (!isValidUrlFormat(url)) {
						errors.add("SSE类型的URL格式无效: " + url);
					}
					else if (!isSSEUrl(url)) {
						errors.add("SSE类型的URL路径必须包含'sse'，当前URL: " + url);
					}
					break;
				case "STREAMING":
					if (url == null || url.trim().isEmpty()) {
						errors.add("STREAMING类型必须提供URL");
					}
					else if (!isValidUrlFormat(url)) {
						errors.add("STREAMING类型的URL格式无效: " + url);
					}
					else if (isSSEUrl(url)) {
						errors.add("STREAMING类型的URL路径不能包含'sse'，当前URL: " + url);
					}
					break;
				default:
					errors.add("不支持的连接类型: " + connectionTypeUpper);
					break;
			}
		}

		return errors;
	}

	/**
	 * 验证URL格式是否有效
	 * @param url 服务器URL
	 * @return 是否为有效URL格式
	 */
	private boolean isValidUrlFormat(String url) {
		if (url == null || url.trim().isEmpty()) {
			return false;
		}

		try {
			new java.net.URL(url.trim());
			return true;
		}
		catch (java.net.MalformedURLException e) {
			return false;
		}
	}

	/**
	 * 判断URL是否为SSE连接（与McpServerConfig保持一致）
	 * @param url 服务器URL
	 * @return 是否为SSE URL
	 */
	private boolean isSSEUrl(String url) {
		if (url == null || url.isEmpty()) {
			return false;
		}

		try {
			java.net.URL parsedUrl = new java.net.URL(url);
			String path = parsedUrl.getPath();

			// 检查路径是否包含sse
			boolean pathContainsSse = path != null && path.toLowerCase().contains("sse");

			return pathContainsSse;
		}
		catch (java.net.MalformedURLException e) {
			// 如果URL格式无效，返回false
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

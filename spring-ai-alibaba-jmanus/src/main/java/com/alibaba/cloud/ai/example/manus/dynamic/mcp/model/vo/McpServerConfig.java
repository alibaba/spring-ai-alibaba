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

import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.po.McpConfigType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.po.McpConfigStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Internal server configuration class
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class McpServerConfig {

	private String url;

	@JsonProperty("command")
	private String command;

	@JsonProperty("args")
	private List<String> args;

	@JsonProperty("env")
	private Map<String, String> env;

	@JsonProperty("status")
	private McpConfigStatus status = McpConfigStatus.ENABLE; // 默认为启用状态

	public McpServerConfig() {
		this.env = new HashMap<>();
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
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

	public McpConfigStatus getStatus() {
		return status;
	}

	public void setStatus(McpConfigStatus status) {
		this.status = status;
	}

	/**
	 * 获取连接类型 判断逻辑： 1. 如果有command字段 → STUDIO 2. 如果URL后缀是sse → SSE 3. 其他情况 → STREAMING
	 * @return 连接类型
	 */
	public McpConfigType getConnectionType() {
		// 1. 检查是否有command字段
		if (command != null && !command.isEmpty()) {
			return McpConfigType.STUDIO;
		}

		// 2. 检查URL后缀是否为sse
		if (url != null && !url.isEmpty() && isSSEUrl(url)) {
			return McpConfigType.SSE;
		}

		// 3. 其他情况默认为STREAMING
		return McpConfigType.STREAMING;
	}

	/**
	 * 判断URL是否为SSE连接
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
	 * Convert ServerConfig to JSON string
	 * @return Converted JSON string
	 */
	public String toJson() {
		try {
			return new ObjectMapper().writeValueAsString(this);
		}
		catch (Exception e) {
			// If serialization fails, manually build a simplified JSON
			StringBuilder sb = new StringBuilder();
			sb.append("{");

			// Add URL (if it exists)
			if (url != null && !url.isEmpty()) {
				sb.append("\"url\":\"").append(url).append("\"");
			}

			// Add command (if it exists)
			if (command != null && !command.isEmpty()) {
				if (sb.length() > 1)
					sb.append(",");
				sb.append("\"command\":\"").append(command).append("\"");
			}

			// Add parameters (if they exist)
			if (args != null && !args.isEmpty()) {
				if (sb.length() > 1)
					sb.append(",");
				sb.append("\"args\":[");
				boolean first = true;
				for (String arg : args) {
					if (!first)
						sb.append(",");
					sb.append("\"").append(arg).append("\"");
					first = false;
				}
				sb.append("]");
			}

			// Add environment variables (if they exist)
			if (env != null && !env.isEmpty()) {
				if (sb.length() > 1)
					sb.append(",");
				sb.append("\"env\":{");
				boolean first = true;
				for (Map.Entry<String, String> entry : env.entrySet()) {
					if (!first)
						sb.append(",");
					sb.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
					first = false;
				}
				sb.append("}");
			}

			// Add status (always include)
			if (sb.length() > 1)
				sb.append(",");
			sb.append("\"status\":\"").append(status.name()).append("\"");

			sb.append("}");
			return sb.toString();
		}
	}

}

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
package com.alibaba.cloud.ai.manus.mcp.model.vo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.manus.mcp.model.po.McpConfigStatus;
import com.alibaba.cloud.ai.manus.mcp.model.po.McpConfigType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Internal server configuration class
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class McpServerConfig {

	private final ObjectMapper objectMapper;

	/**
	 * Default constructor for Jackson deserialization
	 */
	public McpServerConfig() {
		this.env = new HashMap<>();
		this.objectMapper = new ObjectMapper();
	}

	public McpServerConfig(ObjectMapper objectMapper) {
		this.env = new HashMap<>();
		this.objectMapper = objectMapper;
	}

	private String url;

	@JsonProperty("command")
	private String command;

	@JsonProperty("args")
	private List<String> args;

	@JsonProperty("env")
	private Map<String, String> env;

	@JsonProperty("status")
	private McpConfigStatus status = McpConfigStatus.ENABLE; // Default to enabled status

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
	 * Get connection type. Logic: 1. If has command field → STUDIO 2. If URL suffix is
	 * sse → SSE 3. Other cases → STREAMING
	 * @return Connection type
	 */
	public McpConfigType getConnectionType() {
		// 1. Check if has command field
		if (command != null && !command.isEmpty()) {
			return McpConfigType.STUDIO;
		}

		// 2. Check if URL suffix is sse
		if (url != null && !url.isEmpty() && isSSEUrl(url)) {
			return McpConfigType.SSE;
		}

		// 3. Other cases default to STREAMING
		return McpConfigType.STREAMING;
	}

	/**
	 * Determine if URL is SSE connection
	 * @param url Server URL
	 * @return Whether it's SSE URL
	 */
	private boolean isSSEUrl(String url) {
		if (url == null || url.isEmpty()) {
			return false;
		}

		try {
			java.net.URL parsedUrl = new java.net.URL(url);
			String path = parsedUrl.getPath();

			// Check if path contains sse
			boolean pathContainsSse = path != null && path.toLowerCase().contains("sse");

			return pathContainsSse;
		}
		catch (java.net.MalformedURLException e) {
			// Return false if URL format is invalid
			return false;
		}
	}

	/**
	 * Convert ServerConfig to JSON string
	 * @return Converted JSON string
	 */
	public String toJson() {
		try {
			return objectMapper.writeValueAsString(this);
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

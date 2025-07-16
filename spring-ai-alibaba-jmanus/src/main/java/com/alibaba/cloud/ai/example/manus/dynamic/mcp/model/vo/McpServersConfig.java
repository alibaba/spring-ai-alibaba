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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP server configuration parsing object for parsing configuration format like: {
 * "mcpServers": { "server-name": { "url": "http://localhost:3000/sse", "env": {
 * "API_KEY": "value" } } } }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class McpServersConfig {

	@JsonProperty("mcpServers")
	private Map<String, McpServerConfig> mcpServers;

	public McpServersConfig() {
		this.mcpServers = new HashMap<>();
	}

	public Map<String, McpServerConfig> getMcpServers() {
		return mcpServers;
	}

	public void setMcpServers(Map<String, McpServerConfig> mcpServers) {
		this.mcpServers = mcpServers;
	}

	/**
	 * Get the first server configuration
	 * @return First server configuration, returns null if none exists
	 */
	public McpServerConfig getFirstServerConfig() {
		if (mcpServers != null && !mcpServers.isEmpty()) {
			String firstKey = mcpServers.keySet().iterator().next();
			return mcpServers.get(firstKey);
		}
		return null;
	}

	/**
	 * Get the first server name
	 * @return First server name, returns null if none exists
	 */
	public String getFirstServerName() {
		if (mcpServers != null && !mcpServers.isEmpty()) {
			return mcpServers.keySet().iterator().next();
		}
		return null;
	}

	/**
	 * Convert to SseParameters
	 * @return Converted SseParameters content
	 */
	public String toSseParametersJson() {
		McpServerConfig serverConfig = getFirstServerConfig();
		if (serverConfig != null && serverConfig.getUrl() != null) {
			// Build SseParameters format JSON
			StringBuilder sb = new StringBuilder();
			sb.append("{\"base_uri\":\"").append(serverConfig.getUrl()).append("\"");

			// Add environment variables as request headers
			if (serverConfig.getEnv() != null && !serverConfig.getEnv().isEmpty()) {
				sb.append(",\"headers\":{");
				boolean first = true;
				for (Map.Entry<String, String> entry : serverConfig.getEnv().entrySet()) {
					if (!first) {
						sb.append(",");
					}
					sb.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
					first = false;
				}
				sb.append("}");
			}

			sb.append("}");
			return sb.toString();
		}
		return null;
	}

	/**
	 * Determine if current configuration is STUDIO type
	 * @return Returns true if it's STUDIO type (contains command field), false otherwise
	 */
	public boolean isStudioType() {
		McpServerConfig serverConfig = getFirstServerConfig();
		return serverConfig != null && serverConfig.getCommand() != null;
	}

	/**
	 * Convert to ServerParameters format JSON (for STUDIO type connection). Supports the
	 * following two formats: { "mcpServers": { "server-name": { "command": "npx", "args":
	 * ["-y", "mcp-server"], "env": { "API_KEY": "value" } } } }
	 *
	 * Or:
	 *
	 * { "mcpServers": { "server-name": { "command": "python", "args": ["mcp-server.py"],
	 * "env": { "API_KEY": "value" } } } }
	 * @return Converted ServerParameters format JSON
	 */
	public String toServerParametersJson() {
		McpServerConfig serverConfig = getFirstServerConfig();
		if (serverConfig != null && serverConfig.getCommand() != null) {
			StringBuilder sb = new StringBuilder();
			sb.append("{");

			// Add command
			sb.append("\"command\":\"").append(serverConfig.getCommand()).append("\"");

			// Add parameters
			if (serverConfig.getArgs() != null && !serverConfig.getArgs().isEmpty()) {
				sb.append(",\"args\":[");
				boolean first = true;
				for (String arg : serverConfig.getArgs()) {
					if (!first) {
						sb.append(",");
					}
					sb.append("\"").append(arg).append("\"");
					first = false;
				}
				sb.append("]");
			}

			// Add environment variables
			if (serverConfig.getEnv() != null && !serverConfig.getEnv().isEmpty()) {
				sb.append(",\"env\":{");
				boolean first = true;
				for (Map.Entry<String, String> entry : serverConfig.getEnv().entrySet()) {
					if (!first) {
						sb.append(",");
					}
					sb.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
					first = false;
				}
				sb.append("}");
			}

			sb.append("}");
			return sb.toString();
		}
		return null;
	}

}

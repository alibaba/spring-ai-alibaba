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
 * MCP服务器配置解析对象，用于解析形如： { "mcpServers": { "server-name": { "url":
 * "http://localhost:3000/sse", "env": { "API_KEY": "value" } } } } 的配置格式
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
	 * 获取第一个服务器配置
	 * @return 第一个服务器配置，如果没有则返回null
	 */
	public McpServerConfig getFirstServerConfig() {
		if (mcpServers != null && !mcpServers.isEmpty()) {
			String firstKey = mcpServers.keySet().iterator().next();
			return mcpServers.get(firstKey);
		}
		return null;
	}

	/**
	 * 获取第一个服务器名称
	 * @return 第一个服务器名称，如果没有则返回null
	 */
	public String getFirstServerName() {
		if (mcpServers != null && !mcpServers.isEmpty()) {
			return mcpServers.keySet().iterator().next();
		}
		return null;
	}

	/**
	 * 转换为SseParameters
	 * @return 转换后的SseParameters内容
	 */
	public String toSseParametersJson() {
		McpServerConfig serverConfig = getFirstServerConfig();
		if (serverConfig != null && serverConfig.getUrl() != null) {
			// 构建SseParameters格式的JSON
			StringBuilder sb = new StringBuilder();
			sb.append("{\"base_uri\":\"").append(serverConfig.getUrl()).append("\"");

			// 添加环境变量作为请求头
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
	 * 判断当前配置是否为STUDIO类型
	 * @return 如果是STUDIO类型（包含command字段）则返回true，否则返回false
	 */
	public boolean isStudioType() {
		McpServerConfig serverConfig = getFirstServerConfig();
		return serverConfig != null && serverConfig.getCommand() != null;
	}

	/**
	 * 转换为ServerParameters格式的JSON（用于STUDIO类型连接） 支持如下两种格式： { "mcpServers": { "server-name":
	 * { "command": "npx", "args": ["-y", "mcp-server"], "env": { "API_KEY": "value" } } }
	 * }
	 *
	 * 或者：
	 *
	 * { "mcpServers": { "server-name": { "command": "python", "args": ["mcp-server.py"],
	 * "env": { "API_KEY": "value" } } } }
	 * @return 转换后的ServerParameters格式JSON
	 */
	public String toServerParametersJson() {
		McpServerConfig serverConfig = getFirstServerConfig();
		if (serverConfig != null && serverConfig.getCommand() != null) {
			StringBuilder sb = new StringBuilder();
			sb.append("{");

			// 添加命令
			sb.append("\"command\":\"").append(serverConfig.getCommand()).append("\"");

			// 添加参数
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

			// 添加环境变量
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

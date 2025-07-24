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
package com.alibaba.cloud.ai.example.manus.dynamic.mcp.service;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.po.McpConfigType;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpServerConfig;

/**
 * DecisionConnectionType - 基于配置判断连接类型
 *
 * 判断逻辑：
 * 1. 如果有command字段 → STUDIO
 * 2. 如果URL后缀是sse → SSE
 * 3. 其他情况 → STREAMING
 */
@Component
public class DecisionConnectionType {

	private static final Logger logger = LoggerFactory.getLogger(DecisionConnectionType.class);

	/**
	 * 判断MCP服务器配置的连接类型
	 * @param serverConfig MCP服务器配置
	 * @return 连接类型
	 * @throws IOException 当无法判断协议类型时抛出异常
	 */
	public static McpConfigType decideConnectionType(McpServerConfig serverConfig) throws IOException {
		// 1. 检查是否有command字段
		if (serverConfig.getCommand() != null && !serverConfig.getCommand().isEmpty()) {
			logger.debug("Server has command field, using STUDIO connection type");
			return McpConfigType.STUDIO;
		}

		// 2. 检查URL后缀是否为sse
		String url = serverConfig.getUrl();
		if (url != null && !url.isEmpty() && isSSEUrl(url)) {
			logger.debug("URL ends with sse, using SSE connection type");
			return McpConfigType.SSE;
		}

		// 3. 其他情况默认为STREAMING
		logger.debug("Defaulting to STREAMING connection type");
		return McpConfigType.STREAMING;
	}

	/**
	 * 判断URL后缀是否为sse
	 * @param url 服务器URL
	 * @return 是否为SSE URL
	 */
	private static boolean isSSEUrl(String url) {
		if (url == null || url.isEmpty()) {
			return false;
		}

		// 检查URL是否以sse结尾
		String lowerUrl = url.toLowerCase();
		return lowerUrl.endsWith("/sse") || lowerUrl.endsWith("sse");
	}

	/**
	 * 批量判断多个服务器配置的连接类型
	 * @param serverConfigs 服务器配置映射
	 * @return 服务器名称到连接类型的映射
	 * @throws IOException 当无法判断协议类型时抛出异常
	 */
	public static Map<String, McpConfigType> decideConnectionTypes(Map<String, McpServerConfig> serverConfigs)
			throws IOException {

		Map<String, McpConfigType> connectionTypes = new java.util.HashMap<>();

		for (Map.Entry<String, McpServerConfig> entry : serverConfigs.entrySet()) {
			String serverName = entry.getKey();
			McpServerConfig serverConfig = entry.getValue();

			try {
				McpConfigType connectionType = decideConnectionType(serverConfig);
				connectionTypes.put(serverName, connectionType);
				logger.info("Server '{}' connection type determined as: {}", serverName, connectionType);
			}
			catch (Exception e) {
				logger.error("Failed to determine connection type for server '{}': {}", serverName, e.getMessage());
				throw new IOException(
						"Failed to determine connection type for server '" + serverName + "': " + e.getMessage(), e);
			}
		}

		return connectionTypes;
	}

}
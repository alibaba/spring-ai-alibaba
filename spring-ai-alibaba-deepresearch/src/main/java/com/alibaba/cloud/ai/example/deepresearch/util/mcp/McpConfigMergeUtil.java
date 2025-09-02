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

package com.alibaba.cloud.ai.example.deepresearch.util.mcp;

import com.alibaba.cloud.ai.example.deepresearch.config.McpAssignNodeProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.client.autoconfigure.NamedClientMcpTransport;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MCP配置合并工具类
 *
 * @author Makoto
 */
public class McpConfigMergeUtil {

	private static final Logger logger = LoggerFactory.getLogger(McpConfigMergeUtil.class);

	/**
	 * 合并静态配置和动态配置
	 */
	public static Map<String, McpAssignNodeProperties.McpServerConfig> mergeAgent2McpConfigs(
			Map<String, McpAssignNodeProperties.McpServerConfig> staticConfig, Map<String, Object> runtimeSettings,
			ObjectMapper objectMapper) {

		Map<String, McpAssignNodeProperties.McpServerConfig> result = new HashMap<>();

		// 这边复制所有静态配置
		for (Map.Entry<String, McpAssignNodeProperties.McpServerConfig> entry : staticConfig.entrySet()) {
			String agentName = entry.getKey();
			List<McpAssignNodeProperties.McpServerInfo> staticServers = new ArrayList<>(
					Optional.ofNullable(entry.getValue().mcpServers()).orElse(List.of()));
			result.put(agentName, new McpAssignNodeProperties.McpServerConfig(staticServers));
		}

		// 处理动态配置
		for (Map.Entry<String, Object> entry : runtimeSettings.entrySet()) {
			String agentName = entry.getKey();

			if (entry.getValue() instanceof Map) {
				Map<String, Object> agentConfig = (Map<String, Object>) entry.getValue();

				if (agentConfig.containsKey("mcp-servers")) {
					McpAssignNodeProperties.McpServerConfig dynamicConfig = objectMapper.convertValue(agentConfig,
							McpAssignNodeProperties.McpServerConfig.class);

					// 合并该Agent的服务器配置
					List<McpAssignNodeProperties.McpServerInfo> mergedServers = mergeAgent2McpServers(
							result.getOrDefault(agentName, new McpAssignNodeProperties.McpServerConfig(List.of()))
								.mcpServers(),
							dynamicConfig.mcpServers());

					result.put(agentName, new McpAssignNodeProperties.McpServerConfig(mergedServers));
					logger.debug("Merged MCP config for agent: {}", agentName);
				}
			}
		}

		return result;
	}

	/**
	 * 合并服务器配置列表（相同URL覆盖，新URL追加）
	 */
	public static List<McpAssignNodeProperties.McpServerInfo> mergeAgent2McpServers(
			List<McpAssignNodeProperties.McpServerInfo> staticServers,
			List<McpAssignNodeProperties.McpServerInfo> dynamicServers) {

		Map<String, McpAssignNodeProperties.McpServerInfo> serverMap = new LinkedHashMap<>();

		for (McpAssignNodeProperties.McpServerInfo server : staticServers) {
			serverMap.put(server.url(), server);
		}

		// 动态服务器覆盖或添加
		for (McpAssignNodeProperties.McpServerInfo server : dynamicServers) {
			serverMap.put(server.url(), server);
		}

		return new ArrayList<>(serverMap.values());
	}

	/**
	 * 创建Transport的辅助方法
	 */
	public static List<NamedClientMcpTransport> createAgent2McpTransports(String agentName,
			McpAssignNodeProperties.McpServerConfig config, WebClient.Builder webClientBuilderTemplate,
			ObjectMapper objectMapper) {
		List<NamedClientMcpTransport> transports = new ArrayList<>();

		for (McpAssignNodeProperties.McpServerInfo serverInfo : config.mcpServers()) {
			if (!serverInfo.enabled()) {
				continue;
			}

			WebClient.Builder webClientBuilder = webClientBuilderTemplate.clone().baseUrl(serverInfo.url());
			String sseEndpoint = serverInfo.sseEndpoint() != null ? serverInfo.sseEndpoint() : "/sse";
			WebFluxSseClientTransport transport = WebFluxSseClientTransport.builder(webClientBuilder)
				.sseEndpoint(sseEndpoint)
				.objectMapper(objectMapper)
				.build();
			String transportName = agentName + "-" + serverInfo.url().hashCode();
			transports.add(new NamedClientMcpTransport(transportName, transport));
		}

		return transports;
	}

}

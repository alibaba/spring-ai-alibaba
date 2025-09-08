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
 * MCP Configuration Merging Utility Class
 *
 * @author Makoto
 */
public class McpConfigMergeUtil {

	private static final Logger logger = LoggerFactory.getLogger(McpConfigMergeUtil.class);

	/**
	 * Merges static and dynamic configurations
	 */
	public static Map<String, McpAssignNodeProperties.McpServerConfig> mergeAgent2McpConfigs(
			Map<String, McpAssignNodeProperties.McpServerConfig> staticConfig, Map<String, Object> runtimeSettings,
			ObjectMapper objectMapper) {

		Map<String, McpAssignNodeProperties.McpServerConfig> result = new HashMap<>();

		// Copy all static configurations here
		for (Map.Entry<String, McpAssignNodeProperties.McpServerConfig> entry : staticConfig.entrySet()) {
			String agentName = entry.getKey();
			List<McpAssignNodeProperties.McpServerInfo> staticServers = new ArrayList<>(
					Optional.ofNullable(entry.getValue().mcpServers()).orElse(List.of()));
			result.put(agentName, new McpAssignNodeProperties.McpServerConfig(staticServers));
		}

		// Process dynamic configurations
		for (Map.Entry<String, Object> entry : runtimeSettings.entrySet()) {
			String agentName = entry.getKey();

			if (entry.getValue() instanceof Map) {
				Map<String, Object> agentConfig = (Map<String, Object>) entry.getValue();

				if (agentConfig.containsKey("mcp-servers")) {
					McpAssignNodeProperties.McpServerConfig dynamicConfig = objectMapper.convertValue(agentConfig,
							McpAssignNodeProperties.McpServerConfig.class);

					// Merge server configurations for this agent
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
	 * Merges server configuration lists (same URLs are overwritten, new URLs are appended)
	 */
	public static List<McpAssignNodeProperties.McpServerInfo> mergeAgent2McpServers(
			List<McpAssignNodeProperties.McpServerInfo> staticServers,
			List<McpAssignNodeProperties.McpServerInfo> dynamicServers) {

		Map<String, McpAssignNodeProperties.McpServerInfo> serverMap = new LinkedHashMap<>();

		for (McpAssignNodeProperties.McpServerInfo server : staticServers) {
			serverMap.put(server.url(), server);
		}

		// Dynamic server override or addition
		for (McpAssignNodeProperties.McpServerInfo server : dynamicServers) {
			serverMap.put(server.url(), server);
		}

		return new ArrayList<>(serverMap.values());
	}

	/**
	 * Helper method for creating Transport
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

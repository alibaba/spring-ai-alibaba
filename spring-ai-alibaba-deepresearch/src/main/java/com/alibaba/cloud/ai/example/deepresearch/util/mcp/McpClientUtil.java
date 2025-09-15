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
import com.alibaba.cloud.ai.graph.OverAllState;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.mcp.client.autoconfigure.NamedClientMcpTransport;
import org.springframework.ai.mcp.client.autoconfigure.configurer.McpAsyncClientConfigurer;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * MCP客户端创建和初始化工具类
 *
 * @author Makoto
 */
public class McpClientUtil {

	private static final Logger logger = LoggerFactory.getLogger(McpClientUtil.class);

	/**
	 * 创建MCP客户端提供者
	 * @param state 状态对象
	 * @param agentName 代理名称 (如: "coderAgent", "researchAgent")
	 * @param mcpConfigProvider MCP配置提供者
	 * @param mcpAsyncClientConfigurer MCP异步客户端配置器
	 * @param commonProperties MCP客户端通用属性
	 * @param webClientBuilderTemplate WebClient构建器模板
	 * @param objectMapper JSON对象映射器
	 * @return AsyncMcpToolCallbackProvider 或 null
	 */
	public static AsyncMcpToolCallbackProvider createMcpProvider(OverAllState state, String agentName,
			Function<OverAllState, Map<String, McpAssignNodeProperties.McpServerConfig>> mcpConfigProvider,
			McpAsyncClientConfigurer mcpAsyncClientConfigurer, McpClientCommonProperties commonProperties,
			WebClient.Builder webClientBuilderTemplate, ObjectMapper objectMapper) {

		if (mcpConfigProvider == null || mcpAsyncClientConfigurer == null) {
			logger.debug("MCP configuration not available for {}", agentName);
			return null;
		}

		try {
			// 获取配置
			Map<String, McpAssignNodeProperties.McpServerConfig> mcpAgentConfigs = mcpConfigProvider.apply(state);
			McpAssignNodeProperties.McpServerConfig config = mcpAgentConfigs.get(agentName);

			if (config == null || config.mcpServers().isEmpty()) {
				logger.debug("No MCP servers configured for {}", agentName);
				return null;
			}

			List<McpAsyncClient> mcpAsyncClients = new ArrayList<>();
			String threadId = state.value("thread_id", "__default__");

			logger.debug("Creating MCP clients for {} in thread: {}", agentName, threadId);

			for (McpAssignNodeProperties.McpServerInfo serverInfo : config.mcpServers()) {
				if (!serverInfo.enabled()) {
					logger.debug("Skipping disabled MCP server: {} for {}", serverInfo.url(), agentName);
					continue;
				}

				// 为每个服务器动态创建transport
				List<NamedClientMcpTransport> namedTransports = McpConfigMergeUtil.createAgent2McpTransports(agentName,
						new McpAssignNodeProperties.McpServerConfig(List.of(serverInfo)), webClientBuilderTemplate,
						objectMapper);

				for (NamedClientMcpTransport namedTransport : namedTransports) {
					McpSchema.Implementation clientInfo = new McpSchema.Implementation(commonProperties.getName(),
							commonProperties.getVersion());

					McpClient.AsyncSpec spec = McpClient.async(namedTransport.transport()).clientInfo(clientInfo);
					spec = mcpAsyncClientConfigurer.configure(namedTransport.name(), spec);
					McpAsyncClient client = spec.build();

					// 初始化MCP客户端
					client.initialize().block(java.time.Duration.ofMinutes(2));

					mcpAsyncClients.add(client);
					logger.debug("Created MCP client for server: {} (agent: {})", serverInfo.url(), agentName);
				}
			}

			if (!mcpAsyncClients.isEmpty()) {
				logger.info("Successfully created {} MCP clients for {}", mcpAsyncClients.size(), agentName);
				return new AsyncMcpToolCallbackProvider(mcpAsyncClients);
			}

		}
		catch (Exception e) {
			logger.error("Failed to create MCP clients for {}", agentName, e);
		}

		return null;
	}

	/**
	 * 检查MCP配置是否可用
	 * @param mcpConfigProvider MCP配置提供者
	 * @param mcpAsyncClientConfigurer MCP异步客户端配置器
	 * @param commonProperties MCP客户端通用属性
	 * @param webClientBuilderTemplate WebClient构建器模板
	 * @param objectMapper JSON对象映射器
	 * @return true 如果所有必需的组件都可用
	 */
	public static boolean isMcpConfigurationAvailable(
			Function<OverAllState, Map<String, McpAssignNodeProperties.McpServerConfig>> mcpConfigProvider,
			McpAsyncClientConfigurer mcpAsyncClientConfigurer, McpClientCommonProperties commonProperties,
			WebClient.Builder webClientBuilderTemplate, ObjectMapper objectMapper) {

		return mcpConfigProvider != null && mcpAsyncClientConfigurer != null && commonProperties != null
				&& webClientBuilderTemplate != null && objectMapper != null;
	}

}

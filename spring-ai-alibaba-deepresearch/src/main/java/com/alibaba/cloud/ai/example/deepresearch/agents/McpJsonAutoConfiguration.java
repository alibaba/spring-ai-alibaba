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

package com.alibaba.cloud.ai.example.deepresearch.agents;

import com.alibaba.cloud.ai.example.deepresearch.config.McpJsonProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.mcp.client.autoconfigure.NamedClientMcpTransport;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP JSON自动配置类 从JSON文件读取MCP配置并创建相应的MCP客户端
 *
 * @author Makoto
 * @since 2025/6/14
 */
@Configuration
@ConditionalOnClass({ McpAsyncClient.class, WebFluxSseClientTransport.class })
@ConditionalOnProperty(prefix = "spring.ai.alibaba.deepresearch.mcp", name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties({ McpJsonProperties.class })
public class McpJsonAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(McpJsonAutoConfiguration.class);

	private final McpJsonProperties mcpJsonProperties;

	private final ResourceLoader resourceLoader;

	private final ObjectMapper objectMapper;

	public McpJsonAutoConfiguration(McpJsonProperties mcpJsonProperties, ResourceLoader resourceLoader,
			ObjectMapper objectMapper) {
		this.mcpJsonProperties = mcpJsonProperties;
		this.resourceLoader = resourceLoader;
		this.objectMapper = objectMapper;
	}

	/**
	 * 步骤1: 创建NamedClientMcpTransport列表
	 */
	@Bean
	public Map<String, List<NamedClientMcpTransport>> node2NamedClientMcpTransports() throws IOException {
		Map<String, List<NamedClientMcpTransport>> node2Transports = new HashMap<>();

		Resource configResource = resourceLoader.getResource(mcpJsonProperties.getConfigFile());
		if (!configResource.exists()) {
			logger.warn("MCP配置文件不存在: {}", mcpJsonProperties.getConfigFile());
			return node2Transports;
		}

		McpJsonProperties.McpJsonConfig config;
		try (InputStream inputStream = configResource.getInputStream()) {
			config = objectMapper.readValue(inputStream, McpJsonProperties.McpJsonConfig.class);
		}
		catch (Exception e) {
			logger.error("解析JSON配置文件失败", e);
			throw e;
		}

		Map<String, McpJsonProperties.AgentConfig> allAgentConfigs = config.getAllAgentConfigs();
		for (Map.Entry<String, McpJsonProperties.AgentConfig> entry : allAgentConfigs.entrySet()) {
			String agentName = entry.getKey();
			McpJsonProperties.AgentConfig agentConfig = entry.getValue();

			List<NamedClientMcpTransport> transports = new ArrayList<>();
			List<McpJsonProperties.McpServerConfig> enabledServers = agentConfig.getEnabledMcpServers();

			for (McpJsonProperties.McpServerConfig serverConfig : enabledServers) {
				String serverName = agentName + "_" + serverConfig.getUrl().hashCode();
				NamedClientMcpTransport transport = createNamedClientMcpTransport(serverName, serverConfig);
				if (transport != null) {
					transports.add(transport);
				}
			}

			if (!transports.isEmpty()) {
				node2Transports.put(agentName, transports);
			}
		}

		logger.info("从JSON配置创建了NamedClientMcpTransport: {}",
				node2Transports.entrySet()
					.stream()
					.collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().size())));
		return node2Transports;
	}

	/**
	 * 步骤2: 将NamedClientMcpTransport转换为McpAsyncClient
	 */
	@Bean
	public Map<String, List<McpAsyncClient>> node2McpAsyncClients(
			Map<String, List<NamedClientMcpTransport>> node2NamedClientMcpTransports) {
		Map<String, List<McpAsyncClient>> node2Clients = new HashMap<>();

		for (Map.Entry<String, List<NamedClientMcpTransport>> entry : node2NamedClientMcpTransports.entrySet()) {
			String nodeName = entry.getKey();
			List<NamedClientMcpTransport> transports = entry.getValue();

			List<McpAsyncClient> clients = new ArrayList<>();
			for (NamedClientMcpTransport namedTransport : transports) {
				try {
					McpAsyncClient client = McpClient.async(namedTransport.transport())
						.clientInfo(new McpSchema.Implementation(namedTransport.name(), "1.0.0"))
						.build();

					client.initialize().block(Duration.ofMinutes(2));
					clients.add(client);
					logger.info("MCP客户端初始化成功: {} -> {}", namedTransport.name(), nodeName);
				}
				catch (Exception e) {
					logger.error("创建MCP客户端失败: {} -> {}", namedTransport.name(), nodeName, e);
				}
			}

			if (!clients.isEmpty()) {
				node2Clients.put(nodeName, clients);
			}
		}

		return node2Clients;
	}

	/**
	 * 步骤3: 将McpAsyncClient转换为AsyncMcpToolCallbackProvider
	 */
	@Bean
	public Map<String, AsyncMcpToolCallbackProvider> node2AsyncMcpToolCallbackProvider(
			Map<String, List<McpAsyncClient>> node2McpAsyncClients) {
		Map<String, AsyncMcpToolCallbackProvider> node2Providers = new HashMap<>();

		for (Map.Entry<String, List<McpAsyncClient>> entry : node2McpAsyncClients.entrySet()) {
			String nodeName = entry.getKey();
			List<McpAsyncClient> clients = entry.getValue();

			if (!clients.isEmpty()) {
				// 这边是为了每个节点创建一个AsyncMcpToolCallbackProvider，需要注意每个节点的客户端可能不同
				AsyncMcpToolCallbackProvider provider = new AsyncMcpToolCallbackProvider(clients);
				node2Providers.put(nodeName, provider);
				logger.info("为节点 {} 创建了AsyncMcpToolCallbackProvider，包含 {} 个客户端", nodeName, clients.size());
			}
		}

		return node2Providers;
	}

	/**
	 * 创建NamedClientMcpTransport
	 */
	private NamedClientMcpTransport createNamedClientMcpTransport(String serverName,
			McpJsonProperties.McpServerConfig serverConfig) {
		try {
			String url = serverConfig.getUrl();
			String baseUrl = url.contains("?") ? url.substring(0, url.indexOf("?")) : url;
			String sseEndpoint = url.contains("?") ? "/sse" + url.substring(url.indexOf("?")) : "/sse";

			WebClient.Builder webClientBuilder = WebClient.builder()
				.baseUrl(baseUrl)
				.defaultHeader("Accept", "text/event-stream")
				.defaultHeader("Content-Type", "application/json")
				.defaultHeader("User-Agent", "MCP-Client/1.0.0");

			WebFluxSseClientTransport transport = new WebFluxSseClientTransport(webClientBuilder, objectMapper,
					sseEndpoint);

			return new NamedClientMcpTransport(serverName, transport);

		}
		catch (Exception e) {
			logger.error("创建NamedClientMcpTransport失败: {} -> {}", serverName, serverConfig.getUrl(), e);
			return null;
		}
	}

	/**
	 * 支持按代理名称获取工具回调的接口
	 */
	public interface AgentSpecificToolCallbackProvider extends ToolCallbackProvider {

		ToolCallback[] getToolCallbacksForAgent(String agentName);

	}

	/**
	 * 自定义的ToolCallbackProvider实现 基于JSON配置的MCP客户端，支持按代理名称获取工具回调
	 */
	@Bean
	public JsonBasedMcpToolCallbackProvider jsonBasedMcpToolCallbackProvider(
			Map<String, AsyncMcpToolCallbackProvider> node2AsyncMcpToolCallbackProvider) {
		return new JsonBasedMcpToolCallbackProvider(node2AsyncMcpToolCallbackProvider);
	}

	public static class JsonBasedMcpToolCallbackProvider implements AgentSpecificToolCallbackProvider {

		private final Map<String, AsyncMcpToolCallbackProvider> agentProviders;

		private static final Logger logger = LoggerFactory.getLogger(JsonBasedMcpToolCallbackProvider.class);

		public JsonBasedMcpToolCallbackProvider(Map<String, AsyncMcpToolCallbackProvider> agentProviders) {
			this.agentProviders = agentProviders;
			logger.info("JsonBasedMcpToolCallbackProvider创建完成，代理: {}", agentProviders.keySet());
		}

		@Override
		public ToolCallback[] getToolCallbacks() {
			List<ToolCallback> allCallbacks = new ArrayList<>();
			for (AsyncMcpToolCallbackProvider provider : agentProviders.values()) {
				ToolCallback[] callbacks = provider.getToolCallbacks();
				allCallbacks.addAll(List.of(callbacks));
			}
			return allCallbacks.toArray(new ToolCallback[0]);
		}

		@Override
		public ToolCallback[] getToolCallbacksForAgent(String agentName) {
			AsyncMcpToolCallbackProvider provider = agentProviders.get(agentName);
			if (provider != null) {
				return provider.getToolCallbacks();
			}
			else {
				return new ToolCallback[0];
			}
		}

	}

}

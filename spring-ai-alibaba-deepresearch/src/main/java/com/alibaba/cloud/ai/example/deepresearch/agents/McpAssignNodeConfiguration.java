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

import com.alibaba.cloud.ai.example.deepresearch.config.McpAssignNodeProperties;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.mcp.client.autoconfigure.NamedClientMcpTransport;
import org.springframework.ai.mcp.client.autoconfigure.configurer.McpAsyncClientConfigurer;
import org.springframework.ai.mcp.client.autoconfigure.configurer.McpSyncClientConfigurer;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 分配指定的mcp给指定的节点
 *
 * @author Makoto
 */
@ConditionalOnProperty(prefix = McpAssignNodeProperties.MCP_ASSIGN_NODE_PREFIX, name = "enabled", havingValue = "true")
@EnableConfigurationProperties({ McpAssignNodeProperties.class, McpClientCommonProperties.class })
@Configuration
public class McpAssignNodeConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(McpAssignNodeConfiguration.class);

	@Autowired
	private McpAssignNodeProperties mcpAssignNodeProperties;

	@Autowired
	private McpClientCommonProperties commonProperties;

	@Autowired
	private ResourceLoader resourceLoader;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private WebClient.Builder webClientBuilderTemplate;

	/**
	 * 读取JSON配置文件
	 */
	@Bean(name = "agent2mcpConfig")
	public Map<String, McpAssignNodeProperties.McpServerConfig> agent2mcpConfig() {
		try {
			Resource resource = resourceLoader.getResource(mcpAssignNodeProperties.getConfigLocation());
			if (!resource.exists()) {
				return new HashMap<>();
			}

			try (InputStream inputStream = resource.getInputStream()) {
				TypeReference<Map<String, McpAssignNodeProperties.McpServerConfig>> typeRef = new TypeReference<>() {
				};
				return objectMapper.readValue(inputStream, typeRef);
			}
		}
		catch (IOException e) {
			logger.error("读取MCP配置失败", e);
			return new HashMap<>();
		}
	}

	/**
	 * 支持运行时配置的MCP配置提供者
	 */
	@Bean(name = "agent2mcpConfigWithRuntime")
	public Function<OverAllState, Map<String, McpAssignNodeProperties.McpServerConfig>> agent2mcpConfigWithRuntime(
			@Qualifier("agent2mcpConfig") Map<String, McpAssignNodeProperties.McpServerConfig> staticConfig) {

		return state -> {
			// 获取运行时MCP设置
			Map<String, Object> runtimeMcpSettings = state.value("mcp_settings", Map.class)
				.orElse(Collections.emptyMap());
			return mergeAgent2McpConfigs(staticConfig, runtimeMcpSettings);
		};
	}

	/**
	 * 合并静态配置和动态配置
	 */
	private Map<String, McpAssignNodeProperties.McpServerConfig> mergeAgent2McpConfigs(
			Map<String, McpAssignNodeProperties.McpServerConfig> staticConfig, Map<String, Object> runtimeSettings) {

		Map<String, McpAssignNodeProperties.McpServerConfig> result = new HashMap<>();

		// 这边复制所有静态配置
		for (Map.Entry<String, McpAssignNodeProperties.McpServerConfig> entry : staticConfig.entrySet()) {
			String agentName = entry.getKey();
			List<McpAssignNodeProperties.McpServerInfo> staticServers = new ArrayList<>(entry.getValue().mcpServers());
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
	private List<McpAssignNodeProperties.McpServerInfo> mergeAgent2McpServers(
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
	private List<NamedClientMcpTransport> createAgent2McpTransports(String agentName,
			McpAssignNodeProperties.McpServerConfig config) {
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

	/**
	 * 创建按代理分组的AsyncMcpToolCallbackProvider Map（动态配置）
	 */
	@Bean(name = "dynamicAgent2AsyncMcpToolCallbackProvider")
	@ConditionalOnProperty(prefix = "spring.ai.mcp.client", name = { "type" }, havingValue = "ASYNC")
	public Function<OverAllState, Map<String, AsyncMcpToolCallbackProvider>> dynamicAgent2AsyncMcpToolCallbackProvider(
			ObjectProvider<McpAsyncClientConfigurer> mcpAsyncClientConfigurerProvider,
			@Qualifier("agent2mcpConfigWithRuntime") Function<OverAllState, Map<String, McpAssignNodeProperties.McpServerConfig>> configProvider) {

		return state -> {
			Map<String, AsyncMcpToolCallbackProvider> providerMap = new HashMap<>();
			McpAsyncClientConfigurer mcpAsyncClientConfigurer = mcpAsyncClientConfigurerProvider.getObject();

			// 获取运行时配置（静态配置已合并）
			Map<String, McpAssignNodeProperties.McpServerConfig> mcpAgentConfigs = configProvider.apply(state);
			String threadId = state.value("thread_id", "__default__");

			logger.debug("Creating MCP clients for thread: {}", threadId);

			for (Map.Entry<String, McpAssignNodeProperties.McpServerConfig> entry : mcpAgentConfigs.entrySet()) {
				String agentName = entry.getKey();
				McpAssignNodeProperties.McpServerConfig config = entry.getValue();

				List<McpAsyncClient> mcpAsyncClients = new ArrayList<>();

				for (McpAssignNodeProperties.McpServerInfo serverInfo : config.mcpServers()) {
					if (!serverInfo.enabled()) {
						logger.debug("Skipping disabled MCP server: {} for agent: {}", serverInfo.url(), agentName);
						continue;
					}

					// 为每个服务器动态创建transport
					List<NamedClientMcpTransport> namedTransports = createAgent2McpTransports(agentName,
							new McpAssignNodeProperties.McpServerConfig(List.of(serverInfo)));

					for (NamedClientMcpTransport namedTransport : namedTransports) {
						McpSchema.Implementation clientInfo = new McpSchema.Implementation(commonProperties.getName(),
								commonProperties.getVersion());

						McpClient.AsyncSpec spec = McpClient.async(namedTransport.transport()).clientInfo(clientInfo);
						spec = mcpAsyncClientConfigurer.configure(namedTransport.name(), spec);
						McpAsyncClient client = spec.build();
						mcpAsyncClients.add(client);
					}
				}

				if (!mcpAsyncClients.isEmpty()) {
					providerMap.put(agentName, new AsyncMcpToolCallbackProvider(mcpAsyncClients));
				}
			}

			return providerMap;
		};
	}

	/**
	 * 静态MCP配置的AsyncMcpToolCallbackProvider Map（用于启动时Agent初始化）
	 */
	@Bean(name = { "staticAgent2AsyncMcpToolCallbackProvider", "agent2AsyncMcpToolCallbackProvider" })
	@ConditionalOnProperty(prefix = "spring.ai.mcp.client", name = { "type" }, havingValue = "ASYNC")
	public Map<String, AsyncMcpToolCallbackProvider> staticAgent2AsyncMcpToolCallbackProvider(
			ObjectProvider<McpAsyncClientConfigurer> mcpAsyncClientConfigurerProvider,
			@Qualifier("agent2mcpConfig") Map<String, McpAssignNodeProperties.McpServerConfig> staticConfig) {

		Map<String, AsyncMcpToolCallbackProvider> providerMap = new HashMap<>();
		McpAsyncClientConfigurer mcpAsyncClientConfigurer = mcpAsyncClientConfigurerProvider.getObject();

		logger.debug("Creating static MCP clients for application startup");

		for (Map.Entry<String, McpAssignNodeProperties.McpServerConfig> entry : staticConfig.entrySet()) {
			String agentName = entry.getKey();
			McpAssignNodeProperties.McpServerConfig config = entry.getValue();

			List<McpAsyncClient> mcpAsyncClients = new ArrayList<>();

			for (McpAssignNodeProperties.McpServerInfo serverInfo : config.mcpServers()) {
				if (!serverInfo.enabled()) {
					logger.debug("Skipping disabled static MCP server: {} for agent: {}", serverInfo.url(), agentName);
					continue;
				}

				// 为每个服务器动态创建transport
				List<NamedClientMcpTransport> namedTransports = createAgent2McpTransports(agentName,
						new McpAssignNodeProperties.McpServerConfig(List.of(serverInfo)));

				for (NamedClientMcpTransport namedTransport : namedTransports) {
					McpSchema.Implementation clientInfo = new McpSchema.Implementation(commonProperties.getName(),
							commonProperties.getVersion());

					McpClient.AsyncSpec spec = McpClient.async(namedTransport.transport()).clientInfo(clientInfo);
					spec = mcpAsyncClientConfigurer.configure(namedTransport.name(), spec);
					McpAsyncClient client = spec.build();
					mcpAsyncClients.add(client);
				}
			}

			if (!mcpAsyncClients.isEmpty()) {
				providerMap.put(agentName, new AsyncMcpToolCallbackProvider(mcpAsyncClients));
			}
		}

		return providerMap;
	}

	/**
	 * 创建按代理分组的SyncMcpToolCallbackProvider Map（动态配置）
	 */
	@Bean(name = "dynamicAgent2SyncMcpToolCallbackProvider")
	@ConditionalOnProperty(prefix = "spring.ai.mcp.client", name = { "type" }, havingValue = "SYNC")
	public Function<OverAllState, Map<String, SyncMcpToolCallbackProvider>> dynamicAgent2SyncMcpToolCallbackProvider(
			ObjectProvider<McpSyncClientConfigurer> mcpSyncClientConfigurerProvider,
			@Qualifier("agent2mcpConfigWithRuntime") Function<OverAllState, Map<String, McpAssignNodeProperties.McpServerConfig>> configProvider) {

		return state -> {
			Map<String, SyncMcpToolCallbackProvider> providerMap = new HashMap<>();
			McpSyncClientConfigurer mcpSyncClientConfigurer = mcpSyncClientConfigurerProvider.getObject();

			// 获取运行时配置（静态配置已合并）
			Map<String, McpAssignNodeProperties.McpServerConfig> mcpAgentConfigs = configProvider.apply(state);
			String threadId = state.value("thread_id", "__default__");

			logger.debug("Creating MCP sync clients for thread: {}", threadId);

			for (Map.Entry<String, McpAssignNodeProperties.McpServerConfig> entry : mcpAgentConfigs.entrySet()) {
				String agentName = entry.getKey();
				McpAssignNodeProperties.McpServerConfig config = entry.getValue();

				List<McpSyncClient> mcpSyncClients = new ArrayList<>();

				for (McpAssignNodeProperties.McpServerInfo serverInfo : config.mcpServers()) {
					if (!serverInfo.enabled()) {
						logger.debug("Skipping disabled MCP server: {} for agent: {}", serverInfo.url(), agentName);
						continue;
					}

					// 为每个服务器动态创建transport
					List<NamedClientMcpTransport> namedTransports = createAgent2McpTransports(agentName,
							new McpAssignNodeProperties.McpServerConfig(List.of(serverInfo)));

					for (NamedClientMcpTransport namedTransport : namedTransports) {
						McpSchema.Implementation clientInfo = new McpSchema.Implementation(commonProperties.getName(),
								commonProperties.getVersion());

						McpClient.SyncSpec spec = McpClient.sync(namedTransport.transport()).clientInfo(clientInfo);
						spec = mcpSyncClientConfigurer.configure(namedTransport.name(), spec);
						McpSyncClient client = spec.build();
						mcpSyncClients.add(client);
					}
				}

				if (!mcpSyncClients.isEmpty()) {
					providerMap.put(agentName, new SyncMcpToolCallbackProvider(mcpSyncClients));
				}
			}

			return providerMap;
		};
	}

	/**
	 * 静态MCP配置的SyncMcpToolCallbackProvider Map（用于启动时Agent初始化）
	 */
	@Bean(name = { "staticAgent2SyncMcpToolCallbackProvider", "agent2SyncMcpToolCallbackProvider" })
	@ConditionalOnProperty(prefix = "spring.ai.mcp.client", name = { "type" }, havingValue = "SYNC")
	public Map<String, SyncMcpToolCallbackProvider> staticAgent2SyncMcpToolCallbackProvider(
			ObjectProvider<McpSyncClientConfigurer> mcpSyncClientConfigurerProvider,
			@Qualifier("agent2mcpConfig") Map<String, McpAssignNodeProperties.McpServerConfig> staticConfig) {

		Map<String, SyncMcpToolCallbackProvider> providerMap = new HashMap<>();
		McpSyncClientConfigurer mcpSyncClientConfigurer = mcpSyncClientConfigurerProvider.getObject();

		logger.debug("Creating static MCP sync clients for application startup");

		for (Map.Entry<String, McpAssignNodeProperties.McpServerConfig> entry : staticConfig.entrySet()) {
			String agentName = entry.getKey();
			McpAssignNodeProperties.McpServerConfig config = entry.getValue();

			List<McpSyncClient> mcpSyncClients = new ArrayList<>();

			for (McpAssignNodeProperties.McpServerInfo serverInfo : config.mcpServers()) {
				if (!serverInfo.enabled()) {
					logger.debug("Skipping disabled static MCP server: {} for agent: {}", serverInfo.url(), agentName);
					continue;
				}

				// 为每个服务器动态创建transport
				List<NamedClientMcpTransport> namedTransports = createAgent2McpTransports(agentName,
						new McpAssignNodeProperties.McpServerConfig(List.of(serverInfo)));

				for (NamedClientMcpTransport namedTransport : namedTransports) {
					McpSchema.Implementation clientInfo = new McpSchema.Implementation(commonProperties.getName(),
							commonProperties.getVersion());

					McpClient.SyncSpec spec = McpClient.sync(namedTransport.transport()).clientInfo(clientInfo);
					spec = mcpSyncClientConfigurer.configure(namedTransport.name(), spec);
					McpSyncClient client = spec.build();
					mcpSyncClients.add(client);
				}
			}

			if (!mcpSyncClients.isEmpty()) {
				providerMap.put(agentName, new SyncMcpToolCallbackProvider(mcpSyncClients));
			}
		}

		return providerMap;
	}

	private String connectedClientName(String clientName, String serverConnectionName) {
		return clientName + " - " + serverConnectionName;
	}

}

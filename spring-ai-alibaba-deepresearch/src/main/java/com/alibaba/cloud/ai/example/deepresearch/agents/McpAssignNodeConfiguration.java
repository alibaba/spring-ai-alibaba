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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	 * agent对应的的MCP传输列表
	 */
	@Bean(name = "agent2Transports")
	public Map<String, List<NamedClientMcpTransport>> agent2Transports(
			@Qualifier("agent2mcpConfig") Map<String, McpAssignNodeProperties.McpServerConfig> mcpAgentConfigs) {
		Map<String, List<NamedClientMcpTransport>> agent2Transports = new HashMap<>();

		for (Map.Entry<String, McpAssignNodeProperties.McpServerConfig> entry : mcpAgentConfigs.entrySet()) {
			String agentName = entry.getKey();
			McpAssignNodeProperties.McpServerConfig config = entry.getValue();

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
			agent2Transports.put(agentName, transports);
		}
		return agent2Transports;
	}

	/**
	 * 创建按代理分组的AsyncMcpToolCallbackProvider Map
	 */
	@Bean(name = "agent2AsyncMcpToolCallbackProvider")
	@ConditionalOnProperty(prefix = "spring.ai.mcp.client", name = { "type" }, havingValue = "ASYNC")
	public Map<String, AsyncMcpToolCallbackProvider> agent2AsyncMcpToolCallbackProvider(
			@Qualifier("agent2Transports") Map<String, List<NamedClientMcpTransport>> agent2Transports,
			ObjectProvider<McpAsyncClientConfigurer> mcpAsyncClientConfigurerProvider) {
		Map<String, AsyncMcpToolCallbackProvider> providerMap = new HashMap<>();
		McpAsyncClientConfigurer mcpAsyncClientConfigurer = mcpAsyncClientConfigurerProvider.getObject();

		agent2Transports.forEach((agentName, transports) -> {
			List<McpAsyncClient> mcpAsyncClients = new ArrayList();

			for (NamedClientMcpTransport namedTransport : transports) {
				McpSchema.Implementation clientInfo = new McpSchema.Implementation(
						this.connectedClientName(commonProperties.getName(), namedTransport.name()),
						commonProperties.getVersion());
				McpClient.AsyncSpec spec = McpClient.async(namedTransport.transport())
					.clientInfo(clientInfo)
					.requestTimeout(commonProperties.getRequestTimeout());
				spec = mcpAsyncClientConfigurer.configure(namedTransport.name(), spec);
				McpAsyncClient client = spec.build();
				if (commonProperties.isInitialized()) {
					client.initialize().block();
				}

				mcpAsyncClients.add(client);
			}
			providerMap.put(agentName, new AsyncMcpToolCallbackProvider(mcpAsyncClients));
		});
		return providerMap;
	}

	/**
	 * 创建按代理分组的SyncMcpToolCallbackProvider Map
	 */
	@Bean(name = "agent2SyncMcpToolCallbackProvider")
	@ConditionalOnProperty(prefix = "spring.ai.mcp.client", name = { "type" }, havingValue = "SYNC")
	public Map<String, SyncMcpToolCallbackProvider> agent2SyncMcpToolCallbackProvider(
			@Qualifier("agent2Transports") Map<String, List<NamedClientMcpTransport>> agent2Transports,
			ObjectProvider<McpSyncClientConfigurer> mcpSyncClientConfigurerProvider) {
		Map<String, SyncMcpToolCallbackProvider> providerMap = new HashMap<>();
		McpSyncClientConfigurer mcpSyncClientConfigurer = mcpSyncClientConfigurerProvider.getObject();

		agent2Transports.forEach((agentName, transports) -> {
			List<McpSyncClient> mcpSyncClients = new ArrayList();

			for (NamedClientMcpTransport namedTransport : transports) {
				McpSchema.Implementation clientInfo = new McpSchema.Implementation(
						this.connectedClientName(commonProperties.getName(), namedTransport.name()),
						commonProperties.getVersion());
				McpClient.SyncSpec spec = McpClient.sync(namedTransport.transport())
					.clientInfo(clientInfo)
					.requestTimeout(commonProperties.getRequestTimeout());
				spec = mcpSyncClientConfigurer.configure(namedTransport.name(), spec);
				McpSyncClient client = spec.build();
				if (commonProperties.isInitialized()) {
					client.initialize();
				}

				mcpSyncClients.add(client);
			}
			providerMap.put(agentName, new SyncMcpToolCallbackProvider(mcpSyncClients));
		});
		return providerMap;
	}

	private String connectedClientName(String clientName, String serverConnectionName) {
		return clientName + " - " + serverConnectionName;
	}

}

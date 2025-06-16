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
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.client.autoconfigure.NamedClientMcpTransport;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 按代理分配MCP节点的自动配置类
 *
 * @author Makoto
 */
@Configuration
@ConditionalOnClass({ McpAsyncClient.class, WebFluxSseClientTransport.class })
@ConditionalOnProperty(prefix = "spring.ai.alibaba.deepresearch.mcp", name = "enabled", havingValue = "true",
		matchIfMissing = false)
@EnableConfigurationProperties({ McpAssignNodeProperties.class })
public class McpAssignNodeAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(McpAssignNodeAutoConfiguration.class);

	private final McpAssignNodeProperties mcpAssignNodeProperties;

	private final ResourceLoader resourceLoader;

	private final ObjectMapper objectMapper;

	public McpAssignNodeAutoConfiguration(McpAssignNodeProperties mcpAssignNodeProperties,
			ResourceLoader resourceLoader, ObjectMapper objectMapper) {
		this.mcpAssignNodeProperties = mcpAssignNodeProperties;
		this.resourceLoader = resourceLoader;
		this.objectMapper = objectMapper;
	}

	/**
	 * 读取JSON配置文件
	 */
	@Bean
	public Map<String, McpAssignNodeProperties.McpServerConfig> mcpAgentConfigs() {
		try {
			Resource resource = resourceLoader.getResource(mcpAssignNodeProperties.getConfigLocation());
			if (!resource.exists()) {
				logger.warn("MCP配置文件不存在: {}", mcpAssignNodeProperties.getConfigLocation());
				return new HashMap<>();
			}

			try (InputStream inputStream = resource.getInputStream()) {
				TypeReference<Map<String, McpAssignNodeProperties.McpServerConfig>> typeRef = new TypeReference<Map<String, McpAssignNodeProperties.McpServerConfig>>() {
				};
				Map<String, McpAssignNodeProperties.McpServerConfig> configs = objectMapper.readValue(inputStream,
						typeRef);
				logger.info("成功加载MCP代理配置，代理数量: {}", configs.size());
				return configs;
			}
		}
		catch (IOException e) {
			logger.error("读取MCP配置文件失败: {}", mcpAssignNodeProperties.getConfigLocation(), e);
			return new HashMap<>();
		}
	}

	/**
	 * 创建NamedClientMcpTransport列表
	 */
	@Bean
	public List<NamedClientMcpTransport> mcpAssignNodeTransports(
			Map<String, McpAssignNodeProperties.McpServerConfig> mcpAgentConfigs) {

		List<NamedClientMcpTransport> transports = new ArrayList<>();

		for (Map.Entry<String, McpAssignNodeProperties.McpServerConfig> entry : mcpAgentConfigs.entrySet()) {
			String agentName = entry.getKey();
			McpAssignNodeProperties.McpServerConfig config = entry.getValue();

			try {
				for (McpAssignNodeProperties.McpServerInfo serverInfo : config.getMcpServers()) {
					if (!serverInfo.isEnabled()) {
						logger.debug("跳过禁用的MCP服务器: {} for agent: {}", serverInfo.getUrl(), agentName);
						continue;
					}

					WebClient.Builder webClientBuilder = WebClient.builder()
						.baseUrl(serverInfo.getUrl())
						.defaultHeader("Accept", "text/event-stream")
						.defaultHeader("Content-Type", "application/json");

					WebFluxSseClientTransport transport = new WebFluxSseClientTransport(webClientBuilder, objectMapper);

					String transportName = agentName + "-" + serverInfo.getUrl().hashCode();
					transports.add(new NamedClientMcpTransport(transportName, transport));

					logger.info("创建MCP传输: {} for agent: {}, server: {}", transportName, agentName, serverInfo.getUrl());
				}
			}
			catch (Exception e) {
				logger.error("创建MCP传输失败 for agent: {}", agentName, e);
			}
		}

		logger.info("总共创建了 {} 个MCP传输", transports.size());
		return transports;
	}

}

/*
 * Copyright 2024-2025 the original author or authors.
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

package com.alibaba.cloud.ai.mcp.nacos.client;

import com.alibaba.cloud.ai.mcp.nacos.client.transport.LoadbalancedMcpAsyncClient;
import com.alibaba.cloud.ai.mcp.nacos.client.transport.LoadbalancedMcpSyncClient;
import com.alibaba.nacos.api.naming.NamingService;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.client.autoconfigure.McpClientAutoConfiguration;
import org.springframework.ai.mcp.client.autoconfigure.NamedClientMcpTransport;
import org.springframework.ai.mcp.client.autoconfigure.configurer.McpAsyncClientConfigurer;
import org.springframework.ai.mcp.client.autoconfigure.configurer.McpSyncClientConfigurer;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author yingzi
 * @date 2025/4/29:10:05
 */
@AutoConfiguration(after = { NacosMcpSseClientAutoConfiguration.class, McpClientAutoConfiguration.class })
@ConditionalOnClass({ McpSchema.class })
@EnableConfigurationProperties({ McpClientCommonProperties.class })
@ConditionalOnProperty(prefix = "spring.ai.mcp.client", name = { "nacos-enabled" }, havingValue = "true",
		matchIfMissing = false)
public class NacosMcpClientAutoConfiguration {

	public NacosMcpClientAutoConfiguration() {
	}

	private String connectedClientName(String clientName, String serverConnectionName) {
		return clientName + " - " + serverConnectionName;
	}

	@Bean
	@ConditionalOnProperty(prefix = "spring.ai.mcp.client", name = { "type" }, havingValue = "SYNC",
			matchIfMissing = true)
	public List<LoadbalancedMcpSyncClient> loadbalancedMcpSyncClientList(
			ObjectProvider<McpSyncClientConfigurer> mcpSyncClientConfigurerProvider,
			McpClientCommonProperties commonProperties,
			@Qualifier("server2NamedTransport") ObjectProvider<Map<String, List<NamedClientMcpTransport>>> server2NamedTransportProvider,
			ObjectProvider<NamingService> namingServiceProvider) {
		NamingService namingService = namingServiceProvider.getObject();
		McpSyncClientConfigurer mcpSyncClientConfigurer = mcpSyncClientConfigurerProvider.getObject();

		List<LoadbalancedMcpSyncClient> loadbalancedMcpSyncClients = new ArrayList<>();
		Map<String, List<NamedClientMcpTransport>> server2NamedTransport = server2NamedTransportProvider.getObject();
		for (Map.Entry<String, List<NamedClientMcpTransport>> entry : server2NamedTransport.entrySet()) {
			String serviceName = entry.getKey();
			List<NamedClientMcpTransport> namedTransports = entry.getValue();
			List<McpSyncClient> mcpSyncClients = new ArrayList<>();

			McpSyncClient syncClient;
			for (NamedClientMcpTransport namedTransport : namedTransports) {
				McpSchema.Implementation clientInfo = new McpSchema.Implementation(
						this.connectedClientName(commonProperties.getName(), namedTransport.name()),
						commonProperties.getVersion());
				McpClient.SyncSpec syncSpec = McpClient.sync(namedTransport.transport())
					.clientInfo(clientInfo)
					.requestTimeout(commonProperties.getRequestTimeout());
				syncSpec = mcpSyncClientConfigurer.configure(namedTransport.name(), syncSpec);
				syncClient = syncSpec.build();
				if (commonProperties.isInitialized()) {
					syncClient.initialize();
				}

				mcpSyncClients.add(syncClient);
			}

			loadbalancedMcpSyncClients.add(LoadbalancedMcpSyncClient.builder()
				.serviceName(serviceName)
				.mcpSyncClientList(mcpSyncClients)
				.namingService(namingService)
				.build());
		}

		return loadbalancedMcpSyncClients;

	}

	@Bean
	@ConditionalOnProperty(prefix = "spring.ai.mcp.client", name = { "type" }, havingValue = "ASYNC")
	public List<LoadbalancedMcpAsyncClient> loadbalancedMcpAsyncClientList(
			ObjectProvider<McpAsyncClientConfigurer> mcpAsyncClientConfigurerProvider,
			McpClientCommonProperties commonProperties,
			@Qualifier("server2NamedTransport") ObjectProvider<Map<String, List<NamedClientMcpTransport>>> server2NamedTransportProvider,
			ObjectProvider<NamingService> namingServiceProvider) {
		NamingService namingService = namingServiceProvider.getObject();
		McpAsyncClientConfigurer mcpAsyncClientConfigurer = mcpAsyncClientConfigurerProvider.getObject();

		List<LoadbalancedMcpAsyncClient> loadbalancedMcpAsyncClients = new ArrayList<>();
		Map<String, List<NamedClientMcpTransport>> server2NamedTransport = server2NamedTransportProvider.getObject();
		for (Map.Entry<String, List<NamedClientMcpTransport>> entry : server2NamedTransport.entrySet()) {
			String serviceName = entry.getKey();
			List<NamedClientMcpTransport> namedTransports = entry.getValue();
			List<McpAsyncClient> mcpAsyncClients = new ArrayList<>();

			McpAsyncClient syncClient;
			for (NamedClientMcpTransport namedTransport : namedTransports) {
				McpSchema.Implementation clientInfo = new McpSchema.Implementation(
						this.connectedClientName(commonProperties.getName(), namedTransport.name()),
						commonProperties.getVersion());
				McpClient.AsyncSpec syncSpec = McpClient.async(namedTransport.transport())
					.clientInfo(clientInfo)
					.requestTimeout(commonProperties.getRequestTimeout());
				syncSpec = mcpAsyncClientConfigurer.configure(namedTransport.name(), syncSpec);
				syncClient = syncSpec.build();
				if (commonProperties.isInitialized()) {
					syncClient.initialize().block();
				}

				mcpAsyncClients.add(syncClient);
			}

			loadbalancedMcpAsyncClients.add(LoadbalancedMcpAsyncClient.builder()
				.serviceName(serviceName)
				.mcpAsyncClientList(mcpAsyncClients)
				.namingService(namingService)
				.build());
		}
		return loadbalancedMcpAsyncClients;
	}

}

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

package com.alibaba.cloud.ai.autoconfigure.mcp.client;

import com.alibaba.cloud.ai.mcp.nacos2.client.transport.LoadbalancedMcpAsyncClient;
import com.alibaba.cloud.ai.mcp.nacos2.client.transport.LoadbalancedMcpSyncClient;
import com.alibaba.cloud.ai.mcp.nacos2.registry.NacosMcpRegistryProperties;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.client.config.NacosConfigService;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.client.autoconfigure.McpClientAutoConfiguration;
import org.springframework.ai.mcp.client.autoconfigure.NamedClientMcpTransport;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author yingzi
 * @since 2025/4/29:10:05
 */
@AutoConfiguration(after = { Nacos2McpSseClientAutoConfiguration.class, McpClientAutoConfiguration.class })
@ConditionalOnClass({ McpSchema.class })
@EnableConfigurationProperties({ McpClientCommonProperties.class, NacosMcpRegistryProperties.class })
@ConditionalOnProperty(prefix = "spring.ai.mcp.client", name = { "nacos-enabled" }, havingValue = "true",
		matchIfMissing = false)
public class Nacos2McpClientAutoConfiguration {

	public Nacos2McpClientAutoConfiguration() {
	}

	@Bean
	@ConditionalOnProperty(prefix = "spring.ai.mcp.client", name = { "type" }, havingValue = "SYNC",
			matchIfMissing = true)
	public List<LoadbalancedMcpSyncClient> loadbalancedMcpSyncClientList(
			@Qualifier("server2NamedTransport") ObjectProvider<Map<String, List<NamedClientMcpTransport>>> server2NamedTransportProvider,
			ObjectProvider<NamingService> namingServiceProvider,
			ObjectProvider<NacosConfigService> nacosConfigServiceProvider,
			NacosMcpRegistryProperties nacosMcpRegistryProperties, ApplicationContext applicationContext) {
		NamingService namingService = namingServiceProvider.getObject();
		NacosConfigService nacosConfigService = nacosConfigServiceProvider.getObject();

		List<LoadbalancedMcpSyncClient> loadbalancedMcpSyncClients = new ArrayList<>();
		Map<String, List<NamedClientMcpTransport>> server2NamedTransport = server2NamedTransportProvider.getObject();
		for (Map.Entry<String, List<NamedClientMcpTransport>> entry : server2NamedTransport.entrySet()) {
			String serviceName = entry.getKey();

			LoadbalancedMcpSyncClient loadbalancedMcpSyncClient = LoadbalancedMcpSyncClient.builder()
				.serviceName(serviceName)
				.serviceGroup(nacosMcpRegistryProperties.getServiceGroup())
				.namingService(namingService)
				.nacosConfigService(nacosConfigService)
				.applicationContext(applicationContext)
				.build();
			loadbalancedMcpSyncClient.init();
			loadbalancedMcpSyncClient.subscribe();

			loadbalancedMcpSyncClients.add(loadbalancedMcpSyncClient);
		}
		return loadbalancedMcpSyncClients;
	}

	@Bean
	@ConditionalOnProperty(prefix = "spring.ai.mcp.client", name = { "type" }, havingValue = "ASYNC")
	public List<LoadbalancedMcpAsyncClient> loadbalancedMcpAsyncClientList(
			@Qualifier("server2NamedTransport") ObjectProvider<Map<String, List<NamedClientMcpTransport>>> server2NamedTransportProvider,
			ObjectProvider<NamingService> namingServiceProvider,
			ObjectProvider<NacosConfigService> nacosConfigServiceProvider,
			NacosMcpRegistryProperties nacosMcpRegistryProperties, ApplicationContext applicationContext) {
		NamingService namingService = namingServiceProvider.getObject();
		NacosConfigService nacosConfigService = nacosConfigServiceProvider.getObject();

		List<LoadbalancedMcpAsyncClient> loadbalancedMcpAsyncClients = new ArrayList<>();
		Map<String, List<NamedClientMcpTransport>> server2NamedTransport = server2NamedTransportProvider.getObject();
		for (Map.Entry<String, List<NamedClientMcpTransport>> entry : server2NamedTransport.entrySet()) {
			String serviceName = entry.getKey();

			LoadbalancedMcpAsyncClient loadbalancedMcpAsyncClient = LoadbalancedMcpAsyncClient.builder()
				.serviceName(serviceName)
				.serviceGroup(nacosMcpRegistryProperties.getServiceGroup())
				.namingService(namingService)
				.nacosConfigService(nacosConfigService)
				.applicationContext(applicationContext)
				.build();
			loadbalancedMcpAsyncClient.init();
			loadbalancedMcpAsyncClient.subscribe();

			loadbalancedMcpAsyncClients.add(loadbalancedMcpAsyncClient);
		}
		return loadbalancedMcpAsyncClients;
	}

}

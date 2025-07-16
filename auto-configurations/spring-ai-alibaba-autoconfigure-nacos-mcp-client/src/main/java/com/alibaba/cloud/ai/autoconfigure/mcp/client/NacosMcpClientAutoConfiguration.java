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

import com.alibaba.cloud.ai.mcp.nacos.NacosMcpProperties;
import com.alibaba.cloud.ai.mcp.nacos.client.transport.LoadbalancedMcpAsyncClient;
import com.alibaba.cloud.ai.mcp.nacos.client.transport.LoadbalancedMcpSyncClient;
import com.alibaba.cloud.ai.mcp.nacos.service.NacosMcpOperationService;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.client.autoconfigure.McpClientAutoConfiguration;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yingzi
 * @since 2025/4/29:10:05
 */
@AutoConfiguration(after = { NacosMcpAutoConfiguration.class, McpClientAutoConfiguration.class })
@ConditionalOnClass({ McpSchema.class })
@EnableConfigurationProperties({ McpClientCommonProperties.class, NacosMcpProperties.class,
		NacosMcpSseClientProperties.class })
@ConditionalOnProperty(prefix = "spring.ai.alibaba.mcp.nacos.client", name = { "enabled" }, havingValue = "true",
		matchIfMissing = false)
public class NacosMcpClientAutoConfiguration {

	public NacosMcpClientAutoConfiguration() {
	}

	@Bean
	@ConditionalOnProperty(prefix = "spring.ai.mcp.client", name = { "type" }, havingValue = "SYNC",
			matchIfMissing = true)
	public List<LoadbalancedMcpSyncClient> loadbalancedMcpSyncClientList(
			ObjectProvider<NacosMcpOperationService> nacosMcpOperationServiceProvicer,
			NacosMcpSseClientProperties nacosMcpSseClientProperties, ApplicationContext applicationContext) {
		NacosMcpOperationService nacosMcpOperationService = nacosMcpOperationServiceProvicer.getObject();

		List<LoadbalancedMcpSyncClient> loadbalancedMcpSyncClients = new ArrayList<>();
		for (NacosMcpSseClientProperties.NacosSseParameters nacosSseParameters : nacosMcpSseClientProperties
			.getConnections()
			.values()) {
			LoadbalancedMcpSyncClient loadbalancedMcpSyncClient = LoadbalancedMcpSyncClient.builder()
				.serverName(nacosSseParameters.serviceName())
				.version(nacosSseParameters.version())
				.nacosMcpOperationService(nacosMcpOperationService)
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
			ObjectProvider<NacosMcpOperationService> nacosMcpOperationServiceProvicer,
			NacosMcpSseClientProperties nacosMcpSseClientProperties, ApplicationContext applicationContext) {
		NacosMcpOperationService nacosMcpOperationService = nacosMcpOperationServiceProvicer.getObject();

		List<LoadbalancedMcpAsyncClient> loadbalancedMcpAsyncClients = new ArrayList<>();
		for (NacosMcpSseClientProperties.NacosSseParameters nacosSseParameters : nacosMcpSseClientProperties
			.getConnections()
			.values()) {

			LoadbalancedMcpAsyncClient loadbalancedMcpAsyncClient = LoadbalancedMcpAsyncClient.builder()
				.serverName(nacosSseParameters.serviceName())
				.version(nacosSseParameters.version())
				.nacosMcpOperationService(nacosMcpOperationService)
				.applicationContext(applicationContext)
				.build();
			loadbalancedMcpAsyncClient.init();
			loadbalancedMcpAsyncClient.subscribe();

			loadbalancedMcpAsyncClients.add(loadbalancedMcpAsyncClient);
		}
		return loadbalancedMcpAsyncClients;
	}

}

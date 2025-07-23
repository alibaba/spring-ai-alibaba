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

package com.alibaba.cloud.ai.autoconfigure.mcp.gateway.nacos;

import com.alibaba.cloud.ai.autoconfigure.mcp.gateway.McpGatewayAutoConfiguration;
import com.alibaba.cloud.ai.mcp.gateway.core.McpGatewayToolManager;
import com.alibaba.cloud.ai.mcp.gateway.core.McpGatewayToolsInitializer;
import com.alibaba.cloud.ai.mcp.gateway.nacos.properties.NacosMcpGatewayProperties;
import com.alibaba.cloud.ai.mcp.gateway.nacos.provider.NacosMcpAsyncGatewayToolsProvider;
import com.alibaba.cloud.ai.mcp.gateway.nacos.provider.NacosMcpSyncGatewayToolsProvider;
import com.alibaba.cloud.ai.mcp.gateway.nacos.tools.NacosMcpGatewayToolsInitializer;
import com.alibaba.cloud.ai.mcp.gateway.nacos.watcher.NacosMcpGatewayToolsWatcher;
import com.alibaba.cloud.ai.mcp.nacos.NacosMcpProperties;
import com.alibaba.cloud.ai.mcp.nacos.service.NacosMcpOperationService;
import com.alibaba.nacos.api.exception.NacosException;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.server.autoconfigure.McpServerAutoConfiguration;
import org.springframework.ai.mcp.server.autoconfigure.McpServerProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Properties;

/**
 * @author aias00
 */
@EnableConfigurationProperties({ NacosMcpGatewayProperties.class, NacosMcpProperties.class, McpServerProperties.class })
@AutoConfiguration(after = { McpServerAutoConfiguration.class, McpGatewayAutoConfiguration.class })
@ConditionalOnProperty(prefix = "spring.ai.alibaba.mcp.gateway", name = "registry", havingValue = "nacos",
		matchIfMissing = true)
public class NacosMcpGatewayAutoConfiguration {

	private static final Logger log = LoggerFactory.getLogger(NacosMcpGatewayAutoConfiguration.class);

	@Bean
	@ConditionalOnMissingBean(NacosMcpOperationService.class)
	public NacosMcpOperationService nacosMcpOperationService(NacosMcpProperties nacosMcpProperties) {
		Properties nacosProperties = nacosMcpProperties.getNacosProperties();
		try {
			return new NacosMcpOperationService(nacosProperties);
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

	@Bean
	public McpGatewayToolsInitializer nacosMcpGatewayToolsInitializer(NacosMcpOperationService nacosMcpOperationService,
			NacosMcpGatewayProperties nacosMcpGatewayProperties) {
		return new NacosMcpGatewayToolsInitializer(nacosMcpOperationService, nacosMcpGatewayProperties);
	}

	@Bean(destroyMethod = "stop")
	public NacosMcpGatewayToolsWatcher nacosInstanceWatcher(final McpGatewayToolManager mcpGatewayToolManager,
			NacosMcpOperationService nacosMcpOperationService, NacosMcpGatewayProperties nacosMcpGatewayProperties) {
		return new NacosMcpGatewayToolsWatcher(mcpGatewayToolManager, nacosMcpOperationService,
				nacosMcpGatewayProperties);
	}

	@Bean
	@ConditionalOnBean(McpAsyncServer.class)
	@ConditionalOnMissingBean(McpGatewayToolManager.class)
	public McpGatewayToolManager nacosMcpGatewayAsyncToolsProvider(final McpAsyncServer mcpAsyncServer) {
		return new NacosMcpAsyncGatewayToolsProvider(mcpAsyncServer);
	}

	@Bean
	@ConditionalOnBean(McpSyncServer.class)
	@ConditionalOnMissingBean(McpGatewayToolManager.class)
	public McpGatewayToolManager nacosMcpGatewaySyncToolsProvider(final McpSyncServer mcpSyncServer) {
		return new NacosMcpSyncGatewayToolsProvider(mcpSyncServer);
	}

}

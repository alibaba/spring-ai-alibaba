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
package com.alibaba.cloud.ai.autoconfigure.mcp.server;

import com.alibaba.cloud.ai.mcp.nacos2.NacosMcpProperties;
import com.alibaba.cloud.ai.mcp.nacos2.registry.NacosMcpRegister;
import com.alibaba.cloud.ai.mcp.nacos2.registry.NacosMcpRegistryProperties;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import org.springframework.ai.mcp.server.autoconfigure.McpServerAutoConfiguration;
import org.springframework.ai.mcp.server.autoconfigure.McpServerProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * @author Sunrisea
 */
@EnableConfigurationProperties({ NacosMcpRegistryProperties.class, NacosMcpProperties.class,
		McpServerProperties.class })
@AutoConfiguration(after = McpServerAutoConfiguration.class)
@ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class Nacos2McpRegistryAutoConfiguration {

	@Bean
	@ConditionalOnBean(McpSyncServer.class)
	@ConditionalOnProperty(prefix = NacosMcpRegistryProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
			matchIfMissing = false)
	public NacosMcpRegister nacosMcpRegisterSync(McpSyncServer mcpSyncServer, NacosMcpProperties nacosMcpProperties,
			NacosMcpRegistryProperties nacosMcpRegistryProperties, McpServerTransportProvider mcpServerTransport) {
		McpAsyncServer mcpAsyncServer = mcpSyncServer.getAsyncServer();
		return getNacosMcpRegister(mcpAsyncServer, nacosMcpProperties, nacosMcpRegistryProperties, mcpServerTransport);
	}

	@Bean
	@ConditionalOnBean(McpAsyncServer.class)
	@ConditionalOnProperty(prefix = NacosMcpRegistryProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
			matchIfMissing = false)
	public NacosMcpRegister nacosMcpRegisterAsync(McpAsyncServer mcpAsyncServer, NacosMcpProperties nacosMcpProperties,
			NacosMcpRegistryProperties nacosMcpRegistryProperties, McpServerTransportProvider mcpServerTransport) {
		return getNacosMcpRegister(mcpAsyncServer, nacosMcpProperties, nacosMcpRegistryProperties, mcpServerTransport);
	}

	private NacosMcpRegister getNacosMcpRegister(McpAsyncServer mcpAsyncServer, NacosMcpProperties nacosMcpProperties,
			NacosMcpRegistryProperties nacosMcpRegistryProperties, McpServerTransportProvider mcpServerTransport) {
		if (mcpServerTransport instanceof StdioServerTransportProvider) {
			return new NacosMcpRegister(mcpAsyncServer, nacosMcpProperties, nacosMcpRegistryProperties, "stdio");
		}
		else {
			return new NacosMcpRegister(mcpAsyncServer, nacosMcpProperties, nacosMcpRegistryProperties, "sse");
		}
	}

}

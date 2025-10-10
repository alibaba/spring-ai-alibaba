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

package com.alibaba.cloud.ai.autoconfigure.mcp.router;

import com.alibaba.cloud.ai.mcp.router.config.McpRouterProperties;
import com.alibaba.cloud.ai.mcp.router.core.discovery.FileConfigMcpServiceDiscovery;
import com.alibaba.cloud.ai.mcp.router.core.discovery.McpServiceDiscovery;
import com.alibaba.cloud.ai.mcp.router.core.discovery.McpServiceDiscoveryFactory;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.server.autoconfigure.McpServerProperties;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Register FileConfigMcpServiceDiscovery to McpServiceDiscoveryFactory.
 *
 * @author digitzh
 */
@AutoConfigureAfter(McpServiceDiscoveryAutoConfiguration.class)
@EnableConfigurationProperties({ McpRouterProperties.class, McpServerProperties.class })
@ConditionalOnProperty(prefix = McpRouterProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = false)
public class FileMcpRouterAutoConfiguration {

	private static final Logger log = LoggerFactory.getLogger(FileMcpRouterAutoConfiguration.class);

	@Bean
	public FileMcpServiceDiscoveryRegistrar fileMcpServiceDiscoveryRegistrar(
			McpServiceDiscoveryFactory discoveryFactory, McpRouterProperties properties) {
		log.info("Creating file MCP service discovery registrar with properties: {}", properties);
		return new FileMcpServiceDiscoveryRegistrar(discoveryFactory, properties);
	}

	public static class FileMcpServiceDiscoveryRegistrar {

		private final McpServiceDiscoveryFactory discoveryFactory;

		private final McpRouterProperties properties;

		public FileMcpServiceDiscoveryRegistrar(McpServiceDiscoveryFactory discoveryFactory,
				McpRouterProperties properties) {
			this.discoveryFactory = discoveryFactory;
			this.properties = properties;
			log.info("File MCP service discovery registrar constructor called with properties: {}", properties);
		}

		@PostConstruct
		public void init() {
			log.info("File MCP service discovery registrar initialized with properties: {}", properties);
			log.info("Registering file config MCP service discovery with {} services",
					properties.getServices() != null ? properties.getServices().size() : 0);
			McpServiceDiscovery fileDiscovery = new FileConfigMcpServiceDiscovery(properties);
			discoveryFactory.registerDiscovery("file", fileDiscovery);
		}

	}

}

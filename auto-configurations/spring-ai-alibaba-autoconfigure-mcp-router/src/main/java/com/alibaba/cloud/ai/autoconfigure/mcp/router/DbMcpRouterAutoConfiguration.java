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

import com.alibaba.cloud.ai.mcp.router.config.DbMcpProperties;
import com.alibaba.cloud.ai.mcp.router.config.McpRouterProperties;
import com.alibaba.cloud.ai.mcp.router.core.discovery.DbMcpServiceDiscovery;
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
 * Register DbMcpServiceDiscovery to McpServiceDiscoveryFactory.
 *
 * @author digitzh
 */
@AutoConfigureAfter(McpServiceDiscoveryAutoConfiguration.class)
@EnableConfigurationProperties({ McpRouterProperties.class, DbMcpProperties.class, McpServerProperties.class })
@ConditionalOnProperty(prefix = McpRouterProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = false)
public class DbMcpRouterAutoConfiguration {

	private static final Logger log = LoggerFactory.getLogger(DbMcpRouterAutoConfiguration.class);

	@Bean
	@ConditionalOnProperty(prefix = DbMcpProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true")
	public DbMcpServiceDiscoveryRegistrar dbMcpServiceDiscoveryRegistrar(McpServiceDiscoveryFactory discoveryFactory,
			DbMcpProperties dbMcpProperties) {
		log.info("Creating database MCP service discovery registrar with properties: {}", dbMcpProperties);
		return new DbMcpServiceDiscoveryRegistrar(discoveryFactory, dbMcpProperties);
	}

	public static class DbMcpServiceDiscoveryRegistrar {

		private final McpServiceDiscoveryFactory discoveryFactory;

		private final DbMcpProperties dbMcpProperties;

		public DbMcpServiceDiscoveryRegistrar(McpServiceDiscoveryFactory discoveryFactory,
				DbMcpProperties dbMcpProperties) {
			this.discoveryFactory = discoveryFactory;
			this.dbMcpProperties = dbMcpProperties;
			log.info("Database MCP service discovery registrar constructor called with properties: {}",
					dbMcpProperties);
		}

		@PostConstruct
		public void init() {
			log.info("Database MCP service discovery registrar initialized with properties: {}", dbMcpProperties);
			log.info("Registering DB MCP service discovery with configuration: {}", dbMcpProperties);
			McpServiceDiscovery dbDiscovery = new DbMcpServiceDiscovery(dbMcpProperties);
			discoveryFactory.registerDiscovery("database", dbDiscovery);
		}

	}

}

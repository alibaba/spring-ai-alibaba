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
import com.alibaba.cloud.ai.mcp.router.core.discovery.CompositeMcpServiceDiscovery;
import com.alibaba.cloud.ai.mcp.router.core.discovery.McpServiceDiscovery;
import com.alibaba.cloud.ai.mcp.router.core.discovery.McpServiceDiscoveryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * Register McpServiceDiscovery to McpServiceDiscoveryFactory.
 *
 * @author digitzh
 */
@AutoConfiguration
@EnableConfigurationProperties(McpRouterProperties.class)
@ConditionalOnProperty(prefix = McpRouterProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = false)
public class McpServiceDiscoveryAutoConfiguration {

	private static final Logger log = LoggerFactory.getLogger(McpServiceDiscoveryAutoConfiguration.class);

	@Bean
	public McpServiceDiscoveryFactory mcpServiceDiscoveryFactory() {
		log.info("Creating MCP service discovery factory");
		return new McpServiceDiscoveryFactory();
	}

	@Bean
	@Primary
	public McpServiceDiscovery compositeMcpServiceDiscovery(McpServiceDiscoveryFactory discoveryFactory,
			McpRouterProperties properties) {

		List<String> searchOrder = getSearchOrder(properties);
		log.info("Creating composite MCP service discovery with search order: {}", searchOrder);

		return new CompositeMcpServiceDiscovery(discoveryFactory, searchOrder);
	}

	private List<String> getSearchOrder(McpRouterProperties properties) {
		if (properties.getDiscoveryOrder() != null && !properties.getDiscoveryOrder().isEmpty()) {
			return properties.getDiscoveryOrder();
		}

		return List.of("nacos");
	}

}

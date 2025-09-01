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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(McpRouterProperties.class)
@ConditionalOnProperty(prefix = McpRouterProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class FileMcpRouterAutoConfiguration {

	private static final Logger log = LoggerFactory.getLogger(FileMcpRouterAutoConfiguration.class);

	@Value("${spring.ai.dashscope.api-key:default_api_key}")
	private String apiKey;

	@Bean
	@ConditionalOnProperty(prefix = McpRouterProperties.CONFIG_PREFIX, name = "discovery-type", havingValue = "file")
	public McpServiceDiscovery fileConfigMcpServiceDiscovery(McpRouterProperties properties) {
		return new FileConfigMcpServiceDiscovery(properties);
	}

}

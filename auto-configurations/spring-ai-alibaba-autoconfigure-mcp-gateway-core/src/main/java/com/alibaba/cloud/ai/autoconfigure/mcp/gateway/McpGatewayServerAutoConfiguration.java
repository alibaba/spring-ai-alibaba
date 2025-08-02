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

package com.alibaba.cloud.ai.autoconfigure.mcp.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.server.autoconfigure.McpServerAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Auto-configuration to disable Spring AI's default MCP Server when Gateway is enabled.
 *
 * This configuration ensures that when MCP Gateway is enabled, we use our own server
 * configurations instead of Spring AI's default ones.
 *
 * @author aias00
 */
@AutoConfiguration(before = McpServerAutoConfiguration.class)
@ConditionalOnProperty(name = "spring.ai.alibaba.mcp.gateway.enabled", havingValue = "true", matchIfMissing = true)
public class McpGatewayServerAutoConfiguration {

	private static final Logger log = LoggerFactory.getLogger(McpGatewayServerAutoConfiguration.class);

	/**
	 * This method is intentionally empty but serves as a marker to ensure this
	 * configuration takes precedence over Spring AI's default MCP Server configuration.
	 *
	 * The actual server configurations are handled by: -
	 * McpGatewaySseServerAutoConfiguration (for SSE) -
	 * McpGatewayStreamableServerAutoConfiguration (for Streamable HTTP)
	 */
	@Bean
	@Primary
	public String gatewayServerMarker() {
		log.info("MCP Gateway server configuration enabled - using custom server configurations");
		return "gateway-server-marker";
	}

}

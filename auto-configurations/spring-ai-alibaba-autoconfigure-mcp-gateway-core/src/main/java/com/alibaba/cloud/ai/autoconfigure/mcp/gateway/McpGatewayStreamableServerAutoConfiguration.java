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

import com.alibaba.cloud.ai.mcp.gateway.core.McpGatewayProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.server.autoconfigure.McpServerProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for MCP Gateway Streamable HTTP Server Transport.
 *
 * This configuration provides Streamable HTTP transport support for the MCP Gateway,
 * enabling it to expose both SSE and Streamable HTTP protocols.
 *
 * @author aias00
 */
@AutoConfiguration
@ConditionalOnClass({ HttpServletStreamableServerTransportProvider.class })
@ConditionalOnProperty(name = "spring.ai.alibaba.mcp.gateway.streamable.enabled", havingValue = "true",
		matchIfMissing = false)
public class McpGatewayStreamableServerAutoConfiguration {

	private static final Logger log = LoggerFactory.getLogger(McpGatewayStreamableServerAutoConfiguration.class);

	/**
	 * Creates a Streamable HTTP server transport provider for the MCP Gateway.
	 * @param objectMapper ObjectMapper for JSON serialization/deserialization
	 * @param properties MCP Server properties
	 * @param gatewayProperties Gateway properties
	 * @return HttpServletStreamableServerTransportProvider
	 */
	@Bean
	public HttpServletStreamableServerTransportProvider gatewayStreamableTransportProvider(
			ObjectProvider<ObjectMapper> objectMapper, McpServerProperties properties,
			McpGatewayProperties gatewayProperties) {

		log.info("Configuring MCP Gateway Streamable HTTP transport provider");

		// 使用独立的端点路径，不依赖 SSE 端点
		String endpoint = gatewayProperties.getStreamable().getEndpoint();

		HttpServletStreamableServerTransportProvider.Builder builder = HttpServletStreamableServerTransportProvider
			.builder()
			.objectMapper(objectMapper.getIfAvailable(ObjectMapper::new))
			.mcpEndpoint(endpoint)
			.keepAliveInterval(java.time.Duration.ofSeconds(30));

		HttpServletStreamableServerTransportProvider provider = builder.build();

		log.info("MCP Gateway Streamable HTTP transport provider configured with endpoint: {}", endpoint);

		return provider;
	}

	/**
	 * Creates a servlet registration for Streamable HTTP MCP endpoints.
	 * @param streamableTransportProvider Streamable HTTP transport provider
	 * @param properties MCP Server properties
	 * @param gatewayProperties Gateway properties
	 * @return ServletRegistrationBean for Streamable HTTP MCP endpoints
	 */
	@Bean
	public ServletRegistrationBean<HttpServletStreamableServerTransportProvider> gatewayStreamableMcpServletRegistration(
			HttpServletStreamableServerTransportProvider streamableTransportProvider, McpServerProperties properties,
			McpGatewayProperties gatewayProperties) {

		log.info("Configuring MCP Gateway Streamable HTTP servlet registration");

		// 使用独立的端点路径，不依赖 SSE 端点
		String servletPath = gatewayProperties.getStreamable().getEndpoint() + "/*";

		ServletRegistrationBean<HttpServletStreamableServerTransportProvider> registration = new ServletRegistrationBean<>(
				streamableTransportProvider, servletPath);

		log.info("MCP Gateway Streamable HTTP servlet registration configured for path: {}", servletPath);

		return registration;
	}

}

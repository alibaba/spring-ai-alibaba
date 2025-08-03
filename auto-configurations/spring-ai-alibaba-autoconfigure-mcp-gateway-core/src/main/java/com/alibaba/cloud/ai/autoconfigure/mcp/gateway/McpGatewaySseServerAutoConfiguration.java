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
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.server.autoconfigure.McpServerAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for MCP Gateway SSE Server Transport.
 *
 * This configuration provides SSE transport support for the MCP Gateway, enabling it to
 * expose SSE protocol with configurable settings.
 *
 * @author aias00
 */
@AutoConfiguration(before = McpServerAutoConfiguration.class)
@ConditionalOnClass({ HttpServletSseServerTransportProvider.class })
@ConditionalOnProperty(name = "spring.ai.alibaba.mcp.gateway.sse.enabled", havingValue = "true", matchIfMissing = true)
public class McpGatewaySseServerAutoConfiguration {

	private static final Logger log = LoggerFactory.getLogger(McpGatewaySseServerAutoConfiguration.class);

	/**
	 * Creates an SSE server transport provider for the MCP Gateway.
	 * @param objectMapper ObjectMapper for JSON serialization/deserialization
	 * @param gatewayProperties Gateway properties
	 * @return HttpServletSseServerTransportProvider
	 */
	@Bean
	public HttpServletSseServerTransportProvider gatewaySseTransportProvider(ObjectProvider<ObjectMapper> objectMapper,
			McpGatewayProperties gatewayProperties) {

		log.info("Configuring MCP Gateway SSE transport provider");

		// 使用独立的端点路径，不依赖 Spring AI 的 SSE 端点
		String endpoint = gatewayProperties.getSse().getEndpoint();

		HttpServletSseServerTransportProvider.Builder builder = HttpServletSseServerTransportProvider.builder()
			.objectMapper(objectMapper.getIfAvailable(ObjectMapper::new))
			.messageEndpoint(gatewayProperties.getMessageEndpoint())
			.sseEndpoint(endpoint);

		HttpServletSseServerTransportProvider provider = builder.build();

		log.info("MCP Gateway SSE transport provider configured with endpoint: {}", endpoint);

		return provider;
	}

	/**
	 * Creates a servlet registration for SSE MCP endpoints.
	 * @param sseTransportProvider SSE transport provider
	 * @param gatewayProperties Gateway properties
	 * @return ServletRegistrationBean for SSE MCP endpoints
	 */
	@Bean
	public ServletRegistrationBean<HttpServletSseServerTransportProvider> gatewaySseMcpServletRegistration(
			HttpServletSseServerTransportProvider sseTransportProvider, McpGatewayProperties gatewayProperties) {

		log.info("Configuring MCP Gateway SSE servlet registration");

		// 使用独立的端点路径，不依赖 Spring AI 的 SSE 端点
		String servletPath = gatewayProperties.getSse().getEndpoint() + "/*";

		ServletRegistrationBean<HttpServletSseServerTransportProvider> registration = new ServletRegistrationBean<>(
				sseTransportProvider, servletPath);

		log.info("MCP Gateway SSE servlet registration configured for path: {}", servletPath);

		return registration;
	}

}

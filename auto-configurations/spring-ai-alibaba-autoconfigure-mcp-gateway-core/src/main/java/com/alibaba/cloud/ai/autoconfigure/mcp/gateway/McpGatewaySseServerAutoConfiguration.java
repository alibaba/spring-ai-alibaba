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
import io.modelcontextprotocol.server.transport.WebFluxSseServerTransportProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.server.autoconfigure.McpServerAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.server.RouterFunction;

/**
 * Auto-configuration for MCP Gateway SSE Server Transport using WebFlux.
 *
 * This configuration provides SSE transport support for the MCP Gateway using WebFlux,
 * enabling it to expose SSE protocol with configurable settings.
 *
 * @author aias00
 */
@AutoConfiguration(before = McpServerAutoConfiguration.class)
@ConditionalOnClass({ WebFluxSseServerTransportProvider.class })
@ConditionalOnProperty(name = "spring.ai.alibaba.mcp.gateway.sse.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class McpGatewaySseServerAutoConfiguration {

	private static final Logger log = LoggerFactory.getLogger(McpGatewaySseServerAutoConfiguration.class);

	/**
	 * Creates a WebFlux SSE server transport provider for the MCP Gateway.
	 * @param objectMapper ObjectMapper for JSON serialization/deserialization
	 * @return WebFluxSseServerTransportProvider
	 */
	@Bean
	public WebFluxSseServerTransportProvider gatewaySseTransportProvider(ObjectProvider<ObjectMapper> objectMapper,
			McpGatewayProperties gatewayProperties) {

		log.info("Configuring MCP Gateway WebFlux SSE transport provider");

		WebFluxSseServerTransportProvider.Builder builder = WebFluxSseServerTransportProvider.builder()
			.objectMapper(objectMapper.getIfAvailable(ObjectMapper::new))
			.messageEndpoint(gatewayProperties.getMessageEndpoint())
			.sseEndpoint(gatewayProperties.getSse().getEndpoint());

		WebFluxSseServerTransportProvider provider = builder.build();

		log.info("MCP Gateway WebFlux SSE transport provider configured with message endpoint: {} and SSE endpoint: {}",
				gatewayProperties.getMessageEndpoint(), gatewayProperties.getSse().getEndpoint());

		return provider;
	}

	/**
	 * Creates a router function for WebFlux SSE transport.
	 * @param webFluxProvider WebFlux SSE transport provider
	 * @return RouterFunction
	 */
	@Bean
	public RouterFunction<?> webfluxMcpRouterFunction(WebFluxSseServerTransportProvider webFluxProvider) {
		RouterFunction<?> routerFunction = webFluxProvider.getRouterFunction();
		log.info("MCP Gateway WebFlux RouterFunction created successfully");
		return routerFunction;
	}

}

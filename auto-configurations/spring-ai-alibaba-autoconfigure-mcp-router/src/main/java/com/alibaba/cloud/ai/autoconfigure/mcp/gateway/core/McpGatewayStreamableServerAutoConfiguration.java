/// *
// * Copyright 2024-2025 the original author or authors.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * https://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
// package com.alibaba.cloud.ai.autoconfigure.mcp.gateway.core;
//
// import com.alibaba.cloud.ai.mcp.gateway.core.McpGatewayProperties;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import io.modelcontextprotocol.server.McpAsyncServer;
// import io.modelcontextprotocol.server.McpServer;
// import io.modelcontextprotocol.server.McpServer.AsyncSpecification;
// import io.modelcontextprotocol.server.McpServer.SyncSpecification;
// import io.modelcontextprotocol.server.McpServerFeatures;
// import io.modelcontextprotocol.server.McpSyncServer;
// import
/// io.modelcontextprotocol.server.transport.WebFluxStreamableServerTransportProvider;
// import io.modelcontextprotocol.spec.McpSchema;
// import io.modelcontextprotocol.spec.McpStreamableServerTransportProvider;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.ai.mcp.server.autoconfigure.McpServerProperties;
// import org.springframework.beans.factory.ObjectProvider;
// import org.springframework.boot.autoconfigure.AutoConfiguration;
// import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
// import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
// import org.springframework.context.annotation.Bean;
// import org.springframework.web.reactive.function.server.RouterFunction;
//
// import java.util.List;
//
/// **
// * Autoconfiguration for MCP Gateway Streamable HTTP Server Transport. This
/// configuration
// * provides Streamable HTTP transport support for the MCP Gateway, enabling it to expose
// * both SSE and Streamable HTTP protocols.
// *
// * @author aias00
// */
// @AutoConfiguration(before = McpGatewayServerAutoConfiguration.class)
// @ConditionalOnClass({ WebFluxStreamableServerTransportProvider.class })
// @ConditionalOnProperty(name = "spring.ai.alibaba.mcp.gateway.streamable.enabled",
/// havingValue = "true",
// matchIfMissing = false)
// public class McpGatewayStreamableServerAutoConfiguration {
//
// private static final Logger log =
/// LoggerFactory.getLogger(McpGatewayStreamableServerAutoConfiguration.class);
//
// /**
// * Creates a Streamable HTTP server transport provider for the MCP Gateway.
// * @param objectMapper ObjectMapper for JSON serialization/deserialization
// * @param gatewayProperties Gateway properties
// * @return HttpServletStreamableServerTransportProvider
// */
// @Bean
// public WebFluxStreamableServerTransportProvider gatewayStreamableTransportProvider(
// ObjectProvider<ObjectMapper> objectMapper, McpGatewayProperties gatewayProperties) {
//
// log.info("Configuring MCP Gateway Streamable HTTP transport provider");
//
// String endpoint = gatewayProperties.getStreamable().getEndpoint();
//
// WebFluxStreamableServerTransportProvider.Builder builder =
/// WebFluxStreamableServerTransportProvider.builder()
// .objectMapper(objectMapper.getIfAvailable(ObjectMapper::new))
// .messageEndpoint(endpoint)
// .keepAliveInterval(java.time.Duration.ofSeconds(30));
//
// WebFluxStreamableServerTransportProvider provider = builder.build();
//
// log.info("MCP Gateway Streamable HTTP transport provider configured with endpoint: {}",
/// endpoint);
//
// return provider;
// }
//
// /**
// * Creates a router function for WebFlux SSE transport.
// * @param webFluxStreamableServerTransportProvider WebFlux Streamable transport
// * provider
// * @return RouterFunction
// */
// @Bean
// public RouterFunction<?> webfluxMcpStreamableRouterFunction(
// WebFluxStreamableServerTransportProvider webFluxStreamableServerTransportProvider) {
// RouterFunction<?> routerFunction =
/// webFluxStreamableServerTransportProvider.getRouterFunction();
// log.info("MCP Gateway WebFlux RouterFunction created successfully");
// return routerFunction;
// }
//
// /**
// * Creates a synchronous MCP Server bean compatible with v0.11.0. This simulates the
// * old version's sync() method behavior.
// * @param transportProviders Available transport providers
// * @return McpSyncServer
// */
// @Bean
// @ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "type",
/// havingValue = "SYNC",
// matchIfMissing = true)
// public McpSyncServer mcpStreamableSyncServer(
// ObjectProvider<List<McpStreamableServerTransportProvider>> transportProviders,
// ObjectProvider<List<McpServerFeatures.SyncToolSpecification>> tools,
/// McpServerProperties serverProperties) {
//
// log.info("Creating MCP Sync Server bean compatible with v0.11.0");
//
// List<McpStreamableServerTransportProvider> providers =
/// transportProviders.getIfAvailable();
// if (providers == null || providers.isEmpty()) {
// log.warn("No transport providers available for MCP Sync Server");
// return null;
// }
//
// // 使用第一个可用的 transport provider
// McpStreamableServerTransportProvider provider = providers.get(0);
// log.info("Using transport provider: {}", provider.getClass().getSimpleName());
//
// // 创建服务器信息
// McpSchema.Implementation serverInfo = new
/// McpSchema.Implementation("mcp-streamable-gateway", "1.0.0");
//
// // 创建同步服务器规范
// SyncSpecification<?> serverBuilder = McpServer.sync(provider).serverInfo(serverInfo);
//
// // 构建服务器能力
// McpSchema.ServerCapabilities.Builder capabilitiesBuilder =
/// McpSchema.ServerCapabilities.builder();
// capabilitiesBuilder.tools(serverProperties.isToolChangeNotification()); // 启用工具能力
//
// serverBuilder.capabilities(capabilitiesBuilder.build());
//
// serverBuilder.tools(tools.stream().flatMap(List::stream).toList());
// serverBuilder.requestTimeout(serverProperties.getRequestTimeout());
// // 构建并返回同步服务器
// return serverBuilder.build();
// }
//
// /**
// * Creates an asynchronous MCP Server bean compatible with v0.11.0. This simulates the
// * old version's async() method behavior.
// * @param transportProviders Available transport providers
// * @return McpAsyncServer
// */
// @Bean
// @ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "type",
/// havingValue = "ASYNC")
// public McpAsyncServer mcpStreamableAsyncServer(
// ObjectProvider<List<McpStreamableServerTransportProvider>> transportProviders,
// ObjectProvider<List<McpServerFeatures.AsyncToolSpecification>> tools,
// McpServerProperties serverProperties) {
//
// log.info("Creating MCP Async Server bean compatible with v0.11.0");
//
// List<McpStreamableServerTransportProvider> providers =
/// transportProviders.getIfAvailable();
// if (providers == null || providers.isEmpty()) {
// log.warn("No transport providers available for MCP Async Server");
// return null;
// }
//
// // 使用第一个可用的 transport provider
// McpStreamableServerTransportProvider provider = providers.get(0);
// log.info("Using transport provider: {}", provider.getClass().getSimpleName());
//
// // 创建服务器信息
// McpSchema.Implementation serverInfo = new McpSchema.Implementation("mcp-gateway",
/// "1.0.0");
//
// // 创建异步服务器规范
// AsyncSpecification<?> serverBuilder = McpServer.async(provider).serverInfo(serverInfo);
//
// // 构建服务器能力
// McpSchema.ServerCapabilities.Builder capabilitiesBuilder =
/// McpSchema.ServerCapabilities.builder();
//
// capabilitiesBuilder.tools(serverProperties.isToolChangeNotification());
//
// serverBuilder.tools(tools.stream().flatMap(List::stream).toList());
//
// serverBuilder.capabilities(capabilitiesBuilder.build());
//
// // 构建并返回异步服务器
// return serverBuilder.build();
// }
//
// }

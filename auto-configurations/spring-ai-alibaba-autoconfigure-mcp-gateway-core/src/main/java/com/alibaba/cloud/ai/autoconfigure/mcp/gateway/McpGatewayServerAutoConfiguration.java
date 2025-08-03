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
import com.alibaba.cloud.ai.mcp.gateway.core.McpGatewayToolCallbackProvider;
import com.alibaba.cloud.ai.mcp.gateway.core.McpGatewayToolsInitializer;
import com.alibaba.cloud.ai.mcp.gateway.core.utils.SpringBeanUtils;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.mcp.server.autoconfigure.McpServerProperties;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.NonNull;
import org.springframework.util.MimeType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Auto-configuration for MCP Server Bean initialization compatible with v0.11.0.
 *
 * This configuration provides a compatibility layer for Spring AI's MCP Server by
 * creating McpServer beans that work with MCP Java SDK v0.11.0.
 *
 * @author aias00
 */
@AutoConfiguration
@EnableConfigurationProperties({ McpServerProperties.class, McpGatewayProperties.class })
@ConditionalOnClass({ McpServer.class, McpServerTransportProvider.class })
@ConditionalOnProperty(name = "spring.ai.alibaba.mcp.gateway.enabled", havingValue = "true", matchIfMissing = true)
public class McpGatewayServerAutoConfiguration implements ApplicationContextAware {

	private static final Logger log = LoggerFactory.getLogger(McpGatewayServerAutoConfiguration.class);

	@Override
	public void setApplicationContext(@NonNull final ApplicationContext applicationContext) throws BeansException {
		SpringBeanUtils.getInstance().setApplicationContext(applicationContext);
	}

	@Bean
	public ToolCallbackProvider callbackProvider(final McpGatewayToolsInitializer toolsInitializer) {
		return McpGatewayToolCallbackProvider.builder().toolCallbacks(toolsInitializer.initializeTools()).build();
	}

	/**
	 * Creates a synchronous MCP Server bean compatible with v0.11.0. This simulates the
	 * old version's sync() method behavior.
	 * @param transportProviders Available transport providers
	 * @return McpSyncServer
	 */
	@Bean
	public McpSyncServer mcpSyncServer(ObjectProvider<List<McpServerTransportProvider>> transportProviders,
			ObjectProvider<List<SyncToolSpecification>> tools, List<ToolCallbackProvider> toolCallbackProvider,
			McpServerProperties serverProperties) {

		log.info("Creating MCP Sync Server bean compatible with v0.11.0");

		List<McpServerTransportProvider> providers = transportProviders.getIfAvailable();
		if (providers == null || providers.isEmpty()) {
			log.warn("No transport providers available for MCP Sync Server");
			return null;
		}

		// 使用第一个可用的 transport provider
		McpServerTransportProvider provider = providers.get(0);
		log.info("Using transport provider: {}", provider.getClass().getSimpleName());

		// 创建服务器信息
		McpSchema.Implementation serverInfo = new McpSchema.Implementation("mcp-gateway", "1.0.0");

		// 创建同步服务器规范
		SyncSpecification<SingleSessionSyncSpecification> serverBuilder = McpServer.sync(provider)
			.serverInfo(serverInfo);

		// 构建服务器能力
		McpSchema.ServerCapabilities.Builder capabilitiesBuilder = McpSchema.ServerCapabilities.builder();
		capabilitiesBuilder.tools(serverProperties.isToolChangeNotification()); // 启用工具能力

		serverBuilder.capabilities(capabilitiesBuilder.build());

		List<SyncToolSpecification> toolSpecifications = new ArrayList<>(tools.stream().flatMap(List::stream).toList());

		List<ToolCallback> providerToolCallbacks = toolCallbackProvider.stream()
			.map(pr -> List.of(pr.getToolCallbacks()))
			.flatMap(List::stream)
			.filter(fc -> fc instanceof ToolCallback)
			.map(fc -> (ToolCallback) fc)
			.toList();

		toolSpecifications.addAll(this.toSyncToolSpecifications(providerToolCallbacks, serverProperties));

		serverBuilder.tools(toolSpecifications);
		serverBuilder.requestTimeout(serverProperties.getRequestTimeout());
		// 构建并返回同步服务器
		return serverBuilder.build();
	}

	private List<McpServerFeatures.SyncToolSpecification> toSyncToolSpecifications(List<ToolCallback> tools,
			McpServerProperties serverProperties) {

		// De-duplicate tools by their name, keeping the first occurrence of each tool
		// name
		return tools.stream() // Key: tool name
			.collect(Collectors.toMap(tool -> tool.getToolDefinition().name(), tool -> tool, // Value:
					// the
					// tool
					// itself
					(existing, replacement) -> existing)) // On duplicate key, keep the
			// existing tool
			.values()
			.stream()
			.map(tool -> {
				String toolName = tool.getToolDefinition().name();
				MimeType mimeType = (serverProperties.getToolResponseMimeType().containsKey(toolName))
						? MimeType.valueOf(serverProperties.getToolResponseMimeType().get(toolName)) : null;
				return McpToolUtils.toSyncToolSpecification(tool, mimeType);
			})
			.toList();
	}

	/**
	 * Creates an asynchronous MCP Server bean compatible with v0.11.0. This simulates the
	 * old version's async() method behavior.
	 * @param transportProviders Available transport providers
	 * @return McpAsyncServer
	 */
	@Bean
	public McpAsyncServer mcpAsyncServer(ObjectProvider<List<McpServerTransportProvider>> transportProviders) {

		log.info("Creating MCP Async Server bean compatible with v0.11.0");

		List<McpServerTransportProvider> providers = transportProviders.getIfAvailable();
		if (providers == null || providers.isEmpty()) {
			log.warn("No transport providers available for MCP Async Server");
			return null;
		}

		// 使用第一个可用的 transport provider
		McpServerTransportProvider provider = providers.get(0);
		log.info("Using transport provider: {}", provider.getClass().getSimpleName());

		// 创建服务器信息
		McpSchema.Implementation serverInfo = new McpSchema.Implementation("mcp-gateway", "1.0.0");

		// 创建异步服务器规范
		McpServer.AsyncSpecification serverBuilder = McpServer.async(provider).serverInfo(serverInfo);

		// 构建服务器能力
		McpSchema.ServerCapabilities.Builder capabilitiesBuilder = McpSchema.ServerCapabilities.builder();
		capabilitiesBuilder.tools(false); // 启用工具能力

		serverBuilder.capabilities(capabilitiesBuilder.build());

		// 构建并返回异步服务器
		return serverBuilder.build();
	}

	/**
	 * Creates a default MCP Server bean for backward compatibility. This provides a
	 * fallback for applications expecting the old API.
	 * @param syncServer Synchronous server
	 * @param asyncServer Asynchronous server
	 * @return McpAsyncServer (优先使用异步服务器)
	 */
	@Bean
	public McpAsyncServer mcpServer(McpSyncServer syncServer, McpAsyncServer asyncServer) {

		log.info("Creating default MCP Server bean for backward compatibility");

		// 优先使用异步服务器，如果没有则从同步服务器获取
		if (asyncServer != null) {
			log.info("Using MCP Async Server");
			return asyncServer;
		}
		else if (syncServer != null) {
			log.info("Using MCP Sync Server's async server");
			return syncServer.getAsyncServer();
		}
		else {
			log.warn("No server specifications available, cannot create MCP Server");
			return null;
		}
	}

}

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

package com.alibaba.cloud.ai.autoconfigure.mcp.gateway.core;

import com.alibaba.cloud.ai.mcp.gateway.core.McpGatewayProperties;
import com.alibaba.cloud.ai.mcp.gateway.core.McpGatewayToolCallbackProvider;
import com.alibaba.cloud.ai.mcp.gateway.core.McpGatewayToolsInitializer;
import com.alibaba.cloud.ai.mcp.gateway.core.utils.SpringBeanUtils;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncToolSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.mcp.server.autoconfigure.McpServerAutoConfiguration;
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
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Autoconfiguration for MCP Server Bean initialization compatible with v0.11.0. This
 * configuration provides a compatibility layer for Spring AI's MCP Server by creating
 * McpServer beans that work with MCP Java SDK v0.11.0.
 *
 * @author aias00
 */
@AutoConfiguration(after = { McpServerAutoConfiguration.class })
@EnableConfigurationProperties({ McpServerProperties.class, McpGatewayProperties.class })
@ConditionalOnClass({ McpServer.class, McpServerTransportProvider.class })
@ConditionalOnProperty(name = "spring.ai.alibaba.mcp.gateway.enabled", havingValue = "true", matchIfMissing = false)
public class McpGatewayServerAutoConfiguration implements ApplicationContextAware {

	private static final Logger log = LoggerFactory.getLogger(McpGatewayServerAutoConfiguration.class);

	@Override
	public void setApplicationContext(@NonNull final ApplicationContext applicationContext) throws BeansException {
		SpringBeanUtils.getInstance().setApplicationContext(applicationContext);
	}

	@Bean
	public ToolCallbackProvider callbackProvider(final McpGatewayToolsInitializer mcpGatewayToolsInitializer) {
		return McpGatewayToolCallbackProvider.builder()
			.toolCallbacks(mcpGatewayToolsInitializer.initializeTools())
			.build();
	}

	// @Bean
	@ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "type", havingValue = "SYNC",
			matchIfMissing = true)
	public List<McpServerFeatures.SyncToolSpecification> gatewaySyncTools(
			ObjectProvider<List<McpServerFeatures.SyncToolSpecification>> tools,
			List<ToolCallbackProvider> toolCallbackProvider, McpServerProperties serverProperties) {
		List<SyncToolSpecification> toolSpecifications = new ArrayList<>(tools.stream().flatMap(List::stream).toList());
		List<ToolCallback> providerToolCallbacks = toolCallbackProvider.stream()
			.map(pr -> List.of(pr.getToolCallbacks()))
			.flatMap(List::stream)
			.filter(Objects::nonNull)
			.toList();

		toolSpecifications.addAll(this.toSyncToolSpecifications(providerToolCallbacks, serverProperties));

		return toolSpecifications;
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

	// @Bean
	@ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "type", havingValue = "ASYNC")
	public List<McpServerFeatures.AsyncToolSpecification> gatewayAsyncTools(
			ObjectProvider<List<AsyncToolSpecification>> tools, List<ToolCallbackProvider> toolCallbackProvider,
			McpServerProperties serverProperties) {

		List<AsyncToolSpecification> toolSpecifications = new ArrayList<>(
				tools.stream().flatMap(List::stream).toList());
		List<ToolCallback> providerToolCallbacks = toolCallbackProvider.stream()
			.map(pr -> List.of(pr.getToolCallbacks()))
			.flatMap(List::stream)
			.filter(Objects::nonNull)
			.toList();
		toolSpecifications.addAll(this.toAsyncToolSpecification(providerToolCallbacks, serverProperties));

		return toolSpecifications;
	}

	private List<McpServerFeatures.AsyncToolSpecification> toAsyncToolSpecification(List<ToolCallback> tools,
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
				return McpToolUtils.toAsyncToolSpecification(tool, mimeType);
			})
			.toList();
	}

}

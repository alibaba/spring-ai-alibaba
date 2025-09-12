/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.manus.mcp.service;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.stereotype.Component;

import com.alibaba.cloud.ai.manus.mcp.config.McpProperties;
import com.alibaba.cloud.ai.manus.mcp.model.po.McpConfigEntity;
import com.alibaba.cloud.ai.manus.mcp.model.vo.McpServerConfig;
import com.alibaba.cloud.ai.manus.mcp.model.vo.McpServiceEntity;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * MCP connection factory
 */
@Component
public class McpConnectionFactory {

	private static final Logger logger = LoggerFactory.getLogger(McpConnectionFactory.class);

	private final McpTransportBuilder transportBuilder;

	private final McpConfigValidator configValidator;

	private final McpProperties mcpProperties;

	private final ObjectMapper objectMapper;

	public McpConnectionFactory(McpTransportBuilder transportBuilder, McpConfigValidator configValidator,
			McpProperties mcpProperties, ObjectMapper objectMapper) {
		this.transportBuilder = transportBuilder;
		this.configValidator = configValidator;
		this.mcpProperties = mcpProperties;
		this.objectMapper = objectMapper;
	}

	/**
	 * Create MCP connection
	 * @param mcpConfigEntity MCP configuration entity
	 * @return MCP service entity
	 * @throws IOException Thrown when creation fails
	 */
	public McpServiceEntity createConnection(McpConfigEntity mcpConfigEntity) throws IOException {
		String serverName = mcpConfigEntity.getMcpServerName();

		// Validate configuration entity
		configValidator.validateMcpConfigEntity(mcpConfigEntity);

		// Check if enabled
		if (!configValidator.isEnabled(mcpConfigEntity)) {
			logger.info("Skipping disabled MCP server: {}", serverName);
			return null;
		}

		// Parse server configuration
		McpServerConfig serverConfig = parseServerConfig(mcpConfigEntity.getConnectionConfig(), serverName);

		// Build transport
		McpClientTransport transport = transportBuilder.buildTransport(mcpConfigEntity.getConnectionType(),
				serverConfig, serverName);

		// Configure MCP transport
		return configureMcpTransport(serverName, transport);
	}

	/**
	 * Parse server configuration
	 * @param connectionConfig Connection configuration JSON
	 * @param serverName Server name
	 * @return Server configuration object
	 * @throws IOException Thrown when parsing fails
	 */
	private McpServerConfig parseServerConfig(String connectionConfig, String serverName) throws IOException {
		try (JsonParser jsonParser = objectMapper.createParser(connectionConfig)) {
			return jsonParser.readValueAs(McpServerConfig.class);
		}
		catch (Exception e) {
			logger.error("Failed to parse server config for server: {}", serverName, e);
			throw new IOException("Failed to parse server config for server: " + serverName, e);
		}
	}

	/**
	 * Configure MCP transport
	 * @param mcpServerName MCP server name
	 * @param transport MCP client transport
	 * @return MCP service entity
	 * @throws IOException Thrown when configuration fails
	 */
	private McpServiceEntity configureMcpTransport(String mcpServerName, McpClientTransport transport)
			throws IOException {
		if (transport == null) {
			return null;
		}

		McpAsyncClient mcpAsyncClient = McpClient.async(transport)
			.requestTimeout(mcpProperties.getTimeout())
			.clientInfo(new McpSchema.Implementation(mcpServerName, "1.0.0"))
			.build();

		// Retry mechanism
		int maxRetries = mcpProperties.getMaxRetries();
		Exception lastException = null;

		for (int attempt = 1; attempt <= maxRetries; attempt++) {
			try {
				logger.debug("Attempting to initialize MCP transport for: {} (attempt {}/{})", mcpServerName, attempt,
						maxRetries);

				mcpAsyncClient.initialize()
					.timeout(mcpProperties.getTimeout())
					.doOnSuccess(result -> logger.info("MCP client initialized successfully for {}", mcpServerName))
					.doOnError(error -> logger.error("Failed to initialize MCP client for {}: {}", mcpServerName,
							error.getMessage()))
					.block();

				logger.info("MCP transport configured successfully for: {} (attempt {})", mcpServerName, attempt);

				AsyncMcpToolCallbackProvider callbackProvider = new AsyncMcpToolCallbackProvider(mcpAsyncClient);
				return new McpServiceEntity(mcpAsyncClient, callbackProvider, mcpServerName);
			}
			catch (Exception e) {
				lastException = e;
				logger.warn("Failed to initialize MCP transport for {} on attempt {}/{}: {}", mcpServerName, attempt,
						maxRetries, e.getMessage());

				if (attempt < maxRetries) {
					try {
						// Incremental wait time
						Thread.sleep(1000L * mcpProperties.getRetryWaitMultiplier() * attempt);
					}
					catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						logger.warn("Retry wait interrupted for server: {}", mcpServerName);
						break;
					}
				}
			}
		}

		logger.error("Failed to initialize MCP transport for {} after {} attempts", mcpServerName, maxRetries,
				lastException);
		return null;
	}

}

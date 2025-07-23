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
package com.alibaba.cloud.ai.example.manus.dynamic.mcp.service;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.po.McpConfigEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.po.McpConfigType;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpConfigRequestVO;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpServerConfig;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpServersConfig;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpServiceEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.repository.McpConfigRepository;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.transport.StreamableHttpClientTransport;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;

@Component
public class McpService implements IMcpService {

	private static final Logger logger = LoggerFactory.getLogger(McpService.class);

	@Autowired
	private McpConfigRepository mcpConfigRepository;

	private final LoadingCache<String, Map<String, McpServiceEntity>> toolCallbackMapCache = CacheBuilder.newBuilder()
		.expireAfterAccess(10, TimeUnit.MINUTES)
		.removalListener((RemovalListener<String, Map<String, McpServiceEntity>>) notification -> {
			Map<String, McpServiceEntity> mcpServiceEntityMap = notification.getValue();
			if (mcpServiceEntityMap == null) {
				return;
			}
			for (McpServiceEntity mcpServiceEntity : mcpServiceEntityMap.values()) {
				try {
					mcpServiceEntity.getMcpAsyncClient().close();
				}
				catch (Throwable t) {
					logger.error("Failed to close MCP client", t);
				}
			}
		})
		.build(new CacheLoader<>() {
			@Override
			public Map<String, McpServiceEntity> load(String key) throws Exception {
				return loadMcpServices(mcpConfigRepository.findAll());
			}
		});

	private Map<String, McpServiceEntity> loadMcpServices(List<McpConfigEntity> mcpConfigEntities) throws IOException {
		Map<String, McpServiceEntity> toolCallbackMap = new ConcurrentHashMap<>();

		if (mcpConfigEntities == null || mcpConfigEntities.isEmpty()) {
			logger.info("No MCP server configurations found");
			return toolCallbackMap;
		}

		logger.info("Loading {} MCP server configurations", mcpConfigEntities.size());

		for (McpConfigEntity mcpConfigEntity : mcpConfigEntities) {
			String serverName = mcpConfigEntity.getMcpServerName();

			try {
				// Validate basic configuration
				if (mcpConfigEntity.getConnectionType() == null) {
					logger.error("Connection type is required for server: {}", serverName);
					throw new IOException("Connection type is required for server: " + serverName);
				}

				if (serverName == null || serverName.trim().isEmpty()) {
					logger.error("Server name is required");
					throw new IOException("Server name is required");
				}

				McpConfigType type = mcpConfigEntity.getConnectionType();
				logger.debug("Processing MCP server: {} with type: {}", serverName, type);

				McpServiceEntity mcpServiceEntity = null;

				switch (type) {
					case SSE -> {
						mcpServiceEntity = createSseConnection(mcpConfigEntity, serverName);
					}
					case STUDIO -> {
						mcpServiceEntity = createStudioConnection(mcpConfigEntity, serverName);
					}
					case STREAMING -> {
						mcpServiceEntity = createStreamableConnection(mcpConfigEntity, serverName);
					}
					default -> {
						logger.error("Unsupported connection type: {} for server: {}", type, serverName);
						throw new IOException("Unsupported connection type: " + type + " for server: " + serverName);
					}
				}

				if (mcpServiceEntity != null) {
					toolCallbackMap.put(serverName, mcpServiceEntity);
					logger.info("Successfully loaded MCP server: {} with type: {}", serverName, type);
				}
				else {
					logger.warn("Failed to create MCP service entity for server: {}", serverName);
				}

			}
			catch (Exception e) {
				logger.error("Failed to load MCP server configuration for: {}, error: {}", serverName, e.getMessage(),
						e);
				// Decide whether to continue processing other servers or throw exception
				// based on requirements
				// Here we choose to continue processing other servers but log the error
				// If strict mode is needed, uncomment the line below
				// throw new IOException("Failed to load MCP server: " + serverName, e);
			}
		}

		logger.info("Successfully loaded {} out of {} MCP servers", toolCallbackMap.size(), mcpConfigEntities.size());
		return toolCallbackMap;
	}

	private McpServiceEntity createSseConnection(McpConfigEntity mcpConfigEntity, String serverName)
			throws IOException {
		McpClientTransport transport = null;

		try (JsonParser jsonParser = new ObjectMapper().createParser(mcpConfigEntity.getConnectionConfig())) {
			McpServerConfig mcpServerConfig = jsonParser.readValueAs(McpServerConfig.class);

			// Validate URL configuration
			if (mcpServerConfig.getUrl() == null || mcpServerConfig.getUrl().trim().isEmpty()) {
				throw new IOException("Invalid or missing MCP server URL for server: " + serverName);
			}

			String url = mcpServerConfig.getUrl().trim();
			String baseUrl;
			String sseEndpoint;

			try {
				java.net.URL parsedUrl = new java.net.URL(url);
				baseUrl = parsedUrl.getProtocol() + "://" + parsedUrl.getHost()
						+ (parsedUrl.getPort() == -1 ? "" : ":" + parsedUrl.getPort());

				// Check if URL path ends with /sse, throw error if not
				String path = parsedUrl.getPath();
				if (path == null || !path.endsWith("/sse")) {
					throw new IllegalArgumentException(
							"URL path must end with /sse, current path: " + path + " for server: " + serverName);
				}

				// Remove trailing sse and pass as sseEndpoint
				sseEndpoint = path;

				// Remove leading slash because WebClient's baseUrl already contains the
				// domain
				if (sseEndpoint.startsWith("/")) {
					sseEndpoint = sseEndpoint.substring(1);
				}

				// If empty after removing sse, use default path
				if (sseEndpoint.isEmpty()) {
					sseEndpoint = null;
				}
			}
			catch (java.net.MalformedURLException e) {
				logger.error("Invalid URL format: {} for server: {}", url, serverName, e);
				throw new IllegalArgumentException("Invalid URL format: " + url + " for server: " + serverName, e);
			}

			logger.info("Connecting to base URL: {}, SSE endpoint: {} for server: {}", baseUrl, sseEndpoint,
					serverName);

			// Create WebClient and add necessary request headers
			WebClient.Builder webClientBuilder = WebClient.builder()
				.baseUrl(baseUrl)
				.defaultHeader("Accept", "text/event-stream")
				.defaultHeader("Content-Type", "application/json")
				.defaultHeader("User-Agent", "MCP-Client/1.0.0");
			if (sseEndpoint != null && !sseEndpoint.isEmpty()) {
				transport = new WebFluxSseClientTransport(webClientBuilder, new ObjectMapper(), sseEndpoint);
			}
			else {
				transport = new WebFluxSseClientTransport(webClientBuilder, new ObjectMapper());
			}
			return configureMcpTransport(serverName, transport);

		}
		catch (Exception e) {
			logger.error("Failed to create SSE transport for server: {}", serverName, e);
			throw new IOException("Failed to create SSE transport for server: " + serverName, e);
		}
	}

	private McpServiceEntity createStudioConnection(McpConfigEntity mcpConfigEntity, String serverName)
			throws IOException {
		McpClientTransport transport = null;

		try (JsonParser jsonParser = new ObjectMapper().createParser(mcpConfigEntity.getConnectionConfig())) {
			McpServerConfig mcpServerConfig = jsonParser.readValueAs(McpServerConfig.class);

			// Extract and validate command parameters
			String command = mcpServerConfig.getCommand();
			if (command == null || command.trim().isEmpty()) {
				throw new IOException("Missing required 'command' field in server configuration for " + serverName);
			}

			command = command.trim();
			List<String> args = mcpServerConfig.getArgs();
			Map<String, String> env = mcpServerConfig.getEnv();

			logger.debug("Creating STUDIO connection for server: {} with command: {}", serverName, command);

			// Use Builder pattern to create ServerParameters instance
			ServerParameters.Builder builder = ServerParameters.builder(command);

			// Add parameters
			if (args != null && !args.isEmpty()) {
				builder.args(args);
				logger.debug("Added {} arguments for server: {}", args.size(), serverName);
			}

			// Add environment variables
			if (env != null && !env.isEmpty()) {
				builder.env(env);
				logger.debug("Added {} environment variables for server: {}", env.size(), serverName);
			}

			// Build ServerParameters instance
			ServerParameters serverParameters = builder.build();
			transport = new StdioClientTransport(serverParameters, new ObjectMapper());

			// Configure MCP client
			McpServiceEntity mcpServiceEntity = configureMcpTransport(serverName, transport);
			logger.info("STUDIO MCP Client configured successfully for server: {}", serverName);
			return mcpServiceEntity;

		}
		catch (Exception e) {
			logger.error("Error creating STUDIO transport for server: {}", serverName, e);
			throw new IOException(
					"Failed to create StdioClientTransport for server: " + serverName + ": " + e.getMessage(), e);
		}
	}

	private McpServiceEntity createStreamableConnection(McpConfigEntity mcpConfigEntity, String serverName)
			throws IOException {
		McpClientTransport transport = null;

		try (JsonParser jsonParser = new ObjectMapper().createParser(mcpConfigEntity.getConnectionConfig())) {
			McpServerConfig mcpServerConfig = jsonParser.readValueAs(McpServerConfig.class);

			// Validate URL configuration
			if (mcpServerConfig.getUrl() == null || mcpServerConfig.getUrl().trim().isEmpty()) {
				throw new IOException("Invalid or missing MCP server URL for server: " + serverName);
			}

			String url = mcpServerConfig.getUrl().trim();
			// 直接用完整url，不再拆分baseUrl/endpoint
			logger.info("Creating Streamable HTTP connection to full URL: {} for server: {}", url, serverName);

			// 创建WebClient时不再设置baseUrl，直接用完整url
			WebClient.Builder webClientBuilder = WebClient.builder()
				.defaultHeader("Accept", "application/json, text/event-stream")
				.defaultHeader("Content-Type", "application/json");

			transport = new StreamableHttpClientTransport(webClientBuilder, new ObjectMapper(), url);
			return configureMcpTransport(serverName, transport);
		}
		catch (Exception e) {
			logger.error("Failed to create Streamable HTTP transport for server: {}", serverName, e);
			throw new IOException("Failed to create Streamable HTTP transport for server: " + serverName, e);
		}
	}

	private McpServiceEntity configureMcpTransport(String mcpServerName, McpClientTransport transport)
			throws IOException {
		if (transport != null) {
			McpAsyncClient mcpAsyncClient = McpClient.async(transport)
				.clientInfo(new McpSchema.Implementation(mcpServerName, "1.0.0"))
				.build();

			// Retry mechanism: maximum 3 retries
			int maxRetries = 3;
			Exception lastException = null;

			for (int attempt = 1; attempt <= maxRetries; attempt++) {
				try {
					logger.debug("Attempting to initialize MCP transport for: {} (attempt {}/{})", mcpServerName,
							attempt, maxRetries);
					mcpAsyncClient.initialize()
						.timeout(Duration.ofSeconds(60)) // 增加到60秒
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
					logger.warn("Failed to initialize MCP transport for {} on attempt {}/{}: {}", mcpServerName,
							attempt, maxRetries, e.getMessage());

					if (attempt < maxRetries) {
						try {
							// Wait before retrying to avoid immediate retry
							Thread.sleep(1000 * attempt); // Incremental wait time: 1s,
							// 2s, 3s
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
		return null;
	}

	public void addMcpServer(McpConfigRequestVO mcpConfig) throws IOException {
		insertOrUpdateMcpRepo(mcpConfig);
		toolCallbackMapCache.invalidateAll();
	}

	public List<McpConfigEntity> insertOrUpdateMcpRepo(McpConfigRequestVO mcpConfigVO) throws IOException {
		List<McpConfigEntity> entityList = new ArrayList<>();
		try (JsonParser jsonParser = new ObjectMapper().createParser(mcpConfigVO.getConfigJson())) {
			McpServersConfig mcpServerConfig = jsonParser.readValueAs(McpServersConfig.class);
			String type = mcpConfigVO.getConnectionType();
			McpConfigType mcpConfigType = McpConfigType.valueOf(type);
			if (McpConfigType.STUDIO.equals(mcpConfigType)) {
				// STUDIO type connections require special handling
				mcpServerConfig.getMcpServers().forEach((name, config) -> {
					if (config.getCommand() == null || config.getCommand().isEmpty()) {
						throw new IllegalArgumentException(
								"Missing required 'command' field in server configuration for " + name);
					}
					if (config.getUrl() != null && !config.getUrl().isEmpty()) {
						throw new IllegalArgumentException(
								"STUDIO type should not have 'url' field in server configuration for " + name);
					}
				});
			}
			else if (McpConfigType.SSE.equals(mcpConfigType)) {
				// SSE type connections require special handling
				mcpServerConfig.getMcpServers().forEach((name, config) -> {
					if (config.getUrl() == null || config.getUrl().isEmpty()) {
						throw new IllegalArgumentException(
								"Missing required 'url' field in server configuration for " + name);
					}
					if (config.getCommand() != null && !config.getCommand().isEmpty()) {
						throw new IllegalArgumentException(
								"SSE type should not have 'command' field in server configuration for " + name);
					}
				});
			}
			else if (McpConfigType.STREAMING.equals(mcpConfigType)) {
				// STREAMING type connections require special handling
				mcpServerConfig.getMcpServers().forEach((name, config) -> {
					if (config.getUrl() == null || config.getUrl().isEmpty()) {
						throw new IllegalArgumentException(
								"Missing required 'url' field in server configuration for " + name);
					}
					if (config.getCommand() != null && !config.getCommand().isEmpty()) {
						throw new IllegalArgumentException(
								"STREAMING type should not have 'command' field in server configuration for " + name);
					}
				});
			}

			// Iterate through each MCP server configuration
			for (Map.Entry<String, McpServerConfig> entry : mcpServerConfig.getMcpServers().entrySet()) {
				String serverName = entry.getKey();
				McpServerConfig serverConfig = entry.getValue();

				// Use ServerConfig's toJson method to convert configuration to JSON
				// string
				String configJson = serverConfig.toJson();

				// Find the corresponding MCP configuration entity
				McpConfigEntity mcpConfigEntity = mcpConfigRepository.findByMcpServerName(serverName);
				if (mcpConfigEntity == null) {
					mcpConfigEntity = new McpConfigEntity();
					mcpConfigEntity.setConnectionConfig(configJson);
					mcpConfigEntity.setMcpServerName(serverName);
					mcpConfigEntity.setConnectionType(mcpConfigType);
				}
				else {
					mcpConfigEntity.setConnectionConfig(configJson);
					mcpConfigEntity.setConnectionType(mcpConfigType);
				}
				McpConfigEntity entity = mcpConfigRepository.save(mcpConfigEntity);
				entityList.add(entity);
				logger.info("MCP server '{}' has been saved to database.", serverName);

			}
		}
		return entityList;

	}

	public void removeMcpServer(long id) {
		Optional<McpConfigEntity> mcpConfigEntity = mcpConfigRepository.findById(id);
		mcpConfigEntity.ifPresent(this::removeMcpServer);
	}

	public void removeMcpServer(String mcpServerName) {
		var mcpConfig = mcpConfigRepository.findByMcpServerName(mcpServerName);
		removeMcpServer(mcpConfig);
	}

	private void removeMcpServer(McpConfigEntity mcpConfig) {
		if (null == mcpConfig) {
			return;
		}

		mcpConfigRepository.delete(mcpConfig);
		toolCallbackMapCache.invalidateAll();
	}

	public List<McpConfigEntity> getMcpServers() {
		return mcpConfigRepository.findAll();
	}

	public List<McpServiceEntity> getFunctionCallbacks(String planId) {
		try {
			return new ArrayList<>(
					this.toolCallbackMapCache.get(Optional.ofNullable(planId).orElse("DEFAULT")).values());
		}
		catch (Throwable t) {
			logger.error("Failed to get function callbacks for plan: {}", planId, t);
			return new ArrayList<>();
		}
	}

	public void close(String planId) {
		toolCallbackMapCache.invalidate(Optional.ofNullable(planId).orElse("DEFAULT"));
	}

}

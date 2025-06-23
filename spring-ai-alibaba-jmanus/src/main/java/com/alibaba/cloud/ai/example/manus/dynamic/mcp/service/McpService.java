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

import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.po.McpConfigEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.po.McpConfigType;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpConfigRequestVO;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpServerConfig;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpServersConfig;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpServiceEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.repository.McpConfigRepository;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class McpService {

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
				// 验证基础配置
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
						logger.warn("STREAMING connection type is not fully implemented yet for server: {}",
								serverName);
						throw new UnsupportedOperationException(
								"STREAMING connection type is not supported yet for server: " + serverName);
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
				// 根据需求决定是否继续处理其他服务器还是抛出异常
				// 这里选择继续处理其他服务器，但记录错误
				// 如果需要严格模式，可以取消注释下面的行
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

			// 验证URL配置
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

				// 检查URL路径是否以/sse结尾，如果不是则抛出错误
				String path = parsedUrl.getPath();
				if (path == null || !path.endsWith("/sse")) {
					throw new IllegalArgumentException("URL路径必须以/sse结尾，当前路径: " + path + " for server: " + serverName);
				}

				// 去掉尾部的sse，作为sseEndpoint传入
				sseEndpoint = path;

				// 移除开头的斜杠，因为WebClient的baseUrl已经包含了域名
				if (sseEndpoint.startsWith("/")) {
					sseEndpoint = sseEndpoint.substring(1);
				}

				// 如果去掉sse后为空，使用默认路径
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

			// 创建WebClient并添加必要的请求头
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

			// 提取和验证命令参数
			String command = mcpServerConfig.getCommand();
			if (command == null || command.trim().isEmpty()) {
				throw new IOException("Missing required 'command' field in server configuration for " + serverName);
			}

			command = command.trim();
			List<String> args = mcpServerConfig.getArgs();
			Map<String, String> env = mcpServerConfig.getEnv();

			logger.debug("Creating STUDIO connection for server: {} with command: {}", serverName, command);

			// 使用Builder模式创建ServerParameters实例
			ServerParameters.Builder builder = ServerParameters.builder(command);

			// 添加参数
			if (args != null && !args.isEmpty()) {
				builder.args(args);
				logger.debug("Added {} arguments for server: {}", args.size(), serverName);
			}

			// 添加环境变量
			if (env != null && !env.isEmpty()) {
				builder.env(env);
				logger.debug("Added {} environment variables for server: {}", env.size(), serverName);
			}

			// 构建ServerParameters实例
			ServerParameters serverParameters = builder.build();
			transport = new StdioClientTransport(serverParameters, new ObjectMapper());

			// 配置MCP客户端
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

	private McpServiceEntity configureMcpTransport(String mcpServerName, McpClientTransport transport)
			throws IOException {
		if (transport != null) {
			McpAsyncClient mcpAsyncClient = McpClient.async(transport)
				.clientInfo(new McpSchema.Implementation(mcpServerName, "1.0.0"))
				.build();

			// 重试机制：最多重试3次
			int maxRetries = 3;
			Exception lastException = null;

			for (int attempt = 1; attempt <= maxRetries; attempt++) {
				try {
					logger.debug("Attempting to initialize MCP transport for: {} (attempt {}/{})", mcpServerName,
							attempt, maxRetries);
					mcpAsyncClient.initialize().block(Duration.ofMinutes(2));
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
							// 重试前等待一段时间，避免立即重试
							Thread.sleep(1000 * attempt); // 递增等待时间：1s, 2s, 3s
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
				// STUDIO类型的连接需要特殊处理
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
				// SSE类型的连接需要特殊处理
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
				throw new UnsupportedOperationException("STREAMING connection type is not supported yet");
			}

			// 迭代处理每个MCP服务器配置
			for (Map.Entry<String, McpServerConfig> entry : mcpServerConfig.getMcpServers().entrySet()) {
				String serverName = entry.getKey();
				McpServerConfig serverConfig = entry.getValue();

				// 使用ServerConfig的toJson方法将配置转换为JSON字符串
				String configJson = serverConfig.toJson();

				// 查找对应的MCP配置实体
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

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
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.alibaba.cloud.ai.example.manus.dynamic.mcp.config.McpProperties;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.po.McpConfigType;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpServerConfig;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.transport.StreamableHttpClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;

/**
 * MCP传输构建器
 */
@Component
public class McpTransportBuilder {

	private static final Logger logger = LoggerFactory.getLogger(McpTransportBuilder.class);

	private final McpConfigValidator configValidator;

	private final McpProperties mcpProperties;

	private final ObjectMapper objectMapper;

	public McpTransportBuilder(McpConfigValidator configValidator, McpProperties mcpProperties,
			ObjectMapper objectMapper) {
		this.configValidator = configValidator;
		this.mcpProperties = mcpProperties;
		this.objectMapper = objectMapper;
	}

	/**
	 * 构建MCP传输
	 * @param configType 配置类型
	 * @param serverConfig 服务器配置
	 * @param serverName 服务器名称
	 * @return MCP客户端传输
	 * @throws IOException 构建失败时抛出异常
	 */
	public McpClientTransport buildTransport(McpConfigType configType, McpServerConfig serverConfig, String serverName)
			throws IOException {
		// 验证服务器配置
		configValidator.validateServerConfig(serverConfig, serverName);

		switch (configType) {
			case SSE -> {
				return buildSseTransport(serverConfig, serverName);
			}
			case STUDIO -> {
				return buildStudioTransport(serverConfig, serverName);
			}
			case STREAMING -> {
				return buildStreamingTransport(serverConfig, serverName);
			}
			default -> {
				throw new IOException("Unsupported connection type: " + configType + " for server: " + serverName);
			}
		}
	}

	/**
	 * 构建SSE传输
	 * @param serverConfig 服务器配置
	 * @param serverName 服务器名称
	 * @return SSE传输
	 * @throws IOException 构建失败时抛出异常
	 */
	private McpClientTransport buildSseTransport(McpServerConfig serverConfig, String serverName) throws IOException {
		String url = serverConfig.getUrl().trim();
		configValidator.validateSseUrl(url, serverName);

		URL parsedUrl = new URL(url);
		String baseUrl = parsedUrl.getProtocol() + "://" + parsedUrl.getHost()
				+ (parsedUrl.getPort() == -1 ? "" : ":" + parsedUrl.getPort());

		String path = parsedUrl.getPath();
		String sseEndpoint = path;

		// 移除前导斜杠
		if (sseEndpoint.startsWith("/")) {
			sseEndpoint = sseEndpoint.substring(1);
		}

		// 如果为空则设为null
		if (sseEndpoint.isEmpty()) {
			sseEndpoint = null;
		}

		logger.info("Building SSE transport for server: {} with baseUrl: {}, endpoint: {}", serverName, baseUrl,
				sseEndpoint);

		WebClient.Builder webClientBuilder = createWebClientBuilder(baseUrl);

		if (sseEndpoint != null && !sseEndpoint.isEmpty()) {
			return new WebFluxSseClientTransport(webClientBuilder, objectMapper, sseEndpoint);
		}
		else {
			return new WebFluxSseClientTransport(webClientBuilder, objectMapper);
		}
	}

	/**
	 * 构建STUDIO传输
	 * @param serverConfig 服务器配置
	 * @param serverName 服务器名称
	 * @return STUDIO传输
	 * @throws IOException 构建失败时抛出异常
	 */
	private McpClientTransport buildStudioTransport(McpServerConfig serverConfig, String serverName)
			throws IOException {
		String command = serverConfig.getCommand().trim();
		List<String> args = serverConfig.getArgs();
		Map<String, String> env = serverConfig.getEnv();

		logger.debug("Building STUDIO transport for server: {} with command: {}", serverName, command);

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

		ServerParameters serverParameters = builder.build();
		return new StdioClientTransport(serverParameters, objectMapper);
	}

	/**
	 * 构建STREAMING传输
	 * @param serverConfig 服务器配置
	 * @param serverName 服务器名称
	 * @return STREAMING传输
	 * @throws IOException 构建失败时抛出异常
	 */
	private McpClientTransport buildStreamingTransport(McpServerConfig serverConfig, String serverName)
			throws IOException {
		String url = serverConfig.getUrl().trim();
		configValidator.validateUrl(url, serverName);

		logger.info("Building Streamable HTTP transport for server: {} with URL: {}", serverName, url);

		WebClient.Builder webClientBuilder = createWebClientBuilder();
		return new StreamableHttpClientTransport(webClientBuilder, objectMapper, url);
	}

	/**
	 * 创建WebClient构建器（带baseUrl）
	 * @param baseUrl 基础URL
	 * @return WebClient构建器
	 */
	private WebClient.Builder createWebClientBuilder(String baseUrl) {
		return WebClient.builder()
			.baseUrl(baseUrl)
			.defaultHeader("Accept", "text/event-stream")
			.defaultHeader("Content-Type", "application/json")
			.defaultHeader("User-Agent", mcpProperties.getUserAgent());
	}

	/**
	 * 创建WebClient构建器（不带baseUrl）
	 * @return WebClient构建器
	 */
	private WebClient.Builder createWebClientBuilder() {
		return WebClient.builder()
			.defaultHeader("Accept", "application/json, text/event-stream")
			.defaultHeader("Content-Type", "application/json");
	}

}

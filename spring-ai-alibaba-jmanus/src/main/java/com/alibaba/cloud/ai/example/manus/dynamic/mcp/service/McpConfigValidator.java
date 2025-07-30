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
import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alibaba.cloud.ai.example.manus.dynamic.mcp.config.McpProperties;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.po.McpConfigEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.po.McpConfigStatus;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpServerConfig;

/**
 * MCP配置验证器
 */
@Component
public class McpConfigValidator {

	private static final Logger logger = LoggerFactory.getLogger(McpConfigValidator.class);

	private final McpProperties mcpProperties;

	public McpConfigValidator(McpProperties mcpProperties) {
		this.mcpProperties = mcpProperties;
	}

	/**
	 * 验证MCP配置实体
	 * @param mcpConfigEntity MCP配置实体
	 * @throws IOException 验证失败时抛出异常
	 */
	public void validateMcpConfigEntity(McpConfigEntity mcpConfigEntity) throws IOException {
		String serverName = mcpConfigEntity.getMcpServerName();

		// 验证服务器名称
		if (serverName == null || serverName.trim().isEmpty()) {
			throw new IOException("Server name is required");
		}

		// 验证连接类型
		if (mcpConfigEntity.getConnectionType() == null) {
			throw new IOException("Connection type is required for server: " + serverName);
		}

		// 验证连接配置
		if (mcpConfigEntity.getConnectionConfig() == null || mcpConfigEntity.getConnectionConfig().trim().isEmpty()) {
			throw new IOException("Connection config is required for server: " + serverName);
		}

		logger.debug("MCP config entity validation passed for server: {}", serverName);
	}

	/**
	 * 验证服务器配置
	 * @param serverConfig 服务器配置
	 * @param serverName 服务器名称
	 * @throws IOException 验证失败时抛出异常
	 */
	public void validateServerConfig(McpServerConfig serverConfig, String serverName) throws IOException {
		if (serverConfig == null) {
			throw new IOException("Server config is null for server: " + serverName);
		}

		// 根据连接类型验证必需字段
		if (serverConfig.getCommand() != null && !serverConfig.getCommand().trim().isEmpty()) {
			// STUDIO类型：验证command
			validateCommand(serverConfig.getCommand(), serverName);
		}
		else {
			// SSE/STREAMING类型：验证URL
			validateUrl(serverConfig.getUrl(), serverName);
		}

		logger.debug("Server config validation passed for server: {}", serverName);
	}

	/**
	 * 验证命令配置
	 * @param command 命令
	 * @param serverName 服务器名称
	 * @throws IOException 验证失败时抛出异常
	 */
	public void validateCommand(String command, String serverName) throws IOException {
		if (command == null || command.trim().isEmpty()) {
			throw new IOException("Missing required 'command' field in server configuration for " + serverName);
		}
	}

	/**
	 * 验证URL配置
	 * @param url URL
	 * @param serverName 服务器名称
	 * @throws IOException 验证失败时抛出异常
	 */
	public void validateUrl(String url, String serverName) throws IOException {
		if (url == null || url.trim().isEmpty()) {
			throw new IOException("Invalid or missing MCP server URL for server: " + serverName);
		}

		try {
			new URL(url.trim());
		}
		catch (MalformedURLException e) {
			throw new IOException("Invalid URL format: " + url + " for server: " + serverName, e);
		}
	}

	/**
	 * 验证SSE URL格式
	 * @param url URL
	 * @param serverName 服务器名称
	 * @throws IOException 验证失败时抛出异常
	 */
	public void validateSseUrl(String url, String serverName) throws IOException {
		validateUrl(url, serverName);

		try {
			URL parsedUrl = new URL(url.trim());
			String path = parsedUrl.getPath();

			// 检查路径是否包含sse
			boolean pathContainsSse = path != null && path.toLowerCase().contains("sse");

			if (!pathContainsSse) {
				throw new IOException("URL must contain 'sse' in path for SSE connection. " + "Current URL: " + url
						+ " for server: " + serverName);
			}
		}
		catch (MalformedURLException e) {
			throw new IOException("Invalid URL format: " + url + " for server: " + serverName, e);
		}
	}

	/**
	 * 检查配置是否启用
	 * @param mcpConfigEntity MCP配置实体
	 * @return true如果启用，false如果禁用
	 */
	public boolean isEnabled(McpConfigEntity mcpConfigEntity) {
		return mcpConfigEntity.getStatus() != null && mcpConfigEntity.getStatus() == McpConfigStatus.ENABLE;
	}

	/**
	 * 验证服务器名称是否已存在
	 * @param serverName 服务器名称
	 * @param existingServer 已存在的服务器
	 * @throws IOException 如果服务器名称已存在
	 */
	public void validateServerNameNotExists(String serverName, Object existingServer) throws IOException {
		if (existingServer != null) {
			throw new IOException("MCP server with name '" + serverName + "' already exists");
		}
	}

	/**
	 * 验证服务器是否存在
	 * @param serverName 服务器名称
	 * @param existingServer 已存在的服务器
	 * @throws IOException 如果服务器不存在
	 */
	public void validateServerExists(String serverName, Object existingServer) throws IOException {
		if (existingServer == null) {
			throw new IOException("MCP server not found with name: " + serverName);
		}
	}

}

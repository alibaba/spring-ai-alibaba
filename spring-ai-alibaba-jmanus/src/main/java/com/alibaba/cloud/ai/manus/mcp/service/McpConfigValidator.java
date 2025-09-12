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
import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alibaba.cloud.ai.manus.mcp.config.McpProperties;
import com.alibaba.cloud.ai.manus.mcp.model.po.McpConfigEntity;
import com.alibaba.cloud.ai.manus.mcp.model.po.McpConfigStatus;
import com.alibaba.cloud.ai.manus.mcp.model.vo.McpServerConfig;

/**
 * MCP configuration validator
 */
@Component
public class McpConfigValidator {

	private static final Logger logger = LoggerFactory.getLogger(McpConfigValidator.class);

	private final McpProperties mcpProperties;

	public McpConfigValidator(McpProperties mcpProperties) {
		this.mcpProperties = mcpProperties;
	}

	/**
	 * Validate MCP configuration entity
	 * @param mcpConfigEntity MCP configuration entity
	 * @throws IOException Thrown when validation fails
	 */
	public void validateMcpConfigEntity(McpConfigEntity mcpConfigEntity) throws IOException {
		String serverName = mcpConfigEntity.getMcpServerName();

		// Validate server name
		if (serverName == null || serverName.trim().isEmpty()) {
			throw new IOException("Server name is required");
		}

		// Validate connection type
		if (mcpConfigEntity.getConnectionType() == null) {
			throw new IOException("Connection type is required for server: " + serverName);
		}

		// Validate connection configuration
		if (mcpConfigEntity.getConnectionConfig() == null || mcpConfigEntity.getConnectionConfig().trim().isEmpty()) {
			throw new IOException("Connection config is required for server: " + serverName);
		}

		logger.debug("MCP config entity validation passed for server: {}", serverName);
	}

	/**
	 * Validate server configuration
	 * @param serverConfig Server configuration
	 * @param serverName Server name
	 * @throws IOException Thrown when validation fails
	 */
	public void validateServerConfig(McpServerConfig serverConfig, String serverName) throws IOException {
		if (serverConfig == null) {
			throw new IOException("Server config is null for server: " + serverName);
		}

		// Validate required fields based on connection type
		if (serverConfig.getCommand() != null && !serverConfig.getCommand().trim().isEmpty()) {
			// STUDIO type: validate command
			validateCommand(serverConfig.getCommand(), serverName);
		}
		else {
			// SSE/STREAMING type: validate URL
			validateUrl(serverConfig.getUrl(), serverName);
		}

		logger.debug("Server config validation passed for server: {}", serverName);
	}

	/**
	 * Validate command configuration
	 * @param command Command
	 * @param serverName Server name
	 * @throws IOException Thrown when validation fails
	 */
	public void validateCommand(String command, String serverName) throws IOException {
		if (command == null || command.trim().isEmpty()) {
			throw new IOException("Missing required 'command' field in server configuration for " + serverName);
		}
	}

	/**
	 * Validate URL configuration
	 * @param url URL
	 * @param serverName Server name
	 * @throws IOException Thrown when validation fails
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
	 * Validate SSE URL format
	 * @param url URL
	 * @param serverName Server name
	 * @throws IOException Thrown when validation fails
	 */
	public void validateSseUrl(String url, String serverName) throws IOException {
		validateUrl(url, serverName);

		try {
			URL parsedUrl = new URL(url.trim());
			String path = parsedUrl.getPath();

			// Check if path contains sse
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
	 * Check if configuration is enabled
	 * @param mcpConfigEntity MCP configuration entity
	 * @return true if enabled, false if disabled
	 */
	public boolean isEnabled(McpConfigEntity mcpConfigEntity) {
		return mcpConfigEntity.getStatus() != null && mcpConfigEntity.getStatus() == McpConfigStatus.ENABLE;
	}

	/**
	 * Validate if server name already exists
	 * @param serverName Server name
	 * @param existingServer Existing server
	 * @throws IOException If server name already exists
	 */
	public void validateServerNameNotExists(String serverName, Object existingServer) throws IOException {
		if (existingServer != null) {
			throw new IOException("MCP server with name '" + serverName + "' already exists");
		}
	}

	/**
	 * Validate if server exists
	 * @param serverName Server name
	 * @param existingServer Existing server
	 * @throws IOException If server does not exist
	 */
	public void validateServerExists(String serverName, Object existingServer) throws IOException {
		if (existingServer == null) {
			throw new IOException("MCP server not found with name: " + serverName);
		}
	}

}

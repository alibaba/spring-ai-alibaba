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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alibaba.cloud.ai.manus.mcp.model.po.McpConfigEntity;
import com.alibaba.cloud.ai.manus.mcp.model.po.McpConfigStatus;
import com.alibaba.cloud.ai.manus.mcp.model.po.McpConfigType;
import com.alibaba.cloud.ai.manus.mcp.model.vo.McpServerConfig;
import com.alibaba.cloud.ai.manus.mcp.model.vo.McpServerRequestVO;
import com.alibaba.cloud.ai.manus.mcp.model.vo.McpServiceEntity;
import com.alibaba.cloud.ai.manus.mcp.repository.McpConfigRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * MCP service main class (refactored) responsible for coordinating components and
 * providing unified business interface
 */
@Component
public class McpService implements IMcpService {

	private static final Logger logger = LoggerFactory.getLogger(McpService.class);

	private final McpConfigRepository mcpConfigRepository;

	private final McpConfigValidator configValidator;

	private final McpCacheManager cacheManager;

	private final ObjectMapper objectMapper;

	public McpService(McpConfigRepository mcpConfigRepository, McpConfigValidator configValidator,
			McpCacheManager cacheManager, ObjectMapper objectMapper) {
		this.mcpConfigRepository = mcpConfigRepository;
		this.configValidator = configValidator;
		this.cacheManager = cacheManager;
		this.objectMapper = objectMapper;
	}

	/**
	 * Batch save MCP server configurations
	 * @param configJson MCP configuration JSON string
	 * @return Configuration entity list
	 * @throws IOException IO exception
	 */
	@Override
	public List<McpConfigEntity> saveMcpServers(String configJson) throws IOException {
		List<McpConfigEntity> entityList = new ArrayList<>();

		JsonNode jsonNode = objectMapper.readTree(configJson);

		// Check if contains mcpServers field
		if (!jsonNode.has("mcpServers")) {
			throw new IllegalArgumentException("Missing 'mcpServers' field in JSON configuration");
		}

		JsonNode mcpServersNode = jsonNode.get("mcpServers");
		if (!mcpServersNode.isObject()) {
			throw new IllegalArgumentException("'mcpServers' must be an object");
		}

		// Parse directly as Map<String, McpServerConfig>
		Map<String, McpServerConfig> mcpServers = objectMapper.convertValue(mcpServersNode,
				new TypeReference<Map<String, McpServerConfig>>() {
				});

		// Iterate through each MCP server configuration
		for (Map.Entry<String, McpServerConfig> entry : mcpServers.entrySet()) {
			String serverName = entry.getKey();
			McpServerConfig serverConfig = entry.getValue();

			// Validate server configuration
			configValidator.validateServerConfig(serverConfig, serverName);

			// Get connection type
			McpConfigType connectionType = serverConfig.getConnectionType();
			logger.info("Using connection type for server '{}': {}", serverName, connectionType);

			// Convert to JSON
			String serverConfigJson = serverConfig.toJson();

			// Find or create entity
			McpConfigEntity mcpConfigEntity = mcpConfigRepository.findByMcpServerName(serverName);
			if (mcpConfigEntity == null) {
				mcpConfigEntity = new McpConfigEntity();
				mcpConfigEntity.setConnectionConfig(serverConfigJson);
				mcpConfigEntity.setMcpServerName(serverName);
				mcpConfigEntity.setConnectionType(connectionType);
				// Set status, use from serverConfig if available, otherwise use default
				// value
				if (serverConfig.getStatus() != null) {
					mcpConfigEntity.setStatus(serverConfig.getStatus());
				}
				else {
					mcpConfigEntity.setStatus(McpConfigStatus.ENABLE);
				}
			}
			else {
				mcpConfigEntity.setConnectionConfig(serverConfigJson);
				mcpConfigEntity.setConnectionType(connectionType);
				// Update status, use from serverConfig if available, otherwise keep
				// original value
				if (serverConfig.getStatus() != null) {
					mcpConfigEntity.setStatus(serverConfig.getStatus());
				}
			}

			McpConfigEntity entity = mcpConfigRepository.save(mcpConfigEntity);
			entityList.add(entity);
			logger.info("MCP server '{}' has been saved to database with connection type: {}", serverName,
					connectionType);
		}

		// Clear cache to reload services
		cacheManager.invalidateAllCache();
		return entityList;
	}

	/**
	 * Save single MCP server configuration
	 * @param requestVO MCP server form request
	 * @return Configuration entity
	 * @throws IOException IO exception
	 */
	@Override
	public McpConfigEntity saveMcpServer(McpServerRequestVO requestVO) throws IOException {
		// Validate request data
		List<String> validationErrors = requestVO.validateWithDetails();
		if (!validationErrors.isEmpty()) {
			String errorMessage = "MCP server configuration validation failed: " + String.join("; ", validationErrors);
			throw new IllegalArgumentException(errorMessage);
		}

		// Build server configuration
		McpServerConfig serverConfig = new McpServerConfig(objectMapper);
		serverConfig.setCommand(requestVO.getCommand());
		serverConfig.setUrl(requestVO.getUrl());
		serverConfig.setArgs(requestVO.getArgs());
		serverConfig.setEnv(requestVO.getEnv());

		// Set status
		if (requestVO.getStatus() != null) {
			serverConfig.setStatus(McpConfigStatus.valueOf(requestVO.getStatus()));
		}

		// Validate server configuration
		configValidator.validateServerConfig(serverConfig, requestVO.getMcpServerName());

		// Get connection type
		McpConfigType connectionType = serverConfig.getConnectionType();
		logger.info("Using connection type for server '{}': {}", requestVO.getMcpServerName(), connectionType);

		// Convert to JSON
		String configJson = serverConfig.toJson();

		// Find or create entity
		McpConfigEntity mcpConfigEntity;
		if (requestVO.isUpdate()) {
			// Update mode
			Optional<McpConfigEntity> existingEntity = mcpConfigRepository.findById(requestVO.getId());
			if (existingEntity.isEmpty()) {
				throw new IllegalArgumentException("MCP server not found with id: " + requestVO.getId());
			}
			mcpConfigEntity = existingEntity.get();
		}
		else {
			// Add mode - check if server name already exists
			McpConfigEntity existingServer = mcpConfigRepository.findByMcpServerName(requestVO.getMcpServerName());
			configValidator.validateServerNameNotExists(requestVO.getMcpServerName(), existingServer);
			mcpConfigEntity = new McpConfigEntity();
		}

		// Update entity
		mcpConfigEntity.setMcpServerName(requestVO.getMcpServerName());
		mcpConfigEntity.setConnectionConfig(configJson);
		mcpConfigEntity.setConnectionType(connectionType);
		mcpConfigEntity.setStatus(serverConfig.getStatus());

		// Save to database
		McpConfigEntity savedEntity = mcpConfigRepository.save(mcpConfigEntity);
		logger.info("MCP server '{}' has been saved to database with connection type: {}", requestVO.getMcpServerName(),
				connectionType);

		// Clear cache to reload services
		cacheManager.invalidateAllCache();

		return savedEntity;
	}

	/**
	 * Delete MCP server (by ID)
	 * @param id Server ID
	 */
	@Override
	public void removeMcpServer(long id) {
		removeMcpServer((Object) id);
	}

	/**
	 * Delete MCP server (by name)
	 * @param mcpServerName Server name
	 */
	@Override
	public void removeMcpServer(String mcpServerName) {
		removeMcpServer((Object) mcpServerName);
	}

	/**
	 * Delete MCP server (generic method)
	 * @param identifier Server ID (Long) or server name (String)
	 */
	private void removeMcpServer(Object identifier) {
		McpConfigEntity mcpConfig = null;

		if (identifier instanceof Long id) {
			Optional<McpConfigEntity> optionalEntity = mcpConfigRepository.findById(id);
			mcpConfig = optionalEntity.orElse(null);
		}
		else if (identifier instanceof String serverName) {
			mcpConfig = mcpConfigRepository.findByMcpServerName(serverName);
		}
		else {
			throw new IllegalArgumentException("Identifier must be Long (ID) or String (server name)");
		}

		if (mcpConfig != null) {
			mcpConfigRepository.delete(mcpConfig);
			cacheManager.invalidateAllCache();
			logger.info("MCP server '{}' has been removed", mcpConfig.getMcpServerName());
		}
		else {
			logger.warn("MCP server not found for identifier: {}", identifier);
		}
	}

	/**
	 * Get all MCP server configurations
	 * @return MCP configuration entity list
	 */
	@Override
	public List<McpConfigEntity> getMcpServers() {
		return mcpConfigRepository.findAll();
	}

	/**
	 * Find MCP configuration by ID
	 * @param id MCP configuration ID
	 * @return Optional MCP configuration entity
	 */
	public Optional<McpConfigEntity> findById(Long id) {
		return mcpConfigRepository.findById(id);
	}

	/**
	 * Get MCP service entity list
	 * @param planId Plan ID
	 * @return MCP service entity list
	 */
	@Override
	public List<McpServiceEntity> getFunctionCallbacks(String planId) {
		return cacheManager.getServiceEntities(planId);
	}

	/**
	 * Close MCP service for specified plan
	 * @param planId Plan ID
	 */
	@Override
	public void close(String planId) {
		cacheManager.invalidateCache(planId);
	}

	/**
	 * Enable MCP server
	 * @param id MCP server ID
	 * @return true if enabled successfully, false otherwise
	 */
	public boolean enableMcpServer(Long id) {
		return updateMcpServerStatus(id, McpConfigStatus.ENABLE);
	}

	/**
	 * Disable MCP server
	 * @param id MCP server ID
	 * @return true if disabled successfully, false otherwise
	 */
	public boolean disableMcpServer(Long id) {
		return updateMcpServerStatus(id, McpConfigStatus.DISABLE);
	}

	/**
	 * Update MCP server status
	 * @param id MCP server ID
	 * @param status Target status
	 * @return true if updated successfully, false otherwise
	 */
	@Override
	public boolean updateMcpServerStatus(Long id, McpConfigStatus status) {
		Optional<McpConfigEntity> optionalEntity = mcpConfigRepository.findById(id);
		if (optionalEntity.isEmpty()) {
			throw new IllegalArgumentException("MCP server not found with id: " + id);
		}

		McpConfigEntity entity = optionalEntity.get();
		if (entity.getStatus() == status) {
			logger.info("MCP server {} is already {}", entity.getMcpServerName(), status);
			return true;
		}

		try {
			entity.setStatus(status);
			mcpConfigRepository.save(entity);

			// Clear cache to reload services
			cacheManager.invalidateAllCache();

			logger.info("MCP server {} {} successfully", entity.getMcpServerName(), status);
			return true;
		}
		catch (Exception e) {
			logger.error("Failed to {} MCP server {}: {}", status, entity.getMcpServerName(), e.getMessage(), e);
			return false;
		}
	}

}

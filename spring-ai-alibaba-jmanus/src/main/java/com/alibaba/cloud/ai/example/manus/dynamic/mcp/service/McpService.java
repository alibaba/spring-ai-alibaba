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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.po.McpConfigEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.po.McpConfigStatus;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.po.McpConfigType;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpServerConfig;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpServiceEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpServerRequestVO;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.repository.McpConfigRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * MCP服务主类（重构后） 负责协调各个组件，提供统一的业务接口
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
	 * 批量保存MCP服务器配置
	 * @param configJson MCP配置JSON字符串
	 * @return 配置实体列表
	 * @throws IOException IO异常
	 */
	@Override
	public List<McpConfigEntity> saveMcpServers(String configJson) throws IOException {
		List<McpConfigEntity> entityList = new ArrayList<>();

		JsonNode jsonNode = objectMapper.readTree(configJson);

		// 检查是否包含mcpServers字段
		if (!jsonNode.has("mcpServers")) {
			throw new IllegalArgumentException("Missing 'mcpServers' field in JSON configuration");
		}

		JsonNode mcpServersNode = jsonNode.get("mcpServers");
		if (!mcpServersNode.isObject()) {
			throw new IllegalArgumentException("'mcpServers' must be an object");
		}

		// 直接解析为Map<String, McpServerConfig>
		Map<String, McpServerConfig> mcpServers = objectMapper.convertValue(mcpServersNode,
				new TypeReference<Map<String, McpServerConfig>>() {
				});

		// 遍历每个MCP服务器配置
		for (Map.Entry<String, McpServerConfig> entry : mcpServers.entrySet()) {
			String serverName = entry.getKey();
			McpServerConfig serverConfig = entry.getValue();

			// 验证服务器配置
			configValidator.validateServerConfig(serverConfig, serverName);

			// 获取连接类型
			McpConfigType connectionType = serverConfig.getConnectionType();
			logger.info("Using connection type for server '{}': {}", serverName, connectionType);

			// 转换为JSON
			String serverConfigJson = serverConfig.toJson();

			// 查找或创建实体
			McpConfigEntity mcpConfigEntity = mcpConfigRepository.findByMcpServerName(serverName);
			if (mcpConfigEntity == null) {
				mcpConfigEntity = new McpConfigEntity();
				mcpConfigEntity.setConnectionConfig(serverConfigJson);
				mcpConfigEntity.setMcpServerName(serverName);
				mcpConfigEntity.setConnectionType(connectionType);
				// 设置status，如果serverConfig中有status则使用，否则使用默认值
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
				// 更新status，如果serverConfig中有status则使用，否则保持原值
				if (serverConfig.getStatus() != null) {
					mcpConfigEntity.setStatus(serverConfig.getStatus());
				}
			}

			McpConfigEntity entity = mcpConfigRepository.save(mcpConfigEntity);
			entityList.add(entity);
			logger.info("MCP server '{}' has been saved to database with connection type: {}", serverName,
					connectionType);
		}

		// 清除缓存以重新加载服务
		cacheManager.invalidateAllCache();
		return entityList;
	}

	/**
	 * 保存单个MCP服务器配置
	 * @param requestVO MCP服务器表单请求
	 * @return 配置实体
	 * @throws IOException IO异常
	 */
	@Override
	public McpConfigEntity saveMcpServer(McpServerRequestVO requestVO) throws IOException {
		// 验证请求数据
		List<String> validationErrors = requestVO.validateWithDetails();
		if (!validationErrors.isEmpty()) {
			String errorMessage = "MCP服务器配置验证失败: " + String.join("; ", validationErrors);
			throw new IllegalArgumentException(errorMessage);
		}

		// 构建服务器配置
		McpServerConfig serverConfig = new McpServerConfig();
		serverConfig.setCommand(requestVO.getCommand());
		serverConfig.setUrl(requestVO.getUrl());
		serverConfig.setArgs(requestVO.getArgs());
		serverConfig.setEnv(requestVO.getEnv());

		// 设置状态
		if (requestVO.getStatus() != null) {
			serverConfig.setStatus(McpConfigStatus.valueOf(requestVO.getStatus()));
		}

		// 验证服务器配置
		configValidator.validateServerConfig(serverConfig, requestVO.getMcpServerName());

		// 获取连接类型
		McpConfigType connectionType = serverConfig.getConnectionType();
		logger.info("Using connection type for server '{}': {}", requestVO.getMcpServerName(), connectionType);

		// 转换为JSON
		String configJson = serverConfig.toJson();

		// 查找或创建实体
		McpConfigEntity mcpConfigEntity;
		if (requestVO.isUpdate()) {
			// 更新模式
			Optional<McpConfigEntity> existingEntity = mcpConfigRepository.findById(requestVO.getId());
			if (existingEntity.isEmpty()) {
				throw new IllegalArgumentException("MCP server not found with id: " + requestVO.getId());
			}
			mcpConfigEntity = existingEntity.get();
		}
		else {
			// 新增模式 - 检查服务器名称是否已存在
			McpConfigEntity existingServer = mcpConfigRepository.findByMcpServerName(requestVO.getMcpServerName());
			configValidator.validateServerNameNotExists(requestVO.getMcpServerName(), existingServer);
			mcpConfigEntity = new McpConfigEntity();
		}

		// 更新实体
		mcpConfigEntity.setMcpServerName(requestVO.getMcpServerName());
		mcpConfigEntity.setConnectionConfig(configJson);
		mcpConfigEntity.setConnectionType(connectionType);
		mcpConfigEntity.setStatus(serverConfig.getStatus());

		// 保存到数据库
		McpConfigEntity savedEntity = mcpConfigRepository.save(mcpConfigEntity);
		logger.info("MCP server '{}' has been saved to database with connection type: {}", requestVO.getMcpServerName(),
				connectionType);

		// 清除缓存以重新加载服务
		cacheManager.invalidateAllCache();

		return savedEntity;
	}

	/**
	 * 删除MCP服务器（通过ID）
	 * @param id 服务器ID
	 */
	@Override
	public void removeMcpServer(long id) {
		removeMcpServer((Object) id);
	}

	/**
	 * 删除MCP服务器（通过名称）
	 * @param mcpServerName 服务器名称
	 */
	@Override
	public void removeMcpServer(String mcpServerName) {
		removeMcpServer((Object) mcpServerName);
	}

	/**
	 * 删除MCP服务器（通用方法）
	 * @param identifier 服务器ID（Long）或服务器名称（String）
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
	 * 获取所有MCP服务器配置
	 * @return MCP配置实体列表
	 */
	@Override
	public List<McpConfigEntity> getMcpServers() {
		return mcpConfigRepository.findAll();
	}

	/**
	 * 根据ID查找MCP配置
	 * @param id MCP配置ID
	 * @return 可选的MCP配置实体
	 */
	public Optional<McpConfigEntity> findById(Long id) {
		return mcpConfigRepository.findById(id);
	}

	/**
	 * 获取MCP服务实体列表
	 * @param planId 计划ID
	 * @return MCP服务实体列表
	 */
	@Override
	public List<McpServiceEntity> getFunctionCallbacks(String planId) {
		return cacheManager.getServiceEntities(planId);
	}

	/**
	 * 关闭指定计划的MCP服务
	 * @param planId 计划ID
	 */
	@Override
	public void close(String planId) {
		cacheManager.invalidateCache(planId);
	}

	/**
	 * 启用MCP服务器
	 * @param id MCP服务器ID
	 * @return true if enabled successfully, false otherwise
	 */
	public boolean enableMcpServer(Long id) {
		return updateMcpServerStatus(id, McpConfigStatus.ENABLE);
	}

	/**
	 * 禁用MCP服务器
	 * @param id MCP服务器ID
	 * @return true if disabled successfully, false otherwise
	 */
	public boolean disableMcpServer(Long id) {
		return updateMcpServerStatus(id, McpConfigStatus.DISABLE);
	}

	/**
	 * 更新MCP服务器状态
	 * @param id MCP服务器ID
	 * @param status 目标状态
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

			// 清除缓存以重新加载服务
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

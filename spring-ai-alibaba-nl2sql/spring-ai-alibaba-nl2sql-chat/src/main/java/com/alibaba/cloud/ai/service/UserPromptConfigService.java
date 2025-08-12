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

package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.dto.PromptConfigDTO;
import com.alibaba.cloud.ai.entity.UserPromptConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户提示词优化配置管理服务 提供提示词优化配置的增删改查功能，支持多个优化配置同时生效
 *
 * @author Makoto
 */
@Service
public class UserPromptConfigService {

	private static final Logger logger = LoggerFactory.getLogger(UserPromptConfigService.class);

	/**
	 * 内存存储，实际项目中可以替换为数据库存储
	 */
	private final Map<String, UserPromptConfig> configStorage = new ConcurrentHashMap<>();

	/**
	 * 根据提示词类型存储启用的配置ID列表（支持多个配置同时启用）
	 */
	private final Map<String, List<String>> promptTypeToConfigIds = new ConcurrentHashMap<>();

	/**
	 * 创建或更新提示词优化配置
	 * @param configDTO 配置数据传输对象
	 * @return 保存后的配置对象
	 */
	public UserPromptConfig saveOrUpdateConfig(PromptConfigDTO configDTO) {
		logger.info("保存或更新提示词优化配置：{}", configDTO);

		UserPromptConfig config;
		if (configDTO.id() != null && configStorage.containsKey(configDTO.id())) {
			// 更新现有配置
			config = configStorage.get(configDTO.id());
			config.setName(configDTO.name());
			config.setOptimizationPrompt(configDTO.optimizationPrompt());
			config.setEnabled(configDTO.enabled());
			config.setDescription(configDTO.description());
			config.setUpdateTime(LocalDateTime.now());
		}
		else {
			// 创建新配置
			config = new UserPromptConfig();
			config.setId(UUID.randomUUID().toString());
			config.setName(configDTO.name());
			config.setPromptType(configDTO.promptType());
			config.setOptimizationPrompt(configDTO.optimizationPrompt());
			config.setEnabled(configDTO.enabled());
			config.setDescription(configDTO.description());
			config.setCreator(configDTO.creator());
		}

		configStorage.put(config.getId(), config);

		// 更新类型映射（支持多个配置）
		updatePromptTypeMapping(config);

		return config;
	}

	/**
	 * 根据ID获取配置
	 * @param id 配置ID
	 * @return 配置对象，不存在时返回null
	 */
	public UserPromptConfig getConfigById(String id) {
		return configStorage.get(id);
	}

	/**
	 * 根据提示词类型获取所有启用的配置
	 * @param promptType 提示词类型
	 * @return 配置列表
	 */
	public List<UserPromptConfig> getActiveConfigsByType(String promptType) {
		List<String> configIds = promptTypeToConfigIds.get(promptType);
		if (configIds == null || configIds.isEmpty()) {
			return new ArrayList<>();
		}

		return configIds.stream()
			.map(configStorage::get)
			.filter(Objects::nonNull)
			.filter(config -> Boolean.TRUE.equals(config.getEnabled()))
			.sorted(Comparator.comparing(UserPromptConfig::getUpdateTime).reversed())
			.toList();
	}

	/**
	 * 根据提示词类型获取启用的配置（兼容旧接口）
	 * @param promptType 提示词类型
	 * @return 配置对象，不存在时返回null
	 */
	public UserPromptConfig getActiveConfigByType(String promptType) {
		List<UserPromptConfig> configs = getActiveConfigsByType(promptType);
		return configs.isEmpty() ? null : configs.get(0);
	}

	/**
	 * 获取所有配置
	 * @return 配置列表
	 */
	public List<UserPromptConfig> getAllConfigs() {
		return new ArrayList<>(configStorage.values());
	}

	/**
	 * 根据提示词类型获取所有配置
	 * @param promptType 提示词类型
	 * @return 配置列表
	 */
	public List<UserPromptConfig> getConfigsByType(String promptType) {
		return configStorage.values()
			.stream()
			.filter(config -> promptType.equals(config.getPromptType()))
			.sorted(Comparator.comparing(UserPromptConfig::getUpdateTime).reversed())
			.toList();
	}

	/**
	 * 删除配置
	 * @param id 配置ID
	 * @return 是否删除成功
	 */
	public boolean deleteConfig(String id) {
		UserPromptConfig config = configStorage.remove(id);
		if (config != null) {
			// 从类型映射中移除该配置
			removeFromPromptTypeMapping(config);
			logger.info("已删除配置：{}", id);
			return true;
		}
		return false;
	}

	/**
	 * 启用指定配置
	 * @param id 配置ID
	 * @return 是否操作成功
	 */
	public boolean enableConfig(String id) {
		UserPromptConfig config = configStorage.get(id);
		if (config != null) {
			config.setEnabled(true);
			config.setUpdateTime(LocalDateTime.now());
			updatePromptTypeMapping(config);
			logger.info("已启用配置：{}", id);
			return true;
		}
		return false;
	}

	/**
	 * 禁用指定配置
	 * @param id 配置ID
	 * @return 是否操作成功
	 */
	public boolean disableConfig(String id) {
		UserPromptConfig config = configStorage.get(id);
		if (config != null) {
			config.setEnabled(false);
			config.setUpdateTime(LocalDateTime.now());
			removeFromPromptTypeMapping(config);
			logger.info("已禁用配置：{}", id);
			return true;
		}
		return false;
	}

	/**
	 * 更新提示词类型映射
	 * @param config 配置对象
	 */
	private void updatePromptTypeMapping(UserPromptConfig config) {
		if (Boolean.TRUE.equals(config.getEnabled())) {
			promptTypeToConfigIds.computeIfAbsent(config.getPromptType(), k -> new ArrayList<>());
			List<String> configIds = promptTypeToConfigIds.get(config.getPromptType());
			if (!configIds.contains(config.getId())) {
				configIds.add(config.getId());
				logger.info("已将配置 {} 添加到提示词类型 [{}] 的映射中", config.getId(), config.getPromptType());
			}
		}
		else {
			removeFromPromptTypeMapping(config);
		}
	}

	/**
	 * 从提示词类型映射中移除配置
	 * @param config 配置对象
	 */
	private void removeFromPromptTypeMapping(UserPromptConfig config) {
		List<String> configIds = promptTypeToConfigIds.get(config.getPromptType());
		if (configIds != null) {
			configIds.remove(config.getId());
			if (configIds.isEmpty()) {
				promptTypeToConfigIds.remove(config.getPromptType());
			}
			logger.info("已从提示词类型 [{}] 的映射中移除配置 {}", config.getPromptType(), config.getId());
		}
	}

	/**
	 * 获取优化提示词内容列表
	 * @param promptType 提示词类型
	 * @return 优化提示词内容列表
	 */
	public List<UserPromptConfig> getOptimizationConfigs(String promptType) {
		return getActiveConfigsByType(promptType);
	}

	/**
	 * 获取自定义提示词内容，如果没有自定义配置则返回null（兼容旧接口）
	 * @param promptType 提示词类型
	 * @return 自定义提示词内容
	 */
	public String getCustomPromptContent(String promptType) {
		List<UserPromptConfig> configs = getActiveConfigsByType(promptType);
		if (!configs.isEmpty()) {
			return configs.get(0).getOptimizationPrompt();
		}
		return null;
	}

	/**
	 * 检查是否有自定义配置
	 * @param promptType 提示词类型
	 * @return 是否有自定义配置
	 */
	public boolean hasCustomConfig(String promptType) {
		return !getActiveConfigsByType(promptType).isEmpty();
	}

}

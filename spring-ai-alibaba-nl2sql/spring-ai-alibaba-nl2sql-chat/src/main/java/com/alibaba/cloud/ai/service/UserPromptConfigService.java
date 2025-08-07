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
 * User Prompt Configuration Management Service Provides CRUD functionality for prompt
 * configurations, supports runtime configuration updates
 *
 * @author Makoto
 */
@Service
public class UserPromptConfigService {

	private static final Logger logger = LoggerFactory.getLogger(UserPromptConfigService.class);

	/**
	 * Memory storage, can be replaced with database storage in actual projects
	 */
	private final Map<String, UserPromptConfig> configStorage = new ConcurrentHashMap<>();

	/**
	 * Store configuration ID mapping by prompt type
	 */
	private final Map<String, String> promptTypeToConfigId = new ConcurrentHashMap<>();

	/**
	 * Create or update prompt configuration
	 * @param configDTO configuration data transfer object
	 * @return saved configuration object
	 */
	public UserPromptConfig saveOrUpdateConfig(PromptConfigDTO configDTO) {
		logger.info("保存或更新提示词配置：{}", configDTO);

		UserPromptConfig config;
		if (configDTO.id() != null && configStorage.containsKey(configDTO.id())) {
			// Update existing configuration
			config = configStorage.get(configDTO.id());
			config.setName(configDTO.name());
			config.setSystemPrompt(configDTO.systemPrompt());
			config.setEnabled(configDTO.enabled());
			config.setDescription(configDTO.description());
			config.setUpdateTime(LocalDateTime.now());
		}
		else {
			// Create new configuration
			config = new UserPromptConfig();
			config.setId(UUID.randomUUID().toString());
			config.setName(configDTO.name());
			config.setPromptType(configDTO.promptType());
			config.setSystemPrompt(configDTO.systemPrompt());
			config.setEnabled(configDTO.enabled());
			config.setDescription(configDTO.description());
			config.setCreator(configDTO.creator());
		}

		configStorage.put(config.getId(), config);

		// If configuration is enabled, update type mapping
		if (Boolean.TRUE.equals(config.getEnabled())) {
			promptTypeToConfigId.put(config.getPromptType(), config.getId());
			logger.info("已启用提示词类型 [{}] 的配置：{}", config.getPromptType(), config.getId());
		}

		return config;
	}

	/**
	 * Get configuration by ID
	 * @param id configuration ID
	 * @return configuration object, returns null if not exists
	 */
	public UserPromptConfig getConfigById(String id) {
		return configStorage.get(id);
	}

	/**
	 * Get enabled configuration by prompt type
	 * @param promptType prompt type
	 * @return configuration object, returns null if not exists
	 */
	public UserPromptConfig getActiveConfigByType(String promptType) {
		String configId = promptTypeToConfigId.get(promptType);
		if (configId != null) {
			UserPromptConfig config = configStorage.get(configId);
			if (config != null && Boolean.TRUE.equals(config.getEnabled())) {
				return config;
			}
		}
		return null;
	}

	/**
	 * Get all configurations
	 * @return configuration list
	 */
	public List<UserPromptConfig> getAllConfigs() {
		return new ArrayList<>(configStorage.values());
	}

	/**
	 * Get all configurations by prompt type
	 * @param promptType prompt type
	 * @return configuration list
	 */
	public List<UserPromptConfig> getConfigsByType(String promptType) {
		return configStorage.values()
			.stream()
			.filter(config -> promptType.equals(config.getPromptType()))
			.sorted(Comparator.comparing(UserPromptConfig::getUpdateTime).reversed())
			.toList();
	}

	/**
	 * Delete configuration
	 * @param id configuration ID
	 * @return whether deletion succeeded
	 */
	public boolean deleteConfig(String id) {
		UserPromptConfig config = configStorage.remove(id);
		if (config != null) {
			// If deleting currently enabled configuration, need to clear type mapping
			String currentActiveId = promptTypeToConfigId.get(config.getPromptType());
			if (id.equals(currentActiveId)) {
				promptTypeToConfigId.remove(config.getPromptType());
				logger.info("已删除提示词类型 [{}] 的活跃配置", config.getPromptType());
			}
			logger.info("已删除配置：{}", id);
			return true;
		}
		return false;
	}

	/**
	 * Enable specified configuration
	 * @param id configuration ID
	 * @return whether operation succeeded
	 */
	public boolean enableConfig(String id) {
		UserPromptConfig config = configStorage.get(id);
		if (config != null) {
			// First disable other configurations of same type
			disableConfigsByType(config.getPromptType());

			// Enable current configuration
			config.setEnabled(true);
			config.setUpdateTime(LocalDateTime.now());
			promptTypeToConfigId.put(config.getPromptType(), id);

			logger.info("已启用配置：{}", id);
			return true;
		}
		return false;
	}

	/**
	 * Disable specified configuration
	 * @param id configuration ID
	 * @return whether operation succeeded
	 */
	public boolean disableConfig(String id) {
		UserPromptConfig config = configStorage.get(id);
		if (config != null) {
			config.setEnabled(false);
			config.setUpdateTime(LocalDateTime.now());

			// If it's current active configuration, remove type mapping
			String currentActiveId = promptTypeToConfigId.get(config.getPromptType());
			if (id.equals(currentActiveId)) {
				promptTypeToConfigId.remove(config.getPromptType());
			}

			logger.info("已禁用配置：{}", id);
			return true;
		}
		return false;
	}

	/**
	 * Disable all configurations of specified type
	 * @param promptType prompt type
	 */
	private void disableConfigsByType(String promptType) {
		configStorage.values().stream().filter(config -> promptType.equals(config.getPromptType())).forEach(config -> {
			config.setEnabled(false);
			config.setUpdateTime(LocalDateTime.now());
		});
		promptTypeToConfigId.remove(promptType);
	}

	/**
	 * Get custom prompt content, returns null if no custom configuration
	 * @param promptType prompt type
	 * @return custom prompt content
	 */
	public String getCustomPromptContent(String promptType) {
		UserPromptConfig config = getActiveConfigByType(promptType);
		return config != null ? config.getSystemPrompt() : null;
	}

	/**
	 * Check if there is custom configuration
	 * @param promptType prompt type
	 * @return whether there is custom configuration
	 */
	public boolean hasCustomConfig(String promptType) {
		return getActiveConfigByType(promptType) != null;
	}

}

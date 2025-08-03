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
 * 用户提示词配置管理服务 提供提示词配置的增删改查功能，支持运行时配置更新
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
	 * 根据提示词类型存储配置ID的映射
	 */
	private final Map<String, String> promptTypeToConfigId = new ConcurrentHashMap<>();

	/**
	 * 创建或更新提示词配置
	 * @param configDTO 配置数据传输对象
	 * @return 保存后的配置对象
	 */
	public UserPromptConfig saveOrUpdateConfig(PromptConfigDTO configDTO) {
		logger.info("保存或更新提示词配置：{}", configDTO);

		UserPromptConfig config;
		if (configDTO.id() != null && configStorage.containsKey(configDTO.id())) {
			// 更新现有配置
			config = configStorage.get(configDTO.id());
			config.setName(configDTO.name());
			config.setSystemPrompt(configDTO.systemPrompt());
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
			config.setSystemPrompt(configDTO.systemPrompt());
			config.setEnabled(configDTO.enabled());
			config.setDescription(configDTO.description());
			config.setCreator(configDTO.creator());
		}

		configStorage.put(config.getId(), config);

		// 如果配置启用，更新类型映射
		if (Boolean.TRUE.equals(config.getEnabled())) {
			promptTypeToConfigId.put(config.getPromptType(), config.getId());
			logger.info("已启用提示词类型 [{}] 的配置：{}", config.getPromptType(), config.getId());
		}

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
	 * 根据提示词类型获取启用的配置
	 * @param promptType 提示词类型
	 * @return 配置对象，不存在时返回null
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
			// 如果删除的是当前启用的配置，需要清除类型映射
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
	 * 启用指定配置
	 * @param id 配置ID
	 * @return 是否操作成功
	 */
	public boolean enableConfig(String id) {
		UserPromptConfig config = configStorage.get(id);
		if (config != null) {
			// 先禁用同类型的其他配置
			disableConfigsByType(config.getPromptType());

			// 启用当前配置
			config.setEnabled(true);
			config.setUpdateTime(LocalDateTime.now());
			promptTypeToConfigId.put(config.getPromptType(), id);

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

			// 如果是当前活跃配置，移除类型映射
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
	 * 禁用指定类型的所有配置
	 * @param promptType 提示词类型
	 */
	private void disableConfigsByType(String promptType) {
		configStorage.values().stream().filter(config -> promptType.equals(config.getPromptType())).forEach(config -> {
			config.setEnabled(false);
			config.setUpdateTime(LocalDateTime.now());
		});
		promptTypeToConfigId.remove(promptType);
	}

	/**
	 * 获取自定义提示词内容，如果没有自定义配置则返回null
	 * @param promptType 提示词类型
	 * @return 自定义提示词内容
	 */
	public String getCustomPromptContent(String promptType) {
		UserPromptConfig config = getActiveConfigByType(promptType);
		return config != null ? config.getSystemPrompt() : null;
	}

	/**
	 * 检查是否有自定义配置
	 * @param promptType 提示词类型
	 * @return 是否有自定义配置
	 */
	public boolean hasCustomConfig(String promptType) {
		return getActiveConfigByType(promptType) != null;
	}

}

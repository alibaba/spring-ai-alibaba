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
import com.alibaba.cloud.ai.mapper.UserPromptConfigMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

	@Autowired
	private UserPromptConfigMapper userPromptConfigMapper;

	/**
	 * 内存存储，用于缓存配置（可选的性能优化）
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
		if (configDTO.id() != null) {
			// 更新现有配置
			config = userPromptConfigMapper.selectById(configDTO.id());
			if (config != null) {
				config.setName(configDTO.name());
				config.setSystemPrompt(configDTO.systemPrompt());
				config.setEnabled(configDTO.enabled());
				config.setDescription(configDTO.description());
				userPromptConfigMapper.updateById(config);
			}
			else {
				// ID不存在，创建新配置
				config = new UserPromptConfig();
				config.setId(configDTO.id());
				config.setName(configDTO.name());
				config.setPromptType(configDTO.promptType());
				config.setSystemPrompt(configDTO.systemPrompt());
				config.setEnabled(configDTO.enabled());
				config.setDescription(configDTO.description());
				config.setCreator(configDTO.creator());
				userPromptConfigMapper.insert(config);
			}
		}
		else {
			// 创建新配置
			config = new UserPromptConfig();
			config.setName(configDTO.name());
			config.setPromptType(configDTO.promptType());
			config.setSystemPrompt(configDTO.systemPrompt());
			config.setEnabled(configDTO.enabled());
			config.setDescription(configDTO.description());
			config.setCreator(configDTO.creator());
			userPromptConfigMapper.insert(config);
		}

		// 更新缓存
		configStorage.put(config.getId(), config);

		// 更新类型映射（支持多个配置）
		updatePromptTypeMapping(config);

		// 如果配置启用，禁用同类型的其他配置
		if (Boolean.TRUE.equals(config.getEnabled())) {
			userPromptConfigMapper.disableAllByPromptType(config.getPromptType());
			userPromptConfigMapper.enableById(config.getId());
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
		return userPromptConfigMapper.selectById(id);
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
		// 优先从数据库获取
		UserPromptConfig dbConfig = userPromptConfigMapper.selectActiveByPromptType(promptType);
		if (dbConfig != null) {
			return dbConfig;
		}

		// 备用：从内存缓存获取
		List<UserPromptConfig> configs = getActiveConfigsByType(promptType);
		return configs.isEmpty() ? null : configs.get(0);
	}

	/**
	 * 获取所有配置
	 * @return 配置列表
	 */
	public List<UserPromptConfig> getAllConfigs() {
		return userPromptConfigMapper
			.selectList(Wrappers.<UserPromptConfig>lambdaQuery().orderByDesc(UserPromptConfig::getUpdateTime));
	}

	/**
	 * 根据提示词类型获取所有配置
	 * @param promptType 提示词类型
	 * @return 配置列表
	 */
	public List<UserPromptConfig> getConfigsByType(String promptType) {
		return userPromptConfigMapper.selectByPromptType(promptType);
	}

	/**
	 * 删除配置
	 * @param id 配置ID
	 * @return 是否删除成功
	 */
	public boolean deleteConfig(String id) {
		UserPromptConfig config = userPromptConfigMapper.selectById(id);
		if (config != null) {
			// 从数据库删除
			int deleted = userPromptConfigMapper.deleteById(id);
			if (deleted > 0) {
				// 从内存缓存和类型映射中移除该配置
				configStorage.remove(id);
				removeFromPromptTypeMapping(config);
				logger.info("已删除配置：{}", id);
				return true;
			}
		}
		return false;
	}

	/**
	 * 启用指定配置
	 * @param id 配置ID
	 * @return 是否操作成功
	 */
	public boolean enableConfig(String id) {
		UserPromptConfig config = userPromptConfigMapper.selectById(id);
		if (config != null) {
			// 先禁用同类型的其他配置
			userPromptConfigMapper.disableAllByPromptType(config.getPromptType());

			// 启用当前配置
			int updated = userPromptConfigMapper.enableById(id);
			if (updated > 0) {
				// 更新内存缓存
				config.setEnabled(true);
				configStorage.put(id, config);
				updatePromptTypeMapping(config);
				logger.info("已启用配置：{}", id);
				return true;
			}
		}
		return false;
	}

	/**
	 * 禁用指定配置
	 * @param id 配置ID
	 * @return 是否操作成功
	 */
	public boolean disableConfig(String id) {
		int updated = userPromptConfigMapper.disableById(id);
		if (updated > 0) {
			// 更新内存缓存
			UserPromptConfig config = configStorage.get(id);
			if (config != null) {
				config.setEnabled(false);
				removeFromPromptTypeMapping(config);
			}
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
			return configs.get(0).getSystemPrompt();
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

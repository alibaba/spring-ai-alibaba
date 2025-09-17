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

import java.util.*;

/**
 * User Prompt Configuration Management Service Provides CRUD functionality for prompt
 * configurations, supports runtime configuration updates
 *
 * @author Makoto
 */
@Service
public class UserPromptConfigService {

	private static final Logger logger = LoggerFactory.getLogger(UserPromptConfigService.class);

	@Autowired
	private UserPromptConfigMapper userPromptConfigMapper;

	/**
	 * Create or update prompt configuration
	 * @param configDTO configuration data transfer object
	 * @return saved configuration object
	 */
	public UserPromptConfig saveOrUpdateConfig(PromptConfigDTO configDTO) {
		logger.info("保存或更新提示词优化配置：{}", configDTO);

		UserPromptConfig config;
		if (configDTO.id() != null) {
			// Update existing configuration
			config = userPromptConfigMapper.selectById(configDTO.id());
			if (config != null) {
				config.setName(configDTO.name());
				config.setSystemPrompt(configDTO.optimizationPrompt());
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
				config.setSystemPrompt(configDTO.optimizationPrompt());
				config.setEnabled(configDTO.enabled());
				config.setDescription(configDTO.description());
				config.setCreator(configDTO.creator());
				userPromptConfigMapper.insert(config);
			}
		}
		else {
			// Create new configuration
			config = new UserPromptConfig();
			config.setName(configDTO.name());
			config.setPromptType(configDTO.promptType());
			config.setSystemPrompt(configDTO.optimizationPrompt());
			config.setEnabled(configDTO.enabled());
			config.setDescription(configDTO.description());
			config.setCreator(configDTO.creator());
			userPromptConfigMapper.insert(config);
		}

		// If the configuration is enabled, disable other configurations of the same type
		if (Boolean.TRUE.equals(config.getEnabled())) {
			userPromptConfigMapper.disableAllByPromptType(config.getPromptType());
			userPromptConfigMapper.enableById(config.getId());
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
		return userPromptConfigMapper.selectById(id);
	}

	/**
	 * 根据提示词类型获取所有启用的配置
	 * @param promptType 提示词类型
	 * @return 配置列表
	 */
	public List<UserPromptConfig> getActiveConfigsByType(String promptType) {
		return userPromptConfigMapper.selectList(Wrappers.<UserPromptConfig>lambdaQuery()
			.eq(UserPromptConfig::getPromptType, promptType)
			.eq(UserPromptConfig::getEnabled, true)
			.orderByDesc(UserPromptConfig::getUpdateTime));
	}

	/**
	 * Get enabled configuration by prompt type
	 * @param promptType prompt type
	 * @return configuration object, returns null if not exists
	 */
	public UserPromptConfig getActiveConfigByType(String promptType) {
		return userPromptConfigMapper.selectActiveByPromptType(promptType);
	}

	/**
	 * Get all configurations
	 * @return configuration list
	 */
	public List<UserPromptConfig> getAllConfigs() {
		return userPromptConfigMapper
			.selectList(Wrappers.<UserPromptConfig>lambdaQuery().orderByDesc(UserPromptConfig::getUpdateTime));
	}

	/**
	 * Get all configurations by prompt type
	 * @param promptType prompt type
	 * @return configuration list
	 */
	public List<UserPromptConfig> getConfigsByType(String promptType) {
		return userPromptConfigMapper.selectByPromptType(promptType);
	}

	/**
	 * Delete configuration
	 * @param id configuration ID
	 * @return whether deletion succeeded
	 */
	public boolean deleteConfig(String id) {
		UserPromptConfig config = userPromptConfigMapper.selectById(id);
		if (config != null) {
			// 从数据库删除
			int deleted = userPromptConfigMapper.deleteById(id);
			if (deleted > 0) {
				logger.info("已删除配置：{}", id);
				return true;
			}
		}
		return false;
	}

	/**
	 * Enable specified configuration
	 * @param id configuration ID
	 * @return whether operation succeeded
	 */
	public boolean enableConfig(String id) {
		UserPromptConfig config = userPromptConfigMapper.selectById(id);
		if (config != null) {
			// First, disable other configurations of the same type
			userPromptConfigMapper.disableAllByPromptType(config.getPromptType());

			// Enable the current configuration
			int updated = userPromptConfigMapper.enableById(id);
			if (updated > 0) {
				logger.info("已启用配置：{}", id);
				return true;
			}
		}
		return false;
	}

	/**
	 * Disable specified configuration
	 * @param id configuration ID
	 * @return whether operation succeeded
	 */
	public boolean disableConfig(String id) {
		int updated = userPromptConfigMapper.disableById(id);
		if (updated > 0) {
			logger.info("已禁用配置：{}", id);
			return true;
		}
		return false;
	}

	/**
	 * Get optimization configurations by prompt type
	 * @param promptType prompt type
	 * @return optimization configuration list
	 */
	public List<UserPromptConfig> getOptimizationConfigs(String promptType) {
		return getActiveConfigsByType(promptType);
	}

}

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
 * 用户提示词配置管理服务 提供提示词配置的增删改查功能，支持运行时配置更新
 *
 * @author Makoto
 */
@Service
public class UserPromptConfigService {

	private static final Logger logger = LoggerFactory.getLogger(UserPromptConfigService.class);

	@Autowired
	private UserPromptConfigMapper userPromptConfigMapper;

	/**
	 * 创建或更新提示词配置
	 * @param configDTO 配置数据传输对象
	 * @return 保存后的配置对象
	 */
	public UserPromptConfig saveOrUpdateConfig(PromptConfigDTO configDTO) {
		logger.info("保存或更新提示词配置：{}", configDTO);

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
	 * 根据提示词类型获取启用的配置
	 * @param promptType 提示词类型
	 * @return 配置对象，不存在时返回null
	 */
	public UserPromptConfig getActiveConfigByType(String promptType) {
		return userPromptConfigMapper.selectActiveByPromptType(promptType);
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
			int deleted = userPromptConfigMapper.deleteById(id);
			if (deleted > 0) {
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
			logger.info("已禁用配置：{}", id);
			return true;
		}
		return false;
	}

	/**
	 * 禁用指定类型的所有配置
	 * @param promptType 提示词类型
	 */
	public void disableConfigsByType(String promptType) {
		userPromptConfigMapper.disableAllByPromptType(promptType);
	}

	/**
	 * 获取自定义提示词内容，如果没有自定义配置则返回null
	 * @param promptType 提示词类型
	 * @return 自定义提示词内容
	 */
	public String getCustomPromptContent(String promptType) {
		// TODO 需要优化，提示词不能完全替代现有的，仅用作补充使用
		return null;
		// UserPromptConfig config = getActiveConfigByType(promptType);
		// return config != null ? config.getSystemPrompt() : null;
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

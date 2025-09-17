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
package com.alibaba.cloud.ai.manus.config;

import java.util.List;
import java.util.Optional;

import com.alibaba.cloud.ai.manus.config.entity.ConfigEntity;

/**
 * Configuration service interface for managing application configurations. This interface
 * is designed to work properly with Spring proxies in native image mode.
 */
public interface IConfigService {

	/**
	 * Get configuration value by path
	 * @param configPath the configuration path
	 * @return the configuration value, or null if not found
	 */
	String getConfigValue(String configPath);

	/**
	 * Update configuration value
	 * @param configPath the configuration path
	 * @param newValue the new value
	 */
	void updateConfig(String configPath, String newValue);

	/**
	 * Get all configurations
	 * @return list of all configurations
	 */
	List<ConfigEntity> getAllConfigs();

	/**
	 * Get configuration by path
	 * @param configPath the configuration path
	 * @return optional configuration entity
	 */
	Optional<ConfigEntity> getConfig(String configPath);

	/**
	 * Reset configuration to default value
	 * @param configPath the configuration path
	 */
	void resetConfig(String configPath);

	/**
	 * Get configurations by group
	 * @param groupName the group name
	 * @return list of configurations in the group
	 */
	List<ConfigEntity> getConfigsByGroup(String groupName);

	/**
	 * Batch update configurations
	 * @param configs list of configurations to update
	 */
	void batchUpdateConfigs(List<ConfigEntity> configs);

	/**
	 * Reset all configurations to their default values
	 */
	void resetAllConfigsToDefaults();

}

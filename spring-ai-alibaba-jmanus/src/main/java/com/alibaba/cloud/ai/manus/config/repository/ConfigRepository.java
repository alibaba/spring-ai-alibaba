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
package com.alibaba.cloud.ai.manus.config.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alibaba.cloud.ai.manus.config.entity.ConfigEntity;

/**
 * System configuration data access interface
 */
@Repository
public interface ConfigRepository extends JpaRepository<ConfigEntity, Long> {

	/**
	 * Find configuration item by configuration path
	 * @param configPath Configuration path
	 * @return Configuration entity
	 */
	Optional<ConfigEntity> findByConfigPath(String configPath);

	/**
	 * Find configuration list by config group and sub group
	 * @param configGroup Configuration group
	 * @param configSubGroup Configuration sub group
	 * @return Configuration entity list
	 */
	List<ConfigEntity> findByConfigGroupAndConfigSubGroup(String configGroup, String configSubGroup);

	/**
	 * Find configuration list by config group
	 * @param configGroup Configuration group
	 * @return Configuration entity list
	 */
	List<ConfigEntity> findByConfigGroup(String configGroup);

	/**
	 * Check if configuration path exists
	 * @param configPath Configuration path
	 * @return Whether exists
	 */
	boolean existsByConfigPath(String configPath);

	/**
	 * Delete configuration by path
	 * @param configPath Configuration path
	 */
	void deleteByConfigPath(String configPath);

	/**
	 * Get all configuration groups
	 * @return Configuration group list
	 */
	@Query("SELECT DISTINCT c.configGroup FROM ConfigEntity c ORDER BY c.configGroup")
	List<String> findAllGroups();

	/**
	 * Get all sub groups under specified configuration group
	 * @param configGroup Configuration group
	 * @return Sub group list
	 */
	@Query("SELECT DISTINCT c.configSubGroup FROM ConfigEntity c WHERE c.configGroup = :group ORDER BY c.configSubGroup")
	List<String> findSubGroupsByGroup(@Param("group") String configGroup);

	/**
	 * Batch update configuration value
	 * @param configPath Configuration path
	 * @param configValue Configuration value
	 * @return Number of updated records
	 */
	@Query("UPDATE ConfigEntity c SET c.configValue = :value WHERE c.configPath = :path")
	int updateConfigValue(@Param("path") String configPath, @Param("value") String configValue);

	/**
	 * Batch get configuration entities by configuration paths
	 * @param configPaths Configuration path list
	 * @return Configuration entity list
	 */
	List<ConfigEntity> findByConfigPathIn(List<String> configPaths);

}

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
package com.alibaba.cloud.ai.example.manus.config.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alibaba.cloud.ai.example.manus.config.entity.ConfigEntity;

/**
 * 系统配置数据访问接口
 */
@Repository
public interface ConfigRepository extends JpaRepository<ConfigEntity, Long> {

	/**
	 * 根据配置路径查找配置项
	 * @param configPath 配置路径
	 * @return 配置项
	 */
	Optional<ConfigEntity> findByConfigPath(String configPath);

	/**
	 * 根据配置组和子组查找配置项列表
	 * @param configGroup 配置组
	 * @param configSubGroup 配置子组
	 * @return 配置项列表
	 */
	List<ConfigEntity> findByConfigGroupAndConfigSubGroup(String configGroup, String configSubGroup);

	/**
	 * 根据配置组查找配置项列表
	 * @param configGroup 配置组
	 * @return 配置项列表
	 */
	List<ConfigEntity> findByConfigGroup(String configGroup);

	/**
	 * 检查配置路径是否存在
	 * @param configPath 配置路径
	 * @return 是否存在
	 */
	boolean existsByConfigPath(String configPath);

	/**
	 * 根据配置路径删除配置项
	 * @param configPath 配置路径
	 */
	void deleteByConfigPath(String configPath);

	/**
	 * 获取所有配置组
	 * @return 配置组列表
	 */
	@Query("SELECT DISTINCT c.configGroup FROM ConfigEntity c ORDER BY c.configGroup")
	List<String> findAllGroups();

	/**
	 * 获取指定配置组下的所有子组
	 * @param configGroup 配置组
	 * @return 子组列表
	 */
	@Query("SELECT DISTINCT c.configSubGroup FROM ConfigEntity c WHERE c.configGroup = :group ORDER BY c.configSubGroup")
	List<String> findSubGroupsByGroup(@Param("group") String configGroup);

	/**
	 * 批量更新配置值
	 * @param configPath 配置路径
	 * @param configValue 配置值
	 * @return 更新的记录数
	 */
	@Query("UPDATE ConfigEntity c SET c.configValue = :value WHERE c.configPath = :path")
	int updateConfigValue(@Param("path") String configPath, @Param("value") String configValue);

	/**
	 * 根据配置路径批量获取配置项
	 * @param configPaths 配置路径列表
	 * @return 配置项列表
	 */
	List<ConfigEntity> findByConfigPathIn(List<String> configPaths);

}

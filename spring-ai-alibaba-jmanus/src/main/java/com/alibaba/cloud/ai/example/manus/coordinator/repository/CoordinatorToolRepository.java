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
package com.alibaba.cloud.ai.example.manus.coordinator.repository;

import com.alibaba.cloud.ai.example.manus.coordinator.entity.CoordinatorToolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 协调器工具数据访问层
 */
@Repository
public interface CoordinatorToolRepository extends JpaRepository<CoordinatorToolEntity, Long> {

	/**
	 * 根据工具名称查找
	 */
	Optional<CoordinatorToolEntity> findByToolName(String toolName);

	/**
	 * 根据计划模板ID查找
	 */
	List<CoordinatorToolEntity> findByPlanTemplateId(String planTemplateId);

	/**
	 * 根据发布状态查找
	 */
	List<CoordinatorToolEntity> findByPublishStatus(CoordinatorToolEntity.PublishStatus publishStatus);

	/**
	 * 查找所有已发布的工具
	 */
	List<CoordinatorToolEntity> findByPublishStatusOrderByCreateTimeDesc(CoordinatorToolEntity.PublishStatus publishStatus);

	/**
	 * 根据工具名称和发布状态查找
	 */
	Optional<CoordinatorToolEntity> findByToolNameAndPublishStatus(String toolName, CoordinatorToolEntity.PublishStatus publishStatus);

	/**
	 * 根据计划模板ID和发布状态查找
	 */
	List<CoordinatorToolEntity> findByPlanTemplateIdAndPublishStatus(String planTemplateId, CoordinatorToolEntity.PublishStatus publishStatus);

	/**
	 * 检查工具名称是否存在
	 */
	boolean existsByToolName(String toolName);

	/**
	 * 检查计划模板ID是否存在
	 */
	boolean existsByPlanTemplateId(String planTemplateId);

	/**
	 * 根据endpoint查找
	 */
	Optional<CoordinatorToolEntity> findByEndpoint(String endpoint);

	/**
	 * 检查endpoint是否存在
	 */
	boolean existsByEndpoint(String endpoint);

	/**
	 * 查找所有去重的endpoint列表
	 */
	@Query("SELECT DISTINCT c.endpoint FROM CoordinatorToolEntity c")
	List<String> findEndPoint();

	/**
	 * 根据工具名称模糊查询
	 */
	List<CoordinatorToolEntity> findByToolNameContainingIgnoreCase(String toolName);

	/**
	 * 根据工具描述模糊查询
	 */
	List<CoordinatorToolEntity> findByToolDescriptionContainingIgnoreCase(String toolDescription);

	/**
	 * 查找最近创建的工具
	 */
	@Query("SELECT c FROM CoordinatorToolEntity c ORDER BY c.createTime DESC")
	List<CoordinatorToolEntity> findRecentTools();

	/**
	 * 查找最近更新的工具
	 */
	@Query("SELECT c FROM CoordinatorToolEntity c ORDER BY c.updateTime DESC")
	List<CoordinatorToolEntity> findRecentlyUpdatedTools();

	/**
	 * 根据创建时间范围查找
	 */
	@Query("SELECT c FROM CoordinatorToolEntity c WHERE c.createTime BETWEEN :startTime AND :endTime")
	List<CoordinatorToolEntity> findByCreateTimeBetween(@Param("startTime") java.time.LocalDateTime startTime, 
													   @Param("endTime") java.time.LocalDateTime endTime);

	/**
	 * 统计已发布的工具数量
	 */
	long countByPublishStatus(CoordinatorToolEntity.PublishStatus publishStatus);

	/**
	 * 统计未发布的工具数量
	 */
	@Query("SELECT COUNT(c) FROM CoordinatorToolEntity c WHERE c.publishStatus = 'UNPUBLISHED'")
	long countUnpublishedTools();

	/**
	 * 删除所有未发布的工具
	 */
	void deleteByPublishStatus(CoordinatorToolEntity.PublishStatus publishStatus);

} 
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
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 协调器工具数据访问层
 */
@Repository
public interface CoordinatorToolRepository extends JpaRepository<CoordinatorToolEntity, Long> {

	/**
	 * 根据计划模板ID查找
	 */
	List<CoordinatorToolEntity> findByPlanTemplateId(String planTemplateId);

}
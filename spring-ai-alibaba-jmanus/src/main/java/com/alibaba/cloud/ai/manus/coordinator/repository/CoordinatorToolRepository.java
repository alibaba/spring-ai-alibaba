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
package com.alibaba.cloud.ai.manus.coordinator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.alibaba.cloud.ai.manus.coordinator.entity.po.CoordinatorToolEntity;

import java.util.List;

/**
 * Coordinator Tool Data Access Layer
 */
@Repository
public interface CoordinatorToolRepository extends JpaRepository<CoordinatorToolEntity, Long> {

	/**
	 * Find by plan template ID
	 */
	List<CoordinatorToolEntity> findByPlanTemplateId(String planTemplateId);

	/**
	 * Find all unique HTTP endpoints
	 */
	@Query("SELECT DISTINCT c.httpEndpoint FROM CoordinatorToolEntity c WHERE c.httpEndpoint IS NOT NULL")
	List<String> findAllUniqueHttpEndpoints();

	/**
	 * Find all unique MCP endpoints
	 */
	@Query("SELECT DISTINCT c.mcpEndpoint FROM CoordinatorToolEntity c WHERE c.mcpEndpoint IS NOT NULL")
	List<String> findAllUniqueMcpEndpoints();

	/**
	 * Find by tool name
	 */
	CoordinatorToolEntity findByToolName(String toolName);

}

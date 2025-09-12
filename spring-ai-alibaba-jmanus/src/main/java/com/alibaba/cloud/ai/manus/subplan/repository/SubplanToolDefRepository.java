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
package com.alibaba.cloud.ai.manus.subplan.repository;

import com.alibaba.cloud.ai.manus.subplan.model.po.SubplanToolDef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for SubplanToolDef entity
 *
 * Provides data access methods for subplan tool definitions
 */
@Repository
public interface SubplanToolDefRepository extends JpaRepository<SubplanToolDef, Long> {

	/**
	 * Find all subplan tools
	 * @return List of all subplan tool definitions
	 */
	List<SubplanToolDef> findAll();

	/**
	 * Find subplan tool by tool name
	 * @param toolName the tool name
	 * @return Optional containing the subplan tool if found
	 */
	Optional<SubplanToolDef> findByToolName(String toolName);

	/**
	 * Find subplan tool by plan template ID (unique)
	 * @param planTemplateId the plan template ID
	 * @return Optional containing the subplan tool if found
	 */
	Optional<SubplanToolDef> findOneByPlanTemplateId(String planTemplateId);

	/**
	 * Find subplan tools by endpoint
	 * @param endpoint the endpoint
	 * @return List of subplan tools for the specified endpoint
	 */
	List<SubplanToolDef> findByEndpoint(String endpoint);

	/**
	 * Check if subplan tool exists by tool name
	 * @param toolName the tool name
	 * @return true if exists, false otherwise
	 */
	boolean existsByToolName(String toolName);

	/**
	 * Delete subplan tool by tool name
	 * @param toolName the tool name
	 */
	void deleteByToolName(String toolName);

	/**
	 * Find subplan tools by tool description containing the given text
	 * @param description the description text to search for
	 * @return List of subplan tools with matching descriptions
	 */
	List<SubplanToolDef> findByToolDescriptionContainingIgnoreCase(String description);

	/**
	 * Custom query to find all subplan tools with their parameters loaded
	 * @return List of subplan tools with parameters
	 */
	@Query("SELECT DISTINCT s FROM SubplanToolDef s LEFT JOIN FETCH s.inputSchema")
	List<SubplanToolDef> findAllWithParameters();

	/**
	 * Count subplan tools by endpoint
	 * @param endpoint the endpoint
	 * @return count of subplan tools for the endpoint
	 */
	long countByEndpoint(String endpoint);

	/**
	 * Find subplan tools by tool name pattern (using LIKE)
	 * @param toolNamePattern the tool name pattern (e.g., "%analysis%")
	 * @return List of subplan tools matching the pattern
	 */
	@Query("SELECT s FROM SubplanToolDef s WHERE s.toolName LIKE %:toolNamePattern%")
	List<SubplanToolDef> findByToolNamePattern(@Param("toolNamePattern") String toolNamePattern);

}

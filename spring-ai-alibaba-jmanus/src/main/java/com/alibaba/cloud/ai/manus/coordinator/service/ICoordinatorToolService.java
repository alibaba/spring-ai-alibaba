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
package com.alibaba.cloud.ai.manus.coordinator.service;

import com.alibaba.cloud.ai.manus.coordinator.entity.vo.CoordinatorToolVO;
import com.alibaba.cloud.ai.manus.coordinator.exception.CoordinatorToolException;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing Coordinator Tools Handles both CoordinatorToolEntity and
 * SubplanToolDef operations
 */
public interface ICoordinatorToolService {

	/**
	 * Create a new coordinator tool
	 * @param toolVO Coordinator tool data
	 * @return Created coordinator tool VO
	 * @throws CoordinatorToolException if validation fails or creation fails
	 */
	CoordinatorToolVO createCoordinatorTool(CoordinatorToolVO toolVO) throws CoordinatorToolException;

	/**
	 * Update an existing coordinator tool
	 * @param id Tool ID
	 * @param toolVO Updated tool data
	 * @return Updated coordinator tool VO
	 * @throws CoordinatorToolException if tool not found or update fails
	 */
	CoordinatorToolVO updateCoordinatorTool(Long id, CoordinatorToolVO toolVO) throws CoordinatorToolException;

	/**
	 * Delete a coordinator tool by ID Deletes both CoordinatorToolEntity and
	 * corresponding SubplanToolDef
	 * @param id Tool ID
	 * @throws CoordinatorToolException if tool not found or deletion fails
	 */
	void deleteCoordinatorTool(Long id) throws CoordinatorToolException;

	/**
	 * Get coordinator tool by ID
	 * @param id Tool ID
	 * @return Coordinator tool VO if found
	 */
	Optional<CoordinatorToolVO> getCoordinatorToolById(Long id);

	/**
	 * Get coordinator tool by plan template ID
	 * @param planTemplateId Plan template ID
	 * @return Coordinator tool VO if found
	 */
	Optional<CoordinatorToolVO> getCoordinatorToolByPlanTemplateId(String planTemplateId);

	/**
	 * Get or create coordinator tool by plan template ID
	 * @param planTemplateId Plan template ID
	 * @return Existing or default coordinator tool VO
	 */
	CoordinatorToolVO getOrCreateCoordinatorToolByPlanTemplateId(String planTemplateId);

	/**
	 * Get all coordinator tools
	 * @return List of all coordinator tool VOs
	 */
	List<CoordinatorToolVO> getAllCoordinatorTools();

	/**
	 * Get all unique MCP endpoints
	 * @return List of unique MCP endpoints
	 */
	List<String> getAllUniqueMcpEndpoints();

}

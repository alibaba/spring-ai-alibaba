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
package com.alibaba.cloud.ai.manus.subplan.service;

import com.alibaba.cloud.ai.manus.planning.PlanningFactory;
import com.alibaba.cloud.ai.manus.subplan.model.po.SubplanToolDef;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for managing subplan tools
 *
 * Provides functionality to register and manage subplan tools from the database
 */
public interface ISubplanToolService {

	/**
	 * Get all subplan tools from the database
	 * @return List of all subplan tool definitions
	 */
	List<SubplanToolDef> getAllSubplanTools();

	/**
	 * Get subplan tool by plan template ID (unique)
	 * @param planTemplateId the plan template ID
	 * @return Optional containing the subplan tool if found
	 */
	Optional<SubplanToolDef> getSubplanToolByTemplate(String planTemplateId);

	/**
	 * Get subplan tools by endpoint
	 * @param endpoint the endpoint
	 * @return List of subplan tools for the specified endpoint
	 */
	List<SubplanToolDef> getSubplanToolsByEndpoint(String endpoint);

	/**
	 * Create tool callback context map for subplan tools This integrates with the
	 * existing PlanningFactory tool registry system
	 * @param planId the current plan ID
	 * @param rootPlanId the root plan ID
	 * @param expectedReturnInfo expected return information
	 * @return Map of tool name to ToolCallBackContext
	 */
	Map<String, PlanningFactory.ToolCallBackContext> createSubplanToolCallbacks(String planId, String rootPlanId,
			String expectedReturnInfo);

	/**
	 * Register a new subplan tool
	 * @param toolDef the subplan tool definition
	 * @return the saved tool definition
	 */
	SubplanToolDef registerSubplanTool(SubplanToolDef toolDef);

	/**
	 * Update an existing subplan tool
	 * @param toolDef the subplan tool definition to update
	 * @return the updated tool definition
	 */
	SubplanToolDef updateSubplanTool(SubplanToolDef toolDef);

	/**
	 * Delete a subplan tool by ID
	 * @param id the tool ID
	 */
	void deleteSubplanTool(Long id);

	/**
	 * Check if a subplan tool exists by name
	 * @param toolName the tool name
	 * @return true if exists, false otherwise
	 */
	boolean existsByToolName(String toolName);

	/**
	 * Get subplan tool by name
	 * @param toolName the tool name
	 * @return the subplan tool definition or null if not found
	 */
	SubplanToolDef getByToolName(String toolName);

}

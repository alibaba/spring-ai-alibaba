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
import com.alibaba.cloud.ai.manus.coordinator.entity.po.CoordinatorToolEntity;
import com.alibaba.cloud.ai.manus.coordinator.exception.CoordinatorToolException;
import com.alibaba.cloud.ai.manus.coordinator.repository.CoordinatorToolRepository;
import com.alibaba.cloud.ai.manus.subplan.model.po.SubplanToolDef;
import com.alibaba.cloud.ai.manus.subplan.model.po.SubplanParamDef;
import com.alibaba.cloud.ai.manus.subplan.service.ISubplanToolService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service implementation for managing Coordinator Tools Handles both
 * CoordinatorToolEntity and SubplanToolDef operations with transaction support
 */
@Service
public class CoordinatorToolServiceImpl implements ICoordinatorToolService {

	private static final Logger log = LoggerFactory.getLogger(CoordinatorToolServiceImpl.class);

	@Autowired
	private CoordinatorToolRepository coordinatorToolRepository;

	@Autowired
	private ISubplanToolService subplanToolService;

	@Autowired
	private ObjectMapper objectMapper;

	@Override
	@Transactional
	public CoordinatorToolVO createCoordinatorTool(CoordinatorToolVO toolVO) throws CoordinatorToolException {
		int maxRetries = 3;
		int retryCount = 0;

		while (retryCount < maxRetries) {
			try {
				log.info("Creating coordinator tool: {} (attempt {})", toolVO, retryCount + 1);

				// Validate required fields
				validateCoordinatorToolVO(toolVO);

				// Set default values
				setDefaultValues(toolVO);

				// Create and save CoordinatorToolEntity
				CoordinatorToolEntity entity = toolVO.toEntity();
				CoordinatorToolEntity savedEntity = coordinatorToolRepository.save(entity);
				log.info("Successfully saved CoordinatorToolEntity: {} with ID: {}", savedEntity.getToolName(),
						savedEntity.getId());

				// Create SubplanToolDef for tool call registration
				SubplanToolDef subplanToolDef = createSubplanToolDefFromVO(toolVO);
				// Don't set ID manually - let the database generate it
				// The relationship will be maintained through the tool name

				// Register the tool in subplan tool service
				SubplanToolDef registeredTool = subplanToolService.registerSubplanTool(subplanToolDef);
				log.info("Successfully registered subplan tool: {} with ID: {}", registeredTool.getToolName(),
						registeredTool.getId());

				// Convert CoordinatorToolEntity back to CoordinatorToolVO and return
				return CoordinatorToolVO.fromEntity(savedEntity);

			}
			catch (CoordinatorToolException e) {
				// Re-throw custom exceptions
				throw e;
			}
			catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
				retryCount++;
				if (retryCount >= maxRetries) {
					log.error("Max retries reached for optimistic locking failure: {}", e.getMessage(), e);
					throw new CoordinatorToolException("CONCURRENT_ACCESS",
							"Tool creation failed due to concurrent access. Please try again.");
				}
				log.warn("Optimistic locking failure, retrying... (attempt {})", retryCount);
				try {
					Thread.sleep(100 * retryCount); // Exponential backoff
				}
				catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new CoordinatorToolException("INTERRUPTED", "Operation was interrupted");
				}
			}
			catch (Exception e) {
				log.error("Unexpected error creating coordinator tool: {}", e.getMessage(), e);
				throw new CoordinatorToolException("INTERNAL_ERROR",
						"An unexpected error occurred while creating coordinator tool");
			}
		}

		throw new CoordinatorToolException("MAX_RETRIES_EXCEEDED", "Maximum retry attempts exceeded");
	}

	@Override
	@Transactional
	public CoordinatorToolVO updateCoordinatorTool(Long id, CoordinatorToolVO toolVO) throws CoordinatorToolException {
		try {
			log.info("Updating coordinator tool with ID: {}", id);

			// Validate required fields
			validateCoordinatorToolVO(toolVO);

			// Check if CoordinatorToolEntity exists
			CoordinatorToolEntity existingEntity = coordinatorToolRepository.findById(id)
				.orElseThrow(
						() -> new CoordinatorToolException("NOT_FOUND", "Coordinator tool not found with ID: " + id));

			// Set default values
			setDefaultValues(toolVO);

			// Update CoordinatorToolEntity
			updateCoordinatorToolEntityFromVO(existingEntity, toolVO);
			CoordinatorToolEntity savedEntity = coordinatorToolRepository.save(existingEntity);
			log.info("Successfully updated CoordinatorToolEntity: {} with ID: {}", savedEntity.getToolName(),
					savedEntity.getId());

			// Also update SubplanToolDef for backward compatibility
			// Mapping Strategy: coordinator_tools.planTemplateId <->
			// subplan_tool_def.planTemplateId (unique)
			// This ensures data consistency between the two tables when updating
			// coordinator tools
			Optional<SubplanToolDef> existingToolOpt = subplanToolService
				.getSubplanToolByTemplate(toolVO.getPlanTemplateId());
			if (existingToolOpt.isPresent()) {
				// Update existing SubplanToolDef with all fields from CoordinatorToolVO
				SubplanToolDef existingTool = existingToolOpt.get();
				SubplanToolDef updatedToolDef = createSubplanToolDefFromVO(toolVO);
				updatedToolDef.setId(existingTool.getId()); // Preserve the existing
															// SubplanToolDef ID

				// Update the SubplanToolDef
				subplanToolService.updateSubplanTool(updatedToolDef);
				log.info("Successfully updated subplan tool: {} with ID: {} for planTemplateId: {}",
						updatedToolDef.getToolName(), updatedToolDef.getId(), toolVO.getPlanTemplateId());
			}
			else {
				log.warn("No existing SubplanToolDef found for planTemplateId: {}, creating new one",
						toolVO.getPlanTemplateId());
				// If no existing SubplanToolDef found, create a new one
				SubplanToolDef newToolDef = createSubplanToolDefFromVO(toolVO);
				subplanToolService.registerSubplanTool(newToolDef);
				log.info("Successfully created new subplan tool: {} with ID: {} for planTemplateId: {}",
						newToolDef.getToolName(), newToolDef.getId(), toolVO.getPlanTemplateId());
			}

			// Convert back to VO and return
			return CoordinatorToolVO.fromEntity(savedEntity);

		}
		catch (CoordinatorToolException e) {
			throw e;
		}
		catch (Exception e) {
			log.error("Error updating coordinator tool: {}", e.getMessage(), e);
			throw new CoordinatorToolException("INTERNAL_ERROR",
					"An unexpected error occurred while updating coordinator tool");
		}
	}

	@Override
	@Transactional
	public void deleteCoordinatorTool(Long id) throws CoordinatorToolException {
		try {
			log.info("Deleting coordinator tool with ID: {}", id);

			// Check if CoordinatorToolEntity exists and get its planTemplateId
			CoordinatorToolEntity existingEntity = coordinatorToolRepository.findById(id)
				.orElseThrow(
						() -> new CoordinatorToolException("NOT_FOUND", "Coordinator tool not found with ID: " + id));

			// Delete from SubplanToolDef first (for backward compatibility)
			// Mapping Strategy: coordinator_tools.planTemplateId <->
			// subplan_tool_def.planTemplateId (unique)
			// This ensures data consistency between the two tables when deleting
			// coordinator tools
			try {
				Optional<SubplanToolDef> existingToolOpt = subplanToolService
					.getSubplanToolByTemplate(existingEntity.getPlanTemplateId());
				if (existingToolOpt.isPresent()) {
					// Delete the matching SubplanToolDef entry for this planTemplateId
					SubplanToolDef tool = existingToolOpt.get();
					subplanToolService.deleteSubplanTool(tool.getId());
					log.info("Successfully deleted subplan tool: {} with ID: {} for planTemplateId: {}",
							tool.getToolName(), tool.getId(), existingEntity.getPlanTemplateId());
				}
				else {
					log.warn("No SubplanToolDef found for planTemplateId: {}", existingEntity.getPlanTemplateId());
				}
			}
			catch (Exception e) {
				log.warn("Failed to delete subplan tool for planTemplateId: {}, continuing with entity deletion: {}",
						existingEntity.getPlanTemplateId(), e.getMessage());
			}

			// Delete CoordinatorToolEntity
			coordinatorToolRepository.deleteById(id);
			log.info("Successfully deleted CoordinatorToolEntity with ID: {}", id);

		}
		catch (CoordinatorToolException e) {
			throw e;
		}
		catch (Exception e) {
			log.error("Error deleting coordinator tool: {}", e.getMessage(), e);
			throw new CoordinatorToolException("INTERNAL_ERROR",
					"An unexpected error occurred while deleting coordinator tool");
		}
	}

	@Override
	public Optional<CoordinatorToolVO> getCoordinatorToolById(Long id) {
		try {
			return coordinatorToolRepository.findById(id).map(CoordinatorToolVO::fromEntity);
		}
		catch (Exception e) {
			log.error("Error getting coordinator tool by ID: {}", e.getMessage(), e);
			return Optional.empty();
		}
	}

	@Override
	public Optional<CoordinatorToolVO> getCoordinatorToolByPlanTemplateId(String planTemplateId) {
		try {
			List<CoordinatorToolEntity> entities = coordinatorToolRepository.findByPlanTemplateId(planTemplateId);
			if (!entities.isEmpty()) {
				return Optional.of(CoordinatorToolVO.fromEntity(entities.get(0)));
			}
			return Optional.empty();
		}
		catch (Exception e) {
			log.error("Error getting coordinator tool by plan template ID: {}", e.getMessage(), e);
			return Optional.empty();
		}
	}

	@Override
	public CoordinatorToolVO getOrCreateCoordinatorToolByPlanTemplateId(String planTemplateId) {
		try {
			// Try to get existing tool
			Optional<CoordinatorToolVO> existingTool = getCoordinatorToolByPlanTemplateId(planTemplateId);
			if (existingTool.isPresent()) {
				return existingTool.get();
			}

			// Create default tool VO (not saved to database)
			return createDefaultToolVO(planTemplateId);
		}
		catch (Exception e) {
			log.error("Error getting or creating coordinator tool: {}", e.getMessage(), e);
			return createDefaultToolVO(planTemplateId);
		}
	}

	@Override
	public List<CoordinatorToolVO> getAllCoordinatorTools() {
		try {
			return coordinatorToolRepository.findAll()
				.stream()
				.map(CoordinatorToolVO::fromEntity)
				.collect(Collectors.toList());
		}
		catch (Exception e) {
			log.error("Error getting all coordinator tools: {}", e.getMessage(), e);
			return new ArrayList<>();
		}
	}

	@Override
	public List<String> getAllUniqueMcpEndpoints() {
		try {
			return coordinatorToolRepository.findAllUniqueMcpEndpoints();
		}
		catch (Exception e) {
			log.error("Error getting unique MCP endpoints: {}", e.getMessage(), e);
			return new ArrayList<>();
		}
	}

	/**
	 * Validate CoordinatorToolVO
	 */
	private void validateCoordinatorToolVO(CoordinatorToolVO toolVO) throws CoordinatorToolException {
		if (toolVO.getToolName() == null || toolVO.getToolName().trim().isEmpty()) {
			throw new CoordinatorToolException("VALIDATION_ERROR", "Tool name is required");
		}
		if (toolVO.getToolDescription() == null || toolVO.getToolDescription().trim().isEmpty()) {
			throw new CoordinatorToolException("VALIDATION_ERROR", "Tool description is required");
		}
		if (toolVO.getPlanTemplateId() == null || toolVO.getPlanTemplateId().trim().isEmpty()) {
			throw new CoordinatorToolException("VALIDATION_ERROR", "Plan template ID is required");
		}

		// Validate service enablement and endpoints
		if (toolVO.getEnableMcpService() != null && toolVO.getEnableMcpService()
				&& (toolVO.getMcpEndpoint() == null || toolVO.getMcpEndpoint().trim().isEmpty())) {
			throw new CoordinatorToolException("VALIDATION_ERROR",
					"MCP endpoint is required when MCP service is enabled");
		}

		// At least one service must be enabled
		boolean hasEnabledService = (toolVO.getEnableInternalToolcall() != null && toolVO.getEnableInternalToolcall())
				|| (toolVO.getEnableHttpService() != null && toolVO.getEnableHttpService())
				|| (toolVO.getEnableMcpService() != null && toolVO.getEnableMcpService());
		if (!hasEnabledService) {
			throw new CoordinatorToolException("VALIDATION_ERROR", "At least one service must be enabled");
		}
	}

	/**
	 * Set default values for CoordinatorToolVO
	 */
	private void setDefaultValues(CoordinatorToolVO toolVO) {
		if (toolVO.getInputSchema() == null || toolVO.getInputSchema().trim().isEmpty()) {
			toolVO.setInputSchema("[]");
		}
		if (toolVO.getPublishStatus() == null) {
			toolVO.setPublishStatus("UNPUBLISHED");
		}
		if (toolVO.getEnableInternalToolcall() == null) {
			toolVO.setEnableInternalToolcall(false);
		}
		if (toolVO.getEnableHttpService() == null) {
			toolVO.setEnableHttpService(false);
		}
		if (toolVO.getEnableMcpService() == null) {
			toolVO.setEnableMcpService(false);
		}
	}

	/**
	 * Update CoordinatorToolEntity from CoordinatorToolVO
	 */
	private void updateCoordinatorToolEntityFromVO(CoordinatorToolEntity entity, CoordinatorToolVO toolVO) {
		entity.setToolName(toolVO.getToolName());
		entity.setToolDescription(toolVO.getToolDescription());
		entity.setInputSchema(toolVO.getInputSchema());
		entity.setPlanTemplateId(toolVO.getPlanTemplateId());
		entity.setMcpEndpoint(toolVO.getMcpEndpoint());
		entity.setServiceGroup(toolVO.getServiceGroup());

		// Set service enablement flags
		entity.setEnableInternalToolcall(
				toolVO.getEnableInternalToolcall() != null ? toolVO.getEnableInternalToolcall() : true);
		entity.setEnableHttpService(toolVO.getEnableHttpService() != null ? toolVO.getEnableHttpService() : false);
		entity.setEnableMcpService(toolVO.getEnableMcpService() != null ? toolVO.getEnableMcpService() : false);
	}

	/**
	 * Create SubplanToolDef from CoordinatorToolVO
	 */
	private SubplanToolDef createSubplanToolDefFromVO(CoordinatorToolVO toolVO) {
		SubplanToolDef toolDef = new SubplanToolDef();
		toolDef.setToolName(toolVO.getToolName());
		toolDef.setToolDescription(toolVO.getToolDescription());
		toolDef.setPlanTemplateId(toolVO.getPlanTemplateId());

		// Set endpoint based on enabled services
		if (toolVO.getEnableMcpService() != null && toolVO.getEnableMcpService() && toolVO.getMcpEndpoint() != null) {
			toolDef.setEndpoint(toolVO.getMcpEndpoint());
		}
		else if (toolVO.getEnableInternalToolcall() != null && toolVO.getEnableInternalToolcall()) {
			toolDef.setEndpoint("internal-toolcall");
		}
		else {
			toolDef.setEndpoint("internal-toolcall");
		}

		toolDef.setServiceGroup(toolVO.getServiceGroup());

		// Parse input schema and create parameters
		try {
			JsonNode inputParams = objectMapper.readTree(toolVO.getInputSchema());
			List<SubplanParamDef> parameters = new ArrayList<>();

			if (inputParams.isArray()) {
				for (JsonNode param : inputParams) {
					String paramName = param.get("name").asText();
					String paramType = param.get("type").asText();
					String paramDescription = param.get("description").asText();
					boolean required = param.has("required") ? param.get("required").asBoolean() : true;

					SubplanParamDef paramDef = new SubplanParamDef(paramName, paramType, paramDescription, required);
					parameters.add(paramDef);
				}
			}

			toolDef.setInputSchema(parameters);

		}
		catch (Exception e) {
			log.warn("Failed to parse input schema for subplan tool: {}", e.getMessage());
			toolDef.setInputSchema(new ArrayList<>());
		}

		return toolDef;
	}

	/**
	 * Create default CoordinatorToolVO for a plan template
	 */
	private CoordinatorToolVO createDefaultToolVO(String planTemplateId) {
		CoordinatorToolVO toolVO = new CoordinatorToolVO();
		toolVO.setToolName(null);
		toolVO.setToolDescription(null);
		toolVO.setPlanTemplateId(planTemplateId);
		toolVO.setEnableInternalToolcall(false);
		toolVO.setEnableHttpService(false);
		toolVO.setEnableMcpService(false);
		toolVO.setInputSchema("[]");
		toolVO.setPublishStatus("UNPUBLISHED");
		toolVO.setServiceGroup(null);

		return toolVO;
	}

}

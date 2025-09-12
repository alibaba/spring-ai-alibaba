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
import com.alibaba.cloud.ai.manus.planning.service.PlanTemplateService;
import com.alibaba.cloud.ai.manus.planning.service.IPlanParameterMappingService;
import com.alibaba.cloud.ai.manus.runtime.service.PlanIdDispatcher;
import com.alibaba.cloud.ai.manus.runtime.service.PlanningCoordinator;
import com.alibaba.cloud.ai.manus.subplan.model.po.SubplanToolDef;
import com.alibaba.cloud.ai.manus.subplan.model.vo.SubplanToolWrapper;
import com.alibaba.cloud.ai.manus.subplan.repository.SubplanToolDefRepository;
import com.alibaba.cloud.ai.manus.tool.code.ToolExecuteResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service implementation for managing subplan tools
 *
 * Integrates with the existing PlanningFactory tool registry system
 */
@Service
@Transactional
public class SubplanToolService implements ISubplanToolService {

	private static final Logger logger = LoggerFactory.getLogger(SubplanToolService.class);

	@Autowired
	private SubplanToolDefRepository subplanToolDefRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private PlanTemplateService planTemplateService;

	@Autowired
	private PlanningCoordinator planningCoordinator;

	@Autowired
	private PlanIdDispatcher planIdDispatcher;

	@Autowired
	private IPlanParameterMappingService parameterMappingService;

	@Override
	public List<SubplanToolDef> getAllSubplanTools() {
		logger.debug("Fetching all subplan tools from database");
		return subplanToolDefRepository.findAll();
	}

	@Override
	public Optional<SubplanToolDef> getSubplanToolByTemplate(String planTemplateId) {
		logger.debug("Fetching subplan tool for template: {}", planTemplateId);
		return subplanToolDefRepository.findOneByPlanTemplateId(planTemplateId);
	}

	@Override
	public List<SubplanToolDef> getSubplanToolsByEndpoint(String endpoint) {
		logger.debug("Fetching subplan tools for endpoint: {}", endpoint);
		return subplanToolDefRepository.findByEndpoint(endpoint);
	}

	@Override
	public Map<String, PlanningFactory.ToolCallBackContext> createSubplanToolCallbacks(String planId, String rootPlanId,
			String expectedReturnInfo) {

		logger.info("Creating subplan tool callbacks for planId: {}, rootPlanId: {}", planId, rootPlanId);

		Map<String, PlanningFactory.ToolCallBackContext> toolCallbackMap = new HashMap<>();

		try {
			// Get all subplan tools from database
			List<SubplanToolDef> subplanTools = subplanToolDefRepository.findAllWithParameters();

			if (subplanTools.isEmpty()) {
				logger.info("No subplan tools found in database");
				return toolCallbackMap;
			}

			logger.info("Found {} subplan tools to register", subplanTools.size());

			for (SubplanToolDef subplanTool : subplanTools) {
				try {
					// Create a SubplanToolWrapper that extends AbstractBaseTool
					SubplanToolWrapper toolWrapper = new SubplanToolWrapper(subplanTool, planId, rootPlanId,
							planTemplateService, planningCoordinator, planIdDispatcher, objectMapper,
							parameterMappingService);

					// Create FunctionToolCallback
					FunctionToolCallback<Map<String, Object>, ToolExecuteResult> functionToolCallback = FunctionToolCallback
						.builder(subplanTool.getToolName(), toolWrapper)
						.description(subplanTool.getToolDescription())
						.inputSchema(convertParametersToSchema(subplanTool))
						.inputType(Map.class) // Map input type for subplan tools
						.toolMetadata(ToolMetadata.builder().returnDirect(false).build())
						.build();

					// Create ToolCallBackContext
					PlanningFactory.ToolCallBackContext context = new PlanningFactory.ToolCallBackContext(
							functionToolCallback, toolWrapper);

					toolCallbackMap.put(subplanTool.getToolName(), context);

					logger.info("Successfully registered subplan tool: {} -> {}", subplanTool.getToolName(),
							subplanTool.getPlanTemplateId());

				}
				catch (Exception e) {
					logger.error("Failed to register subplan tool: {}", subplanTool.getToolName(), e);
				}
			}

		}
		catch (Exception e) {
			logger.error("Error creating subplan tool callbacks", e);
		}

		logger.info("Created {} subplan tool callbacks", toolCallbackMap.size());
		return toolCallbackMap;
	}

	@Override
	public SubplanToolDef registerSubplanTool(SubplanToolDef toolDef) {
		logger.info("Registering new subplan tool: {}", toolDef.getToolName());

		if (subplanToolDefRepository.existsByToolName(toolDef.getToolName())) {
			throw new IllegalArgumentException("Subplan tool with name '" + toolDef.getToolName() + "' already exists");
		}

		SubplanToolDef savedTool = subplanToolDefRepository.save(toolDef);
		logger.info("Successfully registered subplan tool: {} with ID: {}", savedTool.getToolName(), savedTool.getId());

		return savedTool;
	}

	@Override
	public SubplanToolDef updateSubplanTool(SubplanToolDef toolDef) {
		logger.info("Updating subplan tool: {} with ID: {}", toolDef.getToolName(), toolDef.getId());

		if (toolDef.getId() == null) {
			throw new IllegalArgumentException("Tool ID cannot be null for update operation");
		}

		if (!subplanToolDefRepository.existsById(toolDef.getId())) {
			throw new IllegalArgumentException("Subplan tool with ID " + toolDef.getId() + " not found");
		}

		SubplanToolDef updatedTool = subplanToolDefRepository.save(toolDef);
		logger.info("Successfully updated subplan tool: {} with ID: {}", updatedTool.getToolName(),
				updatedTool.getId());

		return updatedTool;
	}

	@Override
	public void deleteSubplanTool(Long id) {
		logger.info("Deleting subplan tool with ID: {}", id);

		if (!subplanToolDefRepository.existsById(id)) {
			throw new IllegalArgumentException("Subplan tool with ID " + id + " not found");
		}

		subplanToolDefRepository.deleteById(id);
		logger.info("Successfully deleted subplan tool with ID: {}", id);
	}

	@Override
	public boolean existsByToolName(String toolName) {
		return subplanToolDefRepository.existsByToolName(toolName);
	}

	@Override
	public SubplanToolDef getByToolName(String toolName) {
		return subplanToolDefRepository.findByToolName(toolName).orElse(null);
	}

	/**
	 * Convert SubplanParamDef parameters to JSON schema format
	 */
	private String convertParametersToSchema(SubplanToolDef subplanTool) {
		try {
			if (subplanTool.getInputSchema() == null || subplanTool.getInputSchema().isEmpty()) {
				return "{}";
			}

			// Convert List<SubplanParamDef> to JSON schema format
			Map<String, Object> schema = new HashMap<>();
			schema.put("type", "object");

			Map<String, Object> properties = new HashMap<>();
			List<String> required = new ArrayList<>();

			for (com.alibaba.cloud.ai.manus.subplan.model.po.SubplanParamDef param : subplanTool.getInputSchema()) {
				Map<String, Object> paramSchema = new HashMap<>();
				paramSchema.put("type", param.getType().toLowerCase());
				paramSchema.put("description", param.getDescription());

				properties.put(param.getName(), paramSchema);

				if (param.isRequired()) {
					required.add(param.getName());
				}
			}

			schema.put("properties", properties);
			if (!required.isEmpty()) {
				schema.put("required", required);
			}

			return objectMapper.writeValueAsString(schema);

		}
		catch (Exception e) {
			logger.error("Error converting parameters to schema for tool: {}", subplanTool.getToolName(), e);
			return "{}";
		}
	}

}

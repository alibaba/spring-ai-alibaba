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
package com.alibaba.cloud.ai.manus.planning.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.cloud.ai.manus.planning.PlanningFactory;
import com.alibaba.cloud.ai.manus.planning.model.po.PlanTemplate;
import com.alibaba.cloud.ai.manus.planning.service.IPlanCreator;
import com.alibaba.cloud.ai.manus.planning.service.PlanTemplateService;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.ExecutionContext;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.PlanInterface;
import com.alibaba.cloud.ai.manus.runtime.service.PlanIdDispatcher;
import com.alibaba.cloud.ai.manus.planning.service.IPlanParameterMappingService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Plan template controller, handles API requests for the plan template page
 */
@RestController
@RequestMapping("/api/plan-template")
public class PlanTemplateController {

	private static final Logger logger = LoggerFactory.getLogger(PlanTemplateController.class);

	@Autowired
	@Lazy
	private PlanningFactory planningFactory;

	@Autowired
	private PlanTemplateService planTemplateService;

	@Autowired
	private PlanIdDispatcher planIdDispatcher;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private IPlanParameterMappingService parameterMappingService;

	/**
	 * Serialize plan object to JSON string
	 * @param plan Plan object
	 * @return Formatted JSON string (with indentation and line breaks)
	 * @throws Exception Thrown when serialization fails
	 */
	private String planToJson(PlanInterface plan) throws Exception {
		return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(plan);
	}

	/**
	 * Generate plan
	 * @param request Request containing plan requirements and optional JSON data
	 * @return Complete JSON data for the plan
	 */
	@PostMapping("/generate")
	public ResponseEntity<Map<String, Object>> generatePlan(@RequestBody Map<String, String> request) {
		String query = request.get("query");
		String existingJson = request.get("existingJson"); // Get possible existing JSON
		String planType = request.get("planType"); // Get plan type

		// Default to simple plan type if not specified
		if (planType == null || planType.trim().isEmpty()) {
			planType = "simple";
		}

		if (query == null || query.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Plan description cannot be empty"));
		}

		ExecutionContext context = new ExecutionContext();
		// If there is existing JSON data, add it to the user request
		String enhancedQuery;
		if (existingJson != null && !existingJson.trim().isEmpty()) {
			// Escape curly braces in JSON to prevent String.format from misinterpreting
			// them as placeholders
			String escapedJson = existingJson.replace("{", "\\{").replace("}", "\\}");
			enhancedQuery = String.format(
					"Refer to the past execution plan %s and the user's new query: %s. Build a new execution plan.",
					escapedJson, query);
		}
		else {
			enhancedQuery = query;
		}
		context.setUserRequest(enhancedQuery);

		// Use PlanIdDispatcher to generate a unique plan template ID
		String planTemplateId = planIdDispatcher.generatePlanTemplateId();
		context.setCurrentPlanId(planTemplateId);
		context.setRootPlanId(planTemplateId);
		context.setNeedSummary(false); // We don't need to generate a summary, because we
										// only need the plan

		try {
			// Create PlanCreator using PlanningFactory with specified plan type
			IPlanCreator planCreator = planningFactory.createPlanCreator(planType);

			// Create plan using PlanCreator directly
			planCreator.createPlanWithoutMemory(context);
			logger.info("Plan generation successful: {}", planTemplateId);

			// Get the generated plan from the recorder
			if (context.getPlan() == null) {
				return ResponseEntity.internalServerError()
					.body(Map.of("error", "Plan generation failed, cannot get plan data"));
			}

			// Get plan JSON - using Jackson serialization
			String planJson;
			try {
				planJson = planToJson(context.getPlan());
			}
			catch (Exception jsonException) {
				logger.error("Failed to serialize plan to JSON", jsonException);
				return ResponseEntity.internalServerError()
					.body(Map.of("error", "Plan serialization failed: " + jsonException.getMessage()));
			}

			// Save to version history
			PlanTemplateService.VersionSaveResult saveResult = saveToVersionHistory(planJson);

			// Return plan data
			Map<String, Object> response = new HashMap<>();
			response.put("planTemplateId", planTemplateId);
			response.put("status", "completed");
			response.put("planJson", planJson);
			response.put("saved", saveResult.isSaved());
			response.put("duplicate", saveResult.isDuplicate());
			response.put("saveMessage", saveResult.getMessage());

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Plan generation failed", e);
			return ResponseEntity.internalServerError()
				.body(Map.of("error", "Plan generation failed: " + e.getMessage()));
		}
	}

	/**
	 * Save version history
	 * @param planJson Plan JSON data
	 * @return Save result
	 */
	private PlanTemplateService.VersionSaveResult saveToVersionHistory(String planJson) {
		try {
			// Parse JSON to extract planTemplateId and title
			PlanInterface planData = objectMapper.readValue(planJson, PlanInterface.class);

			String planTemplateId = planData.getPlanTemplateId();
			if (planTemplateId == null || planTemplateId.trim().isEmpty()) {
				String newPlanTemplateId = planIdDispatcher.generatePlanTemplateId();
				planData.setPlanTemplateId(newPlanTemplateId);
				planTemplateId = newPlanTemplateId;

				// Re-serialize the updated plan object to JSON
				planJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(planData);
				planTemplateId = newPlanTemplateId;
			}

			String title = planData.getTitle();

			if (planTemplateId == null || planTemplateId.trim().isEmpty()) {
				throw new IllegalArgumentException("Plan ID cannot be found in JSON");
			}
			if (title == null || title.trim().isEmpty()) {
				title = "Untitled Plan";
			}

			// Check if the plan exists
			PlanTemplate template = planTemplateService.getPlanTemplate(planTemplateId);
			if (template == null) {
				// If it doesn't exist, create a new plan
				planTemplateService.savePlanTemplate(planTemplateId, title, title, planJson, false);
				logger.info("New plan created: {}", planTemplateId);
				return new PlanTemplateService.VersionSaveResult(true, false, "New plan created", 0);
			}
			else {
				// If it exists, update the template with new title and save a new version
				boolean updated = planTemplateService.updatePlanTemplate(planTemplateId, title, planJson, false);
				if (updated) {
					logger.info("Updated plan template {} with new title and saved new version", planTemplateId);
					// Get the latest version index after update
					Integer maxVersionIndex = planTemplateService.getPlanVersions(planTemplateId).size() - 1;
					return new PlanTemplateService.VersionSaveResult(true, false,
							"Plan template updated and new version saved", maxVersionIndex);
				}
				else {
					// Fallback to just saving version if update failed
					PlanTemplateService.VersionSaveResult result = planTemplateService
						.saveToVersionHistory(planTemplateId, planJson);
					if (result.isSaved()) {
						logger.info("New version of plan {} saved", planTemplateId, result.getVersionIndex());
					}
					else {
						logger.info("Plan {} is the same, no new version saved", planTemplateId);
					}
					return result;
				}
			}
		}
		catch (Exception e) {
			logger.error("Failed to parse plan JSON", e);
			throw new RuntimeException("Failed to save version history: " + e.getMessage());
		}
	}

	/**
	 * Save plan
	 * @param request Request containing plan ID and JSON
	 * @return Save result
	 */
	@PostMapping("/save")
	public ResponseEntity<Map<String, Object>> savePlan(@RequestBody Map<String, String> request) {
		String planJson = request.get("planJson");

		if (planJson == null || planJson.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Plan data cannot be empty"));
		}

		try {
			// Parse JSON to get planId
			PlanInterface planData = objectMapper.readValue(planJson, PlanInterface.class);
			String planId = planData.getPlanTemplateId();
			if (planId == null) {
				planId = planData.getRootPlanId();
			}

			// Check if planId is empty or starts with "new-", then generate a new one
			if (planId == null || planId.trim().isEmpty() || planId.startsWith("new-")) {
				String newPlanId = planIdDispatcher.generatePlanTemplateId();
				logger.info("Original planId '{}' is empty or starts with 'new-', generated new planId: {}", planId,
						newPlanId);

				// Update the plan object with new ID
				planData.setCurrentPlanId(newPlanId);
				planData.setRootPlanId(newPlanId);

				// Re-serialize the updated plan object to JSON
				planJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(planData);
				planId = newPlanId;
			}

			if (planId == null || planId.trim().isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("error", "Plan ID cannot be found in JSON"));
			}

			// Save to version history
			PlanTemplateService.VersionSaveResult saveResult = saveToVersionHistory(planJson);

			// Calculate version count
			List<String> versions = planTemplateService.getPlanVersions(planId);
			int versionCount = versions.size();

			// Build response
			Map<String, Object> response = new HashMap<>();
			response.put("status", "success");
			response.put("planId", planId);
			response.put("versionCount", versionCount);
			response.put("saved", saveResult.isSaved());
			response.put("duplicate", saveResult.isDuplicate());
			response.put("message", saveResult.getMessage());
			response.put("versionIndex", saveResult.getVersionIndex());

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Failed to save plan", e);
			return ResponseEntity.internalServerError().body(Map.of("error", "Failed to save plan: " + e.getMessage()));
		}
	}

	/**
	 * Get the version history of the plan
	 * @param request Request containing plan ID
	 * @return Version history list
	 */
	@PostMapping("/versions")
	public ResponseEntity<Map<String, Object>> getPlanVersions(@RequestBody Map<String, String> request) {
		String planId = request.get("planId");

		if (planId == null || planId.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Plan ID cannot be empty"));
		}

		List<String> versions = planTemplateService.getPlanVersions(planId);

		Map<String, Object> response = new HashMap<>();
		response.put("planId", planId);
		response.put("versionCount", versions.size());
		response.put("versions", versions);

		return ResponseEntity.ok(response);
	}

	/**
	 * Get a specific version of the plan
	 * @param request Request containing plan ID and version index
	 * @return Specific version of the plan
	 */
	@PostMapping("/get-version")
	public ResponseEntity<Map<String, Object>> getVersionPlan(@RequestBody Map<String, String> request) {
		String planId = request.get("planId");
		String versionIndex = request.get("versionIndex");

		if (planId == null || planId.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Plan ID cannot be empty"));
		}

		try {
			int index = Integer.parseInt(versionIndex);
			List<String> versions = planTemplateService.getPlanVersions(planId);

			if (versions.isEmpty()) {
				return ResponseEntity.notFound().build();
			}

			if (index < 0 || index >= versions.size()) {
				return ResponseEntity.badRequest().body(Map.of("error", "Version index out of range"));
			}

			String planJson = planTemplateService.getPlanVersion(planId, index);

			if (planJson == null) {
				return ResponseEntity.notFound().build();
			}

			Map<String, Object> response = new HashMap<>();
			response.put("planId", planId);
			response.put("versionIndex", index);
			response.put("versionCount", versions.size());
			response.put("planJson", planJson);

			return ResponseEntity.ok(response);
		}
		catch (NumberFormatException e) {
			return ResponseEntity.badRequest().body(Map.of("error", "Version index must be a number"));
		}
		catch (Exception e) {
			logger.error("Failed to get plan version", e);
			return ResponseEntity.internalServerError()
				.body(Map.of("error", "Failed to get plan version: " + e.getMessage()));
		}
	}

	/**
	 * Get all plan templates
	 * @return All plan templates
	 */
	@GetMapping("/list")
	public ResponseEntity<Map<String, Object>> getAllPlanTemplates() {
		try {
			// Use PlanTemplateService to get all plan templates
			// Since there is no direct method to get all templates, we use the findAll
			// method of PlanTemplateRepository
			List<PlanTemplate> templates = planTemplateService.getAllPlanTemplates();

			// Build response data
			List<Map<String, Object>> templateList = new ArrayList<>();
			for (PlanTemplate template : templates) {
				Map<String, Object> templateData = new HashMap<>();
				if (template.isInternalToolcall()) {
					continue;
				}
				templateData.put("id", template.getPlanTemplateId());
				templateData.put("title", template.getTitle());
				templateData.put("description", template.getUserRequest());
				templateData.put("createTime", template.getCreateTime());
				templateData.put("updateTime", template.getUpdateTime());
				templateList.add(templateData);
			}

			Map<String, Object> response = new HashMap<>();
			response.put("templates", templateList);
			response.put("count", templateList.size());

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Failed to get plan template list", e);
			return ResponseEntity.internalServerError()
				.body(Map.of("error", "Failed to get plan template list: " + e.getMessage()));
		}
	}

	/**
	 * Update plan template
	 * @param request Request containing plan template ID, plan requirements and optional
	 * JSON data
	 * @return Updated plan JSON data
	 */
	@PostMapping("/update")
	public ResponseEntity<Map<String, Object>> updatePlanTemplate(@RequestBody Map<String, String> request) {
		String planId = request.get("planId");
		String query = request.get("query");
		String existingJson = request.get("existingJson"); // Get possible existing JSON
		String planType = request.get("planType"); // Get plan type

		// Default to simple plan type if not specified
		if (planType == null || planType.trim().isEmpty()) {
			planType = "simple";
		}

		if (planId == null || planId.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Plan template ID cannot be empty"));
		}

		if (query == null || query.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Plan description cannot be empty"));
		}

		// Check if the plan template exists
		PlanTemplate template = planTemplateService.getPlanTemplate(planId);
		if (template == null) {
			return ResponseEntity.notFound().build();
		}

		ExecutionContext context = new ExecutionContext();
		// If there is existing JSON data, add it to the user request
		String enhancedQuery;
		if (existingJson != null && !existingJson.trim().isEmpty()) {
			// Escape curly braces in JSON to prevent String.format from misinterpreting
			// them as placeholders
			String escapedJson = existingJson.replace("{", "\\{").replace("}", "\\}");
			enhancedQuery = String.format(
					"Refer to the past execution plan %s and the user's new query: %s. Update this execution plan.",
					escapedJson, query);
		}
		else {
			enhancedQuery = query;
		}
		context.setUserRequest(enhancedQuery);

		// Use the existing plan template ID
		context.setCurrentPlanId(planId);
		context.setRootPlanId(planId);
		context.setNeedSummary(false); // We don't need to generate a summary, because we
										// only need the plan

		// Get planning flow
		// planningCoordinator.createPlan(context); // This line is removed as per the
		// new_code

		try {
			// Create PlanCreator using PlanningFactory with specified plan type
			IPlanCreator planCreator = planningFactory.createPlanCreator(planType);

			// Create plan using PlanCreator directly
			planCreator.createPlanWithoutMemory(context);
			logger.info("Plan template updated successfully: {}", planId);

			// Get the generated plan from the recorder
			if (context.getPlan() == null) {
				return ResponseEntity.internalServerError()
					.body(Map.of("error", "Plan update failed, cannot get plan data"));
			}

			// Get plan JSON - using Jackson serialization
			String planJson;
			try {
				planJson = planToJson(context.getPlan());
			}
			catch (Exception jsonException) {
				logger.error("Failed to serialize plan to JSON", jsonException);
				return ResponseEntity.internalServerError()
					.body(Map.of("error", "Plan serialization failed: " + jsonException.getMessage()));
			}

			// Save to version history
			PlanTemplateService.VersionSaveResult saveResult = saveToVersionHistory(planJson);

			// Return plan data
			Map<String, Object> response = new HashMap<>();
			response.put("planTemplateId", planId);
			response.put("status", "completed");
			response.put("planJson", planJson);
			response.put("saved", saveResult.isSaved());
			response.put("duplicate", saveResult.isDuplicate());
			response.put("saveMessage", saveResult.getMessage());

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Failed to update plan template", e);
			return ResponseEntity.internalServerError()
				.body(Map.of("error", "Failed to update plan template: " + e.getMessage()));
		}
	}

	/**
	 * Delete plan template
	 * @param request Request containing plan ID
	 * @return Delete result
	 */
	@PostMapping("/delete")
	public ResponseEntity<Map<String, Object>> deletePlanTemplate(@RequestBody Map<String, String> request) {
		String planId = request.get("planId");

		if (planId == null || planId.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Plan ID cannot be empty"));
		}

		try {
			// Check if the plan template exists
			PlanTemplate template = planTemplateService.getPlanTemplate(planId);
			if (template == null) {
				return ResponseEntity.notFound().build();
			}

			// Delete the plan template and all versions
			boolean deleted = planTemplateService.deletePlanTemplate(planId);

			if (deleted) {
				logger.info("Plan template deleted successfully: {}", planId);
				return ResponseEntity
					.ok(Map.of("status", "success", "message", "Plan template deleted", "planId", planId));
			}
			else {
				logger.error("Failed to delete plan template: {}", planId);
				return ResponseEntity.internalServerError().body(Map.of("error", "Failed to delete plan template"));
			}
		}
		catch (Exception e) {
			logger.error("Failed to delete plan template", e);
			return ResponseEntity.internalServerError()
				.body(Map.of("error", "Failed to delete plan template: " + e.getMessage()));
		}
	}

	/**
	 * Get parameter requirements for a plan template
	 * @param planTemplateId The plan template ID
	 * @return List of required parameters
	 */
	@GetMapping("/{planTemplateId}/parameters")
	public ResponseEntity<Map<String, Object>> getParameterRequirements(@PathVariable String planTemplateId) {
		try {
			PlanTemplate planTemplate = planTemplateService.getPlanTemplate(planTemplateId);
			if (planTemplate == null) {
				return ResponseEntity.notFound().build();
			}

			String planJson = planTemplateService.getLatestPlanVersion(planTemplateId);
			if (planJson == null) {
				return ResponseEntity.notFound().build();
			}

			List<String> parameters = parameterMappingService.extractParameterPlaceholders(planJson);

			Map<String, Object> response = new HashMap<>();
			response.put("parameters", parameters);
			response.put("hasParameters", !parameters.isEmpty());
			response.put("requirements", parameterMappingService.getParameterRequirements(planJson));

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Failed to get parameter requirements for plan template: " + planTemplateId, e);
			return ResponseEntity.internalServerError()
				.body(Map.of("error", "Failed to get parameter requirements: " + e.getMessage()));
		}
	}

}

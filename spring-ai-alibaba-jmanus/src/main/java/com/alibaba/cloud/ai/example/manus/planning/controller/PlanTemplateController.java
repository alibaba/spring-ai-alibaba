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
package com.alibaba.cloud.ai.example.manus.planning.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.cloud.ai.example.manus.planning.PlanningFactory;
import com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanIdDispatcher;
import com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanningCoordinator;
import com.alibaba.cloud.ai.example.manus.planning.model.po.PlanTemplate;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.PlanInterface;
import com.alibaba.cloud.ai.example.manus.planning.service.PlanTemplateService;
import com.alibaba.cloud.ai.example.manus.recorder.service.PlanExecutionRecorder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.alibaba.cloud.ai.example.manus.planning.creator.PlanCreator;

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
	private PlanExecutionRecorder planExecutionRecorder;

	@Autowired
	private PlanTemplateService planTemplateService;

	@Autowired
	private PlanIdDispatcher planIdDispatcher;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private PlanningCoordinator planningCoordinator;

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
															// data

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
			// Create PlanCreator using PlanningFactory
			PlanCreator planCreator = planningFactory.createPlanCreator();

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
	 * Execute plan by plan template ID (POST method)
	 * @param request Request containing plan template ID
	 * @return Result status
	 */
	@PostMapping("/executePlanByTemplateId")
	public ResponseEntity<Map<String, Object>> executePlanByTemplateId(@RequestBody Map<String, Object> request) {
		String planTemplateId = (String) request.get("planTemplateId");
		if (planTemplateId == null || planTemplateId.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Plan template ID cannot be empty"));
		}

		String rawParam = (String) request.get("rawParam");

		// Handle uploaded files if present
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> uploadedFiles = (List<Map<String, Object>>) request.get("uploadedFiles");

		logger.info("Received request with uploadedFiles: {}", uploadedFiles != null ? uploadedFiles.size() : "null");
		if (uploadedFiles != null && !uploadedFiles.isEmpty()) {
			logger.info("First uploaded file planId: {}", uploadedFiles.get(0).get("planId"));
		}

		return executePlanAndBuildResponse(planTemplateId, rawParam, uploadedFiles);
	}

	/**
	 * Execute plan by template ID and build response Note: This method submits the
	 * execution task asynchronously and returns immediately. The actual execution happens
	 * in the background. To track execution status, you may need to implement a separate
	 * status checking endpoint.
	 * @param planTemplateId The plan template ID to execute
	 * @param rawParam Raw parameters for execution (can be null)
	 * @param uploadedFiles List of uploaded files (can be null)
	 * @return ResponseEntity with submission status
	 */
	private ResponseEntity<Map<String, Object>> executePlanAndBuildResponse(String planTemplateId, String rawParam,
			List<Map<String, Object>> uploadedFiles) {
		try {
			// Submit the plan execution task asynchronously
			String rootPlanId = executePlanTemplate(planTemplateId, rawParam, uploadedFiles);

			// Return success immediately when task is submitted
			Map<String, Object> response = new HashMap<>();
			response.put("planId", rootPlanId);
			response.put("status", "processing");
			response.put("message", "Task submitted, processing");

			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			logger.error("Failed to submit plan execution task: {}", planTemplateId, e);
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("error", "Failed to submit plan execution task: " + e.getMessage());
			errorResponse.put("planId", planTemplateId);
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	/**
	 * Execute a plan template by its ID using PlanningCoordinator This method fetches the
	 * plan template and executes it using the common execution logic.
	 * @param planTemplateId The ID of the plan template to execute
	 * @param rawParam Raw parameters for the execution (can be null)
	 * @param uploadedFiles List of uploaded files (can be null)
	 * @return The root plan ID for this execution
	 */
	private String executePlanTemplate(String planTemplateId, String rawParam,
			List<Map<String, Object>> uploadedFiles) {
		if (planTemplateId == null || planTemplateId.trim().isEmpty()) {
			logger.error("Plan template ID is null or empty");
			throw new IllegalArgumentException("Plan template ID cannot be null or empty");
		}

		try {
			// Generate a unique plan ID for this execution
			String currentPlanId = planIdDispatcher.generatePlanId();
			String rootPlanId = currentPlanId;

			// Fetch the plan template from PlanTemplateService
			PlanInterface plan = createPlanFromTemplate(planTemplateId, rawParam);

			if (plan == null) {
				throw new RuntimeException("Failed to create plan from template: " + planTemplateId);
			}

			// Handle uploaded files if present
			if (uploadedFiles != null && !uploadedFiles.isEmpty()) {
				// Set uploaded files context to the plan
				Map<String, String> fileContext = new HashMap<>();
				fileContext.put("hasUploadedFiles", "true");
				fileContext.put("fileCount", String.valueOf(uploadedFiles.size()));

				// Store file names and paths as comma-separated strings
				StringBuilder fileNames = new StringBuilder();
				StringBuilder filePaths = new StringBuilder();

				for (int i = 0; i < uploadedFiles.size(); i++) {
					Map<String, Object> file = uploadedFiles.get(i);
					if (i > 0) {
						fileNames.append(",");
						filePaths.append(",");
					}
					fileNames.append(String.valueOf(file.get("name")));
					filePaths.append(String.valueOf(file.get("relativePath")));
				}

				fileContext.put("uploadedFileNames", fileNames.toString());
				fileContext.put("uploadedFilePaths", filePaths.toString());
				fileContext.put("uploadPlanId", currentPlanId);

				// Add file context to plan's execution parameters
				if (plan.getExecutionParams() != null) {
					// If execution params exist, append file context
					StringBuilder enhancedParams = new StringBuilder(plan.getExecutionParams());
					enhancedParams.append("\nUploaded Files Context:\n");
					for (Map.Entry<String, String> entry : fileContext.entrySet()) {
						enhancedParams.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
					}
					plan.setExecutionParams(enhancedParams.toString());
				}
				else {
					// If no execution params, create new ones with file context
					StringBuilder params = new StringBuilder();
					for (Map.Entry<String, String> entry : fileContext.entrySet()) {
						params.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
					}
					plan.setExecutionParams(params.toString());
				}
				logger.info("Added uploaded files context to plan execution parameters: {} files",
						uploadedFiles.size());
			}

			// Execute using the PlanningCoordinator's common execution logic
			planningCoordinator.executeByPlan(plan, rootPlanId, null, currentPlanId);

			// Return the root plan ID
			return rootPlanId;

		}
		catch (Exception e) {
			logger.error("Failed to execute plan template: {}", planTemplateId, e);
			throw new RuntimeException("Execution failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Create a plan interface from template ID and parameters. Fetches the plan template
	 * from PlanTemplateService and converts it to PlanInterface.
	 * @param planTemplateId The template ID
	 * @param rawParam Raw parameters
	 * @return PlanInterface object or null if creation fails
	 */
	private PlanInterface createPlanFromTemplate(String planTemplateId, String rawParam) {
		try {
			// Fetch the latest plan version from template service
			String planJson = planTemplateService.getLatestPlanVersion(planTemplateId);

			if (planJson == null) {
				logger.error("No plan version found for template: {}", planTemplateId);
				return null;
			}

			// Parse the JSON to create a PlanInterface
			PlanInterface plan = objectMapper.readValue(planJson, PlanInterface.class);

			logger.info("Successfully created plan interface from template: {}", planTemplateId);
			return plan;

		}
		catch (Exception e) {
			logger.error("Failed to create plan interface from template: {}", planTemplateId, e);
			return null;
		}
	}

	/**
	 * Execute plan by plan template ID (GET method)
	 * @param planTemplateId Plan template ID
	 * @param allParams All URL query parameters
	 * @return Result status
	 */
	@GetMapping("/execute/{planTemplateId}")
	public ResponseEntity<Map<String, Object>> executePlanByTemplateIdGet(
			@PathVariable("planTemplateId") String planTemplateId,
			@RequestParam(required = false, name = "allParams") Map<String, String> allParams) {
		if (planTemplateId == null || planTemplateId.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Plan template ID cannot be empty"));
		}

		logger.info("Execute plan template, ID: {}, parameters: {}", planTemplateId, allParams);
		String rawParam = allParams != null ? allParams.get("rawParam") : null;
		// If there are URL parameters, use the method with parameters
		return executePlanAndBuildResponse(planTemplateId, rawParam, null);
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

			String planTemplateId = planData.getRootPlanId();
			if (planTemplateId == null || planTemplateId.trim().isEmpty()) {
				planTemplateId = planData.getCurrentPlanId();
			}

			// Check if planTemplateId is empty or starts with "new-", then generate a new
			// one
			if (planTemplateId == null || planTemplateId.trim().isEmpty() || planTemplateId.startsWith("new-")) {
				String newPlanTemplateId = planIdDispatcher.generatePlanTemplateId();
				logger.info(
						"Original planTemplateId '{}' is empty or starts with 'new-', generated new planTemplateId: {}",
						planTemplateId, newPlanTemplateId);

				// Update the plan object with new ID
				planData.setCurrentPlanId(newPlanTemplateId);
				planData.setRootPlanId(newPlanTemplateId);

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
				planTemplateService.savePlanTemplate(planTemplateId, title,
						"User request to generate plan: " + planTemplateId, planJson);
				logger.info("New plan created: {}", planTemplateId);
				return new PlanTemplateService.VersionSaveResult(true, false, "New plan created", 0);
			}
			else {
				// If it exists, save a new version
				PlanTemplateService.VersionSaveResult result = planTemplateService.saveToVersionHistory(planTemplateId,
						planJson);
				if (result.isSaved()) {
					logger.info("New version of plan {} saved", planTemplateId, result.getVersionIndex());
				}
				else {
					logger.info("Plan {} is the same, no new version saved", planTemplateId);
				}
				return result;
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
			String planId = planData.getCurrentPlanId();
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
			// Immediately execute the create plan stage, not asynchronously
			// planningCoordinator.createPlan(context); // This line is removed as per the
			// new_code
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

}

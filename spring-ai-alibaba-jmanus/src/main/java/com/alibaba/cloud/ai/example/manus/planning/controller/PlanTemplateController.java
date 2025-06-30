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
import java.util.concurrent.CompletableFuture;

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
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionPlan;
import com.alibaba.cloud.ai.example.manus.planning.service.PlanTemplateService;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;

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
			enhancedQuery = String.format("参照过去的执行计划 %s 。以及用户的新的query：%s。构建一个新的执行计划。", escapedJson, query);
		}
		else {
			enhancedQuery = query;
		}
		context.setUserRequest(enhancedQuery);

		// Use PlanIdDispatcher to generate a unique plan template ID
		String planTemplateId = planIdDispatcher.generatePlanTemplateId();
		context.setPlanId(planTemplateId);
		context.setNeedSummary(false); // We don't need to generate a summary, because we
										// only need the plan

		// Get planning flow
		PlanningCoordinator planningCoordinator = planningFactory.createPlanningCoordinator(planTemplateId);

		try {
			// Immediately execute the create plan stage, not asynchronously
			planningCoordinator.createPlan(context);
			logger.info("Plan generation successful: {}", planTemplateId);

			// Get the generated plan from the recorder
			if (context.getPlan() == null) {
				return ResponseEntity.internalServerError()
					.body(Map.of("error", "Plan generation failed, cannot get plan data"));
			}

			// Get plan JSON
			String planJson = context.getPlan().toJson();

			// Save to version history
			PlanTemplateService.VersionSaveResult saveResult = saveToVersionHistory(planTemplateId, planJson);

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
	public ResponseEntity<Map<String, Object>> executePlanByTemplateId(@RequestBody Map<String, String> request) {
		String planTemplateId = request.get("planTemplateId");
		if (planTemplateId == null || planTemplateId.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Plan template ID cannot be empty"));
		}

		String rawParam = request.get("rawParam");
		return executePlanByTemplateIdInternal(planTemplateId, rawParam);
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
		return executePlanByTemplateIdInternal(planTemplateId, rawParam);
	}

	/**
	 * Internal common method for executing plans (version with URL parameters)
	 * @param planTemplateId Plan template ID
	 * @param rawParam URL query parameters
	 * @return Result status
	 */
	private ResponseEntity<Map<String, Object>> executePlanByTemplateIdInternal(String planTemplateId,
			String rawParam) {
		try {
			// Step 1: Get execution JSON from repository by planTemplateId
			PlanTemplate template = planTemplateService.getPlanTemplate(planTemplateId);
			if (template == null) {
				return ResponseEntity.notFound().build();
			}

			// Get the latest version of the plan JSON
			List<String> versions = planTemplateService.getPlanVersions(planTemplateId);
			if (versions.isEmpty()) {
				return ResponseEntity.internalServerError()
					.body(Map.of("error", "Plan template has no executable version"));
			}
			String planJson = planTemplateService.getPlanVersion(planTemplateId, versions.size() - 1);
			if (planJson == null || planJson.trim().isEmpty()) {
				return ResponseEntity.internalServerError().body(Map.of("error", "Cannot get plan JSON data"));
			}

			// Generate a new plan ID, not using the template ID
			String newPlanId = planIdDispatcher.generatePlanId();

			// Get planning flow, using the new plan ID
			PlanningCoordinator planningCoordinator = planningFactory.createPlanningCoordinator(newPlanId);
			ExecutionContext context = new ExecutionContext();
			context.setPlanId(newPlanId);
			context.setNeedSummary(true); // We need to generate a summary

			try {
				ExecutionPlan plan = ExecutionPlan.fromJson(planJson, newPlanId);

				// Set URL parameters to ExecutionPlan
				if (rawParam != null && !rawParam.isEmpty()) {
					logger.info("Set execution parameters to plan: {}", rawParam);
					plan.setExecutionParams(rawParam);
				}

				// Set plan to context
				context.setPlan(plan);

				// Get user request from recorder
				context.setUserRequest(template.getTitle());
			}
			catch (Exception e) {
				logger.error("Failed to parse plan JSON or get user request", e);
				context.setUserRequest("Execute plan: " + newPlanId + "\nFrom template: " + planTemplateId);

				// If parsing fails, record the error but continue with the flow
				logger.warn("Using original JSON to continue execution", e);
			}

			// Execute the plan asynchronously
			CompletableFuture.runAsync(() -> {
				try {
					// Execute the plan and summary steps, skipping the create plan step
					planningCoordinator.executeExistingPlan(context);
					logger.info("Plan execution successful: {}", newPlanId);
				}
				catch (Exception e) {
					logger.error("Plan execution failed", e);
				}
			});

			// Return task ID and initial status
			Map<String, Object> response = new HashMap<>();
			response.put("planId", newPlanId);
			response.put("status", "processing");
			response.put("message", "计划执行请求已提交，正在处理中");

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Plan execution failed", e);
			return ResponseEntity.internalServerError()
				.body(Map.of("error", "Plan execution failed: " + e.getMessage()));
		}
	}

	/**
	 * Save version history
	 * @param planId Plan ID
	 * @param planJson Plan JSON data
	 * @return Save result
	 */
	private PlanTemplateService.VersionSaveResult saveToVersionHistory(String planId, String planJson) {
		// Extract title from JSON
		String title = planTemplateService.extractTitleFromPlan(planJson);

		// Check if the plan exists
		PlanTemplate template = planTemplateService.getPlanTemplate(planId);
		if (template == null) {
			// If it doesn't exist, create a new plan
			planTemplateService.savePlanTemplate(planId, title, "User request to generate plan: " + planId, planJson);
			logger.info("New plan created: {}", planId);
			return new PlanTemplateService.VersionSaveResult(true, false, "New plan created", 0);
		}
		else {
			// If it exists, save a new version
			PlanTemplateService.VersionSaveResult result = planTemplateService.saveToVersionHistory(planId, planJson);
			if (result.isSaved()) {
				logger.info("New version of plan {} saved", planId, result.getVersionIndex());
			}
			else {
				logger.info("Plan {} is the same, no new version saved", planId);
			}
			return result;
		}
	}

	/**
	 * Save plan
	 * @param request Request containing plan ID and JSON
	 * @return Save result
	 */
	@PostMapping("/save")
	public ResponseEntity<Map<String, Object>> savePlan(@RequestBody Map<String, String> request) {
		String planId = request.get("planId");
		String planJson = request.get("planJson");

		if (planId == null || planId.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Plan ID cannot be empty"));
		}

		if (planJson == null || planJson.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Plan data cannot be empty"));
		}

		try {
			// Save to version history
			PlanTemplateService.VersionSaveResult saveResult = saveToVersionHistory(planId, planJson);

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
			enhancedQuery = String.format("参照过去的执行计划 %s 。以及用户的新的query：%s。更新这个执行计划。", escapedJson, query);
		}
		else {
			enhancedQuery = query;
		}
		context.setUserRequest(enhancedQuery);

		// Use the existing plan template ID
		context.setPlanId(planId);
		context.setNeedSummary(false); // We don't need to generate a summary, because we
										// only need the plan

		// Get planning flow
		PlanningCoordinator planningCoordinator = planningFactory.createPlanningCoordinator(planId);

		try {
			// Immediately execute the create plan stage, not asynchronously
			planningCoordinator.createPlan(context);
			logger.info("Plan template updated successfully: {}", planId);

			// Get the generated plan from the recorder
			if (context.getPlan() == null) {
				return ResponseEntity.internalServerError()
					.body(Map.of("error", "Plan update failed, cannot get plan data"));
			}

			// Get plan JSON
			String planJson = context.getPlan().toJson();

			// Save to version history
			PlanTemplateService.VersionSaveResult saveResult = saveToVersionHistory(planId, planJson);

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

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
package com.alibaba.cloud.ai.example.manus.planning.service;

import com.alibaba.cloud.ai.example.manus.planning.model.po.PlanTemplate;
import com.alibaba.cloud.ai.example.manus.planning.model.po.PlanTemplateVersion;
import com.alibaba.cloud.ai.example.manus.planning.repository.PlanTemplateRepository;
import com.alibaba.cloud.ai.example.manus.planning.repository.PlanTemplateVersionRepository;
import com.alibaba.cloud.ai.example.manus.runtime.task.TaskManager;
import com.alibaba.cloud.ai.example.manus.runtime.task.PlanTask;
import com.alibaba.cloud.ai.example.manus.runtime.vo.PlanExecutionResult;
import com.alibaba.cloud.ai.example.manus.planning.executor.factory.PlanExecutorFactory;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.PlanInterface;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;

/**
 * Plan template service class that provides business logic related to plan templates
 */
@Service
public class PlanTemplateService implements IPlanTemplateService {

	private static final Logger logger = LoggerFactory.getLogger(PlanTemplateService.class);

	@Autowired
	private PlanTemplateRepository planTemplateRepository;

	@Autowired
	private PlanTemplateVersionRepository versionRepository;

	@Autowired
	private TaskManager taskManager;

	@Autowired
	private PlanExecutorFactory planExecutorFactory;

	@Autowired
	private ObjectMapper objectMapper;

	/**
	 * Save plan template and its first version
	 * @param planTemplateId Plan template ID
	 * @param title Plan title
	 * @param userRequest User request
	 * @param planJson Plan JSON data
	 */
	@Transactional
	public void savePlanTemplate(String planTemplateId, String title, String userRequest, String planJson) {
		// Save basic plan template information
		PlanTemplate template = new PlanTemplate(planTemplateId, title, userRequest);
		planTemplateRepository.save(template);

		// Save the first version
		saveToVersionHistory(planTemplateId, planJson);

		logger.info("Saved plan template {} and its first version", planTemplateId);
	}

	/**
	 * Update plan template information
	 * @param planTemplateId Plan template ID
	 * @param title Plan title
	 * @param planJson Plan JSON data
	 * @return Whether the update was successful
	 */
	@Transactional
	public boolean updatePlanTemplate(String planTemplateId, String title, String planJson) {
		Optional<PlanTemplate> templateOpt = planTemplateRepository.findByPlanTemplateId(planTemplateId);
		if (templateOpt.isPresent()) {
			PlanTemplate template = templateOpt.get();
			if (title != null && !title.isEmpty()) {
				template.setTitle(title);
			}
			template.setUpdateTime(LocalDateTime.now());
			planTemplateRepository.save(template);

			// Save new version
			saveToVersionHistory(planTemplateId, planJson);

			logger.info("Updated plan template {} and saved new version", planTemplateId);
			return true;
		}
		return false;
	}

	/**
	 * Version save result class
	 */
	public static class VersionSaveResult {

		private final boolean saved;

		private final boolean duplicate;

		private final String message;

		private final int versionIndex;

		public VersionSaveResult(boolean saved, boolean duplicate, String message, int versionIndex) {
			this.saved = saved;
			this.duplicate = duplicate;
			this.message = message;
			this.versionIndex = versionIndex;
		}

		public boolean isSaved() {
			return saved;
		}

		public boolean isDuplicate() {
			return duplicate;
		}

		public String getMessage() {
			return message;
		}

		public int getVersionIndex() {
			return versionIndex;
		}

	}

	/**
	 * Save plan version to history record
	 * @param planTemplateId Plan template ID
	 * @param planJson Plan JSON data
	 * @return Save result information
	 */
	@Transactional
	public VersionSaveResult saveToVersionHistory(String planTemplateId, String planJson) {
		// Check if content is the same as the latest version
		if (isContentSameAsLatestVersion(planTemplateId, planJson)) {
			logger.info("Content of plan {} is the same as latest version, skipping version save", planTemplateId);
			Integer maxVersionIndex = versionRepository.findMaxVersionIndexByPlanTemplateId(planTemplateId);
			return new VersionSaveResult(false, true, "Content same as latest version, no new version created",
					maxVersionIndex != null ? maxVersionIndex : -1);
		}

		// Get maximum version number
		Integer maxVersionIndex = versionRepository.findMaxVersionIndexByPlanTemplateId(planTemplateId);
		int newVersionIndex = (maxVersionIndex == null) ? 0 : maxVersionIndex + 1;

		// Save new version
		PlanTemplateVersion version = new PlanTemplateVersion(planTemplateId, newVersionIndex, planJson);
		versionRepository.save(version);

		logger.info("Saved version {} of plan {}", newVersionIndex, planTemplateId);
		return new VersionSaveResult(true, false, "New version saved", newVersionIndex);
	}

	/**
	 * Save plan version to history record (compatibility method)
	 * @param planTemplateId Plan template ID
	 * @param planJson Plan JSON data
	 */
	@Transactional
	public void saveVersionToHistory(String planTemplateId, String planJson) {
		saveToVersionHistory(planTemplateId, planJson);
	}

	/**
	 * Get plan template
	 * @param planTemplateId Plan template ID
	 * @return Plan template entity, returns null if not exists
	 */
	public PlanTemplate getPlanTemplate(String planTemplateId) {
		return planTemplateRepository.findByPlanTemplateId(planTemplateId).orElse(null);
	}

	/**
	 * Get all version JSON data of the plan
	 * @param planTemplateId Plan template ID
	 * @return List of version JSON data
	 */
	public List<String> getPlanVersions(String planTemplateId) {
		List<PlanTemplateVersion> versions = versionRepository
			.findByPlanTemplateIdOrderByVersionIndexAsc(planTemplateId);
		List<String> jsonVersions = new ArrayList<>();
		for (PlanTemplateVersion version : versions) {
			jsonVersions.add(version.getPlanJson());
		}
		return jsonVersions;
	}

	/**
	 * Get specified version of the plan
	 * @param planTemplateId Plan template ID
	 * @param versionIndex Version index
	 * @return Version JSON data, returns null if version does not exist
	 */
	public String getPlanVersion(String planTemplateId, int versionIndex) {
		PlanTemplateVersion version = versionRepository.findByPlanTemplateIdAndVersionIndex(planTemplateId,
				versionIndex);
		return version != null ? version.getPlanJson() : null;
	}

	/**
	 * Get the latest version of the plan
	 * @param planTemplateId Plan template ID
	 * @return Latest version JSON data, returns null if no version exists
	 */
	public String getLatestPlanVersion(String planTemplateId) {
		Integer maxVersionIndex = versionRepository.findMaxVersionIndexByPlanTemplateId(planTemplateId);
		if (maxVersionIndex == null) {
			return null;
		}
		return getPlanVersion(planTemplateId, maxVersionIndex);
	}

	/**
	 * Check if given content is the same as the latest version
	 * @param planTemplateId Plan template ID
	 * @param planJson Plan JSON data to check
	 * @return Returns true if content is the same, false otherwise
	 */
	public boolean isContentSameAsLatestVersion(String planTemplateId, String planJson) {
		String latestVersion = getLatestPlanVersion(planTemplateId);
		return isJsonContentEquivalent(latestVersion, planJson);
	}

	/**
	 * Intelligently compare if two JSON strings are semantically identical, ignoring
	 * format differences (spaces, line breaks, etc.), only comparing actual content
	 * @param json1 First JSON string
	 * @param json2 Second JSON string
	 * @return Returns true if semantically identical, false otherwise
	 */
	public boolean isJsonContentEquivalent(String json1, String json2) {
		if (json1 == null && json2 == null) {
			return true;
		}
		if (json1 == null || json2 == null) {
			return false;
		}

		// First try simple string comparison
		if (json1.equals(json2)) {
			return true;
		}

		try {
			JsonNode node1 = objectMapper.readTree(json1);
			JsonNode node2 = objectMapper.readTree(json2);
			return node1.equals(node2);
		}
		catch (Exception e) {
			logger.warn("Failed to parse JSON content during comparison, falling back to string comparison", e);
			// If JSON parsing fails, fall back to string comparison
			return json1.equals(json2);
		}
	}

	/**
	 * Extract title from ExecutionPlan object
	 * @param planJson Plan JSON string
	 * @return Plan title, returns default title if extraction fails
	 */
	public String extractTitleFromPlan(String planJson) {
		try {
			JsonNode rootNode = objectMapper.readTree(planJson);
			if (rootNode.has("title")) {
				return rootNode.get("title").asText("Untitled Plan");
			}
		}
		catch (Exception e) {
			logger.warn("Failed to extract title from plan JSON", e);
		}
		return "Untitled Plan";
	}

	/**
	 * Get all plan templates
	 * @return List of all plan templates
	 */
	public List<PlanTemplate> getAllPlanTemplates() {
		return planTemplateRepository.findAll();
	}

	/**
	 * Delete plan template
	 * @param planTemplateId Plan template ID
	 * @return Whether deletion was successful
	 */
	@Transactional
	public boolean deletePlanTemplate(String planTemplateId) {
		try {
			// First delete all related versions
			versionRepository.deleteByPlanTemplateId(planTemplateId);

			// Then delete the template itself
			planTemplateRepository.deleteByPlanTemplateId(planTemplateId);

			logger.info("Deleted plan template {} and all its versions", planTemplateId);
			return true;
		}
		catch (Exception e) {
			logger.error("Failed to delete plan template {}", planTemplateId, e);
			return false;
		}
	}

	/**
	 * Execute a plan template by its ID.
	 * This method fetches the latest plan version and then executes it.
	 * @param planTemplateId The ID of the plan template to execute.
	 * @param parentPlanId The ID of the parent plan (can be null for root plans).
	 * @return A CompletableFuture that completes with the execution result.
	 */
	public CompletableFuture<PlanExecutionResult> executePlanByTemplateId(String planTemplateId, String rootPlanId,String parentPlanId, Map<String, Object> rawParam) {
		String planJson = getLatestPlanVersion(planTemplateId);
		if (planJson == null) {
			PlanExecutionResult errorResult = new PlanExecutionResult();
			errorResult.setSuccess(false);
			errorResult.setErrorMessage("No plan version found for template " + planTemplateId);
			return CompletableFuture.completedFuture(errorResult);
		}

		try {
			// Create ExecutionContext
			ExecutionContext context = new ExecutionContext();
			context.setCurrentPlanId(planTemplateId);
			context.setRootPlanId(parentPlanId != null ? parentPlanId : planTemplateId);
			context.setUserRequest("Execute plan template: " + planTemplateId);
			context.setNeedSummary(true);
			context.setUseMemory(false);

			// Parse plan JSON and set to context
			PlanInterface plan = objectMapper.readValue(planJson, PlanInterface.class);
			context.setPlan(plan);

			// Create task factory that creates PlanTask instances
			Function<String, PlanTask> taskFactory = planId -> {
				ExecutionContext taskContext = new ExecutionContext();
				taskContext.setCurrentPlanId(planId);
				taskContext.setRootPlanId(parentPlanId != null ? parentPlanId : planId);
				taskContext.setUserRequest("Execute plan template: " + planTemplateId);
				taskContext.setNeedSummary(true);
				taskContext.setUseMemory(false);
				taskContext.setPlan(plan);
				
				return new PlanTask(taskContext, parentPlanId, planExecutorFactory);
			};

			// Execute using TaskManager
			Collection<String> planIds = Collections.singleton(planTemplateId);
			return taskManager.scheduleChildren(planIds, taskFactory)
				.thenApply(resultsMap -> {
					PlanExecutionResult result = resultsMap.get(planTemplateId);
					return result != null ? result : new PlanExecutionResult();
				})
				.exceptionally(throwable -> {
					logger.error("Failed to execute plan template: {}", planTemplateId, throwable);
					PlanExecutionResult errorResult = new PlanExecutionResult();
					errorResult.setSuccess(false);
					errorResult.setErrorMessage("Execution failed: " + throwable.getMessage());
					return errorResult;
				});

		} catch (Exception e) {
			logger.error("Failed to parse plan JSON for template: {}", planTemplateId, e);
			PlanExecutionResult errorResult = new PlanExecutionResult();
			errorResult.setSuccess(false);
			errorResult.setErrorMessage("Failed to parse plan JSON: " + e.getMessage());
			return CompletableFuture.completedFuture(errorResult);
		}
	}


	// /**
	//  * Execute plan by plan template ID (internal method for controller).
	//  * This method is designed to work with the controller and returns ResponseEntity.
	//  * @param planTemplateId Plan template ID
	//  * @param rawParam Raw parameters (can be null)
	//  * @param parentPlanId Parent plan ID (can be null for root plans)
	//  * @return ResponseEntity containing execution status and result
	//  */
	// public ResponseEntity<Map<String, PlanExecutionResult>> executePlanByTemplateIdInternal(String planTemplateId, String rawParam, String parentPlanId) {
	// 	try {
	// 		// Execute the plan asynchronously
	// 		CompletableFuture<PlanExecutionResult> future = executePlanByTemplateId(planTemplateId, parentPlanId);
			
	// 		// For now, we'll return immediately with a "started" status
	// 		// In a real implementation, you might want to wait for completion or return a task ID
	// 		Map<String, Object> response = new HashMap<>();
	// 		response.put("status", "started");
	// 		response.put("planTemplateId", planTemplateId);
	// 		response.put("message", "Plan execution started successfully");
			
	// 		// Generate a plan ID for tracking
	// 		String planId = planTemplateId + "-" + System.currentTimeMillis();
	// 		response.put("planId", planId);
			
	// 		// Handle the execution result asynchronously
	// 		future.whenComplete((result, throwable) -> {
	// 			if (throwable != null) {
	// 				logger.error("Plan execution failed for template: {}", planTemplateId, throwable);
	// 			} else {
	// 				if (result.isSuccess()) {
	// 					logger.info("Plan execution completed successfully for template: {}", planTemplateId);
	// 				} else {
	// 					logger.warn("Plan execution failed for template: {} - {}", planTemplateId, result.getErrorMessage());
	// 				}
	// 			}
	// 		});
			
	// 		return ResponseEntity.ok(response);
			
	// 	} catch (Exception e) {
	// 		logger.error("Failed to start plan execution for template: {}", planTemplateId, e);
	// 		Map<String, Object> errorResponse = new HashMap<>();
	// 		errorResponse.put("error", "Failed to start plan execution: " + e.getMessage());
	// 		errorResponse.put("planTemplateId", planTemplateId);
	// 		return ResponseEntity.internalServerError().body(errorResponse);
	// 	}
	// }

}

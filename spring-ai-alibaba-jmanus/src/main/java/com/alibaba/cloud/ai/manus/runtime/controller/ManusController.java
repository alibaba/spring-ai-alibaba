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
package com.alibaba.cloud.ai.manus.runtime.controller;

import com.alibaba.cloud.ai.manus.event.JmanusListener;
import com.alibaba.cloud.ai.manus.event.PlanExceptionEvent;
import com.alibaba.cloud.ai.manus.exception.PlanException;
import com.alibaba.cloud.ai.manus.memory.entity.MemoryEntity;
import com.alibaba.cloud.ai.manus.memory.service.MemoryService;
import com.alibaba.cloud.ai.manus.planning.service.PlanTemplateService;
import com.alibaba.cloud.ai.manus.planning.service.IPlanParameterMappingService;
import com.alibaba.cloud.ai.manus.recorder.entity.vo.PlanExecutionRecord;
import com.alibaba.cloud.ai.manus.recorder.entity.vo.AgentExecutionRecord;
import com.alibaba.cloud.ai.manus.recorder.service.PlanHierarchyReaderService;
import com.alibaba.cloud.ai.manus.recorder.service.NewRepoPlanExecutionRecorder;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.PlanExecutionResult;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.PlanExecutionWrapper;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.PlanInterface;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.UserInputWaitState;
import com.alibaba.cloud.ai.manus.runtime.service.PlanIdDispatcher;
import com.alibaba.cloud.ai.manus.runtime.service.PlanningCoordinator;
import com.alibaba.cloud.ai.manus.runtime.service.UserInputService;
import com.alibaba.cloud.ai.manus.coordinator.repository.CoordinatorToolRepository;
import com.alibaba.cloud.ai.manus.coordinator.entity.po.CoordinatorToolEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/executor")
public class ManusController implements JmanusListener<PlanExceptionEvent> {

	private static final Logger logger = LoggerFactory.getLogger(ManusController.class);

	private final ObjectMapper objectMapper;

	private final Cache<String, Throwable> exceptionCache;

	@Autowired
	@Lazy
	private PlanningCoordinator planningCoordinator;

	@Autowired
	private PlanHierarchyReaderService planHierarchyReaderService;

	@Autowired
	private PlanIdDispatcher planIdDispatcher;

	@Autowired
	private UserInputService userInputService;

	@Autowired
	private MemoryService memoryService;

	@Autowired
	private NewRepoPlanExecutionRecorder planExecutionRecorder;

	@Autowired
	private CoordinatorToolRepository coordinatorToolRepository;

	@Autowired
	private PlanTemplateService planTemplateService;

	@Autowired
	private IPlanParameterMappingService parameterMappingService;

	@Autowired
	public ManusController(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		// Register JavaTimeModule to handle LocalDateTime serialization/deserialization
		this.objectMapper.registerModule(new JavaTimeModule());
		// Ensure pretty printing is disabled by default for compact JSON
		// this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
		// 10minutes timeout for plan exception
		this.exceptionCache = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build();
	}

	private boolean isVue(Map<String, Object> request) {

		// Check if request is from Vue frontend
		Boolean isVueRequest = (Boolean) request.get("isVueRequest");
		if (isVueRequest == null) {
			isVueRequest = false;
		}

		return isVueRequest == null ? false : isVueRequest;

	}

	/**
	 * Asynchronous execution of Manus request using PlanningCoordinator
	 * @param request Request containing user query
	 * @return Task ID and status
	 */
	@PostMapping("/execute")
	public ResponseEntity<Map<String, Object>> executeQuery(@RequestBody Map<String, Object> request) {
		String query = (String) request.get("input");
		if (query == null || query.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Query content cannot be empty"));
		}
		boolean isVueRequest = isVue(request);

		// Log request source
		if (isVueRequest) {
			logger.info("🌐 [VUE] Received query request from Vue frontend: ");
		}
		else {
			logger.info("🔗 [HTTP] Received query request from HTTP client: ");
		}

		String planId = null;
		try {
			// Use sessionPlanId from frontend if available, otherwise generate new one
			String sessionPlanId = (String) request.get("sessionPlanId");

			if (sessionPlanId != null && !sessionPlanId.trim().isEmpty()) {
				// Use existing sessionPlanId from file upload
				planId = sessionPlanId;
				logger.info("🔄 Using existing sessionPlanId: {}", planId);
			}
			else {
				// Generate new plan ID
				planId = planIdDispatcher.generatePlanId();
				logger.info("🆕 Generated new planId: {}", planId);
			}

			String memoryId = (String) request.get("memoryId");

			if (!StringUtils.hasText(memoryId)) {
				memoryId = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
			}
			memoryService.saveMemory(new MemoryEntity(memoryId, query));

			// Execute the plan using PlanningCoordinator (fire and forget)
			planningCoordinator.executeByUserQuery(query, planId, null, planId, memoryId, null);

			// Return task ID and initial status
			Map<String, Object> response = new HashMap<>();
			response.put("planId", planId);
			response.put("status", "processing");
			response.put("message", "Task submitted, processing");

			response.put("memoryId", memoryId);
			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			logger.error("Failed to start plan execution for planId: {}", planId, e);
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("error", "Failed to start plan execution: " + e.getMessage());
			errorResponse.put("planId", planId);
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	/**
	 * Execute plan by tool name synchronously (GET method)
	 * @param toolName Tool name
	 * @return Execution result directly
	 */
	@GetMapping("/executeByToolNameSync/{toolName}")
	public ResponseEntity<Map<String, Object>> executeByToolNameGetSync(@PathVariable("toolName") String toolName,
			@RequestParam(required = false, name = "allParams") Map<String, String> allParams) {
		if (toolName == null || toolName.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Tool name cannot be empty"));
		}

		// Get plan template ID from coordinator tool
		String planTemplateId = getPlanTemplateIdFromTool(toolName);
		if (planTemplateId == null) {
			return ResponseEntity.badRequest().body(Map.of("error", "Tool not found with name: " + toolName));
		}
		if (planTemplateId.trim().isEmpty()) {
			return ResponseEntity.badRequest()
				.body(Map.of("error", "No plan template ID associated with tool: " + toolName));
		}

		logger.info("Execute tool '{}' synchronously with plan template ID '{}', parameters: {}", toolName,
				planTemplateId, allParams);
		// Execute synchronously and return result directly
		return executePlanSyncAndBuildResponse(planTemplateId, null, null, false);
	}

	/**
	 * Execute plan by tool name asynchronously If tool is not published, treat toolName
	 * as planTemplateId
	 * @param request Request containing tool name and parameters
	 * @return Task ID and status
	 */
	@PostMapping("/executeByToolNameAsync")
	public ResponseEntity<Map<String, Object>> executeByToolNameAsync(@RequestBody Map<String, Object> request) {
		String toolName = (String) request.get("toolName");
		if (toolName == null || toolName.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Tool name cannot be empty"));
		}
		boolean isVueRequest = isVue(request);

		// Log request source
		if (isVueRequest) {
			logger.info("🌐 [VUE] Received query request from Vue frontend: ");
		}
		else {
			logger.info("🔗 [HTTP] Received query request from HTTP client: ");
		}

		String planTemplateId = null;

		// First, try to find the coordinator tool by tool name
		planTemplateId = getPlanTemplateIdFromTool(toolName);
		if (planTemplateId != null) {
			// Tool is published, get plan template ID from coordinator tool
			logger.info("Found published tool: {} with plan template ID: {}", toolName, planTemplateId);
		}
		else {
			// Tool is not published, treat toolName as planTemplateId
			planTemplateId = toolName;
			logger.info("Tool not published, using toolName as planTemplateId: {}", planTemplateId);
		}

		if (planTemplateId == null || planTemplateId.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "No plan template ID found for tool: " + toolName));
		}

		try {
			String memoryId = (String) request.get("memoryId");

			// Handle uploaded files if present
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> uploadedFiles = (List<Map<String, Object>>) request.get("uploadedFiles");

			// Get replacement parameters for <<>> replacement
			@SuppressWarnings("unchecked")
			Map<String, Object> replacementParams = (Map<String, Object>) request.get("replacementParams");

			// Execute the plan template using the new unified method
			PlanExecutionWrapper wrapper = executePlanTemplate(planTemplateId, uploadedFiles, memoryId,
					replacementParams, isVueRequest);

			// Start the async execution (fire and forget)
			wrapper.getResult().whenComplete((result, throwable) -> {
				if (throwable != null) {
					logger.error("Async plan execution failed for planId: {}", wrapper.getRootPlanId(), throwable);
				}
				else {
					logger.info("Async plan execution completed for planId: {}", wrapper.getRootPlanId());
				}
			});

			// Generate memory ID if not provided
			if (!StringUtils.hasText(memoryId)) {
				memoryId = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
			}

			String query = "Execute plan template: " + planTemplateId;
			memoryService.saveMemory(new MemoryEntity(memoryId, query));

			// Return task ID and initial status
			Map<String, Object> response = new HashMap<>();
			response.put("planId", wrapper.getRootPlanId());
			response.put("status", "processing");
			response.put("message", "Task submitted, processing");
			response.put("memoryId", memoryId);
			response.put("toolName", toolName);
			response.put("planTemplateId", planTemplateId);

			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			logger.error("Failed to start plan execution for tool: {} with planTemplateId: {}", toolName,
					planTemplateId, e);
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("error", "Failed to start plan execution: " + e.getMessage());
			errorResponse.put("toolName", toolName);
			errorResponse.put("planTemplateId", planTemplateId);
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	/**
	 * Execute plan by tool name synchronously (POST method)
	 * @param request Request containing tool name
	 * @return Execution result directly
	 */
	@PostMapping("/executeByToolNameSync")
	public ResponseEntity<Map<String, Object>> executeByToolNameSync(@RequestBody Map<String, Object> request) {
		String toolName = (String) request.get("toolName");
		if (toolName == null || toolName.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Tool name cannot be empty"));
		}

		boolean isVueRequest = isVue(request);

		// Log request source
		if (isVueRequest) {
			logger.info("🌐 [VUE] Received query request from Vue frontend: ");
		}
		else {
			logger.info("🔗 [HTTP] Received query request from HTTP client: ");
		}

		// Get plan template ID from coordinator tool
		String planTemplateId = getPlanTemplateIdFromTool(toolName);
		if (planTemplateId == null) {
			return ResponseEntity.badRequest().body(Map.of("error", "Tool not found with name: " + toolName));
		}
		if (planTemplateId.trim().isEmpty()) {
			return ResponseEntity.badRequest()
				.body(Map.of("error", "No plan template ID associated with tool: " + toolName));
		}

		// Handle uploaded files if present
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> uploadedFiles = (List<Map<String, Object>>) request.get("uploadedFiles");

		// Get replacement parameters for <<>> replacement
		@SuppressWarnings("unchecked")
		Map<String, Object> replacementParams = (Map<String, Object>) request.get("replacementParams");

		logger.info(
				"Executing tool '{}' synchronously with plan template ID '{}', uploadedFiles: {}, replacementParams: {}",
				toolName, planTemplateId, uploadedFiles != null ? uploadedFiles.size() : "null",
				replacementParams != null ? replacementParams.size() : "null");

		return executePlanSyncAndBuildResponse(planTemplateId, uploadedFiles, replacementParams, isVueRequest);
	}

	/**
	 * Get execution record overview (without detailed ThinkActRecord information) Note:
	 * This method returns basic execution information and does not include detailed
	 * ThinkActRecord steps for each agent execution.
	 * @param planId Plan ID
	 * @return JSON representation of execution record overview
	 */
	@GetMapping("/details/{planId}")
	public synchronized ResponseEntity<?> getExecutionDetails(@PathVariable("planId") String planId) {
		Throwable throwable = this.exceptionCache.getIfPresent(planId);
		if (throwable != null) {
			throw new PlanException(throwable);
		}
		PlanExecutionRecord planRecord = planHierarchyReaderService.readPlanTreeByRootId(planId);

		if (planRecord == null) {
			return ResponseEntity.notFound().build();
		}

		// Check for user input wait state and merge it into the plan record
		UserInputWaitState waitState = userInputService.getWaitState(planId);
		if (waitState != null && waitState.isWaiting()) {
			// Assuming PlanExecutionRecord has a method like setUserInputWaitState
			// You will need to add this field and method to your PlanExecutionRecord
			// class
			planRecord.setUserInputWaitState(waitState);
			logger.info("Plan {} is waiting for user input. Merged waitState into details response.", planId);
		}
		else {
			planRecord.setUserInputWaitState(null); // Clear if not waiting
		}

		// Set rootPlanId if it's null, using currentPlanId as default
		if (planRecord.getRootPlanId() == null) {
			planRecord.setRootPlanId(planRecord.getCurrentPlanId());
			logger.info("Set rootPlanId to currentPlanId for plan: {}", planId);
		}

		try {
			// Use Jackson ObjectMapper to convert object to JSON string
			String jsonResponse = objectMapper.writeValueAsString(planRecord);
			return ResponseEntity.ok(jsonResponse);
		}
		catch (JsonProcessingException e) {
			logger.error("Error serializing PlanExecutionRecord to JSON for planId: {}", planId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("Error processing request: " + e.getMessage());
		}
	}

	/**
	 * Delete execution record for specified plan ID
	 * @param planId Plan ID
	 * @return Result of delete operation
	 */
	@DeleteMapping("/details/{planId}")
	public ResponseEntity<Map<String, String>> removeExecutionDetails(@PathVariable("planId") String planId) {
		PlanExecutionRecord planRecord = planHierarchyReaderService.readPlanTreeByRootId(planId);
		if (planRecord == null) {
			return ResponseEntity.notFound().build();
		}

		// Note: We don't need to remove execution records since they are already stored
		// in the database
		// The database serves as the persistent storage for all execution records
		return ResponseEntity.ok(Map.of("message", "Execution record found (no deletion needed)", "planId", planId));
	}

	/**
	 * Submits user input for a plan that is waiting.
	 * @param planId The ID of the plan.
	 * @param formData The user-submitted form data, expected as Map<String, String>.
	 * @return ResponseEntity indicating success or failure.
	 */
	@PostMapping("/submit-input/{planId}")
	public ResponseEntity<Map<String, Object>> submitUserInput(@PathVariable("planId") String planId,
			@RequestBody Map<String, String> formData) { // Changed formData to
		// Map<String, String>
		try {
			logger.info("Received user input for plan {}: {}", planId, formData);
			boolean success = userInputService.submitUserInputs(planId, formData);
			if (success) {
				return ResponseEntity.ok(Map.of("message", "Input submitted successfully", "planId", planId));
			}
			else {
				// This case might mean the plan was no longer waiting, or input was
				// invalid.
				// UserInputService should ideally throw specific exceptions for clearer
				// error handling.
				logger.warn("Failed to submit user input for plan {}, it might not be waiting or input was invalid.",
						planId);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("error", "Failed to submit input. Plan not waiting or input invalid.", "planId",
							planId));
			}
		}
		catch (IllegalArgumentException e) {
			logger.error("Error submitting user input for plan {}: {}", planId, e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of("error", e.getMessage(), "planId", planId));
		}
		catch (Exception e) {
			logger.error("Unexpected error submitting user input for plan {}: {}", planId, e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("error", "An unexpected error occurred.", "planId", planId));
		}
	}

	/**
	 * Execute plan synchronously and build response with parameter replacement support
	 * @param planTemplateId The plan template ID to execute
	 * @param uploadedFiles List of uploaded files (can be null)
	 * @param replacementParams Parameters for <<>> replacement (can be null)
	 * @param isVueRequest Flag indicating whether this is a Vue frontend request
	 * @return ResponseEntity with execution result
	 */
	private ResponseEntity<Map<String, Object>> executePlanSyncAndBuildResponse(String planTemplateId,
			List<Map<String, Object>> uploadedFiles, Map<String, Object> replacementParams, boolean isVueRequest) {
		try {
			// Execute the plan template using the new unified method
			PlanExecutionWrapper wrapper = executePlanTemplate(planTemplateId, uploadedFiles, null, replacementParams,
					isVueRequest);
			PlanExecutionResult planExecutionResult = wrapper.getResult().get();

			// Return success with execution result
			Map<String, Object> response = new HashMap<>();
			response.put("status", "completed");
			response.put("result", planExecutionResult.getFinalResult());

			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			logger.error("Failed to execute plan template synchronously: {}", planTemplateId, e);
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("error", "Execution failed: " + e.getMessage());
			errorResponse.put("status", "failed");
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	/**
	 * Execute a plan template by its ID with parameter replacement support
	 * @param planTemplateId The ID of the plan template to execute
	 * @param uploadedFiles List of uploaded files (can be null)
	 * @param memoryId Memory ID for the execution (can be null)
	 * @param replacementParams Parameters for <<>> replacement (can be null)
	 * @param isVueRequest Flag indicating whether this is a Vue frontend request
	 * @return PlanExecutionWrapper containing both PlanExecutionResult and rootPlanId
	 */
	private PlanExecutionWrapper executePlanTemplate(String planTemplateId, List<Map<String, Object>> uploadedFiles,
			String memoryId, Map<String, Object> replacementParams, boolean isVueRequest) {
		if (planTemplateId == null || planTemplateId.trim().isEmpty()) {
			logger.error("Plan template ID is null or empty");
			throw new IllegalArgumentException("Plan template ID cannot be null or empty");
		}

		try {
			// Generate a unique plan ID for this execution
			String currentPlanId = planIdDispatcher.generatePlanId();
			String rootPlanId = currentPlanId;

			// Generate memory ID if not provided
			if (!StringUtils.hasText(memoryId)) {
				memoryId = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
			}

			// Get the latest plan version JSON string
			String planJson = planTemplateService.getLatestPlanVersion(planTemplateId);
			if (planJson == null) {
				throw new RuntimeException("Plan template not found: " + planTemplateId);
			}

			// Prepare parameters for replacement
			Map<String, Object> parametersForReplacement = new HashMap<>();
			if (replacementParams != null) {
				parametersForReplacement.putAll(replacementParams);
			}
			// Add the generated planId to parameters
			parametersForReplacement.put("planId", rootPlanId);

			// Replace parameter placeholders (<< >>) with actual input parameters
			if (!parametersForReplacement.isEmpty()) {
				try {
					logger.info("Replacing parameter placeholders in plan template with input parameters: {}",
							parametersForReplacement.keySet());
					planJson = parameterMappingService.replaceParametersInJson(planJson, parametersForReplacement);
					logger.debug("Parameter replacement completed successfully");
				}
				catch (Exception e) {
					String errorMsg = "Failed to replace parameters in plan template: " + e.getMessage();
					logger.error(errorMsg, e);
					CompletableFuture<PlanExecutionResult> failedFuture = new CompletableFuture<>();
					failedFuture.completeExceptionally(new RuntimeException(errorMsg, e));
					return new PlanExecutionWrapper(failedFuture, null);
				}
			}
			else {
				logger.debug("No parameter replacement needed - replacementParams: {}",
						replacementParams != null ? replacementParams.size() : 0);
			}

			// Parse the plan JSON to create PlanInterface
			PlanInterface plan = objectMapper.readValue(planJson, PlanInterface.class);

			// Handle uploaded files if present
			if (uploadedFiles != null && !uploadedFiles.isEmpty()) {
				logger.info("Uploaded files will be handled by the execution context for plan template: {}",
						uploadedFiles.size());
			}

			// Execute using the PlanningCoordinator
			CompletableFuture<PlanExecutionResult> future = planningCoordinator.executeByPlan(plan, rootPlanId, null,
					currentPlanId, null, isVueRequest);

			// Return the wrapper containing both the future and rootPlanId
			return new PlanExecutionWrapper(future, rootPlanId);

		}
		catch (Exception e) {
			logger.error("Failed to execute plan template: {}", planTemplateId, e);
			CompletableFuture<PlanExecutionResult> failedFuture = new CompletableFuture<>();
			failedFuture.completeExceptionally(new RuntimeException("Plan execution failed: " + e.getMessage(), e));
			return new PlanExecutionWrapper(failedFuture, null);
		}
	}

	/**
	 * Get detailed agent execution record by stepId (includes ThinkActRecord details)
	 * @param stepId The step ID to query
	 * @return Detailed agent execution record with ThinkActRecord details
	 */
	@GetMapping("/agent-execution/{stepId}")
	public ResponseEntity<AgentExecutionRecord> getAgentExecutionDetail(@PathVariable("stepId") String stepId) {
		try {
			logger.info("Fetching agent execution detail for stepId: {}", stepId);

			AgentExecutionRecord detail = planExecutionRecorder.getAgentExecutionDetail(stepId);
			if (detail == null) {
				logger.warn("Agent execution detail not found for stepId: {}", stepId);
				return ResponseEntity.notFound().build();
			}

			logger.info("Successfully retrieved agent execution detail for stepId: {}", stepId);
			return ResponseEntity.ok(detail);
		}
		catch (Exception e) {
			logger.error("Error fetching agent execution detail for stepId: {}", stepId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Get plan template ID from coordinator tool by tool name
	 * @param toolName The tool name to look up
	 * @return Plan template ID if found, null if tool not found
	 */
	private String getPlanTemplateIdFromTool(String toolName) {
		CoordinatorToolEntity coordinatorTool = coordinatorToolRepository.findByToolName(toolName);
		if (coordinatorTool == null) {
			return null;
		}
		Boolean isHttpEnabled = coordinatorTool.getEnableHttpService();
		if (!isHttpEnabled) {
			return null;
		}
		return coordinatorTool.getPlanTemplateId();
	}

	@Override
	public void onEvent(PlanExceptionEvent event) {
		this.exceptionCache.put(event.getPlanId(), event.getThrowable());
	}

}

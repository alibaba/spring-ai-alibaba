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
import com.alibaba.cloud.ai.manus.recorder.entity.vo.PlanExecutionRecord;
import com.alibaba.cloud.ai.manus.recorder.entity.vo.AgentExecutionRecord;
import com.alibaba.cloud.ai.manus.recorder.service.PlanHierarchyReaderService;
import com.alibaba.cloud.ai.manus.recorder.service.NewRepoPlanExecutionRecorder;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.UserInputWaitState;
import com.alibaba.cloud.ai.manus.runtime.service.PlanIdDispatcher;
import com.alibaba.cloud.ai.manus.runtime.service.PlanningCoordinator;
import com.alibaba.cloud.ai.manus.runtime.service.UserInputService;
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
import java.util.Map;
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
	public ManusController(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		// Register JavaTimeModule to handle LocalDateTime serialization/deserialization
		this.objectMapper.registerModule(new JavaTimeModule());
		// Ensure pretty printing is disabled by default for compact JSON
		// this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
		// 10minutes timeout for plan exception
		this.exceptionCache = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build();
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
	 * Get detailed execution record
	 * @param planId Plan ID
	 * @return JSON representation of execution record
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
	 * Get detailed agent execution record by stepId
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

	@Override
	public void onEvent(PlanExceptionEvent event) {
		this.exceptionCache.put(event.getPlanId(), event.getThrowable());
	}

}

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

import com.alibaba.cloud.ai.example.manus.dynamic.memory.entity.MemoryEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.memory.service.MemoryService;
import com.alibaba.cloud.ai.example.manus.event.JmanusListener;
import com.alibaba.cloud.ai.example.manus.event.PlanExceptionEvent;
import com.alibaba.cloud.ai.example.manus.exception.PlanException;
import com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanIdDispatcher;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.UserInputWaitState;
import com.alibaba.cloud.ai.example.manus.planning.service.UserInputService;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.recorder.entity.PlanExecutionRecord;
import com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanningCoordinator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.lang3.RandomStringUtils;
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
import java.util.ArrayList;
import java.util.List;
import com.alibaba.cloud.ai.example.manus.recorder.entity.AgentExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.ExecutionStatus;

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
	private PlanExecutionRecorder planExecutionRecorder;

	@Autowired
	private PlanIdDispatcher planIdDispatcher;

	@Autowired
	private UserInputService userInputService;

	@Autowired
	private MemoryService memoryService;

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
				memoryId = RandomStringUtils.randomAlphabetic(8);
			}
			memoryService.saveMemory(new MemoryEntity(memoryId, query));

			// Execute the plan using PlanningCoordinator (fire and forget)
			planningCoordinator.executeByUserQuery(query, planId, planId, planId, memoryId);

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
		PlanExecutionRecord planRecord = planExecutionRecorder.getRootPlanExecutionRecord(planId);

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
		PlanExecutionRecord planRecord = planExecutionRecorder.getRootPlanExecutionRecord(planId);
		if (planRecord == null) {
			return ResponseEntity.notFound().build();
		}

		try {
			planExecutionRecorder.removeExecutionRecord(planId);
			return ResponseEntity.ok(Map.of("message", "Execution record successfully deleted", "planId", planId));
		}
		catch (Exception e) {
			return ResponseEntity.internalServerError()
				.body(Map.of("error", "Failed to delete record: " + e.getMessage()));
		}
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

	@Override
	public void onEvent(PlanExceptionEvent event) {
		this.exceptionCache.put(event.getPlanId(), event.getThrowable());
	}

	/**
	 * Get execution tree with steps (without think-act rounds)
	 * @param rootPlanId Root plan ID
	 * @return JSON representation of execution tree
	 */
	@GetMapping("/tree/{rootPlanId}")
	public synchronized ResponseEntity<?> getExecutionTree(@PathVariable("rootPlanId") String rootPlanId) {
		try {
			PlanExecutionRecord rootRecord = planExecutionRecorder.getRootPlanExecutionRecord(rootPlanId);

			if (rootRecord == null) {
				return ResponseEntity.notFound().build();
			}

			// Build tree response
			Map<String, Object> treeResponse = buildTreeResponse(rootRecord);

			// Use Jackson ObjectMapper to convert object to JSON string
			String jsonResponse = objectMapper.writeValueAsString(treeResponse);
			return ResponseEntity.ok(jsonResponse);

		}
		catch (JsonProcessingException e) {
			logger.error("Error serializing tree response to JSON for rootPlanId: {}", rootPlanId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("Error processing request: " + e.getMessage());
		}
		catch (Exception e) {
			logger.error("Error building tree response for rootPlanId: {}", rootPlanId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("Error building tree response: " + e.getMessage());
		}
	}

	/**
	 * Build tree response structure from PlanExecutionRecord
	 * @param rootRecord Root plan execution record
	 * @return Tree response map
	 */
	private Map<String, Object> buildTreeResponse(PlanExecutionRecord rootRecord) {
		Map<String, Object> response = new HashMap<>();
		response.put("rootPlanId", rootRecord.getCurrentPlanId());
		response.put("tree", buildTreeNode(rootRecord));
		return response;
	}

	/**
	 * Build tree node from PlanExecutionRecord
	 * @param record Plan execution record
	 * @return Tree node map
	 */
	private Map<String, Object> buildTreeNode(PlanExecutionRecord record) {
		Map<String, Object> node = new HashMap<>();

		// Basic plan info
		node.put("currentPlanId", record.getCurrentPlanId());
		node.put("title", record.getTitle());
		node.put("status", deriveStatus(record));
		node.put("progress", calculateProgress(record));
		node.put("startTime", record.getStartTime());
		node.put("endTime", record.getEndTime());
		node.put("userRequest", record.getUserRequest());

		// Transform steps
		node.put("steps", buildSteps(record.getAgentExecutionSequence()));

		// Children (sub-plans) - for now empty, can be extended later
		node.put("children", new ArrayList<>());

		return node;
	}

	/**
	 * Build steps array from agent execution sequence
	 * @param agentExecutions Agent execution records
	 * @return List of step info maps
	 */
	private List<Map<String, Object>> buildSteps(List<AgentExecutionRecord> agentExecutions) {
		if (agentExecutions == null || agentExecutions.isEmpty()) {
			return new ArrayList<>();
		}

		List<Map<String, Object>> steps = new ArrayList<>();
		for (int i = 0; i < agentExecutions.size(); i++) {
			AgentExecutionRecord agentExecution = agentExecutions.get(i);
			Map<String, Object> stepInfo = buildStepInfo(i, agentExecution);
			steps.add(stepInfo);
		}
		return steps;
	}

	/**
	 * Build step info from agent execution record
	 * @param stepIndex Step index
	 * @param agentExecution Agent execution record
	 * @return Step info map
	 */
	private Map<String, Object> buildStepInfo(int stepIndex, AgentExecutionRecord agentExecution) {
		Map<String, Object> stepInfo = new HashMap<>();

		// Step metadata
		stepInfo.put("stepIndex", stepIndex);
		stepInfo.put("stepDescription", generateStepDescription(agentExecution, stepIndex));

		// Agent execution info (without thinkActRounds)
		Map<String, Object> agentInfo = new HashMap<>();
		agentInfo.put("id", agentExecution.getId());
		agentInfo.put("agentName", agentExecution.getAgentName());
		agentInfo.put("agentDescription", agentExecution.getAgentDescription());
		agentInfo.put("status", agentExecution.getStatus());
		agentInfo.put("startTime", agentExecution.getStartTime());
		agentInfo.put("endTime", agentExecution.getEndTime());
		agentInfo.put("currentStep", agentExecution.getCurrentStep());
		agentInfo.put("maxSteps", agentExecution.getMaxSteps());

		stepInfo.put("agentExecution", agentInfo);
		return stepInfo;
	}

	/**
	 * Derive status from plan execution record
	 * @param record Plan execution record
	 * @return Status string
	 */
	private String deriveStatus(PlanExecutionRecord record) {
		if (record.isCompleted()) {
			return "completed";
		}
		else if (record.getStartTime() != null) {
			return "running";
		}
		else {
			return "pending";
		}
	}

	/**
	 * Calculate progress percentage
	 * @param record Plan execution record
	 * @return Progress percentage (0-100)
	 */
	private int calculateProgress(PlanExecutionRecord record) {
		if (record.isCompleted()) {
			return 100;
		}

		List<AgentExecutionRecord> agentExecutions = record.getAgentExecutionSequence();
		if (agentExecutions == null || agentExecutions.isEmpty()) {
			return 0;
		}

		int totalSteps = agentExecutions.size();
		int completedSteps = 0;

		for (AgentExecutionRecord agentExecution : agentExecutions) {
			if (agentExecution.getStatus() == ExecutionStatus.FINISHED) {
				completedSteps++;
			}
		}

		return totalSteps > 0 ? (completedSteps * 100) / totalSteps : 0;
	}

	/**
	 * Generate step description
	 * @param agentExecution Agent execution record
	 * @param stepIndex Step index
	 * @return Step description
	 */
	private String generateStepDescription(AgentExecutionRecord agentExecution, int stepIndex) {
		String agentName = agentExecution.getAgentName();
		if (agentName != null && !agentName.trim().isEmpty()) {
			return String.format("Step %d: Execute %s", stepIndex + 1, agentName);
		}
		else {
			return String.format("Step %d: Execute agent", stepIndex + 1);
		}
	}

}

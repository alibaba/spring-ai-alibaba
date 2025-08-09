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

package com.alibaba.cloud.ai.example.manus.coordinator.service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.alibaba.cloud.ai.example.manus.coordinator.tool.CoordinatorTool;
import com.alibaba.cloud.ai.example.manus.planning.service.PlanTemplateService;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.recorder.entity.PlanExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.AgentExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.ThinkActRecord;
import com.alibaba.cloud.ai.example.manus.config.CoordinatorProperties;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;

/**
 * Coordinator Tool Execution Engine
 *
 * Responsible for actual tool execution, plan execution and polling
 */
@Service
public class CoordinatorToolExecutor {

	private static final Logger log = LoggerFactory.getLogger(CoordinatorToolExecutor.class);

	// Constant definitions
	private static final String LOG_PREFIX = "[CoordinatorToolExecutor]";

	private static final String PLAN_ID_KEY = "planId";

	private static final String DEFAULT_RESULT_MESSAGE = "Plan execution completed, but no specific result obtained";

	private static final String PLAN_NOT_FOUND_ERROR = "Plan not found: %s";

	private static final String POLLING_TIMEOUT_ERROR = "Polling timeout after %d attempts";

	private static final String POLLING_INTERRUPTED_ERROR = "Polling interrupted: %s";

	@Autowired
	private PlanTemplateService planTemplateService;

	@Autowired
	private PlanExecutionRecorder planExecutionRecorder;

	@Autowired
	private CoordinatorProperties coordinatorProperties;

	private final ObjectMapper objectMapper;

	public CoordinatorToolExecutor() {
		this.objectMapper = new ObjectMapper();
		// Register JSR310 module to support LocalDateTime and other Java 8 time types
		this.objectMapper.registerModule(new JavaTimeModule());
	}

	/**
	 * Create tool specification
	 * @param tool Coordinator tool
	 * @return Tool specification
	 */
	public McpServerFeatures.SyncToolSpecification createSpec(CoordinatorTool tool) {
		return McpServerFeatures.SyncToolSpecification.builder()
			.tool(io.modelcontextprotocol.spec.McpSchema.Tool.builder()
				.name(tool.getToolName())
				.description(tool.getToolDescription())
				.inputSchema(tool.getToolSchema())
				.build())
			.callHandler((exchange, request) -> execute(request))
			.build();
	}

	/**
	 * Execute tool call
	 * @param request Tool call request
	 * @return Tool call result
	 */
	public CallToolResult execute(CallToolRequest request) {
		String toolName = request.name();
		log.debug("{} Starting tool call execution: {}", LOG_PREFIX, toolName);

		try {
			// 1. Validate and extract parameters
			Map<String, Object> arguments = request.arguments();
			String planId = validateAndExtractPlanId(arguments);
			String rawParam = serializeArguments(arguments);

			// 2. Execute plan template
			executePlanTemplate(toolName, rawParam, planId);

			// 3. Poll for results
			String resultString = pollPlanResult(planId);

			// 4. Return success result
			log.info("{} Tool call execution completed: {}", LOG_PREFIX, toolName);
			return new CallToolResult(List.of(new McpSchema.TextContent(resultString)), false);

		}
		catch (Exception e) {
			log.error("{} Tool call failed: {} - {}", LOG_PREFIX, toolName, e.getMessage(), e);
			return new CallToolResult(
					List.of(new McpSchema.TextContent(
							String.format("Tool call failed: %s - %s", e.getClass().getSimpleName(), e.getMessage()))),
					true);
		}
	}

	/**
	 * Validate and extract planId
	 * @param arguments Request parameters
	 * @return planId
	 */
	private String validateAndExtractPlanId(Map<String, Object> arguments) {
		if (arguments == null) {
			throw new IllegalArgumentException("Request parameters cannot be empty");
		}

		Object planIdObject = arguments.get(PLAN_ID_KEY);
		if (planIdObject == null) {
			throw new IllegalArgumentException("Missing required planId parameter");
		}

		String planId = planIdObject.toString().trim();
		if (planId.isEmpty()) {
			throw new IllegalArgumentException("planId cannot be empty");
		}

		log.debug("{} Successfully extracted planId: {}", LOG_PREFIX, planId);
		return planId;
	}

	/**
	 * Serialize request parameters (after removing planId)
	 * @param arguments Request parameters
	 * @return Serialized parameter string
	 */
	private String serializeArguments(Map<String, Object> arguments) {
		if (arguments == null || arguments.isEmpty()) {
			return "{}";
		}

		// Create parameter copy to avoid modifying original parameters
		Map<String, Object> paramsCopy = new HashMap<>(arguments);
		paramsCopy.remove(PLAN_ID_KEY);

		try {
			String serializedParams = objectMapper.writeValueAsString(paramsCopy);
			log.debug("{} Parameter serialization completed: {}", LOG_PREFIX, serializedParams);
			return serializedParams;
		}
		catch (Exception e) {
			throw new RuntimeException("Parameter serialization failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Execute plan template
	 * @param toolName Tool name
	 * @param rawParam Raw parameters
	 * @param planId Plan ID
	 */
	private void executePlanTemplate(String toolName, String rawParam, String planId) {
		log.info("{} Executing plan template: {}, parameters: {}, planId: {}", LOG_PREFIX, toolName, rawParam, planId);

		ResponseEntity<Map<String, Object>> responseEntity = planTemplateService
			.executePlanByTemplateIdInternal(toolName, rawParam, planId);

		if (!responseEntity.getStatusCode().is2xxSuccessful()) {
			String errorMsg = String.format("Plan template service call failed, status code: %s",
					responseEntity.getStatusCode());
			log.error("{} {}", LOG_PREFIX, errorMsg);
			throw new RuntimeException(errorMsg);
		}

		if (responseEntity.getBody() == null) {
			String errorMsg = "Plan template service returned empty response";
			log.error("{} {}", LOG_PREFIX, errorMsg);
			throw new RuntimeException(errorMsg);
		}

		log.info("{} Plan template execution successful: {}", LOG_PREFIX, toolName);
	}

	/**
	 * Poll plan execution results
	 * @param planId Plan ID
	 * @return Execution result string
	 */
	public String pollPlanResult(String planId) {
		log.info("{} Starting to poll plan execution results: {}", LOG_PREFIX, planId);

		try {
			// Poll until plan completion
			PlanExecutionRecord record = pollUntilComplete(planId);

			// Extract final result
			String resultOutput = extractFinalResult(record);

			if (resultOutput != null && !resultOutput.trim().isEmpty()) {
				log.info("{} Successfully obtained plan execution result: {}", LOG_PREFIX, planId);
				return resultOutput;
			}
			else {
				log.warn("{} Unable to obtain plan execution result, returning default message: {}", LOG_PREFIX,
						planId);
				return DEFAULT_RESULT_MESSAGE;
			}
		}
		catch (Exception e) {
			log.error("{} Polling plan results failed: {} - {}", LOG_PREFIX, planId, e.getMessage(), e);
			throw new RuntimeException("Polling plan results failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Extract final execution result
	 * @param record Plan execution record
	 * @return Final result string
	 */
	public String extractFinalResult(PlanExecutionRecord record) {
		if (record == null) {
			throw new IllegalArgumentException("Plan execution record cannot be empty");
		}

		List<AgentExecutionRecord> sequence = record.getAgentExecutionSequence();
		if (sequence == null || sequence.isEmpty()) {
			throw new RuntimeException("Agent execution sequence is empty, unable to obtain result output");
		}

		// Get the last Agent execution record
		AgentExecutionRecord lastAgentRecord = sequence.get(sequence.size() - 1);
		List<ThinkActRecord> thinkActSteps = lastAgentRecord.getThinkActSteps();

		if (thinkActSteps == null || thinkActSteps.isEmpty()) {
			throw new RuntimeException("ThinkAct steps are empty, unable to obtain result output");
		}

		// Get the last ThinkAct record
		ThinkActRecord lastThinkActRecord = thinkActSteps.get(thinkActSteps.size() - 1);
		List<ThinkActRecord.ActToolInfo> actToolInfoList = lastThinkActRecord.getActToolInfoList();

		if (actToolInfoList == null || actToolInfoList.isEmpty()) {
			throw new RuntimeException("ActTool info list is empty, unable to obtain result output");
		}

		// Get the result of the last tool call
		ThinkActRecord.ActToolInfo lastToolInfo = actToolInfoList.get(actToolInfoList.size() - 1);
		String result = lastToolInfo.getResult();

		if (result == null) {
			throw new RuntimeException("Tool call result is empty");
		}

		log.debug("{} Successfully extracted final result: {}", LOG_PREFIX, result);
		return result;
	}

	/**
	 * Poll plan until completion
	 * @param planId Plan ID
	 * @return Completed plan execution record
	 */
	private PlanExecutionRecord pollUntilComplete(String planId) {
		int maxAttempts = coordinatorProperties.getPolling().getMaxAttempts();
		long pollInterval = coordinatorProperties.getPolling().getPollInterval();

		log.debug("{} Starting to poll plan: {}, max attempts: {}, poll interval: {}ms", LOG_PREFIX, planId,
				maxAttempts, pollInterval);

		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			log.debug("{} Polling plan {} attempt {}", LOG_PREFIX, planId, attempt);

			// Get execution record
			PlanExecutionRecord planRecord = getPlanExecutionRecord(planId);

			// Check if completed
			if (planRecord.isCompleted()) {
				log.info("{} Plan {} completed, polling ended", LOG_PREFIX, planId);
				return planRecord;
			}

			log.debug("{} Plan {} not yet completed, continuing to poll", LOG_PREFIX, planId);

			// Wait for specified time before continuing to poll
			if (attempt < maxAttempts) {
				sleepWithInterruptHandling(pollInterval);
			}
		}

		String errorMsg = String.format(POLLING_TIMEOUT_ERROR, maxAttempts);
		log.warn("{} {}", LOG_PREFIX, errorMsg);
		throw new RuntimeException(errorMsg);
	}

	/**
	 * Get plan execution record
	 * @param planId Plan ID
	 * @return Plan execution record
	 */
	private PlanExecutionRecord getPlanExecutionRecord(String planId) {
		try {
			PlanExecutionRecord planRecord = planExecutionRecorder.getRootPlanExecutionRecord(planId);

			if (planRecord == null) {
				String errorMsg = String.format(PLAN_NOT_FOUND_ERROR, planId);
				log.warn("{} {}", LOG_PREFIX, errorMsg);
				throw new RuntimeException(errorMsg);
			}

			return planRecord;
		}
		catch (Exception e) {
			log.error("{} Failed to get plan execution record: {} - {}", LOG_PREFIX, planId, e.getMessage(), e);
			throw new RuntimeException("Failed to get plan execution record: " + e.getMessage(), e);
		}
	}

	/**
	 * Handle interrupt exception during sleep
	 * @param millis Sleep time (milliseconds)
	 */
	private void sleepWithInterruptHandling(long millis) {
		try {
			log.debug("{} Waiting {} milliseconds before continuing to poll", LOG_PREFIX, millis);
			Thread.sleep(millis);
		}
		catch (InterruptedException e) {
			log.warn("{} Polling interrupted: {}", LOG_PREFIX, e.getMessage());
			Thread.currentThread().interrupt();
			throw new RuntimeException(String.format(POLLING_INTERRUPTED_ERROR, e.getMessage()));
		}
	}

}

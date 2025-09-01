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

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.alibaba.cloud.ai.manus.coordinator.tool.CoordinatorTool;
import com.alibaba.cloud.ai.manus.planning.service.IPlanParameterMappingService;
import com.alibaba.cloud.ai.manus.recorder.entity.vo.AgentExecutionRecord;
import com.alibaba.cloud.ai.manus.recorder.entity.vo.PlanExecutionRecord;
import com.alibaba.cloud.ai.manus.recorder.service.PlanExecutionRecorder;
import com.alibaba.cloud.ai.manus.recorder.service.PlanHierarchyReaderService;
import com.alibaba.cloud.ai.manus.recorder.repository.ThinkActRecordRepository;
import com.alibaba.cloud.ai.manus.recorder.entity.po.ThinkActRecordEntity;
import com.alibaba.cloud.ai.manus.config.CoordinatorProperties;

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

	@Autowired
	private PlanExecutionRecorder planExecutionRecorder;

	@Autowired
	private CoordinatorProperties coordinatorProperties;

	@Autowired
	private IPlanParameterMappingService planParameterMappingService;

	@Autowired
	private PlanHierarchyReaderService planHierarchyReaderService;

	@Autowired
	private ThinkActRecordRepository thinkActRecordRepository;

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

			// Use IPlanParameterMappingService to process parameters
			Map<String, Object> processedParams = processParameters(arguments);

			// 2. Execute plan template
			executePlanTemplate(toolName, processedParams, planId);

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
	 * Process request parameters using IPlanParameterMappingService
	 * @param arguments Request parameters
	 * @return Processed parameters map
	 */
	private Map<String, Object> processParameters(Map<String, Object> arguments) {
		if (arguments == null || arguments.isEmpty()) {
			return new HashMap<>();
		}

		// Create parameter copy to avoid modifying original parameters
		Map<String, Object> paramsCopy = new HashMap<>(arguments);
		paramsCopy.remove(PLAN_ID_KEY);

		try {
			// Use IPlanParameterMappingService to process parameters
			// For now, we'll return the processed parameters directly
			// In a real implementation, you might want to apply parameter mapping logic
			log.debug("{} Parameter processing completed: {}", LOG_PREFIX, paramsCopy);
			return paramsCopy;
		}
		catch (Exception e) {
			log.warn("{} Parameter processing failed, using original parameters: {}", LOG_PREFIX, e.getMessage());
			return paramsCopy;
		}
	}

	/**
	 * Execute plan template
	 * @param toolName Tool name
	 * @param rawParam Raw parameters
	 * @param planId Plan ID
	 */
	private void executePlanTemplate(String toolName, Map<String, Object> rawParam, String planId) {
		log.info("{} Executing plan template: {}, parameters: {}, planId: {}", LOG_PREFIX, toolName, rawParam, planId);

		try {
			// For now, we'll just log the execution attempt
			// In a real implementation, you would integrate with the planning system
			log.info("{} Plan template execution started for: {}", LOG_PREFIX, toolName);

			// TODO: Integrate with PlanningCoordinator or other execution service
			// This is a placeholder implementation

		}
		catch (Exception e) {
			log.error("{} Plan template execution failed: {}", LOG_PREFIX, toolName, e);
			throw new RuntimeException("Plan template execution failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Execute plan and get results using async execution
	 * @param planId Plan ID
	 * @return Execution result string
	 */
	public String pollPlanResult(String planId) {
		log.info("{} Starting async plan execution for: {}", LOG_PREFIX, planId);

		try {
			// Wait for plan completion
			PlanExecutionRecord record = waitForPlanCompletion(planId);

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
			log.error("{} Async plan execution failed: {} - {}", LOG_PREFIX, planId, e.getMessage(), e);
			throw new RuntimeException("Async plan execution failed: " + e.getMessage(), e);
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

		// Since AgentExecutionRecord doesn't maintain thinkActSteps in the sequence view,
		// we need to retrieve them from the database using the agent execution ID
		List<ThinkActRecordEntity> thinkActEntities = thinkActRecordRepository
			.findByParentExecutionId(lastAgentRecord.getId());

		if (thinkActEntities == null || thinkActEntities.isEmpty()) {
			throw new RuntimeException("ThinkAct steps are empty, unable to obtain result output");
		}

		// Get the last ThinkAct record
		ThinkActRecordEntity lastThinkActEntity = thinkActEntities.get(thinkActEntities.size() - 1);

		// Check if the ThinkActRecord has ActToolInfo
		if (lastThinkActEntity.getActToolInfoList() == null || lastThinkActEntity.getActToolInfoList().isEmpty()) {
			throw new RuntimeException("ActTool info list is empty, unable to obtain result output");
		}

		// Get the result from the last tool call
		// Since we're working with Entity objects, we need to extract the result directly
		// For now, we'll use a simple approach - you may need to convert this to VO
		// objects
		String result = extractResultFromActToolInfo(lastThinkActEntity);

		if (result == null || result.trim().isEmpty()) {
			throw new RuntimeException("Tool call result is empty");
		}

		log.debug("{} Successfully extracted final result: {}", LOG_PREFIX, result);
		return result;
	}

	/**
	 * Wait for plan completion using efficient polling
	 * @param planId Plan ID
	 * @return Completed plan execution record
	 */
	private PlanExecutionRecord waitForPlanCompletion(String planId) {
		log.debug("{} Starting to wait for plan completion: {}", LOG_PREFIX, planId);

		try {
			// Get the plan execution record to check if it exists
			PlanExecutionRecord existingRecord = getPlanExecutionRecord(planId);
			if (existingRecord == null) {
				throw new RuntimeException("Plan execution record not found for plan: " + planId);
			}

			// Wait for completion using efficient polling
			return waitUntilComplete(planId);

		}
		catch (Exception e) {
			log.error("{} Waiting for plan completion failed for plan {}: {}", LOG_PREFIX, planId, e.getMessage(), e);
			throw new RuntimeException("Waiting for plan completion failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Wait for plan completion using efficient polling
	 * @param planId Plan ID
	 * @return Completed plan execution record
	 */
	private PlanExecutionRecord waitUntilComplete(String planId) {
		int maxAttempts = coordinatorProperties.getPolling().getMaxAttempts();
		long pollInterval = coordinatorProperties.getPolling().getPollInterval();

		log.debug("{} Starting to wait for plan: {}, max attempts: {}, poll interval: {}ms", LOG_PREFIX, planId,
				maxAttempts, pollInterval);

		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			log.debug("{} Waiting for plan {} attempt {}", LOG_PREFIX, planId, attempt);

			// Get execution record
			PlanExecutionRecord planRecord = getPlanExecutionRecord(planId);

			// Check if completed
			if (planRecord.isCompleted()) {
				log.info("{} Plan {} completed, waiting ended", LOG_PREFIX, planId);
				return planRecord;
			}

			log.debug("{} Plan {} not yet completed, continuing to wait", LOG_PREFIX, planId);

			// Wait for specified time before continuing to poll
			if (attempt < maxAttempts) {
				sleepWithInterruptHandling(pollInterval);
			}
		}

		String errorMsg = String.format("Waiting timeout after %d attempts", maxAttempts);
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
			// Use PlanHierarchyReaderService to get the plan execution record
			PlanExecutionRecord planRecord = planHierarchyReaderService.readPlanTreeByRootId(planId);

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
	 * Extract result from ActToolInfo in ThinkActRecordEntity
	 * @param thinkActEntity ThinkActRecordEntity containing ActToolInfo
	 * @return Result string from the last tool call
	 */
	private String extractResultFromActToolInfo(ThinkActRecordEntity thinkActEntity) {
		if (thinkActEntity == null || thinkActEntity.getActToolInfoList() == null
				|| thinkActEntity.getActToolInfoList().isEmpty()) {
			return null;
		}

		// Get the last ActToolInfo from the list
		var lastActToolInfo = thinkActEntity.getActToolInfoList().get(thinkActEntity.getActToolInfoList().size() - 1);

		// Extract the result - you may need to adjust this based on your
		// ActToolInfoEntity structure
		// For now, we'll assume there's a getResult() method or similar
		if (lastActToolInfo != null) {
			// Try to get result from the ActToolInfo - adjust method name as needed
			try {
				// This is a placeholder - you'll need to implement the actual result
				// extraction
				// based on your ActToolInfoEntity structure
				return "Tool execution completed successfully";
			}
			catch (Exception e) {
				log.warn("{} Failed to extract result from ActToolInfo: {}", LOG_PREFIX, e.getMessage());
				return "Tool execution completed";
			}
		}

		return null;
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
			throw new RuntimeException(String.format("Polling interrupted: %s", e.getMessage()));
		}
	}

}

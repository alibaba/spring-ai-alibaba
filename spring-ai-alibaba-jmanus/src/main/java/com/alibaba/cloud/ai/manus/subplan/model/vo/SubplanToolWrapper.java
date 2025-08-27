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
package com.alibaba.cloud.ai.manus.subplan.model.vo;

import com.alibaba.cloud.ai.manus.subplan.model.po.SubplanToolDef;
import com.alibaba.cloud.ai.manus.tool.AbstractBaseTool;
import com.alibaba.cloud.ai.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.manus.planning.service.PlanTemplateService;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.PlanExecutionResult;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.PlanInterface;
import com.alibaba.cloud.ai.manus.runtime.service.PlanIdDispatcher;
import com.alibaba.cloud.ai.manus.runtime.service.PlanningCoordinator;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Wrapper class that extends AbstractBaseTool for SubplanToolDef
 *
 * This allows integration with the existing tool registry system
 */
public class SubplanToolWrapper extends AbstractBaseTool<Map<String, Object>> {

	public static final String PARENT_PLAN_ID_ARG_NAME = "PLAN_PARENT_ID_ARG_NAME";

	private static final Logger logger = LoggerFactory.getLogger(SubplanToolWrapper.class);

	private final SubplanToolDef subplanTool;

	private final PlanTemplateService planTemplateService;

	private final PlanningCoordinator planningCoordinator;

	private final PlanIdDispatcher planIdDispatcher;

	private final ObjectMapper objectMapper;

	public SubplanToolWrapper(SubplanToolDef subplanTool, String currentPlanId, String rootPlanId,
			PlanTemplateService planTemplateService, PlanningCoordinator planningCoordinator,
			PlanIdDispatcher planIdDispatcher, ObjectMapper objectMapper) {
		this.subplanTool = subplanTool;
		this.currentPlanId = currentPlanId;
		this.rootPlanId = rootPlanId;
		this.planTemplateService = planTemplateService;
		this.planningCoordinator = planningCoordinator;
		this.planIdDispatcher = planIdDispatcher;
		this.objectMapper = objectMapper;
	}

	@Override
	public String getServiceGroup() {
		return subplanTool.getServiceGroup();
	}

	@Override
	public String getName() {
		return subplanTool.getToolName();
	}

	@Override
	public String getDescription() {
		return subplanTool.getToolDescription();
	}

	@Override
	public String getParameters() {
		// This will be handled by the service layer
		return "{}";
	}

	@Override
	public Class<Map<String, Object>> getInputType() {
		@SuppressWarnings("unchecked")
		Class<Map<String, Object>> mapClass = (Class<Map<String, Object>>) (Class<?>) Map.class;
		return mapClass;
	}

	@Override
	public ToolExecuteResult run(Map<String, Object> input) {
		try {
			logger.info("Executing subplan tool: {} with template: {}", subplanTool.getToolName(),
					subplanTool.getPlanTemplateId());

			// Get the plan template from PlanTemplateService
			String planJson = planTemplateService.getLatestPlanVersion(subplanTool.getPlanTemplateId());
			if (planJson == null) {
				String errorMsg = "Plan template not found: " + subplanTool.getPlanTemplateId();
				logger.error(errorMsg);
				return new ToolExecuteResult(errorMsg);
			}

			// Parse the JSON to create a PlanInterface
			PlanInterface plan = objectMapper.readValue(planJson, PlanInterface.class);

			// Execute the plan using PlanningCoordinator
			// Generate a new plan ID for this subplan execution using PlanIdDispatcher
			String newPlanId = planIdDispatcher.generateSubPlanId(rootPlanId);
			// Generate a toolcall ID for this subplan execution
			String toolCallId = planIdDispatcher.generateToolCallId();
			logger.info("Generated toolcall ID: {} for subplan execution: {}", toolCallId, newPlanId);
			
			CompletableFuture<PlanExecutionResult> future = planningCoordinator.executeByPlan(plan, rootPlanId,
					currentPlanId, newPlanId, toolCallId);

			PlanExecutionResult result = future.get();

			if (result.isSuccess()) {
				String output = result.getFinalResult();
				if (output == null || output.trim().isEmpty()) {
					output = "Subplan executed successfully but no output was generated";
				}
				logger.info("Subplan execution completed successfully: {}", output);
				return new ToolExecuteResult(output);
			}
			else {
				String errorMsg = result.getErrorMessage() != null ? result.getErrorMessage()
						: "Subplan execution failed";
				logger.error("Subplan execution failed: {}", errorMsg);
				return new ToolExecuteResult("Subplan execution failed: " + errorMsg);
			}

		}
		catch (InterruptedException e) {
			String errorMsg = "Subplan execution was interrupted";
			logger.error("{} for tool: {}", errorMsg, subplanTool.getToolName(), e);
			Thread.currentThread().interrupt(); // Restore interrupt status
			return new ToolExecuteResult(errorMsg);
		}
		catch (ExecutionException e) {
			String errorMsg = "Subplan execution failed with exception: " + e.getCause().getMessage();
			logger.error("{} for tool: {}", errorMsg, subplanTool.getToolName(), e);
			return new ToolExecuteResult(errorMsg);
		}
		catch (Exception e) {
			String errorMsg = "Unexpected error during subplan execution: " + e.getMessage();
			logger.error("{} for tool: {}", errorMsg, subplanTool.getToolName(), e);
			return new ToolExecuteResult(errorMsg);
		}
	}

	@Override
	public void cleanup(String planId) {
		// Cleanup logic for the subplan tool
		logger.debug("Cleaning up subplan tool: {} for planId: {}", subplanTool.getToolName(), planId);
	}

	@Override
	public String getCurrentToolStateString() {
		return "Ready";
	}

	// Getter for the wrapped subplan tool
	public SubplanToolDef getSubplanTool() {
		return subplanTool;
	}

}

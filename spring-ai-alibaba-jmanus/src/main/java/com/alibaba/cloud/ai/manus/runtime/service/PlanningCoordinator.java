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
package com.alibaba.cloud.ai.manus.runtime.service;

import com.alibaba.cloud.ai.manus.planning.service.IPlanCreator;
import com.alibaba.cloud.ai.manus.planning.service.PlanFinalizer;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.ExecutionContext;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.PlanExecutionResult;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.PlanInterface;
import com.alibaba.cloud.ai.manus.runtime.executor.PlanExecutorInterface;
import com.alibaba.cloud.ai.manus.runtime.executor.factory.PlanExecutorFactory;
import com.alibaba.cloud.ai.manus.planning.PlanningFactory;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Enhanced Planning Coordinator that uses PlanExecutorFactory to dynamically select the
 * appropriate executor based on plan type
 */
@Service
public class PlanningCoordinator {

	private static final Logger log = LoggerFactory.getLogger(PlanningCoordinator.class);

	private final PlanningFactory planningFactory;

	private final PlanExecutorFactory planExecutorFactory;

	private final PlanFinalizer planFinalizer;

	private final PlanIdDispatcher planIdDispatcher;

	@Autowired
	public PlanningCoordinator(PlanningFactory planningFactory, PlanExecutorFactory planExecutorFactory,
			PlanFinalizer planFinalizer, PlanIdDispatcher planIdDispatcher) {
		this.planningFactory = planningFactory;
		this.planExecutorFactory = planExecutorFactory;
		this.planFinalizer = planFinalizer;
		this.planIdDispatcher = planIdDispatcher;
	}

	/**
	 * Execute plan by user query using plan creator and then execute the created plan
	 * @param userQuery The user's query/request
	 * @param rootPlanId The root plan ID for the execution context
	 * @param parentPlanId The ID of the parent plan (can be null for root plans)
	 * @param currentPlanId The current plan ID for execution
	 * @param memoryId The memory ID for the execution context
	 * @param toolcallId The ID of the tool call that triggered this plan execution
	 * @return A CompletableFuture that completes with the execution result
	 */
	public CompletableFuture<PlanExecutionResult> executeByUserQuery(String userQuery, String rootPlanId,
			String parentPlanId, String currentPlanId, String memoryId, String toolcallId) {
		return executeByUserQuery(userQuery, rootPlanId, parentPlanId, currentPlanId, memoryId, toolcallId, "simple");
	}

	/**
	 * Execute plan by user query using plan creator and then execute the created plan
	 * @param userQuery The user's query/request
	 * @param rootPlanId The root plan ID for the execution context
	 * @param parentPlanId The ID of the parent plan (can be null for root plans)
	 * @param currentPlanId The current plan ID for execution
	 * @param memoryId The memory ID for the execution context
	 * @param toolcallId The ID of the tool call that triggered this plan execution
	 * @param planType The type of plan to create ("simple" or "dynamic_agent")
	 * @return A CompletableFuture that completes with the execution result
	 */
	public CompletableFuture<PlanExecutionResult> executeByUserQuery(String userQuery, String rootPlanId,
			String parentPlanId, String currentPlanId, String memoryId, String toolcallId, String planType) {
		try {
			log.info("Starting plan execution for user query: {}", userQuery);

			// Log toolcallId if provided
			if (toolcallId != null) {
				log.debug("Plan execution triggered by tool call: {}", toolcallId);
			}

			// Create execution context
			ExecutionContext context = new ExecutionContext();
			context.setCurrentPlanId(currentPlanId);
			context.setRootPlanId(rootPlanId);
			context.setUserRequest(userQuery);
			context.setNeedSummary(true);
			context.setUseMemory(false);
			context.setMemoryId(memoryId);
			context.setParentPlanId(parentPlanId);
			context.setToolCallId(toolcallId);

			// Create plan using PlanningFactory with specified plan type
			IPlanCreator planCreator = planningFactory.createPlanCreator(planType);
			planCreator.createPlanWithoutMemory(context);

			// Check if plan was created successfully
			if (context.getPlan() == null) {
				PlanExecutionResult errorResult = new PlanExecutionResult();
				errorResult.setSuccess(false);
				errorResult.setErrorMessage("Plan creation failed, cannot create execution plan");
				return CompletableFuture.completedFuture(errorResult);
			}

			// Execute the plan using PlanExecutorFactory
			PlanExecutorInterface executor = planExecutorFactory.createExecutor(context.getPlan());
			CompletableFuture<PlanExecutionResult> executionFuture = executor.executeAllStepsAsync(context);

			// Add post-execution processing
			return executionFuture.thenCompose(result -> {
				try {
					PlanExecutionResult processedResult = planFinalizer.handlePostExecution(context, result);
					return CompletableFuture.completedFuture(processedResult);
				}
				catch (Exception e) {
					log.error("Error during post-execution processing for plan: {}", context.getCurrentPlanId(), e);
					return CompletableFuture.failedFuture(e);
				}
			});

		}
		catch (Exception e) {
			log.error("Error during plan execution", e);
			PlanExecutionResult errorResult = new PlanExecutionResult();
			errorResult.setSuccess(false);
			errorResult.setErrorMessage("Plan execution failed: " + e.getMessage());
			return CompletableFuture.completedFuture(errorResult);
		}
	}

	/**
	 * Execute a plan directly using the provided plan interface
	 * @param plan The plan to execute
	 * @param rootPlanId The root plan ID for the execution context
	 * @param parentPlanId The ID of the parent plan (can be null for root plans)
	 * @param currentPlanId The current plan ID for execution
	 * @param toolcallId The ID of the tool call that triggered this plan execution
	 * @param isVueRequest Flag indicating whether this is a Vue frontend request
	 * @return A CompletableFuture that completes with the execution result
	 */
	public CompletableFuture<PlanExecutionResult> executeByPlan(PlanInterface plan, String rootPlanId,
			String parentPlanId, String currentPlanId, String toolcallId, boolean isVueRequest) {
		try {
			log.info("Starting direct plan execution for plan: {}", plan.getCurrentPlanId());

			// Create execution context
			ExecutionContext context = new ExecutionContext();
			String userRequest = plan.getUserRequest();
			if (userRequest == null) {
				userRequest = plan.getTitle();
			}
			context.setUserRequest(userRequest);
			context.setCurrentPlanId(currentPlanId);
			context.setRootPlanId(rootPlanId);
			context.setPlan(plan);
			if (toolcallId == null && isVueRequest) {
				context.setNeedSummary(true);
			}
			else {
				// in sub plan or non-Vue request, we don't need to generate summary
				context.setNeedSummary(false);
			}
			// Generate a memory ID if none exists, since we're using memory
			if (context.getMemoryId() == null) {
				String generatedMemoryId = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
				context.setMemoryId(generatedMemoryId);
			}
			context.setUseMemory(true);
			context.setParentPlanId(parentPlanId);
			context.setToolCallId(toolcallId);

			// Log toolcallId if provided
			if (toolcallId != null) {
				log.debug("Plan execution triggered by tool call: {}", toolcallId);
			}

			// Execute the plan using PlanExecutorFactory
			PlanExecutorInterface executor = planExecutorFactory.createExecutor(plan);
			CompletableFuture<PlanExecutionResult> executionFuture = executor.executeAllStepsAsync(context);

			// Add post-execution processing
			return executionFuture.thenCompose(result -> {
				try {
					PlanExecutionResult processedResult = planFinalizer.handlePostExecution(context, result);
					return CompletableFuture.completedFuture(processedResult);
				}
				catch (Exception e) {
					log.error("Error during post-execution processing for plan: {}", context.getCurrentPlanId(), e);
					return CompletableFuture.failedFuture(e);
				}
			});

		}
		catch (Exception e) {
			log.error("Error during direct plan execution", e);
			PlanExecutionResult errorResult = new PlanExecutionResult();
			errorResult.setSuccess(false);
			errorResult.setErrorMessage("Direct plan execution failed: " + e.getMessage());
			return CompletableFuture.completedFuture(errorResult);
		}
	}

}

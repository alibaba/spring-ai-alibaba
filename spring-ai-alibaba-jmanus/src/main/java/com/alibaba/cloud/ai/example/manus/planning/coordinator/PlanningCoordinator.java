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
package com.alibaba.cloud.ai.example.manus.planning.coordinator;

import com.alibaba.cloud.ai.example.manus.planning.creator.PlanCreator;
import com.alibaba.cloud.ai.example.manus.planning.executor.PlanExecutorInterface;
import com.alibaba.cloud.ai.example.manus.planning.executor.factory.PlanExecutorFactory;
import com.alibaba.cloud.ai.example.manus.planning.finalizer.PlanFinalizer;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.PlanInterface;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.PlanExecutionResult;
import com.alibaba.cloud.ai.example.manus.planning.service.IPlanRelationshipService;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enhanced Planning Coordinator that uses PlanExecutorFactory to dynamically select the
 * appropriate executor based on plan type
 */
public class PlanningCoordinator {

	private static final Logger log = LoggerFactory.getLogger(PlanningCoordinator.class);

	private final PlanCreator planCreator;

	private final PlanExecutorFactory planExecutorFactory;

	private PlanFinalizer planFinalizer;

	private final IPlanRelationshipService planRelationshipService;

	private final PlanIdDispatcher planIdDispatcher;

	public PlanningCoordinator(PlanCreator planCreator, PlanExecutorFactory planExecutorFactory,
			PlanFinalizer planFinalizer, IPlanRelationshipService planRelationshipService,
			PlanIdDispatcher planIdDispatcher) {
		this.planCreator = planCreator;
		this.planExecutorFactory = planExecutorFactory;
		this.planFinalizer = planFinalizer;
		this.planRelationshipService = planRelationshipService;
		this.planIdDispatcher = planIdDispatcher;
	}

	/**
	 * Constructor for backward compatibility when relationship service is not available
	 */
	public PlanningCoordinator(PlanCreator planCreator, PlanExecutorFactory planExecutorFactory,
			PlanFinalizer planFinalizer) {
		this(planCreator, planExecutorFactory, planFinalizer, null, null);
	}

	/**
	 * Execute plan by user query using plan creator and then execute the created plan
	 * @param userQuery The user's query/request
	 * @param rootPlanId The root plan ID for the execution context
	 * @param parentPlanId The ID of the parent plan (can be null for root plans)
	 * @param currentPlanId The current plan ID for execution
	 * @return A CompletableFuture that completes with the execution result
	 */
	public CompletableFuture<PlanExecutionResult> executeByUserQuery(String userQuery, String rootPlanId,
			String parentPlanId, String currentPlanId) {

		if (userQuery == null || userQuery.trim().isEmpty()) {
			log.error("User query is null or empty for plan: {}", currentPlanId);
			PlanExecutionResult errorResult = new PlanExecutionResult();
			errorResult.setSuccess(false);
			errorResult.setErrorMessage("User query cannot be null or empty");
			return CompletableFuture.completedFuture(errorResult);
		}

		try {
			// Create execution context for plan creation
			ExecutionContext context = new ExecutionContext();
			context.setCurrentPlanId(currentPlanId);
			context.setRootPlanId(rootPlanId);
			context.setUserRequest(userQuery);
			context.setNeedSummary(false);
			context.setUseMemory(false);

			// Create plan using plan creator
			planCreator.createPlanWithoutMemory(context);

			// Get the created plan from context
			PlanInterface plan = context.getPlan();
			if (plan == null) {
				log.error("Failed to create plan from user query: {}", userQuery);
				PlanExecutionResult errorResult = new PlanExecutionResult();
				errorResult.setSuccess(false);
				errorResult.setErrorMessage("Failed to create plan from user query");
				return CompletableFuture.completedFuture(errorResult);
			}

			// Execute the created plan using executeByPlan
			log.info("Plan created successfully from user query, executing plan: {}", currentPlanId);
			return executeByPlan(plan, rootPlanId, parentPlanId, currentPlanId);

		}
		catch (Exception e) {
			log.error("Failed to execute by user query: {}", userQuery, e);
			PlanExecutionResult errorResult = new PlanExecutionResult();
			errorResult.setSuccess(false);
			errorResult.setErrorMessage("Execution failed: " + e.getMessage());
			return CompletableFuture.completedFuture(errorResult);
		}
	}

	/**
	 * Execute a common plan with the given plan interface. This method handles the core
	 * execution logic and can be called from external classes.
	 * @param plan The plan interface to execute
	 * @param rootPlanId The root plan ID for the execution context
	 * @param parentPlanId The ID of the parent plan (can be null for root plans)
	 * @param currentPlanId The current plan ID for execution
	 * @return A CompletableFuture that completes with the execution result
	 */
	public CompletableFuture<PlanExecutionResult> executeByPlan(PlanInterface plan, String rootPlanId,
			String parentPlanId, String currentPlanId) {

		if (plan == null) {
			log.error("Plan interface is null for plan: {}", currentPlanId);
			PlanExecutionResult errorResult = new PlanExecutionResult();
			errorResult.setSuccess(false);
			errorResult.setErrorMessage("Plan interface is null");
			return CompletableFuture.completedFuture(errorResult);
		}

		try {
			// Log plan relationship if relationship service is available
			if (planRelationshipService != null) {
				planRelationshipService.recordPlanRelationship(parentPlanId, // Parent
																				// plan ID
																				// (can be
																				// null
																				// for
																				// root
																				// plans)
						currentPlanId, // Child plan ID (current executing plan)
						rootPlanId, // Root plan ID (top-level parent plan)
						null, // Plan template ID (not applicable for common plans)
						"common-plan-execution" // Relationship type
				);
				log.info("Recorded plan relationship for common plan execution: parent={}, child={}, root={}",
						parentPlanId, currentPlanId, rootPlanId);
			}

			// Create execution context
			ExecutionContext context = new ExecutionContext();
			context.setCurrentPlanId(currentPlanId);
			context.setRootPlanId(rootPlanId);
			context.setUserRequest("Execute common plan: " + currentPlanId);
			context.setNeedSummary(false);
			context.setUseMemory(false);

			// Set the plan directly to context (no need to parse JSON)
			context.setPlan(plan);

			// Execute directly using PlanExecutorFactory and PlanExecutorInterface
			PlanExecutorInterface executor = planExecutorFactory.createExecutor(plan);
			return executor.executeAllStepsAsync(context);

		}
		catch (Exception e) {
			log.error("Failed to execute common plan: {}", currentPlanId, e);
			PlanExecutionResult errorResult = new PlanExecutionResult();
			errorResult.setSuccess(false);
			errorResult.setErrorMessage("Execution failed: " + e.getMessage());
			return CompletableFuture.completedFuture(errorResult);
		}
	}

}

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
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.PlanInterface;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.PlanExecutionResult;
import com.alibaba.cloud.ai.example.manus.planning.service.IPlanRelationshipService;
import com.alibaba.cloud.ai.example.manus.planning.PlanningFactory;
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

	private final IPlanRelationshipService planRelationshipService;

	private final PlanIdDispatcher planIdDispatcher;

	@Autowired
	public PlanningCoordinator(PlanningFactory planningFactory, PlanExecutorFactory planExecutorFactory,
			IPlanRelationshipService planRelationshipService, PlanIdDispatcher planIdDispatcher) {
		this.planningFactory = planningFactory;
		this.planExecutorFactory = planExecutorFactory;
		this.planRelationshipService = planRelationshipService;
		this.planIdDispatcher = planIdDispatcher;
	}

	/**
	 * Constructor for backward compatibility when relationship service is not available
	 */
	public PlanningCoordinator(PlanningFactory planningFactory, PlanExecutorFactory planExecutorFactory,
			PlanIdDispatcher planIdDispatcher) {
		this(planningFactory, planExecutorFactory, null, planIdDispatcher);
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
		try {
			log.info("Starting plan execution for user query: {}", userQuery);

			// Create execution context
			ExecutionContext context = new ExecutionContext();
			context.setCurrentPlanId(currentPlanId);
			context.setRootPlanId(rootPlanId);
			context.setUserRequest(userQuery);
			context.setNeedSummary(true);
			context.setUseMemory(false);

			// Create plan using PlanningFactory
			PlanCreator planCreator = planningFactory.createPlanCreator();
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
			return executor.executeAllStepsAsync(context);

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
	 * @return A CompletableFuture that completes with the execution result
	 */
	public CompletableFuture<PlanExecutionResult> executeByPlan(PlanInterface plan, String rootPlanId,
			String parentPlanId, String currentPlanId) {
		try {
			log.info("Starting direct plan execution for plan: {}", plan.getCurrentPlanId());

			// Create execution context
			ExecutionContext context = new ExecutionContext();
			context.setCurrentPlanId(currentPlanId);
			context.setRootPlanId(rootPlanId);
			context.setPlan(plan);
			context.setNeedSummary(true);
			context.setUseMemory(false);

			// Execute the plan using PlanExecutorFactory
			PlanExecutorInterface executor = planExecutorFactory.createExecutor(plan);
			return executor.executeAllStepsAsync(context);

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

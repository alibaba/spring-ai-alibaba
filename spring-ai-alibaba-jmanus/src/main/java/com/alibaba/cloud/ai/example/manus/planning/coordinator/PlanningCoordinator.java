// /*
//  * Copyright 2025 the original author or authors.
//  *
//  * Licensed under the Apache License, Version 2.0 (the "License");
//  * you may not use this file except in compliance with the License.
//  * You may obtain a copy of the License at
//  *
//  *      https://www.apache.org/licenses/LICENSE-2.0
//  *
//  * Unless required by applicable law or agreed to in writing, software
//  * distributed under the License is distributed on an "AS IS" BASIS,
//  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  * See the License for the specific language governing permissions and
//  * limitations under the License.
//  */
// package com.alibaba.cloud.ai.example.manus.planning.coordinator;

// import com.alibaba.cloud.ai.example.manus.planning.creator.PlanCreator;
// import com.alibaba.cloud.ai.example.manus.planning.executor.PlanExecutorInterface;
// import com.alibaba.cloud.ai.example.manus.planning.executor.factory.PlanExecutorFactory;
// import com.alibaba.cloud.ai.example.manus.planning.finalizer.PlanFinalizer;
// import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
// import com.alibaba.cloud.ai.example.manus.planning.model.vo.PlanInterface;
// import com.alibaba.cloud.ai.example.manus.runtime.task.PlanTask;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// /**
//  * Enhanced Planning Coordinator that uses PlanExecutorFactory to dynamically select the
//  * appropriate executor based on plan type
//  */
// public class PlanningCoordinator {

// 	private static final Logger log = LoggerFactory.getLogger(PlanningCoordinator.class);

// 	private final PlanCreator planCreator;

// 	private final PlanExecutorFactory planExecutorFactory;

// 	private PlanFinalizer planFinalizer;

// 	public PlanningCoordinator(PlanCreator planCreator, PlanExecutorFactory planExecutorFactory,
// 			PlanFinalizer planFinalizer) {
// 		this.planCreator = planCreator;
// 		this.planExecutorFactory = planExecutorFactory;
// 		this.planFinalizer = planFinalizer;
// 	}

// 	/**
// 	 * Create a plan only, without executing it
// 	 * @param context execution context
// 	 * @return execution context
// 	 */
// 	public void createPlan(ExecutionContext context) {
// 		log.info("Creating plan for planId: {}", context.getCurrentPlanId());
// 		planCreator.createPlanWithoutMemory(context);

// 		PlanInterface plan = context.getPlan();
// 		if (plan != null) {
// 			log.info("Plan created successfully with type: {} for planId: {}", plan.getPlanType(),
// 					context.getCurrentPlanId());
// 		}
// 	}

// 	/**
// 	 * Execute the complete plan process with dynamic executor selection
// 	 * @param context execution context
// 	 * @return execution summary
// 	 */
// 	public ExecutionContext executePlan(ExecutionContext context) {
// 		log.info("Executing complete plan process for planId: {}", context.getCurrentPlanId());

// 		// 1. Create a plan (normal flow)
// 		planCreator.createPlanWithMemory(context);

// 		// 2. Select appropriate executor based on plan type and execute
// 		PlanInterface plan = context.getPlan();

// 		if (plan != null) {
// 			// Check if this is a direct response plan
// 			boolean isDirectResponse = plan.isDirectResponse();
// 			if (isDirectResponse) {
// 				// For direct response plans, use DirectResponseExecutor but handle
// 				// generation in coordinator
// 				PlanExecutorInterface executor = planExecutorFactory.createExecutor(plan);
// 				log.info("Selected executor: {} for direct response plan (planId: {})",
// 						executor.getClass().getSimpleName(), context.getCurrentPlanId());
// 				PlanTask task = new PlanTask(context, null, executor);
// 				task.start();

// 				// Generate direct response using PlanFinalizer
// 				planFinalizer.generateDirectResponse(context);
// 				log.info("Direct response completed successfully for planId: {}", context.getCurrentPlanId());
// 				return context;
// 			}
// 			else {
//                 // Normal plan execution -> wrap into PlanTask and start
//                 PlanExecutorInterface executor = planExecutorFactory.createExecutor(plan);
//                 log.info("Selected executor: {} for plan type: {} (planId: {})", executor.getClass().getSimpleName(),
//                         plan.getPlanType(), context.getCurrentPlanId());
//                 PlanTask task =
//                         new  PlanTask(context, null, executor);
//                 task.start();
// 			}
// 		}
// 		else {
// 			log.error("No plan found in context for planId: {}", context.getCurrentPlanId());
// 			throw new IllegalStateException("Plan creation failed, no plan found in execution context");
// 		}

// 		// 3. Generate a summary
// 		planFinalizer.generateSummary(context);

// 		log.info("Plan execution completed successfully for planId: {}", context.getCurrentPlanId());
// 		return context;
// 	}

// 	/**
// 	 * Execute an existing plan (skip the create plan step) with dynamic executor
// 	 * selection
// 	 * @param context execution context containing the existing plan
// 	 * @return execution summary
// 	 */
// 	public ExecutionContext executeExistingPlan(ExecutionContext context) {
// 		log.info("Executing existing plan for planId: {}", context.getCurrentPlanId());

// 		PlanInterface plan = context.getPlan();
// 		if (plan == null) {
// 			log.error("No existing plan found in context for planId: {}", context.getCurrentPlanId());
// 			throw new IllegalArgumentException("No existing plan found in execution context");
// 		}

// 		// 1. Select appropriate executor based on plan type and execute
// 		PlanExecutorInterface executor = planExecutorFactory.createExecutor(plan);
// 		log.info("Selected executor: {} for existing plan type: {} (planId: {})", executor.getClass().getSimpleName(),
// 				plan.getPlanType(), context.getCurrentPlanId());
// 		PlanTask task = new PlanTask(context, null, executor);
// 		task.start();

// 		// 2. Generate a summary
// 		planFinalizer.generateSummary(context);

// 		log.info("Existing plan execution completed successfully for planId: {}", context.getCurrentPlanId());
// 		return context;
// 	}

// 	/**
// 	 * Get information about supported plan types
// 	 * @return Array of supported plan types
// 	 */
// 	public String[] getSupportedPlanTypes() {
// 		return planExecutorFactory.getSupportedPlanTypes();
// 	}

// 	/**
// 	 * Check if a plan type is supported
// 	 * @param planType The plan type to check
// 	 * @return true if supported, false otherwise
// 	 */
// 	public boolean isPlanTypeSupported(String planType) {
// 		return planExecutorFactory.isPlanTypeSupported(planType);
// 	}

// 	public void setPlanFinalizer(PlanFinalizer planFinalizer) {
// 		this.planFinalizer = planFinalizer;
// 	}

// }

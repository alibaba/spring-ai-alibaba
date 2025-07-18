// /*
// * Copyright 2025 the original author or authors.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * https://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
// package com.alibaba.cloud.ai.example.manus.planning.coordinator;

// import com.alibaba.cloud.ai.example.manus.planning.creator.PlanCreator;
// import com.alibaba.cloud.ai.example.manus.planning.executor.PlanExecutorInterface;
// import
// com.alibaba.cloud.ai.example.manus.planning.executor.factory.PlanExecutorFactory;
// import com.alibaba.cloud.ai.example.manus.planning.finalizer.PlanFinalizer;
// import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
// import com.alibaba.cloud.ai.example.manus.planning.model.vo.PlanInterface;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// /**
// * Enhanced Planning Coordinator that uses PlanExecutorFactory
// * to dynamically select the appropriate executor based on plan type
// */
// public class EnhancedPlanningCoordinator {

// private static final Logger log =
// LoggerFactory.getLogger(EnhancedPlanningCoordinator.class);

// private final PlanCreator planCreator;
// private final PlanExecutorFactory planExecutorFactory;
// private final PlanFinalizer planFinalizer;

// public EnhancedPlanningCoordinator(PlanCreator planCreator, PlanExecutorFactory
// planExecutorFactory,
// PlanFinalizer planFinalizer) {
// this.planCreator = planCreator;
// this.planExecutorFactory = planExecutorFactory;
// this.planFinalizer = planFinalizer;
// }

// /**
// * Create a plan only, without executing it
// * @param context execution context
// * @return execution context
// */
// public ExecutionContext createPlan(ExecutionContext context) {
// log.info("Creating plan for planId: {}", context.getPlanId());
// // Only execute the create plan step
// context.setUseMemory(false);
// planCreator.createPlan(context);

// PlanInterface plan = context.getPlan();
// if (plan != null) {
// log.info("Plan created successfully with type: {} for planId: {}",
// plan.getPlanType(), context.getPlanId());
// }

// return context;
// }

// /**
// * Execute the complete plan process with dynamic executor selection
// * @param context execution context
// * @return execution summary
// */
// public ExecutionContext executePlan(ExecutionContext context) {
// log.info("Executing complete plan process for planId: {}", context.getPlanId());
// context.setUseMemory(true);

// // 1. Create a plan
// planCreator.createPlan(context);

// // 2. Select appropriate executor based on plan type and execute
// PlanInterface plan = context.getPlan();
// if (plan != null) {
// PlanExecutorInterface executor = planExecutorFactory.createExecutor(plan);
// log.info("Selected executor: {} for plan type: {} (planId: {})",
// executor.getClass().getSimpleName(), plan.getPlanType(), context.getPlanId());
// executor.executeAllSteps(context);
// } else {
// log.error("No plan found in context for planId: {}", context.getPlanId());
// throw new IllegalStateException("Plan creation failed, no plan found in execution
// context");
// }

// // 3. Generate a summary
// planFinalizer.generateSummary(context);

// log.info("Plan execution completed successfully for planId: {}", context.getPlanId());
// return context;
// }

// /**
// * Execute an existing plan (skip the create plan step) with dynamic executor selection
// * @param context execution context containing the existing plan
// * @return execution summary
// */
// public ExecutionContext executeExistingPlan(ExecutionContext context) {
// log.info("Executing existing plan for planId: {}", context.getPlanId());

// PlanInterface plan = context.getPlan();
// if (plan == null) {
// log.error("No existing plan found in context for planId: {}", context.getPlanId());
// throw new IllegalArgumentException("No existing plan found in execution context");
// }

// // 1. Select appropriate executor based on plan type and execute
// PlanExecutorInterface executor = planExecutorFactory.createExecutor(plan);
// log.info("Selected executor: {} for existing plan type: {} (planId: {})",
// executor.getClass().getSimpleName(), plan.getPlanType(), context.getPlanId());
// executor.executeAllSteps(context);

// // 2. Generate a summary
// planFinalizer.generateSummary(context);

// log.info("Existing plan execution completed successfully for planId: {}",
// context.getPlanId());
// return context;
// }

// /**
// * Execute plan with explicit executor type (useful for testing or override scenarios)
// * @param context execution context
// * @param executorType explicit executor type to use
// * @return execution summary
// */
// public ExecutionContext executeWithExplicitExecutor(ExecutionContext context, String
// executorType) {
// log.info("Executing plan with explicit executor type: {} for planId: {}",
// executorType, context.getPlanId());

// PlanInterface plan = context.getPlan();
// if (plan == null) {
// log.error("No plan found in context for planId: {}", context.getPlanId());
// throw new IllegalArgumentException("No plan found in execution context");
// }

// // Select executor based on explicit type
// PlanExecutorInterface executor = planExecutorFactory.createExecutorByType(executorType,
// context.getPlanId());
// log.info("Using explicit executor: {} for planId: {}",
// executor.getClass().getSimpleName(), context.getPlanId());

// executor.executeAllSteps(context);
// planFinalizer.generateSummary(context);

// log.info("Plan execution with explicit executor completed successfully for planId: {}",
// context.getPlanId());
// return context;
// }

// /**
// * Get information about supported plan types
// * @return Array of supported plan types
// */
// public String[] getSupportedPlanTypes() {
// return planExecutorFactory.getSupportedPlanTypes();
// }

// /**
// * Check if a plan type is supported
// * @param planType The plan type to check
// * @return true if supported, false otherwise
// */
// public boolean isPlanTypeSupported(String planType) {
// return planExecutorFactory.isPlanTypeSupported(planType);
// }
// }

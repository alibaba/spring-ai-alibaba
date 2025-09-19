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
package com.alibaba.cloud.ai.manus.cron.scheduler;

import com.alibaba.cloud.ai.manus.cron.entity.CronEntity;
import com.alibaba.cloud.ai.manus.cron.enums.TaskStatus;
import com.alibaba.cloud.ai.manus.cron.repository.CronRepository;
import com.alibaba.cloud.ai.manus.planning.PlanningFactory;
import com.alibaba.cloud.ai.manus.planning.model.po.PlanTemplate;
import com.alibaba.cloud.ai.manus.planning.service.PlanTemplateService;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.PlanExecutionResult;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.PlanInterface;
import com.alibaba.cloud.ai.manus.runtime.service.PlanIdDispatcher;
import com.alibaba.cloud.ai.manus.runtime.service.PlanningCoordinator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Dynamic task scheduler responsible for managing the lifecycle of all dynamic scheduled
 * tasks
 */
@Component
public class DynamicCronTaskScheduler {

	private static final Logger log = LoggerFactory.getLogger(DynamicCronTaskScheduler.class);

	private final TaskScheduler taskScheduler;

	private final CronRepository cronRepository;

	@Autowired
	private PlanIdDispatcher planIdDispatcher;

	@Autowired
	@Lazy
	private PlanningFactory planningFactory;

	@Autowired
	private PlanTemplateService planTemplateService;

	@Autowired
	private PlanningCoordinator planningCoordinator;

	@Autowired
	private ObjectMapper objectMapper;

	// Store running tasks
	private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

	@Autowired
	public DynamicCronTaskScheduler(TaskScheduler taskScheduler, CronRepository cronRepository) {
		this.taskScheduler = taskScheduler;
		this.cronRepository = cronRepository;
	}

	/**
	 * Execute scheduled task
	 * @param cronEntity Task entity
	 */
	private void executeTask(CronEntity cronEntity) {
		try {
			// Update task execution time
			cronEntity.setLastExecutedTime(LocalDateTime.now());
			cronRepository.save(cronEntity);

			String planTemplateId = cronEntity.getPlanTemplateId();

			if (planTemplateId != null && !planTemplateId.trim().isEmpty()) {
				// If plan template ID exists, execute according to plan template
				executePlanTemplate(planTemplateId);
			}
			else {
				executePlan(cronEntity);
			}
		}
		catch (Exception e) {
			log.error("Task execution failed: {} - {}", cronEntity.getCronName(), e.getMessage());
		}
	}

	/**
	 * Generate plan and execute
	 * @param cronEntity Task entity
	 */
	private void executePlan(CronEntity cronEntity) {
		String planDesc = cronEntity.getPlanDesc();
		log.info("Executing scheduled task: {} - {}", cronEntity.getCronName(), planDesc);

		String planId = planIdDispatcher.generatePlanId();

		// Execute task asynchronously using PlanningCoordinator
		executePlanByuserQueryDesc(planId, planDesc);
	}

	/**
	 * Execute plan template
	 * @param planTemplateId Plan template ID
	 */
	private void executePlanTemplate(String planTemplateId) {
		try {
			log.info("Executing plan template: {}", planTemplateId);

			// Get the plan template to check if it exists
			PlanTemplate template = planTemplateService.getPlanTemplate(planTemplateId);
			if (template == null) {
				log.error("Plan template not found: {}", planTemplateId);
				return;
			}

			// Execute the plan template using the new method
			executePlanTemplateInternal(planTemplateId, null, null);
		}
		catch (Exception e) {
			log.error("Failed to execute plan template: {}", planTemplateId, e);
		}
	}

	/**
	 * Add scheduled task
	 */
	public boolean addTask(CronEntity cronEntity) {
		try {
			if (scheduledTasks.containsKey(cronEntity.getId())) {
				return false;
			}

			if (!TaskStatus.ENABLED.getCode().equals(cronEntity.getStatus())) {
				return false;
			}

			ScheduledFuture<?> future = taskScheduler.schedule(() -> executeTask(cronEntity),
					new CronTrigger(cronEntity.getCronTime()));

			scheduledTasks.put(cronEntity.getId(), future);
			log.info("Adding scheduled task: {} [{}]", cronEntity.getCronName(), cronEntity.getCronTime());
			return true;
		}
		catch (Exception e) {
			log.error("Failed to add task: {} - {}", cronEntity.getCronName(), e.getMessage());
			return false;
		}
	}

	/**
	 * Remove scheduled task
	 */
	public boolean removeTask(Long taskId) {
		try {
			ScheduledFuture<?> future = scheduledTasks.remove(taskId);
			if (future != null) {
				future.cancel(false);
				return true;
			}
			return false;
		}
		catch (Exception e) {
			log.error("Failed to remove task: {} - {}", taskId, e.getMessage());
			return false;
		}
	}

	/**
	 * Execute task immediately by task ID
	 */
	public void executeTaskById(Long taskId) {
		CronEntity cronEntity = cronRepository.findById(taskId)
			.orElseThrow(() -> new IllegalArgumentException("Cron task not found: " + taskId));

		log.info("Manually executing scheduled task: {} - {}", cronEntity.getCronName(), cronEntity.getPlanDesc());
		executeTask(cronEntity);
	}

	/**
	 * Get set of currently running task IDs
	 */
	public Set<Long> getRunningTaskIds() {
		return new HashSet<>(scheduledTasks.keySet());
	}

	/**
	 * Execute plan by user query description using PlanningCoordinator
	 * @param planId The plan ID to execute
	 * @param planDesc The plan description/user query
	 */
	private void executePlanByuserQueryDesc(String planId, String planDesc) {
		try {
			log.info("Executing plan by user query description: {} - {}", planId, planDesc);

			// Use PlanningCoordinator to execute the plan by user query
			CompletableFuture<PlanExecutionResult> future = planningCoordinator.executeByUserQuery(planDesc, planId,
					null, planId, null, null);

			// Handle the execution result asynchronously
			future.thenAccept(result -> {
				if (result.isSuccess()) {
					log.info("Plan execution successful for description: {}", planDesc);
				}
				else {
					log.error("Plan execution failed for description: {}: {}", planDesc, result.getErrorMessage());
				}
			}).exceptionally(throwable -> {
				log.error("Plan execution failed for description: {}", planDesc, throwable);
				return null;
			});

		}
		catch (Exception e) {
			log.error("Failed to execute plan by user query description: {}", planId, e);
		}
	}

	/**
	 * Execute plan template using PlanningCoordinator (referenced from
	 * PlanTemplateController)
	 * @param planTemplateId The plan template ID to execute
	 * @param rawParam Raw parameters for execution (can be null)
	 * @param parentPlanId The parent plan ID (can be null for root plans)
	 * @return CompletableFuture with execution result
	 */
	private CompletableFuture<PlanExecutionResult> executePlanTemplateInternal(String planTemplateId, String rawParam,
			String parentPlanId) {
		if (planTemplateId == null || planTemplateId.trim().isEmpty()) {
			log.error("Plan template ID is null or empty");
			PlanExecutionResult errorResult = new PlanExecutionResult();
			errorResult.setSuccess(false);
			errorResult.setErrorMessage("Plan template ID cannot be null or empty");
			return CompletableFuture.completedFuture(errorResult);
		}

		try {
			// Generate a unique plan ID for this execution
			String currentPlanId = planIdDispatcher
				.generateSubPlanId(parentPlanId != null ? parentPlanId : planTemplateId);
			String rootPlanId = parentPlanId != null ? parentPlanId : currentPlanId;

			// Fetch the plan template from PlanTemplateService
			PlanInterface plan = createPlanFromTemplate(planTemplateId, rawParam);

			if (plan == null) {
				PlanExecutionResult errorResult = new PlanExecutionResult();
				errorResult.setSuccess(false);
				errorResult.setErrorMessage("Failed to create plan from template: " + planTemplateId);
				return CompletableFuture.completedFuture(errorResult);
			}

			// Execute using the PlanningCoordinator's common execution logic
			return planningCoordinator.executeByPlan(plan, rootPlanId, parentPlanId, currentPlanId, null, false);

		}
		catch (Exception e) {
			log.error("Failed to execute plan template: {}", planTemplateId, e);
			PlanExecutionResult errorResult = new PlanExecutionResult();
			errorResult.setSuccess(false);
			errorResult.setErrorMessage("Execution failed: " + e.getMessage());
			return CompletableFuture.completedFuture(errorResult);
		}
	}

	/**
	 * Create a plan interface from template ID and parameters. Fetches the plan template
	 * from PlanTemplateService and converts it to PlanInterface.
	 * @param planTemplateId The template ID
	 * @param rawParam Raw parameters
	 * @return PlanInterface object or null if creation fails
	 */
	private PlanInterface createPlanFromTemplate(String planTemplateId, String rawParam) {
		try {
			// Fetch the latest plan version from template service
			String planJson = planTemplateService.getLatestPlanVersion(planTemplateId);

			if (planJson == null) {
				log.error("No plan version found for template: {}", planTemplateId);
				return null;
			}

			// Parse the JSON to create a PlanInterface
			PlanInterface plan = objectMapper.readValue(planJson, PlanInterface.class);

			log.info("Successfully created plan interface from template: {}", planTemplateId);
			return plan;

		}
		catch (Exception e) {
			log.error("Failed to create plan interface from template: {}", planTemplateId, e);
			return null;
		}
	}

}

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
package com.alibaba.cloud.ai.example.manus.dynamic.cron.scheduler;

import com.alibaba.cloud.ai.example.manus.dynamic.cron.entity.CronEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.cron.enums.TaskStatus;
import com.alibaba.cloud.ai.example.manus.dynamic.cron.repository.CronRepository;
import com.alibaba.cloud.ai.example.manus.planning.PlanningFactory;
import com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanIdDispatcher;
import com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanningCoordinator;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.service.PlanTemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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

		ExecutionContext context = new ExecutionContext();
		context.setUserRequest(planDesc);

		String planId = planIdDispatcher.generatePlanId();
		context.setCurrentPlanId(planId);
		context.setRootPlanId(planId);
		context.setNeedSummary(true);

		PlanningCoordinator planningFlow = planningFactory.createPlanningCoordinator(planId);

		// Execute task asynchronously
		CompletableFuture.supplyAsync(() -> {
			try {
				return planningFlow.executePlan(context);
			}
			catch (Exception e) {
				log.error("Plan execution failed: {} - {}", cronEntity.getCronName(), e.getMessage());
				throw new RuntimeException("Plan execution failed: " + e.getMessage(), e);
			}
		});
	}

	/**
	 * Execute plan template
	 * @param planTemplateId Plan template ID
	 */
	private void executePlanTemplate(String planTemplateId) {
		try {
			log.info("Using PlanTemplateController to execute plan template: {}", planTemplateId);

			// Call PlanTemplateController's public method executePlanByTemplateId
			ResponseEntity<Map<String, Object>> response = planTemplateService
				.executePlanByTemplateIdInternal(planTemplateId, null);

			if (response.getStatusCode().is2xxSuccessful()) {
				Map<String, Object> responseBody = response.getBody();
				if (responseBody != null) {
					String planId = (String) responseBody.get("planId");
					log.info("Plan template execution successful, new plan ID: {}", planId);
				}
				else {
					log.warn("Plan template execution successful, but response body is empty");
				}
			}
			else {
				log.error("Plan template execution failed, status code: {}", response.getStatusCode());
			}
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

}

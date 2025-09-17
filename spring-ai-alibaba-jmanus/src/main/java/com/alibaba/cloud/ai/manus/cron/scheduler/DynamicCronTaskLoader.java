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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.alibaba.cloud.ai.manus.cron.entity.CronEntity;
import com.alibaba.cloud.ai.manus.cron.enums.TaskStatus;
import com.alibaba.cloud.ai.manus.cron.repository.CronRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DynamicCronTaskLoader implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(DynamicCronTaskLoader.class);

	private final CronRepository cronRepository;

	private final DynamicCronTaskScheduler taskScheduler;

	@Autowired
	public DynamicCronTaskLoader(CronRepository cronRepository, DynamicCronTaskScheduler taskScheduler) {
		this.cronRepository = cronRepository;
		this.taskScheduler = taskScheduler;
	}

	@Override
	public void run(String... args) throws Exception {
		loadAllEnabledTasks();
		log.info("Scheduled task loading completed");
	}

	@Scheduled(fixedRate = 10000)
	public void syncTasksFromDatabase() {
		try {
			List<CronEntity> dbTasks = cronRepository.findAll();
			Set<Long> runningTaskIds = taskScheduler.getRunningTaskIds();

			for (CronEntity dbTask : dbTasks) {
				boolean isRunning = runningTaskIds.contains(dbTask.getId());
				boolean shouldRun = TaskStatus.ENABLED.getCode().equals(dbTask.getStatus());

				if (shouldRun && !isRunning) {
					taskScheduler.addTask(dbTask);
				}
				else if (!shouldRun && isRunning) {
					taskScheduler.removeTask(dbTask.getId());
				}
			}

			// Check tasks that need to be deleted
			Set<Long> dbTaskIds = dbTasks.stream().map(CronEntity::getId).collect(Collectors.toSet());
			for (Long runningTaskId : runningTaskIds) {
				if (!dbTaskIds.contains(runningTaskId)) {
					taskScheduler.removeTask(runningTaskId);
				}
			}
		}
		catch (Exception e) {
			log.error("Failed to sync scheduled task status: {}", e.getMessage(), e);
		}
	}

	private void loadAllEnabledTasks() {
		try {
			List<CronEntity> enabledTasks = cronRepository.findAll()
				.stream()
				.filter(task -> TaskStatus.ENABLED.getCode().equals(task.getStatus()))
				.toList();

			log.info("Loaded {} enabled scheduled tasks", enabledTasks.size());

			int successCount = 0;
			for (CronEntity task : enabledTasks) {
				if (taskScheduler.addTask(task)) {
					successCount++;
				}
			}

			if (successCount < enabledTasks.size()) {
				log.warn("Some tasks failed to load, successful: {}, total: {}", successCount, enabledTasks.size());
			}
		}
		catch (Exception e) {
			log.error("Failed to load scheduled tasks: {}", e.getMessage(), e);
		}
	}

}

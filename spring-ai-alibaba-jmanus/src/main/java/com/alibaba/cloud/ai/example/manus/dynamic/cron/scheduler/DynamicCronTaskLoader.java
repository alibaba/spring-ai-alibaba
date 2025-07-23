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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
		log.info("定时任务加载完成");
	}

	@Scheduled(fixedRate = 10000)
	public void syncTasksFromDatabase() {
		try {
			List<CronEntity> dbTasks = cronRepository.findAll();
			Set<Long> runningTaskIds = taskScheduler.getRunningTaskIds();

			// 检查需要添加的任务
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

			// 检查需要删除的任务
			Set<Long> dbTaskIds = dbTasks.stream().map(CronEntity::getId).collect(Collectors.toSet());
			for (Long runningTaskId : runningTaskIds) {
				if (!dbTaskIds.contains(runningTaskId)) {
					taskScheduler.removeTask(runningTaskId);
				}
			}
		}
		catch (Exception e) {
			log.error("同步定时任务状态失败: {}", e.getMessage(), e);
		}
	}

	private void loadAllEnabledTasks() {
		try {
			List<CronEntity> enabledTasks = cronRepository.findAll()
				.stream()
				.filter(task -> TaskStatus.ENABLED.getCode().equals(task.getStatus()))
				.toList();

			log.info("加载 {} 个启用的定时任务", enabledTasks.size());

			int successCount = 0;
			for (CronEntity task : enabledTasks) {
				if (taskScheduler.addTask(task)) {
					successCount++;
				}
			}

			if (successCount < enabledTasks.size()) {
				log.warn("部分任务加载失败，成功: {}, 总数: {}", successCount, enabledTasks.size());
			}
		}
		catch (Exception e) {
			log.error("加载定时任务失败: {}", e.getMessage(), e);
		}
	}

}

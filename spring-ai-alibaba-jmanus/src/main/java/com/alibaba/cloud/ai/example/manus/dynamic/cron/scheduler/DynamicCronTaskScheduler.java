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
 * 动态任务调度器
 * 负责管理所有动态定时任务的生命周期
 */
@Component
public class DynamicCronTaskScheduler {

    private static final Logger log = LoggerFactory.getLogger(DynamicCronTaskScheduler.class);

    private final TaskScheduler taskScheduler;
    private final CronRepository cronRepository;
    private final DynamicCronTaskExecutor dynamicCronTaskExecutor;

    // 存储正在运行的任务
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @Autowired
    public DynamicCronTaskScheduler(TaskScheduler taskScheduler,
                                    CronRepository cronRepository,
                                    DynamicCronTaskExecutor dynamicCronTaskExecutor) {
        this.taskScheduler = taskScheduler;
        this.cronRepository = cronRepository;
        this.dynamicCronTaskExecutor = dynamicCronTaskExecutor;
    }

    /**
     * 添加定时任务
     */
    public boolean addTask(CronEntity cronEntity) {
        try {
            if (scheduledTasks.containsKey(cronEntity.getId())) {
                log.warn("Task already exists: {}", cronEntity.getId());
                return false;
            }

            if (!TaskStatus.ENABLED.getCode().equals(cronEntity.getStatus())) {
                log.info("Task is not enabled, skip adding: {}", cronEntity.getId());
                return false;
            }

            ScheduledFuture<?> future = taskScheduler.schedule(
                () -> executeTask(cronEntity),
                new CronTrigger(cronEntity.getCronTime())
            );

            scheduledTasks.put(cronEntity.getId(), future);
            log.info("Successfully added task: {} with cron: {}",
                    cronEntity.getCronName(), cronEntity.getCronTime());
            return true;
        } catch (Exception e) {
            log.error("Failed to add task: {}, error: {}", cronEntity.getId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 移除定时任务
     */
    public boolean removeTask(Long taskId) {
        try {
            ScheduledFuture<?> future = scheduledTasks.remove(taskId);
            if (future != null) {
                future.cancel(false);
                log.info("Successfully removed task: {}", taskId);
                return true;
            } else {
                log.warn("Task not found for removal: {}", taskId);
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to remove task: {}, error: {}", taskId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 更新定时任务
     */
    public boolean updateTask(CronEntity cronEntity) {
        // 先移除旧任务
        removeTask(cronEntity.getId());
        // 再添加新任务
        return addTask(cronEntity);
    }

    /**
     * 启用任务
     */
    public boolean enableTask(Long taskId) {
        try {
            CronEntity entity = cronRepository.findById(taskId).orElse(null);
            if (entity == null) {
                log.warn("Task not found: {}", taskId);
                return false;
            }

            entity.setStatus(TaskStatus.ENABLED.getCode());
            cronRepository.save(entity);

            return addTask(entity);
        } catch (Exception e) {
            log.error("Failed to enable task: {}, error: {}", taskId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 禁用任务
     */
    public boolean disableTask(Long taskId) {
        try {
            CronEntity entity = cronRepository.findById(taskId).orElse(null);
            if (entity == null) {
                log.warn("Task not found: {}", taskId);
                return false;
            }

            entity.setStatus(TaskStatus.DISABLED.getCode());
            cronRepository.save(entity);

            return removeTask(taskId);
        } catch (Exception e) {
            log.error("Failed to disable task: {}, error: {}", taskId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 执行任务
     */
    private void executeTask(CronEntity cronEntity) {
        try {
            log.info("Executing task: {} - {}", cronEntity.getCronName(), cronEntity.getPlanDesc());

            // 更新任务状态为运行中
            cronEntity.setStatus(TaskStatus.RUNNING.getCode());
            cronEntity.setLastExecutedTime(LocalDateTime.now());
            cronRepository.save(cronEntity);

            // 执行具体任务
            dynamicCronTaskExecutor.execute(cronEntity);

            // 任务执行完成后，恢复为启用状态
            cronEntity.setStatus(TaskStatus.ENABLED.getCode());
            cronRepository.save(cronEntity);

            log.info("Task executed successfully: {}", cronEntity.getCronName());
        } catch (Exception e) {
            log.error("Task execution failed: {}, error: {}", cronEntity.getCronName(), e.getMessage(), e);

            // 执行失败后，恢复为启用状态
            cronEntity.setStatus(TaskStatus.ENABLED.getCode());
            cronRepository.save(cronEntity);
        }
    }

    /**
     * 获取当前运行的任务数量
     */
    public int getRunningTaskCount() {
        return scheduledTasks.size();
    }

    /**
     * 检查任务是否正在运行
     */
    public boolean isTaskRunning(Long taskId) {
        return scheduledTasks.containsKey(taskId);
    }

    /**
     * 获取当前运行的任务ID集合
     */
    public Set<Long> getRunningTaskIds() {
        return new HashSet<>(scheduledTasks.keySet());
    }

    /**
     * 停止所有任务
     */
    public void stopAllTasks() {
        scheduledTasks.forEach((taskId, future) -> {
            try {
                future.cancel(false);
                log.info("Stopped task: {}", taskId);
            } catch (Exception e) {
                log.error("Failed to stop task: {}, error: {}", taskId, e.getMessage());
            }
        });
        scheduledTasks.clear();
        log.info("All tasks stopped");
    }
}


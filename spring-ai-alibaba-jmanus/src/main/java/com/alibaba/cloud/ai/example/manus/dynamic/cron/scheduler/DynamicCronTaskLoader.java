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
        log.info("开始加载数据库中的定时任务...");
        loadAllEnabledTasks();
        log.info("定时任务加载完成");
    }

    @Scheduled(fixedRate = 10000)
    public void syncTasksFromDatabase() {
        try {
            log.debug("开始同步数据库中的定时任务状态...");

            List<CronEntity> dbTasks = cronRepository.findAll();
            Set<Long> runningTaskIds = taskScheduler.getRunningTaskIds();

            for (CronEntity dbTask : dbTasks) {
                boolean isRunning = runningTaskIds.contains(dbTask.getId());
                boolean shouldRun = TaskStatus.ENABLED.getCode().equals(dbTask.getStatus());

                if (shouldRun && !isRunning) {
                    log.info("发现新的启用任务，添加到调度器: {}", dbTask.getCronName());
                    taskScheduler.addTask(dbTask);
                } else if (!shouldRun && isRunning) {
                    log.info("发现已禁用任务，从调度器移除: {}", dbTask.getCronName());
                    taskScheduler.removeTask(dbTask.getId());
                }
            }

            Set<Long> dbTaskIds = dbTasks.stream().map(CronEntity::getId).collect(Collectors.toSet());
            for (Long runningTaskId : runningTaskIds) {
                if (!dbTaskIds.contains(runningTaskId)) {
                    log.info("发现已删除任务，从调度器移除: {}", runningTaskId);
                    taskScheduler.removeTask(runningTaskId);
                }
            }

            log.debug("定时任务状态同步完成");
        } catch (Exception e) {
            log.error("同步定时任务状态时发生错误: {}", e.getMessage(), e);
        }
    }

    private void loadAllEnabledTasks() {
        try {
            List<CronEntity> enabledTasks = cronRepository.findAll().stream()
                .filter(task -> TaskStatus.ENABLED.getCode().equals(task.getStatus()))
                .toList();

            log.info("找到 {} 个启用的定时任务", enabledTasks.size());

            for (CronEntity task : enabledTasks) {
                boolean success = taskScheduler.addTask(task);
                if (success) {
                    log.info("成功加载定时任务: {} - {}", task.getCronName(), task.getCronTime());
                } else {
                    log.warn("加载定时任务失败: {}", task.getCronName());
                }
            }
        } catch (Exception e) {
            log.error("加载定时任务时发生错误: {}", e.getMessage(), e);
        }
    }

    public void refreshAllTasks() {
        log.info("手动刷新所有定时任务...");
        taskScheduler.stopAllTasks();
        loadAllEnabledTasks();
        log.info("所有定时任务刷新完成");
    }
}


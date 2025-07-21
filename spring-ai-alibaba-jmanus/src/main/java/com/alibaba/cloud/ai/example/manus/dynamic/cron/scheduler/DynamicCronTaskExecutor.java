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
import com.alibaba.cloud.ai.example.manus.planning.PlanningFactory;
import com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanIdDispatcher;
import com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanningCoordinator;
import com.alibaba.cloud.ai.example.manus.planning.executor.PlanExecutorInterface;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 任务执行器
 * 负责执行具体的定时任务逻辑
 */
@Component
public class DynamicCronTaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(DynamicCronTaskExecutor.class);

    @Autowired
    private PlanIdDispatcher planIdDispatcher;

    @Autowired
    private PlanningFactory planningFactory;


    /**
     * 根据任务描述执行相应的计划
     *
     * @param cronEntity 任务实体
     */
    public void execute(CronEntity cronEntity) {
        String planDesc = cronEntity.getPlanDesc();

        ExecutionContext context = new ExecutionContext();
        context.setUserRequest(planDesc);
        // Use PlanIdDispatcher to generate a unique plan ID
        String planId = planIdDispatcher.generatePlanId();
        context.setCurrentPlanId(planId);
        context.setRootPlanId(planId);
        context.setNeedSummary(true);
        // Get or create planning flow
        PlanningCoordinator planningFlow = planningFactory.createPlanningCoordinator(planId);

        // Asynchronous execution of task
        CompletableFuture.supplyAsync(() -> {
            try {
                return planningFlow.executePlan(context);
            } catch (Exception e) {
                log.error("Failed to execute plan", e);
                throw new RuntimeException("Failed to execute plan: " + e.getMessage(), e);
            }
        });
    }
}


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
package com.alibaba.cloud.ai.example.manus.runtime.service;

import com.alibaba.cloud.ai.example.manus.planning.executor.factory.PlanExecutorFactory;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.PlanInterface;
import com.alibaba.cloud.ai.example.manus.runtime.task.PlanTask;
import com.alibaba.cloud.ai.example.manus.runtime.task.TaskManager;
import com.alibaba.cloud.ai.example.manus.runtime.vo.PlanExecutionResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * 计划执行服务，负责管理计划执行和任务提交
 * 提供高级API来简化计划任务的创建、提交和监控
 */
@Service
public class PlanExecutionService {

    private static final Logger log = LoggerFactory.getLogger(PlanExecutionService.class);

    @Autowired
    private TaskManager taskManager;

    @Autowired
    private PlanExecutorFactory planExecutorFactory;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 提交单个新计划任务（基于用户请求创建新计划）
     * @param planId 计划ID
     * @param userRequest 用户请求
     * @return 任务执行结果的CompletableFuture
     */
    public CompletableFuture<String> submitSinglePlanByUserRequest(String planId, String userRequest) {
        log.info("提交新计划任务: planId={}, userRequest={}", planId, userRequest);
        
        Function<String, PlanTask> taskFactory = createTaskFactory(userRequest);
        Collection<String> planIds = Collections.singleton(planId);
        
        return taskManager.scheduleChildren(planIds, taskFactory)
            .thenApply(resultsMap -> {
                PlanExecutionResult executionResult = resultsMap.get(planId);
                String result = executionResult != null ? executionResult.getEffectiveResult() : "";
                log.info("计划任务完成: planId={}, result={}", planId, result);
                return result != null ? result : "";
            })
            .exceptionally(throwable -> {
                log.error("计划任务执行失败: planId={}", planId, throwable);
                return "任务执行失败: " + throwable.getMessage();
            });
    }

    /**
     * 提交单个计划任务（基于现有计划模板）
     * @param planId 计划ID
     * @param planTemplateId 计划模板ID
     * @param planJson 计划JSON内容
     * @return 任务执行结果的CompletableFuture
     */
    public CompletableFuture<PlanExecutionResult> submitSinglePlanByPlanTemplate(String planId, String planTemplateId, String planJson) {
        log.info("提交计划模板任务: planId={}, planTemplateId={}", planId, planTemplateId);
        
        Function<String, PlanTask> taskFactory = createTaskFactoryByPlanTemplate(planId, planJson);
        Collection<String> planIds = Collections.singleton(planId);
        
        return taskManager.scheduleChildren(planIds, taskFactory)
            .thenApply(resultsMap -> {
                PlanExecutionResult executionResult = resultsMap.get(planId);
                log.info("计划模板任务完成: planId={}, executionResult={}", planId, executionResult);
                return executionResult != null ? executionResult : new PlanExecutionResult();
            })
            .exceptionally(throwable -> {
                log.error("计划模板任务执行失败: planId={}", planId, throwable);
                
                // Create error PlanExecutionResult
                PlanExecutionResult errorResult = new PlanExecutionResult();
                errorResult.setSuccess(false);
                errorResult.setErrorMessage("任务执行失败: " + throwable.getMessage());
                
                return errorResult;
            });
    }

    /**
     * 提交多个计划任务
     * @param planIds 计划ID集合
     * @param userRequest 用户请求
     * @return 所有任务执行结果的CompletableFuture
     */
    public CompletableFuture<Map<String, PlanExecutionResult>> submitMultiplePlans(Collection<String> planIds, String userRequest) {
        log.info("提交多个计划任务: planIds={}, userRequest={}", planIds, userRequest);
        
        Function<String, PlanTask> taskFactory = createTaskFactory(userRequest);
        
        return taskManager.scheduleChildren(planIds, taskFactory)
            .thenApply(resultsMap -> {
                log.info("多个计划任务完成: planIds={}, resultsMap={}", planIds, resultsMap);
                return resultsMap; // TaskManager.scheduleChildren now returns Map<String, PlanExecutionResult> directly
            })
            .exceptionally(throwable -> {
                log.error("多个计划任务执行失败: planIds={}", planIds, throwable);
                return Collections.emptyMap();
            });
    }

    /**
     * 处理父任务的子计划
     * @param parentPlanId 父计划ID
     * @param childPlanIds 子计划ID集合
     * @param userRequest 用户请求
     * @return 聚合结果的CompletableFuture
     */
    public CompletableFuture<String> handleSubPlansForParent(String parentPlanId, Collection<String> childPlanIds, String userRequest) {
        log.info("处理父任务的子计划: parentPlanId={}, childPlanIds={}", parentPlanId, childPlanIds);
        
        Function<String, PlanTask> taskFactory = createTaskFactory(userRequest);
        
        return taskManager.handleSubPlansForParent(parentPlanId, childPlanIds, taskFactory)
            .toCompletableFuture()
            .thenApply(aggregatedResult -> {
                log.info("子计划处理完成: parentPlanId={}, aggregatedResult={}", parentPlanId, aggregatedResult);
                return aggregatedResult;
            })
            .exceptionally(throwable -> {
                log.error("子计划处理失败: parentPlanId={}", parentPlanId, throwable);
                return "子计划处理失败: " + throwable.getMessage();
            });
    }

    /**
     * 非阻塞编排：暂停父任务，调度子任务，修补内存，然后恢复
     * @param parentPlanId 父计划ID
     * @param childPlanIds 子计划ID集合
     * @param userRequest 用户请求
     * @return 编排完成的CompletableFuture
     */
    public CompletableFuture<Void> orchestrateChildrenExecution(String parentPlanId, Collection<String> childPlanIds, String userRequest) {
        log.info("编排子任务执行: parentPlanId={}, childPlanIds={}", parentPlanId, childPlanIds);
        
        Function<String, PlanTask> taskFactory = createTaskFactory(userRequest);
        
        return taskManager.scheduleChildrenPatchAndResumeByPlanId(parentPlanId, childPlanIds, taskFactory)
            .toCompletableFuture()
            .thenRun(() -> {
                log.info("子任务编排完成: parentPlanId={}", parentPlanId);
            })
            .exceptionally(throwable -> {
                log.error("子任务编排失败: parentPlanId={}", parentPlanId, throwable);
                return null;
            });
    }

    /**
     * 获取已注册的任务
     * @param planId 计划ID
     * @return 已注册的PlanTask，如果不存在则返回null
     */
    public PlanTask getRegisteredTask(String planId) {
        return taskManager.getRegisteredTask(planId);
    }

    /**
     * 创建任务工厂函数（基于用户请求）
     * @param userRequest 用户请求
     * @return 任务工厂函数
     */
    private Function<String, PlanTask> createTaskFactory(String userRequest) {
        return (planId) -> createPlanTask(planId, userRequest, null);
    }

    /**
     * 创建任务工厂函数（基于计划模板）
     * @param planId 计划ID
     * @param planJson 计划JSON内容
     * @return 任务工厂函数
     */
    private Function<String, PlanTask> createTaskFactoryByPlanTemplate(String planId, String planJson) {
        return (planIdParam) -> createPlanTaskByPlanTemplate(planId, planJson, null);
    }

    /**
     * 创建计划任务（基于用户请求）
     * @param planId 计划ID
     * @param userRequest 用户请求
     * @param parentPlanId 父计划ID，可以为null
     * @return 创建的PlanTask实例
     */
    private PlanTask createPlanTask(String planId, String userRequest, String parentPlanId) {
        ExecutionContext context = new ExecutionContext();
        context.setCurrentPlanId(planId);
        context.setRootPlanId(planId);
        context.setUserRequest(userRequest);
        context.setNeedSummary(true);
        context.setUseMemory(false); // 默认不使用内存
        
        return new PlanTask(context, parentPlanId, planExecutorFactory);
    }

    /**
     * 创建计划任务（基于计划模板）
     * @param planId 计划ID
     * @param planJson 计划JSON内容
     * @param parentPlanId 父计划ID，可以为null
     * @return 创建的PlanTask实例
     */
    private PlanTask createPlanTaskByPlanTemplate(String planId, String planJson, String parentPlanId) {
        try {
            ExecutionContext context = new ExecutionContext();
            context.setCurrentPlanId(planId);
            context.setRootPlanId(planId);
            context.setUserRequest("执行计划模板: " + planId);
            context.setNeedSummary(true);
            context.setUseMemory(false); // 默认不使用内存
            
            // 解析计划JSON并设置到context中
            if (planJson != null && !planJson.trim().isEmpty()) {
                PlanInterface plan = objectMapper.readValue(planJson, PlanInterface.class);
                // 设置新的计划ID，覆盖JSON中的ID
                plan.setCurrentPlanId(planId);
                plan.setRootPlanId(planId);
                context.setPlan(plan);
            }
            
            return new PlanTask(context, parentPlanId, planExecutorFactory);
        } catch (Exception e) {
            log.error("Failed to create plan task from template: planId={}, planJson={}", planId, planJson, e);
            throw new RuntimeException("Failed to create plan task from template", e);
        }
    }
}

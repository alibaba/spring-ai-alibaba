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
package com.alibaba.cloud.ai.example.manus.planning.executor;

import com.alibaba.cloud.ai.example.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.example.manus.flow.PlanStepStatus;
import com.alibaba.cloud.ai.example.manus.planning.model.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.model.ExecutionPlan;
import com.alibaba.cloud.ai.example.manus.planning.model.ExecutionStep;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.recorder.entity.PlanExecutionRecord;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 负责执行计划的类
 */
public class PlanExecutor {

    private static final String EXECUTION_ENV_KEY_STRING = "current_step_env_data";
    private static final Logger logger = LoggerFactory.getLogger(PlanExecutor.class);

    protected final PlanExecutionRecorder recorder;
    Pattern pattern = Pattern.compile("\\[([A-Z_]+)\\]");

    private final List<BaseAgent> agents;

    public PlanExecutor(List<BaseAgent> agents, PlanExecutionRecorder recorder) {
        this.agents = agents;
        this.recorder = recorder;
    }

    /**
     * 执行整个计划的所有步骤
     * 
     * @param plan 要执行的计划
     * @return 执行结果
     */
    public void executeAllSteps(ExecutionContext context) {
        recordPlanExecutionStart(context);
        ExecutionPlan plan = context.getPlan();
        List<ExecutionStep> steps = plan.getSteps();

        for (ExecutionStep step : steps) {
            executeStep(step, context);
        }
        context.setSuccess(true);
    }

    /**
     * 执行单个步骤
     * 
     * @param executor 执行器
     * @param stepInfo 步骤信息
     * @return 步骤执行结果
     */
    private void executeStep(ExecutionStep step, ExecutionContext context) {

        String stepType = getStepFromStepReq(step.getStepRequirement());
        BaseAgent executor = getExecutorForStep(stepType);
        int stepIndex = step.getStepIndex();
        recordStepStart(step, context);

        try {
            String planStatus = context.getPlan().getPlanExecutionStateStringFormat();

            String stepText = step.getStepRequirement();
            Map<String, Object> executorParams = new HashMap<>();
            executorParams.put("planStatus", planStatus);
            executorParams.put("currentStepIndex", String.valueOf(stepIndex));
            executorParams.put("stepText", stepText);
            executorParams.put(EXECUTION_ENV_KEY_STRING, "");
            String stepResultStr = executor.run(executorParams);
            // Execute the step
            step.setResult(stepResultStr);
            step.setStatus(PlanStepStatus.COMPLETED);
        } catch (Exception e) {
            logger.error("Error executing step: " + e.getMessage());
            step.setStatus(PlanStepStatus.FAILED);
            step.setResult("Execution failed: " + e.getMessage());
        }

    }

    private String getStepFromStepReq(String stepRequirement) {
        Matcher matcher = pattern.matcher(stepRequirement);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "DEFAULT_AGENT"; // Default agent if no match found
    }

    /**
     * 获取步骤的执行器
     * 
     * @param stepType 步骤类型
     * @return 对应的执行器
     */
    private BaseAgent getExecutorForStep(String stepType) {
        // 根据步骤类型获取对应的执行器
        for (BaseAgent agent : agents) {
            if (agent.getName().equalsIgnoreCase(stepType)) {
                return agent;
            }
        }
        throw new IllegalArgumentException(
                "No Agent Executor found for step type, check your agents list : " + stepType);
    }

    protected PlanExecutionRecorder getRecorder() {
        return recorder;
    }

    private void recordPlanExecutionStart(ExecutionContext context) {
        PlanExecutionRecord record = getOrCreatePlanExecutionRecord(context);
       
        record.setPlanId(context.getPlan().getPlanId());
        record.setStartTime(LocalDateTime.now());
        record.setTitle(context.getPlan().getTitle());
        record.setUserRequest(context.getUserRequest());
        List<String> steps = new ArrayList<>();
        for (ExecutionStep step : context.getPlan().getSteps()) {
            steps.add(step.getStepInStr());
        }
        record.setSteps(steps);
        getRecorder().recordPlanExecution(record);
    }

    /**
     * Initialize the plan execution record
     */
    private PlanExecutionRecord getOrCreatePlanExecutionRecord(ExecutionContext context) {
        PlanExecutionRecord record = getRecorder().getExecutionRecord(context.getPlan().getPlanId());
        if (record == null) {
            record = new PlanExecutionRecord();
        }
        getRecorder().recordPlanExecution(record);
        return record;
    }


	private void recordStepStart(ExecutionStep step, ExecutionContext context) {
		// 更新 PlanExecutionRecord 中的当前步骤索引
        PlanExecutionRecord record = getOrCreatePlanExecutionRecord(context);
        if (record != null) {
            int currentStepIndex = step.getStepIndex();
            record.setCurrentStepIndex(currentStepIndex);
            getRecorder().recordPlanExecution(record);
        }
	}

}

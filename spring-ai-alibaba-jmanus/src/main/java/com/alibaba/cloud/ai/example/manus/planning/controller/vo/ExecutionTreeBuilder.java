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
package com.alibaba.cloud.ai.example.manus.planning.controller.vo;

import com.alibaba.cloud.ai.example.manus.recorder.entity.PlanExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.AgentExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.ExecutionStatus;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for building execution tree responses from PlanExecutionRecord entities.
 * Provides strongly-typed conversion from domain entities to VO objects.
 * 
 * @author JManus Team
 * @since 1.0.0
 */
public class ExecutionTreeBuilder {

    /**
     * Build the complete execution tree response from a root plan record
     * 
     * @param rootRecord The root plan execution record
     * @return The execution tree response
     */
    public static ExecutionTreeResponse buildTreeResponse(PlanExecutionRecord rootRecord) {
        if (rootRecord == null) {
            throw new IllegalArgumentException("Root record cannot be null");
        }

        ExecutionTreeNode treeNode = buildTreeNode(rootRecord);
        return new ExecutionTreeResponse(rootRecord.getCurrentPlanId(), treeNode);
    }

    /**
     * Build a tree node from a plan execution record
     * 
     * @param record The plan execution record
     * @return The execution tree node
     */
    private static ExecutionTreeNode buildTreeNode(PlanExecutionRecord record) {
        // Build steps from agent execution sequence
        List<ExecutionStepInfo> steps = buildSteps(record.getAgentExecutionSequence());
        
        // Create children list (currently empty, extensible for future use)
        List<ExecutionTreeNode> children = new ArrayList<>();

        return new ExecutionTreeNode(
            record.getCurrentPlanId(),
            record.getTitle(),
            deriveStatus(record),
            calculateProgress(record),
            record.getStartTime(),
            record.getEndTime(),
            record.getUserRequest(),
            steps,
            children
        );
    }

    /**
     * Build steps array from agent execution sequence
     * 
     * @param agentExecutions Agent execution records
     * @return List of step info objects
     */
    private static List<ExecutionStepInfo> buildSteps(List<AgentExecutionRecord> agentExecutions) {
        if (agentExecutions == null || agentExecutions.isEmpty()) {
            return new ArrayList<>();
        }

        List<ExecutionStepInfo> steps = new ArrayList<>();
        for (int i = 0; i < agentExecutions.size(); i++) {
            AgentExecutionRecord agentExecution = agentExecutions.get(i);
            ExecutionStepInfo stepInfo = buildStepInfo(i, agentExecution);
            steps.add(stepInfo);
        }
        return steps;
    }

    /**
     * Build step info from agent execution record
     * 
     * @param stepIndex Step index
     * @param agentExecution Agent execution record
     * @return Step info object
     */
    private static ExecutionStepInfo buildStepInfo(int stepIndex, AgentExecutionRecord agentExecution) {
        String stepDescription = generateStepDescription(agentExecution, stepIndex);
        
        return new ExecutionStepInfo(
            stepIndex, 
            stepDescription, 
            agentExecution.getId(),
            agentExecution.getAgentName(),
            agentExecution.getAgentDescription(),
            agentExecution.getStatus(),
            agentExecution.getStartTime(),
            agentExecution.getEndTime(),
            agentExecution.getCurrentStep(),
            agentExecution.getMaxSteps()
        );
    }

    /**
     * Derive status from plan execution record
     * 
     * @param record Plan execution record
     * @return Plan execution status
     */
    private static PlanExecutionStatus deriveStatus(PlanExecutionRecord record) {
        if (record.isCompleted()) {
            return PlanExecutionStatus.COMPLETED;
        } else if (record.getStartTime() != null) {
            return PlanExecutionStatus.RUNNING;
        } else {
            return PlanExecutionStatus.PENDING;
        }
    }

    /**
     * Calculate progress percentage
     * 
     * @param record Plan execution record
     * @return Progress percentage (0-100)
     */
    private static int calculateProgress(PlanExecutionRecord record) {
        if (record.isCompleted()) {
            return 100;
        }

        List<AgentExecutionRecord> agentExecutions = record.getAgentExecutionSequence();
        if (agentExecutions == null || agentExecutions.isEmpty()) {
            return 0;
        }

        int totalSteps = agentExecutions.size();
        int completedSteps = 0;

        for (AgentExecutionRecord agentExecution : agentExecutions) {
            if (agentExecution.getStatus() == ExecutionStatus.FINISHED) {
                completedSteps++;
            }
        }

        return totalSteps > 0 ? (completedSteps * 100) / totalSteps : 0;
    }

    /**
     * Generate step description
     * 
     * @param agentExecution Agent execution record
     * @param stepIndex Step index
     * @return Step description
     */
    private static String generateStepDescription(AgentExecutionRecord agentExecution, int stepIndex) {
        String agentName = agentExecution.getAgentName();
        if (StringUtils.hasText(agentName)) {
            return String.format("Step %d: Execute %s", stepIndex + 1, agentName);
        } else {
            return String.format("Step %d: Execute agent", stepIndex + 1);
        }
    }
}

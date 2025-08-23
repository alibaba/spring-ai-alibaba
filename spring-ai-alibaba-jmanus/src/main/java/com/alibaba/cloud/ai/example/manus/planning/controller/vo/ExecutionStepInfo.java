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

import com.alibaba.cloud.ai.example.manus.recorder.entity.vo.ExecutionStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Represents information about a single execution step in the plan.
 * Contains step metadata and all agent execution details merged into one class.
 * This consolidates the previous ExecutionStepInfo and AgentExecutionInfo classes.
 * 
 * @author JManus Team
 * @since 1.0.0
 */
public class ExecutionStepInfo {

    /**
     * The index/position of this step in the execution sequence
     */
    @JsonProperty("stepIndex")
    private int stepIndex;

    /**
     * Human-readable description of what this step does
     */
    @JsonProperty("stepDescription")
    private String stepDescription;

    /**
     * Unique identifier for this agent execution
     */
    @JsonProperty("id")
    private Long id;

    /**
     * Human-readable name of the agent
     */
    @JsonProperty("agentName")
    private String agentName;

    /**
     * Detailed description of what the agent does
     */
    @JsonProperty("agentDescription")
    private String agentDescription;

    /**
     * Current execution status of the agent
     */
    @JsonProperty("status")
    private ExecutionStatus status;

    /**
     * When the agent execution started
     */
    @JsonProperty("startTime")
    private LocalDateTime startTime;

    /**
     * When the agent execution completed (null if still running)
     */
    @JsonProperty("endTime")
    private LocalDateTime endTime;

    /**
     * Current step index within the agent's execution sequence
     */
    @JsonProperty("currentStep")
    private Integer currentStep;

    /**
     * Total number of steps the agent needs to complete
     */
    @JsonProperty("maxSteps")
    private Integer maxSteps;

    /**
     * Default constructor for Jackson deserialization
     */
    public ExecutionStepInfo() {
    }

    /**
     * Constructor with required fields
     * 
     * @param stepIndex The step index
     * @param stepDescription The step description
     * @param id The agent execution ID
     * @param agentName The agent name
     * @param agentDescription The agent description
     * @param status The execution status
     * @param startTime When execution started
     * @param endTime When execution ended
     * @param currentStep The current step index
     * @param maxSteps The total number of steps
     */
    public ExecutionStepInfo(int stepIndex, String stepDescription, Long id, String agentName, 
                           String agentDescription, ExecutionStatus status, LocalDateTime startTime, 
                           LocalDateTime endTime, Integer currentStep, Integer maxSteps) {
        this.stepIndex = stepIndex;
        this.stepDescription = stepDescription;
        this.id = id;
        this.agentName = agentName;
        this.agentDescription = agentDescription;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.currentStep = currentStep;
        this.maxSteps = maxSteps;
    }

    // Getters and Setters
    public int getStepIndex() {
        return stepIndex;
    }

    public void setStepIndex(int stepIndex) {
        this.stepIndex = stepIndex;
    }

    public String getStepDescription() {
        return stepDescription;
    }

    public void setStepDescription(String stepDescription) {
        this.stepDescription = stepDescription;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getAgentDescription() {
        return agentDescription;
    }

    public void setAgentDescription(String agentDescription) {
        this.agentDescription = agentDescription;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(Integer currentStep) {
        this.currentStep = currentStep;
    }

    public Integer getMaxSteps() {
        return maxSteps;
    }

    public void setMaxSteps(Integer maxSteps) {
        this.maxSteps = maxSteps;
    }

    @Override
    public String toString() {
        return "ExecutionStepInfo{" +
                "stepIndex=" + stepIndex +
                ", stepDescription='" + stepDescription + '\'' +
                ", id=" + id +
                ", agentName='" + agentName + '\'' +
                ", agentDescription='" + agentDescription + '\'' +
                ", status=" + status +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", currentStep=" + currentStep +
                ", maxSteps=" + maxSteps +
                '}';
    }
}

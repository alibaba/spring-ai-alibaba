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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a node in the execution tree structure.
 * Contains plan information, status, progress, and execution steps.
 * 
 * @author JManus Team
 * @since 1.0.0
 */
public class ExecutionTreeNode {

    /**
     * The current plan ID for this node
     */
    @JsonProperty("currentPlanId")
    private String currentPlanId;

    /**
     * Human-readable title/description of the plan
     */
    @JsonProperty("title")
    private String title;

    /**
     * Current execution status of the plan
     */
    @JsonProperty("status")
    private PlanExecutionStatus status;

    /**
     * Execution progress as a percentage (0-100)
     */
    @JsonProperty("progress")
    private int progress;

    /**
     * When the plan execution started
     */
    @JsonProperty("startTime")
    private LocalDateTime startTime;

    /**
     * When the plan execution completed (null if still running)
     */
    @JsonProperty("endTime")
    private LocalDateTime endTime;

    /**
     * The original user request that triggered this plan
     */
    @JsonProperty("userRequest")
    private String userRequest;

    /**
     * List of execution steps for this plan
     */
    @JsonProperty("steps")
    private List<ExecutionStepInfo> steps;

    /**
     * Child plans/sub-plans (currently empty, extensible for future use)
     */
    @JsonProperty("children")
    private List<ExecutionTreeNode> children;

    /**
     * Default constructor for Jackson deserialization
     */
    public ExecutionTreeNode() {
    }

    /**
     * Constructor with required fields
     * 
     * @param currentPlanId The current plan ID
     * @param title The plan title
     * @param status The execution status
     * @param progress The progress percentage
     * @param startTime When execution started
     * @param endTime When execution ended
     * @param userRequest The user request
     * @param steps The execution steps
     * @param children The child nodes
     */
    public ExecutionTreeNode(String currentPlanId, String title, PlanExecutionStatus status, int progress,
                           LocalDateTime startTime, LocalDateTime endTime, String userRequest,
                           List<ExecutionStepInfo> steps, List<ExecutionTreeNode> children) {
        this.currentPlanId = currentPlanId;
        this.title = title;
        this.status = status;
        this.progress = progress;
        this.startTime = startTime;
        this.endTime = endTime;
        this.userRequest = userRequest;
        this.steps = steps;
        this.children = children;
    }

    // Getters and Setters
    public String getCurrentPlanId() {
        return currentPlanId;
    }

    public void setCurrentPlanId(String currentPlanId) {
        this.currentPlanId = currentPlanId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public PlanExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(PlanExecutionStatus status) {
        this.status = status;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
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

    public String getUserRequest() {
        return userRequest;
    }

    public void setUserRequest(String userRequest) {
        this.userRequest = userRequest;
    }

    public List<ExecutionStepInfo> getSteps() {
        return steps;
    }

    public void setSteps(List<ExecutionStepInfo> steps) {
        this.steps = steps;
    }

    public List<ExecutionTreeNode> getChildren() {
        return children;
    }

    public void setChildren(List<ExecutionTreeNode> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return "ExecutionTreeNode{" +
                "currentPlanId='" + currentPlanId + '\'' +
                ", title='" + title + '\'' +
                ", status='" + status + '\'' +
                ", progress=" + progress +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", userRequest='" + userRequest + '\'' +
                ", steps=" + steps +
                ", children=" + children +
                '}';
    }
}

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

package com.alibaba.cloud.ai.manus.recorder.entity.po;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Plan execution record class for tracking and recording detailed information about
 * PlanningFlow execution process.
 *
 * Data structure is divided into four main parts:
 *
 * 1. Basic Info - id: Unique identifier of the record - currentPlanId: Unique identifier
 * of the plan - title: Plan title - startTime: Execution start time - endTime: Execution
 * end time - userRequest: User's original request
 *
 * 2. Plan Structure - steps: Plan step list - currentStepIndex: Current step index being
 * executed
 *
 * 3. Execution Data - agentExecutionSequence: Records of execution for each smart agent
 *
 * 4. Execution Result - completed: Whether completed - summary: Execution summary
 */
@Entity
@Table(name = "plan_execution_record")
public class PlanExecutionRecordEntity {

	// Unique identifier for the record
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Unique identifier for the current plan
	@Column(name = "current_plan_id", nullable = false, unique = true)
	private String currentPlanId;

	// Root plan ID for sub-plans (null for main plans)
	@Column(name = "root_plan_id")
	private String rootPlanId;

	// Parent plan ID for sub-plans (null for root plans)
	@Column(name = "parent_plan_id")
	private String parentPlanId;

	// Plan title
	@Column(name = "title")
	private String title;

	// User's original request
	@Column(name = "user_request", columnDefinition = "LONGTEXT")
	private String userRequest;

	// Timestamp when execution started
	@Column(name = "start_time")
	private LocalDateTime startTime;

	// Timestamp when execution ended
	@Column(name = "end_time")
	private LocalDateTime endTime;

	// List of plan steps
	@ElementCollection
	@CollectionTable(name = "plan_execution_steps", joinColumns = @JoinColumn(name = "plan_execution_id"))
	@Column(name = "step", columnDefinition = "LONGTEXT")
	private List<String> steps;

	// Current step index being executed
	@Column(name = "current_step_index")
	private Integer currentStepIndex;

	// Whether completed
	@Column(name = "completed")
	private boolean completed;

	// Execution summary
	@Column(name = "summary", columnDefinition = "LONGTEXT")
	private String summary;

	// List to maintain the sequence of agent executions
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "plan_execution_id")
	private List<AgentExecutionRecordEntity> agentExecutionSequence;

	// Tool call ID that triggered this plan (for sub-plans)
	@Column(name = "tool_call_id")
	private String toolCallId;

	// Actual calling model
	@Column(name = "model_name")
	private String modelName;

	/**
	 * Default constructor for Jackson and other frameworks.
	 */
	public PlanExecutionRecordEntity() {
		this.steps = new ArrayList<>();
		this.completed = false;
		this.agentExecutionSequence = new ArrayList<>();
	}

	/**
	 * Constructor for creating a new execution record
	 * @param currentPlanId The unique identifier for the current plan.
	 */
	public PlanExecutionRecordEntity(String currentPlanId) {
		this.currentPlanId = currentPlanId;
		this.steps = new ArrayList<>();
		this.startTime = LocalDateTime.now();
		this.completed = false;
		this.agentExecutionSequence = new ArrayList<>();
	}

	/**
	 * Add an execution step
	 * @param step Step description
	 */
	public void addStep(String step) {
		this.steps.add(step);
	}

	/**
	 * Add agent execution record
	 * @param record Execution record
	 */
	public void addAgentExecutionRecord(AgentExecutionRecordEntity record) {
		this.agentExecutionSequence.add(record);
	}

	/**
	 * Get agent execution records sorted by execution order
	 * @return List of execution records
	 */
	public List<AgentExecutionRecordEntity> getAgentExecutionSequence() {
		return agentExecutionSequence;
	}

	public void setAgentExecutionSequence(List<AgentExecutionRecordEntity> agentExecutionSequence) {
		this.agentExecutionSequence = agentExecutionSequence;
	}

	/**
	 * Complete execution and set end time
	 */
	public void complete(String summary) {
		this.endTime = LocalDateTime.now();
		this.completed = true;
		this.summary = summary;
	}

	// Getters and Setters

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCurrentPlanId() {
		return currentPlanId;
	}

	public void setCurrentPlanId(String currentPlanId) {
		this.currentPlanId = currentPlanId;
	}

	public String getRootPlanId() {
		return rootPlanId;
	}

	public void setRootPlanId(String rootPlanId) {
		this.rootPlanId = rootPlanId;
	}

	public String getParentPlanId() {
		return parentPlanId;
	}

	public void setParentPlanId(String parentPlanId) {
		this.parentPlanId = parentPlanId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getUserRequest() {
		return userRequest;
	}

	public void setUserRequest(String userRequest) {
		this.userRequest = userRequest;
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

	public List<String> getSteps() {
		return steps;
	}

	public void setSteps(List<String> steps) {
		this.steps = steps;
	}

	public Integer getCurrentStepIndex() {
		return currentStepIndex;
	}

	public void setCurrentStepIndex(Integer currentStepIndex) {
		this.currentStepIndex = currentStepIndex;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	public String getToolCallId() {
		return toolCallId;
	}

	public void setToolCallId(String toolCallId) {
		this.toolCallId = toolCallId;
	}

}

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

package com.alibaba.cloud.ai.manus.recorder.entity.vo;

import com.alibaba.cloud.ai.manus.runtime.entity.vo.UserInputWaitState;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

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
public class PlanExecutionRecord {

	// Unique identifier for the record
	private Long id;

	// Unique identifier for the current plan
	private String currentPlanId;

	// Root plan ID for sub-plans (null for main plans)
	private String rootPlanId;

	// Parent plan ID for sub-plans (null for root plans)
	private String parentPlanId;

	// Tool call ID that triggered this plan (for sub-plans)
	private String toolCallId;

	// Plan title
	private String title;

	// User's original request
	private String userRequest;

	// Timestamp when execution started
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	private LocalDateTime startTime;

	// Timestamp when execution ended
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	private LocalDateTime endTime;

	// Current step index being executed
	private Integer currentStepIndex;

	// Whether completed
	private boolean completed;

	// Execution summary
	private String summary;

	// List to maintain the sequence of agent executions
	private List<AgentExecutionRecord> agentExecutionSequence;

	// Field to store user input wait state
	private UserInputWaitState userInputWaitState;

	// Actual calling model
	private String modelName;

	// Parent tool call information that triggered this sub-plan (for sub-plan detail
	// displaying)
	private ActToolInfo parentActToolCall;

	/**
	 * Default constructor for Jackson and other frameworks.
	 */
	public PlanExecutionRecord() {
		this.startTime = LocalDateTime.now();
		this.completed = false;
		this.agentExecutionSequence = new ArrayList<>();
		this.id = generateId();
		// Initialize current step index
		this.currentStepIndex = 0;
	}

	/**
	 * Add agent execution record
	 * @param record Execution record
	 */
	public void addAgentExecutionRecord(AgentExecutionRecord record) {
		this.agentExecutionSequence.add(record);
		// Dynamically update current step index
		updateCurrentStepIndex();
	}

	/**
	 * Get agent execution records sorted by execution order
	 * @return List of execution records
	 */
	public List<AgentExecutionRecord> getAgentExecutionSequence() {
		return agentExecutionSequence;
	}

	public void setAgentExecutionSequence(List<AgentExecutionRecord> agentExecutionSequence) {
		this.agentExecutionSequence = agentExecutionSequence;
		// Update current step index after setting the sequence
		updateCurrentStepIndex();
	}

	/**
	 * Complete execution and set end time
	 */
	public void complete(String summary) {
		this.endTime = LocalDateTime.now();
		this.completed = true;
		this.summary = summary;
		// Update current step index when plan is completed
		updateCurrentStepIndex();
	}

	/**
	 * Dynamically update currentStepIndex based on agentExecutionSequence This method
	 * analyzes the execution state of agents to determine the current step
	 */
	public void updateCurrentStepIndex() {
		if (this.agentExecutionSequence == null || this.agentExecutionSequence.isEmpty()) {
			this.currentStepIndex = 0;
			return;
		}

		// If plan is completed, set to the last step
		if (this.completed) {
			this.currentStepIndex = this.agentExecutionSequence.size() - 1;
			return;
		}

		// Find the currently running step
		for (int i = 0; i < this.agentExecutionSequence.size(); i++) {
			AgentExecutionRecord agent = this.agentExecutionSequence.get(i);
			if (agent != null && ExecutionStatus.RUNNING.equals(agent.getStatus())) {
				this.currentStepIndex = i;
				return;
			}
		}

		// If no step is running, find the first unfinished step
		for (int i = 0; i < this.agentExecutionSequence.size(); i++) {
			AgentExecutionRecord agent = this.agentExecutionSequence.get(i);
			if (agent != null && !ExecutionStatus.FINISHED.equals(agent.getStatus())) {
				this.currentStepIndex = i;
				return;
			}
		}

		// If all steps are completed, set to the last step
		this.currentStepIndex = this.agentExecutionSequence.size() - 1;
	}

	/**
	 * Generate unique ID if not already set
	 * @return Generated or existing ID
	 */
	private Long generateId() {
		if (this.id == null) {
			// Use combination of timestamp and random number to generate ID
			long timestamp = System.currentTimeMillis();
			int random = (int) (Math.random() * 1000000);
			this.id = timestamp * 1000 + random;
		}
		return this.id;
	}

	// Getters and Setters

	public Long getId() {
		// Ensure ID is generated when accessing
		if (this.id == null) {
			this.id = generateId();
		}
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

	public String getToolCallId() {
		return toolCallId;
	}

	public void setToolCallId(String toolCallId) {
		this.toolCallId = toolCallId;
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

	public UserInputWaitState getUserInputWaitState() {
		return userInputWaitState;
	}

	public void setUserInputWaitState(UserInputWaitState userInputWaitState) {
		this.userInputWaitState = userInputWaitState;
	}

	public LocalDateTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalDateTime endTime) {
		this.endTime = endTime;
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

	public ActToolInfo getParentActToolCall() {
		return parentActToolCall;
	}

	public void setParentActToolCall(ActToolInfo parentActToolCall) {
		this.parentActToolCall = parentActToolCall;
	}

}

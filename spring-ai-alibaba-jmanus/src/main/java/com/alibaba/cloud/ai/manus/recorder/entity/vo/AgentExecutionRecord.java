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

import com.alibaba.cloud.ai.manus.agent.BaseAgent;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Unified agent execution record class for tracking and recording detailed information
 * about BaseAgent execution process.
 *
 * This class combines the functionality of both AgentExecutionRecordSimple and
 * AgentExecutionRecordDetail to provide a single, comprehensive record structure.
 *
 * Data structure is divided into three main parts:
 *
 * 1. Basic Info - id: unique identifier of the record - stepId: step identifier -
 * conversationId: conversation unique identifier - agentName: agent name -
 * agentDescription: agent description - startTime: execution start time - endTime:
 * execution end time
 *
 * 2. Execution Data - maxSteps: maximum execution steps - currentStep: current execution
 * step - status: execution status (IDLE, RUNNING, FINISHED) - thinkActSteps: think-act
 * step record list, each element is a ThinkActRecord object - agentRequest: input prompt
 * template
 *
 * 3. Execution Result - result: execution result - errorMessage: error message (if any) -
 * modelName: actual calling model - subPlanExecutionRecords: sub-plan execution records
 *
 * @see BaseAgent
 * @see ThinkActRecord
 * @see JsonSerializable
 */
public class AgentExecutionRecord {

	// Unique identifier of the record
	private Long id;

	// Step ID this record belongs to
	private String stepId;

	// Conversation ID this record belongs to
	private String conversationId;

	// Name of the agent that created this record
	private String agentName;

	// Description information of the agent
	private String agentDescription;

	// Timestamp when execution started
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	private LocalDateTime startTime;

	// Timestamp when execution ended
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	private LocalDateTime endTime;

	// Maximum allowed number of steps
	private int maxSteps;

	// Current execution step number
	private int currentStep;

	// Execution status (IDLE, RUNNING, FINISHED)
	private ExecutionStatus status;

	// Request content for agent execution
	private String agentRequest;

	// Execution result
	private String result;

	// Error message if execution encounters problems
	private String errorMessage;

	// Actual calling model
	private String modelName;

	// List of ThinkActRecord for detailed execution process
	private List<ThinkActRecord> thinkActSteps;

	// Sub-plan execution records list
	private List<PlanExecutionRecord> subPlanExecutionRecords;

	// Default constructor
	public AgentExecutionRecord() {
		this.thinkActSteps = new ArrayList<>();
		this.subPlanExecutionRecords = new ArrayList<>();
	}

	// Constructor with basic parameters
	public AgentExecutionRecord(String stepId, String agentName, String agentDescription) {
		this.stepId = stepId;
		this.agentName = agentName;
		this.agentDescription = agentDescription;
		this.startTime = LocalDateTime.now();
		this.status = ExecutionStatus.IDLE;
		this.currentStep = 0;
		this.thinkActSteps = new ArrayList<>();
		this.subPlanExecutionRecords = new ArrayList<>();
	}

	// Constructor with conversation ID
	public AgentExecutionRecord(String stepId, String conversationId, String agentName, String agentDescription) {
		this.stepId = stepId;
		this.conversationId = conversationId;
		this.agentName = agentName;
		this.agentDescription = agentDescription;
		this.startTime = LocalDateTime.now();
		this.status = ExecutionStatus.IDLE;
		this.currentStep = 0;
		this.thinkActSteps = new ArrayList<>();
		this.subPlanExecutionRecords = new ArrayList<>();
	}

	// Getters and setters

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getStepId() {
		return stepId;
	}

	public void setStepId(String stepId) {
		this.stepId = stepId;
	}

	public String getConversationId() {
		return conversationId;
	}

	public void setConversationId(String conversationId) {
		this.conversationId = conversationId;
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

	public int getMaxSteps() {
		return maxSteps;
	}

	public void setMaxSteps(int maxSteps) {
		this.maxSteps = maxSteps;
	}

	public int getCurrentStep() {
		return currentStep;
	}

	public void setCurrentStep(int currentStep) {
		this.currentStep = currentStep;
	}

	public ExecutionStatus getStatus() {
		return status;
	}

	public void setStatus(ExecutionStatus status) {
		this.status = status;
	}

	public String getAgentRequest() {
		return agentRequest;
	}

	public void setAgentRequest(String agentRequest) {
		this.agentRequest = agentRequest;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	public List<ThinkActRecord> getThinkActSteps() {
		return thinkActSteps;
	}

	public void setThinkActSteps(List<ThinkActRecord> thinkActSteps) {
		this.thinkActSteps = thinkActSteps;
	}

	public List<PlanExecutionRecord> getSubPlanExecutionRecords() {
		return subPlanExecutionRecords;
	}

	public void setSubPlanExecutionRecords(List<PlanExecutionRecord> subPlanExecutionRecords) {
		this.subPlanExecutionRecords = subPlanExecutionRecords;
	}

	// Utility methods

	/**
	 * Check if the execution is completed
	 */
	public boolean isCompleted() {
		return status == ExecutionStatus.FINISHED;
	}

	/**
	 * Check if the execution is running
	 */
	public boolean isRunning() {
		return status == ExecutionStatus.RUNNING;
	}

	/**
	 * Check if the execution is idle
	 */
	public boolean isIdle() {
		return status == ExecutionStatus.IDLE;
	}

	/**
	 * Get the number of completed think-act steps
	 */
	public int getCompletedThinkActStepsCount() {
		if (thinkActSteps == null)
			return 0;
		return (int) thinkActSteps.stream().filter(step -> step.getStatus() == ExecutionStatus.FINISHED).count();
	}

	/**
	 * Get the number of sub-plan execution records
	 */
	public int getSubPlanCount() {
		return subPlanExecutionRecords != null ? subPlanExecutionRecords.size() : 0;
	}

	@Override
	public String toString() {
		return "AgentExecutionRecord{" + "id=" + id + ", stepId='" + stepId + '\'' + ", conversationId='"
				+ conversationId + '\'' + ", agentName='" + agentName + '\'' + ", status=" + status + ", currentStep="
				+ currentStep + ", maxSteps=" + maxSteps + ", thinkActStepsCount="
				+ (thinkActSteps != null ? thinkActSteps.size() : 0) + ", subPlanCount=" + getSubPlanCount() + '}';
	}

}

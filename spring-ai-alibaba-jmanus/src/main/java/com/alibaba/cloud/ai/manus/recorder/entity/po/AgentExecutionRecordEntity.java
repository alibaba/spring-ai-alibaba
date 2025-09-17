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

import com.alibaba.cloud.ai.manus.agent.BaseAgent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Agent execution record class for tracking and recording detailed information about
 * BaseAgent execution process.
 *
 * Data structure is divided into three main parts:
 *
 * 1. Basic Info - id: unique identifier of the record - conversationId: conversation
 * unique identifier - agentName: agent name - agentDescription: agent description -
 * startTime: execution start time - endTime: execution end time
 *
 * 2. Execution Data - maxSteps: maximum execution steps - currentStep: current execution
 * step - status: execution status (IDLE, RUNNING, FINISHED) - thinkActSteps: think-act
 * step record list, each element is a ThinkActRecordEntity object - agentRequest: input
 * prompt template
 *
 * 3. Execution Result - isCompleted: whether completed - isStuck: whether stuck - result:
 * execution result - errorMessage: error message (if any)
 *
 * @see BaseAgent
 * @see ThinkActRecordEntity
 *
 */

@Entity
@Table(name = "agent_execution_record", indexes = { @Index(columnList = "step_id") })
public class AgentExecutionRecordEntity {

	// Unique identifier of the record
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Step ID this record belongs to
	@Column(name = "step_id", unique = true)
	private String stepId;

	// Name of the agent that created this record
	@Column(name = "agent_name")
	private String agentName;

	// Description information of the agent
	@Column(name = "agent_description", columnDefinition = "LONGTEXT")
	private String agentDescription;

	// Timestamp when execution started
	@Column(name = "start_time")
	private LocalDateTime startTime;

	// Timestamp when execution ended
	@Column(name = "end_time")
	private LocalDateTime endTime;

	// Maximum allowed number of steps
	@Column(name = "max_steps")
	private int maxSteps;

	// Current execution step number
	@Column(name = "current_step")
	private int currentStep;

	// Execution status (IDLE, RUNNING, FINISHED)
	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private ExecutionStatusEntity status;

	// Record list of think-act steps, existing as sub-steps
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "agent_execution_id")
	private List<ThinkActRecordEntity> thinkActSteps;

	// Request content for agent execution
	@Column(name = "agent_request", columnDefinition = "LONGTEXT")
	private String agentRequest;

	// Execution result
	@Column(name = "result", columnDefinition = "LONGTEXT")
	private String result;

	// Error message if execution encounters problems
	@Column(name = "error_message", columnDefinition = "LONGTEXT")
	private String errorMessage;

	// Actual calling model
	@Column(name = "model_name")
	private String modelName;

	// Default constructor
	public AgentExecutionRecordEntity() {
		this.thinkActSteps = new ArrayList<>();
	}

	// Constructor with parameters
	public AgentExecutionRecordEntity(String stepId, String agentName, String agentDescription) {
		this.stepId = stepId;
		this.agentName = agentName;
		this.agentDescription = agentDescription;
		this.startTime = LocalDateTime.now();
		this.status = ExecutionStatusEntity.IDLE; // Use enum value
		this.currentStep = 0;
		this.thinkActSteps = new ArrayList<>();
	}

	/**
	 * Add a ThinkActRecordEntity as execution step
	 * @param record ThinkActRecordEntity instance
	 */
	public void addThinkActStep(ThinkActRecordEntity record) {
		if (this.thinkActSteps == null) {
			this.thinkActSteps = new ArrayList<>();
		}
		this.thinkActSteps.add(record);
		this.currentStep = this.thinkActSteps.size();
	}

	// Getters and setters

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

	public ExecutionStatusEntity getStatus() {
		return status;
	}

	public void setStatus(ExecutionStatusEntity status) {
		this.status = status;
	}

	public List<ThinkActRecordEntity> getThinkActSteps() {
		return thinkActSteps;
	}

	public void setThinkActSteps(List<ThinkActRecordEntity> thinkActSteps) {
		this.thinkActSteps = thinkActSteps;
		this.currentStep = thinkActSteps != null ? thinkActSteps.size() : 0;
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

	@Override
	public String toString() {
		return "AgentExecutionRecordEntity{" + "id='" + id + '\'' + ", stepId='" + stepId + '\'' + ", agentName='"
				+ agentName + '\'' + ", status='" + status + '\'' + ", currentStep=" + currentStep + ", maxSteps="
				+ maxSteps + ", stepsCount=" + (thinkActSteps != null ? thinkActSteps.size() : 0) + '}';
	}

	public String getStepId() {
		return stepId;
	}

	public void setStepId(String stepId) {
		this.stepId = stepId;
	}

}

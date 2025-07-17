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
package com.alibaba.cloud.ai.example.manus.recorder.entity;

import com.alibaba.cloud.ai.example.manus.agent.BaseAgent;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

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
 * step record list, each element is a ThinkActRecord object - agentRequest: input prompt
 * template
 *
 * 3. Execution Result - isCompleted: whether completed - isStuck: whether stuck - result:
 * execution result - errorMessage: error message (if any)
 *
 * @see BaseAgent
 * @see ThinkActRecord
 * @see JsonSerializable
 */

public class AgentExecutionRecord {

	// Unique identifier of the record
	private Long id;

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

	// Record list of think-act steps, existing as sub-steps
	private List<ThinkActRecord> thinkActSteps;

	// Request content for agent execution
	private String agentRequest;

	// Execution result
	private String result;

	// Error message if execution encounters problems
	private String errorMessage;

	// Actual calling model
	private String modelName;

	// Default constructor
	public AgentExecutionRecord() {
		this.thinkActSteps = new ArrayList<>();
		// Ensure ID is generated during initialization
		this.id = generateId();
	}

	// Constructor with parameters
	public AgentExecutionRecord(String conversationId, String agentName, String agentDescription) {
		this.conversationId = conversationId;
		this.agentName = agentName;
		this.agentDescription = agentDescription;
		this.startTime = LocalDateTime.now();
		this.status = ExecutionStatus.IDLE; // Use enum value
		this.currentStep = 0;
		this.thinkActSteps = new ArrayList<>();
		// Ensure ID is generated during initialization
		this.id = generateId();
	}

	/**
	 * Add a ThinkActRecord as execution step
	 * @param record ThinkActRecord instance
	 */
	public void addThinkActStep(ThinkActRecord record) {
		if (this.thinkActSteps == null) {
			this.thinkActSteps = new ArrayList<>();
		}
		this.thinkActSteps.add(record);
		this.currentStep = this.thinkActSteps.size();
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

	// Getters and setters

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

	public List<ThinkActRecord> getThinkActSteps() {
		return thinkActSteps;
	}

	public void setThinkActSteps(List<ThinkActRecord> thinkActSteps) {
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
		return "AgentExecutionRecord{" + "id='" + id + '\'' + ", conversationId='" + conversationId + '\''
				+ ", agentName='" + agentName + '\'' + ", status='" + status + '\'' + ", currentStep=" + currentStep
				+ ", maxSteps=" + maxSteps + ", stepsCount=" + (thinkActSteps != null ? thinkActSteps.size() : 0) + '}';
	}

	/**
	 * Save record to persistent storage. Empty implementation, to be overridden by
	 * specific storage implementations. Also recursively saves all ThinkActRecords
	 * @return Record ID after saving
	 */
	public Long save() {
		// If ID is null, generate a random ID
		if (this.id == null) {
			// Use combination of timestamp and random number to generate ID
			long timestamp = System.currentTimeMillis();
			int random = (int) (Math.random() * 1000000);
			this.id = timestamp * 1000 + random;
		}

		// Save all ThinkActRecords
		if (thinkActSteps != null) {
			for (ThinkActRecord thinkActRecord : thinkActSteps) {
				thinkActRecord.save();
			}
		}
		return this.id;
	}

	public String getConversationId() {
		return conversationId;
	}

	public void setConversationId(String conversationId) {
		this.conversationId = conversationId;
	}

}

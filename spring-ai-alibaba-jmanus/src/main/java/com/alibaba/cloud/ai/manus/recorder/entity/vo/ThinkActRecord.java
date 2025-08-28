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

import java.time.LocalDateTime;
import java.util.List;

/**
 * Records the thinking and action process of an agent in a single execution step. Exists
 * as a sub-step of AgentExecutionRecord, focusing on recording processing messages during
 * thinking and action phases.
 *
 * Data structure simplified into three main parts:
 *
 * 1. Basic Info - id: unique identifier of the record - stepNumber: step number -
 * parentExecutionId: parent execution record ID
 *
 * 2. Think Phase - thinkStartTime: thinking start time - thinkInput: thinking input
 * content - thinkOutput: thinking output result - thinkEndTime: thinking end time
 *
 * 3. Act Phase - actStartTime: action start time - toolName: tool name used -
 * toolParameters: tool parameters - actionNeeded: whether action execution is needed -
 * actionDescription: action description - actionResult: action execution result -
 * actEndTime: action end time - status: execution status - errorMessage: error message
 * (if any)
 *
 * @see AgentExecutionRecord
 * @see JsonSerializable
 */
public class ThinkActRecord {

	// Unique identifier of the record
	private Long id;

	// Method to generate unique ID
	private Long generateId() {
		if (this.id == null) {
			long timestamp = System.currentTimeMillis();
			int random = (int) (Math.random() * 1000000);
			this.id = timestamp * 1000 + random;
		}
		return this.id;
	}

	// ID of parent execution record, linked to AgentExecutionRecord
	private Long parentExecutionId;

	// Timestamp when thinking started
	private LocalDateTime thinkStartTime;

	// Timestamp when thinking completed
	private LocalDateTime thinkEndTime;

	// Timestamp when action started
	private LocalDateTime actStartTime;

	// Timestamp when action completed
	private LocalDateTime actEndTime;

	// Input context for the thinking process
	private String thinkInput;

	// Output result of the thinking process
	private String thinkOutput;

	// Whether thinking determined that action is needed
	private boolean actionNeeded;

	// Description of the action to be taken
	private String actionDescription;

	// Result of action execution
	private String actionResult;

	// Status of this think-act cycle (success, failure, etc.)
	private ExecutionStatus status;

	// Error message if the cycle encountered problems
	private String errorMessage;

	// Tool name used for action (if applicable)
	private String toolName;

	// Tool parameters used for action (serialized, if applicable)
	private String toolParameters;

	// Action tool information(When disabling parallel tool calls, there is always only
	// one)
	private List<ActToolInfo> actToolInfoList;

	// Default constructor
	public ThinkActRecord() {
		this.id = generateId();
	}

	// Constructor with parent execution ID
	public ThinkActRecord(Long parentExecutionId) {
		this.parentExecutionId = parentExecutionId;
		this.thinkStartTime = LocalDateTime.now();
		this.id = generateId();
	}

	/**
	 * Record the start of thinking phase
	 */
	public void startThinking(String thinkInput) {

		this.thinkStartTime = LocalDateTime.now();
		this.thinkInput = thinkInput;
	}

	/**
	 * Record the end of thinking phase
	 */
	public void finishThinking(String thinkOutput) {
		this.thinkEndTime = LocalDateTime.now();
		this.thinkOutput = thinkOutput;
	}

	/**
	 * Record the start of action phase
	 */
	public void startAction(String actionDescription, String toolName, String toolParameters) {
		this.actStartTime = LocalDateTime.now();
		this.actionNeeded = true;
		this.actionDescription = actionDescription;
		this.toolName = toolName;
		this.toolParameters = toolParameters;
	}

	/**
	 * Record the end of action phase
	 */
	public void finishAction(String actionResult, ExecutionStatus status) {
		this.actEndTime = LocalDateTime.now();
		this.actionResult = actionResult;
		this.status = status;
	}

	/**
	 * Record error information
	 */
	public void recordError(String errorMessage) {
		this.errorMessage = errorMessage;
		this.status = ExecutionStatus.RUNNING;
	}

	/**
	 * Save record to persistent storage. Empty implementation, to be overridden by
	 * specific storage implementations
	 * @return Record ID after saving
	 */
	public Long save() {

		return this.id;
	}

	// Getters and setters

	public Long getId() {
		// Ensure ID is generated when accessing
		if (this.id == null) {
			this.id = generateId();
		}
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getParentExecutionId() {
		return parentExecutionId;
	}

	public void setParentExecutionId(Long parentExecutionId) {
		this.parentExecutionId = parentExecutionId;
	}

	public LocalDateTime getThinkStartTime() {
		return thinkStartTime;
	}

	public void setThinkStartTime(LocalDateTime thinkStartTime) {
		this.thinkStartTime = thinkStartTime;
	}

	public LocalDateTime getThinkEndTime() {
		return thinkEndTime;
	}

	public void setThinkEndTime(LocalDateTime thinkEndTime) {
		this.thinkEndTime = thinkEndTime;
	}

	public LocalDateTime getActStartTime() {
		return actStartTime;
	}

	public void setActStartTime(LocalDateTime actStartTime) {
		this.actStartTime = actStartTime;
	}

	public LocalDateTime getActEndTime() {
		return actEndTime;
	}

	public void setActEndTime(LocalDateTime actEndTime) {
		this.actEndTime = actEndTime;
	}

	public String getThinkInput() {
		return thinkInput;
	}

	public void setThinkInput(String thinkInput) {
		this.thinkInput = thinkInput;
	}

	public String getThinkOutput() {
		return thinkOutput;
	}

	public void setThinkOutput(String thinkOutput) {
		this.thinkOutput = thinkOutput;
	}

	public boolean isActionNeeded() {
		return actionNeeded;
	}

	public void setActionNeeded(boolean actionNeeded) {
		this.actionNeeded = actionNeeded;
	}

	public String getActionDescription() {
		return actionDescription;
	}

	public void setActionDescription(String actionDescription) {
		this.actionDescription = actionDescription;
	}

	public String getActionResult() {
		return actionResult;
	}

	public void setActionResult(String actionResult) {
		this.actionResult = actionResult;
	}

	public ExecutionStatus getStatus() {
		return status;
	}

	public void setStatus(ExecutionStatus status) {
		this.status = status;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getToolName() {
		return toolName;
	}

	public void setToolName(String toolName) {
		this.toolName = toolName;
	}

	public String getToolParameters() {
		return toolParameters;
	}

	public void setToolParameters(String toolParameters) {
		this.toolParameters = toolParameters;
	}

	public List<ActToolInfo> getActToolInfoList() {
		return actToolInfoList;
	}

	public void setActToolInfoList(List<ActToolInfo> actToolInfoList) {
		this.actToolInfoList = actToolInfoList;
	}

	@Override
	public String toString() {
		return "ThinkActRecord{" + "id='" + id + '\'' + ", parentExecutionId='" + parentExecutionId + '\''
				+ ", actionNeeded=" + actionNeeded + ", status='" + status + '\'' + '}';
	}

}

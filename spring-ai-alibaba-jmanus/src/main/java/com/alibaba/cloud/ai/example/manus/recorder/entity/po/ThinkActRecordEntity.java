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
package com.alibaba.cloud.ai.example.manus.recorder.entity.po;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Records the thinking and action process of an agent in a single execution step. Exists
 * as a sub-step of AgentExecutionRecordEntity, focusing on recording processing messages during
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
 * @see AgentExecutionRecordEntity

 */
@Entity
@Table(name = "think_act_record")
public class ThinkActRecordEntity {

	// Unique identifier of the record
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Think-Act ID as String identifier
	@Column(name = "think_act_id")
	private String thinkActId;

	// ID of parent execution record, linked to AgentExecutionRecordEntity
	@Column(name = "parent_execution_id")
	private Long parentExecutionId;

	// Timestamp when thinking started
	@Column(name = "think_start_time")
	private LocalDateTime thinkStartTime;

	// Timestamp when thinking completed
	@Column(name = "think_end_time")
	private LocalDateTime thinkEndTime;

	// Timestamp when action started
	@Column(name = "act_start_time")
	private LocalDateTime actStartTime;

	// Timestamp when action completed
	@Column(name = "act_end_time")
	private LocalDateTime actEndTime;

	// Input context for the thinking process
	@Column(name = "think_input", columnDefinition = "LONGTEXT")
	private String thinkInput;

	// Output result of the thinking process
	@Column(name = "think_output", columnDefinition = "LONGTEXT")
	private String thinkOutput;

	// Whether thinking determined that action is needed
	@Column(name = "action_needed")
	private boolean actionNeeded;


	// Error message if the cycle encountered problems
	@Column(name = "error_message", columnDefinition = "LONGTEXT")
	private String errorMessage;


	// Action tool information(When disabling parallel tool calls, there is always only
	// one)
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "think_act_record_id")
	private List<ActToolInfoEntity> actToolInfoList;

	// Default constructor
	public ThinkActRecordEntity() {
	}

	// Constructor with parent execution ID
	public ThinkActRecordEntity(Long parentExecutionId) {
		this.parentExecutionId = parentExecutionId;
		this.thinkStartTime = LocalDateTime.now();
	}

	// Getters and setters

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getThinkActId() {
		return thinkActId;
	}

	public void setThinkActId(String thinkActId) {
		this.thinkActId = thinkActId;
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

	public ExecutionStatusEntity getStatus() {
		return status;
	}

	public void setStatus(ExecutionStatusEntity status) {
		this.status = status;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public List<ActToolInfoEntity> getActToolInfoList() {
		return actToolInfoList;
	}

	public void setActToolInfoList(List<ActToolInfoEntity> actToolInfoList) {
		this.actToolInfoList = actToolInfoList;
	}

	@Override
	public String toString() {
		return "ThinkActRecordEntity{" + "id='" + id + '\'' + ", parentExecutionId='" + parentExecutionId + '\''
				+ ", actionNeeded=" + actionNeeded + ", status='" + status + '\'' + '}';
	}



}

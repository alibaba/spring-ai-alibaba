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
package com.alibaba.cloud.ai.example.manus.recorder;

import java.util.List;

import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionStep;
import com.alibaba.cloud.ai.example.manus.recorder.entity.PlanExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.ThinkActRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.ExecutionStatus;

/**
 * Plan execution recorder interface that defines methods for recording and retrieving
 * plan execution details.
 */
public interface PlanExecutionRecorder {

	/**
	 * Removes the execution records for the specified plan ID
	 * @param planId The plan ID to remove
	 */
	void removeExecutionRecord(String planId);

	PlanExecutionRecord getRootPlanExecutionRecord(String rootPlanId);

	/**
	 * Record the start of step execution.
	 * @param step Execution step
	 * @param context Execution context
	 */
	void recordStepStart(ExecutionStep step, ExecutionContext context);

	/**
	 * Record the end of step execution.
	 * @param step Execution step
	 * @param context Execution context
	 */
	void recordStepEnd(ExecutionStep step, ExecutionContext context);

	/**
	 * Record the start of plan execution.
	 * @param context Execution context containing user request and execution process
	 * information
	 */
	void recordPlanExecutionStart(ExecutionContext context);

	/**
	 * Record think-act execution
	 * @param planExecutionRecord Plan execution record
	 * @param agentExecutionId Agent execution ID
	 * @param thinkActRecord Think-act record
	 */
	void setThinkActExecution(PlanExecutionRecord planExecutionRecord, Long agentExecutionId,
			ThinkActRecord thinkActRecord);

	/**
	 * Record plan completion
	 * @param planExecutionRecord Plan execution record
	 * @param summary Execution summary
	 */
	void setPlanCompletion(PlanExecutionRecord planExecutionRecord, String summary);

	/**
	 * Save execution records to persistent storage
	 * @param planExecutionRecord Plan execution record to save
	 * @return true if save was successful
	 */
	boolean savePlanExecutionRecords(PlanExecutionRecord planExecutionRecord);

	/**
	 * Record complete agent execution at the end. This method handles all agent execution
	 * record management logic without exposing internal record objects.
	 * @param currentPlanId Current plan ID
	 * @param rootPlanId Root plan ID
	 * @param thinkActRecordId Think-act record ID for sub-plan executions (null for root
	 * plans)
	 * @param agentName Agent name
	 * @param agentDescription Agent description
	 * @param maxSteps Maximum execution steps
	 * @param actualSteps Actual steps executed
	 * @param completed Whether execution completed successfully
	 * @param stuck Whether agent got stuck
	 * @param errorMessage Error message if any
	 * @param result Final execution result
	 * @param startTime Execution start time
	 * @param endTime Execution end time
	 */
	void recordCompleteAgentExecution(PlanExecutionParams params);

	/**
	 * Interface 1: Record thinking and action execution process. This method handles
	 * ThinkActRecord creation and thinking process without exposing internal record
	 * objects.
	 * @param params Encapsulated parameters for plan execution
	 * @return ThinkActRecord ID for subsequent action recording
	 */
	Long recordThinkingAndAction(PlanExecutionParams params);

	/**
	 * Interface 2: Record action execution result. This method updates the ThinkActRecord
	 * with action results without exposing internal record objects.
	 * @param currentPlanId Current plan ID
	 * @param rootPlanId Root plan ID
	 * @param thinkActRecordId Think-act record ID for sub-plan executions (null for root
	 * plans)
	 * @param createdThinkActRecordId The ThinkActRecord ID returned from
	 * recordThinkingAndAction
	 * @param actionDescription Description of the action to be taken
	 * @param actionResult Result of action execution
	 * @param status Execution status (SUCCESS, FAILED, etc.)
	 * @param errorMessage Error message if action execution failed
	 * @param toolName Tool name used for action
	 * @param subPlanCreated Whether this action created a sub-plan execution
	 */
	void recordActionResult(PlanExecutionParams params);

	/**
	 * Interface 3: Record plan completion. This method handles plan completion recording
	 * logic without exposing internal record objects.
	 * @param currentPlanId Current plan ID
	 * @param rootPlanId Root plan ID
	 * @param thinkActRecordId Think-act record ID for sub-plan executions (null for root
	 * plans)
	 * @param summary The summary of the plan execution
	 */
	void recordPlanCompletion(String currentPlanId, String rootPlanId, Long thinkActRecordId, String summary);

	public Long getCurrentThinkActRecordId(String currentPlanId, String rootPlanId);

	/**
	 * Parameter encapsulation class for recording all relevant information about plan
	 * execution
	 */
	public class PlanExecutionParams {

		/** Current plan ID */
		String currentPlanId;

		/** Root plan ID */
		String rootPlanId;

		/** Think-act record ID */
		Long thinkActRecordId;

		/** Agent name */
		String agentName;

		/** Agent description */
		String agentDescription;

		/** Thinking input */
		String thinkInput;

		/** Thinking output */
		String thinkOutput;

		/** Whether action is needed */
		boolean actionNeeded;

		/** Tool name */
		String toolName;

		/** Tool parameters */
		String toolParameters;

		/** Model name */
		String modelName;

		/** Error message */
		String errorMessage;

		/** Created Think-act record ID */
		Long createdThinkActRecordId;

		/** Action description */
		String actionDescription;

		/** Action result */
		String actionResult;

		/** Execution status */
		ExecutionStatus status;

		/** Whether a sub-plan was created */
		boolean subPlanCreated;

		/** Action tool information list */
		List<ThinkActRecord.ActToolInfo> actToolInfoList;

		/** Execution summary */
		String summary;

		/** Maximum execution steps */
		int maxSteps;

		/** Actual steps executed */
		int actualSteps;

		/** Final execution result */
		String result;

		/** Execution start time */
		java.time.LocalDateTime startTime;

		/** Execution end time */
		java.time.LocalDateTime endTime;

		public int getMaxSteps() {
			return maxSteps;
		}

		public void setMaxSteps(int maxSteps) {
			this.maxSteps = maxSteps;
		}

		public int getActualSteps() {
			return actualSteps;
		}

		public void setActualSteps(int actualSteps) {
			this.actualSteps = actualSteps;
		}

		public String getResult() {
			return result;
		}

		public void setResult(String result) {
			this.result = result;
		}

		public java.time.LocalDateTime getStartTime() {
			return startTime;
		}

		public void setStartTime(java.time.LocalDateTime startTime) {
			this.startTime = startTime;
		}

		public java.time.LocalDateTime getEndTime() {
			return endTime;
		}

		public void setEndTime(java.time.LocalDateTime endTime) {
			this.endTime = endTime;
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

		public Long getThinkActRecordId() {
			return thinkActRecordId;
		}

		public void setThinkActRecordId(Long thinkActRecordId) {
			this.thinkActRecordId = thinkActRecordId;
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

		public String getModelName() {
			return modelName;
		}

		public void setModelName(String modelName) {
			this.modelName = modelName;
		}

		public String getErrorMessage() {
			return errorMessage;
		}

		public void setErrorMessage(String errorMessage) {
			this.errorMessage = errorMessage;
		}

		public Long getCreatedThinkActRecordId() {
			return createdThinkActRecordId;
		}

		public void setCreatedThinkActRecordId(Long createdThinkActRecordId) {
			this.createdThinkActRecordId = createdThinkActRecordId;
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

		public boolean isSubPlanCreated() {
			return subPlanCreated;
		}

		public void setSubPlanCreated(boolean subPlanCreated) {
			this.subPlanCreated = subPlanCreated;
		}

		public String getSummary() {
			return summary;
		}

		public void setSummary(String summary) {
			this.summary = summary;
		}

		public List<com.alibaba.cloud.ai.example.manus.recorder.entity.ThinkActRecord.ActToolInfo> getActToolInfoList() {
			return actToolInfoList;
		}

		public void setActToolInfoList(
				List<com.alibaba.cloud.ai.example.manus.recorder.entity.ThinkActRecord.ActToolInfo> actToolInfoList) {
			this.actToolInfoList = actToolInfoList;
		}

	}

}

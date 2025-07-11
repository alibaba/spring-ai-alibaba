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

import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionStep;
import com.alibaba.cloud.ai.example.manus.recorder.entity.PlanExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.ThinkActRecord;

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
	void recordStepEnd(com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionStep step, com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext context);

	/**
	 * Record the start of plan execution.
	 * @param context Execution context containing user request and execution process information
	 */
	void recordPlanExecutionStart(com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext context);

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
	 * @param thinkActRecordId Think-act record ID for sub-plan executions (null for root plans)
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
	void recordCompleteAgentExecution(String currentPlanId, String rootPlanId, Long thinkActRecordId,
			String agentName, String agentDescription, int maxSteps, int actualSteps,
			boolean completed, boolean stuck, String errorMessage, String result,
			java.time.LocalDateTime startTime, java.time.LocalDateTime endTime);

	/**
	 * 接口1: 记录思考和执行动作
	 * Record thinking and action execution process. This method handles ThinkActRecord creation and thinking process
	 * without exposing internal record objects.
	 * @param currentPlanId Current plan ID
	 * @param rootPlanId Root plan ID  
	 * @param thinkActRecordId Think-act record ID for sub-plan executions (null for root plans)
	 * @param agentName Agent name
	 * @param agentDescription Agent description
	 * @param thinkInput Input context for the thinking process
	 * @param thinkOutput Output result of the thinking process
	 * @param actionNeeded Whether thinking determined that action is needed
	 * @param toolName Tool name used for action (if applicable)
	 * @param toolParameters Tool parameters used for action (if applicable)
	 * @param modelName Model name used for thinking
	 * @param errorMessage Error message if thinking process failed
	 * @return ThinkActRecord ID for subsequent action recording
	 */
	Long recordThinkingAndAction(String currentPlanId, String rootPlanId, Long thinkActRecordId,
			String agentName, String agentDescription, String thinkInput, String thinkOutput,
			boolean actionNeeded, String toolName, String toolParameters, String modelName, String errorMessage);

	/**
	 * 接口2: 记录执行结果  
	 * Record action execution result. This method updates the ThinkActRecord with action results
	 * without exposing internal record objects.
	 * @param currentPlanId Current plan ID
	 * @param rootPlanId Root plan ID
	 * @param thinkActRecordId Think-act record ID for sub-plan executions (null for root plans)
	 * @param createdThinkActRecordId The ThinkActRecord ID returned from recordThinkingAndAction
	 * @param actionDescription Description of the action to be taken
	 * @param actionResult Result of action execution
	 * @param status Execution status (SUCCESS, FAILED, etc.)
	 * @param errorMessage Error message if action execution failed
	 * @param toolName Tool name used for action
	 * @param subPlanCreated Whether this action created a sub-plan execution
	 */
	void recordActionResult(String currentPlanId, String rootPlanId, Long thinkActRecordId,
			Long createdThinkActRecordId, String actionDescription, String actionResult,
			String status, String errorMessage, String toolName, String toolParameters,  boolean subPlanCreated);

	/**
	 * 接口3: 记录计划完成
	 * Record plan completion. This method handles plan completion recording logic
	 * without exposing internal record objects.
	 * @param currentPlanId Current plan ID
	 * @param rootPlanId Root plan ID
	 * @param thinkActRecordId Think-act record ID for sub-plan executions (null for root plans)
	 * @param summary The summary of the plan execution
	 */
	void recordPlanCompletion(String currentPlanId, String rootPlanId, Long thinkActRecordId, String summary);


	public Long getCurrentThinkActRecordId(String currentPlanId, String rootPlanId);
}

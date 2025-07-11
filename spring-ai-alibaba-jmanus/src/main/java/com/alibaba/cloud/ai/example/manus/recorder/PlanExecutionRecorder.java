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

import com.alibaba.cloud.ai.example.manus.recorder.entity.AgentExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.PlanExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.ThinkActRecord;

/**
 * Plan execution recorder interface that defines methods for recording and retrieving
 * plan execution details.
 */
public interface PlanExecutionRecorder {

	/**
	 * Records an agent execution instance associated with a specific plan execution
	 * record
	 * @param planExecutionRecord Plan execution record
	 * @param agentRecord Agent execution record
	 * @return Agent execution ID
	 */
	Long setAgentExecution(PlanExecutionRecord planExecutionRecord, AgentExecutionRecord agentRecord);

	/**
	 * Records a think-act execution instance associated with a specific agent execution
	 * @param planExecutionRecord Plan execution record
	 * @param agentExecutionId Agent execution ID
	 * @param thinkActRecord Think-act record
	 */
	void setThinkActExecution(PlanExecutionRecord planExecutionRecord, Long agentExecutionId,
			ThinkActRecord thinkActRecord);

	/**
	 * Marks plan execution as completed
	 * @param planExecutionRecord Plan execution record
	 * @param summary Execution summary
	 */
	void setPlanCompletion(PlanExecutionRecord planExecutionRecord, String summary);

	/**
	 * Gets or creates root plan execution record
	 * @param rootPlanId Root plan ID
	 * @param createIfNotExists Whether to create if not exists
	 * @return Root plan execution record, or null if not found and createIfNotExists is false
	 */
	PlanExecutionRecord getOrCreateRootPlanExecutionRecord(String rootPlanId, boolean createIfNotExists);

	/**
	 * Gets or creates sub-plan execution record from parent plan
	 * @param parentPlan Parent plan execution record
	 * @param subPlanId Sub-plan ID
	 * @param thinkActRecordId Think-act record ID that contains the sub-plan
	 * @param createIfNotExists Whether to create if not exists
	 * @return Sub-plan execution record, or null if thinkActRecordId is null or not found and createIfNotExists is false
	 */
	PlanExecutionRecord getOrCreateSubPlanExecutionRecord(PlanExecutionRecord parentPlan, String subPlanId, 
			Long thinkActRecordId, boolean createIfNotExists);

	/**
	 * Saves the execution records for the specified plan ID to persistent storage. This
	 * method will recursively call the save methods of PlanExecutionRecord,
	 * AgentExecutionRecord, and ThinkActRecord
	 * @param planId The plan ID to save
	 * @return true if records were found and saved, false otherwise
	 */
	boolean savePlanExecutionRecords(PlanExecutionRecord planExecutionRecord);

	/**
	 * Saves all execution records to persistent storage. This method will iterate through
	 * all plan records and call their save methods
	 */
	void saveAllExecutionRecords();

	/**
	 * Gets the current active agent execution record for the specified plan execution
	 * record
	 * @param planExecutionRecord Plan execution record
	 * @return Current active agent execution record, or null if none exists
	 */
	AgentExecutionRecord getCurrentAgentExecutionRecord(PlanExecutionRecord planExecutionRecord);

	/**
	 * Removes the execution records for the specified plan ID
	 * @param planId The plan ID to remove
	 */
	void removeExecutionRecord(String planId);

	/**
	 * Record the start of step execution.
	 * @param step Execution step
	 * @param context Execution context
	 */
	void recordStepStart(com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionStep step, com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext context);

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

}

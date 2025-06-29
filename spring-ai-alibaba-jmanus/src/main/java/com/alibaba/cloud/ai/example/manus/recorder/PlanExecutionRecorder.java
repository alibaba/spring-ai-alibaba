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
	 * Records a plan execution instance and returns its unique identifier
	 * @param stepRecord Plan execution record
	 * @return Plan ID
	 */
	String recordPlanExecution(PlanExecutionRecord stepRecord);

	/**
	 * Records an agent execution instance associated with a specific plan
	 * @param planId Plan ID
	 * @param agentRecord Agent execution record
	 * @return Agent execution ID
	 */
	Long recordAgentExecution(String planId, AgentExecutionRecord agentRecord);

	/**
	 * Records a think-act execution instance associated with a specific agent execution
	 * @param planId Plan ID
	 * @param agentExecutionId Agent execution ID
	 * @param thinkActRecord Think-act record
	 */
	void recordThinkActExecution(String planId, Long agentExecutionId, ThinkActRecord thinkActRecord);

	/**
	 * Marks plan execution as completed
	 * @param planId Plan ID
	 * @param summary Execution summary
	 */
	void recordPlanCompletion(String planId, String summary);

	/**
	 * Gets plan execution record
	 * @param planId Plan ID
	 * @return Plan execution record
	 */
	PlanExecutionRecord getExecutionRecord(String planId);

	/**
	 * Saves the execution records for the specified plan ID to persistent storage. This
	 * method will recursively call the save methods of PlanExecutionRecord,
	 * AgentExecutionRecord, and ThinkActRecord
	 * @param planId The plan ID to save
	 * @return true if records were found and saved, false otherwise
	 */
	boolean savePlanExecutionRecords(String planId);

	/**
	 * Saves all execution records to persistent storage. This method will iterate through
	 * all plan records and call their save methods
	 */
	void saveAllExecutionRecords();

	/**
	 * Gets the current active agent execution record for the specified plan
	 * @param planId Plan ID
	 * @return Current active agent execution record, or null if none exists
	 */
	AgentExecutionRecord getCurrentAgentExecutionRecord(String planId);

	/**
	 * Removes the execution records for the specified plan ID
	 * @param planId The plan ID to remove
	 */
	void removeExecutionRecord(String planId);

}

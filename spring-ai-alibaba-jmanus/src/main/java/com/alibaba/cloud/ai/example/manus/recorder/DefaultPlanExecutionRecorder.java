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

import org.springframework.stereotype.Component;

import com.alibaba.cloud.ai.example.manus.recorder.entity.AgentExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.PlanExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.ThinkActRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class DefaultPlanExecutionRecorder implements PlanExecutionRecorder {

	private final Map<String, PlanExecutionRecord> planRecords = new ConcurrentHashMap<>();

	private final AtomicLong agentExecutionIdGenerator = new AtomicLong(0);

	/**
	 * Get or create PlanExecutionRecord by planId
	 * @param planId Plan ID
	 * @return PlanExecutionRecord instance
	 */
	public PlanExecutionRecord getOrCreatePlanExecutionRecord(String planId) {
		return planRecords.computeIfAbsent(planId, id -> new PlanExecutionRecord(id));
	}

	/**
	 * Get PlanExecutionRecord by planId and thinkActRecordId (for sub-plans)
	 * @param planId Parent plan ID
	 * @param thinkActRecordId Think-act record ID that contains the sub-plan
	 * @return Sub-plan execution record or null if not found
	 */
	public PlanExecutionRecord getPlanExecutionRecordByThinkActId(String planId, Long thinkActRecordId) {
		PlanExecutionRecord parentPlan = planRecords.get(planId);
		if (parentPlan != null) {
			for (AgentExecutionRecord agentRecord : parentPlan.getAgentExecutionSequence()) {
				for (ThinkActRecord thinkActRecord : agentRecord.getThinkActSteps()) {
					if (thinkActRecordId.equals(thinkActRecord.getId())) {
						return thinkActRecord.getSubPlanExecutionRecord();
					}
				}
			}
		}
		return null;
	}

	@Override
	public String recordPlanExecution(PlanExecutionRecord stepRecord) {
		String planId = stepRecord.getPlanId();
		planRecords.put(planId, stepRecord);
		return planId;
	}

	/**
	 * Record agent execution with PlanExecutionRecord parameter
	 * @param planExecutionRecord Plan execution record
	 * @param agentRecord Agent execution record
	 * @return Agent execution ID
	 */
	@Override
	public Long recordAgentExecution(PlanExecutionRecord planExecutionRecord, AgentExecutionRecord agentRecord) {
		Long agentExecutionId = agentExecutionIdGenerator.incrementAndGet();
		agentRecord.setId(agentExecutionId);
		if (planExecutionRecord != null) {
			planExecutionRecord.addAgentExecutionRecord(agentRecord);
		}
		return agentExecutionId;
	}

	/**
	 * Record think-act execution with PlanExecutionRecord parameter
	 * @param planExecutionRecord Plan execution record
	 * @param agentExecutionId Agent execution ID
	 * @param thinkActRecord Think-act record
	 */
	@Override
	public void recordThinkActExecution(PlanExecutionRecord planExecutionRecord, Long agentExecutionId, ThinkActRecord thinkActRecord) {
		if (planExecutionRecord != null) {
			for (AgentExecutionRecord agentRecord : planExecutionRecord.getAgentExecutionSequence()) {
				if (agentExecutionId.equals(agentRecord.getId())) {
					agentRecord.addThinkActStep(thinkActRecord);
					break;
				}
			}
		}
	}

	/**
	 * Record plan completion with PlanExecutionRecord parameter
	 * @param planExecutionRecord Plan execution record
	 * @param summary Execution summary
	 */
	@Override
	public void recordPlanCompletion(PlanExecutionRecord planExecutionRecord, String summary) {
		if (planExecutionRecord != null) {
			planExecutionRecord.complete(summary);
		}
	}

	@Override
	public PlanExecutionRecord getExecutionRecord(String planId, Long thinkActRecordId) {
		if (thinkActRecordId != null) {
			// This is a sub-plan, get the sub-plan record by planId + thinkActRecordId
			return getPlanExecutionRecordByThinkActId(planId, thinkActRecordId);
		} else {
			// This is a main plan
			return planRecords.get(planId);
		}
	}

	/**
	 * Save execution records of the specified plan ID to persistent storage. This method
	 * will recursively call save methods of PlanExecutionRecord, AgentExecutionRecord and
	 * ThinkActRecord
	 * @param planId Plan ID to save
	 * @return Returns true if record is found and saved, false otherwise
	 */
	@Override
	public boolean savePlanExecutionRecords(String planId) {
		PlanExecutionRecord record = getExecutionRecord(planId, null);
		if (record == null) {
			return false;
		}

		// Call save method of PlanExecutionRecord, which will recursively call save
		// methods of all sub-records
		record.save();
		return true;
	}

	/**
	 * Save all execution records to persistent storage. This method will iterate through
	 * all plan records and call their save methods
	 */
	@Override
	public void saveAllExecutionRecords() {
		for (Map.Entry<String, PlanExecutionRecord> entry : planRecords.entrySet()) {
			entry.getValue().save();
		}
	}

	@Override
	public AgentExecutionRecord getCurrentAgentExecutionRecord(String planId) {
		// Automatically clean plan records older than 30 minutes
		cleanOutdatedPlans(30);

		PlanExecutionRecord planRecord = getExecutionRecord(planId, null);
		return getCurrentAgentExecutionRecord(planRecord);
	}

	/**
	 * Clean expired plan records that exceed the specified number of minutes
	 * @param expirationMinutes Expiration time (minutes)
	 */
	private void cleanOutdatedPlans(int expirationMinutes) {
		LocalDateTime currentTime = LocalDateTime.now();

		planRecords.entrySet().removeIf(entry -> {
			PlanExecutionRecord record = entry.getValue();
			// Check if record creation time has exceeded the specified expiration time
			if (record != null && record.getStartTime() != null) {
				LocalDateTime expirationTime = record.getStartTime().plusMinutes(expirationMinutes);
				return currentTime.isAfter(expirationTime);
			}
			return false;
		});
	}

	/**
	 * Delete execution record of the specified plan ID
	 * @param planId Plan ID to delete
	 */
	@Override
	public void removeExecutionRecord(String planId) {
		planRecords.remove(planId);
	}

	/**
	 * Get all plan execution records
	 * @return Map of all plan records
	 */
	public Map<String, PlanExecutionRecord> getAllPlanRecords() {
		return new ConcurrentHashMap<>(planRecords);
	}

	/**
	 * Check if a plan execution record exists
	 * @param planId Plan ID to check
	 * @return true if exists, false otherwise
	 */
	public boolean hasPlanExecutionRecord(String planId) {
		return planRecords.containsKey(planId);
	}

	/**
	 * Get plan execution record by ID, with automatic creation if not exists
	 * @param planId Plan ID
	 * @param createIfNotExists Whether to create if not exists
	 * @return Plan execution record
	 */
	public PlanExecutionRecord getPlanExecutionRecord(String planId, boolean createIfNotExists) {
		if (createIfNotExists) {
			return getOrCreatePlanExecutionRecord(planId);
		} else {
			return planRecords.get(planId);
		}
	}

	@Override
	public PlanExecutionRecord getOrCreateSubPlanExecution(String parentPlanId, Long parentAgentExecutionId, Long thinkActRecordId, String subPlanId) {
		PlanExecutionRecord parentPlanRecord = planRecords.get(parentPlanId);
		if (parentPlanRecord != null) {
			// Find the parent agent execution record
			for (AgentExecutionRecord agentRecord : parentPlanRecord.getAgentExecutionSequence()) {
				if (parentAgentExecutionId.equals(agentRecord.getId())) {
					// Find the specific think-act record
					for (ThinkActRecord thinkActRecord : agentRecord.getThinkActSteps()) {
						if (thinkActRecordId.equals(thinkActRecord.getId())) {
							// Check if sub-plan already exists
							PlanExecutionRecord existingSubPlan = thinkActRecord.getSubPlanExecutionRecord();
							if (existingSubPlan != null) {
								return existingSubPlan;
							}
							
							// Create new sub-plan if it doesn't exist
							String actualSubPlanId = subPlanId != null ? subPlanId : 
								parentPlanId + "_sub_" + thinkActRecordId + "_" + System.currentTimeMillis();
							PlanExecutionRecord newSubPlan = new PlanExecutionRecord(actualSubPlanId);
							
							// Record the sub-plan in the think-act record
							thinkActRecord.recordSubPlanExecution(newSubPlan);
							
							// Also register the sub-plan in the main planRecords for direct access
							planRecords.put(newSubPlan.getPlanId(), newSubPlan);
							
							return newSubPlan;
						}
					}
					break;
				}
			}
		}
		return null;
	}

	@Override
	public PlanExecutionRecord getSubPlanExecution(String parentPlanId, Long parentAgentExecutionId, Long thinkActRecordId) {
		return getPlanExecutionRecordByThinkActId(parentPlanId, thinkActRecordId);
	}

	/**
	 * Get current agent execution record for a specific plan execution record
	 * @param planExecutionRecord Plan execution record
	 * @return Current active agent execution record, or null if none exists
	 */
	@Override
	public AgentExecutionRecord getCurrentAgentExecutionRecord(PlanExecutionRecord planExecutionRecord) {
		if (planExecutionRecord != null) {
			List<AgentExecutionRecord> agentExecutionSequence = planExecutionRecord.getAgentExecutionSequence();
			Integer currentIndex = planExecutionRecord.getCurrentStepIndex();
			if (!agentExecutionSequence.isEmpty() && currentIndex != null && currentIndex < agentExecutionSequence.size()) {
				return agentExecutionSequence.get(currentIndex);
			}
		}
		return null;
	}

}

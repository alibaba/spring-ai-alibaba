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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger logger = LoggerFactory.getLogger(DefaultPlanExecutionRecorder.class);

	private final Map<String, PlanExecutionRecord> planRecords = new ConcurrentHashMap<>();

	private final AtomicLong agentExecutionIdGenerator = new AtomicLong(0);

	/**
	 * Find ThinkActRecord by parent plan ID and think-act record ID
	 * @param parentPlanId Parent plan ID
	 * @param thinkActRecordId Think-act record ID
	 * @return ThinkActRecord if found, null otherwise
	 */
	private ThinkActRecord findThinkActRecord(String parentPlanId, Long thinkActRecordId) {
		if (parentPlanId == null || thinkActRecordId == null) {
			return null;
		}
		
		PlanExecutionRecord parentPlan = planRecords.get(parentPlanId);
		if (parentPlan != null) {
			for (AgentExecutionRecord agentRecord : parentPlan.getAgentExecutionSequence()) {
				for (ThinkActRecord thinkActRecord : agentRecord.getThinkActSteps()) {
					if (thinkActRecordId.equals(thinkActRecord.getId())) {
						return thinkActRecord;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Get or create PlanExecutionRecord by planId and optional thinkActRecordId
	 * @param planId Plan ID
	 * @param thinkActRecordId Think-act record ID that contains the sub-plan (null for main plan)
	 * @param createIfNotExists Whether to create if not exists
	 * @return PlanExecutionRecord instance
	 */
	public PlanExecutionRecord getOrCreatePlanExecutionRecord(String planId, Long thinkActRecordId, boolean createIfNotExists) {
		if (thinkActRecordId == null) {
			// This is a main plan
			if (createIfNotExists) {
				PlanExecutionRecord mainPlan = planRecords.computeIfAbsent(planId, id -> {
					logger.info("Creating main plan with ID: {}", id);
					return new PlanExecutionRecord(id);
				});
				
				// Additional validation for main plan
				if (!mainPlan.getPlanId().equals(planId)) {
					logger.error("CRITICAL ERROR: Main plan ID mismatch. Expected: {}, Actual: {}", planId, mainPlan.getPlanId());
					throw new RuntimeException("Main plan ID mismatch");
				}
				
				return mainPlan;
			} else {
				return planRecords.get(planId);
			}
		} else {
			// This is a sub-plan, find or create it
			ThinkActRecord thinkActRecord = findThinkActRecord(planId, thinkActRecordId);
			if (thinkActRecord != null) {
				PlanExecutionRecord subPlan = thinkActRecord.getSubPlanExecutionRecord();
				if (subPlan == null && createIfNotExists) {
					// Create new sub-plan if it doesn't exist with unique ID
					// Use UUID to ensure absolute uniqueness and avoid any collision with parent plan ID
					String subPlanId;
					int retryCount = 0;
					do {
						subPlanId = "sub_" + planId + "_" + thinkActRecordId + "_" + System.nanoTime() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8);
						retryCount++;
						// Safety check to prevent infinite loop
						if (retryCount > 10) {
							throw new RuntimeException("Failed to generate unique sub-plan ID after 10 attempts");
						}
					} while (planRecords.containsKey(subPlanId) || subPlanId.equals(planId));
					
					subPlan = new PlanExecutionRecord(subPlanId);
					// Set parent plan information
					subPlan.setParentPlanId(planId);
					subPlan.setThinkActRecordId(thinkActRecordId);
					// Record the sub-plan in the think-act record
					thinkActRecord.recordSubPlanExecution(subPlan);
					// Also register the sub-plan in the main planRecords for direct access
					planRecords.put(subPlan.getPlanId(), subPlan);
					
					// Add logging to ensure unique sub-plan ID generation
					logger.info("Created sub-plan with unique ID: {} for parent plan: {}, thinkActRecordId: {}", 
						subPlanId, planId, thinkActRecordId);
						
					// Additional validation
					if (subPlanId.equals(planId)) {
						logger.error("CRITICAL ERROR: Sub-plan ID {} is identical to parent plan ID {}", subPlanId, planId);
						throw new RuntimeException("Sub-plan ID cannot be identical to parent plan ID");
					}
				}
				return subPlan;
			}
			return null;
		}
	}

	/**
	 * Get or create PlanExecutionRecord by planId and optional thinkActRecordId (creates if not exists)
	 * @param planId Plan ID
	 * @param thinkActRecordId Think-act record ID that contains the sub-plan (null for main plan)
	 * @return PlanExecutionRecord instance
	 */
	public PlanExecutionRecord getOrCreatePlanExecutionRecord(String planId, Long thinkActRecordId) {
		return getOrCreatePlanExecutionRecord(planId, thinkActRecordId, true);
	}

	@Override
	public String recordPlanExecution(PlanExecutionRecord stepRecord) {
		String planId = stepRecord.getPlanId();
		String parentPlanId = stepRecord.getParentPlanId();
		Long thinkActRecordId = stepRecord.getThinkActRecordId();
		
		if (parentPlanId != null && thinkActRecordId != null) {
			// This is a sub-plan, need to attach it to the corresponding think-act record
			ThinkActRecord thinkActRecord = findThinkActRecord(parentPlanId, thinkActRecordId);
			if (thinkActRecord != null) {
				// Found the corresponding think-act record, attach the sub-plan
				thinkActRecord.recordSubPlanExecution(stepRecord);
			}
		}
		
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
		return getOrCreatePlanExecutionRecord(planId, thinkActRecordId, false);
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

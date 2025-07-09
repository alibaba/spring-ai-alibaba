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
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import com.alibaba.cloud.ai.example.manus.recorder.entity.AgentExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.PlanExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.ThinkActRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

//@Component
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
	 * @param rootPlanId Root plan ID
	 * @param thinkActRecordId Think-act record ID that contains the sub-plan (null for
	 * main plan)
	 * @param createIfNotExists Whether to create if not exists
	 * @return PlanExecutionRecord instance
	 */
	public PlanExecutionRecord getOrCreatePlanExecutionRecord(String planId, String rootPlanId, Long thinkActRecordId,
			boolean createIfNotExists) {

		// Add detailed logging to debug NPE
		logger.info(
				"Enter getOrCreatePlanExecutionRecord with planId: {}, rootPlanId: {}, thinkActRecordId: {}, createIfNotExists: {}",
				planId, rootPlanId, thinkActRecordId, createIfNotExists);

		if (rootPlanId == null) {
			logger.error("rootPlanId is null, which will cause NPE. PlanId: {}, thinkActRecordId: {}.", planId,
					thinkActRecordId);
			// For further debugging, log the stack trace to understand the call path
			// throw new IllegalArgumentException("rootPlanId cannot be null");
		}

		// Get or create root plan record first
		PlanExecutionRecord rootRecord = planRecords.computeIfAbsent(rootPlanId, id -> {
			logger.info("Creating root plan with ID: {}", id);
			return new PlanExecutionRecord(id, rootPlanId);
		});

		// If no thinkActRecordId, return root record directly
		if (thinkActRecordId == null) {
			return rootRecord;
		}

		// Find ThinkActRecord in root plan
		ThinkActRecord thinkActRecord = findThinkActRecord(rootPlanId, thinkActRecordId);
		if (thinkActRecord == null) {
			return rootRecord;
		}

		// Check if subPlanExecutionRecord exists
		PlanExecutionRecord subPlan = thinkActRecord.getSubPlanExecutionRecord();
		if (subPlan == null && createIfNotExists) {
			// Create new sub-plan with planId and rootPlanId
			subPlan = new PlanExecutionRecord(planId, rootPlanId);
			subPlan.setThinkActRecordId(thinkActRecordId);
			thinkActRecord.recordSubPlanExecution(subPlan);
		}

		return subPlan != null ? subPlan : rootRecord;
	}

	/**
	 * Get or create PlanExecutionRecord by planId and optional thinkActRecordId (creates
	 * if not exists)
	 * @param planId Plan ID
	 * @param thinkActRecordId Think-act record ID that contains the sub-plan (null for
	 * main plan)
	 * @return PlanExecutionRecord instance
	 */
	public PlanExecutionRecord getOrCreatePlanExecutionRecord(String planId, String rootPlanId, Long thinkActRecordId) {
		return getOrCreatePlanExecutionRecord(planId, rootPlanId, thinkActRecordId, true);
	}

	/**
	 * 记录计划执行情况的方法。
	 *
	 * <p>该方法用于记录执行的计划。如果当前记录是一个子计划，则将其附加到相应的思维—行动记录上。
	 * 通过获取计划ID、根计划ID和思维—行动记录ID，判断当前记录是否为子计划。
	 * 如果是子计划，则查找相应的思维—行动记录，并将子计划的执行情况记录到该思维—行动记录中。
	 *
	 * @param stepRecord 执行的计划记录对象，包含当前计划ID、根计划ID和思维—行动记录ID。
	 * @return 当前计划的ID，如果没有找到相应的思维—行动记录，则返回的ID可能仍然有效。
	 */
	@Override
	public String recordPlanExecution(PlanExecutionRecord stepRecord) {
		String planId = stepRecord.getCurrentPlanId();
		String parentPlanId = stepRecord.getRootPlanId();
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
		Long agentExecutionId = agentRecord.getId();
		if(agentRecord.getId() == null){
			agentExecutionId = agentExecutionIdGenerator.incrementAndGet();
			agentRecord.setId(agentExecutionId);
			if (planExecutionRecord != null) {
				planExecutionRecord.addAgentExecutionRecord(agentRecord);
			}
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
	public void recordThinkActExecution(PlanExecutionRecord planExecutionRecord, Long agentExecutionId,
			ThinkActRecord thinkActRecord) {
		if (planExecutionRecord != null) {
			for (AgentExecutionRecord agentRecord : planExecutionRecord.getAgentExecutionSequence()) {
				if (agentExecutionId.equals(agentRecord.getId())) {
//					agentRecord.addThinkActStep(thinkActRecord);
					addThinkActStep(agentRecord, thinkActRecord);
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

	/**
	 * 获取计划执行记录
	 *
	 * <p>根据给定的计划ID、根计划ID和思维行为记录ID，获取或创建对应的计划执行记录。
	 *
	 * @param planId 计划的唯一标识符
	 * @param rootPlanId 根计划的唯一标识符
	 * @param thinkActRecordId 思维行为记录的唯一标识符
	 * @return 返回对应的计划执行记录，如果无法创建记录则返回null
	 */
	@Override
	public PlanExecutionRecord getExecutionRecord(String planId, String rootPlanId, Long thinkActRecordId) {
		return getOrCreatePlanExecutionRecord(planId, rootPlanId, thinkActRecordId, false);
	}

	/**
	 * Save execution records of the specified plan ID to persistent storage. This method
	 * will recursively call save methods of PlanExecutionRecord, AgentExecutionRecord and
	 * ThinkActRecord
	 * @param planId Plan ID to save
	 * @return Returns true if record is found and saved, false otherwise
	 */
	@Override
	public boolean savePlanExecutionRecords(String rootPlanId) {
		PlanExecutionRecord record = getExecutionRecord(null, rootPlanId, null);
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
			if (!agentExecutionSequence.isEmpty() && currentIndex != null
					&& currentIndex < agentExecutionSequence.size()) {
				return agentExecutionSequence.get(currentIndex);
			}
		}
		return null;
	}

	private void addThinkActStep(AgentExecutionRecord agentRecord, ThinkActRecord thinkActRecord) {
		if (agentRecord.getThinkActSteps() == null) {
			agentRecord.addThinkActStep(thinkActRecord);
			return;
		}
		//会多次调用，因此需要根据id修改
		ThinkActRecord exist = agentRecord.getThinkActSteps().stream().filter(r -> r.getId().equals(thinkActRecord.getId())).findFirst().orElse(null);
		if(exist == null){
			agentRecord.getThinkActSteps().add(thinkActRecord);
		}else {
			BeanUtils.copyProperties(thinkActRecord, exist);
		}
	}
}

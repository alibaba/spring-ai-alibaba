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

	@Override
	public String recordPlanExecution(PlanExecutionRecord stepRecord) {
		String planId = stepRecord.getPlanId();
		planRecords.put(planId, stepRecord);
		return planId;
	}

	@Override
	public Long recordAgentExecution(String planId, AgentExecutionRecord agentRecord) {
		Long agentExecutionId = agentExecutionIdGenerator.incrementAndGet();
		agentRecord.setId(agentExecutionId);
		PlanExecutionRecord planRecord = planRecords.get(planId);
		if (planRecord != null) {
			planRecord.addAgentExecutionRecord(agentRecord);
		}
		return agentExecutionId;
	}

	@Override
	public void recordThinkActExecution(String planId, Long agentExecutionId, ThinkActRecord thinkActRecord) {
		PlanExecutionRecord planRecord = planRecords.get(planId);
		if (planRecord != null) {
			for (AgentExecutionRecord agentRecord : planRecord.getAgentExecutionSequence()) {
				if (agentExecutionId.equals(agentRecord.getId())) {
					agentRecord.addThinkActStep(thinkActRecord);
					break;
				}
			}
		}
	}

	@Override
	public void recordPlanCompletion(String planId, String summary) {
		PlanExecutionRecord record = planRecords.get(planId);
		if (record != null) {
			record.complete(summary);
		}
	}

	@Override
	public PlanExecutionRecord getExecutionRecord(String planId) {
		return planRecords.get(planId);
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
		PlanExecutionRecord record = planRecords.get(planId);
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

		PlanExecutionRecord planRecord = planRecords.get(planId);
		if (planRecord != null) {
			List<AgentExecutionRecord> agentExecutionSequence = planRecord.getAgentExecutionSequence();
			int currentIndex = planRecord.getCurrentStepIndex();
			if (!agentExecutionSequence.isEmpty()) {
				return agentExecutionSequence.get(currentIndex);
			}
		}
		return null;
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

}

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
	 * 将指定计划ID的执行记录保存到持久化存储 此方法会递归调用 PlanExecutionRecord、AgentExecutionRecord 和
	 * ThinkActRecord 的 save 方法
	 * @param planId 要保存的计划ID
	 * @return 如果找到并保存了记录则返回 true，否则返回 false
	 */
	@Override
	public boolean savePlanExecutionRecords(String planId) {
		PlanExecutionRecord record = planRecords.get(planId);
		if (record == null) {
			return false;
		}

		// 调用 PlanExecutionRecord 的 save 方法，它会递归调用所有子记录的 save 方法
		record.save();
		return true;
	}

	/**
	 * 将所有执行记录保存到持久化存储 此方法会遍历所有计划记录并调用它们的 save 方法
	 */
	@Override
	public void saveAllExecutionRecords() {
		for (Map.Entry<String, PlanExecutionRecord> entry : planRecords.entrySet()) {
			entry.getValue().save();
		}
	}

	@Override
	public AgentExecutionRecord getCurrentAgentExecutionRecord(String planId) {
		// 自动清理超过30分钟的计划记录
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
	 * 清理超过指定分钟数的过期计划记录
	 * @param expirationMinutes 过期时间（分钟）
	 */
	private void cleanOutdatedPlans(int expirationMinutes) {
		LocalDateTime currentTime = LocalDateTime.now();

		planRecords.entrySet().removeIf(entry -> {
			PlanExecutionRecord record = entry.getValue();
			// 检查记录创建时间是否超过了指定的过期时间
			if (record != null && record.getStartTime() != null) {
				LocalDateTime expirationTime = record.getStartTime().plusMinutes(expirationMinutes);
				return currentTime.isAfter(expirationTime);
			}
			return false;
		});
	}

	/**
	 * 删除指定计划ID的执行记录
	 * @param planId 要删除的计划ID
	 */
	@Override
	public void removeExecutionRecord(String planId) {
		planRecords.remove(planId);
	}

}

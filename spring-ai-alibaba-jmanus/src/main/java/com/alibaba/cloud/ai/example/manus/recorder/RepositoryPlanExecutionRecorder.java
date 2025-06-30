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
import com.alibaba.cloud.ai.example.manus.recorder.entity.PlanExecutionRecordEntity;
import com.alibaba.cloud.ai.example.manus.recorder.entity.ThinkActRecord;
import com.alibaba.cloud.ai.example.manus.recorder.repository.PlanExecutionRecordRepository;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存PlanExecutionRecorder 无法在分布式环境下使用，因此需要使用DB持久化PlanExecutionRecord
 *
 * 解决的问题可参考: https://github.com/alibaba/spring-ai-alibaba/issues/1391
 */
@Component
public class RepositoryPlanExecutionRecorder implements PlanExecutionRecorder {

	private final AtomicLong agentExecutionIdGenerator = new AtomicLong(0);

	@Resource
	private PlanExecutionRecordRepository planExecutionRecordRepository;

	@Override
	public String recordPlanExecution(PlanExecutionRecord stepRecord) {
		String planId = stepRecord.getPlanId();
		// 会重复进来，因此，这个地方必须同时支持保存和更新
		PlanExecutionRecordEntity entity = planExecutionRecordRepository.findByPlanId(planId);
		if (entity == null) {
			entity = new PlanExecutionRecordEntity();
			entity.setPlanId(planId);
			entity.setGmtCreate(new Date());
		}

		entity.setPlanExecutionRecord(stepRecord);
		entity.setGmtModified(new Date());
		planExecutionRecordRepository.save(entity);

		return planId;
	}

	@Override
	public Long recordAgentExecution(String planId, AgentExecutionRecord agentRecord) {
		Long agentExecutionId = agentExecutionIdGenerator.incrementAndGet();
		agentRecord.setId(agentExecutionId);
		PlanExecutionRecordEntity planRecordEntity = planExecutionRecordRepository.findByPlanId(planId);
		if (planRecordEntity == null || planRecordEntity.getPlanExecutionRecord() == null) {
			return agentExecutionId;
		}

		PlanExecutionRecord planRecord = planRecordEntity.getPlanExecutionRecord();
		planRecord.addAgentExecutionRecord(agentRecord);
		planExecutionRecordRepository.save(planRecordEntity);
		return agentExecutionId;
	}

	@Override
	public void recordThinkActExecution(String planId, Long agentExecutionId, ThinkActRecord thinkActRecord) {
		PlanExecutionRecordEntity planRecordEntity = planExecutionRecordRepository.findByPlanId(planId);
		if (planRecordEntity == null || planRecordEntity.getPlanExecutionRecord() == null) {
			return;
		}

		PlanExecutionRecord planRecord = planRecordEntity.getPlanExecutionRecord();
		planRecordEntity.setGmtModified(new Date());
		for (AgentExecutionRecord agentRecord : planRecord.getAgentExecutionSequence()) {
			if (agentExecutionId.equals(agentRecord.getId())) {
				addThinkActStep(agentRecord, thinkActRecord);

				planExecutionRecordRepository.save(planRecordEntity);
				break;
			}
		}
	}

	@Override
	public void recordPlanCompletion(String planId, String summary) {
		PlanExecutionRecordEntity planRecordEntity = planExecutionRecordRepository.findByPlanId(planId);
		if (planRecordEntity == null || planRecordEntity.getPlanExecutionRecord() == null) {
			return;
		}

		planRecordEntity.setGmtModified(new Date());
		if (planRecordEntity != null && planRecordEntity.getPlanExecutionRecord() != null) {
			planRecordEntity.getPlanExecutionRecord().complete(summary);
			planExecutionRecordRepository.save(planRecordEntity);
		}
	}

	@Override
	public PlanExecutionRecord getExecutionRecord(String planId) {
		PlanExecutionRecordEntity entity = planExecutionRecordRepository.findByPlanId(planId);
		return entity != null ? entity.getPlanExecutionRecord() : null;
	}

	@Override
	public boolean savePlanExecutionRecords(String planId) {
		return false;
	}

	@Override
	public void saveAllExecutionRecords() {

	}

	@Override
	public AgentExecutionRecord getCurrentAgentExecutionRecord(String planId) {
		PlanExecutionRecord planRecord = getExecutionRecord(planId);
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
	 * 删除指定计划ID的执行记录
	 * @param planId 要删除的计划ID
	 */
	@Override
	public void removeExecutionRecord(String planId) {
		planExecutionRecordRepository.deleteByPlanId(planId);
	}

	private void addThinkActStep(AgentExecutionRecord agentRecord, ThinkActRecord thinkActRecord) {
		if (agentRecord.getThinkActSteps() == null) {
			agentRecord.addThinkActStep(thinkActRecord);
			return;
		}
		// 会多次调用，因此需要根据id修改
		ThinkActRecord exist = agentRecord.getThinkActSteps()
			.stream()
			.filter(r -> r.getId().equals(thinkActRecord.getId()))
			.findFirst()
			.orElse(null);
		if (exist == null) {
			agentRecord.getThinkActSteps().add(thinkActRecord);
		}
		else {
			BeanUtils.copyProperties(thinkActRecord, exist);
		}
	}

}

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

import com.alibaba.cloud.ai.example.manus.agent.AgentState;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionStep;
import com.alibaba.cloud.ai.example.manus.recorder.entity.vo.AgentExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.vo.ExecutionStatus;
import com.alibaba.cloud.ai.example.manus.recorder.entity.vo.PlanExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.vo.PlanExecutionRecordEntity;
import com.alibaba.cloud.ai.example.manus.recorder.entity.vo.ThinkActRecord;
import com.alibaba.cloud.ai.example.manus.recorder.repository.PlanExecutionRecordRepository;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Repository-based PlanExecutionRecorder implementation for persistent storage. Each
 * PlanExecutionRecord only tracks its own currentPlanId execution. RootPlanId concept is
 * preserved in context but not in record objects.
 *
 * fix feature: https://github.com/alibaba/spring-ai-alibaba/issues/1391
 */
@Component
public class RepositoryPlanExecutionRecorder implements PlanExecutionRecorder {

	private static final Logger logger = LoggerFactory.getLogger(RepositoryPlanExecutionRecorder.class);

	@Resource
	private PlanExecutionRecordRepository planExecutionRecordRepository;

	/**
	 * Record think-act execution with PlanExecutionRecord parameter
	 * @param planExecutionRecord Plan execution record
	 * @param agentExecutionId Agent execution ID
	 * @param thinkActRecord Think-act record
	 */
	@Override
	public void setThinkActExecution(PlanExecutionRecord planExecutionRecord, Long agentExecutionId,
			ThinkActRecord thinkActRecord) {
		if (planExecutionRecord != null) {
			for (AgentExecutionRecord agentRecord : planExecutionRecord.getAgentExecutionSequence()) {
				if (agentExecutionId.equals(agentRecord.getId())) {
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
	public void setPlanCompletion(PlanExecutionRecord planExecutionRecord, String summary) {
		if (planExecutionRecord != null) {
			planExecutionRecord.complete(summary);
		}
	}

	/**
	 * Save execution records to persistent storage
	 * @param planExecutionRecord Plan execution record to save
	 * @return true if save was successful
	 */
	@Override
	public boolean savePlanExecutionRecords(PlanExecutionRecord planExecutionRecord) {
		saveExecutionRecord(planExecutionRecord);
		return true;
	}

	/**
	 * Delete execution record of the specified plan ID
	 * @param planId Plan ID to delete
	 */
	@Override
	public void removeExecutionRecord(String planId) {
		planExecutionRecordRepository.deleteByPlanId(planId);
	}

	/**
	 * Record the start of plan execution.
	 */
	public void recordPlanExecutionStart(ExecutionContext context) {
		String currentPlanId = context.getPlan().getCurrentPlanId();
		PlanExecutionRecord record = getOrCreatePlanExecutionRecord(currentPlanId, true);

		record.setCurrentPlanId(currentPlanId);
		record.setStartTime(LocalDateTime.now());
		record.setTitle(context.getPlan().getTitle());
		record.setUserRequest(context.getUserRequest());
		retrieveExecutionSteps(context, record);

		savePlanExecutionRecords(record);
	}

	/**
	 * Record the start of step execution.
	 */
	public void recordStepStart(ExecutionStep step, ExecutionContext context) {
		String currentPlanId = context.getPlan().getCurrentPlanId();
		PlanExecutionRecord record = getOrCreatePlanExecutionRecord(currentPlanId, true);

		if (record != null) {
			int currentStepIndex = step.getStepIndex();
			record.setCurrentStepIndex(currentStepIndex);
			retrieveExecutionSteps(context, record);

			List<AgentExecutionRecord> agentExecutionSequence = record.getAgentExecutionSequence();
			AgentExecutionRecord agentExecutionRecord;

			if (agentExecutionSequence.size() > currentStepIndex) {
				agentExecutionRecord = agentExecutionSequence.get(currentStepIndex);
			}
			else {
				// Create and add new AgentExecutionRecord
				agentExecutionRecord = new AgentExecutionRecord(currentPlanId, null, null);
				// Fill up to currentStepIndex
				while (agentExecutionSequence.size() < currentStepIndex) {
					agentExecutionSequence.add(new AgentExecutionRecord());
				}
				agentExecutionSequence.add(agentExecutionRecord);
			}
			agentExecutionRecord.setStatus(ExecutionStatus.RUNNING);

			savePlanExecutionRecords(record);
		}
	}

	/**
	 * Record the end of step execution.
	 */
	public void recordStepEnd(ExecutionStep step, ExecutionContext context) {
		String currentPlanId = context.getPlan().getCurrentPlanId();
		PlanExecutionRecord record = getOrCreatePlanExecutionRecord(currentPlanId, true);

		if (record != null) {
			int currentStepIndex = step.getStepIndex();
			record.setCurrentStepIndex(currentStepIndex);
			retrieveExecutionSteps(context, record);

			List<AgentExecutionRecord> agentExecutionSequence = record.getAgentExecutionSequence();
			// Check boundaries to ensure agentExecutionSequence has enough elements
			if (agentExecutionSequence.size() > currentStepIndex) {
				AgentExecutionRecord agentExecutionRecord = agentExecutionSequence.get(currentStepIndex);
				agentExecutionRecord.setStatus(
						step.getStatus() == AgentState.COMPLETED ? ExecutionStatus.FINISHED : ExecutionStatus.RUNNING);
			}
			else {
				// If there is no corresponding AgentExecutionRecord, create a new one
				AgentExecutionRecord agentExecutionRecord = new AgentExecutionRecord(currentPlanId, null, null);
				agentExecutionRecord.setStatus(
						step.getStatus() == AgentState.COMPLETED ? ExecutionStatus.FINISHED : ExecutionStatus.RUNNING);

				// Fill up to currentStepIndex
				while (agentExecutionSequence.size() < currentStepIndex) {
					agentExecutionSequence.add(new AgentExecutionRecord());
				}
				agentExecutionSequence.add(agentExecutionRecord);
			}

			savePlanExecutionRecords(record);
		}
	}

	/**
	 * Record complete agent execution at the end.
	 */
	@Override
	public void recordCompleteAgentExecution(PlanExecutionParams params) {
		if (params == null || params.getCurrentPlanId() == null) {
			return;
		}

		// Create agent execution record with all the final data
		AgentExecutionRecord agentRecord = new AgentExecutionRecord(params.getCurrentPlanId(), params.getAgentName(),
				params.getAgentDescription());
		agentRecord.setMaxSteps(params.getMaxSteps());
		agentRecord.setCurrentStep(params.getActualSteps());
		agentRecord.setErrorMessage(params.getErrorMessage());
		agentRecord.setResult(params.getResult());
		agentRecord.setStartTime(params.getStartTime());
		agentRecord.setEndTime(params.getEndTime());
		agentRecord.setStatus(params.getStatus());

		PlanExecutionRecord planRecord = getOrCreatePlanExecutionRecord(params.getCurrentPlanId(), true);
		if (planRecord != null) {
			setAgentExecution(planRecord, agentRecord);
			savePlanExecutionRecords(planRecord);
		}
	}

	/**
	 * Record thinking and action execution process.
	 */
	@Override
	public Long recordThinkingAndAction(PlanExecutionParams params) {
		if (params.getCurrentPlanId() == null) {
			return null;
		}

		PlanExecutionRecord planExecutionRecord = getOrCreatePlanExecutionRecord(params.getCurrentPlanId(), true);
		AgentExecutionRecord agentExecutionRecord = getCurrentAgentExecutionRecord(planExecutionRecord);

		if (agentExecutionRecord == null && planExecutionRecord != null) {
			agentExecutionRecord = new AgentExecutionRecord(params.getCurrentPlanId(), params.getAgentName(),
					params.getAgentDescription());
			setAgentExecution(planExecutionRecord, agentExecutionRecord);
		}

		if (agentExecutionRecord == null) {
			logger.error("Failed to create or retrieve AgentExecutionRecord for plan: {}", params.getCurrentPlanId());
			return null;
		}

		ThinkActRecord thinkActRecord = new ThinkActRecord(agentExecutionRecord.getId());
		thinkActRecord.setActStartTime(LocalDateTime.now());

		if (params.getModelName() != null) {
			planExecutionRecord.setModelName(params.getModelName());
			agentExecutionRecord.setModelName(params.getModelName());
		}

		if (params.getThinkInput() != null) {
			thinkActRecord.startThinking(params.getThinkInput());
		}
		if (params.getThinkOutput() != null) {
			thinkActRecord.finishThinking(params.getThinkOutput());
		}

		if (params.isActionNeeded() && params.getToolName() != null) {
			thinkActRecord.setActionNeeded(true);
			thinkActRecord.setToolName(params.getToolName());
			thinkActRecord.setToolParameters(params.getToolParameters());
			thinkActRecord.setStatus(ExecutionStatus.RUNNING);
		}

		if (params.getErrorMessage() != null) {
			thinkActRecord.recordError(params.getErrorMessage());
		}

		if (planExecutionRecord != null) {
			setThinkActExecution(planExecutionRecord, agentExecutionRecord.getId(), thinkActRecord);
			savePlanExecutionRecords(planExecutionRecord);
		}

		return thinkActRecord.getId();
	}

	/**
	 * Record action execution result.
	 */
	@Override
	public void recordActionResult(PlanExecutionParams params) {
		if (params.getCurrentPlanId() == null || params.getCreatedThinkActRecordId() == null) {
			return;
		}

		PlanExecutionRecord planExecutionRecord = getOrCreatePlanExecutionRecord(params.getCurrentPlanId(), true);
		AgentExecutionRecord agentExecutionRecord = getCurrentAgentExecutionRecord(planExecutionRecord);

		// Additional safety check
		if (agentExecutionRecord == null) {
			logger.error("Failed to retrieve AgentExecutionRecord for plan: {} in recordActionResult",
					params.getCurrentPlanId());
			return;
		}

		// Find the ThinkActRecord by ID and update it with action results
		ThinkActRecord thinkActRecord = findThinkActRecordInPlan(planExecutionRecord,
				params.getCreatedThinkActRecordId());

		if (thinkActRecord != null) {
			// Record action start if not already recorded
			if (params.getActionDescription() != null && params.getToolName() != null) {
				thinkActRecord.startAction(params.getActionDescription(), params.getToolName(),
						params.getToolParameters());
			}

			// Record action completion
			if (params.getActionResult() != null && params.getStatus() != null) {
				thinkActRecord.finishAction(params.getActionResult(), params.getStatus());
			}

			// Record error if any
			if (params.getErrorMessage() != null) {
				thinkActRecord.recordError(params.getErrorMessage());
			}

			// Set actToolInfoList if available
			if (params.getActToolInfoList() != null) {
				thinkActRecord.setActToolInfoList(params.getActToolInfoList());
			}

			// Set think-act execution to update the record
			setThinkActExecution(planExecutionRecord, agentExecutionRecord.getId(), thinkActRecord);

			// Save the execution records
			savePlanExecutionRecords(planExecutionRecord);
		}
		else {
			logger.warn("ThinkActRecord not found with ID: {} for plan: {}", params.getCreatedThinkActRecordId(),
					params.getCurrentPlanId());
		}
	}

	/**
	 * Record plan completion.
	 */
	@Override
	public void recordPlanCompletion(String currentPlanId, Long thinkActRecordId, String summary) {
		if (currentPlanId == null) {
			return;
		}

		PlanExecutionRecord planExecutionRecord = getOrCreatePlanExecutionRecord(currentPlanId, false);
		if (planExecutionRecord != null) {
			setPlanCompletion(planExecutionRecord, summary);
			savePlanExecutionRecords(planExecutionRecord);
		}

		logger.info("Plan completed with ID: {} and summary: {}", currentPlanId, summary);
	}

	@Override
	public PlanExecutionRecord getRootPlanExecutionRecord(String rootPlanId) {
		// For backward compatibility, treat rootPlanId as currentPlanId
		if (rootPlanId == null) {
			logger.warn("rootPlanId is null, cannot retrieve plan execution record");
			return null;
		}
		return getOrCreatePlanExecutionRecord(rootPlanId, false);
	}

	/**
	 * Gets or creates plan execution record for currentPlanId
	 * @param currentPlanId Current plan ID
	 * @param createIfNotExists Whether to create if not exists
	 * @return Plan execution record, or null if not found and createIfNotExists is false
	 */
	private PlanExecutionRecord getOrCreatePlanExecutionRecord(String currentPlanId, boolean createIfNotExists) {
		logger.debug("Enter getOrCreatePlanExecutionRecord with currentPlanId: {}, createIfNotExists: {}",
				currentPlanId, createIfNotExists);

		if (currentPlanId == null) {
			logger.error("currentPlanId is null");
			return null;
		}

		// Get existing plan record
		PlanExecutionRecord record = getExecutionRecord(currentPlanId);

		// Create if not exists and createIfNotExists is true
		if (record == null && createIfNotExists) {
			logger.info("Creating plan with ID: {}", currentPlanId);
			record = new PlanExecutionRecord(currentPlanId);
		}

		return record;
	}

	/**
	 * Helper method to find ThinkActRecord in a plan
	 * @param plan Plan execution record
	 * @param thinkActRecordId Think-act record ID
	 * @return ThinkActRecord if found, null otherwise
	 */
	private ThinkActRecord findThinkActRecordInPlan(PlanExecutionRecord plan, Long thinkActRecordId) {
		if (plan == null || thinkActRecordId == null) {
			return null;
		}

		for (AgentExecutionRecord agentRecord : plan.getAgentExecutionSequence()) {
			for (ThinkActRecord thinkActRecord : agentRecord.getThinkActSteps()) {
				if (thinkActRecordId.equals(thinkActRecord.getId())) {
					return thinkActRecord;
				}
			}
		}
		return null;
	}

	/**
	 * Get current agent execution record for a specific plan execution record
	 * @param planExecutionRecord Plan execution record
	 * @return Current active agent execution record, or null if none exists
	 */
	private AgentExecutionRecord getCurrentAgentExecutionRecord(PlanExecutionRecord planExecutionRecord) {
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

	private PlanExecutionRecord getExecutionRecord(String currentPlanId) {
		PlanExecutionRecordEntity entity = planExecutionRecordRepository.findByPlanId(currentPlanId);
		return entity != null ? entity.getPlanExecutionRecord() : null;
	}

	private void saveExecutionRecord(PlanExecutionRecord planExecutionRecord) {
		PlanExecutionRecordEntity entity = planExecutionRecordRepository
			.findByPlanId(planExecutionRecord.getCurrentPlanId());
		if (entity == null) {
			entity = new PlanExecutionRecordEntity();
			entity.setPlanId(planExecutionRecord.getCurrentPlanId());
			entity.setGmtCreate(new Date());
		}

		entity.setPlanExecutionRecord(planExecutionRecord);
		entity.setGmtModified(new Date());

		planExecutionRecordRepository.save(entity);
	}

	private void addThinkActStep(AgentExecutionRecord agentRecord, ThinkActRecord thinkActRecord) {
		if (agentRecord.getThinkActSteps() == null) {
			agentRecord.addThinkActStep(thinkActRecord);
			return;
		}
		// Will be called multiple times, so need to modify based on id
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

	/**
	 * Retrieve execution step information and set it to the record.
	 */
	private void retrieveExecutionSteps(ExecutionContext context, PlanExecutionRecord record) {
		List<String> steps = new ArrayList<>();
		for (ExecutionStep step : context.getPlan().getAllSteps()) {
			steps.add(step.getStepInStr());
		}
		record.setSteps(steps);
	}

	/**
	 * Record agent execution with PlanExecutionRecord parameter
	 * @param planExecutionRecord Plan execution record
	 * @param agentRecord Agent execution record
	 * @return Agent execution ID
	 */
	private Long setAgentExecution(PlanExecutionRecord planExecutionRecord, AgentExecutionRecord agentRecord) {
		if (planExecutionRecord != null) {
			planExecutionRecord.addAgentExecutionRecord(agentRecord);
		}
		return agentRecord.getId();
	}

	/**
	 * Get current think-act record ID
	 * @param currentPlanId Current plan ID
	 * @return Current think-act record ID, returns null if none exists
	 */
	@Override
	public Long getCurrentThinkActRecordId(String currentPlanId) {
		try {
			PlanExecutionRecord planExecutionRecord = getOrCreatePlanExecutionRecord(currentPlanId, false);
			if (planExecutionRecord != null) {
				AgentExecutionRecord currentAgentRecord = getCurrentAgentExecutionRecord(planExecutionRecord);
				if (currentAgentRecord != null && currentAgentRecord.getThinkActSteps() != null
						&& !currentAgentRecord.getThinkActSteps().isEmpty()) {
					List<ThinkActRecord> steps = currentAgentRecord.getThinkActSteps();
					ThinkActRecord lastStep = steps.get(steps.size() - 1);
					return lastStep.getId();
				}
			}
		}
		catch (Exception e) {
			logger.warn("Failed to get current think-act record ID: {}", e.getMessage());
		}

		return null;
	}

}

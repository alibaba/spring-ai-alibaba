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
import com.alibaba.cloud.ai.example.manus.recorder.entity.AgentExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.PlanExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.PlanExecutionRecordEntity;
import com.alibaba.cloud.ai.example.manus.recorder.entity.ThinkActRecord;
import com.alibaba.cloud.ai.example.manus.recorder.repository.PlanExecutionRecordRepository;
import com.alibaba.cloud.ai.example.manus.recorder.entity.ExecutionStatus;
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
 * The in-memory PlanExecutionRecorder cannot be used in a distributed environment, so it
 * is necessary to use DB persistence for PlanExecutionRecord.
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

					updateThinkActRecord(planExecutionRecord, thinkActRecord);
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
	 * Save execution records of the specified plan ID to persistent storage. This method
	 * will recursively call save methods of PlanExecutionRecord, AgentExecutionRecord and
	 * ThinkActRecord
	 * @param rootPlanId Plan ID to save
	 * @return Returns true if record is found and saved, false otherwise
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

		String rootPlanId = context.getPlan().getRootPlanId();
		PlanExecutionRecord rootPlan = getOrCreateRootPlanExecutionRecord(rootPlanId, true);
		PlanExecutionRecord recordToUpdate = getRecordToUpdate(context, rootPlan);

		recordToUpdate.setCurrentPlanId(context.getPlan().getCurrentPlanId());
		recordToUpdate.setStartTime(LocalDateTime.now());
		recordToUpdate.setTitle(context.getPlan().getTitle());
		recordToUpdate.setUserRequest(context.getUserRequest());
		retrieveExecutionSteps(context, recordToUpdate);

		// Save the correct plan (parent for sub-plan, self for root plan)
		if (rootPlan != null) {
			savePlanExecutionRecords(rootPlan);
		}
	}

	/**
	 * Record the start of step execution.
	 */
	public void recordStepStart(ExecutionStep step, ExecutionContext context) {
		String rootPlanId = context.getPlan().getRootPlanId();
		PlanExecutionRecord rootPlan = getOrCreateRootPlanExecutionRecord(rootPlanId, true);
		PlanExecutionRecord recordToUpdate = getRecordToUpdate(context, rootPlan);
		if (recordToUpdate != null) {
			int currentStepIndex = step.getStepIndex();
			recordToUpdate.setCurrentStepIndex(currentStepIndex);
			retrieveExecutionSteps(context, recordToUpdate);
			List<AgentExecutionRecord> agentExecutionSequence = recordToUpdate.getAgentExecutionSequence();
			AgentExecutionRecord agentExecutionRecord;
			if (agentExecutionSequence.size() > currentStepIndex) {
				agentExecutionRecord = agentExecutionSequence.get(currentStepIndex);
			}
			else {
				// create and add new AgentExecutionRecord
				agentExecutionRecord = new AgentExecutionRecord(recordToUpdate.getCurrentPlanId(), null, null);
				// 补齐到 currentStepIndex
				while (agentExecutionSequence.size() < currentStepIndex) {
					agentExecutionSequence.add(new AgentExecutionRecord());
				}
				agentExecutionSequence.add(agentExecutionRecord);
			}
			agentExecutionRecord.setStatus(ExecutionStatus.RUNNING);
			// Save the correct plan (parent for sub-plan, self for root plan)
			if (rootPlan != null) {
				savePlanExecutionRecords(rootPlan);
			}
		}
	}

	/**
	 * Record the end of step execution.
	 */
	public void recordStepEnd(ExecutionStep step, ExecutionContext context) {
		String rootPlanId = context.getPlan().getRootPlanId();
		PlanExecutionRecord rootPlan = getOrCreateRootPlanExecutionRecord(rootPlanId, true);
		PlanExecutionRecord recordToUpdate = getRecordToUpdate(context, rootPlan);

		if (recordToUpdate != null) {
			int currentStepIndex = step.getStepIndex();
			recordToUpdate.setCurrentStepIndex(currentStepIndex);
			retrieveExecutionSteps(context, recordToUpdate);
			AgentExecutionRecord agentExecutionRecord = recordToUpdate.getAgentExecutionSequence()
				.get(currentStepIndex);
			agentExecutionRecord.setStatus(
					step.getStatus() == AgentState.COMPLETED ? ExecutionStatus.FINISHED : ExecutionStatus.RUNNING);
			// Save the correct plan (parent for sub-plan, self for root plan)
			if (rootPlan != null) {
				savePlanExecutionRecords(rootPlan);
			}
		}
	}

	/**
	 * Get the plan execution record to update in memory. Returns the record that should
	 * be updated with current execution data.
	 */
	private PlanExecutionRecord getRecordToUpdate(ExecutionContext context, PlanExecutionRecord rootRecord) {
		Long thinkActRecordId = context.getThinkActRecordId();

		// For sub-plan execution, we want to save to parent plan, so return the root
		// plan
		// record
		if (thinkActRecordId != null) {
			// This is a sub-plan execution - save data to parent plan
			String currentPlanId = context.getPlan().getCurrentPlanId();
			if (currentPlanId.startsWith("sub")) {
				// If currentPlanId starts with "sub-", it indicates a sub-plan execution
				// Use the root record to create or get the sub-plan execution record
				return getOrCreateSubPlanExecutionRecord(rootRecord, currentPlanId, thinkActRecordId, true);
			}
			else {
				throw new IllegalArgumentException("Current plan ID is not a sub-plan: " + currentPlanId);
			}

		}
		else {
			// This is a parent/root plan execution - update the plan itself (the record
			// we already have)
			return rootRecord;
		}
	}

	/**
	 * Record complete agent execution at the end. This method handles all agent execution
	 * record management logic without exposing internal record objects.
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

		PlanExecutionRecord planRecord = null;
		PlanExecutionRecord rootPlan = getOrCreateRootPlanExecutionRecord(params.getRootPlanId(), true);
		// Handle both root plan and sub-plan execution cases
		if (params.getThinkActRecordId() != null) {
			// For sub-plan execution, we need the parent plan first
			if (rootPlan != null) {
				planRecord = getOrCreateSubPlanExecutionRecord(rootPlan, params.getCurrentPlanId(),
						params.getThinkActRecordId(), true);
				// For sub-plan, set execution to sub-plan but save parent plan
				if (planRecord != null) {
					setAgentExecution(planRecord, agentRecord);
				}
			}
		}
		else {
			// For root plan execution
			planRecord = getOrCreateRootPlanExecutionRecord(params.getCurrentPlanId(), true);
			if (planRecord != null) {
				setAgentExecution(planRecord, agentRecord);
			}
		}

		// Save the correct plan (parent for sub-plan, self for root plan)
		if (rootPlan != null) {
			savePlanExecutionRecords(rootPlan);
		}
	}

	/**
	 * 接口1: 记录思考和执行动作 Record thinking and action execution process. This method handles
	 * ThinkActRecord creation and thinking process without exposing internal record
	 * objects.
	 */
	@Override
	public Long recordThinkingAndAction(PlanExecutionParams params) {
		if (params.getCurrentPlanId() == null) {
			return null;
		}

		PlanExecutionRecord planExecutionRecord = null;
		PlanExecutionRecord planToSave = null; // Track which plan should be saved

		// Handle both root plan and sub-plan execution cases based on thinkActRecordId
		if (params.getThinkActRecordId() != null) {
			PlanExecutionRecord parentPlan = getOrCreateRootPlanExecutionRecord(params.getRootPlanId(), true);
			if (parentPlan != null) {
				planExecutionRecord = getOrCreateSubPlanExecutionRecord(parentPlan, params.getCurrentPlanId(),
						params.getThinkActRecordId(), true);
				planToSave = parentPlan; // Save parent plan for sub-plan execution
			}
		}
		else {
			planExecutionRecord = getOrCreateRootPlanExecutionRecord(params.getCurrentPlanId(), true);
			planToSave = planExecutionRecord; // Save the root plan record itself
		}

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
			if (planToSave != null) {
				savePlanExecutionRecords(planToSave);
			}
		}

		return thinkActRecord.getId();
	}

	/**
	 * 接口2: 记录执行结果 Record action execution result. This method updates the ThinkActRecord
	 * with action results without exposing internal record objects.
	 */
	@Override
	public void recordActionResult(PlanExecutionParams params) {
		if (params.getCurrentPlanId() == null || params.getCreatedThinkActRecordId() == null) {
			return;
		}

		PlanExecutionRecord planExecutionRecord = null;
		PlanExecutionRecord planToSave = null; // Track which plan should be saved

		// Handle both root plan and sub-plan execution cases based on thinkActRecordId
		if (params.getThinkActRecordId() != null) {
			// Sub-plan execution: thinkActRecordId indicates this is triggered by a tool
			// call
			PlanExecutionRecord parentPlan = getOrCreateRootPlanExecutionRecord(params.getRootPlanId(), true);
			if (parentPlan != null) {
				planExecutionRecord = getOrCreateSubPlanExecutionRecord(parentPlan, params.getCurrentPlanId(),
						params.getThinkActRecordId(), true);
				planToSave = parentPlan; // Save parent plan for sub-plan execution
			}
		}
		else {
			// Root plan execution: no thinkActRecordId means this is a main plan
			planExecutionRecord = getOrCreateRootPlanExecutionRecord(params.getCurrentPlanId(), true);
			planToSave = planExecutionRecord; // Save the root plan record itself
		}

		AgentExecutionRecord agentExecutionRecord = getCurrentAgentExecutionRecord(planExecutionRecord);

		// Additional safety check
		if (agentExecutionRecord == null) {
			logger.error("Failed to retrieve AgentExecutionRecord for plan: {} in recordActionResult",
					params.getCurrentPlanId());
			return;
		}

		// Find the ThinkActRecord by ID and update it with action results
		com.alibaba.cloud.ai.example.manus.recorder.entity.ThinkActRecord thinkActRecord = findThinkActRecordInPlan(
				planExecutionRecord, params.getCreatedThinkActRecordId());

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
			if (planToSave != null) {
				savePlanExecutionRecords(planToSave);
			}
		}
		else {
			logger.warn("ThinkActRecord not found with ID: {} for plan: {}", params.getCreatedThinkActRecordId(),
					params.getCurrentPlanId());
		}
	}

	/**
	 * 接口3: 记录计划完成 Record plan completion. This method handles plan completion recording
	 * logic without exposing internal record objects.
	 */
	@Override
	public void recordPlanCompletion(String currentPlanId, String rootPlanId, Long thinkActRecordId, String summary) {
		if (currentPlanId == null) {
			return;
		}

		PlanExecutionRecord planExecutionRecord = null;
		PlanExecutionRecord planToSave = null; // Track which plan should be saved

		// Handle both root plan and sub-plan execution cases based on thinkActRecordId
		if (thinkActRecordId != null) {
			// Sub-plan execution: thinkActRecordId indicates this is triggered by a tool
			// call
			PlanExecutionRecord parentPlan = getOrCreateRootPlanExecutionRecord(rootPlanId, false);
			if (parentPlan != null) {
				planExecutionRecord = getOrCreateSubPlanExecutionRecord(parentPlan, currentPlanId, thinkActRecordId,
						false);
				planToSave = parentPlan; // Save parent plan for sub-plan execution
			}
		}
		else {
			// Root plan execution: no thinkActRecordId means this is a main plan
			planExecutionRecord = getOrCreateRootPlanExecutionRecord(currentPlanId, false);
			planToSave = planExecutionRecord; // Save the root plan record itself
		}

		if (planExecutionRecord != null) {
			setPlanCompletion(planExecutionRecord, summary);
			// Save the correct plan (parent for sub-plan, self for root plan)
			if (planToSave != null) {
				savePlanExecutionRecords(planToSave);
			}
		}

		logger.info("Plan completed with ID: {} (thinkActRecordId: {}) and summary: {}", currentPlanId,
				thinkActRecordId, summary);
	}

	@Override
	public PlanExecutionRecord getRootPlanExecutionRecord(String rootPlanId) {
		if (rootPlanId == null) {
			logger.warn("rootPlanId is null, cannot retrieve plan execution record");
			return null;
		}
		return getOrCreateRootPlanExecutionRecord(rootPlanId, false);
	}

	/**
	 * Gets or creates root plan execution record
	 * @param rootPlanId Root plan ID
	 * @param createIfNotExists Whether to create if not exists
	 * @return Root plan execution record, or null if not found and createIfNotExists is
	 * false
	 */
	private PlanExecutionRecord getOrCreateRootPlanExecutionRecord(String rootPlanId, boolean createIfNotExists) {
		logger.debug("Enter getOrCreateRootPlanExecutionRecord with rootPlanId: {}, createIfNotExists: {}", rootPlanId,
				createIfNotExists);

		if (rootPlanId == null) {
			logger.error("rootPlanId is null");
			return null;
		}

		// Get existing root plan record
		PlanExecutionRecord rootRecord = getExecutionRecord(rootPlanId);

		// Create if not exists and createIfNotExists is true
		if (rootRecord == null && createIfNotExists) {
			logger.info("Creating root plan with ID: {}", rootPlanId);
			rootRecord = new PlanExecutionRecord(rootPlanId, rootPlanId);
			// Note: No explicit save here as per requirement
		}

		return rootRecord;
	}

	/**
	 * Gets or creates sub-plan execution record from parent plan
	 * @param parentPlan Parent plan execution record
	 * @param subPlanId Sub-plan ID
	 * @param thinkActRecordId Think-act record ID that contains the sub-plan
	 * @param createIfNotExists Whether to create if not exists
	 * @return Sub-plan execution record, or null if thinkActRecordId is null or not found
	 * and createIfNotExists is false
	 */
	private PlanExecutionRecord getOrCreateSubPlanExecutionRecord(PlanExecutionRecord parentPlan, String subPlanId,
			Long thinkActRecordId, boolean createIfNotExists) {
		logger.info(
				"Enter getOrCreateSubPlanExecutionRecord with subPlanId: {}, thinkActRecordId: {}, createIfNotExists: {}",
				subPlanId, thinkActRecordId, createIfNotExists);

		// Return null if thinkActRecordId is null as per requirement
		if (thinkActRecordId == null) {
			logger.warn("thinkActRecordId is null, returning null");
			return null;
		}

		if (parentPlan == null) {
			logger.warn("parentPlan is null");
			return null;
		}

		// Find ThinkActRecord in parent plan
		ThinkActRecord thinkActRecord = findThinkActRecordInPlan(parentPlan, thinkActRecordId);
		if (thinkActRecord == null) {
			logger.warn("ThinkActRecord not found with ID: {}", thinkActRecordId);
			return null;
		}

		// Check if subPlanExecutionRecord exists
		PlanExecutionRecord subPlan = thinkActRecord.getSubPlanExecutionRecord();
		if (subPlan == null && createIfNotExists) {
			// Create new sub-plan with subPlanId and rootPlanId from parent
			logger.info("Creating sub-plan with ID: {}", subPlanId);
			subPlan = new PlanExecutionRecord(subPlanId, parentPlan.getRootPlanId());
			subPlan.setThinkActRecordId(thinkActRecordId);
			thinkActRecord.recordSubPlanExecution(subPlan);
			// Note: No explicit save here as per requirement
		}

		return subPlan;
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

	private PlanExecutionRecord getExecutionRecord(String rootPlanId) {
		PlanExecutionRecordEntity entity = planExecutionRecordRepository.findByPlanId(rootPlanId);
		return entity != null ? entity.getPlanExecutionRecord() : null;
	}

	private void saveExecutionRecord(PlanExecutionRecord planExecutionRecord) {
		PlanExecutionRecordEntity entity = planExecutionRecordRepository
			.findByPlanId(planExecutionRecord.getRootPlanId());
		if (entity == null) {
			entity = new PlanExecutionRecordEntity();
			entity.setPlanId(planExecutionRecord.getRootPlanId());
			entity.setGmtCreate(new Date());
		}

		entity.setPlanExecutionRecord(planExecutionRecord);
		entity.setGmtModified(new Date());

		planExecutionRecordRepository.save(entity);
	}

	private void updateThinkActRecord(PlanExecutionRecord parentPlan, ThinkActRecord record) {
		if (parentPlan != null && record != null) {
			ThinkActRecord existingRecord = findThinkActRecordInPlan(parentPlan, record.getId());
			if (existingRecord != null) {
				BeanUtils.copyProperties(record, existingRecord);
			}
		}
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
	 * get current think-act record ID
	 * @param currentPlanId 当前计划ID
	 * @param rootPlanId 根计划ID
	 * @return 当前 think-act 记录ID，如果没有则返回 null
	 */
	public Long getCurrentThinkActRecordId(String currentPlanId, String rootPlanId) {
		try {
			PlanExecutionRecord planExecutionRecord = null;

			if (rootPlanId != null && !rootPlanId.equals(currentPlanId)) {
				PlanExecutionRecord parentPlan = getOrCreateRootPlanExecutionRecord(rootPlanId, false);
				if (parentPlan != null) {
					AgentExecutionRecord currentAgentRecord = getCurrentAgentExecutionRecord(parentPlan);
					if (currentAgentRecord != null && currentAgentRecord.getThinkActSteps() != null
							&& !currentAgentRecord.getThinkActSteps().isEmpty()) {
						List<ThinkActRecord> steps = currentAgentRecord.getThinkActSteps();
						ThinkActRecord lastStep = steps.get(steps.size() - 1);
						return lastStep.getId();
					}
				}
			}
			else {
				planExecutionRecord = getOrCreateRootPlanExecutionRecord(currentPlanId, false);
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
		}
		catch (Exception e) {
			logger.warn("Failed to get current think-act record ID: {}", e.getMessage());
		}

		return null;
	}

}

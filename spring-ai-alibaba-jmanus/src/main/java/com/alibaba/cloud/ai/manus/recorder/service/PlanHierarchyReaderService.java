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

package com.alibaba.cloud.ai.manus.recorder.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.cloud.ai.manus.recorder.entity.po.PlanExecutionRecordEntity;
import com.alibaba.cloud.ai.manus.recorder.entity.po.AgentExecutionRecordEntity;
import com.alibaba.cloud.ai.manus.recorder.entity.po.ExecutionStatusEntity;
import com.alibaba.cloud.ai.manus.recorder.entity.po.ThinkActRecordEntity;
import com.alibaba.cloud.ai.manus.recorder.entity.vo.PlanExecutionRecord;
import com.alibaba.cloud.ai.manus.recorder.entity.vo.AgentExecutionRecord;
import com.alibaba.cloud.ai.manus.recorder.entity.vo.ExecutionStatus;
import com.alibaba.cloud.ai.manus.recorder.entity.vo.ActToolInfo;
import com.alibaba.cloud.ai.manus.recorder.entity.po.ActToolInfoEntity;
import com.alibaba.cloud.ai.manus.recorder.repository.PlanExecutionRecordRepository;
import com.alibaba.cloud.ai.manus.recorder.repository.AgentExecutionRecordRepository;
import com.alibaba.cloud.ai.manus.recorder.repository.ActToolInfoRepository;
import com.alibaba.cloud.ai.manus.recorder.repository.ThinkActRecordRepository;

import jakarta.annotation.Resource;

/**
 * Service for reading plan execution records by rootPlanId and converting them to VO
 * objects with proper hierarchy relationships built using subPlanExecutionRecords.
 *
 * This service handles: 1. Reading plans by rootPlanId 2. Converting PO entities to VO
 * objects 3. Building hierarchy relationships 4. Simplifying agent execution records
 * (without ThinkActRecord and ActToolInfo details)
 */
@Service
public class PlanHierarchyReaderService {

	private static final Logger logger = LoggerFactory.getLogger(PlanHierarchyReaderService.class);

	@Resource
	private PlanExecutionRecordRepository planExecutionRecordRepository;

	@Resource
	private AgentExecutionRecordRepository agentExecutionRecordRepository;

	@Resource
	private ActToolInfoRepository actToolInfoRepository;

	@Resource
	private ThinkActRecordRepository thinkActRecordRepository;

	/**
	 * Read plan execution records by rootPlanId and convert to VO objects with hierarchy.
	 *
	 * Data model explanation: - Root plan: currentPlanId = rootPlanId (root plan points
	 * to itself) - Sub plans: currentPlanId ≠ rootPlanId, but rootPlanId field points to
	 * root plan
	 *
	 * Since rootPlanId is unique, this method returns a single root plan with all its
	 * sub-plans.
	 * @param rootPlanId The root plan ID to search for
	 * @return Root PlanExecutionRecord with hierarchy relationships, or null if not found
	 */
	public PlanExecutionRecord readPlanTreeByRootId(String rootPlanId) {
		try {
			if (rootPlanId == null || rootPlanId.trim().isEmpty()) {
				logger.warn("rootPlanId is null or empty, cannot read plans");
				return null;
			}

			// Step 1: Find the root plan itself
			// Root plan's currentPlanId should equal rootPlanId
			Optional<PlanExecutionRecordEntity> rootPlanEntityOpt = planExecutionRecordRepository
				.findByCurrentPlanId(rootPlanId);

			if (!rootPlanEntityOpt.isPresent()) {
				logger.debug("Root plan not found for rootPlanId: {}", rootPlanId);
				return null;
			}

			// Step 2: Find all plans that have this rootPlanId (including the root plan
			// itself)
			// This includes the root plan and all its sub-plans
			List<PlanExecutionRecordEntity> planEntities = planExecutionRecordRepository.findByRootPlanId(rootPlanId);

			if (planEntities.isEmpty()) {
				logger.debug("No plans found for rootPlanId: {}", rootPlanId);
				return null;
			}

			logger.debug("Found {} plans for rootPlanId: {}", planEntities.size(), rootPlanId);

			// Step 3: Convert to VO objects and build hierarchy
			List<PlanExecutionRecord> planRecords = new ArrayList<>();

			for (PlanExecutionRecordEntity planEntity : planEntities) {
				PlanExecutionRecord planRecord = convertToPlanExecutionRecord(planEntity);
				planRecords.add(planRecord);
			}

			// Step 4: Build hierarchy relationships between plans
			buildHierarchyRelationships(planRecords);

			// Step 5: Find and return the root plan from the converted records
			// Root plan's currentPlanId should equal the input rootPlanId
			PlanExecutionRecord rootPlan = planRecords.stream()
				.filter(plan -> rootPlanId.equals(plan.getCurrentPlanId()))
				.findFirst()
				.orElse(null);

			if (rootPlan != null) {
				logger.info("Successfully converted root plan with {} sub-plans for rootPlanId: {}",
						planRecords.size() - 1, rootPlanId);
			}
			else {
				logger.warn("Root plan not found in converted records for rootPlanId: {}", rootPlanId);
			}

			return rootPlan;

		}
		catch (Exception e) {
			logger.error("Failed to read plans by rootPlanId: {}", rootPlanId, e);
			return null;
		}
	}

	/**
	 * Read a single plan execution record by currentPlanId and convert to VO object. This
	 * method only reads the specified plan without loading sub-plans.
	 * @param currentPlanId The current plan ID to search for
	 * @return PlanExecutionRecord VO object, or null if not found
	 */
	public PlanExecutionRecord readSinglePlanById(String currentPlanId) {
		try {
			if (currentPlanId == null || currentPlanId.trim().isEmpty()) {
				logger.warn("currentPlanId is null or empty, cannot read plan");
				return null;
			}

			Optional<PlanExecutionRecordEntity> planEntityOpt = planExecutionRecordRepository
				.findByCurrentPlanId(currentPlanId);

			if (!planEntityOpt.isPresent()) {
				logger.debug("Plan not found for currentPlanId: {}", currentPlanId);
				return null;
			}

			PlanExecutionRecordEntity planEntity = planEntityOpt.get();
			PlanExecutionRecord planRecord = convertToPlanExecutionRecord(planEntity);

			logger.debug("Successfully converted plan to VO object for currentPlanId: {}", currentPlanId);

			return planRecord;

		}
		catch (Exception e) {
			logger.error("Failed to read plan by currentPlanId: {}", currentPlanId, e);
			return null;
		}
	}

	/**
	 * Convert PlanExecutionRecordEntity to PlanExecutionRecord VO object.
	 * @param entity The PO entity to convert
	 * @return Converted VO object
	 */
	private PlanExecutionRecord convertToPlanExecutionRecord(PlanExecutionRecordEntity entity) {
		PlanExecutionRecord vo = new PlanExecutionRecord();

		// Set basic properties
		vo.setId(entity.getId());
		vo.setCurrentPlanId(entity.getCurrentPlanId()); // Use setter to set currentPlanId
		vo.setTitle(entity.getTitle());
		vo.setUserRequest(entity.getUserRequest());
		vo.setStartTime(entity.getStartTime());
		vo.setEndTime(entity.getEndTime());
		vo.setCompleted(entity.isCompleted());
		vo.setSummary(entity.getSummary());
		vo.setCurrentStepIndex(entity.getCurrentStepIndex());
		vo.setModelName(entity.getModelName());
		// vo.setUserInputWaitState(entity.getUserInputWaitState());

		// Set hierarchy properties
		vo.setRootPlanId(entity.getRootPlanId());
		vo.setParentPlanId(entity.getParentPlanId());
		vo.setToolCallId(entity.getToolCallId());

		// Convert agent execution records to simple versions
		if (entity.getAgentExecutionSequence() != null && !entity.getAgentExecutionSequence().isEmpty()) {
			List<AgentExecutionRecord> agentRecords = entity.getAgentExecutionSequence()
				.stream()
				.map(this::convertToAgentExecutionRecord)
				.collect(Collectors.toList());
			vo.setAgentExecutionSequence(agentRecords);
		}

		// Query parent ActToolInfo by toolCallId for sub-plan detail displaying
		if (entity.getToolCallId() != null && !entity.getToolCallId().trim().isEmpty()) {
			try {
				Optional<ActToolInfoEntity> actToolInfoEntityOpt = actToolInfoRepository
					.findByToolCallId(entity.getToolCallId());
				if (actToolInfoEntityOpt.isPresent()) {
					ActToolInfoEntity actToolInfoEntity = actToolInfoEntityOpt.get();
					ActToolInfo parentActToolCall = convertToActToolInfo(actToolInfoEntity);
					vo.setParentActToolCall(parentActToolCall);
					logger.debug("Found parent ActToolInfo for toolCallId: {}", entity.getToolCallId());
				}
				else {
					logger.debug("No parent ActToolInfo found for toolCallId: {}", entity.getToolCallId());
				}
			}
			catch (Exception e) {
				logger.error("Failed to query parent ActToolInfo for toolCallId: {}", entity.getToolCallId(), e);
			}
		}

		return vo;
	}

	/**
	 * Convert AgentExecutionRecordEntity to AgentExecutionRecord VO object.
	 * @param entity The PO entity to convert
	 * @return Converted VO object
	 */
	private AgentExecutionRecord convertToAgentExecutionRecord(AgentExecutionRecordEntity entity) {
		AgentExecutionRecord vo = new AgentExecutionRecord(entity.getStepId(), entity.getAgentName(),
				entity.getAgentDescription());

		// Set basic properties
		vo.setId(entity.getId());
		vo.setStartTime(entity.getStartTime());
		vo.setEndTime(entity.getEndTime());
		vo.setMaxSteps(entity.getMaxSteps());
		vo.setCurrentStep(entity.getCurrentStep());
		vo.setAgentRequest(entity.getAgentRequest());
		vo.setResult(entity.getResult());
		vo.setErrorMessage(entity.getErrorMessage());
		vo.setModelName(entity.getModelName());

		// Convert execution status
		if (entity.getStatus() != null) {
			vo.setStatus(convertExecutionStatus(entity.getStatus()));
		}

		// Note: subPlanExecutionRecords will be populated later when building hierarchy

		return vo;
	}

	/**
	 * Convert ExecutionStatusEntity to ExecutionStatus enum.
	 * @param status The PO status entity
	 * @return Corresponding enum value
	 */
	private ExecutionStatus convertExecutionStatus(ExecutionStatusEntity status) {
		if (status == null) {
			return ExecutionStatus.IDLE;
		}

		switch (status) {
			case IDLE:
				return ExecutionStatus.IDLE;
			case RUNNING:
				return ExecutionStatus.RUNNING;
			case FINISHED:
				return ExecutionStatus.FINISHED;
			default:
				return ExecutionStatus.IDLE;
		}
	}

	/**
	 * Build hierarchy relationships between plans using subPlanExecutionRecords. This
	 * method establishes parent-child relationships by populating the
	 * subPlanExecutionRecords field in AgentExecutionRecord objects.
	 *
	 * Hierarchy logic: - For each plan, look through its agent execution sequence - For
	 * each agent, find sub-plans where parentPlanId = current plan's currentPlanId - This
	 * creates a tree structure: Root Plan -> Agents -> Sub Plans
	 * @param planRecords List of plan records to build hierarchy for
	 */
	private void buildHierarchyRelationships(List<PlanExecutionRecord> planRecords) {
		if (planRecords == null || planRecords.isEmpty()) {
			return;
		}

		// Create a map for quick lookup by currentPlanId
		java.util.Map<String, PlanExecutionRecord> planMap = planRecords.stream()
			.collect(Collectors.toMap(PlanExecutionRecord::getCurrentPlanId, plan -> plan));

		// Build hierarchy relationships for each plan
		for (PlanExecutionRecord plan : planRecords) {
			if (plan.getAgentExecutionSequence() != null) {
				for (AgentExecutionRecord agentRecord : plan.getAgentExecutionSequence()) {
					// Find sub-plans for this specific agent using enhanced SQL-based
					// approach
					List<PlanExecutionRecord> subPlans = findSubPlansForAgent(agentRecord, planMap);

					if (!subPlans.isEmpty()) {
						agentRecord.setSubPlanExecutionRecords(subPlans);
						logger.debug("Found {} sub-plans for agent {} (ID: {}) in plan {}", subPlans.size(),
								agentRecord.getAgentName(), agentRecord.getId(), plan.getCurrentPlanId());
					}
					else {
						logger.debug("No sub-plans found for agent {} (ID: {}) in plan {}", agentRecord.getAgentName(),
								agentRecord.getId(), plan.getCurrentPlanId());
					}
				}
			}
		}

		logger.debug("Successfully built hierarchy relationships for {} plans", planRecords.size());
	}

	/**
	 * Find sub-plans for a specific agent execution record using enhanced SQL-based
	 * approach.
	 *
	 * This method uses the following logic: 1. Query ActToolInfo by toolCallId from
	 * sub-plans 2. Query ThinkActRecord by ActToolInfo relationship 3. Get
	 * parentExecutionId from ThinkActRecord 4. Match parentExecutionId with
	 * AgentExecutionRecord.id in the current plan's agent sequence
	 * @param agentRecord The agent execution record to find sub-plans for
	 * @param planMap Map of all plans for quick lookup
	 * @return List of sub-plans that belong to the specified agent execution record
	 */
	private List<PlanExecutionRecord> findSubPlansForAgent(AgentExecutionRecord agentRecord,
			java.util.Map<String, PlanExecutionRecord> planMap) {
		if (agentRecord == null || agentRecord.getId() == null) {
			logger.debug("Agent record or ID is null, returning empty sub-plans list");
			return new ArrayList<>();
		}
		List<PlanExecutionRecord> matchingSubPlans = new ArrayList<>();
		Long agentExecutionId = agentRecord.getId();

		// Find all sub-plans (plans with non-null toolCallId)
		List<PlanExecutionRecord> subPlans = planMap.values()
			.stream()
			.filter(plan -> plan.getToolCallId() != null && !plan.getToolCallId().trim().isEmpty())
			.collect(Collectors.toList());

		logger.debug("Found {} sub-plans with toolCallId for agent execution ID: {}", subPlans.size(),
				agentExecutionId);

		// For each sub-plan, check if it was triggered by this agent
		for (PlanExecutionRecord subPlan : subPlans) {
			try {
				// Step 1: Query ActToolInfo by toolCallId
				Optional<ActToolInfoEntity> actToolInfoOpt = actToolInfoRepository
					.findByToolCallId(subPlan.getToolCallId());

				if (!actToolInfoOpt.isPresent()) {
					logger.debug("No ActToolInfo found for toolCallId: {}", subPlan.getToolCallId());
					continue;
				}

				// Step 2: Query ThinkActRecord by ActToolInfo relationship
				Optional<ThinkActRecordEntity> thinkActRecordOpt = thinkActRecordRepository
					.findByActToolInfoToolCallId(subPlan.getToolCallId());

				if (!thinkActRecordOpt.isPresent()) {
					logger.debug("No ThinkActRecord found for toolCallId: {}", subPlan.getToolCallId());
					continue;
				}

				// Step 3: Get parentExecutionId from ThinkActRecord
				ThinkActRecordEntity thinkActRecord = thinkActRecordOpt.get();
				Long parentExecutionId = thinkActRecord.getParentExecutionId();

				if (parentExecutionId == null) {
					logger.debug("ThinkActRecord has null parentExecutionId for toolCallId: {}",
							subPlan.getToolCallId());
					continue;
				}

				// Step 4: Match parentExecutionId with AgentExecutionRecord.id
				if (agentExecutionId.equals(parentExecutionId)) {
					matchingSubPlans.add(subPlan);
					logger.debug("Found matching sub-plan {} for agent execution ID: {} via toolCallId: {}",
							subPlan.getCurrentPlanId(), agentExecutionId, subPlan.getToolCallId());
				}

			}
			catch (Exception e) {
				logger.error("Error processing sub-plan {} for agent execution ID: {}", subPlan.getCurrentPlanId(),
						agentExecutionId, e);
			}
		}

		logger.debug("Found {} matching sub-plans for agent execution ID: {}", matchingSubPlans.size(),
				agentExecutionId);
		return matchingSubPlans;
	}

	/**
	 * Convert ActToolInfoEntity to ActToolInfo VO object.
	 * @param entity The PO entity to convert
	 * @return Converted VO object
	 */
	private ActToolInfo convertToActToolInfo(ActToolInfoEntity entity) {
		ActToolInfo vo = new ActToolInfo();
		vo.setName(entity.getName());
		vo.setParameters(entity.getParameters());
		vo.setResult(entity.getResult());
		vo.setId(entity.getToolCallId()); // Map toolCallId to id field
		return vo;
	}

}

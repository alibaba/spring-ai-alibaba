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
import com.alibaba.cloud.ai.manus.recorder.entity.vo.PlanExecutionRecord;
import com.alibaba.cloud.ai.manus.recorder.entity.vo.AgentExecutionRecordSimple;
import com.alibaba.cloud.ai.manus.recorder.entity.vo.ExecutionStatus;
import com.alibaba.cloud.ai.manus.recorder.entity.vo.ActToolInfo;
import com.alibaba.cloud.ai.manus.recorder.entity.po.ActToolInfoEntity;
import com.alibaba.cloud.ai.manus.recorder.repository.PlanExecutionRecordRepository;
import com.alibaba.cloud.ai.manus.recorder.repository.AgentExecutionRecordRepository;
import com.alibaba.cloud.ai.manus.recorder.repository.ActToolInfoRepository;

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
		vo.setCurrentPlanId(entity.getCurrentPlanId()); // 使用setter设置currentPlanId
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
			List<AgentExecutionRecordSimple> agentRecords = entity.getAgentExecutionSequence()
				.stream()
				.map(this::convertToAgentExecutionRecordSimple)
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
	 * Convert AgentExecutionRecordEntity to AgentExecutionRecordSimple VO object. This is
	 * a simplified version without ThinkActRecord and ActToolInfo details.
	 * @param entity The PO entity to convert
	 * @return Converted simple VO object
	 */
	private AgentExecutionRecordSimple convertToAgentExecutionRecordSimple(AgentExecutionRecordEntity entity) {
		AgentExecutionRecordSimple vo = new AgentExecutionRecordSimple(entity.getStepId(), entity.getAgentName(),
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
	 * subPlanExecutionRecords field in AgentExecutionRecordSimple objects.
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
				for (AgentExecutionRecordSimple agentRecord : plan.getAgentExecutionSequence()) {
					// Find sub-plans for this agent
					// Safety check: ensure currentPlanId is not null
					if (plan.getCurrentPlanId() != null) {
						List<PlanExecutionRecord> subPlans = findSubPlansForAgent(plan.getCurrentPlanId(), planMap);

						if (!subPlans.isEmpty()) {
							agentRecord.setSubPlanExecutionRecords(subPlans);
							logger.debug("Found {} sub-plans for agent {} in plan {}", subPlans.size(),
									agentRecord.getAgentName(), plan.getCurrentPlanId());
						}
					}
					else {
						logger.warn("Plan with null currentPlanId found, skipping hierarchy building for this plan");
					}
				}
			}
		}

		logger.debug("Successfully built hierarchy relationships for {} plans", planRecords.size());
	}

	/**
	 * Find sub-plans for a given parent plan.
	 *
	 * This method finds all plans where parentPlanId = the input parentPlanId. It's used
	 * to build the hierarchy: Parent Plan -> Sub Plans
	 * @param parentPlanId The parent plan ID to search for sub-plans
	 * @param planMap Map of all plans for quick lookup
	 * @return List of sub-plans that belong to the specified parent plan
	 */
	private List<PlanExecutionRecord> findSubPlansForAgent(String parentPlanId,
			java.util.Map<String, PlanExecutionRecord> planMap) {
		// Safety check: avoid NullPointerException when parentPlanId is null
		if (parentPlanId == null) {
			return new ArrayList<>();
		}

		return planMap.values()
			.stream()
			.filter(plan -> parentPlanId.equals(plan.getParentPlanId()))
			.collect(Collectors.toList());
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

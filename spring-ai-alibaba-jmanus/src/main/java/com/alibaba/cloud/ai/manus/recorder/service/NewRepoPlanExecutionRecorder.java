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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.cloud.ai.manus.recorder.entity.po.AgentExecutionRecordEntity;
import com.alibaba.cloud.ai.manus.recorder.entity.po.PlanExecutionRecordEntity;
import com.alibaba.cloud.ai.manus.recorder.entity.po.ExecutionStatusEntity;
import com.alibaba.cloud.ai.manus.recorder.repository.PlanExecutionRecordRepository;
import com.alibaba.cloud.ai.manus.recorder.repository.AgentExecutionRecordRepository;
import com.alibaba.cloud.ai.manus.recorder.repository.ThinkActRecordRepository;
import com.alibaba.cloud.ai.manus.recorder.entity.po.ThinkActRecordEntity;
import com.alibaba.cloud.ai.manus.recorder.entity.po.ActToolInfoEntity;
import com.alibaba.cloud.ai.manus.agent.AgentState;
import com.alibaba.cloud.ai.manus.recorder.repository.ActToolInfoRepository;
import com.alibaba.cloud.ai.manus.recorder.service.PlanExecutionRecorder.ActToolParam;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.ExecutionStep;
import com.alibaba.cloud.ai.manus.recorder.entity.vo.AgentExecutionRecord;
import com.alibaba.cloud.ai.manus.recorder.entity.vo.ThinkActRecord;
import com.alibaba.cloud.ai.manus.recorder.entity.vo.ActToolInfo;
import com.alibaba.cloud.ai.manus.recorder.entity.vo.ExecutionStatus;
import java.util.ArrayList;

import jakarta.annotation.Resource;

@Service
public class NewRepoPlanExecutionRecorder implements PlanExecutionRecorder {

	@Resource
	private PlanExecutionRecordRepository planExecutionRecordRepository;

	@Resource
	private AgentExecutionRecordRepository agentExecutionRecordRepository;

	@Resource
	private ThinkActRecordRepository thinkActRecordRepository;

	@Resource
	private ActToolInfoRepository actToolInfoRepository;

	private static final Logger logger = LoggerFactory.getLogger(NewRepoPlanExecutionRecorder.class);

	/**
	 * Record plan execution start with hierarchy information.
	 * @param currentPlanId The unique identifier for the current plan
	 * @param title Plan title
	 * @param userRequset User's original request
	 * @param executionSteps List of execution steps
	 * @param parentPlanId Parent plan ID (can be null for root plans)
	 * @param rootPlanId Root plan ID (can be null for main plans)
	 * @param toolcallId Tool call ID that triggered this plan (can be null)
	 * @return The ID of the created plan execution record, or null if creation failed
	 */
	@Transactional
	public Long recordPlanExecutionStart(String currentPlanId, String title, String userRequset,
			List<ExecutionStep> executionSteps, String parentPlanId, String rootPlanId, String toolcallId) {
		try {
			// Check if plan already exists
			Optional<PlanExecutionRecordEntity> existingPlanOpt = planExecutionRecordRepository
				.findByCurrentPlanId(currentPlanId);

			PlanExecutionRecordEntity planExecutionRecordEntity;
			if (existingPlanOpt.isPresent()) {
				// Update existing plan
				planExecutionRecordEntity = existingPlanOpt.get();
				logger.debug("Updating existing plan execution record for ID: {}", currentPlanId);
			}
			else {
				// Create new plan
				planExecutionRecordEntity = new PlanExecutionRecordEntity(currentPlanId);
				logger.debug("Creating new plan execution record for ID: {}", currentPlanId);
			}

			// Set/update all fields
			planExecutionRecordEntity.setCurrentPlanId(currentPlanId);
			planExecutionRecordEntity.setStartTime(LocalDateTime.now());
			planExecutionRecordEntity.setTitle(title);
			planExecutionRecordEntity.setUserRequest(userRequset);

			// Process execution steps and create/update AgentExecutionRecordEntity
			// instances
			if (executionSteps != null && !executionSteps.isEmpty()) {
				for (ExecutionStep step : executionSteps) {
					// Create or update AgentExecutionRecordEntity for each step

					AgentExecutionRecordEntity agentRecord = createOrUpdateAgentExecutionRecord(step);
					if (agentRecord != null) {
						// Add to plan execution record
						planExecutionRecordEntity.addAgentExecutionRecord(agentRecord);
					}
				}
			}

			// Save the entity using repository
			PlanExecutionRecordEntity savedEntity = planExecutionRecordRepository.save(planExecutionRecordEntity);

			// Create hierarchy relationships if provided
			// Note: With enhanced validation, we now require rootPlanId to be provided
			if (rootPlanId != null && !rootPlanId.trim().isEmpty()) {
				try {
					createPlanRelationship(currentPlanId, parentPlanId, rootPlanId, toolcallId);
					logger.debug("Successfully created hierarchy relationship for plan ID: {}", currentPlanId);
				}
				catch (IllegalArgumentException e) {
					logger.error("Validation error creating hierarchy relationship for plan ID: {}: {}", currentPlanId,
							e.getMessage());
					throw e; // Re-throw validation errors as they indicate programming
								// issues
				}
				catch (IllegalStateException e) {
					logger.error("State error creating hierarchy relationship for plan ID: {}: {}", currentPlanId,
							e.getMessage());
					throw e; // Re-throw state errors as they indicate system issues
				}
				catch (Exception e) {
					logger.error("Unexpected error creating hierarchy relationship for plan ID: {}", currentPlanId, e);
					throw new RuntimeException("Failed to create hierarchy relationship", e);
				}
			}
			else {
				logger.debug(
						"Skipping hierarchy relationship creation - rootPlanId is required but not provided for plan ID: {}",
						currentPlanId);
			}

			logger.info("Successfully saved plan execution record for ID: {} with {} steps", currentPlanId,
					executionSteps != null ? executionSteps.size() : 0);
			return savedEntity.getId();

		}
		catch (Exception e) {
			logger.error("Failed to create or update plan execution record for ID: {}", currentPlanId, e);
			return null;
		}
	}

	/**
	 * Create or update AgentExecutionRecordEntity for a given ExecutionStep
	 * @param step ExecutionStep to process
	 * @return AgentExecutionRecordEntity instance, or null if creation failed
	 */
	private AgentExecutionRecordEntity createOrUpdateAgentExecutionRecord(ExecutionStep step) {
		try {
			if (step == null || step.getStepId() == null) {
				logger.warn("ExecutionStep or stepId is null, skipping agent record creation");
				return null;
			}

			// Extract agent name and request from stepRequirement
			String extractedAgentName = "Agent";
			String agentRequest = null;

			if (step.getStepRequirement() != null && !step.getStepRequirement().trim().isEmpty()) {
				String stepReq = step.getStepRequirement().trim();
				// Parse format: "[AGENT_NAME] request content"
				if (stepReq.startsWith("[") && stepReq.contains("]")) {
					int endBracketIndex = stepReq.indexOf("]");
					if (endBracketIndex > 1) {
						String bracketContent = stepReq.substring(1, endBracketIndex).trim();
						String requestContent = stepReq.substring(endBracketIndex + 1).trim();

						// Use extracted agent name if available
						if (!bracketContent.isEmpty()) {
							extractedAgentName = bracketContent;
						}

						// Set the request content
						if (!requestContent.isEmpty()) {
							agentRequest = requestContent;
						}

						logger.debug("Extracted agent name: '{}' and request: '{}' from stepRequirement: '{}'",
								extractedAgentName, agentRequest, stepReq);
					}
				}
			}

			// 1. Find existing AgentExecutionRecordEntity by stepId
			Optional<AgentExecutionRecordEntity> existingRecordOpt = agentExecutionRecordRepository
				.findByStepId(step.getStepId());

			AgentExecutionRecordEntity agentRecord;
			if (existingRecordOpt.isPresent()) {
				// 2. Update existing record
				agentRecord = existingRecordOpt.get();
				logger.debug("Updating existing AgentExecutionRecordEntity for step ID: {}", step.getStepId());

				// Update fields if they have changed
				agentRecord.setAgentName(extractedAgentName);
				// Set agent request if extracted
				if (agentRequest != null) {
					agentRecord.setAgentRequest(agentRequest);
				}

			}
			else {
				// 2. Create new record
				agentRecord = new AgentExecutionRecordEntity(step.getStepId(), extractedAgentName,
						"No description available");
				logger.debug("Created new AgentExecutionRecordEntity for step ID: {}", step.getStepId());

				// Set agent request for new records
				if (agentRequest != null) {
					agentRecord.setAgentRequest(agentRequest);
				}
			}

			// Set additional fields
			if (step.getAgent() != null) {
				agentRecord.setStatus(convertAgentStateToExecutionStatus(step.getAgent().getState()));
			}

			// Set step index
			agentRecord.setCurrentStep(step.getStepIndex() != null ? step.getStepIndex() : 0);

			logger.debug("Processed AgentExecutionRecordEntity for step ID: {} with agent: '{}' and request: '{}'",
					step.getStepId(), agentRecord.getAgentName(), agentRequest);

			return agentRecord;

		}
		catch (Exception e) {
			logger.error("Failed to create or update AgentExecutionRecordEntity for step: {}", step.getStepId(), e);
			return null;
		}
	}

	/**
	 * Convert AgentState to ExecutionStatusEntity
	 * @param agentState Agent state to convert
	 * @return Corresponding ExecutionStatusEntity
	 */
	private ExecutionStatusEntity convertAgentStateToExecutionStatus(AgentState agentState) {
		if (agentState == null) {
			return ExecutionStatusEntity.IDLE;
		}

		switch (agentState) {
			case NOT_STARTED:
				return ExecutionStatusEntity.IDLE;
			case IN_PROGRESS:
				return ExecutionStatusEntity.RUNNING;
			case COMPLETED:
				return ExecutionStatusEntity.FINISHED;
			case BLOCKED:
			case FAILED:
				return ExecutionStatusEntity.FINISHED; // Both blocked and failed are
														// considered finished states
			default:
				throw new IllegalArgumentException("Invalid agent state: " + agentState);
		}
	}

	/**
	 * Convert ActToolParam to ActToolInfoEntity
	 * @param actToolParam ActToolParam to convert
	 * @return Corresponding ActToolInfoEntity
	 */
	private ActToolInfoEntity convertToActToolInfoEntity(ActToolParam actToolParam) {
		ActToolInfoEntity entity = new ActToolInfoEntity(actToolParam.getName(), actToolParam.getParameters(),
				actToolParam.getToolCallId());
		// Set the result if available
		if (actToolParam.getResult() != null) {
			entity.setResult(actToolParam.getResult());
		}
		return entity;
	}

	@Override
	public void recordStepStart(ExecutionStep step, String currentPlanId) {
		try {
			if (step == null || step.getStepId() == null || currentPlanId == null) {
				logger.warn("ExecutionStep, stepId, or currentPlanId is null, skipping step start recording");
				return;
			}

			// 1. Query by stepId in ExecutionStep
			Optional<AgentExecutionRecordEntity> agentRecordOpt = agentExecutionRecordRepository
				.findByStepId(step.getStepId());

			if (!agentRecordOpt.isPresent()) {
				throw new IllegalArgumentException("agent record is null ");
			}

			// 2. The entity should be in the database because we insert it before
			AgentExecutionRecordEntity agentRecord = agentRecordOpt.get();
			logger.debug("Found existing AgentExecutionRecordEntity for stepId: {}", step.getStepId());

			// 3. Update the entity & save
			agentRecord.setStatus(ExecutionStatusEntity.RUNNING);
			agentRecord.setStartTime(LocalDateTime.now());

			// Update step index if available
			if (step.getStepIndex() != null) {
				agentRecord.setCurrentStep(step.getStepIndex());
			}

			// Update agent information if available
			if (step.getAgent() != null) {
				agentRecord.setAgentName(step.getAgent().getName());
				agentRecord.setAgentDescription(step.getAgent().getDescription());
			}

			// Save the updated entity
			agentExecutionRecordRepository.save(agentRecord);

			logger.info("Successfully recorded step start for stepId: {}, planId: {}, status: RUNNING",
					step.getStepId(), currentPlanId);

		}
		catch (Exception e) {
			logger.error("Failed to record step start for stepId: {}, planId: {}",
					step != null ? step.getStepId() : "null", currentPlanId, e);
		}
	}

	@Override
	public void recordStepEnd(ExecutionStep step, String currentPlanId) {
		try {
			if (step == null || step.getStepId() == null || currentPlanId == null) {
				logger.warn("ExecutionStep, stepId, or currentPlanId is null, skipping step end recording");
				return;
			}

			// 1. Query by stepId in ExecutionStep
			Optional<AgentExecutionRecordEntity> agentRecordOpt = agentExecutionRecordRepository
				.findByStepId(step.getStepId());

			if (!agentRecordOpt.isPresent()) {
				throw new IllegalArgumentException("agent record is null ");
			}

			// 2. The entity should be in the database because we insert it before
			AgentExecutionRecordEntity agentRecord = agentRecordOpt.get();
			logger.debug("Found existing AgentExecutionRecordEntity for stepId: {}", step.getStepId());

			// 3. Update the entity & save
			agentRecord.setStatus(ExecutionStatusEntity.FINISHED);
			agentRecord.setEndTime(LocalDateTime.now());

			// Update step index if available
			if (step.getStepIndex() != null) {
				agentRecord.setCurrentStep(step.getStepIndex());
			}

			// Update agent information if available
			if (step.getAgent() != null) {
				agentRecord.setAgentName(step.getAgent().getName());
				agentRecord.setAgentDescription(step.getAgent().getDescription());
			}

			// Save the updated entity
			agentExecutionRecordRepository.save(agentRecord);

			logger.info("Successfully recorded step end for stepId: {}, planId: {}, status: FINISHED", step.getStepId(),
					currentPlanId);

		}
		catch (Exception e) {
			logger.error("Failed to record step end for stepId: {}, planId: {}",
					step != null ? step.getStepId() : "null", currentPlanId, e);
		}
	}

	@Override
	public Long recordThinkingAndAction(ExecutionStep step, ThinkActRecordParams params) {
		try {
			if (step == null || step.getStepId() == null || params == null) {
				logger.warn(
						"ExecutionStep, stepId, or ThinkActRecordParams is null, skipping thinking and action recording");
				return null;
			}

			// 1. Query by stepId in ExecutionStep to get the agent execution record
			Optional<AgentExecutionRecordEntity> agentRecordOpt = agentExecutionRecordRepository
				.findByStepId(step.getStepId());

			if (!agentRecordOpt.isPresent()) {
				logger.error("Agent execution record not found for stepId: {}", step.getStepId());
				return null;
			}

			// 2. Get the agent execution record entity
			AgentExecutionRecordEntity agentRecord = agentRecordOpt.get();
			logger.debug("Found existing AgentExecutionRecordEntity for stepId: {}", step.getStepId());

			// 3. Create new ThinkActRecordEntity
			ThinkActRecordEntity thinkActRecord = new ThinkActRecordEntity();
			thinkActRecord.setParentExecutionId(agentRecord.getId());
			thinkActRecord.setThinkActId(params.getThinkActId());
			thinkActRecord.setThinkInput(params.getThinkInput());
			thinkActRecord.setThinkOutput(params.getThinkOutput());
			thinkActRecord.setErrorMessage(params.getErrorMessage());

			// Convert ActToolParam to ActToolInfoEntity and set the list
			if (params.getActToolInfoList() != null && !params.getActToolInfoList().isEmpty()) {
				List<ActToolInfoEntity> actToolInfoEntities = params.getActToolInfoList()
					.stream()
					.map(this::convertToActToolInfoEntity)
					.collect(java.util.stream.Collectors.toList());
				thinkActRecord.setActToolInfoList(actToolInfoEntities);
			}

			// 4. Save the think-act record
			ThinkActRecordEntity savedThinkActRecord = thinkActRecordRepository.save(thinkActRecord);

			logger.info("Successfully recorded thinking and action for stepId: {}, thinkActRecordId: {}",
					step.getStepId(), savedThinkActRecord.getId());

			return savedThinkActRecord.getId();

		}
		catch (Exception e) {
			logger.error("Failed to record thinking and action for stepId: {}",
					step != null ? step.getStepId() : "null", e);
			return null;
		}
	}

	@Override
	public void recordActionResult(List<ActToolParam> actToolParamList) {
		try {
			if (actToolParamList == null || actToolParamList.isEmpty()) {
				logger.warn("ActToolParamList is null/empty, skipping action result recording");
				return;
			}

			// 1. Get List of ActToolParam
			logger.debug("Processing {} ActToolParam entries", actToolParamList.size());

			// 2. Find each entity by using toolCallId and refresh with new data
			for (ActToolParam actToolParam : actToolParamList) {
				if (actToolParam.getToolCallId() == null) {
					logger.warn("ActToolParam has null toolCallId, skipping: {}", actToolParam);
					continue;
				}

				try {
					// Find existing ActToolInfoEntity by toolCallId
					Optional<ActToolInfoEntity> existingEntityOpt = actToolInfoRepository
						.findByToolCallId(actToolParam.getToolCallId());

					if (existingEntityOpt.isPresent()) {
						// 3. Refresh the entity with new data from ActToolParam
						ActToolInfoEntity existingEntity = existingEntityOpt.get();
						logger.debug("Found existing ActToolInfoEntity with toolCallId: {}, updating with new data",
								actToolParam.getToolCallId());

						// Update fields with new data
						if (actToolParam.getName() != null) {
							existingEntity.setName(actToolParam.getName());
						}
						if (actToolParam.getParameters() != null) {
							existingEntity.setParameters(actToolParam.getParameters());
						}
						if (actToolParam.getResult() != null) {
							existingEntity.setResult(actToolParam.getResult());
						}

						// Save the updated entity
						actToolInfoRepository.save(existingEntity);
						logger.debug("Successfully updated ActToolInfoEntity with toolCallId: {}",
								actToolParam.getToolCallId());

					}
					else {
						logger.warn("No ActToolInfoEntity found with toolCallId: {}, creating new entity",
								actToolParam.getToolCallId());

						// Create new entity if not found
						ActToolInfoEntity newEntity = convertToActToolInfoEntity(actToolParam);
						actToolInfoRepository.save(newEntity);
						logger.debug("Created new ActToolInfoEntity with toolCallId: {}", actToolParam.getToolCallId());
					}

				}
				catch (Exception e) {
					logger.error("Failed to process ActToolParam with toolCallId: {}", actToolParam.getToolCallId(), e);
				}
			}

			logger.info("Successfully processed action results, processed {} tools", actToolParamList.size());

		}
		catch (Exception e) {
			logger.error("Failed to record action result", e);
		}
	}

	@Override
	public void recordCompleteAgentExecution(ExecutionStep step) {
		try {
			if (step == null || step.getStepId() == null) {
				logger.warn("ExecutionStep or stepId is null, skipping complete agent execution recording");
				return;
			}

			// 1. Query by stepId in ExecutionStep
			Optional<AgentExecutionRecordEntity> agentRecordOpt = agentExecutionRecordRepository
				.findByStepId(step.getStepId());

			if (!agentRecordOpt.isPresent()) {
				logger.error("Agent execution record not found for stepId: {}, cannot complete recording",
						step.getStepId());
				return;
			}

			// 2. The entity should be in the database because we insert it before
			AgentExecutionRecordEntity agentRecord = agentRecordOpt.get();
			logger.debug("Found existing AgentExecutionRecordEntity for stepId: {}", step.getStepId());

			// 3. Update the entity with complete execution information

			// Set completion status based on agent state
			if (step.getAgent() != null) {
				ExecutionStatusEntity finalStatus = convertAgentStateToExecutionStatus(step.getAgent().getState());
				agentRecord.setStatus(finalStatus);

				// Update agent information
				if (step.getAgent().getName() != null) {
					agentRecord.setAgentName(step.getAgent().getName());
				}
				if (step.getAgent().getDescription() != null) {
					agentRecord.setAgentDescription(step.getAgent().getDescription());
				}

				logger.debug("Updated agent status to: {} for stepId: {}", finalStatus, step.getStepId());
			}
			else {
				// If no agent info available, mark as finished
				agentRecord.setStatus(ExecutionStatusEntity.FINISHED);
			}

			// Set end time if not already set
			if (agentRecord.getEndTime() == null) {
				agentRecord.setEndTime(LocalDateTime.now());
			}

			// Update step index if available
			if (step.getStepIndex() != null) {
				agentRecord.setCurrentStep(step.getStepIndex());
			}

			// Set final result if available
			if (step.getResult() != null && !step.getResult().isEmpty()) {
				agentRecord.setResult(step.getResult());
			}

			// Set step requirement as agent request if available
			if (step.getStepRequirement() != null && !step.getStepRequirement().isEmpty()) {
				agentRecord.setAgentRequest(step.getStepRequirement());
			}

			// Save the updated entity
			agentExecutionRecordRepository.save(agentRecord);

			logger.info("Successfully recorded complete agent execution for stepId: {}, final status: {}",
					step.getStepId(), agentRecord.getStatus());

		}
		catch (Exception e) {
			logger.error("Failed to record complete agent execution for stepId: {}",
					step != null ? step.getStepId() : "null", e);
		}
	}

	@Override
	public void recordPlanCompletion(String currentPlanId, String summary) {
		try {
			if (currentPlanId == null || currentPlanId.trim().isEmpty()) {
				logger.warn("currentPlanId is null or empty, skipping plan completion recording");
				return;
			}

			// 1. Find existing plan execution record by currentPlanId
			Optional<PlanExecutionRecordEntity> existingPlanOpt = planExecutionRecordRepository
				.findByCurrentPlanId(currentPlanId);

			if (!existingPlanOpt.isPresent()) {
				logger.error("Plan execution record not found for currentPlanId: {}, cannot record completion",
						currentPlanId);
				return;
			}

			// 2. The entity should be in the database because we created it in
			// recordPlanExecutionStart
			PlanExecutionRecordEntity planExecutionRecord = existingPlanOpt.get();
			logger.debug("Found existing PlanExecutionRecordEntity for currentPlanId: {}", currentPlanId);

			// 3. Mark plan as completed using the entity's complete method
			planExecutionRecord.complete(summary);

			logger.debug("Marked plan as completed for currentPlanId: {}, summary length: {}", currentPlanId,
					summary != null ? summary.length() : 0);

			// 4. Save the updated plan execution record
			planExecutionRecordRepository.save(planExecutionRecord);

			logger.info("Successfully recorded plan completion for currentPlanId: {}, completed: {}, endTime: {}",
					currentPlanId, planExecutionRecord.isCompleted(), planExecutionRecord.getEndTime());

		}
		catch (Exception e) {
			logger.error("Failed to record plan completion for currentPlanId: {}", currentPlanId, e);
		}
	}

	/**
	 * Get detailed agent execution record by stepId
	 * @param stepId The step ID to query
	 * @return Detailed agent execution record with ThinkActRecord details
	 */
	public AgentExecutionRecord getAgentExecutionDetail(String stepId) {
		try {
			if (stepId == null || stepId.trim().isEmpty()) {
				logger.warn("StepId is null or empty, cannot fetch agent execution detail");
				return null;
			}

			// Find the agent execution record by stepId
			Optional<AgentExecutionRecordEntity> agentRecordOpt = agentExecutionRecordRepository.findByStepId(stepId);

			if (!agentRecordOpt.isPresent()) {
				logger.warn("Agent execution record not found for stepId: {}", stepId);
				return null;
			}

			AgentExecutionRecordEntity agentRecord = agentRecordOpt.get();

			// Convert to AgentExecutionRecord
			AgentExecutionRecord detail = new AgentExecutionRecord(agentRecord.getStepId(), agentRecord.getAgentName(),
					agentRecord.getAgentDescription());

			// Set basic properties
			detail.setId(agentRecord.getId());
			detail.setStartTime(agentRecord.getStartTime());
			detail.setEndTime(agentRecord.getEndTime());
			detail.setStatus(convertToExecutionStatus(agentRecord.getStatus()));
			detail.setAgentRequest(agentRecord.getAgentRequest());
			detail.setResult(agentRecord.getResult());
			detail.setErrorMessage(agentRecord.getErrorMessage());
			detail.setModelName(agentRecord.getModelName());

			// Fetch and set ThinkActRecord details
			List<ThinkActRecord> thinkActSteps = fetchThinkActRecords(agentRecord.getId());
			detail.setThinkActSteps(thinkActSteps);

			logger.info("Successfully fetched agent execution detail for stepId: {} with {} think-act steps", stepId,
					thinkActSteps.size());

			return detail;
		}
		catch (Exception e) {
			logger.error("Error fetching agent execution detail for stepId: {}", stepId, e);
			return null;
		}
	}

	/**
	 * Fetch ThinkActRecord details for a given agent execution ID
	 * @param agentExecutionId The agent execution record ID
	 * @return List of ThinkActRecord
	 */
	private List<ThinkActRecord> fetchThinkActRecords(Long agentExecutionId) {
		try {
			// Find all ThinkActRecordEntity by parentExecutionId
			List<ThinkActRecordEntity> thinkActEntities = thinkActRecordRepository
				.findByParentExecutionId(agentExecutionId);

			List<ThinkActRecord> thinkActRecords = new ArrayList<>();

			for (ThinkActRecordEntity entity : thinkActEntities) {
				ThinkActRecord record = new ThinkActRecord(entity.getParentExecutionId());
				record.setId(entity.getId());
				record.setThinkInput(entity.getThinkInput());
				record.setThinkOutput(entity.getThinkOutput());
				record.setErrorMessage(entity.getErrorMessage());

				// Convert ActToolInfoEntity to ActToolInfo if available
				if (entity.getActToolInfoList() != null && !entity.getActToolInfoList().isEmpty()) {
					List<ActToolInfo> actToolInfoList = new ArrayList<>();
					for (ActToolInfoEntity toolInfoEntity : entity.getActToolInfoList()) {
						ActToolInfo actToolInfo = new ActToolInfo(toolInfoEntity.getName(),
								toolInfoEntity.getParameters(), toolInfoEntity.getToolCallId());
						actToolInfo.setResult(toolInfoEntity.getResult());
						actToolInfoList.add(actToolInfo);
					}
					record.setActToolInfoList(actToolInfoList);
					record.setActionNeeded(true);
				}

				thinkActRecords.add(record);
			}

			return thinkActRecords;
		}
		catch (Exception e) {
			logger.error("Error fetching ThinkActRecord details for agentExecutionId: {}", agentExecutionId, e);
			return new ArrayList<>();
		}
	}

	/**
	 * Convert ExecutionStatusEntity to ExecutionStatus
	 * @param statusEntity The entity status
	 * @return Corresponding ExecutionStatus
	 */
	private ExecutionStatus convertToExecutionStatus(ExecutionStatusEntity statusEntity) {
		if (statusEntity == null) {
			return ExecutionStatus.IDLE;
		}

		switch (statusEntity) {
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
	 * Creates a plan relationship by setting parent and root plan IDs. This method
	 * establishes the hierarchical structure between plans.
	 *
	 * Enhanced validation rules: 1. rootPlanId and currentPlanId are mandatory 2. If
	 * parentPlanId is provided, toolcallId must also be provided 3. If parentPlanId is
	 * provided, rootPlanId cannot be the same as currentPlanId
	 * @param currentPlanId The ID of the current plan
	 * @param parentPlanId The ID of the parent plan (can be null for root plans)
	 * @param rootPlanId The ID of the root plan (can be null for main plans)
	 * @param toolcallId The ID of the tool call that triggered this plan (can be null)
	 * @throws IllegalArgumentException if validation fails
	 * @throws IllegalStateException if plan record not found or save fails
	 */
	private void createPlanRelationship(String currentPlanId, String parentPlanId, String rootPlanId,
			String toolcallId) {
		// 1. Must have rootId and currentId
		if (currentPlanId == null || currentPlanId.trim().isEmpty()) {
			String errorMsg = "currentPlanId is null or empty, cannot create plan relationship";
			logger.error(errorMsg);
			throw new IllegalArgumentException(errorMsg);
		}

		if (rootPlanId == null || rootPlanId.trim().isEmpty()) {
			String errorMsg = "rootPlanId is null or empty, cannot create plan relationship";
			logger.error(errorMsg);
			throw new IllegalArgumentException(errorMsg);
		}

		// 2. If parentId is not empty, must have toolcallId
		if (parentPlanId != null && !parentPlanId.trim().isEmpty()) {
			if (toolcallId == null || toolcallId.trim().isEmpty()) {
				String errorMsg = String.format(
						"parentPlanId is provided but toolcallId is null or empty. parentPlanId: %s, currentPlanId: %s",
						parentPlanId, currentPlanId);
				logger.error(errorMsg);
				throw new IllegalArgumentException(errorMsg);
			}
		}

		// 3. If parentId is not empty, rootId and currentId cannot be the same
		if (parentPlanId != null && !parentPlanId.trim().isEmpty()) {
			if (rootPlanId.equals(currentPlanId)) {
				String errorMsg = String.format(
						"parentPlanId is provided but rootPlanId equals currentPlanId: %s, this is not allowed",
						currentPlanId);
				logger.error(errorMsg);
				throw new IllegalArgumentException(errorMsg);
			}
		}

		// Find the existing plan execution record
		var existingPlanOpt = planExecutionRecordRepository.findByCurrentPlanId(currentPlanId);

		if (!existingPlanOpt.isPresent()) {
			String errorMsg = String.format(
					"Plan execution record not found for currentPlanId: %s, cannot create relationship", currentPlanId);
			logger.error(errorMsg);
			throw new IllegalStateException(errorMsg);
		}

		PlanExecutionRecordEntity planRecord = existingPlanOpt.get();

		// Set the hierarchy relationships
		planRecord.setParentPlanId(parentPlanId);
		planRecord.setRootPlanId(rootPlanId);

		// Set the tool call ID if provided (for sub-plans triggered by tools)
		if (toolcallId != null && !toolcallId.trim().isEmpty()) {
			planRecord.setToolCallId(toolcallId);
		}

		// Save the updated entity
		try {
			planExecutionRecordRepository.save(planRecord);
			logger.info(
					"Successfully created plan relationship for currentPlanId: {}, parentPlanId: {}, rootPlanId: {}",
					currentPlanId, parentPlanId, rootPlanId);
		}
		catch (Exception e) {
			String errorMsg = String.format(
					"Failed to save plan relationship for currentPlanId: %s, parentPlanId: %s, rootPlanId: %s",
					currentPlanId, parentPlanId, rootPlanId);
			logger.error(errorMsg, e);
			throw new IllegalStateException(errorMsg, e);
		}
	}

}

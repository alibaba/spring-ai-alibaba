// /*
//  * Copyright 2025 the original author or authors.
//  *
//  * Licensed under the Apache License, Version 2.0 (the "License");
//  * you may not use this file except in compliance with the License.
//  * You may obtain a copy of the License at
//  *
//  *      https://www.apache.org/licenses/LICENSE-2.0
//  *
//  * Unless required by applicable law or agreed to in writing, software
//  * distributed under the License is distributed on an "AS IS" BASIS,
//  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  * See the License for the specific language governing permissions and
//  * limitations under the License.
//  */
// package com.alibaba.cloud.ai.example.manus.recorder.service;

// import com.alibaba.cloud.ai.example.manus.agent.AgentState;
// import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
// import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionStep;
// import com.alibaba.cloud.ai.example.manus.recorder.entity.po.*;
// import com.alibaba.cloud.ai.example.manus.recorder.repository.PlanExecutionRecordRepository;

// import jakarta.annotation.Resource;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.stereotype.Component;

// import java.time.LocalDateTime;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.Optional;

// /**
//  * Repository-based PlanExecutionRecorder implementation for persistent storage
//  * using PO entities.
//  * This implementation works directly with JPA entities from the po/ package.
//  */
// @Component
// public class RepositoryPlanExecutionRecorder implements PlanExecutionRecorder {

// 	private static final Logger logger = LoggerFactory.getLogger(RepositoryPlanExecutionRecorder.class);

// 	@Resource
// 	private PlanExecutionRecordRepository planExecutionRecordRepository;

// 	/**
// 	 * Record think-act execution with PlanExecutionRecordEntity parameter
// 	 * 
// 	 * @param planExecutionRecord Plan execution record entity
// 	 * @param agentExecutionId    Agent execution ID
// 	 * @param thinkActRecord      Think-act record entity
// 	 */
// 	@Override
// 	public void setThinkActExecution(
// 			com.alibaba.cloud.ai.example.manus.recorder.entity.vo.PlanExecutionRecord planExecutionRecord,
// 			Long agentExecutionId,
// 			com.alibaba.cloud.ai.example.manus.recorder.entity.vo.ThinkActRecord thinkActRecord) {
// 		// This method is kept for interface compatibility but delegates to PO-based
// 		// implementation
// 		logger.warn("setThinkActExecution called with VO objects - this should be updated to use PO entities");
// 	}

// 	/**
// 	 * Record plan completion with PlanExecutionRecordEntity parameter
// 	 * 
// 	 * @param planExecutionRecord Plan execution record entity
// 	 * @param summary             Execution summary
// 	 */
// 	@Override
// 	public void setPlanCompletion(
// 			com.alibaba.cloud.ai.example.manus.recorder.entity.vo.PlanExecutionRecord planExecutionRecord,
// 			String summary) {
// 		// This method is kept for interface compatibility but delegates to PO-based
// 		// implementation
// 		logger.warn("setPlanCompletion called with VO objects - this should be updated to use PO entities");
// 	}

// 	/**
// 	 * Delete execution record of the specified plan ID
// 	 * 
// 	 * @param planId Plan ID to delete
// 	 */
// 	@Override
// 	public void removeExecutionRecord(String planId) {
// 		planExecutionRecordRepository.deleteByPlanId(planId);
// 	}

// 	/**
// 	 * Record the start of plan execution.
// 	 */
// 	@Override
// 	public Long recordPlanExecutionStart(String currentPlanId, String title,
// 			String userRequset, List<ExecutionStep> executionSteps) {
// 		return createOrUpdatePlanExecutionRecordEntity(currentPlanId, title, userRequset, executionSteps);
// 	}
	

// 	/**
// 	 * Record the start of step execution.
// 	 */
// 	@Override
// 	public void recordStepStart(ExecutionStep step,String currentPlanI) {
		
// 		Optional<PlanExecutionRecordEntity> record = getPlanExecutionRecord(currentPlanId);

// 		if (record.isPresent()) {
// 			PlanExecutionRecordEntity planExecutionRecordEntity = record.get();
// 			int currentStepIndex = step.getStepIndex();
// 			planExecutionRecordEntity.setCurrentStepIndex(currentStepIndex);
// 			retrieveExecutionSteps(executionSteps, planExecutionRecordEntity);

// 			List<AgentExecutionRecordEntity> agentExecutionSequence = record.getAgentExecutionSequence();
// 			AgentExecutionRecordEntity agentExecutionRecord;

// 			if (agentExecutionSequence.size() > currentStepIndex) {
// 				agentExecutionRecord = agentExecutionSequence.get(currentStepIndex);
// 			} else {
// 				// Create and add new AgentExecutionRecord
// 				agentExecutionRecord = new AgentExecutionRecordEntity(currentPlanId, null, null);
// 				// Fill up to currentStepIndex
// 				while (agentExecutionSequence.size() < currentStepIndex) {
// 					agentExecutionSequence.add(new AgentExecutionRecordEntity());
// 				}
// 				agentExecutionSequence.add(agentExecutionRecord);
// 			}
// 			agentExecutionRecord.setStatus(ExecutionStatusEntity.RUNNING);

// 			savePlanExecutionRecordsEntity(record);
// 		}
// 	}

// 	/**
// 	 * Record step end
// 	 * 
// 	 * @param step    Execution step
// 	 * @param context Execution context
// 	 */
// 	@Override
// 	public void recordStepEnd(ExecutionStep step, ExecutionContext context) {
// 		String currentPlanId = context.getPlan().getCurrentPlanId();
// 		PlanExecutionRecordEntity record = getOrCreatePlanExecutionRecord(currentPlanId, true);

// 		if (record != null) {
// 			int currentStepIndex = step.getStepIndex();
// 			record.setCurrentStepIndex(currentStepIndex);
// 			retrieveExecutionSteps(context, record);

// 			List<AgentExecutionRecordEntity> agentExecutionSequence = record.getAgentExecutionSequence();
// 			// Check boundaries to ensure agentExecutionSequence has enough elements
// 			if (agentExecutionSequence.size() > currentStepIndex) {
// 				AgentExecutionRecordEntity agentExecutionRecord = agentExecutionSequence.get(currentStepIndex);
// 				agentExecutionRecord.setStatus(
// 						step.getStatus() == AgentState.COMPLETED ? ExecutionStatusEntity.FINISHED
// 								: ExecutionStatusEntity.RUNNING);
// 			} else {
// 				// If there is no corresponding AgentExecutionRecord, create a new one
// 				AgentExecutionRecordEntity agentExecutionRecord = new AgentExecutionRecordEntity(currentPlanId, null,
// 						null);
// 				agentExecutionRecord.setStatus(
// 						step.getStatus() == AgentState.COMPLETED ? ExecutionStatusEntity.FINISHED
// 								: ExecutionStatusEntity.RUNNING);

// 				// Fill up to currentStepIndex
// 				while (agentExecutionSequence.size() < currentStepIndex) {
// 					agentExecutionSequence.add(new AgentExecutionRecordEntity());
// 				}
// 				agentExecutionSequence.add(agentExecutionRecord);
// 			}

// 			savePlanExecutionRecordsEntity(record);
// 		}
// 	}

// 	/**
// 	 * Record complete agent execution at the end.
// 	 */
// 	@Override
// 	public void recordCompleteAgentExecution(PlanExecutionParams params) {
// 		if (params == null || params.getCurrentPlanId() == null) {
// 			return;
// 		}

// 		// Create agent execution record with all the final data
// 		AgentExecutionRecordEntity agentRecord = new AgentExecutionRecordEntity(params.getCurrentPlanId(),
// 				params.getAgentName(),
// 				params.getAgentDescription());
// 		agentRecord.setMaxSteps(params.getMaxSteps());
// 		agentRecord.setCurrentStep(params.getActualSteps());
// 		agentRecord.setErrorMessage(params.getErrorMessage());
// 		agentRecord.setResult(params.getResult());
// 		agentRecord.setStartTime(params.getStartTime());
// 		agentRecord.setEndTime(params.getEndTime());
// 		agentRecord.setStatus(params.getStatus());

// 		PlanExecutionRecordEntity planRecord = getOrCreatePlanExecutionRecord(params.getCurrentPlanId(), true);
// 		if (planRecord != null) {
// 			setAgentExecution(planRecord, agentRecord);
// 			savePlanExecutionRecords(planRecord);
// 		}
// 	}

// 	/**
// 	 * Record thinking and action execution process.
// 	 */
// 	@Override
// 	public Long recordThinkingAndAction(PlanExecutionParams params) {
// 		if (params.getCurrentPlanId() == null) {
// 			return null;
// 		}

// 		PlanExecutionRecordEntity planExecutionRecord = getOrCreatePlanExecutionRecord(params.getCurrentPlanId(), true);
// 		AgentExecutionRecordEntity agentExecutionRecord = getCurrentAgentExecutionRecord(planExecutionRecord);

// 		if (agentExecutionRecord == null && planExecutionRecord != null) {
// 			agentExecutionRecord = new AgentExecutionRecordEntity(params.getCurrentPlanId(), params.getAgentName(),
// 					params.getAgentDescription());
// 			setAgentExecution(planExecutionRecord, agentExecutionRecord);
// 		}

// 		if (agentExecutionRecord == null) {
// 			logger.error("Failed to create or retrieve AgentExecutionRecord for plan: {}", params.getCurrentPlanId());
// 			return null;
// 		}

// 		ThinkActRecordEntity thinkActRecord = new ThinkActRecordEntity(agentExecutionRecord.getId());
// 		thinkActRecord.setActStartTime(LocalDateTime.now());

// 		if (params.getModelName() != null) {
// 			planExecutionRecord.setModelName(params.getModelName());
// 			agentExecutionRecord.setModelName(params.getModelName());
// 		}

// 		if (params.getThinkInput() != null) {
// 			thinkActRecord.startThinking(params.getThinkInput());
// 		}
// 		if (params.getThinkOutput() != null) {
// 			thinkActRecord.finishThinking(params.getThinkOutput());
// 		}

// 		if (params.isActionNeeded() && params.getToolName() != null) {
// 			thinkActRecord.setActionNeeded(true);
// 			thinkActRecord.setToolName(params.getToolName());
// 			thinkActRecord.setToolParameters(params.getToolParameters());
// 			thinkActRecord.setStatus(ExecutionStatusEntity.RUNNING);
// 		}

// 		if (params.getErrorMessage() != null) {
// 			thinkActRecord.recordError(params.getErrorMessage());
// 		}

// 		if (planExecutionRecord != null) {
// 			setThinkActExecution(planExecutionRecord, agentExecutionRecord.getId(), thinkActRecord);
// 			savePlanExecutionRecords(planExecutionRecord);
// 		}

// 		return thinkActRecord.getId();
// 	}

// 	/**
// 	 * Record action execution result.
// 	 */
// 	@Override
// 	public void recordActionResult(PlanExecutionParams params) {
// 		if (params.getCurrentPlanId() == null || params.getCreatedThinkActRecordId() == null) {
// 			return;
// 		}

// 		PlanExecutionRecordEntity planExecutionRecord = getOrCreatePlanExecutionRecord(params.getCurrentPlanId(), true);
// 		AgentExecutionRecordEntity agentExecutionRecord = getCurrentAgentExecutionRecord(planExecutionRecord);

// 		// Additional safety check
// 		if (agentExecutionRecord == null) {
// 			logger.error("Failed to retrieve AgentExecutionRecord for plan: {} in recordActionResult",
// 					params.getCurrentPlanId());
// 			return;
// 		}

// 		// Find the ThinkActRecord by ID and update it with action results
// 		ThinkActRecordEntity thinkActRecord = findThinkActRecordInPlan(planExecutionRecord,
// 				params.getCreatedThinkActRecordId());

// 		if (thinkActRecord != null) {
// 			// Record action start if not already recorded
// 			if (params.getActionDescription() != null && params.getToolName() != null) {
// 				thinkActRecord.startAction(params.getActionDescription(), params.getToolName(),
// 						params.getToolParameters());
// 			}

// 			// Record action completion
// 			if (params.getActionResult() != null && params.getStatus() != null) {
// 				thinkActRecord.finishAction(params.getActionResult(), params.getStatus());
// 			}

// 			// Record error if any
// 			if (params.getErrorMessage() != null) {
// 				thinkActRecord.recordError(params.getErrorMessage());
// 			}

// 			// Set actToolInfoList if available
// 			if (params.getActToolInfoList() != null) {
// 				thinkActRecord.setActToolInfoList(params.getActToolInfoList());
// 			}

// 			// Set think-act execution to update the record
// 			setThinkActExecution(planExecutionRecord, agentExecutionRecord.getId(), thinkActRecord);

// 			// Save the execution records
// 			savePlanExecutionRecords(planExecutionRecord);
// 		} else {
// 			logger.warn("ThinkActRecord not found with ID: {} for plan: {}", params.getCreatedThinkActRecordId(),
// 					params.getCurrentPlanId());
// 		}
// 	}

// 	/**
// 	 * Record tool call intention before execution. This method records the
// 	 * intention
// 	 * to call a tool before the actual execution, providing a complete lifecycle
// 	 * tracking of tool calls.
// 	 */
// 	@Override
// 	public void recordToolCallIntention(PlanExecutionParams params) {
// 		if (params.getCurrentPlanId() == null || params.getCreatedThinkActRecordId() == null) {
// 			return;
// 		}

// 		PlanExecutionRecordEntity planExecutionRecord = getOrCreatePlanExecutionRecord(params.getCurrentPlanId(), true);
// 		AgentExecutionRecordEntity agentExecutionRecord = getCurrentAgentExecutionRecord(planExecutionRecord);

// 		// Additional safety check
// 		if (agentExecutionRecord == null) {
// 			logger.error("Failed to retrieve AgentExecutionRecord for plan: {} in recordToolCallIntention",
// 					params.getCurrentPlanId());
// 			return;
// 		}

// 		// Find the ThinkActRecord by ID and update it with tool call intention
// 		ThinkActRecordEntity thinkActRecord = findThinkActRecordInPlan(planExecutionRecord,
// 				params.getCreatedThinkActRecordId());

// 		if (thinkActRecord != null) {
// 			// Record tool call intention - this is before actual execution
// 			if (params.getActionDescription() != null && params.getToolName() != null) {
// 				thinkActRecord.startAction(params.getActionDescription(), params.getToolName(),
// 						params.getToolParameters());
// 			}

// 			// Set actToolInfoList if available for tool call intention
// 			if (params.getActToolInfoList() != null) {
// 				thinkActRecord.setActToolInfoList(params.getActToolInfoList());
// 			}

// 			// Set think-act execution to update the record
// 			setThinkActExecution(planExecutionRecord, agentExecutionRecord.getId(), thinkActRecord);

// 			// Save the execution records
// 			savePlanExecutionRecords(planExecutionRecord);

// 			logger.debug("Recorded tool call intention for plan: {}, tool: {}",
// 					params.getCurrentPlanId(), params.getToolName());
// 		} else {
// 			logger.warn("ThinkActRecord not found with ID: {} for plan: {} in recordToolCallIntention",
// 					params.getCreatedThinkActRecordId(), params.getCurrentPlanId());
// 		}
// 	}

// 	/**
// 	 * Record plan completion.
// 	 */
// 	@Override
// 	public void recordPlanCompletion(String currentPlanId, Long thinkActRecordId, String summary) {
// 		if (currentPlanId == null) {
// 			return;
// 		}

// 		PlanExecutionRecordEntity planExecutionRecord = getOrCreatePlanExecutionRecord(currentPlanId, false);
// 		if (planExecutionRecord != null) {
// 			setPlanCompletion(planExecutionRecord, summary);
// 			savePlanExecutionRecords(planExecutionRecord);
// 		}

// 		logger.info("Plan completed with ID: {} and summary: {}", currentPlanId, summary);
// 	}

// 	@Override
// 	public PlanExecutionRecordEntity getRootPlanExecutionRecord(String rootPlanId) {
// 		// For backward compatibility, treat rootPlanId as currentPlanId
// 		if (rootPlanId == null) {
// 			logger.warn("rootPlanId is null, cannot retrieve plan execution record");
// 			return null;
// 		}
// 		return getOrCreatePlanExecutionRecord(rootPlanId, false);
// 	}

// 	/**
// 	 * Gets plan execution record entity for currentPlanId
// 	 * 
// 	 * @param currentPlanId Current plan ID
// 	 * @return Optional containing plan execution record entity if found
// 	 */
// 	private Optional<PlanExecutionRecordEntity> getPlanExecutionRecord(String currentPlanId) {
// 		logger.debug("Enter getPlanExecutionRecord with currentPlanId: {}", currentPlanId);

// 		if (currentPlanId == null) {
// 			logger.error("currentPlanId is null");
// 			return Optional.empty();
// 		}

// 		// Get existing plan record entity
// 		return planExecutionRecordRepository.findByPlanId(currentPlanId);
// 	}

// 	/**
// 	 * Helper method to find ThinkActRecord in a plan
// 	 * 
// 	 * @param plan             Plan execution record
// 	 * @param thinkActRecordId Think-act record ID
// 	 * @return ThinkActRecord if found, null otherwise
// 	 */
// 	private ThinkActRecordEntity findThinkActRecordInPlan(PlanExecutionRecordEntity plan, Long thinkActRecordId) {
// 		if (plan == null || thinkActRecordId == null) {
// 			return null;
// 		}

// 		for (AgentExecutionRecordEntity agentRecord : plan.getAgentExecutionSequence()) {
// 			for (ThinkActRecordEntity thinkActRecord : agentRecord.getThinkActSteps()) {
// 				if (thinkActRecordId.equals(thinkActRecord.getId())) {
// 					return thinkActRecord;
// 				}
// 			}
// 		}
// 		return null;
// 	}

// 	/**
// 	 * Get current agent execution record for a specific plan execution record
// 	 * 
// 	 * @param planExecutionRecord Plan execution record
// 	 * @return Current active agent execution record, or null if none exists
// 	 */
// 	private AgentExecutionRecordEntity getCurrentAgentExecutionRecord(PlanExecutionRecordEntity planExecutionRecord) {
// 		if (planExecutionRecord != null) {
// 			List<AgentExecutionRecordEntity> agentExecutionSequence = planExecutionRecord.getAgentExecutionSequence();
// 			Integer currentIndex = planExecutionRecord.getCurrentStepIndex();
// 			if (!agentExecutionSequence.isEmpty() && currentIndex != null
// 					&& currentIndex < agentExecutionSequence.size()) {
// 				return agentExecutionSequence.get(currentIndex);
// 			}
// 		}
// 		return null;
// 	}

// 	private PlanExecutionRecordEntity getExecutionRecordEntity(String currentPlanId) {
// 		Optional<PlanExecutionRecordEntity> entityOpt = planExecutionRecordRepository.findByPlanId(currentPlanId);
// 		if (entityOpt.isPresent()) {
// 			PlanExecutionRecordEntity entity = entityOpt.get();
// 			// Convert PO entity to VO object using EntityToVoConverter
// 			return entity;
// 		}
// 		return null;
// 	}

// 	private void saveExecutionRecord(PlanExecutionRecordEntity planExecutionRecord) {
// 		Optional<PlanExecutionRecordEntity> entityOpt = planExecutionRecordRepository
// 				.findByPlanId(planExecutionRecord.getCurrentPlanId());

// 		PlanExecutionRecordEntity entity;
// 		if (entityOpt.isPresent()) {
// 			entity = entityOpt.get();
// 		} else {
// 			entity = new PlanExecutionRecordEntity(planExecutionRecord.getCurrentPlanId());
// 		}

// 		// Update entity fields from VO object
// 		entity.setTitle(planExecutionRecord.getTitle());
// 		entity.setUserRequest(planExecutionRecord.getUserRequest());
// 		entity.setStartTime(planExecutionRecord.getStartTime());
// 		entity.setEndTime(planExecutionRecord.getEndTime());
// 		entity.setSteps(planExecutionRecord.getSteps());
// 		entity.setCurrentStepIndex(planExecutionRecord.getCurrentStepIndex());
// 		entity.setCompleted(planExecutionRecord.isCompleted());
// 		entity.setSummary(planExecutionRecord.getSummary());
// 		entity.setModelName(planExecutionRecord.getModelName());
// 		entity.setUserInputWaitState(planExecutionRecord.getUserInputWaitState());

// 		// Note: Agent execution sequence conversion is complex and requires reverse
// 		// conversion methods
// 		// For now, we'll save the basic plan information without the agent execution
// 		// details
// 		// This can be enhanced later with proper reverse conversion methods

// 		planExecutionRecordRepository.save(entity);
// 	}

// 	private void addThinkActStep(AgentExecutionRecordEntity agentRecord, ThinkActRecordEntity thinkActRecord) {
// 		if (agentRecord.getThinkActSteps() == null) {
// 			agentRecord.addThinkActStep(thinkActRecord);
// 			return;
// 		}
// 		// Will be called multiple times, so need to modify based on id
// 		ThinkActRecordEntity exist = agentRecord.getThinkActSteps()
// 				.stream()
// 				.filter(r -> r.getId().equals(thinkActRecord.getId()))
// 				.findFirst()
// 				.orElse(null);
// 		if (exist == null) {
// 			agentRecord.getThinkActSteps().add(thinkActRecord);
// 		} else {
// 			// No BeanUtils.copyProperties as it's PO entities
// 			exist.setActStartTime(thinkActRecord.getActStartTime());
// 			exist.setActionNeeded(thinkActRecord.isActionNeeded());
// 			exist.setToolName(thinkActRecord.getToolName());
// 			exist.setToolParameters(thinkActRecord.getToolParameters());
// 			exist.setStatus(thinkActRecord.getStatus());
// 			exist.setResult(thinkActRecord.getResult());
// 			exist.setErrorMessage(thinkActRecord.getErrorMessage());
// 			exist.setActToolInfoList(thinkActRecord.getActToolInfoList());
// 		}
// 	}

// 	/**
// 	 * Retrieve execution step information and set it to the record.
// 	 */
// 	private void retrieveExecutionSteps(List<ExecutionStep> executionSteps, PlanExecutionRecordEntity record) {
// 		List<String> steps = new ArrayList<>();
// 		for (ExecutionStep step : executionSteps) {
// 			steps.add(step.getStepInStr());
// 		}
// 		record.setSteps(steps);
// 	}

// 	/**
// 	 * Record agent execution with PlanExecutionRecordEntity parameter
// 	 * 
// 	 * @param planExecutionRecord Plan execution record
// 	 * @param agentRecord         Agent execution record
// 	 * @return Agent execution ID
// 	 */
// 	private Long setAgentExecution(PlanExecutionRecordEntity planExecutionRecord,
// 			AgentExecutionRecordEntity agentRecord) {
// 		if (planExecutionRecord != null) {
// 			planExecutionRecord.addAgentExecutionRecord(agentRecord);
// 		}
// 		return agentRecord.getId();
// 	}

// 	/**
// 	 * Get current think-act record ID
// 	 * 
// 	 * @param currentPlanId Current plan ID
// 	 * @return Current think-act record ID, returns null if none exists
// 	 */
// 	@Override
// 	public Long getCurrentThinkActRecordId(String currentPlanId) {
// 		try {
// 			PlanExecutionRecordEntity planExecutionRecord = getOrCreatePlanExecutionRecord(currentPlanId, false);
// 			if (planExecutionRecord != null) {
// 				AgentExecutionRecordEntity currentAgentRecord = getCurrentAgentExecutionRecord(planExecutionRecord);
// 				if (currentAgentRecord != null && currentAgentRecord.getThinkActSteps() != null
// 						&& !currentAgentRecord.getThinkActSteps().isEmpty()) {
// 					List<ThinkActRecordEntity> steps = currentAgentRecord.getThinkActSteps();
// 					ThinkActRecordEntity lastStep = steps.get(steps.size() - 1);
// 					return lastStep.getId();
// 				}
// 			}
// 		} catch (Exception e) {
// 			logger.warn("Failed to get current think-act record ID: {}", e.getMessage());
// 		}

// 		return null;
// 	}

// 	/**
// 	 * Create or update AgentExecutionRecordEntity for a given ExecutionStep
// 	 * 
// 	 * @param step ExecutionStep to process
// 	 * @param currentPlanId Current plan ID
// 	 * @return AgentExecutionRecordEntity instance, or null if creation failed
// 	 */
// 	private AgentExecutionRecordEntity createOrUpdateAgentExecutionRecord(ExecutionStep step, String currentPlanId) {
// 	    try {
// 	        if (step == null || step.getStepId() == null) {
// 	            logger.warn("ExecutionStep or stepId is null, skipping agent record creation");
// 	            return null;
// 	        }
	        
// 	        // Check if agent execution record already exists for this step
// 	        // Since stepId is unique in AgentExecutionRecordEntity, we can search by it
// 	        // Note: This would require a repository method to find by stepId
// 	        // For now, we'll create a new one each time
	        
// 	        // Create new AgentExecutionRecordEntity
// 	        AgentExecutionRecordEntity agentRecord = new AgentExecutionRecordEntity(
// 	            step.getStepId(),
// 	            step.getAgent() != null ? step.getAgent().getName() : "Unknown Agent",
// 	            step.getAgent() != null ? step.getAgent().getDescription() : "No description available"
// 	        );
	        
// 	        // Set additional fields if available
// 	        if (step.getAgent() != null) {
// 	            agentRecord.setStatus(convertAgentStateToExecutionStatus(step.getAgent().getState()));
// 	        }
	        
// 	        // Set step index
// 	        agentRecord.setCurrentStep(step.getStepIndex() != null ? step.getStepIndex() : 0);
	        
// 	        logger.debug("Created AgentExecutionRecordEntity for step ID: {} with agent: {}", 
// 	                   step.getStepId(), agentRecord.getAgentName());
	        
// 	        return agentRecord;
	        
// 	    } catch (Exception e) {
// 	        logger.error("Failed to create AgentExecutionRecordEntity for step: {}", step.getStepId(), e);
// 	        return null;
// 	    }
// 	}
	
// 	/**
// 	 * Convert AgentState to ExecutionStatusEntity
// 	 * 
// 	 * @param agentState Agent state to convert
// 	 * @return Corresponding ExecutionStatusEntity
// 	 */
// 	private ExecutionStatusEntity convertAgentStateToExecutionStatus(AgentState agentState) {
// 	    if (agentState == null) {
// 	        return ExecutionStatusEntity.IDLE;
// 	    }
	    
// 	    switch (agentState) {
// 	        case NOT_STARTED:
// 	            return ExecutionStatusEntity.IDLE;
// 	        case IN_PROGRESS:
// 	            return ExecutionStatusEntity.RUNNING;
// 	        case COMPLETED:
// 	            return ExecutionStatusEntity.FINISHED;
// 	        default:
// 				throw new IllegalArgumentException("Invalid agent state: " + agentState);
// 	    }
// 	}

// 	/**
// 	 * Save execution records to persistent storage using PO entities
// 	 * 
// 	 * @param planExecutionRecord Plan execution record entity to save
// 	 * @return true if save was successful
// 	 */
// 	private boolean savePlanExecutionRecordsEntity(PlanExecutionRecordEntity planExecutionRecord) {
// 		try {
// 			planExecutionRecordRepository.save(planExecutionRecord);
// 			return true;
// 		} catch (Exception e) {
// 			logger.error("Failed to save plan execution record entity: {}", planExecutionRecord.getCurrentPlanId(), e);
// 			return false;
// 		}
// 	}

// 	/**
// 	 * Create or update plan execution record entity
// 	 * 
// 	 * @param currentPlanId    Current plan ID
// 	 * @param title            Plan title
// 	 * @param userRequest      User request
// 	 * @param executionSteps   Execution steps
// 	 * @return Plan execution record entity ID, or null if operation failed
// 	 */
// 	private Long createOrUpdatePlanExecutionRecordEntity(String currentPlanId, String title, String userRequest, List<ExecutionStep> executionSteps) {
// 	    try {
// 	        // Check if plan already exists
// 	        Optional<PlanExecutionRecordEntity> existingPlanOpt = planExecutionRecordRepository.findByPlanId(currentPlanId);
	        
// 	        PlanExecutionRecordEntity planExecutionRecordEntity;
// 	        if (existingPlanOpt.isPresent()) {
// 	            // Update existing plan
// 	            planExecutionRecordEntity = existingPlanOpt.get();
// 	            logger.debug("Updating existing plan execution record for ID: {}", currentPlanId);
// 	        } else {
// 	            // Create new plan
// 	            planExecutionRecordEntity = new PlanExecutionRecordEntity(currentPlanId);
// 	            logger.debug("Creating new plan execution record for ID: {}", currentPlanId);
// 	        }
	        
// 	        // Set/update all fields
// 	        planExecutionRecordEntity.setCurrentPlanId(currentPlanId);
// 	        planExecutionRecordEntity.setStartTime(LocalDateTime.now());
// 	        planExecutionRecordEntity.setTitle(title);
// 	        planExecutionRecordEntity.setUserRequest(userRequest);
// 	        retrieveExecutionSteps(executionSteps, planExecutionRecordEntity);
	        
// 	        // Process execution steps and create/update AgentExecutionRecordEntity instances
// 	        if (executionSteps != null && !executionSteps.isEmpty()) {
// 	            for (ExecutionStep step : executionSteps) {
// 	                // Create or update AgentExecutionRecordEntity for each step
// 	                AgentExecutionRecordEntity agentRecord = createOrUpdateAgentExecutionRecord(step, currentPlanId);
// 	                if (agentRecord != null) {
// 	                    // Add to plan execution record
// 	                    planExecutionRecordEntity.addAgentExecutionRecord(agentRecord);
// 	                }
// 	            }
// 	        }
	        
// 	        // Save the entity using repository
// 	        PlanExecutionRecordEntity savedEntity = planExecutionRecordRepository.save(planExecutionRecordEntity);
	        
// 	        logger.info("Successfully saved plan execution record for ID: {} with {} steps", currentPlanId, 
// 	                   executionSteps != null ? executionSteps.size() : 0);
// 	        return savedEntity.getId();
	        
// 	    } catch (Exception e) {
// 	        logger.error("Failed to create or update plan execution record for ID: {}", currentPlanId, e);
// 	        return null;
// 	    }
// 	}

// }

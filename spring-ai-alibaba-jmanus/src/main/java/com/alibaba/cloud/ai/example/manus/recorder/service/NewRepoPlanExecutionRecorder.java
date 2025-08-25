package com.alibaba.cloud.ai.example.manus.recorder.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionStep;
import com.alibaba.cloud.ai.example.manus.recorder.entity.po.AgentExecutionRecordEntity;
import com.alibaba.cloud.ai.example.manus.recorder.entity.po.PlanExecutionRecordEntity;
import com.alibaba.cloud.ai.example.manus.recorder.entity.po.ExecutionStatusEntity;
import com.alibaba.cloud.ai.example.manus.recorder.repository.PlanExecutionRecordRepository;
import com.alibaba.cloud.ai.example.manus.recorder.repository.AgentExecutionRecordRepository;
import com.alibaba.cloud.ai.example.manus.recorder.repository.ThinkActRecordRepository;
import com.alibaba.cloud.ai.example.manus.recorder.entity.po.ThinkActRecordEntity;
import com.alibaba.cloud.ai.example.manus.recorder.entity.po.ActToolInfoEntity;
import com.alibaba.cloud.ai.example.manus.agent.AgentState;
import com.alibaba.cloud.ai.example.manus.recorder.repository.ActToolInfoRepository;
import com.alibaba.cloud.ai.example.manus.recorder.service.PlanExecutionRecorder.ActToolParam;

import jakarta.annotation.Resource;

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

    @Override
    public Long recordPlanExecutionStart(String currentPlanId, String title, String userRequset,
            List<ExecutionStep> executionSteps) {
        try {
            // Check if plan already exists
            Optional<PlanExecutionRecordEntity> existingPlanOpt = planExecutionRecordRepository
                    .findByPlanId(currentPlanId);

            PlanExecutionRecordEntity planExecutionRecordEntity;
            if (existingPlanOpt.isPresent()) {
                // Update existing plan
                planExecutionRecordEntity = existingPlanOpt.get();
                logger.debug("Updating existing plan execution record for ID: {}", currentPlanId);
            } else {
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
                    String agentName = step.getAgent() != null ? step.getAgent().getName() : "Unknown Agent";
                    String description = step.getAgent() != null ? step.getAgent().getDescription() : "No description available";
                    AgentExecutionRecordEntity agentRecord = createOrUpdateAgentExecutionRecord(step, agentName, description);
                    if (agentRecord != null) {
                        // Add to plan execution record
                        planExecutionRecordEntity.addAgentExecutionRecord(agentRecord);
                    }
                }
            }

            // Save the entity using repository
            PlanExecutionRecordEntity savedEntity = planExecutionRecordRepository.save(planExecutionRecordEntity);

            logger.info("Successfully saved plan execution record for ID: {} with {} steps", currentPlanId,
                    executionSteps != null ? executionSteps.size() : 0);
            return savedEntity.getId();

        } catch (Exception e) {
            logger.error("Failed to create or update plan execution record for ID: {}", currentPlanId, e);
            return null;
        }
    }



	/**
	 * Create or update AgentExecutionRecordEntity for a given ExecutionStep
	 * 
	 * @param step ExecutionStep to process
	 * @param agentName Agent name
	 * @param description Agent description
	 * @return AgentExecutionRecordEntity instance, or null if creation failed
	 */
	private AgentExecutionRecordEntity createOrUpdateAgentExecutionRecord(ExecutionStep step, String agentName, String description) {
	    try {
	        if (step == null || step.getStepId() == null) {
	            logger.warn("ExecutionStep or stepId is null, skipping agent record creation");
	            return null;
	        }
	        
	        // 1. Find existing AgentExecutionRecordEntity by stepId
	        Optional<AgentExecutionRecordEntity> existingRecordOpt = agentExecutionRecordRepository.findByStepId(step.getStepId());
	        
	        AgentExecutionRecordEntity agentRecord;
	        if (existingRecordOpt.isPresent()) {
	            // 2. Update existing record
	            agentRecord = existingRecordOpt.get();
	            logger.debug("Updating existing AgentExecutionRecordEntity for step ID: {}", step.getStepId());
	            
	            // Update fields if they have changed
	            if (agentName != null) {
	                agentRecord.setAgentName(agentName);
	            }
	            if (description != null) {
	                agentRecord.setAgentDescription(description);
	            }
	            
	        } else {
	            // 2. Create new record
	            agentRecord = new AgentExecutionRecordEntity(
	                step.getStepId(),
	                agentName != null ? agentName : "Unknown Agent",
	                description != null ? description : "No description available"
	            );
	            logger.debug("Created new AgentExecutionRecordEntity for step ID: {}", step.getStepId());
	        }
	        
	        // Set additional fields
	        if (step.getAgent() != null) {
	            agentRecord.setStatus(convertAgentStateToExecutionStatus(step.getAgent().getState()));
	        }
	        
	        // Set step index
	        agentRecord.setCurrentStep(step.getStepIndex() != null ? step.getStepIndex() : 0);
	        
	        logger.debug("Processed AgentExecutionRecordEntity for step ID: {} with agent: {}", 
	                   step.getStepId(), agentRecord.getAgentName());
	        
	        return agentRecord;
	        
	    } catch (Exception e) {
	        logger.error("Failed to create or update AgentExecutionRecordEntity for step: {}", step.getStepId(), e);
	        return null;
	    }
	}
	
	/**
	 * Convert AgentState to ExecutionStatusEntity
	 * 
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
	            return ExecutionStatusEntity.FINISHED; // Both blocked and failed are considered finished states
	        default:
	            throw new IllegalArgumentException("Invalid agent state: " + agentState);
	    }
	}

    /**
     * Convert ActToolParam to ActToolInfoEntity
     * 
     * @param actToolParam ActToolParam to convert
     * @return Corresponding ActToolInfoEntity
     */
    private ActToolInfoEntity convertToActToolInfoEntity(ActToolParam actToolParam) {
        ActToolInfoEntity entity = new ActToolInfoEntity(
            actToolParam.getName(),
            actToolParam.getParameters(),
            actToolParam.getToolCallId()
        );
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
            Optional<AgentExecutionRecordEntity> agentRecordOpt = agentExecutionRecordRepository.findByStepId(step.getStepId());
            
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

        } catch (Exception e) {
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
            Optional<AgentExecutionRecordEntity> agentRecordOpt = agentExecutionRecordRepository.findByStepId(step.getStepId());
            
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
            
            logger.info("Successfully recorded step end for stepId: {}, planId: {}, status: FINISHED", 
                       step.getStepId(), currentPlanId);

        } catch (Exception e) {
            logger.error("Failed to record step end for stepId: {}, planId: {}", 
                        step != null ? step.getStepId() : "null", currentPlanId, e);
        }
    }



    @Override
    public Long recordThinkingAndAction(ExecutionStep step, ThinkActRecordParams params) {
        try {
            if (step == null || step.getStepId() == null || params == null) {
                logger.warn("ExecutionStep, stepId, or ThinkActRecordParams is null, skipping thinking and action recording");
                return null;
            }

            // 1. Query by stepId in ExecutionStep to get the agent execution record
            Optional<AgentExecutionRecordEntity> agentRecordOpt = agentExecutionRecordRepository.findByStepId(step.getStepId());
            
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
                List<ActToolInfoEntity> actToolInfoEntities = params.getActToolInfoList().stream()
                    .map(this::convertToActToolInfoEntity)
                    .collect(java.util.stream.Collectors.toList());
                thinkActRecord.setActToolInfoList(actToolInfoEntities);
            }

            // 4. Save the think-act record
            ThinkActRecordEntity savedThinkActRecord = thinkActRecordRepository.save(thinkActRecord);
            
            logger.info("Successfully recorded thinking and action for stepId: {}, thinkActRecordId: {}", 
                       step.getStepId(), savedThinkActRecord.getId());
            
            return savedThinkActRecord.getId();

        } catch (Exception e) {
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
                    Optional<ActToolInfoEntity> existingEntityOpt = actToolInfoRepository.findByToolCallId(actToolParam.getToolCallId());
                    
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
                        
                    } else {
                        logger.warn("No ActToolInfoEntity found with toolCallId: {}, creating new entity", 
                                   actToolParam.getToolCallId());
                        
                        // Create new entity if not found
                        ActToolInfoEntity newEntity = convertToActToolInfoEntity(actToolParam);
                        actToolInfoRepository.save(newEntity);
                        logger.debug("Created new ActToolInfoEntity with toolCallId: {}", 
                                   actToolParam.getToolCallId());
                    }
                    
                } catch (Exception e) {
                    logger.error("Failed to process ActToolParam with toolCallId: {}", 
                               actToolParam.getToolCallId(), e);
                }
            }
            
            logger.info("Successfully processed action results, processed {} tools", actToolParamList.size());

        } catch (Exception e) {
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
            Optional<AgentExecutionRecordEntity> agentRecordOpt = agentExecutionRecordRepository.findByStepId(step.getStepId());
            
            if (!agentRecordOpt.isPresent()) {
                logger.error("Agent execution record not found for stepId: {}, cannot complete recording", step.getStepId());
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
            } else {
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

        } catch (Exception e) {
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
                    .findByPlanId(currentPlanId);
            
            if (!existingPlanOpt.isPresent()) {
                logger.error("Plan execution record not found for currentPlanId: {}, cannot record completion", currentPlanId);
                return;
            }

            // 2. The entity should be in the database because we created it in recordPlanExecutionStart
            PlanExecutionRecordEntity planExecutionRecord = existingPlanOpt.get();
            logger.debug("Found existing PlanExecutionRecordEntity for currentPlanId: {}", currentPlanId);

            // 3. Mark plan as completed using the entity's complete method
            planExecutionRecord.complete(summary);
            
            logger.debug("Marked plan as completed for currentPlanId: {}, summary length: {}", 
                       currentPlanId, summary != null ? summary.length() : 0);

            // 4. Save the updated plan execution record
            planExecutionRecordRepository.save(planExecutionRecord);
            
            logger.info("Successfully recorded plan completion for currentPlanId: {}, completed: {}, endTime: {}", 
                       currentPlanId, planExecutionRecord.isCompleted(), planExecutionRecord.getEndTime());

        } catch (Exception e) {
            logger.error("Failed to record plan completion for currentPlanId: {}", currentPlanId, e);
        }
    }


}

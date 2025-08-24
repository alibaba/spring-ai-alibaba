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
package com.alibaba.cloud.ai.example.manus.recorder.service;

import com.alibaba.cloud.ai.example.manus.recorder.entity.po.PlanExecutionRecordEntity;
import com.alibaba.cloud.ai.example.manus.recorder.entity.vo.PlanExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.repository.PlanExecutionRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.Optional;

/**
 * Service for managing plan hierarchies using existing parentPlanId and actToolInfoEntityId fields.
 * This service replaces the need for a separate PlanRelationship table.
 */
@Service
public class PlanHierarchyService {

    private static final Logger logger = LoggerFactory.getLogger(PlanHierarchyService.class);

    @Autowired
    private PlanExecutionRecordRepository planExecutionRecordRepository;

    @Autowired
    private EntityToVoConverter entityToVoConverter;

    /**
     * Get all direct child plans for a given parent plan (returns VO objects)
     */
    @Transactional(readOnly = true)
    public List<PlanExecutionRecord> getChildrenByParentId(String parentPlanId) {
        if (parentPlanId == null) {
            return Collections.emptyList();
        }
        try {
            // First get entities, then convert to VO objects
            List<PlanExecutionRecordEntity> entities = planExecutionRecordRepository.findByParentPlanId(parentPlanId);
            return entityToVoConverter.convertToPlanExecutionRecordList(entities);
        } catch (Exception e) {
            logger.error("Failed to get children for parent plan: {}", parentPlanId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get all plans in an execution chain by root plan ID (returns VO objects)
     */
    @Transactional(readOnly = true)
    public List<PlanExecutionRecord> getPlansByRootId(String rootPlanId) {
        if (rootPlanId == null) {
            return Collections.emptyList();
        }
        try {
            // First get entities, then convert to VO objects
            List<PlanExecutionRecordEntity> entities = planExecutionRecordRepository.findByRootPlanId(rootPlanId);
            return entityToVoConverter.convertToPlanExecutionRecordList(entities);
        } catch (Exception e) {
            logger.error("Failed to get plans for root plan: {}", rootPlanId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get the parent plan for a given child plan (returns VO object)
     */
    @Transactional(readOnly = true)
    public PlanExecutionRecord getParentPlan(String childPlanId) {
        if (childPlanId == null) {
            return null;
        }
        try {
            PlanExecutionRecordEntity childPlan = planExecutionRecordRepository.findByCurrentPlanId(childPlanId)
                    .orElse(null);
            if (childPlan == null || childPlan.getParentPlanId() == null) {
                return null;
            }
            PlanExecutionRecordEntity parentEntity = planExecutionRecordRepository.findByCurrentPlanId(childPlan.getParentPlanId())
                    .orElse(null);
            return entityToVoConverter.convertToPlanExecutionRecord(parentEntity);
        } catch (Exception e) {
            logger.error("Failed to get parent plan for child: {}", childPlanId, e);
            return null;
        }
    }

    /**
     * Get the root plan for a given plan (traverses up the hierarchy) (returns VO object)
     */
    @Transactional(readOnly = true)
    public PlanExecutionRecord getRootPlan(String planId) {
        if (planId == null) {
            return null;
        }
        try {
            PlanExecutionRecordEntity currentPlan = planExecutionRecordRepository.findByCurrentPlanId(planId)
                    .orElse(null);
            if (currentPlan == null) {
                return null;
            }
            if (currentPlan.getRootPlanId() != null) {
                PlanExecutionRecordEntity rootEntity = planExecutionRecordRepository.findByCurrentPlanId(currentPlan.getRootPlanId())
                        .orElse(null);
                return entityToVoConverter.convertToPlanExecutionRecord(rootEntity);
            }
            String currentParentId = currentPlan.getParentPlanId();
            while (currentParentId != null) {
                PlanExecutionRecordEntity parentPlan = planExecutionRecordRepository.findByCurrentPlanId(currentParentId)
                        .orElse(null);
                if (parentPlan == null) {
                    break;
                }
                if (parentPlan.getRootPlanId() != null) {
                    PlanExecutionRecordEntity rootEntity = planExecutionRecordRepository.findByCurrentPlanId(parentPlan.getRootPlanId())
                            .orElse(null);
                    return entityToVoConverter.convertToPlanExecutionRecord(rootEntity);
                }
                currentParentId = parentPlan.getParentPlanId();
            }
            return entityToVoConverter.convertToPlanExecutionRecord(currentPlan);
        } catch (Exception e) {
            logger.error("Failed to get root plan for plan: {}", planId, e);
            return null;
        }
    }

    	/**
	 * Get the depth of a plan in the execution hierarchy
	 */
	@Transactional(readOnly = true)
	public Integer getPlanDepth(String planId) {
		if (planId == null) {
			return null;
		}
		try {
			int depth = 0;
			String currentPlanId = planId;
			while (currentPlanId != null) {
				PlanExecutionRecordEntity currentPlan = planExecutionRecordRepository.findByCurrentPlanId(currentPlanId)
						.orElse(null);
				if (currentPlan == null) {
					break;
				}
				String parentId = currentPlan.getParentPlanId();
				if (parentId == null) {
					return depth;
				}
				depth++;
				currentPlanId = parentId;
			}
			return depth;
		} catch (Exception e) {
			logger.error("Failed to calculate plan depth for plan: {}", planId, e);
			return null;
		}
	}

	/**
	 * Create relationship between parent and current execution plan
	 * @param currentPlanId The current plan ID
	 * @param parentPlanId The parent plan ID (must not be null)
	 * @param rootPlanId The root plan ID
	 * @param toolcallId The tool call ID that triggered this execution (can be null)
	 * @return true if relationship was created successfully, false otherwise
	 */
	@Transactional
	public boolean createPlanRelationship(String currentPlanId, String parentPlanId, String rootPlanId, String toolcallId) {
		if (currentPlanId == null) {
			logger.error("Cannot create plan relationship: currentPlanId is null");
			return false;
		}
		
		if (parentPlanId == null) {
			logger.error("Cannot create plan relationship: parentPlanId is null");
			return false;
		}
        if (rootPlanId == null) {
           throw new IllegalArgumentException("rootPlanId is null");
        }
		
		try {
			// Check if the current plan already exists
			Optional<PlanExecutionRecordEntity> existingPlan = planExecutionRecordRepository.findByCurrentPlanId(currentPlanId);
			
			if (existingPlan.isPresent()) {
				// Update existing plan with relationship information
				PlanExecutionRecordEntity planEntity = existingPlan.get();
				planEntity.setParentPlanId(parentPlanId);
				planEntity.setRootPlanId(rootPlanId);
				
				// If toolcallId is provided, we might need to set it in the context
				// For now, we'll log it for tracking purposes
				if (toolcallId != null) {
					logger.debug("Plan {} was triggered by tool call: {}", currentPlanId, toolcallId);
				}
				
				planExecutionRecordRepository.save(planEntity);
				logger.info("Updated plan relationship for plan: {} -> parent: {}, root: {}", 
					currentPlanId, parentPlanId, rootPlanId);
				return true;
			} else {
				// Create new plan entity with relationship information
				PlanExecutionRecordEntity newPlanEntity = new PlanExecutionRecordEntity(currentPlanId);
				newPlanEntity.setParentPlanId(parentPlanId);
				newPlanEntity.setRootPlanId(rootPlanId);
				
				// Set basic information
				newPlanEntity.setTitle("Plan " + currentPlanId);
				newPlanEntity.setStartTime(java.time.LocalDateTime.now());
				
				planExecutionRecordRepository.save(newPlanEntity);
				logger.info("Created new plan relationship for plan: {} -> parent: {}, root: {}", 
					currentPlanId, parentPlanId, rootPlanId);
				return true;
			}
		} catch (Exception e) {
			logger.error("Failed to create plan relationship for plan: {}", currentPlanId, e);
			return false;
		}
	}

}

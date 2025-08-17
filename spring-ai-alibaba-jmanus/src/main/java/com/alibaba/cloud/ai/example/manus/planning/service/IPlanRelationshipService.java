package com.alibaba.cloud.ai.example.manus.planning.service;

import com.alibaba.cloud.ai.example.manus.planning.model.po.PlanRelationship;

import java.util.List;
import java.util.Map;

/**
 * Service interface for managing plan relationships
 * Provides methods to record and query parent-child relationships between plans
 */
public interface IPlanRelationshipService {
    
    /**
     * Record a new plan relationship
     * @param parentPlanId The parent plan ID (can be null for root plans)
     * @param childPlanId The child plan ID (current executing plan)
     * @param rootPlanId The root plan ID (top-level parent plan)
     * @param planTemplateId The plan template ID if applicable
     * @param relationshipType The type of relationship
     */
    void recordPlanRelationship(String parentPlanId, String childPlanId, String rootPlanId, 
                              String planTemplateId, String relationshipType);
    
    /**
     * Get all child relationships for a given parent plan
     * @param parentPlanId The parent plan ID
     * @return List of child relationships
     */
    List<PlanRelationship> getChildrenByParentId(String parentPlanId);
    
    /**
     * Get all relationships in an execution chain by root plan ID
     * @param rootPlanId The root plan ID
     * @return List of all relationships in the execution chain
     */
    List<PlanRelationship> getChildrenByRootId(String rootPlanId);
    
    /**
     * Get relationship information by child plan ID
     * @param childPlanId The child plan ID
     * @return The relationship record
     */
    PlanRelationship getRelationshipByChildId(String childPlanId);
    
    /**
     * Get the depth of a plan in the execution hierarchy
     * @param planId The plan ID
     * @return The depth level (0 for root, 1 for first level children, etc.)
     */
    int getPlanDepth(String planId);
}

package com.alibaba.cloud.ai.example.manus.planning.repository;

import com.alibaba.cloud.ai.example.manus.planning.model.po.PlanRelationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for PlanRelationship entity
 * Provides data access methods for plan relationship operations
 */
@Repository
public interface PlanRelationshipRepository extends JpaRepository<PlanRelationship, Long> {
    
    /**
     * Find all relationships by root plan ID ordered by creation time
     * @param rootPlanId The root plan ID
     * @return List of relationships ordered by creation time
     */
    List<PlanRelationship> findByRootPlanIdOrderByCreatedTimeAsc(String rootPlanId);
    
    /**
     * Find all relationships by parent plan ID ordered by creation time
     * @param parentPlanId The parent plan ID
     * @return List of child relationships ordered by creation time
     */
    List<PlanRelationship> findByParentPlanIdOrderByCreatedTimeAsc(String parentPlanId);
    
    /**
     * Find relationship by child plan ID
     * @param childPlanId The child plan ID
     * @return The relationship record
     */
    PlanRelationship findByChildPlanId(String childPlanId);
}

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
package com.alibaba.cloud.ai.example.manus.planning.model.po;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA entity for storing parent-child relationships between plans
 * This entity tracks the execution hierarchy and relationships between different plan executions
 */
@Entity
@Table(name = "plan_relationships", indexes = {
    @Index(name = "idx_parent_plan_id", columnList = "parent_plan_id"),
    @Index(name = "idx_child_plan_id", columnList = "child_plan_id"),
    @Index(name = "idx_root_plan_id", columnList = "root_plan_id"),
    @Index(name = "idx_created_time", columnList = "created_time")
})
public class PlanRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Parent plan ID (can be null for root plans)
     */
    @Column(name = "parent_plan_id", length = 255)
    private String parentPlanId;

    /**
     * Child plan ID (current executing plan)
     */
    @Column(name = "child_plan_id", length = 255, nullable = false)
    private String childPlanId;

    /**
     * Root plan ID (top-level parent plan)
     */
    @Column(name = "root_plan_id", length = 255, nullable = false)
    private String rootPlanId;

    /**
     * Plan template ID if this is a template-based execution
     */
    @Column(name = "plan_template_id", length = 255)
    private String planTemplateId;

    /**
     * Relationship type (e.g., "subplan", "template", "user-query")
     */
    @Column(name = "relationship_type", length = 100)
    private String relationshipType;

    /**
     * When this relationship record was created
     */
    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;

    /**
     * When this relationship record was last updated
     */
    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    // Default constructor
    public PlanRelationship() {
        this.createdTime = LocalDateTime.now();
        this.updatedTime = LocalDateTime.now();
    }

    // Constructor with required fields
    public PlanRelationship(String childPlanId, String rootPlanId) {
        this();
        this.childPlanId = childPlanId;
        this.rootPlanId = rootPlanId;
    }

    // Constructor with parent-child relationship
    public PlanRelationship(String parentPlanId, String childPlanId, String rootPlanId) {
        this(childPlanId, rootPlanId);
        this.parentPlanId = parentPlanId;
    }

    // Constructor with all fields
    public PlanRelationship(String parentPlanId, String childPlanId, String rootPlanId, 
                          String planTemplateId, String relationshipType) {
        this(parentPlanId, childPlanId, rootPlanId);
        this.planTemplateId = planTemplateId;
        this.relationshipType = relationshipType;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getParentPlanId() {
        return parentPlanId;
    }

    public void setParentPlanId(String parentPlanId) {
        this.parentPlanId = parentPlanId;
        this.updatedTime = LocalDateTime.now();
    }

    public String getChildPlanId() {
        return childPlanId;
    }

    public void setChildPlanId(String childPlanId) {
        this.childPlanId = childPlanId;
        this.updatedTime = LocalDateTime.now();
    }

    public String getRootPlanId() {
        return rootPlanId;
    }

    public void setRootPlanId(String rootPlanId) {
        this.rootPlanId = rootPlanId;
        this.updatedTime = LocalDateTime.now();
    }

    public String getPlanTemplateId() {
        return planTemplateId;
    }

    public void setPlanTemplateId(String planTemplateId) {
        this.planTemplateId = planTemplateId;
        this.updatedTime = LocalDateTime.now();
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
        this.updatedTime = LocalDateTime.now();
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    // Utility methods
    public boolean isRootPlan() {
        return parentPlanId == null;
    }

    @Override
    public String toString() {
        return "PlanRelationship{" +
                "id=" + id +
                ", parentPlanId='" + parentPlanId + '\'' +
                ", childPlanId='" + childPlanId + '\'' +
                ", rootPlanId='" + rootPlanId + '\'' +
                ", planTemplateId='" + planTemplateId + '\'' +
                ", relationshipType='" + relationshipType + '\'' +
                ", createdTime=" + createdTime +
                ", updatedTime=" + updatedTime +
                '}';
    }
}

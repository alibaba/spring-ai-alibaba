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
package com.alibaba.cloud.ai.example.manus.planning.controller.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response object for execution tree API endpoint.
 * Provides a strongly-typed structure for the execution tree data.
 * 
 * @author JManus Team
 * @since 1.0.0
 */
public class ExecutionTreeResponse {

    /**
     * The root plan ID that represents the top-level execution plan
     */
    @JsonProperty("rootPlanId")
    private String rootPlanId;

    /**
     * The main execution tree node containing plan details and steps
     */
    @JsonProperty("tree")
    private ExecutionTreeNode tree;

    /**
     * Default constructor for Jackson deserialization
     */
    public ExecutionTreeResponse() {
    }

    /**
     * Constructor with required fields
     * 
     * @param rootPlanId The root plan ID
     * @param tree The execution tree node
     */
    public ExecutionTreeResponse(String rootPlanId, ExecutionTreeNode tree) {
        this.rootPlanId = rootPlanId;
        this.tree = tree;
    }

    // Getters and Setters
    public String getRootPlanId() {
        return rootPlanId;
    }

    public void setRootPlanId(String rootPlanId) {
        this.rootPlanId = rootPlanId;
    }

    public ExecutionTreeNode getTree() {
        return tree;
    }

    public void setTree(ExecutionTreeNode tree) {
        this.tree = tree;
    }

    @Override
    public String toString() {
        return "ExecutionTreeResponse{" +
                "rootPlanId='" + rootPlanId + '\'' +
                ", tree=" + tree +
                '}';
    }
}

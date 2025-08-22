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

/**
 * Enum representing the execution status of a plan.
 * Provides clear, type-safe status values for plan execution states.
 * 
 * @author JManus Team
 * @since 1.0.0
 */
public enum PlanExecutionStatus {

    /**
     * Plan is waiting to start execution
     */
    PENDING("pending"),

    /**
     * Plan is currently being executed
     */
    RUNNING("running"),

    /**
     * Plan execution has completed successfully
     */
    COMPLETED("completed");

    private final String value;

    /**
     * Constructor with string value
     * 
     * @param value The string representation of the status
     */
    PlanExecutionStatus(String value) {
        this.value = value;
    }

    /**
     * Get the string value of the status
     * 
     * @return The string representation
     */
    public String getValue() {
        return value;
    }

    /**
     * Get the enum from string value
     * 
     * @param value The string value to convert
     * @return The corresponding enum value, or null if not found
     */
    public static PlanExecutionStatus fromValue(String value) {
        for (PlanExecutionStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return value;
    }
}

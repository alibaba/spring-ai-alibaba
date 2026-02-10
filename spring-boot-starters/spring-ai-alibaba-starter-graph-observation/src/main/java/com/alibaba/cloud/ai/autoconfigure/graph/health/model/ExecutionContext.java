package com.alibaba.cloud.ai.autoconfigure.graph.health.model;

import java.time.Duration;

/**
 * Context object capturing agent execution metadata.
 * Used for metrics collection.
 */
public class ExecutionContext {

    private final String agentName;
    private final String agentType;
    private final Duration duration;
    private final ExecutionStatus status;
    private final String errorMessage;

    /**
     * Execution status enumeration.
     */
    public enum ExecutionStatus {
        SUCCESS,
        FAILED,
        TIMEOUT,
        CANCELLED
    }

    // Private constructor - use Builder
    private ExecutionContext(Builder builder) {
        this.agentName = builder.agentName;
        this.agentType = builder.agentType;
        this.duration = builder.duration;
        this.status = builder.status;
        this.errorMessage = builder.errorMessage;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for ExecutionContext.
     */
    public static class Builder {
        private String agentName;
        private String agentType = "ReactAgent";
        private Duration duration;
        private ExecutionStatus status = ExecutionStatus.SUCCESS;
        private String errorMessage;

        public Builder agentName(String agentName) {
            this.agentName = agentName;
            return this;
        }

        public Builder agentType(String agentType) {
            this.agentType = agentType;
            return this;
        }

        public Builder duration(Duration duration) {
            this.duration = duration;
            return this;
        }

        public Builder status(ExecutionStatus status) {
            this.status = status;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public ExecutionContext build() {
            return new ExecutionContext(this);
        }
    }

    // Getters
    public String getAgentName() {
        return agentName;
    }

    public String getAgentType() {
        return agentType;
    }

    public Duration getDuration() {
        return duration;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}

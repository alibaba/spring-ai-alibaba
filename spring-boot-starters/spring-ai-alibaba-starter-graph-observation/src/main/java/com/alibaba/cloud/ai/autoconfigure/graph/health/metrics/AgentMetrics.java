package com.alibaba.cloud.ai.autoconfigure.graph.health.metrics;

import com.alibaba.cloud.ai.autoconfigure.graph.health.config.ObservabilityProperties;
import com.alibaba.cloud.ai.autoconfigure.graph.health.model.ExecutionContext;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

/**
 * Metrics collector for agent execution lifecycle.
 */
public class AgentMetrics {

    private static final String METRIC_PREFIX = "spring.ai.agent";
    
    private final MeterRegistry meterRegistry;
    private final ObservabilityProperties properties;

    public AgentMetrics(MeterRegistry meterRegistry, ObservabilityProperties properties) {
        this.meterRegistry = meterRegistry;
        this.properties = properties;
    }

    /**
     * Record agent execution metrics.
     */
    public void recordExecution(ExecutionContext context) {
        Tags tags = Tags.of(
            "agent.name", context.getAgentName(),
            "agent.type", context.getAgentType(),
            "status", context.getStatus().name()
        );

        // Timer for execution duration
        Timer.builder(METRIC_PREFIX + ".execution.duration")
            .description("Agent execution duration")
            .tags(tags)
            .register(meterRegistry)
            .record(context.getDuration());

        // Counter for total executions
        Counter.builder(METRIC_PREFIX + ".execution.total")
            .description("Total agent executions")
            .tags(tags)
            .register(meterRegistry)
            .increment();
    }

    /**
     * Record tool invocation.
     */
    public void recordToolCall(String agentName, String toolName, boolean success) {
        Tags tags = Tags.of(
            "agent.name", agentName,
            "tool.name", toolName,
            "status", success ? "SUCCESS" : "FAILED"
        );

        Counter.builder(METRIC_PREFIX + ".tool.calls")
            .description("Tool invocation count")
            .tags(tags)
            .register(meterRegistry)
            .increment();
    }

    /**
     * Record agent state transition (for workflow agents).
     */
    public void recordStateTransition(String agentName, String fromState, String toState) {
        Tags tags = Tags.of(
            "agent.name", agentName,
            "from.state", fromState,
            "to.state", toState
        );

        Counter.builder(METRIC_PREFIX + ".state.transitions")
            .description("Graph state transitions")
            .tags(tags)
            .register(meterRegistry)
            .increment();
    }
}

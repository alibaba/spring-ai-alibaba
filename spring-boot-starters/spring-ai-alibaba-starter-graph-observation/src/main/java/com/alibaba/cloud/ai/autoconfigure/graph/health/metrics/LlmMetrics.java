package com.alibaba.cloud.ai.autoconfigure.graph.health.metrics;

import java.time.Duration;

import com.alibaba.cloud.ai.autoconfigure.graph.health.config.ObservabilityProperties;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

/**
 * Metrics collector for LLM API calls.
 */
public class LlmMetrics {

    private static final String METRIC_PREFIX = "spring.ai.llm";
    
    private final MeterRegistry meterRegistry;
    private final ObservabilityProperties properties;

    public LlmMetrics(MeterRegistry meterRegistry, ObservabilityProperties properties) {
        this.meterRegistry = meterRegistry;
        this.properties = properties;
    }

    /**
     * Record LLM API call metrics.
     */
    public void recordLlmCall(String modelName, 
                              Duration duration,
                              long inputTokens, 
                              long outputTokens,
                              boolean success) {
        Tags baseTags = Tags.of(
            "model.name", modelName != null ? modelName : "unknown",
            "status", success ? "SUCCESS" : "FAILED"
        );

        // API call latency (only if duration is provided)
        if (duration != null && !duration.isZero()) {
            Timer.builder(METRIC_PREFIX + ".request.duration")
                .description("LLM API call duration")
                .tags(baseTags)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry)
                .record(duration);
        }

        if (properties.isDetailedTokenMetrics()) {
            recordTokenUsage(modelName, inputTokens, outputTokens);
        }

        if (properties.isEnableCostEstimation()) {
            double estimatedCost = calculateCost(inputTokens, outputTokens);
            recordCost(modelName, estimatedCost);
        }
    }

    private void recordTokenUsage(String modelName, long inputTokens, long outputTokens) {
        String safeModelName = modelName != null ? modelName : "unknown";
        
        // Input tokens
        DistributionSummary.builder(METRIC_PREFIX + ".tokens.input")
            .description("Input tokens per request")
            .baseUnit("tokens")
            .tags("model.name", safeModelName)
            .register(meterRegistry)
            .record(inputTokens);

        // Output tokens
        DistributionSummary.builder(METRIC_PREFIX + ".tokens.output")
            .description("Output tokens per request")
            .baseUnit("tokens")
            .tags("model.name", safeModelName)
            .register(meterRegistry)
            .record(outputTokens);

        // Total tokens
        DistributionSummary.builder(METRIC_PREFIX + ".tokens.total")
            .description("Total tokens per request")
            .baseUnit("tokens")
            .tags("model.name", safeModelName)
            .register(meterRegistry)
            .record(inputTokens + outputTokens);
    }

    private void recordCost(String modelName, double cost) {
        Counter.builder(METRIC_PREFIX + ".cost.estimated")
            .description("Estimated LLM cost in USD")
            .baseUnit("usd")
            .tags("model.name", modelName != null ? modelName : "unknown")
            .register(meterRegistry)
            .increment(cost);
    }

    private double calculateCost(long inputTokens, long outputTokens) {
        double inputCost = (inputTokens / 1_000_000.0) 
            * properties.getPricing().getInputTokenCostPerMillion();
        double outputCost = (outputTokens / 1_000_000.0) 
            * properties.getPricing().getOutputTokenCostPerMillion();
        return inputCost + outputCost;
    }

    /**
     * Record LLM error.
     */
    public void recordError(String modelName, String errorType) {
        Tags tags = Tags.of(
            "model.name", modelName != null ? modelName : "unknown",
            "error.type", errorType
        );

        Counter.builder(METRIC_PREFIX + ".errors")
            .description("LLM API errors")
            .tags(tags)
            .register(meterRegistry)
            .increment();
    }
}

package com.alibaba.cloud.ai.autoconfigure.graph.health.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for LLM observability features.
 */
@ConfigurationProperties(prefix = "spring.ai.alibaba.observability")
public class ObservabilityProperties {

    /**
     * Enable/disable observability metrics collection.
     */
    private boolean enabled = true;

    /**
     * Enable detailed token usage metrics (input/output/total).
     * May have minor performance impact.
     */
    private boolean detailedTokenMetrics = true;

    /**
     * Enable cost estimation based on token usage.
     * Requires pricing configuration.
     */
    private boolean enableCostEstimation = false;

    /**
     * Pricing configuration for cost estimation.
     */
    private PricingConfig pricing = new PricingConfig();

    public static class PricingConfig {
        /**
         * Cost per 1 million input tokens (USD).
         * Example: For DashScope qwen-max, typically 0.04 USD per 1M tokens.
         */
        private double inputTokenCostPerMillion = 0.0;

        /**
         * Cost per 1 million output tokens (USD).
         * Example: For DashScope qwen-max, typically 0.12 USD per 1M tokens.
         */
        private double outputTokenCostPerMillion = 0.0;

        // Getters and Setters
        public double getInputTokenCostPerMillion() {
            return inputTokenCostPerMillion;
        }

        public void setInputTokenCostPerMillion(double inputTokenCostPerMillion) {
            this.inputTokenCostPerMillion = inputTokenCostPerMillion;
        }

        public double getOutputTokenCostPerMillion() {
            return outputTokenCostPerMillion;
        }

        public void setOutputTokenCostPerMillion(double outputTokenCostPerMillion) {
            this.outputTokenCostPerMillion = outputTokenCostPerMillion;
        }
    }

    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDetailedTokenMetrics() {
        return detailedTokenMetrics;
    }

    public void setDetailedTokenMetrics(boolean detailedTokenMetrics) {
        this.detailedTokenMetrics = detailedTokenMetrics;
    }

    public boolean isEnableCostEstimation() {
        return enableCostEstimation;
    }

    public void setEnableCostEstimation(boolean enableCostEstimation) {
        this.enableCostEstimation = enableCostEstimation;
    }

    public PricingConfig getPricing() {
        return pricing;
    }

    public void setPricing(PricingConfig pricing) {
        this.pricing = pricing;
    }
}

package com.alibaba.cloud.ai.autoconfigure.graph.health;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Integration test for health endpoint exposure (Dimension 4).
 * Tests one complete path: auto-config → registry → endpoint.
 */
class LlmReadinessHealthIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            HealthEndpointAutoConfiguration.class,
            HealthContributorAutoConfiguration.class,
            LlmReadinessAutoConfiguration.class
        ));

    // ===== Happy Path: Full Integration =====
    @Test
    void shouldExposeHealthEndpointWithLlmReadinessWhenApiKeyPresent() {
        this.contextRunner
            .withPropertyValues("AI_DASHSCOPE_API_KEY=test-key")
            .run(context -> {
                assertThat(context).hasSingleBean(HealthEndpoint.class);
                assertThat(context).hasSingleBean(HealthContributorRegistry.class);
                
                // Verify via registry
                HealthContributorRegistry registry = context.getBean(HealthContributorRegistry.class);
                HealthContributor contributor = registry.getContributor("llmReadiness");
                
                assertThat(contributor).isNotNull();
                assertThat(contributor).isInstanceOf(HealthIndicator.class);
                
                // Verify health status
                HealthIndicator indicator = (HealthIndicator) contributor;
                assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
                assertThat(indicator.health().getDetails()).containsEntry("provider", "dashscope");
            });
    }

    // ===== Unhappy Path: Integration with Missing Config =====
    @Test
    void shouldExposeHealthEndpointWithDownStatusWhenApiKeyMissing() {
        this.contextRunner
            .run(context -> {
                HealthContributorRegistry registry = context.getBean(HealthContributorRegistry.class);
                HealthContributor contributor = registry.getContributor("llmReadiness");
                
                assertThat(contributor).isNotNull();
                
                HealthIndicator indicator = (HealthIndicator) contributor;
                assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
                assertThat(indicator.health().getDetails())
                    .containsEntry("reason", "Missing LLM configuration");
            });
    }
    
    // ===== Verify endpoint name convention =====
    @Test
    void shouldRegisterContributorWithCorrectName() {
        this.contextRunner
            .withPropertyValues("AI_DASHSCOPE_API_KEY=test-key")
            .run(context -> {
                HealthContributorRegistry registry = context.getBean(HealthContributorRegistry.class);
                
                // Verify the name follows Spring Boot naming convention
                // Bean name "llmReadinessHealthIndicator" → contributor name "llmReadiness"
                assertThat(registry.getContributor("llmReadiness")).isNotNull();
                assertThat(registry.getContributor("llmReadinessHealthIndicator")).isNull();
            });
    }
}

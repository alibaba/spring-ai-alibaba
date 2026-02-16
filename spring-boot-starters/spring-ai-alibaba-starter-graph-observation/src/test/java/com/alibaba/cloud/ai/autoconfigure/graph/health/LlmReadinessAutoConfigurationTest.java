package com.alibaba.cloud.ai.autoconfigure.graph.health;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tests auto-configuration behavior (Dimension 1+2).
 * One test per boundary of each sub-dimension.
 */
class LlmReadinessAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(LlmReadinessAutoConfiguration.class));

    // ===== DIM 1: Actuator Presence =====
    @Test
    void shouldAutoConfigureHealthIndicatorWhenActuatorPresent() {
        this.contextRunner.run(context -> {
            assertThat(context).hasSingleBean(HealthIndicator.class);
            assertThat(context).hasBean("llmReadinessHealthIndicator");
        });
    }

    // ===== DIM 2: Bean Lifecycle =====
    @Test
    void shouldNotOverrideCustomHealthIndicator() {
        this.contextRunner
            .withUserConfiguration(CustomHealthIndicatorConfig.class)
            .run(context -> {
                assertThat(context).hasSingleBean(HealthIndicator.class);
                HealthIndicator indicator = context.getBean("llmReadinessHealthIndicator", HealthIndicator.class);
                assertThat(indicator).isInstanceOf(CustomHealthIndicator.class);
            });
    }

    @Configuration
    static class CustomHealthIndicatorConfig {
        @Bean
        public HealthIndicator llmReadinessHealthIndicator() {
            return new CustomHealthIndicator();
        }
    }

    static class CustomHealthIndicator implements HealthIndicator {
        @Override
        public Health health() {
            return Health.up().withDetail("custom", true).build();
        }
    }
}

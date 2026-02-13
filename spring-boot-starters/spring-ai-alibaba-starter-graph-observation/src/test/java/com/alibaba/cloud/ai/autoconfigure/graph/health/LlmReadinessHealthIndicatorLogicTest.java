package com.alibaba.cloud.ai.autoconfigure.graph.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.mock.env.MockEnvironment;

/**
 * Tests health indicator logic in isolation (Dimension 3).
 * Tests each boundary of API key validation independently.
 */
class LlmReadinessHealthIndicatorLogicTest {

    // ===== Boundary: API Key Present (IN) =====
    @Test
    void shouldReportUpWhenApiKeyIsPresent() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("AI_DASHSCOPE_API_KEY", "valid-key-123");

        LlmReadinessHealthIndicator indicator = new LlmReadinessHealthIndicator(env);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("provider", "dashscope");
    }

    // ===== Boundary: API Key Missing (OUT) =====
    @Test
    void shouldReportDownWhenApiKeyIsMissing() {
        MockEnvironment env = new MockEnvironment();
        // No property set

        LlmReadinessHealthIndicator indicator = new LlmReadinessHealthIndicator(env);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
            .containsEntry("reason", "Missing LLM configuration");
        
        @SuppressWarnings("unchecked")
        List<String> missing = (List<String>) health.getDetails().get("missing");
        assertThat(missing).containsExactly("AI_DASHSCOPE_API_KEY");
    }

    // ===== Boundary: API Key Empty/Blank (EDGE) =====
    @Test
    void shouldReportDownWhenApiKeyIsBlank() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("AI_DASHSCOPE_API_KEY", "   ");

        LlmReadinessHealthIndicator indicator = new LlmReadinessHealthIndicator(env);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        
        @SuppressWarnings("unchecked")
        List<String> missing = (List<String>) health.getDetails().get("missing");
        assertThat(missing).contains("AI_DASHSCOPE_API_KEY");
    }

    @Test
    void shouldReportDownWhenApiKeyIsEmpty() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("AI_DASHSCOPE_API_KEY", "");

        LlmReadinessHealthIndicator indicator = new LlmReadinessHealthIndicator(env);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }

    // ===== Edge Case: Null Environment =====
    @Test
    void shouldHandleNullEnvironmentGracefully() {
        // This test documents expected behavior if Environment is somehow null
        // In production this shouldn't happen due to Spring's injection
        LlmReadinessHealthIndicator indicator = new LlmReadinessHealthIndicator(null);
        
        // Should throw NPE or handle gracefully - document the choice
        assertThat(org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class,
            () -> indicator.health()
        )).isNotNull();
    }
}

package com.alibaba.cloud.ai.autoconfigure.graph.health;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * LLM readiness check for local deployments.
 * Currently validates DashScope API key presence as documented in the project README.
 */
public class LlmReadinessHealthIndicator implements HealthIndicator {

    private final Environment environment;

    public LlmReadinessHealthIndicator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public Health health() {
        List<String> missing = new ArrayList<>();

        // DashScope (as documented in README)
        String dashscopeKey = environment.getProperty("AI_DASHSCOPE_API_KEY");
        if (!StringUtils.hasText(dashscopeKey)) {
            missing.add("AI_DASHSCOPE_API_KEY");
        }

        if (!missing.isEmpty()) {
            return Health.down()
                    .withDetail("reason", "Missing LLM configuration")
                    .withDetail("missing", missing)
                    .build();
        }

        return Health.up()
                .withDetail("provider", "dashscope")
                .build();
    }
}

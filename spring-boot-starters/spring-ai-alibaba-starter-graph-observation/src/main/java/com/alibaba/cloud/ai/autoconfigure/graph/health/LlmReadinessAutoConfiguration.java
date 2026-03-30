package com.alibaba.cloud.ai.autoconfigure.graph.health;

import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@AutoConfiguration
@ConditionalOnClass(HealthIndicator.class)
public class LlmReadinessAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "llmReadinessHealthIndicator")
    public HealthIndicator llmReadinessHealthIndicator(Environment environment) {
        return new LlmReadinessHealthIndicator(environment);
    }
}


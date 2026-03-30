package com.alibaba.cloud.ai.autoconfigure.graph.health.config;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import com.alibaba.cloud.ai.autoconfigure.graph.health.metrics.AgentExecutionInterceptor;
import com.alibaba.cloud.ai.autoconfigure.graph.health.metrics.AgentMetrics;
import com.alibaba.cloud.ai.autoconfigure.graph.health.metrics.LlmMetrics;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Auto-configuration for Spring AI Alibaba observability features.
 */
@AutoConfiguration(after = MetricsAutoConfiguration.class)
@ConditionalOnClass({MeterRegistry.class})
@ConditionalOnProperty(
    prefix = "spring.ai.alibaba.observability",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
@EnableConfigurationProperties(ObservabilityProperties.class)
@EnableAspectJAutoProxy
public class ObservabilityAutoConfiguration {

    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    public AgentMetrics agentMetrics(MeterRegistry meterRegistry, 
                                     ObservabilityProperties properties) {
        return new AgentMetrics(meterRegistry, properties);
    }

    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    public LlmMetrics llmMetrics(MeterRegistry meterRegistry,
                                 ObservabilityProperties properties) {
        return new LlmMetrics(meterRegistry, properties);
    }

    @Bean
    public AgentExecutionInterceptor agentExecutionInterceptor(
            AgentMetrics agentMetrics,
            LlmMetrics llmMetrics) {
        return new AgentExecutionInterceptor(agentMetrics, llmMetrics);
    }
}

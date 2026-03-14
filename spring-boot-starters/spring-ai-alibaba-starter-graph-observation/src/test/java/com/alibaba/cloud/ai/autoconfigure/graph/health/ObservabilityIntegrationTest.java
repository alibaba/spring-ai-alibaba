package com.alibaba.cloud.ai.autoconfigure.graph.health;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.alibaba.cloud.ai.autoconfigure.graph.health.config.ObservabilityAutoConfiguration;
import com.alibaba.cloud.ai.autoconfigure.graph.health.metrics.AgentMetrics;
import com.alibaba.cloud.ai.autoconfigure.graph.health.metrics.LlmMetrics;
import com.alibaba.cloud.ai.autoconfigure.graph.health.model.ExecutionContext;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class ObservabilityIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ObservabilityAutoConfiguration.class))
        .withBean(MeterRegistry.class, SimpleMeterRegistry::new);

    @Test
    void shouldAutoConfigureWhenMicrometerIsPresent() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AgentMetrics.class);
            assertThat(context).hasSingleBean(LlmMetrics.class);
        });
    }

    @Test
    void shouldNotAutoConfigureWhenDisabled() {
        contextRunner
            .withPropertyValues("spring.ai.alibaba.observability.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(AgentMetrics.class);
            });
    }

    @Test
    void shouldRecordAgentExecutionMetrics() {
        contextRunner.run(context -> {
            AgentMetrics agentMetrics = context.getBean(AgentMetrics.class);
            MeterRegistry registry = context.getBean(MeterRegistry.class);

            ExecutionContext ctx = ExecutionContext.builder()
                .agentName("test-agent")
                .agentType("ReactAgent")
                .duration(Duration.ofMillis(100))
                .status(ExecutionContext.ExecutionStatus.SUCCESS)
                .build();

            agentMetrics.recordExecution(ctx);

            assertThat(registry.find("spring.ai.agent.execution.duration").timer())
                .isNotNull();
            assertThat(registry.find("spring.ai.agent.execution.total").counter())
                .isNotNull()
                .satisfies(counter -> assertThat(counter.count()).isEqualTo(1.0));
        });
    }

    @Test
    void shouldRecordLlmMetrics() {
        contextRunner.run(context -> {
            LlmMetrics llmMetrics = context.getBean(LlmMetrics.class);
            MeterRegistry registry = context.getBean(MeterRegistry.class);

            llmMetrics.recordLlmCall(
                "qwen-max",
                Duration.ofMillis(500),
                1000L,
                500L,
                true
            );

            assertThat(registry.find("spring.ai.llm.request.duration").timer())
                .isNotNull();
            assertThat(registry.find("spring.ai.llm.tokens.input").summary())
                .isNotNull();
        });
    }
}

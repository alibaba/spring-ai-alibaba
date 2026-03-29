package com.alibaba.cloud.ai.autoconfigure.graph.health;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class LlmReadinessAutoConfigurationNoActuatorTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(LlmReadinessAutoConfiguration.class))
		.withClassLoader(new FilteredClassLoader(HealthIndicator.class));

	@Test
	void shouldNotAutoConfigureWhenActuatorMissing() {
		this.contextRunner.run(context -> {
			assertThat(context).doesNotHaveBean(HealthIndicator.class);
		});
	}

}

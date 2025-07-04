/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.autoconfigure.graph;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.observation.GraphObservationLifecycleListener;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GraphObservationAutoConfiguration}.
 *
 * @author sixiyida
 * @since 2025/7/3
 */
class GraphObservationAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(GraphObservationAutoConfiguration.class));

	@Test
	void shouldAutoConfigureWhenEnabled() {
		this.contextRunner.withPropertyValues("spring.ai.alibaba.graph.observation.enabled=true")
			.withUserConfiguration(TestConfiguration.class)
			.run(context -> {
				assertThat(context).hasSingleBean(GraphObservationLifecycleListener.class);
				assertThat(context).hasSingleBean(CompileConfig.class);
				assertThat(context).hasBean("observationGraphCompileConfig");
			});
	}

	@Test
	void shouldNotAutoConfigureWhenDisabled() {
		this.contextRunner.withPropertyValues("spring.ai.alibaba.graph.observation.enabled=false").run(context -> {
			assertThat(context).doesNotHaveBean(GraphObservationLifecycleListener.class);
			assertThat(context).doesNotHaveBean(CompileConfig.class);
		});
	}

	@Test
	void shouldAutoConfigureWithDefaultProperties() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class).run(context -> {
			assertThat(context).hasSingleBean(GraphObservationLifecycleListener.class);
			assertThat(context).hasSingleBean(CompileConfig.class);

			GraphObservationProperties properties = context.getBean(GraphObservationProperties.class);
			assertThat(properties.isEnabled()).isTrue();
		});
	}

	@Test
	void shouldConfigureObservationHandlersWhenMeterRegistryPresent() {
		this.contextRunner.withUserConfiguration(TestConfigurationWithMeterRegistry.class).run(context -> {
			assertThat(context).hasBean("graphObservationHandler");
			assertThat(context).hasBean("graphNodeObservationHandler");
			assertThat(context).hasBean("graphEdgeObservationHandler");
		});
	}

	@Test
	void shouldNotConfigureObservationHandlersWhenMeterRegistryAbsent() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class).run(context -> {
			assertThat(context).doesNotHaveBean("graphObservationHandler");
			assertThat(context).doesNotHaveBean("graphNodeObservationHandler");
			assertThat(context).doesNotHaveBean("graphEdgeObservationHandler");
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		ObservationRegistry observationRegistry() {
			return ObservationRegistry.create();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfigurationWithMeterRegistry extends TestConfiguration {

		@Bean
		MeterRegistry meterRegistry() {
			return new SimpleMeterRegistry();
		}

	}

}

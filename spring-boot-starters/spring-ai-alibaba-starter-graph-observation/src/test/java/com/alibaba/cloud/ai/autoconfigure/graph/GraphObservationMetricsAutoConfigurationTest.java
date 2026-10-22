/*
 * Copyright 2024-2026 the original author or authors.
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

import com.alibaba.cloud.ai.graph.observation.edge.GraphEdgeObservationHandler;
import com.alibaba.cloud.ai.graph.observation.graph.GraphObservationHandler;
import com.alibaba.cloud.ai.graph.observation.node.GraphNodeObservationHandler;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class GraphObservationMetricsAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(GraphObservationAutoConfiguration.class, MetricsAutoConfiguration.class,
					SimpleMetricsExportAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class));

	@Test
	void shouldConfigureHandlersForAutoConfiguredMeterRegistry() {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(MeterRegistry.class);
			assertThat(context).hasSingleBean(GraphObservationHandler.class);
			assertThat(context).hasSingleBean(GraphNodeObservationHandler.class);
			assertThat(context).hasSingleBean(GraphEdgeObservationHandler.class);
		});
	}

	@Test
	void shouldKeepCustomHandlerWithAutoConfiguredMeterRegistry() {
		this.contextRunner.withUserConfiguration(CustomHandlerConfiguration.class).run(context -> {
			assertThat(context).hasSingleBean(GraphObservationHandler.class);
			assertThat(context.getBean(GraphObservationHandler.class)).isSameAs(context.getBean("customGraphHandler"));
			assertThat(context).hasSingleBean(GraphNodeObservationHandler.class);
			assertThat(context).hasSingleBean(GraphEdgeObservationHandler.class);
		});
	}

	@Test
	void shouldNotConfigureHandlersWhenDisabled() {
		this.contextRunner.withPropertyValues("spring.ai.alibaba.graph.observation.enabled=false").run(context -> {
			assertThat(context).hasSingleBean(MeterRegistry.class);
			assertThat(context).doesNotHaveBean(GraphObservationHandler.class);
			assertThat(context).doesNotHaveBean(GraphNodeObservationHandler.class);
			assertThat(context).doesNotHaveBean(GraphEdgeObservationHandler.class);
		});
	}

	@Test
	void shouldConfigureWithoutActuatorOnClasspath() {
		new ApplicationContextRunner().withClassLoader(new FilteredClassLoader("org.springframework.boot.actuate"))
			.withConfiguration(AutoConfigurations.of(GraphObservationAutoConfiguration.class))
			.run(context -> {
				assertThat(context).hasNotFailed();
				assertThat(context).hasSingleBean(GraphObservationProperties.class);
				assertThat(context).doesNotHaveBean(MeterRegistry.class);
				assertThat(context).doesNotHaveBean(GraphObservationHandler.class);
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomHandlerConfiguration {

		@Bean
		GraphObservationHandler customGraphHandler(MeterRegistry meterRegistry) {
			return new GraphObservationHandler(meterRegistry);
		}

	}

}

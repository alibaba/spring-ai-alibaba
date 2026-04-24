/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.a2a.autoconfigure.server;

import com.alibaba.cloud.ai.a2a.autoconfigure.A2aMultiAgentProperties;
import com.alibaba.cloud.ai.a2a.core.route.MultiAgentRequestRouter;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link A2aServerMultiAgentAutoConfiguration}.
 *
 * @author xiweng.yy
 */
class A2aServerMultiAgentAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(A2aServerMultiAgentAutoConfiguration.class));

	@Test
	void shouldNotCreateMultiAgentBeansWhenNoAgentsConfigured() {
		this.contextRunner.run(context -> {
			assertThat(context).doesNotHaveBean(MultiAgentRequestRouter.class);
		});
	}

	@Test
	void shouldCreateMultiAgentPropertiesWhenAgentsConfigured() {
		this.contextRunner
			.withPropertyValues("spring.ai.alibaba.a2a.server.agents.weather-agent.name=Weather Agent",
					"spring.ai.alibaba.a2a.server.agents.weather-agent.description=Weather service",
					"spring.ai.alibaba.a2a.server.agents.translate-agent.name=Translate Agent",
					"spring.ai.alibaba.a2a.server.agents.translate-agent.description=Translation service",
					"spring.ai.alibaba.a2a.server.address=localhost", "spring.ai.alibaba.a2a.server.port=8080")
			.run(context -> {
				assertThat(context).hasSingleBean(A2aMultiAgentProperties.class);
				assertThat(context).hasSingleBean(MultiAgentRequestRouter.class);

				A2aMultiAgentProperties props = context.getBean(A2aMultiAgentProperties.class);
				assertThat(props.isMultiAgentMode()).isTrue();
				assertThat(props.getAgents()).hasSize(2);
				assertThat(props.getAgents()).containsKey("weather-agent");
				assertThat(props.getAgents()).containsKey("translate-agent");
				assertThat(props.getAgents().get("weather-agent").getName()).isEqualTo("Weather Agent");
				assertThat(props.getAgents().get("translate-agent").getName()).isEqualTo("Translate Agent");
			});
	}

	@Test
	void shouldCreateRouterWithEmptyHandlersWhenNoAgentBeansMatch() {
		this.contextRunner
			.withPropertyValues("spring.ai.alibaba.a2a.server.agents.weather-agent.name=Weather Agent",
					"spring.ai.alibaba.a2a.server.address=localhost", "spring.ai.alibaba.a2a.server.port=8080")
			.run(context -> {
				assertThat(context).hasSingleBean(MultiAgentRequestRouter.class);
				MultiAgentRequestRouter router = context.getBean(MultiAgentRequestRouter.class);
				// Without agent beans, no handlers should be registered
				assertThat(router.size()).isEqualTo(0);
				assertThat(router.hasHandler("weather-agent")).isFalse();
			});
	}

}

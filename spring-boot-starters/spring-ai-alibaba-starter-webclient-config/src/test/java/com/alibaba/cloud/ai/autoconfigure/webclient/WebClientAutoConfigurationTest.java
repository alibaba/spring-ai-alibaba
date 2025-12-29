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
package com.alibaba.cloud.ai.autoconfigure.webclient;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebClientAutoConfiguration}.
 *
 * @author GitHub Copilot
 */
class WebClientAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(WebClientAutoConfiguration.class));

	@Test
	void autoConfigurationIsEnabled() {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(WebClient.Builder.class);
			assertThat(context).hasSingleBean(WebClientConfigProperties.class);
		});
	}

	@Test
	void autoConfigurationCanBeDisabled() {
		this.contextRunner.withPropertyValues("spring.ai.alibaba.webclient.enabled=false").run(context -> {
			assertThat(context).doesNotHaveBean(WebClient.Builder.class);
		});
	}

	@Test
	void propertiesAreConfigurable() {
		this.contextRunner
			.withPropertyValues("spring.ai.alibaba.webclient.max-connections=1000",
					"spring.ai.alibaba.webclient.max-idle-time=60s",
					"spring.ai.alibaba.webclient.max-life-time=10m")
			.run(context -> {
				assertThat(context).hasSingleBean(WebClientConfigProperties.class);
				WebClientConfigProperties properties = context.getBean(WebClientConfigProperties.class);
				assertThat(properties.getMaxConnections()).isEqualTo(1000);
				assertThat(properties.getMaxIdleTime()).hasSeconds(60);
				assertThat(properties.getMaxLifeTime()).hasMinutes(10);
			});
	}

	@Test
	void webClientBuilderIsCreated() {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(WebClient.Builder.class);
			WebClient.Builder builder = context.getBean(WebClient.Builder.class);
			assertThat(builder).isNotNull();

			// Verify we can build a WebClient
			WebClient webClient = builder.build();
			assertThat(webClient).isNotNull();
		});
	}

}

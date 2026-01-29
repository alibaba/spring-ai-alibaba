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
package com.alibaba.cloud.ai.agent.studio.config;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class WebConfigConditionalTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(WebConfig.class);

	@Test
	void shouldRegisterWebConfigWhenPropertyMissing() {
		this.contextRunner.run(context -> assertThat(context).hasSingleBean(WebConfig.class));
	}

	@Test
	void shouldNotRegisterWebConfigWhenDisabled() {
		this.contextRunner.withPropertyValues("spring.ai.alibaba.agent.studio.web.cors.enabled=false")
			.run(context -> assertThat(context).doesNotHaveBean(WebConfig.class));
	}

}


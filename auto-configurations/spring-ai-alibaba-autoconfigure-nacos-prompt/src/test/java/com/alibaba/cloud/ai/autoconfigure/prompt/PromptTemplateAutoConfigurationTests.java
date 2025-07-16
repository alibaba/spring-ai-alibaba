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
package com.alibaba.cloud.ai.autoconfigure.prompt;

import com.alibaba.cloud.ai.prompt.ConfigurablePromptTemplateFactory;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@SpringBootTest(classes = PromptTemplateAutoConfiguration.class)
class PromptTemplateAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(PromptTemplateAutoConfiguration.class);

	@Test
	void whenEnabledPropertyIsTrue_thenBeanShouldBeCreated() {

		this.contextRunner.withPropertyValues("spring.ai.nacos.prompt.template.enabled=true")
			.run(context -> assertThat(context).hasSingleBean(ConfigurablePromptTemplateFactory.class));
	}

	/**
	 * Nacos autoconfigure default value is false.
	 */
	@Test
	void whenNoPropertiesConfigured_thenBeanShouldNotBeCreated() {

		this.contextRunner.run(context -> assertThat(context).doesNotHaveBean(ConfigurablePromptTemplateFactory.class));
	}

	@Test
	void whenEnabledPropertyIsFalse_thenBeanShouldNotBeCreated() {

		this.contextRunner.withPropertyValues("spring.ai.nacos.prompt.template.enabled=false")
			.run(context -> assertThat(context).doesNotHaveBean(ConfigurablePromptTemplateFactory.class));
	}

}

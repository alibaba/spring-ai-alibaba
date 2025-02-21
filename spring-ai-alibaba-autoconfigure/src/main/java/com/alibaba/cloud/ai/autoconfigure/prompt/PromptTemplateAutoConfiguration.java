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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * @author KrakenZJC
 **/

@AutoConfiguration
@EnableConfigurationProperties(NacosPromptTmplProperties.class)
public class PromptTemplateAutoConfiguration {

	// @formatter:off
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(
			prefix = NacosPromptTmplProperties.TEMPLATE_PREFIX,
			name = "enabled",
			havingValue = "true"
	)
	public ConfigurablePromptTemplateFactory configurablePromptTemplateFactory() {

		return new ConfigurablePromptTemplateFactory();
	}
	// @formatter:on

}

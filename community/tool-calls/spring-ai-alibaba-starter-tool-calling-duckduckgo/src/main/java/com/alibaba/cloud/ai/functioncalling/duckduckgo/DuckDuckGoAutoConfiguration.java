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
package com.alibaba.cloud.ai.functioncalling.duckduckgo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

/**
 * @author 北极星
 */
@Configuration
@EnableConfigurationProperties(DuckDuckGoProperties.class)
@ConditionalOnProperty(prefix = "spring.ai.alibaba.functioncalling.duckduckgo", name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class DuckDuckGoAutoConfiguration {

	@ConditionalOnMissingBean
	@Bean
	@Description("use duckduckgo engine to search news.")
	public DuckDuckGoQueryNewsService duckDuckGoQueryNewsFunction(DuckDuckGoProperties duckDuckGoProperties) {
		return new DuckDuckGoQueryNewsService(duckDuckGoProperties);
	}

}

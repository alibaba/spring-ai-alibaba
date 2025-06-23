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

package com.alibaba.cloud.ai.toolcalling.sensitivefilter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

/**
 * @author Makoto
 */
@Configuration
@ConditionalOnClass(SensitiveFilterService.class)
@ConditionalOnProperty(prefix = SensitiveFilterConstants.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(SensitiveFilterProperties.class)
public class SensitiveFilterAutoConfiguration {

	@Bean(name = SensitiveFilterConstants.TOOL_NAME)
	@ConditionalOnMissingBean
	@Description("It is used to filter and replace sensitive information in text, "
			+ "such as mobile phone numbers, ID numbers, bank card numbers, etc")
	public SensitiveFilterService sensitiveFilter(SensitiveFilterProperties properties) {
		return new SensitiveFilterService(properties);
	}

}

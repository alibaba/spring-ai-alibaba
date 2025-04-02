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
 * 敏感信息过滤自动配置
 *
 * @author Makoto
 */
@Configuration
@ConditionalOnClass(SensitiveInfoFilterService.class)
@EnableConfigurationProperties(SensitiveInfoFilterProperties.class)
@ConditionalOnProperty(prefix = "spring.ai.alibaba.toolcalling.sensitivefilter", name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class SensitiveInfoFilterAutoConfiguration {

	private final SensitiveInfoFilterProperties properties;

	public SensitiveInfoFilterAutoConfiguration(SensitiveInfoFilterProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	@Description("检测和过滤文本中的敏感信息，如身份证号、手机号、信用卡号等个人隐私数据")
	public SensitiveInfoFilterService sensitiveInfoFilterFunction() {
		return new SensitiveInfoFilterService();
	}

}
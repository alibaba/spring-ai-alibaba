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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a> Temporarily fixed the issue
 * of Nacos log error during startup.
 */

public class PromptTmplNacosConfigCondition implements Condition {

	private final Logger logger = LoggerFactory.getLogger(PromptTmplNacosConfigCondition.class);

	public PromptTmplNacosConfigCondition() {
	}

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {

		// @formatter:off
		String tmplPrefix = NacosPromptTmplProperties.TEMPLATE_PREFIX + ".enabled";

		// The default value is false, means that the nacos prompt template is not enabled.
		Boolean enabled = context.getEnvironment().getProperty(tmplPrefix, Boolean.class, false);

		// Setting NacosAutoConfiguration#enabled=false, avoid spring alibaba nacos related bean auto config.
		// Causes an error to be reported in the Spring AI Alibaba Nacos startup log
		if (!enabled) {
			System.setProperty("spring.nacos.config.enabled", "false");
		}
		logger.debug("PromptTmplNacosConfigCondition matches enabled: {}",enabled);
		// @formatter:on

		return enabled;
	}

}

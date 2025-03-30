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
		logger.debug("PromptTmplNacosConfigCondition matches enabled: " + enabled);
		// @formatter:on

		return enabled;
	}

}

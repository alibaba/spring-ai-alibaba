package com.alibaba.cloud.ai.autoconfig.prompt;

import com.alibaba.cloud.ai.prompt.ConfigurablePromptTemplateFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * .
 *
 * @since 2024-09-20
 * @author KrakenZJC
 **/

public class PromptTemplateAutoConfiguration {

	private final static String TEMPLATE_PREFIX = "spring.ai.nacos.prompt.template";

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = TEMPLATE_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
	public ConfigurablePromptTemplateFactory configurablePromptTemplateFactory() {
		return new ConfigurablePromptTemplateFactory();
	}

}
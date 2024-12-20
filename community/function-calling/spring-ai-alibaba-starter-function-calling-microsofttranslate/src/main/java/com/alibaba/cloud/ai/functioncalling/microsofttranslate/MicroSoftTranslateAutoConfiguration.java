package com.alibaba.cloud.ai.functioncalling.microsofttranslate;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

/**
 * @author 31445
 */
@Configuration
@ConditionalOnClass(MicroSoftTranslateService.class)
@EnableConfigurationProperties(MicroSoftTranslateProperties.class)
@ConditionalOnProperty(prefix = "spring.ai.alibaba.functioncalling.microsofttranslate", name = "enabled",
		havingValue = "true")
public class MicroSoftTranslateAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Description("Implement natural language translation capabilities.")
	public MicroSoftTranslateService microSoftTranslateFunction(MicroSoftTranslateProperties properties) {
		return new MicroSoftTranslateService(properties);
	}

}

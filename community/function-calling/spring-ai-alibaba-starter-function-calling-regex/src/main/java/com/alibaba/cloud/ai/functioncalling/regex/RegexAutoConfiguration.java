package com.alibaba.cloud.ai.functioncalling.regex;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;

/**
 * @author 北极星
 */
@ConditionalOnClass(RegexService.class)
@ConditionalOnProperty(prefix = "spring.ai.alibaba.functioncalling.regex", name = "enabled", havingValue = "true")
public class RegexAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Description("Use regex to find content based on the expression.")
	public RegexService regexFindAllFunction() {
		return new RegexService();
	}

}
